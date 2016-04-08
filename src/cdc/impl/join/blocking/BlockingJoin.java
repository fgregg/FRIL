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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.LinkageSummary;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.utils.CPUInfo;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.RJException;

public class BlockingJoin extends AbstractJoin {
	
	public static final String HASHING_FUNCTION_SOUNDEX = "soundex";
	public static final String HASHING_FUNCTION_EQUALITY = "equality";
	
	public static final String BLOCKING_PARAM = "blocking-param";
	public static final String BLOCKING_FUNCTION = "blocking-function";
	
	public static class Wrapper {
		DataRow row;
	}
	
	public class BlockingJoinConnector {

		public AbstractJoinCondition getJoinCondition() {
			return BlockingJoin.this.getJoinCondition();
		}

		public DataColumnDefinition[] getOutColumns() {
			return BlockingJoin.this.getOutColumns();
		}

		public boolean isAnyJoinListenerRegistered() {
			return BlockingJoin.this.isAnyJoinListenerRegistered();
		}

		public void notifyJoined(DataRow rowA, DataRow rowB, DataRow row) throws RJException {
			BlockingJoin.this.notifyJoined(rowA, rowB, row);
		}

		public void notifyNotJoined(DataRow rowA, DataRow rowB) throws RJException {
			BlockingJoin.this.notifyNotJoined(rowA, rowB);
		}

		public boolean isCancelled() {
			return BlockingJoin.this.isCancelled();
		}

		public void notifyTrashingJoined(DataRow dataRow) throws RJException {
			BlockingJoin.this.notifyTrashingJoined(dataRow);
		}

		public void notifyTrashingNotJoined(DataRow dataRow) throws RJException {
			BlockingJoin.this.notifyTrashingNotJoined(dataRow);
		}
		
	}
	
	private boolean closed = false;
	private boolean open = false;
	
	private int[] blockingFactor;
	private HashingFunction blockingFunction;
	private DataColumnDefinition[][] blocks;
	
	private BucketManager buckets;
	
	private boolean initialized = false;
	
	private HashingThread[] hashers;
	private BlockingJoinThread[] threads;
	private ArrayBlockingQueue result = new ArrayBlockingQueue(1000);
	private CountDownLatch latch;
	
	private int readA;
	private int readB;
	private int linked;
	
	public BlockingJoin(AbstractDataSource sourceA, AbstractDataSource sourceB,
			DataColumnDefinition[] outColumns, AbstractJoinCondition condition, Map params) throws RJException, IOException {
		super(fixSource(sourceA, condition.getLeftJoinColumns()), 
				fixSource(sourceB, condition.getRightJoinColumns()), condition, outColumns, params);
		
		int paramId = Integer.parseInt(getProperty(BLOCKING_PARAM));
		String function = getProperty(BLOCKING_FUNCTION);
		
		blockingFactor = new int[] {paramId};
		blocks = new DataColumnDefinition[this.blockingFactor.length][2];
		for (int i = 0; i < blocks.length; i++) {
			blocks[i][0] = condition.getLeftJoinColumns()[this.blockingFactor[i]];
			blocks[i][1] = condition.getRightJoinColumns()[this.blockingFactor[i]];
		}
		
		if (function.startsWith(HASHING_FUNCTION_SOUNDEX)) {
			String paramsStr = function.substring(function.indexOf("(") + 1, function.length()-1);
			blockingFunction = new SoundexHashingFunction(blocks, Integer.parseInt(paramsStr));
		} else if (function.startsWith(HASHING_FUNCTION_EQUALITY)) {
			blockingFunction = new EqualityHashingFunction(blocks);
		} else {
			throw new RuntimeException("Property " + BLOCKING_FUNCTION + " accepts only soundex or equality options.");
		}
		
		this.buckets = new BucketManager(this.blockingFunction);
		
		doOpen();
	}
	
	private static AbstractDataSource fixSource(AbstractDataSource source, DataColumnDefinition[] order) throws IOException, RJException {
		return source;
//		if (source.canSort()) {
//			source.setOrderBy(order);
//			return source;
//		} else {
//			ExternallySortingDataSource sorter = new ExternallySortingDataSource(source.getSourceName(), source, order, new HashMap());
//			return sorter;
//		}
	}
	
	protected void doClose() throws IOException, RJException {
		if (closed) {
			return;
		}
		this.closed = true;
		this.open = false;
		
		if (threads != null) {
			for (int i = 0; i < threads.length; i++) {
				threads[i].stopProcessing();
			}
		}
		
		threads = null;
		hashers = null;
		if (buckets != null) {
			buckets.cleanup();
		}
		
		getSourceA().close();
		getSourceB().close();
	}

	protected DataRow doJoinNext() throws IOException, RJException {

		if (!initialized ) {
			getSourceA().reset();
			getSourceB().reset();
			
			latch = new CountDownLatch(CPUInfo.testNumberOfCPUs());
			hashers = new HashingThread[CPUInfo.testNumberOfCPUs()];
			for (int i = 0; i < hashers.length; i++) {
				hashers[i] = new HashingThread(this, latch, buckets);
				hashers[i].setName("hash-" + i);
				hashers[i].start();
			}
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
			
			updateSrcStats();
			
			if (isCancelled()) {
				buckets.stopProcessing();
			} else {
				buckets.addingCompleted();
			}
			
			for (int i = 0; i < hashers.length; i++) {
				if (hashers[i].getError() != null) {
					throw hashers[i].getError();
				}
			}
			
			if (isCancelled()) {
				if (buckets != null) {
					buckets.cleanup();
				}
				buckets = new BucketManager(this.blockingFunction);
				return null;
			} else {
				initialized = true;
			}
		}
		
		createThreadsIfNeeded();
		
		if (isCancelled()) {
			tearDownThreads();
			return null;
		}
		
		try {
			main: while (true) {
				if (isCancelled()) {
					tearDownThreads();
					return null;
				}
				Object fromQueue = result.poll(100, TimeUnit.MILLISECONDS);
				checkError();
				updateProgress();
				if (fromQueue != null) {
					linked++;
					return ((Wrapper)fromQueue).row;
				} else {
					for (int i = 0; i < threads.length; i++) {
						if (!threads[i].done()) {
							continue main;
						}
					}
					if (result.peek() == null) {
						return null;
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void updateSrcStats() {
		for (int i = 0; i < hashers.length; i++) {
			readA += hashers[i].getReadA();
			readB += hashers[i].getReadB();
		}
	}

	private void checkError() throws RJException {
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getError() != null) {
				throw threads[i].getError();
			}
		}
	}

	private void createThreadsIfNeeded() {
		if (threads == null) {
			BlockingJoinConnector connector = new BlockingJoinConnector();
			threads = new BlockingJoinThread[CPUInfo.testNumberOfCPUs()];
			for (int j = 0; j < threads.length; j++) {
				threads[j] = new BlockingJoinThread(buckets, result, connector);
				threads[j].setName("blocking-joiner-" + j);
				threads[j].start();
			}
		}
	}

	private void tearDownThreads() {
		System.out.println("Need to implement thear down threads!!");
	}

	private void updateProgress() {
		long buckets = this.buckets.getNumberOfBuckets();
		double step = 100 / (double)buckets;
		long bucketsCompleted = 0;
		long completedWithinBucket = 0;
		for (int i = 0; i < threads.length; i++) {
			bucketsCompleted += threads[i].getCompletedBuckets();
			completedWithinBucket += threads[i].getCompletedWithinBucket();
		}
		int initialProgress = (int) (step * bucketsCompleted);
		double withinProgress = (completedWithinBucket / (double)threads.length);
		withinProgress = Math.round(withinProgress * step);
		setProgress(initialProgress + (int)withinProgress);	
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		throw new RuntimeException("Not yet implemented!");
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		if (threads != null) {
			for (int i = 0; i < threads.length; i++) {
				if (threads[i] != null) {
					threads[i].stopProcessing();
				}
			}
		}
		if (!open) {
			doOpen();
		}
		if (deep) {
			buckets.cleanup();
			buckets = new BucketManager(this.blockingFunction);
			initialized = false;
		} else {
			buckets.reset();
		}
		threads = null;
		hashers = null;
		readA = 0;
		readB = 0;
		linked = 0;
	}

	private void doOpen() {
		this.open = true;
		this.closed = false;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new BlockingGUIVisibleComponent();
	}
	
	public String toHTMLString() {
		StringBuilder builder = new StringBuilder();
		builder.append(HTMLUtils.getHTMLHeader());
		builder.append(HTMLUtils.encodeTable(new String[][] {
				{"Search method:", "Clustering search method"}, 
			}));
		builder.append("<br>Blocking configuration:</br>");
		String[][] table = new String[blocks.length + 1][2];
		table[0][0] = "Attribute (" + getSourceA().getSourceName() + ")";
		table[0][1] = "Attribute (" + getSourceB().getSourceName() + ")";
		for (int i = 1; i < table.length; i++) {
			table[i][0] = blocks[i - 1][0].getColumnName();
			table[i][1] = blocks[i - 1][1].getColumnName();
		}
		builder.append(HTMLUtils.encodeTable(table, true));
		builder.append("<br>Attributes mapping and distance function selection:<br>");
		builder.append(HTMLUtils.encodeJoinCondition(getJoinCondition()));
		builder.append("</html>");
		return builder.toString();
	}
	
	protected void finalize() throws Throwable {
		
		Log.log(getClass(), "Finalize. About to clear memory.");
		
		buckets = null;
		threads = null;
		hashers = null;
		
		super.finalize();
	}
	
	public boolean isProgressSupported() {
		return true;
	}
	
	public boolean isConfigurationProgressSupported() {
		return true;
	}
	
	
	public int getConfigurationProgress() {
		try {
			super.setConfigurationProgress((int) (100 * (getSourceA().position() + getSourceB().position()) / (double)(getSourceA().size() + getSourceB().size())));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.getConfigurationProgress();
	}
	
	public LinkageSummary getLinkageSummary() {
		return new LinkageSummary(readA, readB, linked);
	}
	
}
