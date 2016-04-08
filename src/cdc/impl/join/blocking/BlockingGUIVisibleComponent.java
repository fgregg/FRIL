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


package cdc.impl.join.blocking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.ComboBoxPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.utils.RJException;

public class BlockingGUIVisibleComponent extends GUIVisibleComponent {

	protected static final String BLOCKING_FUNCTION = "blocking-function";
	protected static final String BLOCKING_ATTR = "blocking-attr";
	
	public static final String functions[] = new String[] {
			"Equality metric",
			"Soundex, length = 4",
			"Soundex, length = 5",
			"Soundex, length = 6",
			"Soundex, length = 7"
		};
	public static final String functionsEncoded[] = new String[] {
		"equality",
		"soundex(4)",
		"soundex(5)",
		"soundex(6)",
		"soundex(7)"
	};
	
	protected AbstractDataSource sourceA;
	protected AbstractDataSource sourceB;
	protected AbstractJoinCondition joinCondition;
	protected DataColumnDefinition[] outModel;
	protected String[] hashAttrs;
	
	protected ParamsPanel buffer;
	
	public Object generateSystemComponent() throws RJException, IOException {
		
		Map params = buffer.getParams();
		String attribute = (String) params.get(BLOCKING_ATTR);
		String function = (String) params.get(BLOCKING_FUNCTION);
		Map properties = new HashMap();
		for (int i = 0; i < hashAttrs.length; i++) {
			if (hashAttrs[i].equals(attribute)) {
				properties.put(BlockingJoin.BLOCKING_PARAM, String.valueOf(i));
				break;
			}
		}
		for (int i = 0; i < functions.length; i++) {
			if (functions[i].equals(function)) {
				properties.put(BlockingJoin.BLOCKING_FUNCTION, functionsEncoded[i]);
				break;
			}
		}
		
		return new BlockingJoin(sourceA.getPreprocessedDataSource(), sourceB.getPreprocessedDataSource(), outModel, joinCondition, properties);
	}

	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		
		this.sourceA = (AbstractDataSource) objects[0];
		this.sourceB = (AbstractDataSource) objects[1];
		this.outModel = (DataColumnDefinition[]) objects[2];
		this.joinCondition = (AbstractJoinCondition) objects[3];
		
		hashAttrs = new String[joinCondition.getLeftJoinColumns().length];
		for (int i = 0; i < hashAttrs.length; i++) {
			hashAttrs[i] = joinCondition.getLeftJoinColumns()[i] + 
				" and " + joinCondition.getRightJoinColumns()[i];
		}
		
		
		Map creators = new HashMap();
		creators.put(BLOCKING_ATTR, new ComboBoxPanelFieldCreator(hashAttrs));
		creators.put(BLOCKING_FUNCTION, new ComboBoxPanelFieldCreator(functions));
		
		String[] defaults = new String[] {hashAttrs[0], "Soundex, length = 5"};
		String restoredAttribute = getRestoredParam(BlockingJoin.BLOCKING_PARAM);
		if (restoredAttribute != null) {
			defaults[0] = hashAttrs[Integer.parseInt(restoredAttribute)];
		}
		String restoredFunction = getRestoredParam(BlockingJoin.BLOCKING_FUNCTION);
		String properFunction = null;
		for (int i = 0; i < functions.length; i++) {
			if (functionsEncoded[i].equals(restoredFunction)) {
				properFunction = functions[i];
				break;
			}
		}
		if (properFunction != null) {
			defaults[1] = properFunction;
		}
		
		buffer = new ParamsPanel(new String[] {BLOCKING_ATTR, BLOCKING_FUNCTION}, 
				new String[] {"Blocking attribute", "Blocking function"},
				defaults,
				creators);
		
		return buffer;
	}

	public Class getProducedComponentClass() {
		return BlockingJoin.class;
	}

	public String toString() {
		return "Blocking search method";
	}

	public boolean validate(JDialog dialog) {
		return true;
	}

}
