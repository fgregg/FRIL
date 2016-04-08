package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.linkagesanalysis.spantable.JSpanTable;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;
import cdc.utils.CompareFunctionInterface;

public class LinkagesPanel extends JPanel {
	
	private ColorConfig colors = ColorConfig.getDefault();
	
	private int rollOverIndex = -1;
	private JSpanTable table;
	private SpanTableModel model;

	private FirstColumnComponentCreator firstColumnCreator;
	
	private List mouseOverRowListeners = new ArrayList();
	
	private boolean visibleLinkages;
	private boolean strataEnabled;
	
	private AbstractDistance[] usedDistances;
	//private DataColumnDefinition[][] columns;
	
	public LinkagesPanel(FirstColumnComponentCreator firstColumn, SpanTableModel model, boolean visibleLinkages, boolean strataEnabled) {
		this.firstColumnCreator = firstColumn;
		this.model = model;
		this.visibleLinkages = visibleLinkages;
		this.strataEnabled = strataEnabled;
		createTable();
	}

	public LinkagesPanel(SpanTableModel tableModel, boolean visibleLinkages, boolean strataEnabled) {
		this.model = tableModel;
		this.visibleLinkages = visibleLinkages;
		this.strataEnabled = strataEnabled;
		createTable();
	}

	private void createTable() {
		table = new JSpanTable(model) {
			
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component r = super.prepareRenderer(renderer, row, column);
				prepareBackground(r, row, column);
				if (visibleLinkages && ((firstColumnCreator != null && column == 1) || (firstColumnCreator == null && column == 0))) {
					r.setFont(getFont().deriveFont(Font.BOLD).deriveFont(r.getFont().getSize() + 2.0F));
					((JLabel)r).setHorizontalAlignment(JLabel.CENTER);
				}
				if (rollOverIndex != -1) {
					if (model.spannedRowsAndColumns(row, column)[0] == 1 && visibleLinkages) {
						if ((row % 2 != 0 && row == rollOverIndex + 1) || (row % 2 == 0 && row == rollOverIndex)) {
							r.setBackground(colors.getMouseOverColor());
						}
					} else {
						if (rollOverIndex == row) {
							r.setBackground(colors.getMouseOverColor());
						}
					}
				}
				return r;
			}
			
			private void prepareBackground(Component e, int row, int col) {
				int[] cell = model.visibleRowAndColumn(row, 0);
				if (!visibleLinkages) {
					if (row % 2 == 1) {
						e.setBackground(colors.getOddRowColor());
					} else {
						e.setBackground(colors.getEvenRowColor());
					}
				} else {
					Object r1 = model.getValueAt(cell[0], col);
					Object r2 = model.getValueAt(cell[0] + 1, col);
					if (((firstColumnCreator == null && col != 0) || (firstColumnCreator != null && col > 1)) && r1 != null && r2 != null && !theSame(r1, r2, row, col)) {
						e.setBackground(colors.getDiffColor());
					} else if (row % 4 < 2) {
						e.setBackground(colors.getOddRowColor());
					} else {
						e.setBackground(colors.getEvenRowColor());
					}
				}
			}

			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				Component e = super.prepareEditor(editor, row, column);
				//prepareBackground(e, row, column);
				//e.setBackground(colors.getEditorColor());
				return e;
			}
			
			public boolean isCellEditable(int row, int column) {
				return (firstColumnCreator != null && column < 1);
			}
		
		};
		
		table.setSelectionBackground(new Color(255, 255, 255, 0));
		table.setSelectionForeground(Color.BLACK);
		table.getTableHeader().setReorderingAllowed(false);
		RollOverListener lst = new RollOverListener();
		table.addMouseListener(lst);
		table.addMouseMotionListener(lst);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		//table.setIntercellSpacing(new Dimension(0, 0));
		
		table.addMouseMotionListener(new MouseMotionAdapter() {
			
			public void mouseMoved(MouseEvent e) {
				int x = table.rowAtPoint(e.getPoint());
				int y = table.columnAtPoint(e.getPoint());
				int[] cell = model.visibleRowAndColumn(x, y);
				table.editCellAt(cell[0], 0);
			}
			
		});
		
		if (firstColumnCreator != null) {
			table.getColumnModel().getColumn(0).setCellRenderer(new FirstColumnRenderer());
			table.getColumnModel().getColumn(0).setCellEditor(new FirstColumnCellEditor());
		}
		
		setLayout(new BorderLayout());
		add(new JScrollPane(table), BorderLayout.CENTER);
	}
	
	public void setTableModel(SpanTableModel tableModel, boolean restoreWidth, AbstractDistance[] usedDistances, DataColumnDefinition[][] columns) {
		TableColumnModel tcm = table.getColumnModel();
		int[] w = new int[tcm.getColumnCount()];
		for (int i = 0; i < w.length; i++) {
			w[i] = tcm.getColumn(i).getWidth();
		}
		model = tableModel;
		table.setModel(tableModel);
		if (firstColumnCreator != null) {
			table.getColumnModel().getColumn(0).setCellRenderer(new FirstColumnRenderer());
			table.getColumnModel().getColumn(0).setCellEditor(new FirstColumnCellEditor());
		}
		if (restoreWidth) {
			table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
			for (int i = 0; i < table.getColumnCount(); i++) {
				//System.out.println("Setting: " + w[i]);
				table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
			}
		}
		setUsedDistances(usedDistances, columns);
	}

	public void setTableModel(SpanTableModel tableModel, AbstractDistance[] usedDistances, DataColumnDefinition[][] columns) {
		setTableModel(tableModel, false, usedDistances, columns);
	}
	
	public void setColors(ColorConfig colors) {
		this.colors = colors;
	}

	public ColorConfig getColors() {
		return colors;
	}
	
	
	private class FirstColumnRenderer implements TableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return firstColumnCreator.getRenderer();
		}
	}
	
	private class FirstColumnCellEditor implements TableCellEditor {

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			return firstColumnCreator.getEditor(row, model);
		}

		public Object getCellEditorValue() {
			return null;
		}

		public boolean isCellEditable(EventObject anEvent) {
			return true;
		}
		
		public boolean shouldSelectCell(EventObject anEvent) {
			return true;
		}

		public boolean stopCellEditing() {
			return true;
		}
		
		public void removeCellEditorListener(CellEditorListener l) {}
		public void addCellEditorListener(CellEditorListener l) {}
		public void cancelCellEditing() {}
		
	}
	
	private class RollOverListener extends MouseInputAdapter {
		
		public void mouseExited(MouseEvent e) {
			rollOverIndex = -1;
			doCalculations(e);
			table.repaint();
		}
		
		public void mouseMoved(MouseEvent arg0) {
			doCalculations(arg0);
		}

		private void doCalculations(MouseEvent arg0) {
			int rId = table.rowAtPoint(arg0.getPoint());
			if (visibleLinkages) {
				rId = rId - rId % 2;
			}
			if (rId != rollOverIndex) {
				rollOverIndex = rId;
				table.repaint();
				for (Iterator iterator = mouseOverRowListeners.iterator(); iterator.hasNext();) {
					MouseOverRowListener l = (MouseOverRowListener) iterator.next();
					l.mouseOverRow(visibleLinkages ? rId / 2 : rId);
				}
			}
		}
		
	}
	
	public void addMouseOverRowListener(MouseOverRowListener l) {
		mouseOverRowListeners.add(l);
	}
	
	public void removeMouseOverRowListener(MouseOverRowListener l) {
		mouseOverRowListeners.remove(l);
	}

	public int getSelectedLinkage() {
		return rollOverIndex / 2;
	}
	
	private void setUsedDistances(AbstractDistance[] usedDistances, DataColumnDefinition[][] columns) {
		this.usedDistances = usedDistances;
		//this.columns = columns;
	}

	private boolean theSame(Object r1, Object r2, int row, int column) {
		if (usedDistances == null) {
			return r1.equals(r2);
		}
		column -= 1 + (firstColumnCreator != null ? 1 : 0) + (strataEnabled ? 1 : 0);
		AbstractDistance dst = usedDistances[column];
		CompareFunctionInterface compareFnc = dst.getCompareFunction();
		//System.out.println(dst);
		return compareFnc.compare(new DataCell(DataColumnDefinition.TYPE_STRING, r1), new DataCell(DataColumnDefinition.TYPE_STRING, r2)) == 0;
	}
	
}
