package cdc.impl.deduplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AtomicCondition;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.datasource.wrappers.BufferedData;
import cdc.impl.join.blocking.BucketManager;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CPUInfo;
import cdc.utils.Log;
import cdc.utils.RJException;

public class DeduplicationDataSource extends AbstractDataSource {

	private AbstractDataSource parent;
	private boolean initialized = false;
	private BufferedData deduplicatedData;
	private BucketManager buckets;
	private DeduplicationConfig config;
	private int sizeDedup;
	private AtomicInteger nDuplicates = new AtomicInteger(0);
	
	private CSVFileSaver minusSaver = null;
	
	private CountDownLatch latch;
	private volatile RJException exception;
	private DeduplicationThread[] threads;
	private AtomicInteger progressDataSource = new AtomicInteger(0);
	private AtomicInteger progressDedupe = new AtomicInteger(0);
	
	private AtomicInteger duplicateId = new AtomicInteger(0);
	private int inputRecordsCnt = 0;
	
	public DeduplicationDataSource(AbstractDataSource parentSource, DeduplicationConfig config) {
		super(parentSource.getSourceName(), parentSource.getProperties());
		this.config = config;
		parent = parentSource;
	}
	
	public boolean canSort() {
		return false;
	}

	protected void doClose() throws IOException, RJException {
		parent.close();
		if (deduplicatedData != null) {
			deduplicatedData.close();
		}
		if (threads != null) {
			for (int i = 0; i < threads.length; i++) {
				threads[i].cancel();
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if (buckets != null) {
			buckets.cleanup();
		}
		initialized = false;
		
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	public AbstractDataSource copy() throws IOException, RJException {
		initialize();
		DeduplicationDataSource that = new DeduplicationDataSource(parent, config);
		that.initialized = true;
		that.inputRecordsCnt = inputRecordsCnt;
		that.nDuplicates = nDuplicates;
		that.deduplicatedData = deduplicatedData.copy();
		that.config = config;
		return that;
	}

	protected void doReset() throws IOException, RJException {
		if (!initialized) {
			parent.reset();
			progressDataSource.set(0);
			progressDedupe.set(0);
		} else {
			if (threads != null) {
				for (int i = 0; i < threads.length; i++) {
					threads[i].cancel();
					try {
						threads[i].join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			progressDedupe.set(0);
			duplicateId.set(0);
			deduplicatedData.reset();
		}
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof DeduplicationDataSource)) {
			return false;
		}
		DeduplicationDataSource that = (DeduplicationDataSource)arg0;
		return this.parent.equals(that.parent);
	}

	protected DataRow nextRow() throws IOException, RJException {
		initialize();
		return deduplicatedData.getDataRow();
	}

	public long size() throws IOException, RJException {
		initialize();
		return deduplicatedData.getSize();
	}

	private void initialize() throws IOException, RJException {
		if (initialized) {
			return;
		}
		initialized = true;
		sizeDedup = 0;
		inputRecordsCnt = 0;
		nDuplicates.set(0);
		
		if (config.getMinusFile() != null) {
			Map props = new HashMap();
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, config.getMinusFile());
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			minusSaver = new CSVFileSaver(props);
		}
		
		Log.log(getClass(), "Deduplication begins for data source " + getSourceName(), 1);
		buckets = new BucketManager(config.getHashingFunction());
		deduplicatedData = new BufferedData(parent.size());
		DataRow row;
		while ((row = parent.getNextRow()) != null) {
			buckets.addToBucketLeftSource(row);
			progressDataSource.set((int) (parent.position() * 100 / parent.size()));
			inputRecordsCnt++;
		}
		Log.log(getClass(), "Deduplication of " + getSourceName() + ": added " + inputRecordsCnt + " rows to bucket manager.", 2);
		buckets.addingCompleted();
		Log.log(getClass(), "Deduplication: buckets generated", 2);
		
		Log.log(getClass(), "Using " + CPUInfo.testNumberOfCPUs() + " cores for deduplication.");
		latch = new CountDownLatch(CPUInfo.testNumberOfCPUs());
		
		threads = new DeduplicationThread[CPUInfo.testNumberOfCPUs()];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new DeduplicationThread(i);
			threads[i].start();
		}
		
		try {
			while (latch.getCount() != 0) {
				latch.await(100, TimeUnit.MILLISECONDS);
				int progress = 0;
				for (int i = 0; i < threads.length; i++) {
					progress += threads[i].completed.get();
				}
				if (inputRecordsCnt != 0) {
					progressDedupe.set(progress * 100 / inputRecordsCnt);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < threads.length; i++) {
			sizeDedup += threads[i].getNonDuplicatesCnt();
			//nDuplicates += threads[i].getDuplicatesCnt();
		}
		if (minusSaver != null) {
			minusSaver.flush();
			minusSaver.close();
		}
		
		if (exception != null) {
			throw exception;
		}
		
		deduplicatedData.addingCompleted();
		Log.log(getClass(), "Deduplication finished for data source " + getSourceName() + ". Identified " + nDuplicates + " duplicates." , 1);
		Log.log(getClass(), "Size of data in deduplicated " + getSourceName() + ": " + sizeDedup + " (" + deduplicatedData.getSize() + ")" , 1);
	}
	
	public ModelGenerator getDataModel() {
		return parent.getDataModel();
	}
	
	public DataColumnDefinition[] getAvailableColumns() {
		return parent.getAvailableColumns();
	}
	
	public int getConfigurationProgress() {
		return parent.getConfigurationProgress();
	}
	
	public AtomicCondition[] getFilterCondition() {
		return parent.getFilterCondition();
	}
	
	public AbstractDataSource getRawDataSource() {
		return parent;
	}
	
	private class DeduplicationThread extends Thread {
		
		private volatile boolean cancel = false;
		//private int duplicatesCnt = 0;
		private int nonDuplicatesCnt = 0;
		private AtomicInteger completed = new AtomicInteger(0);
		
		private DataColumnDefinition[] model;
		
		public DeduplicationThread(int id) {
			setName("DeduplicationThread-" + id);
		}
		
		public void run() {
			Log.log(getClass(), "Thread " + getName() + " starts.", 2);
			
			DataRow[][] bucket;
			try {
				while (!cancel && (bucket = buckets.getBucket()) != null) {
					deduplicate(bucket[0]);
					completed.addAndGet(bucket[0].length);
				}
			} catch (IOException e) {
				exception = new RJException("Error reading data from BucketsManager", e);
			} catch (RJException e) {
				exception = e;
			}
			
			latch.countDown();
			Log.log(getClass(), "Thread " + getName() + " completed work.", 2);
		}
		
		private void deduplicate(DataRow[] dataRows) throws IOException, RJException {
			List duplicates = new ArrayList();
			for (int i = 0; i < dataRows.length; i++) {
				if (dataRows[i] == null) {
					continue;
				}
				for (int j = i + 1; j < dataRows.length; j++) {
					if (dataRows[j] == null) {
						continue;
					}
					if (duplicate(dataRows[i], dataRows[j])) {
						duplicates.add(dataRows[j]);
						dataRows[j] = null;
						nDuplicates.incrementAndGet();
					}
				}
				
				synchronized (deduplicatedData) {
					deduplicatedData.addRow(dataRows[i]);
				}
				nonDuplicatesCnt++;
				if (duplicates.size() != 0 && minusSaver != null) {
					//this is the original record that wasn't counted...
					//nDuplicates.incrementAndGet();
					
					synchronized (minusSaver) {
						int id = duplicateId.incrementAndGet();
						minusSaver.saveRow(extendRow(dataRows[i], id));
						for (Iterator iterator = duplicates.iterator(); iterator.hasNext();) {
							DataRow duplicate = (DataRow) iterator.next();
							minusSaver.saveRow(extendRow(duplicate, id));
						}
					}
				}
				
				dataRows[i] = null;
				duplicates.clear();
//				if (!dup) {
//					synchronized (deduplicatedData) {
//						deduplicatedData.addRow(dataRows[i]);
//					}
//					nonDuplicatesCnt++;
//				} else if (minusSaver != null) {
//					synchronized (minusSaver) {
//						minusSaver.saveRow(dataRows[i]);
//					}
//				}
			}
		}

		private DataRow extendRow(DataRow dataRow, int id) {
			DataCell[] oldCells = dataRow.getData();
			DataCell[] cells = new DataCell[oldCells.length + 1];
			cells[0] = new DataCell(DataColumnDefinition.TYPE_STRING, String.valueOf(id));
			System.arraycopy(oldCells, 0, cells, 1, oldCells.length);
			if (model == null) {
				DataColumnDefinition[] oldModel = dataRow.getRowModel();
				model = new DataColumnDefinition[oldModel.length + 1];
				model[0] = new DataColumnDefinition("Duplicate ID", DataColumnDefinition.TYPE_STRING, oldModel[0].getSourceName());
				System.arraycopy(oldModel, 0, model, 1, oldModel.length);
			}
			return new DataRow(model, cells);
		}

		private boolean duplicate(DataRow r1, DataRow r2) {
			DataColumnDefinition[] cols = config.getTestedColumns();
			AbstractDistance[] distances = config.getTestCondition();
			for (int i = 0; i < distances.length; i++) {
				DataCell cellA = r1.getData(cols[i]);
				DataCell cellB = r2.getData(cols[i]);
				if (distances[i].distance(cellA, cellB) == 0) {
					return false;
				}
			}
			Log.log(getClass(), "Duplicates identified:\n   " + r1 + "\n   " + r2, 2);
			return true;
		}
		
		public void cancel() {
			cancel = true;
		}
		
//		public int getDuplicatesCnt() {
//			return duplicatesCnt;
//		}
		
		public int getNonDuplicatesCnt() {
			return nonDuplicatesCnt;
		}
		
	}

	public int getProgress() {
		return (1 * progressDataSource.get() + 2 * progressDedupe.get()) / 3;
	}

	public int getDuplicatesCount() {
		return nDuplicates.get();
	}

	public int getInputRecordsCount() {
		return inputRecordsCnt;
	}
	
}
