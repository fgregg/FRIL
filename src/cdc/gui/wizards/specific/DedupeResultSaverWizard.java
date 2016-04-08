package cdc.gui.wizards.specific;

import javax.swing.JButton;

import cdc.gui.MainFrame;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.DedupeConfigureResultsAction;

public class DedupeResultSaverWizard {

	private DedupeConfigureResultsAction dedupeLocation;
	private AbstractWizard wizard;
	
	public DedupeResultSaverWizard(MainFrame main, JButton saversButton) {
		dedupeLocation = new DedupeConfigureResultsAction();
		wizard = new AbstractWizard(main, new WizardAction[] {dedupeLocation}, new String[] {"Deduplicated data saver (step 1 of 1)"});
		wizard.setLocationRelativeTo(saversButton);
	}

	public void setFileName(String loc) {
		dedupeLocation.setFileLocation(loc);
	}

	public int getResult() {
		return wizard.getResult();
	}

	public String getFileName() {
		return dedupeLocation.getFileLocation();
	}
	
	public void dispose() {
		dedupeLocation.dispose();
		wizard.dispose();
		dedupeLocation = null;
		wizard = null;
	}

}
