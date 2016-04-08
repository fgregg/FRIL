package cdc.gui.components.linkagesanalysis;

public abstract class LoadingThread extends Thread {
	
	public abstract boolean moveCursorForward();
	public abstract boolean moveCursorBackward();
	public abstract boolean moveCursorToPage(int page);
	public abstract void updateCursor();
	public abstract void cancelReading();
	
}
