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


package cdc.gui.wizards.specific;

import javax.swing.JFrame;

import cdc.configuration.ConfiguredSystem;
import cdc.gui.Configs;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.ChooseResultSaversAction;
import cdc.gui.wizards.specific.actions.ChooseSourceAction;
import cdc.gui.wizards.specific.actions.ChooseSourceFieldsAction;
import cdc.gui.wizards.specific.actions.JoinChooseConditionsAction;
import cdc.gui.wizards.specific.actions.JoinConfigurationAction;
import cdc.gui.wizards.specific.actions.JoinStrataChooser;

public class SystemWizard {
	
	private static String[] steps = new String[] {
		"Left data source configuration (step 1 of 7)",
		"Right data source configuration (step 2 of 7)",
		"Right data source configuration (step 3 of 7)",
		"Right data source configuration (step 4 of 7)",
		"Join conditions and output columns (step 5 of 7)",
		"Join method configuration (step 6 of 7)",
		"Results saving configuration (step 7 of 7)"
	};
	
	private AbstractWizard wizard;
	
	private ChooseSourceAction leftSourceAction;
	private ChooseSourceFieldsAction leftSourceFieldsAction;
	private ChooseSourceAction rightSourceAction;
	private ChooseSourceFieldsAction rightSourceFieldsAction;
	private JoinConfigurationAction joinConfiguration;
	private JoinChooseConditionsAction joinFieldsConfiguration;
	private ChooseResultSaversAction resultSaversActions;
	private JoinStrataChooser joinStratificationConfiguration;
	
	public SystemWizard(JFrame parent) {
		
		leftSourceAction = new ChooseSourceAction("sourceA");
		leftSourceFieldsAction = new ChooseSourceFieldsAction(leftSourceAction);
		rightSourceAction = new ChooseSourceAction("sourceB");
		rightSourceFieldsAction = new ChooseSourceFieldsAction(rightSourceAction);
		joinStratificationConfiguration = new JoinStrataChooser(leftSourceAction, rightSourceAction);
		joinFieldsConfiguration = new JoinChooseConditionsAction(leftSourceAction, rightSourceAction, joinStratificationConfiguration);
		joinConfiguration = new JoinConfigurationAction(leftSourceAction, rightSourceAction, joinStratificationConfiguration, joinFieldsConfiguration);
		resultSaversActions = new ChooseResultSaversAction();
		
		WizardAction[] actions = new WizardAction[] {
				leftSourceAction,
				leftSourceFieldsAction,
				rightSourceAction,
				rightSourceFieldsAction,
				joinFieldsConfiguration,
				joinConfiguration,
				resultSaversActions,
		};
		
		wizard = new AbstractWizard(parent, actions, steps);
		wizard.setLocationRelativeTo(parent);
		wizard.setMinimum(Configs.DFAULT_WIZARD_SIZE);
	}
	
	public int getResult() {
		return wizard.getResult();
	}
	
	public ConfiguredSystem getConfiguredSystem() {
		return new ConfiguredSystem(leftSourceAction.getDataSource(), rightSourceAction.getDataSource(), joinConfiguration.getJoin(), resultSaversActions.getResultsSavers());
	}
	
	public void dispose() {
		leftSourceAction.dispose();
		leftSourceFieldsAction.dispose();
		rightSourceAction.dispose();
		rightSourceFieldsAction.dispose();
		joinConfiguration.dispose();
		joinFieldsConfiguration.dispose();
		resultSaversActions.dispose();
		joinStratificationConfiguration.dispose();
		
		leftSourceAction = null;
		leftSourceFieldsAction = null;
		rightSourceAction = null;
		rightSourceFieldsAction = null;
		joinConfiguration = null;
		joinFieldsConfiguration = null;
		resultSaversActions = null;
		joinStratificationConfiguration = null;
	}
	
}
