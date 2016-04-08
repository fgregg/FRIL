package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;

public class DetailsDialog extends JDialog {
	
	private DataColumnDefinition[][] comparedCols;
	private DataColumnDefinition[][] dataModel;
	
	private JTable compared;
	private JTable left;
	private JTable right;
	
	public DetailsDialog(JDialog parent, DataColumnDefinition[][] comparedColumns, DataColumnDefinition[][] dataModel) {
		super(parent, "Linkage details");
		setSize(500, 300);
		this.comparedCols = comparedColumns;
		this.dataModel = dataModel;
		
		TableModel modelCompared = getComparedModel();
		compared = new JTable(modelCompared) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		TableModel leftModel = getMinusModel();
		left = new JTable(leftModel) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		TableModel rightModel = getMinusModel();
		right = new JTable(rightModel) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		JPanel compPanel = new JPanel(new BorderLayout());
		compPanel.add(new JScrollPane(compared));
		compPanel.setBorder(BorderFactory.createTitledBorder("Compared attributes"));
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(new JScrollPane(left));
		leftPanel.setBorder(BorderFactory.createTitledBorder("Other attributes (" + comparedCols[0][0].getSourceName() + ")"));
		
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(new JScrollPane(right));
		rightPanel.setBorder(BorderFactory.createTitledBorder("Other attributes (" + comparedCols[1][0].getSourceName() + ")"));
		
		setLayout(new GridBagLayout());
		
		add(compPanel, new GridBagConstraints(0, 0, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(leftPanel, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(rightPanel, new GridBagConstraints(1, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
	}

	private DefaultTableModel getMinusModel() {
		return new DefaultTableModel(new String[] {"Attribute name", "Attribute value"}, 0);
	}

	private TableModel getComparedModel() {
		return new DefaultTableModel(new String[] {"Attribute (" + comparedCols[0][0].getSourceName() + ")", "Value", "Attribute (" + comparedCols[1][0].getSourceName() + ")", "Value"}, 0);
	}
	
	public void setDetail(DataRow object) {
		compared.setModel(getComparedModel());
		left.setModel(getMinusModel());
		right.setModel(getMinusModel());
		List leftModel = new ArrayList(Arrays.asList(dataModel[0]));
		List rightModel = new ArrayList(Arrays.asList(dataModel[1]));
		for (int i = 0; i < comparedCols[0].length; i++) {
			DataColumnDefinition col1 = removeColumnFromList(leftModel, comparedCols[0][i]);
			DataColumnDefinition col2 = removeColumnFromList(rightModel, comparedCols[1][i]);
			((DefaultTableModel)compared.getModel()).addRow(new Object[] {col1.getColumnName(), object.getData(col1).getValue(), col2.getColumnName(), object.getData(col2).getValue()});
		}
		for (Iterator iterator = leftModel.iterator(); iterator.hasNext();) {
			DataColumnDefinition col = (DataColumnDefinition) iterator.next();
			((DefaultTableModel)left.getModel()).addRow(new Object[] {col.getColumnName(), object.getData(col).getValue()});
		}
		for (Iterator iterator = rightModel.iterator(); iterator.hasNext();) {
			DataColumnDefinition col = (DataColumnDefinition) iterator.next();
			((DefaultTableModel)right.getModel()).addRow(new Object[] {col.getColumnName(), object.getData(col).getValue()});
		}
	}

//	private List asList(String sourceName, DataRow object) {
//		List l = new ArrayList();
//		DataColumnDefinition[] dataColumnDefinitions = object.getRowModel();
//		for (int i = 0; i < dataColumnDefinitions.length; i++) {
//			if (dataColumnDefinitions[i].getSourceName().equals(sourceName)) {
//				l.add(dataColumnDefinitions[i]);
//			}
//		}
//		return l;
//	}

	private DataColumnDefinition removeColumnFromList(List model, DataColumnDefinition col) {
		for (Iterator iterator = model.iterator(); iterator.hasNext();) {
			DataColumnDefinition tested = (DataColumnDefinition) iterator.next();
			if (tested.equals(col)) {
				model.remove(tested);
				return tested;
			}
		}
		return null;
	}
	
	
}
