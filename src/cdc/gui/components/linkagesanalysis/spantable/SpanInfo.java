package cdc.gui.components.linkagesanalysis.spantable;

public class SpanInfo {
	
	private int row;
	private int col;
	
	private int spanWidth = 1;
	private int spanHeight = 1;
	
	private int rowId = -1;
	
	public SpanInfo(int row, int col) {
		this.row = row;
		this.col = col;
	}
	
	public SpanInfo(int row, int col, int spanWidth, int spanHeight) {
		this.row = row;
		this.col = col;
		if (spanHeight < 1) {
			throw new IllegalArgumentException("Span height has to be > 0");
		}
		if (spanWidth < 1) {
			throw new IllegalArgumentException("Span width has to be > 0");
		}
		this.spanHeight = spanHeight;
		this.spanWidth = spanWidth;
	}
	
	public void setSpanHeight(int spanHeight) {
		if (spanHeight < 1) {
			throw new IllegalArgumentException("Span height has to be > 0");
		}
		this.spanHeight = spanHeight;
	}
	
	public void setSpanWidth(int spanWidth) {
		if (spanWidth < 1) {
			throw new IllegalArgumentException("Span width has to be > 0");
		}
		this.spanWidth = spanWidth;
	}
	
	public int getSpanHeight() {
		return spanHeight;
	}
	
	public int getSpanWidth() {
		return spanWidth;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getCol() {
		return col;
	}
	
	void setRowId(int rowId) {
		this.rowId = rowId;
	}
	
	public int getRowId() {
		return rowId;
	}
}
