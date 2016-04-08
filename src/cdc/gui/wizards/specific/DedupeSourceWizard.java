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

import javax.swing.JComponent;
import javax.swing.JFrame;

import cdc.components.AbstractDataSource;
import cdc.gui.Configs;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.ChooseSourceAction;
import cdc.gui.wizards.specific.actions.ChooseSourceFieldsAction;
import cdc.gui.wizards.specific.actions.DataSourceDeduplication;
import cdc.gui.wizards.specific.actions.DeduplicationResultsLocation;
import cdc.impl.deduplication.DeduplicationConfig;

public class DedupeSourceWizard {
	
	private static String[] steps = new String[] {
		"Data source configuration (step 1 of 4)",
		"Data source fields (step 2 of 4)",
		"Data deduplication (step 3 of 4)",
		"Deduplication results (step 4 of 4)"
	};
	
	private AbstractWizard wizard;
	
	private ChooseSourceAction sourceAction;
	private ChooseSourceFieldsAction sourceFieldsAction;
	private DataSourceDeduplication sourceDeduplication;
	private DeduplicationResultsLocation dedupeLocation;
	
	public DedupeSourceWizard(JFrame parent, JComponent component, String defaultName) {
		
		sourceAction = new ChooseSourceAction(defaultName);
		sourceFieldsAction = new ChooseSourceFieldsAction(-1, sourceAction);
		sourceDeduplication = new DataSourceDeduplication(sourceAction, null, false);
		dedupeLocation = new DeduplicationResultsLocation();
		sourceAction.setDataSource(null);
		
		WizardAction[] actions = new WizardAction[] {
				sourceAction,
				sourceFieldsAction,
				sourceDeduplication,
				dedupeLocation
		};
		
		wizard = new AbstractWizard(parent, actions, steps);
		wizard.setLocationRelativeTo(component);
		wizard.setMinimum(Configs.DFAULT_WIZARD_SIZE);
	}
	
	public int getResult() {
		return wizard.getResult();
	}
	
	public AbstractDataSource getConfiguredDataSource() {
		DeduplicationConfig deduplication = sourceDeduplication.getDeduplicationConfig();
		AbstractDataSource source = sourceAction.getDataSource();
		source.setDeduplicationConfig(deduplication);
		return source;
	}
	
	public void dispose() {
		sourceAction.dispose();
		sourceFieldsAction.dispose();
		sourceAction = null;
		sourceFieldsAction = null;
	}

	public String getResultsLocation() {
		return dedupeLocation.getFileLocation();
	}
}
