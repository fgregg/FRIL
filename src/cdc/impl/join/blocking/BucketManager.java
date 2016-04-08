/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.impl.join.blocking;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import cdc.datamodel.DataRow;
import cdc.impl.datastream.DataRowInputStream;
import cdc.impl.datastream.DataRowOutputStream;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class BucketManager {

	private static final int BLOCK_TASH_FACTOR = Props.getInteger("bucket-manager-trash-factor");
	private static final String FILE_PREFIX = Props.getString("bucket-manager-file-prefix");
	private static final int FILE_POOL = Props.getInteger("bucket-manager-file-pool");
	
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final String BUCKET_MARKER = "bucket-id";
	
	private class BufferItem {
		private int id;
		private DataRow row;
		private String hash;
	}
	
	private class BufferConsumer extends Thread {
		
		public void run() {
			Log.log(getClass(), "Thread starts.", 2);
			int addedLeft = 0;
			int addedRight = 0;
			try {
				while ((!done || !buffer.isEmpty()) && !stopped) {
					BufferItem item = (BufferItem)buffer.poll(100, TimeUnit.MILLISECONDS);
					if (item == null) {
						continue;
					} else if (item.id == LEFT) {
						addedLeft++;
						addToBucket(LEFT, item.row, item.hash);
						//System.out.println("Adding to left: " + item.row);
					} else {
						addedRight++;
						int pool = addToBucket(RIGHT, item.row, item.hash);
						test[pool] = true;
					}
				}
				Log.log(getClass(), "Thread done with data reading. Now pushing results.", 2);
				//System.out.println("Thread received left: " + addedLeft);
				//System.out.println("Thread received right: " + addedRight);
				//verifyBuckets();
				counter.countDown();
				addedLeft = addedRight = 0;
				while (true && !stopped) {
					if (bucketsInMemory.isEmpty()) {
						closeStreams();
						if (usedFilesFromPool[0] == FILE_POOL || usedFilesFromPool[1] == FILE_POOL) {
							break;
						}
						read[LEFT] = new HashMap();
						read[RIGHT] = new HashMap();
						if (limit[0] && limit[1]) {
							readBucketsFromFile(LEFT);
							readBucketsFromFile(RIGHT);
						} else if (limit[0]) {
							readBucketsFromFile(LEFT);
							readBucketsFromMem(RIGHT);
						} else if (limit[1]) {
							readBucketsFromMem(LEFT);
							readBucketsFromFile(RIGHT);
						} else {
							readBucketsFromMem(LEFT);
							readBucketsFromMem(RIGHT);
						}
						
						bucketsInMemory.addAll(read[LEFT].keySet());
						for (Iterator iterator = read[RIGHT].keySet().iterator(); iterator.hasNext();) {
							Bucket b = (Bucket) iterator.next();
							if (!bucketsInMemory.contains(b)) {
								bucketsInMemory.add(b);
							}
						}
						
//						if (read[LEFT].isEmpty()) {
//							bucketsInMemory.addAll(read[RIGHT].keySet());
//						} else {
//							bucketsInMemory.addAll(read[LEFT].keySet());
//						}
					}
					
					if (bucketsInMemory.isEmpty()) {
						break;
					}
					
					DataRow[][] ret = new DataRow[2][];
					Bucket b = (Bucket) bucketsInMemory.remove(0);
					
					ret[0] = getBucket(LEFT, b);
					ret[1] = getBucket(RIGHT, b);
					addedLeft += ret[0].length;
					addedRight += ret[1].length;
					Log.log(getClass(), "Adding bucket to buffer: " + ret[0].length + " <-> " + ret[1].length, 3);
					bufferBuckets.put(ret);
				}
				Log.log(getClass(), "Thread has completed.", 2);
				//System.out.println("Thread retrieved left: " + addedLeft);
				//System.out.println("Thread retrieved right: " + addedRight);
				//System.out.println("Not touched: " + buckets.size());
				bufferBuckets.put(new DataRow[][] {});
				thread = null;
				return;
			} catch (InterruptedException e) {
				Log.log(getClass(), "Thread was interrupted.", 2);
			} catch (IOException e) {
				error = true;
				synchronized(BucketManager.this) {
					exception = new RJException("Error", e);
				}
			} catch (RJException e) {
				error = true;
				synchronized(BucketManager.this) {
					exception = e;
				}
			}
			Log.log(getClass(), "Thread has completed with error.", 2);
			thread = null;
		}
	}
	
//	private void verifyBuckets() {
//		int l = 0;
//		int r = 0;
//		for (Iterator iterator = buckets.values().iterator(); iterator.hasNext();) {
//			Bucket b = (Bucket) iterator.next();
//			l += b.getLeftRowsCount();
//			r += b.getRightRowsCount();
//			if (b.getLeftRowsCount() == 0) {
//				System.out.println("Bucket: " + b);
//			}
//		}
//		System.out.println("::: " + l + "  --  " + r);
//		
//		r = 0;
//		for (int i = 0; i < blocks[1].length; i++) {
//			Map bb = blocks[1][i];
//			for (Iterator iterator = bb.keySet().iterator(); iterator.hasNext();) {
//				Bucket b = (Bucket) iterator.next();
//				List list = (List) bb.get(b);
//				r += list.size();
//			}
//		}
//		System.out.println("RRRRRRRR: " + r);
//		
//	}
	
	//private static final int logLevel = Log.getLogLevel(BucketManager.class);
	
	private String filePrefix;
	
	private File[][] file = new File[2][FILE_POOL];
	private DataRowOutputStream dros[][] = new DataRowOutputStream[2][FILE_POOL];
	
	private HashingFunction blockingFunction;
	
	private Map[][] blocks;
	private Map bucktesToFileId = new HashMap();
	private int[] sizes;
	private int nextFileFromPool = 0;
	private Map buckets = new HashMap();
	
	private int[] usedFilesFromPool;
	private Iterator bucketIterator[] = new Iterator[FILE_POOL];
	private Iterator rowsIterator;
	private Bucket trashedBucket;
	
	private boolean limit[] = new boolean[] {false, false};
	private Map[] read = new Map[2];
	private List bucketsInMemory = new ArrayList();
	
	private boolean test[] = new boolean[FILE_POOL];
	private boolean cachingEnabled;
	private int addedRows = 0;
	
	private BufferConsumer thread = new BufferConsumer();
	private volatile boolean stopped= false;
	private volatile boolean done = false;
	private volatile boolean error = false;
	private volatile RJException exception = null;
	private ArrayBlockingQueue buffer = new ArrayBlockingQueue(Props.getInteger("intrathread-buffer"));
	private ArrayBlockingQueue bufferBuckets = new ArrayBlockingQueue(300);
	private CountDownLatch counter = new CountDownLatch(1);
	private volatile boolean completed = false;
	
	private AtomicInteger leftSize = new AtomicInteger(0);
	private AtomicInteger rightSize = new AtomicInteger(0);
	
	public BucketManager(HashingFunction blockingFunction, boolean cache) {
		this.cachingEnabled = cache;
		
		this.filePrefix = FILE_PREFIX + "_" + hashCode();
		
		Log.log(getClass(), "Buckets manager created. File prefix: " + filePrefix, 1);
		
		this.blockingFunction = blockingFunction;
		blocks = new Map[2][FILE_POOL];
		for (int i = 0; i < FILE_POOL; i++) {
			blocks[0][i] = new HashMap();
		}
		for (int i = 0; i < FILE_POOL; i++) {
			blocks[1][i] = new HashMap();
		}
		sizes = new int[2];
		for (int i = 0; i < FILE_POOL; i++) {
			file[0][i] = new File(filePrefix + "_" + i +  "_0.bin");
			file[0][i].deleteOnExit();
		}
		for (int i = 0; i < FILE_POOL; i++) {
			file[1][i] = new File(filePrefix + "_" + i + "_1.bin");
			file[1][i].deleteOnExit();
			test[i] = false;
		}
		usedFilesFromPool = new int[2];
		usedFilesFromPool[0] = 0;
		usedFilesFromPool[1] = 0;
		
		thread.setName("buckets-manager-thread");
		thread.start();
	}
	
	public BucketManager(HashingFunction blockingFunction) {
		this(blockingFunction, true);
	}

	public void addToBucketLeftSource(DataRow row) throws IOException {
		BufferItem item = new BufferItem();
		item.row = row;
		item.id = LEFT;
		item.hash = blockingFunction.hash(row, LEFT);
		if (item.hash == null) {
			item.hash = "";
		}
		leftSize.incrementAndGet();
		try {
			buffer.put(item);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void addToBucketRightSource(DataRow row) throws IOException {
		BufferItem item = new BufferItem();
		item.row = row;
		item.id = RIGHT;
		item.hash = blockingFunction.hash(row, RIGHT);
		if (item.hash == null) {
			item.hash = "";
		}
		rightSize.incrementAndGet();
		try {
			buffer.put(item);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized DataRow[][] getBucket() throws IOException, RJException {
		try {
			if (completed ) {
				return null;
			}
			while (true) {
				if (error) {
					synchronized(this) {
						throw exception;
					}
				}
				DataRow[][] bucket = (DataRow[][]) bufferBuckets.poll(100, TimeUnit.MILLISECONDS);
				if (bucket == null) {
					continue;
				} else if (bucket.length == 0) {
					completed = true;
					break;
				} else {
					resetRows(bucket);
					return bucket;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void resetRows(DataRow[][] bucket) {
		for (int i = 0; i < bucket.length; i++) {
			for (int j = 0; j < bucket[i].length; j++) {
				RowUtils.resetRow(bucket[i][j]);
			}
		}
	}

	public void stopProcessing() {
		stopped = true;
	}
	
	public void addingCompleted() {
		done = true;
		try {
			counter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//System.out.println("Left: " + leftSize.get());
		//System.out.println("Right: " + rightSize.get());
	}

	private DataRow[] getBucket(int id, Bucket b) {
		List bucket = (List)read[id].get(b);
		if (bucket == null) {
			bucket = new ArrayList();
		}
		return (DataRow[]) bucket.toArray(new DataRow[] {});
	}

	private void closeStreams() throws IOException, RJException {
		if (limit[0]) {
			for (int i = 0; i < FILE_POOL; i++) {
				if (dros[0][i] != null) dros[0][i].close();
			}
		}
		if (limit[1]) {
			for (int i = 0; i < FILE_POOL; i++) {
				if (dros[1][i] != null) dros[1][i].close();
			}
		}
	}
	
	public void cleanup() throws IOException, RJException {
		//System.out.println(getClass().getName() + ": Cleanup called.");
		closeStreams();
		if (file != null) {
			for (int i = 0; i < file.length; i++) {
				if (file[i] == null) {
					continue;
				}
				for (int j = 0; j < file[i].length; j++) {
					if (file[i][j] != null) {
						file[i][j].delete();
					}
				}
			}
		}
	}

	private void readBucketsFromMem(int id) {
		read[id] = blocks[id][usedFilesFromPool[id]];
		usedFilesFromPool[id]++;
	}

	private void readBucketsFromFile(int id) throws FileNotFoundException, IOException, RJException {
		DataRowInputStream dris;
		try {
			dris = new DataRowInputStream(createInputStream(file[id][usedFilesFromPool[id]]));
		} catch (FileNotFoundException e) {
			System.out.println("File " + file[id][usedFilesFromPool[id]] + " has not been found.");
			return;
		}
		DataRow row;
		while ((row = dris.readDataRow()) != null) {
			Bucket b = new Bucket(Integer.parseInt(row.getProperty(BUCKET_MARKER)));
			List list = (List) read[id].get(b);
			if (list == null) {
				list = new ArrayList();
				read[id].put(b, list);
			}
			list.add(row);
		}
		usedFilesFromPool[id]++;
		dris.close();
	}

	private synchronized int addToBucket(int id, DataRow row, String bucket) throws IOException {
		addedRows++;
		sizes[id]++;
		
		if (bucket == null) {
			return 0;
		}
		//System.out.println("Data: " + row.getData(blockingFactor[0][id]) + "   Bucket: " + buck[0]);
		Bucket b = (Bucket)buckets.get(bucket); //new Bucket(new String[] {bucket});
		if (b == null) {
			b = new Bucket(new String[] {bucket});
			buckets.put(bucket, b);
		}
		if (id == LEFT) {
			b.leftRecordAdded();
		} else {
			b.rightRecordAdded();
		}
		Integer pool = (Integer) bucktesToFileId.get(b);
		if (pool == null) {
			pool = new Integer(nextFileFromPool);
			bucktesToFileId.put(b, pool);
			nextFileFromPool = (nextFileFromPool + 1) % FILE_POOL;
		}

		int poolId = pool.intValue();
		if (blocks[id][poolId].containsKey(b)) {
			List l = (List) blocks[id][poolId].get(b);
			l.add(row);
		} else {
			List l = new ArrayList();
			l.add(row);
			blocks[id][poolId].put(b, l);
		}
		if (cachingEnabled) {
			trashIfNeeded(id);
		}
		return poolId;
	}

	private void trashIfNeeded(int id) throws IOException {
		if (sizes[id] > BLOCK_TASH_FACTOR) {
			//System.out.println("Trashing to file");
			
			limit[id] = true;
			for (int i = 0; i < FILE_POOL; i++) {
				bucketIterator[i] = null;
				DataRow row = getNext(id, i);
				if (row == null) {
					//System.out.println("Not sure here....test it");
					//this is just empty bucket... (no file data needs to be written)
					continue;
				}
				if (dros[id][i] == null) {
					dros[id][i] = new DataRowOutputStream(row.getSourceName(), row.getRowModel(), createOutputStream(file[id][i]));
				}
				do {
					dros[id][i].writeDataRow(row);
				} while ((row = getNext(id, i)) != null);
				blocks[id][i] = new HashMap();
			}
			sizes[id] = 0;
		}
	}

	private DataRow getNext(int id, int poolId) {
		if (bucketIterator[poolId] == null) {
			bucketIterator[poolId] = blocks[id][poolId].keySet().iterator();
		}
		if (rowsIterator == null || !rowsIterator.hasNext()) {
			if (!bucketIterator[poolId].hasNext()) {
				return null;
			}
			trashedBucket = (Bucket) bucketIterator[poolId].next();
			rowsIterator = ((List)blocks[id][poolId].get(trashedBucket)).iterator();
		}
		DataRow row = (DataRow) rowsIterator.next();
		row.setProperty(BUCKET_MARKER, trashedBucket.hashCode() + "");
		return row;
	}
	
	private InputStream createInputStream(File file) throws FileNotFoundException, IOException {
		return new InflaterInputStream(new FileInputStream(file), new Inflater(), 4096);
	}

	private OutputStream createOutputStream(File file) throws FileNotFoundException, IOException {
		DeflaterOutputStream os = new DeflaterOutputStream(new FileOutputStream(file), new Deflater(Deflater.BEST_SPEED), 4096);
		
		return os;
	}

	public HashingFunction getHashingFunction() {
		return this.blockingFunction;
	}
	
	public void reset() {
		usedFilesFromPool[0] = 0;
		usedFilesFromPool[1] = 0;
		bucketsInMemory.clear();
		bufferBuckets.clear();
		stopped = false;
		completed = false;
		if (thread != null) {
			thread.interrupt();
		}
		thread = new BufferConsumer();
		thread.setName("buckets-manager-thread");
		thread.start();
		
//		if (thread == null) {
//			thread = new BufferConsumer();
//			thread.start();
//		}
	}
	
	protected void finalize() throws Throwable {
		
		cleanup();	
		Log.log(getClass(), "Temporary files with prefix " + filePrefix + " deleted.");
		
		super.finalize();
	}
	
	public long getNumberOfBuckets() {
		return bucktesToFileId.size();
	}
	
	public long getTotalNumberOfComparisons() {
		long comps = 0;
		for (Iterator keys = bucktesToFileId.keySet().iterator(); keys.hasNext();) {
			Bucket bucket = (Bucket) keys.next();
			comps += bucket.getLeftRowsCount() * bucket.getRightRowsCount();
		}
		return comps;
	}

	public int getNumberOfRows() {
		return addedRows ;
	}
}
