package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JDialog;

import cdc.datamodel.DataColumnDefinition;

public abstract class AbstractColumnConfigDialog extends JDialog {

	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 2;
	public static final Dimension PREFERRED_SIZE = new Dimension(100, 20);
	
	
	public AbstractColumnConfigDialog(Window viewLinkagesDialog, String string) {
		super(viewLinkagesDialog, string);
	}
	
	public abstract int getResult();
	public abstract DataColumnDefinition[][] getConfiguredColumns();
	public abstract ColorConfig getColorConfig();

}
