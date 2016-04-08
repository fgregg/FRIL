package cdc.gui.components.uicomponents;

import java.awt.Dimension;
import java.awt.Window;

import cdc.gui.OptionDialog;

public class ManualReviewConfigDialog extends OptionDialog {
	
	public static final Dimension DEFAULT_SIZE = new Dimension(500, 340);
	
	private ManualReviewConfigPanel panel;
	
	public ManualReviewConfigDialog(Window parent, int acceptance, int manual) {
		super(parent, "Manual review configuration");
		setMainPanel(panel = new ManualReviewConfigPanel(acceptance, manual));
		//setSize(DEFAULT_SIZE);
	}
	
	public int getAcceptanceLevel() {
		return panel.getAcceptance();
	}
	
	public int getManualReviewLevel() {
		return panel.getManulaReview();
	}
}
