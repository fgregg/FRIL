package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Window;

import javax.swing.JDialog;

import cdc.datamodel.DataRow;

public abstract class AbstractDetailsDialog extends JDialog {
	public AbstractDetailsDialog(Window parent, String string) {
		super(parent, string);
	}

	public abstract void setDetail(DataRow object);
}
