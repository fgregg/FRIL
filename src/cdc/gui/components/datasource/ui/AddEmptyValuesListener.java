package cdc.gui.components.datasource.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.table.TablePanel;
import cdc.gui.validation.NonEmptyValidator;

public class AddEmptyValuesListener implements ActionListener{

	private MainFrame main;
	private DataColumnDefinition column;
	
	private OptionDialog dialog;
	private TablePanel table;
	
	public AddEmptyValuesListener(MainFrame main, Brick brick) {
		this.main = main;
		this.column = brick.col;
	}

	public void actionPerformed(ActionEvent e) {
	
		table = new TablePanel(new String[] {"Empty values"}, false, true);
		String[] emptyValues = column.getEmptyValues();
		if (emptyValues != null) {
			for (int i = 0; i < emptyValues.length; i++) {
				table.addRow(new Object[] {emptyValues[i]});
			}
		}
		
		table.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog addEmptyValue = new OptionDialog(dialog, "Add empty value");
				ParamsPanel panel = new ParamsPanel(new String[] {"value"}, new String[] {"New empty value"});
				Map validators = new HashMap();
				validators.put("value", new NonEmptyValidator());
				panel.setValidators(validators);
				addEmptyValue.setMainPanel(panel);
				//addEmptyValue.setPreferredSize(new Dimension(300, 200));
				addEmptyValue.pack();
				if (addEmptyValue.getResult() == OptionDialog.RESULT_OK) {
					table.addRow(new Object[] {panel.getParameterValue("value")});
				}
			}
		});
		
		dialog = new OptionDialog(main, "Empty values (attribute " + column.getColumnName() + ")");
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(table, BorderLayout.CENTER);
		dialog.setMainPanel(mainPanel);
		dialog.setPreferredSize(new Dimension(400, 300));
		if (dialog.getResult() == OptionDialog.RESULT_OK) {
			Object[] emptysObj = table.getRows();
			String[] emptysStr = new String[emptysObj.length];
			for (int i = 0; i < emptysStr.length; i++) {
				emptysStr[i] = (String)((Object[])emptysObj[i])[0];
			}
			column.setEmptyValues(emptysStr);
		}
	}

}
