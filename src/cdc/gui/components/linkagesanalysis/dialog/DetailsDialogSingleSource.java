package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;

public class DetailsDialogSingleSource extends AbstractDetailsDialog {
	
	private DataColumnDefinition[][] dataModel;
	
	private JTable left;
	
	public DetailsDialogSingleSource(Window parent, DataColumnDefinition[][] dataModel) {
		super(parent, "Record details");
		setSize(500, 300);
		this.dataModel = dataModel;
		
		TableModel leftModel = getMinusModel();
		left = new JTable(leftModel) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(new JScrollPane(left));
		leftPanel.setBorder(BorderFactory.createTitledBorder("Record attributes"));
		
		setLayout(new GridBagLayout());
		
		add(leftPanel, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
	}

	private DefaultTableModel getMinusModel() {
		return new DefaultTableModel(new String[] {"Attribute name", "Attribute value"}, 0);
	}

	public void setDetail(DataRow object) {
		left.setModel(getMinusModel());
		List leftModel = new ArrayList(Arrays.asList(dataModel[0]));
		for (Iterator iterator = leftModel.iterator(); iterator.hasNext();) {
			DataColumnDefinition col = (DataColumnDefinition) iterator.next();
			((DefaultTableModel)left.getModel()).addRow(new Object[] {col.getColumnName(), object.getData(col).getValue()});
		}
	}
	
}
