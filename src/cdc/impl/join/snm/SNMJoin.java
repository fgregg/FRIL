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


package cdc.impl.join.snm;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class SNMJoin extends AbstractJoin {

	private static final int DEFAULT_WINDOW_SIZE = 8;
	public static final String PARAM_WINDOW_SIZE = "window";
	
	private class ActivePair {
		public LinkedList buffer;
		public DataRow activeRow;
		public int index = 0;
		public DataColumnDefinition[] bufferAttrJoin;
		public DataColumnDefinition[] activeAttrJoin;
		
		public DataRow getBufferEl() {
			return (DataRow)pair.buffer.get(pair.index);
		}
	}

	
	//int ttt = 0;
	
	private int window = DEFAULT_WINDOW_SIZE;

	private boolean closed = false;

	private DataRow nextA;

	private DataRow nextB;

	/* buffers for the input */
	private LinkedList bufferA = new LinkedList();
	private LinkedList bufferB = new LinkedList();
	
	private LinkedList candidatesA = null;
	private LinkedList candidatesB = null;
	
	private Map joinedData = new HashMap();

	private ActivePair pair = null;

	public SNMJoin(AbstractDataSource sourceA, AbstractDataSource sourceB, DataColumnDefinition outFormat[], AbstractJoinCondition condition, Map params) throws IOException, RJException {
		super(fixSource(sourceA, condition.getLeftJoinColumns()), 
				fixSource(sourceB, condition.getRightJoinColumns()), condition, outFormat, params);
		nextA = getSourceA().getNextRow();
		nextB = getSourceB().getNextRow();
		if (params.get(PARAM_WINDOW_SIZE) != null) {
			this.window = Integer.parseInt((String) params.get(PARAM_WINDOW_SIZE));
		}
		pair = fillInBuffer();
		Log.log(getClass(), "SNM join created, window size = " + window, 1);
	}
	
	public SNMJoin(AbstractDataSource sourceA, 
			AbstractDataSource sourceB, 
			DataColumnDefinition outFormat[], 
			AbstractJoinCondition condition, int windowSize) throws IOException, RJException {
		super(fixSource(sourceA, condition.getLeftJoinColumns()), 
				fixSource(sourceB, condition.getRightJoinColumns()), condition, outFormat, null);
		nextA = getSourceA().getNextRow();
		nextB = getSourceB().getNextRow();
		window = windowSize;
		pair = fillInBuffer();
	}

	private static AbstractDataSource fixSource(AbstractDataSource source, DataColumnDefinition[] order) throws IOException, RJException {
		if (source.canSort()) {
			source.setOrderBy(order);
			return source;
		} else {
			ExternallySortingDataSource sorter = new ExternallySortingDataSource(source.getSourceName(), source, order, null, new HashMap());
			return sorter;
		}
	}

	protected DataRow doJoinNext() throws IOException, RJException {
		while (true) {
			if (pair == null) {
				return null;
			}
			for (int i = pair.index; i < pair.buffer.size(); i++) {
				DataRow row1 = pair.activeRow;
				DataRow row2 = (DataRow) pair.buffer.get(pair.index);
				DataRow rowA, rowB;
				if (row1.getSourceName().equals(getJoinCondition().getLeftJoinColumns()[0].getSourceName())) {
					rowA = row1;
					rowB = row2;
				} else {
					rowA = row2;
					rowB = row1;
				}
				
				DataRow row1Projected = RowUtils.buildSubrow(row1, pair.activeAttrJoin);
				DataRow row2Projected = RowUtils.buildSubrow(row2, pair.bufferAttrJoin);
				EvaluatedCondition eval;
				if ((eval = getJoinCondition().conditionSatisfied(rowA, rowB)).isSatisfied() && !inResults(row1Projected, row2Projected)) {
					
					if (joinedData.containsKey(row1Projected)) {
						List list = (List) joinedData.get(row1Projected);
						list.add(row2Projected);
					} else {
						List list = new ArrayList();
						list.add(row2Projected);
						joinedData.put(row1Projected, list);
					}
					
					pair.index++;
					if (pair.buffer.size() == pair.index) {
						pair = fillInBuffer();
					}
					
					DataRow joined = RowUtils.buildMergedRow(row1, row2, getOutColumns());
					joined.setProperty(PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
					return joined;
				}
				pair.index++;
				if (pair.buffer.size() == pair.index || pair.index > window + 1) {
					//System.out.println("Fill in buffer");
					pair = fillInBuffer();
					i = -1;
				}
				if (pair == null) {
					return null;
				}
			}
		}
	}

	private boolean inResults(DataRow row1, DataRow row2) {
		if (joinedData.containsKey(row1)) {
			List list = (List) joinedData.get(row1);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				DataRow row = (DataRow) iterator.next();
				if (row.equals(row2)) {
					//System.out.println("Row was found!!!");
					return true;
				}
			}
		}
		if (joinedData.containsKey(row2)) {
			List list = (List) joinedData.get(row2);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				DataRow row = (DataRow) iterator.next();
				if (row.equals(row2)) {
					//System.out.println("Row was found!!!");
					return true;
				}
			}
		}
		return false;
	}

	private ActivePair fillInBuffer() throws IOException, RJException {
		
		//initialize if first time
		if (candidatesA == null && candidatesB == null) {
			candidatesA = new LinkedList();
			candidatesB = new LinkedList();
			candidatesA.add(nextA);
			candidatesB.add(nextB);
		}
		
		//check smaller and load buffer
		boolean leftSmaller;
		if (candidatesA.size() != 0 && candidatesB.size() != 0) {
			DataRow left = (DataRow) candidatesA.get(0);
			DataRow right = (DataRow) candidatesB.get(0);
			if (RowUtils.compareRows(left, right, getJoinCondition().getLeftJoinColumns(), getJoinCondition().getRightJoinColumns(), null) < 0) {
				// left is smaller
				leftSmaller = true;
			} else {
				leftSmaller = false;
			}
		} else if (candidatesA.size() != 0) {
			leftSmaller = true;
		} else if (candidatesB.size() != 0) {
			leftSmaller = false;
		} else {
			//end.. no more actives
			return null;
		}
		
		ActivePair pair = new ActivePair();
		//load buffer if needed
		if (leftSmaller) {
			pair.activeRow = (DataRow) candidatesA.removeFirst();
			pair.buffer = bufferB;
			pair.activeAttrJoin = getJoinCondition().getLeftJoinColumns();
			pair.bufferAttrJoin = getJoinCondition().getRightJoinColumns();
			
			loadBuffer(pair, leftSmaller);
			
			if (candidatesA.size() == 0 && nextA != null) {
				bufferA.add(nextA);
				nextA = getSourceA().getNextRow();
				if (nextA != null) candidatesA.add(nextA);
			}
		} else {
			pair.activeRow = (DataRow) candidatesB.removeFirst();
			pair.buffer = bufferA;
			pair.activeAttrJoin = getJoinCondition().getRightJoinColumns();
			pair.bufferAttrJoin = getJoinCondition().getLeftJoinColumns();
			
			loadBuffer(pair, leftSmaller);
			
			if (candidatesB.size() == 0 && nextB != null) {
				bufferB.add(nextB);
				nextB = getSourceB().getNextRow();
				if (nextB != null) candidatesB.add(nextB);
			}
		}
		
		if (pair.buffer.size() == 0) {
			return null;
		}
		
		return pair;
		
	}
	
	private void loadBuffer(ActivePair pair, boolean leftSmaller) throws IOException, RJException {
		if (pair != null) {
			while (pair.buffer.size() != 0 && 
					RowUtils.compareRows((DataRow)pair.buffer.getFirst(), pair.activeRow, pair.bufferAttrJoin, pair.activeAttrJoin, null) < 0) {
				joinedData.remove(pair.buffer.removeFirst());
			}
		}
		
		if (leftSmaller) {
			while (nextB != null && bufferB.size() < window) {
				bufferB.add(nextB);
				nextB = getSourceB().getNextRow();
				if (nextB != null) candidatesB.add(nextB);
			}
		} else {
			while (nextA != null && bufferA.size() < window) {
				bufferA.add(nextA);
				nextA = getSourceA().getNextRow();
				if (nextA != null) candidatesA.add(nextA);
			}
		}
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		List joinResult = new ArrayList();
		DataRow result;
		while (((result = joinNext()) != null) && joinResult.size() < size) {
			joinResult.add(result);
		}
		return (DataRow[]) joinResult.toArray(new DataRow[] {});
	}

	protected void doClose() throws IOException, RJException {
		if (!closed) {
			getSourceA().close();
			getSourceB().close();
			closed = true;
		}
	}

	public String toString() {
		return "SNMJoin(window size " + window + ")";
	}

//	public static AbstractJoin fromXML(AbstractDataSource leftSource, AbstractDataSource rightSource, Element node) throws RJException, IOException {
//		Element joinConditionElement = DOMUtils.getChildElement(node, Configuration.JOIN_CONDITION_TAG);
//		if (joinConditionElement == null) {
//			throw new RJException("Join has to contain configuration of join condition.");
//		}
//		AbstractJoinCondition cond = Configuration.readConditionConfiguration(leftSource, rightSource, joinConditionElement);
//		Map params = new HashMap();
//		Element paramsElement = DOMUtils.getChildElement(node, Configuration.PARAMS_TAG);
//		if (paramsElement != null) {
//			params = Configuration.parseParams(paramsElement);
//		}
//		
//		Element rowModelConfig = DOMUtils.getChildElement(node, Configuration.ROW_MODEL_TAG);
//		if (rowModelConfig == null) {
//			throw new RJException("Join has to contain configuration of output row model (tag " + Configuration.ROW_MODEL_TAG + ").");
//		}
//		DataColumnDefinition[] rowModel = Configuration.readRowModelConfiguration(rowModelConfig, 
//				new AbstractDataSource[] {leftSource, rightSource});
//		
//		return new SNMJoin(leftSource, rightSource, rowModel, cond, params);
//	}

	protected void doReset(boolean deep) throws IOException, RJException {
		getSourceA().reset();
		getSourceB().reset();
		nextA = getSourceA().getNextRow();
		nextB = getSourceB().getNextRow();
		bufferA = new LinkedList();
		bufferB = new LinkedList();
		candidatesA = null;
		candidatesB = null;
		joinedData = new HashMap();
		pair = fillInBuffer();
	}
	
	protected void finalize() throws Throwable {
		System.out.println(getClass() + " finalize");
		close();
	}
}
