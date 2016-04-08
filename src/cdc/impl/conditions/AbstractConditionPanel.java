package cdc.impl.conditions;

import javax.swing.JPanel;

import cdc.gui.DialogListener;

public abstract class AbstractConditionPanel extends JPanel implements DialogListener  {
	public abstract ConditionItem getConditionItem();
	
}
