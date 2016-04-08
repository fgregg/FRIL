package cdc.gui.wizards.specific.actions;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.deduplication.gui.DeduplicationWizard;

public class DataSourceDeduplication extends WizardAction {

	private ChooseSourceAction sourceAction;
	private JButton button;
	private DeduplicationConfig config;
	private AbstractWizard activeWizard;
	private JCheckBox dedupOn;
	private AbstractDataSource originalDataSource;
	private boolean enabled = true;
	
	public DataSourceDeduplication(ChooseSourceAction sourceAction, AbstractDataSource originalDataSource) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
	}

	public DataSourceDeduplication(ChooseSourceAction sourceAction, AbstractDataSource originalDataSource, boolean enabled) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
		this.enabled = enabled;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		this.activeWizard = wizard;
		JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		dedupOn = new JCheckBox("Perform de-duplication for the data source");
		button = new JButton("Preferences");
		if (!enabled) {
			dedupOn.setEnabled(false);
			dedupOn.setSelected(true);
			config = new DeduplicationConfig(sourceAction.getDataSource());
		} else {
			if (originalDataSource != null && originalDataSource.getDeduplicationConfig() != null) {
				config = originalDataSource.getDeduplicationConfig();
				config.fixIfNeeded(sourceAction.getDataSource());
				dedupOn.setSelected(true);
				button.setEnabled(true);
			} else {
				config = new DeduplicationConfig(sourceAction.getDataSource());
				dedupOn.setSelected(false);
				button.setEnabled(false);
			}
		}
		dedupOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				button.setEnabled(((JCheckBox)e.getSource()).isSelected());
			}
		});
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractDataSource source = sourceAction.getDataSource();
				DeduplicationWizard wizard = new DeduplicationWizard(source, activeWizard);
				config = wizard.getDeduplicationConfig(config);
			}
		});
		mainPanel.add(dedupOn);
		mainPanel.add(button);
		return mainPanel;
	}

	public void dispose() {
		this.sourceAction = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}

	public void setSize(int width, int height) {
	}

	public DeduplicationConfig getDeduplicationConfig() {
		if (dedupOn.isSelected()) {
			return config;
		} else {
			return null;
		}
	}

}
