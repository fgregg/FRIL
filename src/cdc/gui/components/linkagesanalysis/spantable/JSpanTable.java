package cdc.gui.components.linkagesanalysis.spantable;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class JSpanTable extends JTable {
	
	SpanTableModel data;

	public JSpanTable(SpanTableModel data) {
		super(data);
		this.data = data;
		setUI(new SpanTalbeUI());
		setShowGrid(true);
		setGridColor(Color.LIGHT_GRAY);
		setSelectionModel(new DefaultListSelectionModel() {
			public void addSelectionInterval(int index0, int index1) {
				if (index0 > index1) {
					int tmp = index0;
					index0 = index1;
					index1 = tmp;
				}
				for (int i = index0; i <= index1; i++) {
					int[] rows = getRows(i);
					super.addSelectionInterval(rows[0], rows[1]);
				}
			}
			public void setSelectionInterval(int index0, int index1) {
				if (index0 > index1) {
					int tmp = index0;
					index0 = index1;
					index1 = tmp;
				}
				for (int i = index0; i <= index1; i++) {
					int[] rows = getRows(i);
					super.setSelectionInterval(rows[0], rows[1]);
				}
			}
			public void removeSelectionInterval(int index0, int index1) {
				if (index0 > index1) {
					int tmp = index0;
					index0 = index1;
					index1 = tmp;
				}
				for (int i = index0; i <= index1; i++) {
					int[] rows = getRows(i);
					super.removeSelectionInterval(rows[0], rows[1]);
				}
			}
		});
		
	}

	public Rectangle getCellRect(int row, int column, boolean includeSpacing) {

		if (data == null) {
			return super.getCellRect(row, column, includeSpacing);
		}

		// add widths of all spanned logical cells
		int[] visCell = data.visibleRowAndColumn(row, column);
		int[] spanSize = data.spannedRowsAndColumns(row, column);
		
		Rectangle r1 = super.getCellRect(visCell[0], visCell[1], includeSpacing);
		
		for (int i = 1; i < spanSize[0]; i++) {
			r1.height += getRowHeight(visCell[0] + i);
		}
		
		for (int i = 1; i < spanSize[1]; i++) {
			r1.width += getColumnModel().getColumn(visCell[1] + i).getWidth();
		}
		
		return r1;
	}

	public int columnAtPoint(Point p) {
		int x = super.columnAtPoint(p);
		// -1 is returned by columnAtPoint if the point is not in the table
		if (x < 0)
			return x;
		int y = super.rowAtPoint(p);
		return data.visibleRowAndColumn(y, x)[1];
	}
	
	private int[] getRows(int row) {
		int[] range = new int[] {row, row};
		getRange(row, range);
		return range;
	}
	
	private void getRange(int row, int[] currRange) {
		for (int i = 0; i < getColumnCount(); i++) {
			int[] cell = data.visibleRowAndColumn(row, i);
			int[] range = data.spannedRowsAndColumns(row, i);
			if (cell[0] < row) {
				currRange[0] = cell[0];
				getRange(cell[0], currRange);
			}
			if (cell[0] + range[0] - 1 > currRange[1]) {
				currRange[1] = cell[0] + range[0] - 1;
				getRange(cell[0] + range[0] - 1, currRange);
			}
		}
	}
	
	public void setModel(TableModel dataModel) {
		if (!(dataModel instanceof SpanTableModel)) {
			throw new RuntimeException("JSpanTable has to use SpanTableModel.");
		}
		this.data = (SpanTableModel)dataModel;
		super.setModel(dataModel);
	}

}
