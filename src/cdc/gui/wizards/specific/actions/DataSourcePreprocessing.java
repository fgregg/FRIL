package cdc.gui.wizards.specific.actions;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.gui.components.filtereditor.FilterExpressionEditor;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.deduplication.gui.DeduplicationWizard;
import cdc.utils.RJException;

public class DataSourcePreprocessing extends WizardAction {
	
	private ChooseSourceAction sourceAction;
	private JButton button;
	private DeduplicationConfig config;
	private Filter filterExpression = null;
	private AbstractWizard activeWizard;
	private JCheckBox dedupOn;
	private AbstractDataSource originalDataSource;
	private boolean enabled = true;
	
	private JCheckBox filterOn;
	private FilterExpressionEditor expressionEditor;
	
	public DataSourcePreprocessing(ChooseSourceAction sourceAction, AbstractDataSource originalDataSource) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
	}

	public DataSourcePreprocessing(ChooseSourceAction sourceAction, AbstractDataSource originalDataSource, boolean enabled) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
		this.enabled = enabled;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		this.activeWizard = wizard;
		JPanel dedupePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		dedupePanel.setBorder(BorderFactory.createTitledBorder("Data source deduplication"));
		dedupOn = new JCheckBox("Perform de-duplication for the data source");
		button = new JButton("Preferences");
		button.setPreferredSize(new Dimension(button.getPreferredSize().width, 20));
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
		dedupePanel.add(dedupOn);
		dedupePanel.add(button);
		
		JPanel filterPanel = new JPanel(new GridBagLayout());
		filterPanel.setBorder(BorderFactory.createTitledBorder("Data source filtering"));
		filterOn = new JCheckBox("Use only records that satisfy the following condition:");
		filterOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				expressionEditor.setEnabled(filterOn.isSelected());
			}
		});
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		filterPanel.add(filterOn, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 5, 0, 5);
		filterPanel.add(expressionEditor = new FilterExpressionEditor(activeWizard, originalDataSource == null ? sourceAction.getDataSource() : originalDataSource), c);
		
		JPanel main = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		main.add(dedupePanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		main.add(filterPanel, c);
		
		if (originalDataSource != null && originalDataSource.getFilter() != null) {
			filterOn.setSelected(true);
			expressionEditor.setEnabled(true);
		}
		
		return main;
	}

	public void dispose() {
		this.sourceAction = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		try {
			if (filterOn.isSelected()) {
				this.filterExpression = expressionEditor.getFilter();
			} else {
				this.filterExpression = null;
			}
			return true;
		} catch (RJException e) {
			JXErrorDialog.showDialog(activeWizard, "Error in fiter expression", e);
			return false;
		}
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
	
	public Filter getFilter() {
		return filterExpression;
	}

}
