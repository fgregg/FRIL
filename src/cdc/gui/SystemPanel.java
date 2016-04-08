package cdc.gui;

import javax.swing.JPanel;

import cdc.configuration.ConfiguredSystem;
import cdc.utils.RJException;

public abstract class SystemPanel extends JPanel {

	public abstract ConfiguredSystem getSystem();
	public abstract void setSystem(ConfiguredSystem system) throws RJException;
	public abstract void systemSaved();
	public abstract void unloadConfiguration();
	public abstract ProcessPanel getProcessPanel();
	public abstract void setAltered(boolean b);
	public abstract boolean saveIfNeeded();
	public abstract void setViewButtonEnabled(boolean b);
	
	public abstract void reportErrorLeftSource();
	public abstract void reportErrorRightSource();
	public abstract void reportErrorJoinSource();
	public abstract void reportErrorResultSavers();

}