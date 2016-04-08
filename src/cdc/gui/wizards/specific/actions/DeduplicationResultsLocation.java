package cdc.gui.wizards.specific.actions;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;

public class DeduplicationResultsLocation extends WizardAction {

	private JButton button;
	private JTextField file;
	
	public JPanel beginStep(AbstractWizard wizard) {
		JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel fileLabel = new JLabel("Deduplication results location: ");
		file = new JTextField("deduplicated-source.csv");
		button = new JButton("...");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(new File("."));				
				int retVal = chooser.showOpenDialog(null);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					file.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		mainPanel.add(fileLabel);
		mainPanel.add(file);
		mainPanel.add(button);
		return mainPanel;
	}

	public void dispose() {
	}

	public boolean endStep(AbstractWizard wizard) {
		return !file.getText().trim().isEmpty();
	}

	public void setSize(int width, int height) {
	}

	public String getFileLocation() {
		return file.getText().trim();
	}

}
