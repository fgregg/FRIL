package cdc.impl.deduplication.gui;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.components.AbstractStringDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.AbstractConditionPanel.ConditionItem;
import cdc.impl.join.blocking.EqualityHashingFunction;
import cdc.impl.join.blocking.HashingFunction;
import cdc.impl.join.blocking.SoundexHashingFunction;

public class DeduplicationSearchMethod extends WizardAction {

	private DeduplicationConditionAction action;
	private JComboBox comboBox;
	private HashingFunction[] hashs;
	private HashingFunction originalHashingFunction;
	
	public DeduplicationSearchMethod(DeduplicationConditionAction action, HashingFunction originalHashingFunction) {
		this.action = action;
		this.originalHashingFunction = originalHashingFunction;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ConditionItem[] dedupCond = action.getDeduplicationCondition();
		comboBox = new JComboBox();
		List list = new ArrayList();
		for (int i = 0; i < dedupCond.length; i++) {
			String label;
			HashingFunction hash;
			if (!(dedupCond[i].getDistanceFunction() instanceof AbstractStringDistance)) {
				label = dedupCond[i].getLeft().getColumnName() + " (equality)";
				hash = new EqualityHashingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {dedupCond[i].getLeft()}, new DataColumnDefinition[] {dedupCond[i].getRight()}});
				comboBox.addItem(label);
				list.add(hash);
			} else {
				label = dedupCond[i].getLeft().getColumnName() + " (equality)";
				hash = new EqualityHashingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {dedupCond[i].getLeft()}, new DataColumnDefinition[] {dedupCond[i].getRight()}});
				comboBox.addItem(label);
				list.add(hash);
				
				label = dedupCond[i].getLeft().getColumnName() + " (soundex, length=5)";
				hash = new SoundexHashingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {dedupCond[i].getLeft()}, new DataColumnDefinition[] {dedupCond[i].getRight()}}, 5);
				comboBox.addItem(label);
				list.add(hash);
				
				label = dedupCond[i].getLeft().getColumnName() + " (soundex, length=6)";
				hash = new SoundexHashingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {dedupCond[i].getLeft()}, new DataColumnDefinition[] {dedupCond[i].getRight()}}, 6);
				comboBox.addItem(label);
				list.add(hash);
			}
		}
		
		hashs = (HashingFunction[]) list.toArray(new HashingFunction[] {});
		
		if (originalHashingFunction != null) {
			for (int i = 0; i < hashs.length; i++) {
				if (hashs[i].equals(originalHashingFunction)) {
					comboBox.setSelectedIndex(i);
					break;
				}
			}
		}
		panel.add(new JLabel("Blocking attribute and method: "));
		panel.add(comboBox);
		
		return panel;
	}

	public void dispose() {
		this.action = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}

	public void setSize(int width, int height) {
	}
	
	public HashingFunction getHashingFunction() {
		return hashs[comboBox.getSelectedIndex()];
	}

}
