package cdc.gui.components.linkagesanalysis.dialog;

import javax.swing.JPanel;

import cdc.datamodel.DataColumnDefinition;

public abstract class AbstractSortingEditor extends JPanel {

	public abstract DataColumnDefinition[] getSortColumns();
	public abstract int[] getSortOrder();
	
}
