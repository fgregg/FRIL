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

import cdc.gui.components.linkagesanalysis.spantable.JSpanTable;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;

public class LinkagesPanel extends JPanel {
	
	private ColorConfig colors = ColorConfig.getDefault();
	
	private int rollOverIndex = -1;
	private JSpanTable table;
	private SpanTableModel model;

	private FirstColumnComponentCreator firstColumnCreator;
	
	private List mouseOverRowListeners = new ArrayList();
	
	public LinkagesPanel(FirstColumnComponentCreator firstColumn, SpanTableModel model) {
		this.firstColumnCreator = firstColumn;
		this.model = model;
		createTable();
	}

	public LinkagesPanel(SpanTableModel tableModel) {
		this.model = tableModel;
		createTable();
	}

	private void createTable() {
		table = new JSpanTable(model) {
			
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component r = super.prepareRenderer(renderer, row, column);
				prepareBackground(r, row, column);
				if ((firstColumnCreator != null && column == 1) || (firstColumnCreator == null && column == 0)) {
					r.setFont(getFont().deriveFont(Font.BOLD).deriveFont(r.getFont().getSize() + 2.0F));
					((JLabel)r).setHorizontalAlignment(JLabel.CENTER);
				}
				if (rollOverIndex != -1) {
					if (model.spannedRowsAndColumns(row, column)[0] == 1) {
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
				Object r1 = model.getValueAt(cell[0], col);
				Object r2 = model.getValueAt(cell[0] + 1, col);
				if (((firstColumnCreator == null && col != 0) || (firstColumnCreator != null && col > 1)) && r1 != null && r2 != null && !r1.equals(r2)) {
					e.setBackground(colors.getDiffColor());
				} else if (row % 4 < 2) {
					e.setBackground(colors.getOddRowColor());
				} else {
					e.setBackground(colors.getEvenRowColor());
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
	
	public void setTableModel(SpanTableModel tableModel, boolean restoreWidth) {
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
	}

	public void setTableModel(SpanTableModel tableModel) {
		setTableModel(tableModel, false);
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
			rId = rId - rId % 2;
			if (rId != rollOverIndex) {
				rollOverIndex = rId;
				table.repaint();
				for (Iterator iterator = mouseOverRowListeners.iterator(); iterator.hasNext();) {
					MouseOverRowListener l = (MouseOverRowListener) iterator.next();
					l.mouseOverRow(rId / 2);
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

}
