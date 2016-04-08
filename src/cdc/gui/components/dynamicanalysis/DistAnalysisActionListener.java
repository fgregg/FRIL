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


/**
 * 
 */
package cdc.gui.components.dynamicanalysis;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import cdc.configuration.ConfiguredSystem;
import cdc.gui.MainFrame;
import cdc.impl.conditions.AbstractConditionPanel;

public class DistAnalysisActionListener implements ActionListener {
	
	public static final String[] columns = {"Left source", "Right source", "Match"};
	
	private AbstractConditionPanel panel;
	private Window parent;
	private DistAnalysisRestartListener changeListener;
	private boolean on = false;
	private DynamicAnalysisFrame frame;
	private JButton button;
	
	public DistAnalysisActionListener(Window parent, AbstractConditionPanel panel, DistAnalysisRestartListener changeListener) {
		this.panel = panel;
		this.parent = parent;
		this.changeListener = changeListener;
	}
	
	public void actionPerformed(ActionEvent arg0) {
		on = !on;
		
		if (on) {
			button = (JButton)arg0.getSource();
			button.setEnabled(false);
			ConfiguredSystem system = MainFrame.main.getSystem();
			String error = null;
			try {
				panel.okPressed(null);
			} catch (Exception e) {
				error = e.toString();
			}
			cdc.impl.conditions.AbstractConditionPanel.ConditionItem item = panel.getConditionItem();
			frame = DynamicAnalysis.getDistanceAnalysisFrame(parent);
			if (item != null) {
				frame.setParameters(columns, new Object[] {system.getSourceA(), system.getSourceB(), item.getLeft(), item.getRight(), item.getDistanceFunction()});
			} else if (error == null) {
				error = "Error has occured.";
			}
			frame.addCloseListener(this);
			frame.setVisible(true);
			changeListener.setPanel(panel);
			changeListener.setFrame(frame);
			if (error != null) {
				frame.finished(false);
				frame.setWarningMessage(error);
			}
		} else {
			button.setEnabled(true);
			changeListener.setFrame(null);
			changeListener.setPanel(null);
		}
	}
	
	public void closeWindow() {
		if (frame != null) {
			frame.stop();
			frame.dispose();
		}
		if (button != null) {
			button.setEnabled(true);
		}
		on = false;
	}

	public void reportError(String string) {
		if (frame != null) {
			frame.setWarningMessage(string);
		}
	}
}
