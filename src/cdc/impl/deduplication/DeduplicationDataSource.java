package cdc.impl.deduplication;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AtomicCondition;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.datasource.wrappers.BufferedData;
import cdc.impl.join.blocking.BucketManager;
import cdc.utils.Log;
import cdc.utils.RJException;

public class DeduplicationDataSource extends AbstractDataSource {

	private AbstractDataSource parent;
	private boolean initialized = false;
	private BufferedData deduplicatedData;
	private BucketManager buckets;
	private DeduplicationConfig config;
	private int sizeDedup;
	
	public DeduplicationDataSource(AbstractDataSource parentSource, DeduplicationConfig config) {
		super(parentSource.getSourceName(), parentSource.getProperties());
		this.config = config;
		parent = parentSource;
	}
	
	public boolean canSort() {
		return false;
	}

	public void close() throws IOException, RJException {
		parent.close();
		if (deduplicatedData != null) {
			deduplicatedData.close();
		}
		if (buckets != null) {
			buckets.cleanup();
		}
		initialized = false;
	}

	public AbstractDataSource copy() throws IOException, RJException {
		initialize();
		DeduplicationDataSource that = new DeduplicationDataSource(parent, config);
		that.initialized = true;
		that.deduplicatedData = deduplicatedData.copy();
		that.config = config;
		return that;
	}

	protected void doReset() throws IOException, RJException {
		if (!initialized) {
			parent.reset();
		} else {
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
		Log.log(getClass(), "Deduplication begins for data source " + getSourceName(), 1);
		buckets = new BucketManager(config.getHashingFunction());
		deduplicatedData = new BufferedData(parent.size());
		DataRow row;
		while ((row = parent.getNextRow()) != null) {
			buckets.addToBucketLeftSource(row);
		}
		buckets.addingCompleted();
		Log.log(getClass(), "Deduplication: buckets generated", 2);
		DataRow[][] bucket;
		while ((bucket = buckets.getBucket()) != null) {
			deduplicate(bucket[0]);
		}
		deduplicatedData.addingCompleted();
		Log.log(getClass(), "Deduplication finished for data source " + getSourceName(), 1);
	}

	private void deduplicate(DataRow[] dataRows) throws IOException {
		for (int i = 0; i < dataRows.length; i++) {
			boolean dup = false;
			for (int j = i + 1; j < dataRows.length; j++) {
				if (duplicate(dataRows[i], dataRows[j])) {
					dup = true;
				}
			}
			if (!dup) {
				deduplicatedData.addRow(dataRows[i]);
				sizeDedup++;
			}
		}
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
		Log.log(getClass(), "Duplicates identified:\n   " + r1 + "\n   " + r2, 1);
		return true;
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
	
}
