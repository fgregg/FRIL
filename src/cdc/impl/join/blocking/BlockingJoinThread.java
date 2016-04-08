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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import cdc.components.AbstractJoin;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataRow;
import cdc.impl.join.blocking.BlockingJoin.BlockingJoinConnector;
import cdc.impl.join.blocking.BlockingJoin.Wrapper;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class BlockingJoinThread extends Thread {
	
	private BlockingJoinConnector join;
	private BucketManager bucketManager;
	private ArrayBlockingQueue resultsBuffer;
	private DataRow[][] activeBucket;
	private int index1;
	private int index2;
	
	private AtomicLong bucketsCompleted = new AtomicLong(0);
	private AtomicInteger completedWithinBucket = new AtomicInteger(0);
	private long joined = 0;
	
	private volatile RJException error;
	
	private volatile boolean finished;
	private volatile boolean forceFinish = false;
	private int tB = 0;
	private int tA = 0;
	
	public BlockingJoinThread(BucketManager manager, ArrayBlockingQueue resultBuffer, BlockingJoinConnector join) {
		this.join = join;
		this.bucketManager = manager;
		this.resultsBuffer = resultBuffer;
	}
	
	public void run() {
		
		Log.log(getClass(), "Thread " + getName() + " is starting.", 2);
		
		
		try {
			
		main: while (true) {
			
			if (activeBucket == null || (index1 == activeBucket[0].length)) {
				do {
					activeBucket = bucketManager.getBucket();
					bucketsCompleted.incrementAndGet();
					completedWithinBucket.set(0);
					if (activeBucket == null) {
						break main;
					}
					tA += activeBucket[0].length; tB += activeBucket[1].length;
				} while (activeBucket[0].length == 0 || activeBucket[1].length == 0);
				index1 = index2 = 0;
				//System.out.println("Buckets: " + activeBucket[0].length + " <--> " + activeBucket[1].length);
			}
			long completed = 0;
			for (; index1 < activeBucket[0].length; index1++) {
				for (; index2 < activeBucket[1].length; index2++) {
					completed++;
					completedWithinBucket.set((int)(completed / (double)(activeBucket[0].length * activeBucket[1].length)));
					DataRow rowA = activeBucket[0][index1];
					DataRow rowB = activeBucket[1][index2];
					EvaluatedCondition eval;
					if ((eval = join.getJoinCondition().conditionSatisfied(rowA, rowB)).isSatisfied()) {
						DataRow joined = RowUtils.buildMergedRow(rowA, rowB, join.getOutColumns());
						joined.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
						if (join.isAnyJoinListenerRegistered()) {
							rowA.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
							rowB.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
							join.notifyJoined(rowA, rowB);
						}
						rowA.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
						rowB.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
						this.joined++;
						Wrapper w = new Wrapper();
						w.row = joined;
						resultsBuffer.put(w);
					} else {
						if (join.isAnyJoinListenerRegistered()) {
							rowA.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
							rowB.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
							join.notifyNotJoined(rowA, rowB);
						}
					}
					
					if (join.isCancelled() || forceFinish ) {
						break main;
					}
				}
				index2 = 0;
				if (activeBucket[0][index1].getProperty(AbstractJoin.PROPERTY_JOINED) != null) {
					join.notifyTrashingJoined(activeBucket[0][index1]);
				} else {
					join.notifyTrashingNotJoined(activeBucket[0][index1]);
				}
			}
			for (index2=0; index2 < activeBucket[1].length; index2++) {
				if (activeBucket[1][index2].getProperty(AbstractJoin.PROPERTY_JOINED) != null) {
					join.notifyTrashingJoined(activeBucket[1][index2]);
				} else {
					join.notifyTrashingNotJoined(activeBucket[1][index2]);
				}
			}
			
		}
			
		} catch (RJException e) {
			synchronized (this) {
				error = e;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			synchronized (this) {
				error = new RJException("Exception in joining thread", e);
			}
		} catch (Exception e) {
			synchronized (this) {
				error = new RJException("Exception in joining thread", e);
			}
		}
		endSequence();
		synchronized (this) {
			finished = true;
		}
	}

	private void endSequence() {
		Log.log(getClass(), "Thread " + getName() + " finished its job. Joined records: " + joined + "; used buckets: " + bucketsCompleted.get() + " tested records: " + tA + "<->" + tB, 2);
		this.bucketManager = null;
		this.activeBucket = null;
	}

	public long getCompletedBuckets() {
		return bucketsCompleted.get();
	}

	public long getCompletedWithinBucket() {
		return completedWithinBucket.get();
	}
	
	public boolean done() {
		synchronized (this) {
			return finished;
		}
	}
	
	public RJException getError() {
		synchronized (this) {
			return error;
		}
	}

	public void stopProcessing() {
		try {
			interrupt();
			forceFinish = true;
			synchronized (this) {
				while (!finished) {
					this.wait();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}