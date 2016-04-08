package cdc.gui;

import cdc.configuration.ConfiguredSystem;

public interface ProcessStarterInterface {
	public void startProcessAndWait(ConfiguredSystem system);
	//public void appendSummaryMessage(String msg);
}
