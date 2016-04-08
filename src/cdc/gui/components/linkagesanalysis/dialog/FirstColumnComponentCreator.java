package cdc.gui.components.linkagesanalysis.dialog;

import javax.swing.JComponent;

import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;

public abstract class FirstColumnComponentCreator {
	
	public abstract JComponent getRenderer();
	public abstract JComponent getEditor(int row, SpanTableModel model);
	
}
