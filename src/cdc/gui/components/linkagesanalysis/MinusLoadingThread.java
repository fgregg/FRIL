package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.comparators.StringComparator;

public class MinusLoadingThread extends LoadingThread {

	private LinkagesWindowPanel dialog;
	private ThreadCreatorInterface tCInterface;
	private volatile boolean cancel = false;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	private int maxPages;
	private int currentPage = 1;
	private AtomicInteger page = new AtomicInteger(1);
	
	public MinusLoadingThread(ThreadCreatorInterface interf, LinkagesWindowPanel viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.dialog = viewLinkagesDialog;
		this.tCInterface = interf;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	public void run() {
		AbstractDataSource src = null;
		try {
			src = createSource();
			
			dialog.setStatusBarMessage("Loading records...");
			dialog.setSortOn(sort != null && sort.length != 0);
			dialog.setFilterOn(filter != null && !filter.isEmpty());
			
			while (!cancel) {
				if (currentPage > page.get()) {
					src.reset();
					currentPage = 1;
				}
				try {
					while (!cancel && currentPage < page.get()) {
						src.getNextRows(dialog.getRecordsPerPage());
						currentPage++;
					}
					int loaded = 0;
					DataRow row;
					dialog.clearTable();
					while (!cancel && loaded != dialog.getRecordsPerPage() && (row = src.getNextRow()) != null) {
						//add only relevant records...
						if (currentPage == page.get()) {
							dialog.addRecord(row);
						}
						loaded++;
					}
					dialog.setStatusBarMessage(getMessage());
					currentPage++;
					synchronized (this) {
						if (currentPage == page.get() + 1) {
							wait();
						}
					}
				} catch (InterruptedException e) {
					
				}
			}
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading file", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading file", e);
		} finally {
			if (src != null) {
				try {
					src.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RJException e) {
					e.printStackTrace();
				}
				src = null;
			}
		}
	}

	private AbstractDataSource createSource() throws IOException, RJException {
		AbstractDataSource src;
		src = tCInterface.getDataSource(filter);
		testNumberOfRecords(src);
		if (sort != null) {
			CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
			for (int i = 0; i < compareFunctions.length; i++) {
				compareFunctions[i] = new StringComparator(order[i]);
			}
			src = new ExternallySortingDataSource("m-", src, sort, compareFunctions, new HashMap());
		}
		return src;
	}
	
	private String getMessage() {
		return "Linkages loaded. Page " + currentPage + " of " + maxPages;
	}

	private void testNumberOfRecords(AbstractDataSource src) throws IOException, RJException {
		int n = 0;
		while (src.getNextRow() != null) {
			n++;
		}
		src.reset();
		maxPages = (int) Math.ceil(n / (double)dialog.getRecordsPerPage());
	}

	public void cancelReading() {
		cancel = true;
		interrupt();
	}

	public boolean moveCursorBackward() {
		synchronized (this) {
			int c = page.get();
			if (c - 1 > 0) {
				page.set(c - 1);
				notifyAll();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean moveCursorForward() {
		synchronized (this) {
			int c = page.get();
			if (c + 1 <= maxPages) {
				page.set(c + 1);
				notifyAll();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean moveCursorToPage(int page) {
		return false;
	}

	public void updateCursor() {
		synchronized (this) {
			notifyAll();
		}
		interrupt();
	}

	public void saveToFile(String fileName, boolean all) {
		try {
			Map props = new HashMap();
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileName);
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			AbstractResultsSaver saver = new CSVFileSaver(props);
			DataColumnDefinition[] model = RowUtils.getModelForSave(dialog.getUsedModel());
			if (all) {
				AbstractDataSource src = createSource();
				DataRow row;
				while ((row = src.getNextRow()) != null) {
					saver.saveRow(RowUtils.buildSubrow(row, model, true));
				}
			} else {
				DataRow[] rows = dialog.getVisibleRows();
				for (int i = 0; i < rows.length; i++) {
					saver.saveRow(RowUtils.buildSubrow(rows[i], model, true));
				}
			}
			saver.flush();
			saver.close();
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error saving data", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error saving data", e);
		}
		
	}

}
