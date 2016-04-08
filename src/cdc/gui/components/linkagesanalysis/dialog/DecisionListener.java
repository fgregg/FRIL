package cdc.gui.components.linkagesanalysis.dialog;

import cdc.datamodel.DataRow;

public interface DecisionListener {
	
	public void linkageAccepted(DataRow linkage);
	public void linkageRejected(DataRow linkage);
	
}
