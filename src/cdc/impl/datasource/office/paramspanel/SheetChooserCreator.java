package cdc.impl.datasource.office.paramspanel;

import javax.swing.JComponent;

import cdc.gui.components.paramspanel.FieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;

public class SheetChooserCreator implements FieldCreator {

	public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
		return new SheetChooser(param, label, defaultValue);
	}
	
}
