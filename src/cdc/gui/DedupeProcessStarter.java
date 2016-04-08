package cdc.gui;

import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.progress.DedupeInfoPanel;
import cdc.gui.components.progress.ProgressDialog;

public class DedupeProcessStarter implements ProcessStarterInterface {

	public void startProcessAndWait(ConfiguredSystem system) {
		ProgressDialog progress = new ProgressDialog(MainFrame.main, "Deduplication progress", false, false);
		DedupeInfoPanel infoPanel = new DedupeInfoPanel(progress);
		DeduplicationThread thread = new DeduplicationThread(system.getSourceA(), infoPanel);
		progress.addCancelListener(new CancelThreadListener(thread));
		progress.setInfoPanel(infoPanel);
		progress.setLocationRelativeTo(MainFrame.main);
		thread.start();
		progress.setVisible(true);
	}

	public void appendSummaryMessage(String msg) {
		
	}
	
}
