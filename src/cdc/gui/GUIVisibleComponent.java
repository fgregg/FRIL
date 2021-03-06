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


package cdc.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.SystemComponent;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.utils.RJException;

public abstract class GUIVisibleComponent implements DialogListener, ChangedConfigurationListener {
	
	private Map params = new HashMap();
	private List changedConfigurationListeners = new ArrayList();
	
	public void restoreValues(SystemComponent component) {
		if (component.getProperties() != null) {
			params = component.getProperties();
		}
	}

	public String getRestoredParam(String string) {
		return (String) params.get(string);
	}
	
	public void setSize(int x, int y) {
		//System.out.println("Warning: class " + this.getClass() + " does not implement setSize.");
	}
	
	public void configurationPanelClosed() {
	}
	
	public boolean okPressed(JDialog parent) {
		return validate(parent);
	}
	
	public void windowClosing(JDialog parent) {
	}
	
	public void cancelPressed(JDialog parent) {
	}
	
	public void addChangedConfigurationListener(ChangedConfigurationListener listener) {
		changedConfigurationListeners.add(listener);
	}
	
	public void removeChangedConfigurationListener(ChangedConfigurationListener listener) {
		System.out.println("REMOVE LISTENER");
		changedConfigurationListeners.remove(listener);
	}
	
	public void configurationChanged() {
		for (Iterator iterator = changedConfigurationListeners.iterator(); iterator.hasNext();) {
			ChangedConfigurationListener listener = (ChangedConfigurationListener) iterator.next();
			listener.configurationChanged();
		}
	}
	
	public abstract JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY);
	public abstract String toString();
	public abstract Object generateSystemComponent() throws RJException, IOException;
	public abstract Class getProducedComponentClass();
	public abstract boolean validate(JDialog dialog);
	
}
