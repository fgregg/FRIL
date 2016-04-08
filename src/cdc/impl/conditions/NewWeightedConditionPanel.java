/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.impl.conditions;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class NewWeightedConditionPanel extends AbstractConditionPanel {
	
	private DefaultListModel leftModel = new DefaultListModel();
	private DefaultListModel rightModel = new DefaultListModel();
	private JList leftColList = new JList(leftModel);
	private JList rightColList = new JList(rightModel);
	private JComboBox avaialbleMethods = new JComboBox(GuiUtils.getAvailableDistanceMetrics());
	private JPanel comboSpecificPanel;
	private GUIVisibleComponent componentCreator;
	private AbstractDistance distance;
	private JTextField weight = new JTextField();
	private java.awt.Window parent;
	private GUIVisibleComponent oldCreator;
	
	public NewWeightedConditionPanel(DataColumnDefinition[] leftColumns, DataColumnDefinition[] rightColumns, java.awt.Window parent) {
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.parent = parent;
		
		leftColList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rightColList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		for (int i = 0; i < leftColumns.length; i++) {
			leftModel.addElement(leftColumns[i]);
		}
		for (int i = 0; i < rightColumns.length; i++) {
			rightModel.addElement(rightColumns[i]);
		}
		
		JPanel leftPanel = new JPanel();
		leftPanel.setBorder(BorderFactory.createTitledBorder("Left column"));
		JScrollPane scroll = new JScrollPane(leftColList);
		scroll.setPreferredSize(new Dimension(200, 100));
		leftPanel.add(scroll);
		
		JPanel rightPanel = new JPanel();
		rightPanel.setBorder(BorderFactory.createTitledBorder("Right column"));
		scroll = new JScrollPane(rightColList);
		scroll.setPreferredSize(new Dimension(200, 100));
		rightPanel.add(scroll);
		
		JPanel columnsSelectionPanel = new JPanel(new FlowLayout());
		columnsSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select columns"));
		columnsSelectionPanel.add(leftPanel);
		columnsSelectionPanel.add(rightPanel);
		this.add(columnsSelectionPanel);
		
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
					cachedPanel = (JPanel)componentCreator.getConfigurationPanel(new Object[] {new Boolean(false), NewWeightedConditionPanel.this, NewWeightedConditionPanel.this.parent, leftColList, rightColList, weight}, 400, 170);
					cache.put(componentCreator, cachedPanel);
				}
				comboSpecificPanel.removeAll();
				comboSpecificPanel.add(cachedPanel);
				NewWeightedConditionPanel.this.validate();
				NewWeightedConditionPanel.this.repaint();
				oldCreator = componentCreator;
			}
		});
		
		comboSpecificPanel = new JPanel();
		JScrollPane comboSpecificScroll = new JScrollPane(comboSpecificPanel);
		comboSpecificScroll.setPreferredSize(new Dimension(500, 180));
		
		JPanel methodSelectionPanel = new JPanel();
		methodSelectionPanel.setLayout(new BoxLayout(methodSelectionPanel, BoxLayout.PAGE_AXIS));
		methodSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select distance metric"));
		methodSelectionPanel.add(comboPanel);
		methodSelectionPanel.add(comboSpecificScroll);
		this.add(methodSelectionPanel);
		
		JLabel label = new JLabel("Condition weight: ");
		label.setPreferredSize(new Dimension(120, 20));
		
		JPanel weightsSumPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		weightsSumPanel.setBorder(BorderFactory.createTitledBorder("Select weight"));
		weightsSumPanel.add(label);
		weight.setPreferredSize(new Dimension(40, 20));
		weight.setBorder(BorderFactory.createEtchedBorder());
		weightsSumPanel.add(weight);
		this.add(weightsSumPanel);
		
		avaialbleMethods.setSelectedIndex(0);
		
	}
	
	public ConditionItem getConditionItem() {
		if (distance == null) {
			return null;
		}
		return new ConditionItem((DataColumnDefinition)leftColList.getSelectedValue(), 
				(DataColumnDefinition)rightColList.getSelectedValue(), distance, Integer.parseInt(weight.getText()));
	}

	public void cancelPressed(JDialog parent) {}
	
	public boolean okPressed(JDialog parent) {
		distance = null;
		if (leftColList.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(parent, "Please select left column");
			return false;
		}
		if (rightColList.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(parent, "Please select right column");
			return false;
		}
		
		if (!((GUIVisibleComponent)avaialbleMethods.getSelectedItem()).validate(parent)) {
			return false;
		}
		
		try {
			Integer.parseInt(weight.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(parent, "Weight should be an integer");
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

	public void restoreValues(AbstractDistance abstractDistance, DataColumnDefinition left, DataColumnDefinition right, int weight) {
		for (int i = 0; i < leftModel.getSize(); i++) {
			if (leftModel.get(i).equals(left)) {
				leftColList.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < rightModel.getSize(); i++) {
			if (rightModel.get(i).equals(right)) {
				rightColList.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < avaialbleMethods.getItemCount(); i++) {
			if (((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).getProducedComponentClass().equals(abstractDistance.getClass())) {
				((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).restoreValues(abstractDistance);
				avaialbleMethods.setSelectedIndex(i);
				break;
			}
		}
		this.weight.setText(String.valueOf(weight));
	}
	
}
