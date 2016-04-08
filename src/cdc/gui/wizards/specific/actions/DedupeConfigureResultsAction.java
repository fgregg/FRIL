package cdc.gui.wizards.specific.actions;

import javax.swing.JPanel;

import cdc.gui.components.uicomponents.FileLocationPanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.utils.StringUtils;

public class DedupeConfigureResultsAction extends WizardAction {

	FileLocationPanel panel;
	
	public JPanel beginStep(AbstractWizard wizard) {
		return panel = new FileLocationPanel("Deduplication results location: ", "deduplicated-source.csv", 20);
	}

	public void dispose() {
	}

	public boolean endStep(AbstractWizard wizard) {
		return !panel.getFileName().isEmpty();
	}

	public void setSize(int width, int height) {
	}

	public String getFileLocation() {
		return panel.getFileName();
	}

	public void setFileLocation(String loc) {
		if (!StringUtils.isNullOrEmpty(loc)) {
			panel.setFileName(loc);
		}
	}

}
