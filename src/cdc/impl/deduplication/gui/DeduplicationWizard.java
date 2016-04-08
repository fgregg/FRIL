package cdc.impl.deduplication.gui;

import java.awt.Window;

import javax.swing.JButton;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.AbstractConditionPanel.ConditionItem;
import cdc.impl.deduplication.DeduplicationConfig;

public class DeduplicationWizard {

	private static final String[] LABELS = new String[] {
		"Duplicates identification condition (step 1 of 2)",
		"Duplicates search method (step 2 of 2)"
	};

	private AbstractWizard wizard;

	private WizardAction[] actions;
	
	public DeduplicationWizard(AbstractDataSource source, DeduplicationConfig configToUse, Window parent, JButton dedupeButton) {
		actions = new WizardAction[2];
		DeduplicationConfig config = configToUse;
		if (config == null) {
			config = source.getDeduplicationConfig();
		}
		if (config == null) {
			config = new DeduplicationConfig(source);
		}
		actions[0] = new DeduplicationConditionAction(source, config);
		actions[1] = new DeduplicationSearchMethod((DeduplicationConditionAction) actions[0], config != null ? config.getHashingFunction() : null);
		wizard = new AbstractWizard(parent, actions, LABELS);
		wizard.setLocationRelativeTo(dedupeButton);
	}

	public int getResult() {
		return wizard.getResult();
	}
	
	
	public DeduplicationConfig getDeduplicationConfig() {
		DeduplicationConfig config = null;
		ConditionItem[] items = ((DeduplicationConditionAction) actions[0]).getDeduplicationCondition();
		DataColumnDefinition[] cols = new DataColumnDefinition[items.length];
		AbstractDistance[] distances = new AbstractDistance[items.length];
		for (int i = 0; i < items.length; i++) {
			cols[i] = items[i].getLeft();
			distances[i] = items[i].getDistanceFunction();
		}
		config = new DeduplicationConfig(cols, distances);
		config.setHashingConfig(((DeduplicationSearchMethod)actions[1]).getHashingFunction());
		return config;
	}

}
