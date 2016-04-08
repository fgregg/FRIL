package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.table.TablePanel;
import cdc.utils.CompareFunctionInterface;

public class SortingEditorSingleSource extends AbstractSortingEditor {
	
	private TablePanel sourceColumns;
	private TablePanel sortColumns;

	private boolean showSourceName;
	
	public SortingEditorSingleSource(DataColumnDefinition[][] dataModel, DataColumnDefinition[] currSort, int[] order, boolean showSourceName) {
		setPreferredSize(new Dimension(500, 300));
		this.showSourceName = showSourceName;
		
		JPanel tables = createTables();
		setLayout(new GridBagLayout());
		add(tables, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		
		fillInTables(dataModel, currSort, order);
	}

	private void fillInTables(DataColumnDefinition[][] dataModel, DataColumnDefinition[] sort, int[] order) {
		
		for (int i = 0; i < dataModel[0].length; i++) {
			int id = -1;
			if (sort != null) {
				id = find(dataModel[0][i], sort);
			}
			sourceColumns.addRow(new Object[] {new Boolean(id != -1), new DataColumnWrapper(dataModel[0][i])});
		}
		
		if (sort != null) {
			for (int i = 0; i < sort.length; i++) {
				sortColumns.addRow(new Object[] {new DataColumnWrapper(sort[i]), order[i] == CompareFunctionInterface.ORDER_ASC ? "Asc" : "Desc"});
			}
		}
		
		sourceColumns.getTable().getModel().addTableModelListener(new ModelListener(sourceColumns));
	}

	private int find(DataColumnDefinition col, DataColumnDefinition[] sort) {
		for (int i = 0; i < sort.length; i++) {
			if (sort[i].toString().equals(col.toString())) {
				return i;
			}
		}
		return -1;
	}

	private JPanel createTables() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		//panel.add(new JLabel("Left source attributes", JLabel.CENTER), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,10,0,5), 0, 0));
		//panel.add(new JLabel("Sort attributes"), new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,5,0,5), 0, 0));
		//panel.add(new JLabel("Right source attributes", JLabel.CENTER), new GridBagConstraints(4, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,5,0,10), 0, 0));
		
		JPanel pLeft = new JPanel(new BorderLayout());
		pLeft.add(sourceColumns = new TablePanel(new String[] {"", "Attribute"}, false, false, false), BorderLayout.CENTER);
		pLeft.setBorder(BorderFactory.createTitledBorder("Source attributes"));
		
		JPanel pSort = new JPanel(new BorderLayout());
		pSort.add(sortColumns = new TablePanel(new String[] {"Attribute", "Order"}, false, false, true), BorderLayout.CENTER);
		pSort.setBorder(BorderFactory.createTitledBorder("Sort attributes"));
		
		panel.add(pLeft, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,10,5,5), 0, 0));
		panel.add(pSort, new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,5,5), 0, 0));
		
		sortColumns.setEditableColumns(new int[] {});
		sortColumns.setColumnClasses(new Class[] {Object.class, Object[].class});
		sortColumns.getTable().getColumnModel().getColumn(1).setPreferredWidth(30);
		sortColumns.getTable().getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox(new String[] {"Asc", "Desc"})));
		sortColumns.setEditableColumns(new int[] {1});
		
		sourceColumns.setEditableColumns(new int[] {0});
		sourceColumns.setColumnClasses(new Class[] {Boolean.class, Object.class});
		sourceColumns.getTable().getColumnModel().getColumn(0).setPreferredWidth(10);
		
		return panel;
	}

	public DataColumnDefinition[] getSortColumns() {
		Object[] sortObj = sortColumns.getRows();
		DataColumnDefinition[] sort = new DataColumnDefinition[sortObj.length];
		for (int i = 0; i < sort.length; i++) {
			sort[i] = ((DataColumnWrapper)((Object[])sortObj[i])[0]).col;
		}
		return sort;
	}
	
	public int[] getSortOrder() {
		Object[] sortObj = sortColumns.getRows();
		int[] order = new int[sortObj.length];
		for (int i = 0; i < order.length; i++) {
			order[i] = ((Object[])sortObj[i])[1].equals("Asc") ? 1 : -1;
		}
		return order;
	}

	private class ModelListener implements TableModelListener {
		
		private TablePanel table;
		
		public ModelListener(TablePanel activePanel) {
			this.table = activePanel;
		}
		
		public void tableChanged(TableModelEvent e) {
			int row = e.getFirstRow();
			Object[] rowObj = (Object[])table.getRows()[row];
			addOrRemove((DataColumnWrapper)rowObj[1], ((Boolean)rowObj[0]).booleanValue());
		}

		private void addOrRemove(DataColumnWrapper col, boolean selected) {
			Object[] rows = sortColumns.getRows();
			if (selected) {
				//try to add if not added
				boolean inList = false;
				for (int i = 0; i < rows.length; i++) {
					if (((DataColumnWrapper)((Object[])rows[i])[0]).col.equals(col.col)) {
						inList = true;
						break;
					}
				}
				if (!inList) {
					sortColumns.addRow(new Object[] {col, "Asc"});
				}
			} else {
				//try to remove if in list
				for (int i = 0; i < rows.length; i++) {
					if (((DataColumnWrapper)((Object[])rows[i])[0]).col.equals(col.col)) {
						sortColumns.removeRow(i);
						break;
					}
				}
			}
		}
	}
	
	private class DataColumnWrapper {
		DataColumnDefinition col;
		public DataColumnWrapper(DataColumnDefinition col) {
			this.col = col;
		}
		public String toString() {
			return showSourceName ? col.toString() : col.getColumnName();
		}
	}
	
}
