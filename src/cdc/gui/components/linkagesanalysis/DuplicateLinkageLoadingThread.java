package cdc.gui.components.linkagesanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.Filter;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.comparators.NumberComparator;
import cdc.utils.comparators.StringComparator;

public class DuplicateLinkageLoadingThread extends LoadingThread {
	
	private List data;
	private List toRemove = new ArrayList();
	
	private ViewLinkagesDialog dialog;
	private volatile boolean cancel;
	private volatile boolean reload;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	private int position = 0;
	
	private int maxPage;
	private AtomicInteger move = new AtomicInteger(1);
	private int currentPage = 1;
	
	public DuplicateLinkageLoadingThread(List data, ThreadCreatorInterface interf, ViewLinkagesDialog viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.data = data;
		this.dialog = viewLinkagesDialog;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	public void run() {
		
		dialog.setStatusBarMessage("Linkages deduplication...");
		dialog.setSortOn(sort != null && sort.length != 0);
		dialog.setFilterOn(filter != null && !filter.isEmpty());
		
		setPriority(MIN_PRIORITY);
		
		//give window some time to load nicely
		try {
			sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
				
		synchronized (data) {
			maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
			if (sort != null) {
				CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
				for (int i = 0; i < compareFunctions.length; i++) {
					if (isConfidence(sort[i])) {
						compareFunctions[i] = new NumberComparator(order[i]);
					} else {
						compareFunctions[i] = new StringComparator(order[i]);
					}
				}
				sortData(sort, compareFunctions);
			}
		}
			
		int loaded = 0;
		while (!cancel) {
			try {
				reload = false;
				dialog.clearTable();
				loaded = 0;
				
				synchronized (data) {
					while (!reload && (move.get() == currentPage)) {
						maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
						position = (currentPage - 1) * dialog.getRecordsPerPage(); 
						checkToRemove();
						while (!reload && loaded < dialog.getRecordsPerPage() && data.size() > position) {
							DataRow row = (DataRow) data.get(position++);
							dialog.addLinkage(row);
							loaded++;
						}
						if (reload) {
							break;
						}
						String msg = getStatusMsg(loaded, cancel);
						dialog.setStatusBarMessage(msg);
						try {
							data.wait();
						} catch (InterruptedException e) {
							if (cancel) return;
						}
					}
					maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
				}
				
//				dialog.clearTable();
//				currentPage = move.get();
//				position = (currentPage - 1) * dialog.getRecordsPerPage();
				
//				synchronized (this) {
//					reload = true;
//					loaded = 0;
//					checkToRemove();
//					for (int i = 0; i < dialog.getRecordsPerPage() && position < data.size(); i++) {
//						DataRow row = (DataRow) data.get(position++);
//						//if not the page that should be shown, then skip showing, just read quickly records...
//						dialog.addLinkage(row);
//						loaded++;
//						Thread.sleep(500);
//						if (reload) {
//							continue main;
//						}
//					}
//				}
			} catch (InterruptedException e) {if (cancel) return;}
		}
	}
	
	private void checkToRemove() {
		synchronized (toRemove) {
			for (Iterator iterator = toRemove.iterator(); iterator.hasNext();) {
				DataRow row = (DataRow) iterator.next();
				data.remove(row);
			}
			reload = !toRemove.isEmpty();
			toRemove.clear();
		}
	}

	private void sortData(DataColumnDefinition[] sort, CompareFunctionInterface[] compareFunctions) {
		Collections.sort(data, new SortComparator(sort, compareFunctions));
	}

	private String getStatusMsg(int loaded, boolean cancel) {
		if (cancel) {
			return "Loading linkages was canceled.";
		}
		return loaded + " linkages loaded. Page " + currentPage + " of " + maxPage + ".";
	}
	
	private boolean isConfidence(DataColumnDefinition dataColumnDefinition) {
		return dataColumnDefinition.getColumnName().equals("Confidence");
	}
	
	public void cancelReading() {
		cancel = true;
		interrupt();
	}

	public boolean moveCursorForward() {
		int sched = move.get();
		if (sched + 1 <= maxPage) {
			move.set(sched + 1);
			synchronized (data) {
				data.notifyAll();
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
			synchronized (data) {
				data.notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorToPage(int page) {
		if (page > 0 && page <= maxPage) {
			move.set(page);
			synchronized (data) {
				data.notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}

	public void updateCursor() {
		synchronized (data) {
			reload = true;
			data.notifyAll();
		}
	}

	private class SortComparator implements Comparator {

		private DataColumnDefinition[] sort;
		private CompareFunctionInterface[] compareFunctions;
		
		public SortComparator(DataColumnDefinition[] sort, CompareFunctionInterface[] compareFunctions) {
			this.compareFunctions = compareFunctions;
			this.sort = sort;
		}

		public int compare(Object arg0, Object arg1) {
			DataRow r1 = (DataRow)arg0;
			DataRow r2 = (DataRow)arg1;
			
			for (int i = 0; i < sort.length; i++) {
				DataCell c1 = r1.getData(sort[i]);
				DataCell c2 = r2.getData(sort[i]);
				int cmp = compareFunctions[i].compare(c1, c2);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}
		
	}

	public void removeLinkage(DataRow linkage) {
		synchronized (toRemove) {
			toRemove.add(linkage);
		}
		interrupt();
	}
	
}
