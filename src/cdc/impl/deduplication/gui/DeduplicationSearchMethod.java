package cdc.impl.deduplication.gui;

import javax.swing.JPanel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.uicomponents.BlockingAttributePanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.ConditionItem;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.impl.join.blocking.BlockingFunctionFactory;

public class DeduplicationSearchMethod extends WizardAction {

	public static final String WARNING_DEDUPE = "The available options depend on distance metric used for selected blocking attribute.";
	
	private DeduplicationConditionAction action;
	private BlockingAttributePanel blockPanel;
	
	private DataColumnDefinition[][] originalHashAttr;
	private String originalHashFunction;
	
	public DeduplicationSearchMethod(DeduplicationConditionAction action, BlockingFunction fnct) {
		this.action = action;
		this.originalHashFunction = BlockingFunctionFactory.encodeBlockingFunction(fnct);
		this.originalHashAttr = fnct.getColumns();
	}

	public JPanel beginStep(AbstractWizard wizard) {
		ConditionItem[] dedupCond = action.getDeduplicationCondition();
		String[] attrLabels = new String[dedupCond.length];
		AbstractDistance[] distances = new AbstractDistance[dedupCond.length];
		for (int i = 0; i < dedupCond.length; i++) {
			attrLabels[i] = dedupCond[i].getLeft().getColumnName();
			distances[i] = dedupCond[i].getDistanceFunction();
		}
		
		blockPanel = new BlockingAttributePanel(attrLabels, distances, WARNING_DEDUPE);
		if (originalHashAttr != null) {
			int id = -1;
			for (int i = 0; i < dedupCond.length; i++) {
				if (dedupCond[i].getLeft().equals(originalHashAttr[0][0])) {
					id = i;
					break;
				}
			}
			
			if (id != -1) {
				blockPanel.setBlockingAttribute(id);
				blockPanel.setBlockingFunction(originalHashFunction);
			}
		}
		
		return blockPanel;
	}

	public void dispose() {
		this.action = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}

	public void setSize(int width, int height) {
	}
	
	public BlockingFunction getHashingFunction() {
		DataColumnDefinition col = action.getDeduplicationCondition()[blockPanel.getBlockingAttributeId()].getLeft();
		return BlockingFunctionFactory.createBlockingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {col, col}}, blockPanel.getBlockingFunction());
	}

}
