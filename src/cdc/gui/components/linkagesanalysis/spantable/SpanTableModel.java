package cdc.gui.components.linkagesanalysis.spantable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class SpanTableModel extends DefaultTableModel  {

	private List listSpan = new ArrayList();
	
	public SpanTableModel(int rowCnt, int colCnt) {
		super(rowCnt, colCnt);
	}
	
	public SpanTableModel(Object[] colNames, int rows) {
		super(colNames, rows);
	}
	
	public SpanTableModel(Object[] colNames) {
		super(colNames, 0);
	}
	
	public void addSpannedRows(Object[][] rows, SpanInfo[] span) {
		
		synchronized (this) {
			int currRowCnt = getRowCount();
			
			//Add rows
			for (int i = 0; i < rows.length; i++) {
				addRow(rows[i]);
			}
			
			//Calculate the info map data
			for (int i = 0; i < span.length; i++) {
				if (span[i].getRow() + span[i].getSpanHeight() > rows.length) {
					throw new IllegalArgumentException("span[i].getRow() + span[i].getSpanHeight() > rows.length");
				} else if (span[i].getCol() + span[i].getSpanWidth() > rows[span[i].getRow()].length) {
					throw new IllegalArgumentException("span[i].getCol() + span[i].getSpanWidth() > rows[span[i].getRow()].length");
				}
				span[i].setRowId(currRowCnt + span[i].getRow());
				listSpan.add(span[i]);
			}
		}
		
	}

	public int[] visibleRowAndColumn(int row, int col) {
		synchronized (this) {
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo span = (SpanInfo) iterator.next();
				int realRow = span.getRowId();
				if (realRow <= row && realRow + span.getSpanHeight() - 1 >= row && span.getCol() <= col && span.getCol() + span.getSpanWidth() - 1 >= col) {
					return new int[] {realRow, span.getCol()};
				}
			}
			return new int[] {row, col};
		}
	}

	public int[] spannedRowsAndColumns(int row, int col) {
		synchronized (this) {
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo span = (SpanInfo) iterator.next();
				int realRow = span.getRowId();
				if (realRow <= row && realRow + span.getSpanHeight() - 1 >= row && span.getCol() <= col && span.getCol() + span.getSpanWidth() - 1 >= col) {
					return new int[] {span.getSpanHeight(), span.getSpanWidth()};
				}
			}
			return new int[] {1, 1};
		}
	}
	
	public void removeRow(int row) {
		synchronized (this) {	
			//Find the extent of current row
			int[] range = new int[] {row, row};
			boolean change = true;
			while (change) {
				change = false;
				
				int[] rows = visibleRowAndColumn(range[0], 0);
				if (rows[0] < range[0]) {
					range[0] = rows[0];
					change = true;
				}
				
				rows = visibleRowAndColumn(range[1], 0);
				int[] size = spannedRowsAndColumns(range[1], 0);
				if (rows[0] + size[0] - 1 > range[1]) {
					range[1] = rows[0] + size[0] - 1;
					change = true;
				}
			}
			
			//I know the extent. Remove the rows.
			for (int i = 0; i <= range[1] - range[0]; i++) {
				super.removeRow(range[0]);
			}
		
			//Remove the span information
			List toRemove = new ArrayList();
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo info = (SpanInfo) iterator.next();
				if (info.getRowId() >= range[0] && info.getRowId() + info.getSpanHeight() - 1 <= range[1]) {
					toRemove.add(info);
				}
				if (info.getRowId() > range[1]) {
					info.setRowId(info.getRowId() - (range[1] - range[0] + 1));
				}
			}
			listSpan.removeAll(toRemove);
		}
	}

}
