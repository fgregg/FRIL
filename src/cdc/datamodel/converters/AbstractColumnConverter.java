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


package cdc.datamodel.converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.SystemComponent;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.utils.Props;
import cdc.utils.RJException;

public abstract class AbstractColumnConverter extends SystemComponent {
	
	private static Map scripts = new HashMap();
	
	public AbstractColumnConverter(Map props) {
		super(props);
	}
	//public abstract void updateName(String newName);
	public abstract DataColumnDefinition[] getExpectedColumns();
	public abstract DataCell[] convert(DataCell[] dataCells) throws RJException;
	public abstract DataColumnDefinition[] getOutputColumns();
	public abstract void saveToXML(Document doc, Element rowModel);
	
	public static synchronized String getDefaultScript(Class class1) throws RuntimeException {
		if (scripts.containsKey(class1.getName())) {
			return (String) scripts.get(class1.getName());
		}
		String scriptFile = Props.getString("script-dir") + File.separator + class1.getName();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
			String line;
			StringBuffer script = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				script.append(line).append("\n");
			}
			scripts.put(class1.getName(), line = script.toString());
			return line;
		} catch (IOException e) {
			throw new RuntimeException("Converter script file not found: " + scriptFile + ". Make sure the configuration of script directory is correct.");
		}
	}
}
