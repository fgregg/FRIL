package cdc.impl.deduplication.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.conditions.AbstractConditionPanel;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class DeduplicationConditionPanel extends AbstractConditionPanel {

	private JComboBox avaialbleMethods = new JComboBox(GuiUtils.getAvailableDistanceMetrics());
	private GUIVisibleComponent componentCreator;
	private GUIVisibleComponent oldCreator;
	private Window parent;
	private JPanel comboSpecificPanel;
	private DefaultListModel attributesListModel = new DefaultListModel();
	private JList attributesList = new JList(attributesListModel);
	private AbstractDistance distance;
	
	public DeduplicationConditionPanel(DataColumnDefinition[] availableAttributes, Window parent) {
		
		this.parent = parent;
		
		setLayout(new GridBagLayout());
		
		attributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		for (int i = 0; i < availableAttributes.length; i++) {
			attributesListModel.addElement(availableAttributes[i]);
		}
		attributesList.setSelectedIndex(0);
		JPanel attributesPanel = new JPanel();
		attributesPanel.setBorder(BorderFactory.createTitledBorder("Available column"));
		JScrollPane scroll = new JScrollPane(attributesList);
		scroll.setPreferredSize(new Dimension(400, 100));
		attributesPanel.add(scroll);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		this.add(attributesPanel, c);
		
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboPanel.add(new JLabel("Distance metric"));
		comboPanel.add(avaialbleMethods);
		avaialbleMethods.addActionListener(new ActionListener() {
			private Map cache = new HashMap();
			public void actionPerformed(ActionEvent e) {
				if (oldCreator != null) {
					oldCreator.configurationPanelClosed();
				}
				componentCreator = (GUIVisibleComponent) avaialbleMethods.getSelectedItem();
				JPanel cachedPanel = (JPanel) cache.get(componentCreator);
				if (cachedPanel == null) {
					cachedPanel = (JPanel)componentCreator.getConfigurationPanel(new Object[] {new Boolean(false), DeduplicationConditionPanel.this, DeduplicationConditionPanel.this.parent, attributesList, attributesList, new Integer(100)}, 400, 170);
					cache.put(componentCreator, cachedPanel);
				}
				comboSpecificPanel.removeAll();
				comboSpecificPanel.add(cachedPanel);
				DeduplicationConditionPanel.this.validate();
				DeduplicationConditionPanel.this.repaint();
				oldCreator = componentCreator;
			}
		});
		
		comboSpecificPanel = new JPanel();
		JScrollPane comboSpecificScroll = new JScrollPane(comboSpecificPanel);
		comboSpecificScroll.setPreferredSize(new Dimension(500, 180));
		
		JPanel methodSelectionPanel = new JPanel();
		methodSelectionPanel.setLayout(new GridBagLayout());
		methodSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select distance metric"));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		methodSelectionPanel.add(comboPanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		methodSelectionPanel.add(comboSpecificScroll, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		this.add(methodSelectionPanel, c);
		
	}
	
	public void restoreValues(AbstractDistance distance, DataColumnDefinition attribute) {
		for (int i = 0; i < attributesListModel.getSize(); i++) {
			if (attributesListModel.get(i).equals(attribute)) {
				attributesList.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < avaialbleMethods.getItemCount(); i++) {
			if (((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).getProducedComponentClass().equals(distance.getClass())) {
				((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).restoreValues(distance);
				avaialbleMethods.setSelectedIndex(i);
				break;
			}
		}
	}

	public ConditionItem getConditionItem() {
		if (distance == null) {
			return null;
		}
		return new ConditionItem((DataColumnDefinition)attributesList.getSelectedValue(), (DataColumnDefinition)attributesList.getSelectedValue(), distance, 100);
	}

	public void cancelPressed(JDialog parent) {
	}

	public boolean okPressed(JDialog parent) {
		distance = null;
		if (attributesList.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(parent, "Please select left column");
			return false;
		}
		
		if (!((GUIVisibleComponent)avaialbleMethods.getSelectedItem()).validate(parent)) {
			return false;
		}
		
		try {
			distance = (AbstractDistance) componentCreator.generateSystemComponent();
			return true;
		} catch (RJException e) {
			JXErrorDialog.showDialog(parent, "Error creating distance method", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(parent, "Error creating distance method", e);
		}
		return false;
	}

	public void windowClosing(JDialog parent) {
		// TODO Auto-generated method stub
		
	}

}
