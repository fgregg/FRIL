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


package cdc.impl.distance;

import java.awt.Dimension;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDistance;
import cdc.components.AbstractStringDistance;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.dynamicanalysis.DistAnalysisActionListener;
import cdc.gui.components.dynamicanalysis.DistAnalysisRestartListener;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.slope.SlopePanel;
import cdc.gui.validation.NumberValidator;
import cdc.impl.conditions.AbstractConditionPanel;
import cdc.impl.conditions.WeightedJoinCondition;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.StringUtils;

public class SoundexDistance extends AbstractStringDistance {

	private static class CreatorV1 extends DefaultParamPanelFieldCreator {
		private SlopePanel slope;
		public CreatorV1(SlopePanel slope) {this.slope = slope;}
		public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
			ParamPanelField field = super.create(parent, param, label, defaultValue);
			slope.bindV1(field);
			field.addPropertyChangeListener(propertyListener);
			return field;
		}
	}

	private static class CreatorV2 extends DefaultParamPanelFieldCreator {
		private SlopePanel slope;
		public CreatorV2(SlopePanel slope) {this.slope = slope;}
		public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
			ParamPanelField field = super.create(parent, param, label, defaultValue);
			slope.bindV2(field);
			field.addPropertyChangeListener(propertyListener);
			return field;
		}
	}
	
	private static class CreatorQ extends DefaultParamPanelFieldCreator {
		public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
			ParamPanelField field = super.create(parent, param, label, defaultValue);
			field.addPropertyChangeListener(propertyListener);
			return field;
		}
	}
	
	private static final int logLevel = Log.getLogLevel(SoundexDistance.class);
	
	public static final String PROP_SIZE = "soundex-length";
	
	public static final int DFAULT_SIZE = 5;
	
	private static DistAnalysisRestartListener propertyListener = new DistAnalysisRestartListener();
	
	private static class SoundexVisibleComponent extends GUIVisibleComponent {

		private ParamsPanel panel;
		private DistAnalysisActionListener analysisListener;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new SoundexDistance(panel.getParams());
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			Boolean boolCond = (Boolean)objects[0];
			String[] defs = new String[boolCond.booleanValue() ? 2:3];
			if (getRestoredParam(PROP_SIZE) != null) {
				defs[0] = getRestoredParam(PROP_SIZE);
			} else {
				defs[0] = String.valueOf(DFAULT_SIZE);
			}
			if (getRestoredParam(EditDistance.PROP_BEGIN_APPROVE_LEVEL) != null) {
				defs[1] = getRestoredParam(EditDistance.PROP_BEGIN_APPROVE_LEVEL);
			} else {
				defs[1] = String.valueOf(0);
			}
			if (boolCond.booleanValue()) {
				panel = new ParamsPanel(
						new String[] {PROP_SIZE, EditDistance.PROP_BEGIN_APPROVE_LEVEL},
						new String[] {"Soundex size", "Acceptance level (edit distance)"},
						defs
				);
				
				Map validators = new HashMap();
				validators.put(PROP_SIZE, new NumberValidator(NumberValidator.INTEGER));
				validators.put(EditDistance.PROP_BEGIN_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
				
			} else {
				if (getRestoredParam(EditDistance.PROP_END_APPROVE_LEVEL) != null) {
					defs[2] = getRestoredParam(EditDistance.PROP_END_APPROVE_LEVEL);
				} else {
					defs[2] = String.valueOf(0);
				}
				
				SlopePanel slope = new SlopePanel(Double.parseDouble(defs[1]), Double.parseDouble(defs[2]));
				slope.setPreferredSize(new Dimension(240, 70));
				CreatorV1 v1 = new CreatorV1(slope);
				CreatorV2 v2 = new CreatorV2(slope);
				CreatorQ l = new CreatorQ();
				Map creators = new HashMap();
				creators.put(EditDistance.PROP_BEGIN_APPROVE_LEVEL, v1);
				creators.put(EditDistance.PROP_END_APPROVE_LEVEL, v2);
				creators.put(PROP_SIZE, l);
				
				panel = new ParamsPanel(
						new String[] {PROP_SIZE, EditDistance.PROP_BEGIN_APPROVE_LEVEL, EditDistance.PROP_END_APPROVE_LEVEL},
						new String[] {"Soundex length", "Approve level (edit distance)", "Disapprove level (edit distance)"},
						defs, creators
				);
				panel.append(slope);
				
				Map validators = new HashMap();
				validators.put(PROP_SIZE, new NumberValidator(NumberValidator.INTEGER));
				validators.put(EditDistance.PROP_BEGIN_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				validators.put(EditDistance.PROP_END_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
				
				JButton visual = Configs.getAnalysisButton();
				//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
				visual.addActionListener(analysisListener = new DistAnalysisActionListener((Window)objects[2], (AbstractConditionPanel) objects[1], propertyListener));
				panel.append(visual);
			}
			
			panel.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getNewValue() == null && analysisListener != null) {
						analysisListener.closeWindow();
					}
				}});
			WeightedJoinCondition.attachListener(objects, propertyListener);
			
			return panel;
		}

		public Class getProducedComponentClass() {
			return SoundexDistance.class;
		}

		public String toString() {
			return "Soundex";
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}
		
		public void configurationPanelClosed() {
			analysisListener.closeWindow();
		}
		
	}
	
	private EditDistance distance;
	private int size;
	
	public SoundexDistance() {
		super(null);
		this.size = DFAULT_SIZE;
		distance = new EditDistance();
		Log.log(getClass(), "Soundex size=" + this.size, 1);
	}
	
	public SoundexDistance(Map props) {
		super(props);
		distance = new EditDistance(props);
		if (getProperty(PROP_SIZE) != null) {
			this.size = Integer.parseInt((String)getProperty(PROP_SIZE));
		}
		
		Log.log(getClass(), "Soundex size=" + this.size, 1);
	}
	
	public boolean distanceSatisfied(DataCell cell1, DataCell cell2) {
		
		String s1 = cell1.getValue().toString();
		String s2 = cell2.getValue().toString();
		
		String soundexS1 = encodeToSoundex(s1);
		String soundexS2 = encodeToSoundex(s2);
		
		
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, s1 + "=?=" + s2 + ": s1=" + soundexS1 + ", s2=" + soundexS2 + ", satisfied:" + distance.distanceSatisfied(soundexS1, soundexS2), 2);
		}
		
		return distance.distanceSatisfied(soundexS1, soundexS2);
	}
	
	
	public String encodeToSoundex(String string) {
		if (logLevel >= 2) {
			Log.log(getClass(), "Encoding to soundex: '" + string + "'", 2);
		}
		
		if (StringUtils.isNullOrEmptyNoTrim(string)) {
			return "";
		}
		
		string = string.toLowerCase();
		char first = string.charAt(0);
		String reminder = string.substring(1);
		
		reminder = reminder.replaceAll("[aehiouwy]", "");
		String soundexString = first + reminder;
		
		char[] table = (soundexString + "0000000000").toLowerCase().toCharArray();
        
		for (int i = 1; i < table.length; i++) {
            switch (table[i]) {
                case 'b':
                case 'f':
                case 'p':
                case 'v':
                	table[i] = '1'; break;
                case 'c':
                case 'g':
                case 'j':
                case 'k':
                case 'q':
                case 's':
                case 'x':
                case 'z': 
                	table[i] = '2'; break;
                case 'd':
                case 't': 
                	table[i] = '3'; break;
                case 'l': 
                	table[i] = '4'; break;
                case 'm':
                case 'n': 
                	table[i] = '5'; break;
                case 'r': 
                	table[i] = '6'; break;
                default: 
                	table[i] = '0'; break;
            }
        }
        return new String(table, 0, size);
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new SoundexVisibleComponent();
	}

	public String toString() {
		return "Soundex distance " + getProperties();
	}

	public double distance(DataCell cellA, DataCell cellB) {
		String s1 = cellA.getValue().toString();
		String s2 = cellB.getValue().toString();
		
		String soundexS1 = encodeToSoundex(s1);
		String soundexS2 = encodeToSoundex(s2);
		
		
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, s1 + "=?=" + s2 + ": s1=" + soundexS1 + ", s2=" + soundexS2 + ", satisfied:" + distance.distanceSatisfied(soundexS1, soundexS2), 2);
		}
		
		return distance.distance(soundexS1, soundexS2);
	}
	
	
	public static void main(String[] args) {
		AbstractDistance dst = new SoundexDistance();
		String[][] c1 = new String[][] {{"PAGE", "LEWIS"}};
		for (int i = 0; i < c1.length; i++) {
			System.out.println("Soundex between " + c1[i][0] + " and " + c1[i][1] + ": " + 
					dst.distance(new DataCell(DataColumnDefinition.TYPE_STRING, c1[i][0]), 
							new DataCell(DataColumnDefinition.TYPE_STRING, c1[i][1])));
		}
	}
}
