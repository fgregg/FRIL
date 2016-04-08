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


package cdc.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.gui.components.datasource.JDataSource;

public class RowUtils {
	
	public static DataRow buildMergedRow(DataRow rowA, DataRow rowB, DataColumnDefinition[] outModel) {
		DataCell[] data = new DataCell[outModel.length];
		for (int i = 0; i < data.length; i++) {
			if (rowA.getSourceName().equals(outModel[i].getSourceName())) {
				data[i] = rowA.getData(outModel[i]);
			} else if (rowB.getSourceName().equals(outModel[i].getSourceName())) {
				data[i] = rowB.getData(outModel[i]);
			} else {
				throw new RuntimeException("Undefined datasource: " + outModel[i].getSourceName());
			}
		}
		DataRow row = new DataRow(outModel, data);
		Map props = new HashMap();
		if (rowA.getProperties() != null) {
			props.putAll(rowA.getProperties());
		}
		row.setProperies(props);
		return row;
	}
	
//	public static boolean areRowsEqual(DataRow rowA, DataRow rowB, JoinCondition cond) {
//		for (int i = 0; i < cond.getLeftJoinColumns().length; i++) {
//			DataCell cellA = rowA.getData(cond.getLeftJoinColumns()[i]);
//			DataCell cellB = rowB.getData(cond.getRightJoinColumns()[i]);
//			if (!cond.getDistances()[i].distanceSatisfied(cellA, cellB)) {
//				return false;
//			}
//		}
//		return true;
//	}
	
	public static int compareRows(DataRow rowA, DataRow rowB, DataColumnDefinition[] sourceAJoinCols, DataColumnDefinition[] sourceBJoinCols) {
		return compareRows(rowA, rowB, sourceAJoinCols, sourceBJoinCols, null);
	}
	
	public static int compareRows(DataRow rowA, DataRow rowB, DataColumnDefinition[] sourceAJoinCols, DataColumnDefinition[] sourceBJoinCols, CompareFunctionInterface[] function) {
		if (function == null) {
			for (int i = 0; i < sourceAJoinCols.length; i++) {
				DataCell cellA = rowA.getData(sourceAJoinCols[i]);
				DataCell cellB = rowB.getData(sourceBJoinCols[i]);
				int cmp = cellA.compareTo(cellB);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else {
			for (int i = 0; i < sourceAJoinCols.length; i++) {
				DataCell cellA = rowA.getData(sourceAJoinCols[i]);
				DataCell cellB = rowB.getData(sourceBJoinCols[i]);
				int cmp = function[i].compare(cellA, cellB);
				if (cmp != 0) {
					return cmp;
				}
			}
		}
		return 0;
	}

	public static DataRow buildSubrow(DataRow row, DataColumnDefinition[] activeAttrJoin) {
		DataCell[] cells = new DataCell[activeAttrJoin.length];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = row.getData(activeAttrJoin[i]);
		}
		return new DataRow(activeAttrJoin, cells, row.getSourceName());
	}

	public static void fixConverter(AbstractColumnConverter conv, JDataSource model, int convId) {
		DataColumnDefinition[] columns = conv.getOutputColumns();
		boolean switched = true;
		while (switched) {
			switched = false;
			for (int i = columns.length - 1; i >= 0; i--) {
				for (int j = columns.length - 1 ; j >= 0; j--) {
					if (i != j && columns[i].getColumnName().equals(columns[j].getColumnName())) {
						switched = true;
						columns[i].setName(columns[i].getColumnName() + "_1");
					}
				}
			}
		}
		
		for (int i = 0; i < columns.length; i++) {
			int n = 1;
			String name = columns[i].getColumnName();
			while (isConflict(conv, name, model, convId)) {
				name = columns[i].getColumnName() + "_" + n;
				n++;
			}
			columns[i].setName(name);
		}
	}
	
	public static void fixConverter(AbstractColumnConverter conv, JDataSource model) {
		fixConverter(conv, model, -1);
	}

	private static boolean isConflict(AbstractColumnConverter tested, String name, JDataSource model, int convId) {
		JDataSource.Connection[] cons = model.getConnections();
		for (int i = 0; i < cons.length; i++) {
			if (i == convId) {
				continue;
			}
			JDataSource.Brick[] alreadyIn = cons[i].to;
			for (int j = 0; j < alreadyIn.length; j++) {
				if (alreadyIn[j].col.getColumnName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static byte[] rowToByteArray(DataRow row, DataColumnDefinition[] rowModel) throws IOException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(array);
		for (int i = 0; i < rowModel.length; i++) {
			DataCell cell = row.getData(rowModel[i]);
			oos.writeObject(cell.getValue());
			oos.writeInt(cell.getValueType());
		}
		if (row.getProperties() != null) {
			oos.writeBoolean(true);
			oos.writeObject(row.getProperties());
		} else {
			oos.writeBoolean(false);
		}
		oos.flush();
		byte[] bytes = array.toByteArray();
		return bytes;
	}
		
	public static DataRow byteArrayToDataRow(byte[] b, DataColumnDefinition[] columns, String sourceName) throws IOException, RJException {
		try {
			ByteArrayInputStream array = new ByteArrayInputStream(b);
			ObjectInputStream ois = new ObjectInputStream(array);
			try {
				DataCell cells[] = new DataCell[columns.length];
				for (int i = 0; i < cells.length; i++) {
					Object val = ois.readObject();
					int type = ois.readInt();
					cells[i] = new DataCell(type, val);
				}
				DataRow row = new DataRow(columns, cells, sourceName);
				if (ois.readBoolean()) {
					row.setProperies((Map) ois.readObject());
				}
				return row;
			} catch (ClassNotFoundException e) {
				throw new RJException("Error reading input file", e);
			}
		} catch (EOFException e) {
			return null;
		}
	}

}
