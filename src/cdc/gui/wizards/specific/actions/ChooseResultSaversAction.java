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


package cdc.gui.wizards.specific.actions;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cdc.components.AbstractResultsSaver;
import cdc.gui.DialogListener;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class ChooseResultSaversAction extends WizardAction {

	private JPanel buffer;
	private DefaultListModel saversModel = new DefaultListModel();
	private JList saversList = new JList(saversModel);
	private AbstractWizard parent;
	
	private class NewSaverPanel extends JPanel implements DialogListener {
		private AbstractResultsSaver saver;
		private JComboBox activeCombo;
		public NewSaverPanel(OptionDialog dialog, AbstractResultsSaver saver) {
			JPanel internalPanel = new JPanel();
			internalPanel.setPreferredSize(new Dimension(350, 100));
			activeCombo = new JComboBox();
			activeCombo.setPreferredSize(new Dimension(350, (int)activeCombo.getPreferredSize().getHeight()));
			GUIVisibleComponent[] comps = GuiUtils.getAvailableSavers();
			for (int i = 0; i < comps.length; i++) {
				activeCombo.addItem(comps[i]);
			}
			activeCombo.addActionListener(new ComboListener(dialog, internalPanel, null));
			int selected = 0;
			if (saver != null) {
				for (int i = 0; i < comps.length; i++) {
					if (comps[i].getProducedComponentClass().equals(saver.getClass())) {
						comps[i].restoreValues(saver);
						selected = i;
						break;
					}
				}
			}
			
			JLabel activeLabel = new JLabel("Result saver type:");
			JPanel comboPanel = new JPanel();
			comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.PAGE_AXIS));
			
			JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			typePanel.add(activeLabel);
			typePanel.add(activeCombo);
			comboPanel.add(typePanel);
			
			JScrollPane scrollPanel = new JScrollPane(internalPanel);
			scrollPanel.setPreferredSize(new Dimension(550, 150));
			comboPanel.add(scrollPanel);
			activeCombo.setSelectedIndex(selected);
			add(comboPanel);
		}
		
		public AbstractResultsSaver getConfiguredSaver() {
			return saver; 
		}

		public void cancelPressed(JDialog parent) {
		}

		public boolean okPressed(JDialog parent) {
			try {
				GUIVisibleComponent compGen = (GUIVisibleComponent) activeCombo.getSelectedItem();
				saver = (AbstractResultsSaver) compGen.generateSystemComponent();
				return true;
			} catch (RJException e) {
				JXErrorDialog.showDialog(parent, "Error creating results saver", e);
			} catch (IOException e) {
				JXErrorDialog.showDialog(parent, "Error creating results saver", e);
			}
			return false;
		}

		public void windowClosing(JDialog parent) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class ButtonEnablerListener implements ListSelectionListener {
		private JButton button;
		public ButtonEnablerListener(JButton button) {
			this.button = button;
		}
		public void valueChanged(ListSelectionEvent e) {
			if (e.getFirstIndex() != -1) {
				button.setEnabled(true);
			} else {
				button.setEnabled(false);
			}
		}
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		wizard.getMainPanel().setLayout(new BoxLayout(wizard.getMainPanel(), BoxLayout.LINE_AXIS));
		parent = wizard;
		if (buffer == null) {			
			
			JButton remOut = new JButton("Remove selected");
			remOut.setPreferredSize(new Dimension(120, 20));
			remOut.setEnabled(false);
			remOut.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Object[] selected = saversList.getSelectedValues();
					saversList.clearSelection();
					for (int i = 0; i < selected.length; i++) {
						saversModel.removeElement(selected[i]);
					}
					((JButton)e.getSource()).setEnabled(false);
				}
			});
			JButton add = new JButton("Create new...");
			add.setPreferredSize(new Dimension(120, 20));
			add.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					OptionDialog dialog = new OptionDialog(parent, "New results saver");
					NewSaverPanel panel = new NewSaverPanel(dialog, null);
					dialog.setMainPanel(panel);
					dialog.setLocationRelativeTo((JButton)e.getSource());
					dialog.addOptionDialogListener(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						saversModel.addElement(panel.getConfiguredSaver());
					}
				}
			});
			JButton edit = new JButton("Edit");
			edit.setPreferredSize(new Dimension(120, 20));
			edit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					OptionDialog dialog = new OptionDialog(parent, "Edit results saver");
					DefaultListModel model = (DefaultListModel) saversList.getModel();
					int selected = saversList.getSelectedIndex();
					NewSaverPanel panel = new NewSaverPanel(dialog, (AbstractResultsSaver) model.elementAt(selected));
					dialog.setMainPanel(panel);
					dialog.setLocationRelativeTo((JButton)e.getSource());
					dialog.addOptionDialogListener(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						model.add(selected, panel.getConfiguredSaver());
						model.remove(selected + 1);
					}
				}
			});
			edit.setEnabled(false);
			
			JPanel panelButtons = new JPanel();
			panelButtons.setLayout(new GridLayout(3, 1, 10, 0));
			panelButtons.setPreferredSize(new Dimension(140, 60));
			panelButtons.add(add);
			panelButtons.add(edit);
			panelButtons.add(remOut);
			JScrollPane listScrollPane = new JScrollPane(saversList);
	        listScrollPane.setPreferredSize(new Dimension(420, 200));
			JPanel savers = new JPanel(new FlowLayout());
			savers.setPreferredSize(new Dimension(620, 250));
			savers.setBorder(BorderFactory.createTitledBorder("Current results savers"));
			savers.add(panelButtons);
			savers.add(listScrollPane);
			saversList.addListSelectionListener(new ButtonEnablerListener(remOut));
			saversList.addListSelectionListener(new ButtonEnablerListener(edit));
			
			buffer = new JPanel();
			buffer.add(savers);
		}
		return buffer;
	}

	public boolean endStep(AbstractWizard wizard) {
		if (saversModel.getSize() == 0) {
			JOptionPane.showMessageDialog(wizard, "At least one result saver is required.");
			return false;
		}
		return true;
	}

	public AbstractResultsSaver[] getResultsSavers() {
		AbstractResultsSaver[] cols = new AbstractResultsSaver[saversModel.size()];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = (AbstractResultsSaver) saversModel.getElementAt(i);
		}
		return cols;
	}
	
	public void setResultSavers(AbstractResultsSaver[] savers) {
		if (savers == null) return;
		saversModel.removeAllElements();
		for (int i = 0; i < savers.length; i++) {
			saversModel.addElement(savers[i]);
		}
	}
	
	public void setSize(int width, int height) {
		new Exception().printStackTrace();
	}

	public void dispose() {
		parent = null;
		saversModel = null;
		saversList = null;
		buffer = null;
	}

}
