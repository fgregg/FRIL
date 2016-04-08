package cdc.impl.deduplication.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.AbstractConditionPanel.ConditionItem;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.distance.EqualFieldsDistance;

/**
 * 
 * @author pjurczy
 *
 */
public class DeduplicationConditionAction extends WizardAction {

	private static final String[] COLUMNS = new String[] {"Attribute", "Distance function"};
	
	private AbstractDataSource source;
	private DeduplicationConfig config;
	
	private JLabel text;

	private TablePanel table;
	private AbstractWizard activeWizard;
	
	public DeduplicationConditionAction(AbstractDataSource source, DeduplicationConfig config) {
		this.source = source;
		this.config = config;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		table = new TablePanel(COLUMNS, true, true, TablePanel.BUTTONS_LEFT);
		text = new JLabel();
		activeWizard = wizard;
		table.addEditButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] row = (Object[]) table.getSelectedRows()[0];
				OptionDialog dialog = new OptionDialog(activeWizard, "Deduplication configuration: edit condition");
				DeduplicationConditionPanel panel = new DeduplicationConditionPanel(source.getDataModel().getOutputFormat(), dialog);
				panel.restoreValues((AbstractDistance)row[1], (DataColumnDefinition)row[0]);
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					ConditionItem cond = panel.getConditionItem();
					table.replaceRow(table.getSelectedRowId()[0], new Object[] {cond.getLeft(), cond.getDistanceFunction()});
				}
				computeText();
			}
		});
		table.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DeduplicationConditionPanel panel = new DeduplicationConditionPanel(source.getDataModel().getOutputFormat(), activeWizard);
				OptionDialog dialog = new OptionDialog(activeWizard, "Deduplication configuration: new condition");
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					ConditionItem cond = panel.getConditionItem();
					table.addRow(new Object[] {cond.getLeft(), cond.getDistanceFunction()});
				}
				computeText();
			}
		});
		
		for (int i = 0; i < config.getTestedColumns().length; i++) {
			table.addRow(new Object[] {config.getTestedColumns()[i], config.getTestCondition()[i]});
		}
		computeText();
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.8;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(table, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(20, 20, 20 ,20);
		mainPanel.add(text, c);
		return mainPanel;
	}
	
	
	private void computeText() {
		Object[] data = table.getRows();
		String text = "<html>Record that have";
		for (int i = 0; i < data.length; i++) {
			if (((AbstractDistance)((Object[])data[i])[1]) instanceof EqualFieldsDistance) {
				if (i != 0) {
					if (i == data.length - 1) {
						text += " and";
					} else {
						text += ", ";
					}
				}
				text += " the same value of attribute " + ((DataColumnDefinition)((Object[])data[i])[0]).getColumnName();
			} else {
				if (i != 0) {
					if (i == data.length - 1) {
						text += " and";
					} else {
						text += ", ";
					}
				}
				text += " similar value of attribute " + ((DataColumnDefinition)((Object[])data[i])[0]).getColumnName();
			}
		}
		text += " will be considered as a duplicate.</html>";
		this.text.setText(text);
	}

	public void dispose() {
		this.text = null;
		this.source = null;
		this.config = null;
		this.table = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		return table.getRows().length != 0;
	}

	public void setSize(int width, int height) {
	}

	public ConditionItem[] getDeduplicationCondition() {
		Object[] rows = table.getRows();
		ConditionItem[] items = new ConditionItem[rows.length];
		for (int i = 0; i < items.length; i++) {
			DataColumnDefinition col = (DataColumnDefinition) ((Object[])rows[i])[0];
			AbstractDistance dst = (AbstractDistance) ((Object[])rows[i])[1];
			items[i] = new ConditionItem(col, col, dst, 100);
		}
		return items;
	}

}
