package cdc.impl.deduplication.gui;

import java.awt.Window;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.AbstractConditionPanel.ConditionItem;
import cdc.impl.deduplication.DeduplicationConfig;

public class DeduplicationWizard {

	private static final String[] LABELS = new String[] {
		"Duplicates idenitication condition (step 1 of 2)",
		"Duplicates search method (step 2 of 2)"
	};
	
	private AbstractDataSource source;
	private Window parent;
	
	public DeduplicationWizard(AbstractDataSource source, Window parent) {
		this.source = source;
		this.parent = parent;
	}

	public DeduplicationConfig getDeduplicationConfig(DeduplicationConfig config) {
		
		WizardAction[] actions = new WizardAction[2];
		actions[0] = new DeduplicationConditionAction(source, config);
		actions[1] = new DeduplicationSearchMethod((DeduplicationConditionAction) actions[0], config.getHashingFunction());
		
		AbstractWizard wizard = new AbstractWizard(parent, actions, LABELS);
		if (wizard.getResult() == AbstractWizard.RESULT_OK) {
			ConditionItem[] items = ((DeduplicationConditionAction) actions[0]).getDeduplicationCondition();
			DataColumnDefinition[] cols = new DataColumnDefinition[items.length];
			AbstractDistance[] distances = new AbstractDistance[items.length];
			for (int i = 0; i < items.length; i++) {
				cols[i] = items[i].getLeft();
				distances[i] = items[i].getDistanceFunction();
			}
			config = new DeduplicationConfig(cols, distances);
			config.setHashingConfig(((DeduplicationSearchMethod)actions[1]).getHashingFunction());
		}
		return config;
	}

}
