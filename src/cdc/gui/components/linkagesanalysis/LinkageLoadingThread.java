package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.datasource.wrappers.SortingDataSource;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.comparators.NumberComparator;
import cdc.utils.comparators.StringComparator;

public class LinkageLoadingThread extends LoadingThread {

	private ViewLinkagesDialog dialog;
	private ThreadCreatorInterface tCInterface;
	private AbstractDataSource results;
	private volatile boolean cancel;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	private int totalRecords = 0;
	private int maxPage;
	private AtomicInteger move = new AtomicInteger(1);
	private int currentPage = 1;

	public LinkageLoadingThread(ThreadCreatorInterface interf, ViewLinkagesDialog viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.tCInterface = interf;
		this.dialog = viewLinkagesDialog;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	private void doStartup() {
		try {
			results = tCInterface.getDataSource(filter);
			while (results.getNextRow() != null) {
				totalRecords++;
			}
			results.reset();
			maxPage = (int) Math.ceil(totalRecords / (double)dialog.getRecordsPerPage());
		} catch (IOException e) {
			JXErrorDialog.showDialog(dialog, "Error reading linkage results", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(dialog, "Error reading linkage results", e);
		}
	}

	public void run() {
		doStartup();
		
		if (results == null) {
			return;
		}
		
		DataRow row = null;
		dialog.setStatusBarMessage("Loading linkages...");
		dialog.setSortOn(sort != null && sort.length != 0);
		dialog.setFilterOn(filter != null && !filter.isEmpty());
		
		setPriority(MIN_PRIORITY);
		
		//give window some time to load nicely
		try {
			sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
				
		try{	
			if (sort != null) {
				CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
				for (int i = 0; i < compareFunctions.length; i++) {
					if (isConfidence(sort[i])) {
						compareFunctions[i] = new NumberComparator(order[i]);
					} else {
						compareFunctions[i] = new StringComparator(order[i]);
					}
				}
				results = new SortingDataSource("results", results, sort, compareFunctions, new HashMap());
			}
			int loaded = 0;
			while (!cancel) {
				row = results.getNextRow();
				if (loaded == dialog.getRecordsPerPage() || row ==  null) {
					while (move.get() == currentPage) {
						String msg = getStatusMsg(loaded, cancel);
						dialog.setStatusBarMessage(msg);
						synchronized (this) {
							try {
								//System.out.println("Move: " + move.get() + "   curr: " + currentPage);
								wait();
							} catch (InterruptedException e) {
								return;
							}
						}
					}
					currentPage = currentPage + 1;
					int go = move.get();
					if (currentPage > go) {
						//need to reset source
						results.reset();
						currentPage = 1;
						row = results.getNextRow();
					}
					loaded = 0;
					dialog.setStatusBarMessage("Loading linkages...");
					dialog.clearTable();
				}
				try {
					//if not the page that should be shown, then skip showing, just read quickly records...
					if (currentPage == move.get() && row != null) {
						dialog.addLinkage(row);
					}
				} catch (InterruptedException e) {return;}
				if (row != null) {
					loaded++;
				}
			}
			
		} catch (IOException e) {
			JXErrorDialog.showDialog(dialog, "Error reading linkage results", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(dialog, "Error reading linkage results", e);
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RJException e) {
					e.printStackTrace();
				}
				results = null;
			}
		}
	}
	
	private boolean isConfidence(DataColumnDefinition dataColumnDefinition) {
		return dataColumnDefinition.getColumnName().equals("Confidence");
	}

	private String getStatusMsg(int loaded, boolean cancel) {
		if (cancel) {
			return "Loading linkages was canceled.";
		}
		return loaded + " linkages loaded. Page " + currentPage + " of " + maxPage + ".";
	}

	public void cancelReading() {
		this.cancel = true;
		interrupt();
	}
	
	public boolean moveCursorForward() {
		int sched = move.get();
		if (sched + 1 <= maxPage) {
			move.set(sched + 1);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorBackward() {
		int sched = move.get();
		if (sched - 1 > 0) {
			move.set(sched - 1);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorToPage(int page) {
		if (page > 0 && page <= maxPage) {
			move.set(page);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}

	public void updateCursor() {
		synchronized (this) {
			notifyAll();
		}
	}
	
}
