package cdc.impl.deduplication.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.ConditionItem;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.distance.EqualFieldsDistance;

/**
 * 
 * @author pjurczy
 *
 */
public class DeduplicationConditionAction extends WizardAction {

	private static final String[] COLUMNS = new String[] {"Distance function", "Attribute", "Weight", "Empty value score"};
	
	private AbstractWizard dialog;
	private AbstractDataSource source;
	private DeduplicationConfig config;
	
	private JLabel sumLabel = new JLabel("0");
	private JTextField acceptLevel = new JTextField(String.valueOf(100));
	
	private JLabel text;

	private TablePanel table;
	private AbstractWizard activeWizard;
	
	private ConditionItem[] prevConditions;
	
	public DeduplicationConditionAction(AbstractDataSource source, DeduplicationConfig config) {
		this.source = source;
		this.config = config;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		dialog = wizard;
		table = new TablePanel(COLUMNS, true, true, TablePanel.BUTTONS_LEFT);
		text = new JLabel();
		activeWizard = wizard;
		table.addEditButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] row = (Object[]) table.getSelectedRows()[0];
				OptionDialog dialog = new OptionDialog(activeWizard, "Deduplication configuration: edit condition");
				DeduplicationConditionPanel panel = new DeduplicationConditionPanel(source, dialog);
				panel.restoreValues((AbstractDistance)row[0], (DataColumnDefinition)row[1], ((Integer)row[2]).intValue(), ((Double)row[3]).doubleValue());
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					ConditionItem cond = panel.getConditionItem();
					table.replaceRow(table.getSelectedRowId()[0], new Object[] {cond.getDistanceFunction(), cond.getLeft(), new Integer(cond.getWeight()), new Double(cond.getEmptyMatchScore())});
				}
				updateWeights();
				computeText();
			}
		});
		table.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DeduplicationConditionPanel panel = new DeduplicationConditionPanel(source, activeWizard);
				OptionDialog dialog = new OptionDialog(activeWizard, "Deduplication configuration: new condition");
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					ConditionItem cond = panel.getConditionItem();
					table.addRow(new Object[] {cond.getDistanceFunction(), cond.getLeft(), new Integer(cond.getWeight()), new Double(cond.getEmptyMatchScore())});
				}
				updateWeights();
				computeText();
			}
		});
		
		if (prevConditions == null) {
			for (int i = 0; i < config.getTestedColumns().length; i++) {
				table.addRow(new Object[] {config.getTestCondition()[i], config.getTestedColumns()[i], new Integer(config.getWeights()[i]), new Double(config.getEmptyMatchScore()[i])});
			}
			acceptLevel.setText(String.valueOf(config.getAcceptanceLevel()));
		} else {
			//Already been here. Restoring what was changed before.
			for (int i = 0; i < prevConditions.length; i++) {
				table.addRow(new Object[] {prevConditions[i].getDistanceFunction(), prevConditions[i].getLeft(), new Integer(prevConditions[i].getWeight()), new Double(prevConditions[i].getEmptyMatchScore())});
			}
		}
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.8;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(table, c);
		
		JPanel weightsSumPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JLabel label = new JLabel("Duplicate acceptance level: ");
		weightsSumPanel.add(label);
		weightsSumPanel.add(acceptLevel);
		weightsSumPanel.add(Box.createRigidArea(new Dimension(20, 20)));
		label = new JLabel("Current sum of weights: ");
		weightsSumPanel.add(label);
		weightsSumPanel.add(sumLabel);
		sumLabel.addPropertyChangeListener("text", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				int sum = Integer.parseInt((String)evt.getNewValue());
				if (sum != 100) {
					((JLabel)evt.getSource()).setForeground(Color.RED);
				} else {
					((JLabel)evt.getSource()).setForeground(Color.BLACK);
				}
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(20, 20, 20 ,40);
		mainPanel.add(weightsSumPanel, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(20, 20, 20 ,20);
		mainPanel.add(text, c);
		
		updateWeights();
		computeText();
		
		return mainPanel;
	}
	
	
	private void computeText() {
		Object[] data = table.getRows();
		String text = "<html>Record that have";
		for (int i = 0; i < data.length; i++) {
			if (((AbstractDistance)((Object[])data[i])[0]) instanceof EqualFieldsDistance) {
				if (i != 0) {
					if (i == data.length - 1) {
						text += " and";
					} else {
						text += ", ";
					}
				}
				text += " the same value of attribute " + ((DataColumnDefinition)((Object[])data[i])[1]).getColumnName();
			} else {
				if (i != 0) {
					if (i == data.length - 1) {
						text += " and";
					} else {
						text += ", ";
					}
				}
				text += " similar value of attribute " + ((DataColumnDefinition)((Object[])data[i])[1]).getColumnName();
			}
		}
		text += " will be considered as a duplicate.</html>";
		this.text.setText(text);
	}
	
	private void updateWeights() {
		ConditionItem[] cond = getDeduplicationCondition();
		int sum = 0;
		for (int i = 0; i < cond.length; i++) {
			sum += cond[i].getWeight();
		}
		sumLabel.setText(String.valueOf(sum));
	}

	public void dispose() {
		this.text = null;
		this.source = null;
		this.config = null;
		this.table = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		prevConditions = getDeduplicationCondition();
		try {
			Integer.parseInt(acceptLevel.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(dialog, "Acceptance level should be an integer value.");
			return false;
		}
		if (Integer.parseInt(sumLabel.getText()) != 100) {
			JOptionPane.showMessageDialog(dialog, "Sum of weights have to be 100.");
			return false;
		}
		if (Integer.parseInt(acceptLevel.getText()) > 100) {
			JOptionPane.showMessageDialog(dialog, "Acceptance level cannot exceed 100.");
			return false;
		}
		if (table.getRows().length == 0) {
			JOptionPane.showMessageDialog(dialog, "At least one condition is required.");
			return false;
		}
		return true;
	}

	public void setSize(int width, int height) {
	}

	public ConditionItem[] getDeduplicationCondition() {
		Object[] rows = table.getRows();
		ConditionItem[] items = new ConditionItem[rows.length];
		for (int i = 0; i < items.length; i++) {
			DataColumnDefinition col = (DataColumnDefinition) ((Object[])rows[i])[1];
			AbstractDistance dst = (AbstractDistance) ((Object[])rows[i])[0];
			items[i] = new ConditionItem(col, col, dst, ((Integer)((Object[])rows[i])[2]).intValue());
			items[i].setEmptyMatchScore(((Double)((Object[])rows[i])[3]).doubleValue());
		}
		return items;
	}

	public int getAcceptanceLevel() {
		return Integer.parseInt(acceptLevel.getText());
	}

}
