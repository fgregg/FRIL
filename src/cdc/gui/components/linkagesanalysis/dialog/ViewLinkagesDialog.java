package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import cdc.components.AbstractJoin;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.Configs;
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.filtereditor.FilterExpressionEditor;
import cdc.gui.components.linkagesanalysis.LoadingThread;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;
import cdc.gui.components.linkagesanalysis.spantable.SpanInfo;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.Props;
import cdc.utils.RJException;

public class ViewLinkagesDialog extends JDialog {

	private static final String STRATUM_NAME = "Stratum name";
	private static final String CONFIDENCE = "Confidence";
	private static final String VIEW_PREFERENCES = "View preferences";
	private static final String REJECT_ALL_LINKAGES = "Reject all linkages";
	private static final String ACCEPT_ALL_LINKAGES = "Accept all linkages";
	private static final String ANALYZE_MINUS = "Analyze not joined records";
	private static final String LINKAGE_DETAILS = "Show linkage details";
	private static final String SORT_LINKAGES = "Sort linkages";
	private static final String FILTER_LINKAGES = "Filter linkages";
	private static final String CONFIGURE_SORTING = "Configure sorting";
	private static final String CONFIGURE_FITER = "Configure fiter";
	
	private static int RECORDS_PER_PAGE = Props.getInteger("records-per-page", 200);
	
//	private static final String ACCEPT_ALL_QUESTION = "Do you want to accept all available linkages?";
//	private static final String REJECT_ALL_QUESTION = "Do you want to reject all available linkages?";
//	private static final String[] REJECT_ALL_BUTTONS = {"Only visible linkages", "All available linkages"};
//	private static final String[] ACCEPT_ALL_BUTTONS = {"Only visible linkages", "All available linkages"};
	
	private LinkagesPanel linkages;
	private SpanTableModel tableModel;
	
	private DataColumnDefinition[][] comparedColumns;
	private DataColumnDefinition[][] dataModel;
	private DataColumnDefinition confidence;
	private DataColumnDefinition stratum;
	
	private DataColumnDefinition[][] usedModel;
	
	private boolean acceptRejectOption;
	private boolean showSourceName;
	private ThreadCreatorInterface threadCreator;
	private LoadingThread loadingThread;
	
	private Filter filter;
	private DataColumnDefinition[] sortColumns;
	private int[] sortOrder;
	
	private JLabel statusMsg = new JLabel(" ", JLabel.LEFT);
	
	private Object mutex = new Object();
	
	private JLabel sortStatus;
	private JLabel filterStatus;
	
	private JTextField positionField;
	
	private List details = new ArrayList();
	private DetailsDialog detailsWindow;
	
	private List decisionListeners = new ArrayList();
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, ThreadCreatorInterface threadCreator) {
		this(columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, threadCreator, false);
	}
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, ThreadCreatorInterface threadCreator, boolean acceptRejectOption) {
		super(MainFrame.main);
		super.setTitle("Linkage results");
		setModal(true);
		this.threadCreator = threadCreator;
		this.showSourceName = showDataSourceName;
		this.dataModel = columns;
		this.usedModel = comparedColumns;
		this.comparedColumns = comparedColumns;
		this.confidence = confidenceColumn;
		this.stratum = stratum;
		this.acceptRejectOption = acceptRejectOption;
		
		JToolBar toolBar = createToolBar();
		JPanel statusBar = createStatusBar();
		
		tableModel = new SpanTableModel(getDefaultColumns());
		
		if (acceptRejectOption) {
			linkages = new LinkagesPanel(
				new DecisionCellCreator(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						notifyListeners(true);
					}
				}, 
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						notifyListeners(false);
					}
				}), tableModel);
		} else {
			linkages = new LinkagesPanel(tableModel);
		}
		
		//JScrollPane scroll = new JScrollPane(linkages);
		setSize(500, 500);
		setLocationRelativeTo(null);
		
		add(toolBar, BorderLayout.PAGE_START);
		add(linkages, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					return;
				}
				if (loadingThread != null) {
					loadingThread.cancelReading();
				}
			}
			public void windowClosed(WindowEvent e) {
				if (loadingThread != null) {
					loadingThread.cancelReading();
				}
			}
		});
		
		linkages.addMouseOverRowListener(new MouseOverRowListener() {
			public void mouseOverRow(int rowId) {
				if (detailsWindow != null) {
					detailsWindow.setDetail((DataRow) details.get(rowId));
				}
			}
		});
			
		loadingThread = threadCreator.createNewThread(threadCreator, this, null, null, null);
		loadingThread.start();
	}

	private JPanel createStatusBar() {
		JPanel panel = new JPanel(new GridBagLayout());
		JPanel msgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		statusMsg.setVerticalTextPosition(JLabel.CENTER);
		msgPanel.add(statusMsg);
		panel.add(msgPanel, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
		JPanel sortStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		sortStatusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		sortStatusPanel.add(new JLabel("Sort:"));
		sortStatus = new JLabel(Configs.bulbOff);
		sortStatusPanel.add(sortStatus);
		JPanel filterStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		filterStatusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		filterStatusPanel.add(new JLabel("Filter:"));
		filterStatus = new JLabel(Configs.bulbOff);
		filterStatusPanel.add(filterStatus);
		panel.add(sortStatusPanel, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		panel.add(filterStatusPanel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		msgPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		JButton forward = Configs.getForwardButton();
		JButton backward = Configs.getBackwardButton();
		navigation.add(backward);
		navigation.add(positionField = new JTextField("1", 3));
		positionField.setEditable(false);
		positionField.setHorizontalAlignment(JTextField.CENTER);
		positionField.setBorder(BorderFactory.createEmptyBorder());
		navigation.add(forward);
		panel.add(navigation, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		backward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (loadingThread.moveCursorBackward()) {
					positionField.setText(String.valueOf(Integer.parseInt(positionField.getText()) - 1));
				}
			}
		});
		forward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (loadingThread.moveCursorForward()) {
					positionField.setText(String.valueOf(Integer.parseInt(positionField.getText()) + 1));
				}
			}
		});
		
		panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)), new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		return panel;
	}

	private JToolBar createToolBar() {
		JToolBar tb = new JToolBar();
		
		JButton filter = Configs.getFilterButton();
		filter.setMaximumSize(new Dimension(30, 30));
		filter.setToolTipText(FILTER_LINKAGES);
		if (acceptRejectOption) {
			filter.setEnabled(false);
		}
		tb.add(filter);
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog dialog = new OptionDialog(ViewLinkagesDialog.this, CONFIGURE_FITER);
				DataColumnDefinition[] cols = new DataColumnDefinition[dataModel[0].length + dataModel[1].length];
				System.arraycopy(dataModel[0], 0, cols, 0, dataModel[0].length);
				System.arraycopy(dataModel[1], 0, cols, dataModel[0].length, dataModel[1].length);
				FilterExpressionEditor expr = new FilterExpressionEditor(dialog, cols, ViewLinkagesDialog.this.filter);
				expr.setPreferredSize(new Dimension(500, 300));
				expr.setEnabled(true);
				expr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				dialog.setMainPanel(expr);
				dialog.setSize(400, 300);
				while (true) {
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						loadingThread.cancelReading();
						try {
							loadingThread.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						synchronized (mutex) {
							tableModel = new SpanTableModel(getDefaultColumns());
							linkages.setTableModel(tableModel, true);
						}
						Filter f = null;
						try {
							loadingThread = threadCreator.createNewThread(threadCreator, ViewLinkagesDialog.this, f = expr.getFilter(), sortColumns, sortOrder);
							positionField.setText("1");
							loadingThread.start();
						} catch (RJException ex) {
							JXErrorDialog.showDialog(ViewLinkagesDialog.this, "Filter expression error", ex);
						}
						ViewLinkagesDialog.this.filter = f;
					}
					break;
				}
			}
		});
		
		JButton sort = Configs.getSortButton();
		sort.setMaximumSize(new Dimension(30, 30));
		sort.setToolTipText(SORT_LINKAGES);
		tb.add(sort);
		sort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog dialog = new OptionDialog(ViewLinkagesDialog.this, CONFIGURE_SORTING);
				SortingEditor sortPanel = new SortingEditor(dataModel, confidence, comparedColumns, sortColumns, sortOrder, showSourceName);
				dialog.setMainPanel(sortPanel);
				dialog.setSize(400, 300);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					
					loadingThread.cancelReading();
					try {
						loadingThread.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					synchronized (mutex) {
						tableModel = new SpanTableModel(getDefaultColumns());
						linkages.setTableModel(tableModel, true);
					}
					loadingThread = threadCreator.createNewThread(threadCreator, ViewLinkagesDialog.this, ViewLinkagesDialog.this.filter, sortColumns = sortPanel.getSortColumns(), sortOrder = sortPanel.getSortOrder());
					positionField.setText("1");
					loadingThread.start();
					
				}
			}
		});
		
		tb.addSeparator();
		
		JButton details = Configs.getDetailsButton();
		details.setMaximumSize(new Dimension(30, 30));
		details.setToolTipText(LINKAGE_DETAILS);
		tb.add(details);
		details.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (detailsWindow != null) {
					detailsWindow.toFront();
				} else {
					detailsWindow = new DetailsDialog(ViewLinkagesDialog.this, comparedColumns, dataModel);
					detailsWindow.addWindowListener(new WindowAdapter() {
						public void windowClosed(WindowEvent e) {
							detailsWindow = null;
						}
						public void windowClosing(WindowEvent e) {
							detailsWindow = null;
						}
					});
					detailsWindow.setVisible(true);
				}
			}
		});
		
		tb.addSeparator();
		
		if (!acceptRejectOption) {
			JButton minus = Configs.getAnalysisMinusButton();
			minus.setMaximumSize(new Dimension(30, 30));
			minus.setToolTipText(ANALYZE_MINUS);
			tb.add(minus);
			minus.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(ViewLinkagesDialog.this, "FRIL-integrated viewer for not joined records is not yet implemented.\n\nTo see the not joined data, navigate to the disk location\nwhere the results are saved and locate the minus files.");
				}
			});
		} else {
			JButton addAll = new JButton(Configs.scale(Configs.addAllButtonIcon, 30, 30));
			addAll.setPreferredSize(new Dimension(30, 30));
			addAll.setMaximumSize(new Dimension(30, 30));
			addAll.setToolTipText(ACCEPT_ALL_LINKAGES);
			tb.add(addAll);
			addAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//int option = JOptionPane.showOptionDialog(ViewLinkagesDialog.this, ACCEPT_ALL_QUESTION, ACCEPT_ALL_LINKAGES, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ACCEPT_ALL_BUTTONS, ACCEPT_ALL_BUTTONS[0]);
					doLinkages(true);
				}
			});
			
			JButton removeAll = new JButton(Configs.scale(Configs.removeAllButtonIcon, 30, 30));
			removeAll.setPreferredSize(new Dimension(30, 30));
			removeAll.setMaximumSize(new Dimension(30, 30));
			removeAll.setToolTipText(REJECT_ALL_LINKAGES);
			tb.add(removeAll);
			removeAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//int option = JOptionPane.showOptionDialog(ViewLinkagesDialog.this, REJECT_ALL_QUESTION, REJECT_ALL_LINKAGES, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, REJECT_ALL_BUTTONS, REJECT_ALL_BUTTONS[0]);
					doLinkages(false);
				}
			});
		}
		
		tb.addSeparator();
		
		JButton prefs = new JButton(Configs.scale(Configs.configurationButtonIconBig, 30, 30));
		prefs.setPreferredSize(new Dimension(30, 30));
		prefs.setMaximumSize(new Dimension(30, 30));
		prefs.setToolTipText(VIEW_PREFERENCES);
		tb.add(prefs);
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColumnConfigDialog dialog = new ColumnConfigDialog(ViewLinkagesDialog.this, linkages.getColors(), comparedColumns, dataModel, usedModel);
				dialog.setLocationRelativeTo(ViewLinkagesDialog.this);
				if (dialog.getResult() == ColumnConfigDialog.RESULT_OK) {
					loadingThread.cancelReading();
					try {
						loadingThread.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					synchronized (mutex) {
						usedModel = dialog.getConfiguredColumns();
						tableModel = new SpanTableModel(getDefaultColumns());
						linkages.setTableModel(tableModel);
						linkages.setColors(dialog.getColorConfig());
					}
					loadingThread = threadCreator.createNewThread(threadCreator, ViewLinkagesDialog.this, ViewLinkagesDialog.this.filter, sortColumns, sortOrder);
					positionField.setText("1");
					loadingThread.start();
				}
			}
		});
		
		
		return tb;
	}
	
	private Object[] getDefaultColumns() {
		int extraCols = (acceptRejectOption ? 1 : 0) + (stratum != null ? 1 : 0);
		String[] names = new String[usedModel[0].length + 1 + extraCols];
		if (acceptRejectOption) {
			names[0] = "Decision";
		}
		names[acceptRejectOption ? 1 : 0] = CONFIDENCE;
		if (stratum != null) {
			names[extraCols] = STRATUM_NAME;
		}
		
		for (int i = 1; i < names.length - extraCols; i++) {
			if (usedModel[0][i-1] == null) {
				names[i + extraCols] = " --- / " + (showSourceName ? usedModel[1][i-1].toString() : usedModel[1][i-1].getColumnName());
			} else if (usedModel[1][i-1] == null) {
				names[i + extraCols] = (showSourceName ? usedModel[0][i-1].toString() : usedModel[0][i-1].getColumnName()) + " / --- ";
			} else {
				names[i + extraCols] = (showSourceName ? usedModel[0][i-1].toString() : usedModel[0][i-1].getColumnName()) + " / " + 
								(showSourceName ? usedModel[1][i-1].toString() : usedModel[1][i-1].getColumnName());
			}
		}
		return names;
	}
	
	public void addLinkage(DataRow linkage) throws InterruptedException {
		synchronized (mutex) {
			int extraColumns = (acceptRejectOption ? 1 : 0) + (stratum != null ? 1 : 0); 
			String[][] data = new String[2][usedModel[0].length + 1 + extraColumns];
			String conf = linkage.getProperty(AbstractJoin.PROPERTY_CONFIDNCE);
			if (conf == null) {
				conf = linkage.getData(CONFIDENCE).getValue().toString();
			}
			data[0][acceptRejectOption ? 1 : 0] = conf;
			for (int i = 1; i < data[0].length - extraColumns; i++) {
				if (usedModel[0][i - 1] != null) {
					data[0][i + extraColumns] = linkage.getData(usedModel[0][i - 1]).getValue().toString();
				} else {
					data[0][i + extraColumns] = null;
				}
				if (usedModel[1][i - 1] != null) {
					data[1][i + extraColumns] = linkage.getData(usedModel[1][i - 1]).getValue().toString();
				} else {
					data[1][i + extraColumns] = null;
				}
			}
			if (stratum != null) {
				data[0][acceptRejectOption ? 2 : 1] = linkage.getData(stratum).getValue().toString();
			}
			
			details.add(linkage);
			
			Adder doRun;
			if (acceptRejectOption) {
				if (stratum != null) {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2), new SpanInfo(0, 2, 1, 2)});
				} else {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2)});
				}
			} else {
				if (stratum != null) {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2)});
				} else {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2)});
				}
			}
			try {
				SwingUtilities.invokeAndWait(doRun);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				doRun.ignore();
				throw e;
			}
			
		}
		
	}

	public void setStatusBarMessage(String string) {
		statusMsg.setText(string);
	}
	
	public void setFilterOn(boolean filter) {
		filterStatus.setIcon(filter ? Configs.bulbOn : Configs.bulbOff);
		filterStatus.repaint();
	}
	
	public void setSortOn(boolean sort) {
		sortStatus.setIcon(sort ? Configs.bulbOn : Configs.bulbOff);
		sortStatus.repaint();
	}
	
	private class Adder implements Runnable {
		private String[][] data;
		private SpanInfo[] spanInfos;
		private volatile boolean ignore = false;
		public Adder(String[][] data, SpanInfo[] spanInfos) {
			this.data = data;
			this.spanInfos = spanInfos;
		}
		public void run() {
			if (!ignore) {
				tableModel.addSpannedRows(data, spanInfos);
			}
		}
		public void ignore() {
			ignore = true;
		}
	}

	public int getRecordsPerPage() {
		return RECORDS_PER_PAGE;
	}

	public void clearTable() {
		synchronized (mutex) {
			Clearer c = new Clearer();
			try {
				SwingUtilities.invokeAndWait(c);
				details.clear();
			} catch (InterruptedException e) {
				c.ignore = true;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				c.ignore = true;
			}
		}
	}
	
	private class Clearer implements Runnable {

		boolean ignore = false;
		
		public void run() {
			if (!ignore) {
				tableModel = new SpanTableModel(getDefaultColumns());
				linkages.setTableModel(tableModel, true);
			}
		}
		
	}
	
	private void doLinkages(boolean acceptReject) {
		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			notifyAllListeners(acceptReject, row);
		}
		loadingThread.updateCursor();
	}
	
	private void notifyListeners(boolean accepted) {
		int rId = linkages.getSelectedLinkage();
		DataRow linkage = (DataRow) details.remove(rId);
		//SwingUtilities.invokeLater(r);
		tableModel.removeRow(rId * 2);
		notifyAllListeners(accepted, linkage);
		loadingThread.updateCursor();
	}

	private void notifyAllListeners(boolean accepted, DataRow linkage) {
		for (Iterator iterator = decisionListeners.iterator(); iterator.hasNext();) {
			DecisionListener l = (DecisionListener) iterator.next();
			if (accepted) {
				l.linkageAccepted(linkage);
			} else {
				l.linkageRejected(linkage);
			}
		}
	}
	
	public void addDecisionListener(DecisionListener l) {
		decisionListeners.add(l);
	}
	
	public void removeDecisionListener(DecisionListener l) {
		decisionListeners.add(l);
	}
	
	public void removeAllDecisionListeners() {
		decisionListeners.clear();
	}
	
}
