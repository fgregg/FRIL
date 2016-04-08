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
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import cdc.components.AbstractStringDistance;
import cdc.datamodel.DataCell;
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

public class EditDistance extends AbstractStringDistance {

	private static final int logLevel = Log.getLogLevel(EditDistance.class);
	
	public static final String PROP_END_APPROVE_LEVEL = "math-level-end";
	public static final String PROP_BEGIN_APPROVE_LEVEL = "match-level-start";
	public static final double DEFAULT_BEGIN_APPROVE_LEVEL = 0.2;
	public static final double DEFAULT_END_APPROVE_LEVEL = 0.4;
	
	private static DistAnalysisRestartListener propertyListener = new DistAnalysisRestartListener();
	
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
	
	private static class EditDistanceVisibleComponent extends GUIVisibleComponent {

		private ParamsPanel panel;
		private DistAnalysisActionListener analysisListener;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new EditDistance(panel.getParams());
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			Boolean boolCond = (Boolean)objects[0];
			String[] defs = new String[boolCond.booleanValue() ? 1:2];
			if (getRestoredParam(PROP_BEGIN_APPROVE_LEVEL) != null) {
				defs[0] = getRestoredParam(PROP_BEGIN_APPROVE_LEVEL);
			} else {
				defs[0] = String.valueOf(DEFAULT_BEGIN_APPROVE_LEVEL);
			}
			if (boolCond.booleanValue()) {
				panel = new ParamsPanel(
						new String[] {PROP_BEGIN_APPROVE_LEVEL},
						new String[] {"Approve level"},
						defs
				);
				
				Map validators = new HashMap();
				validators.put(PROP_BEGIN_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
				
			} else {
				if (getRestoredParam(PROP_END_APPROVE_LEVEL) != null) {
					defs[1] = getRestoredParam(PROP_END_APPROVE_LEVEL);
				} else {
					defs[1] = String.valueOf(DEFAULT_END_APPROVE_LEVEL);
				}
				SlopePanel slope = new SlopePanel(Double.parseDouble(defs[0]), Double.parseDouble(defs[1]));
				slope.setPreferredSize(new Dimension(240, 70));
				CreatorV1 v1 = new CreatorV1(slope);
				CreatorV2 v2 = new CreatorV2(slope);
				Map creators = new HashMap();
				creators.put(PROP_BEGIN_APPROVE_LEVEL, v1);
				creators.put(PROP_END_APPROVE_LEVEL, v2);
				panel = new ParamsPanel(
						new String[] {PROP_BEGIN_APPROVE_LEVEL, PROP_END_APPROVE_LEVEL},
						new String[] {"Approve level", "Disapprove level"},
						defs, creators
				);
				panel.append(slope);
				JButton visual = Configs.getAnalysisButton();
				//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
				visual.addActionListener(analysisListener = new DistAnalysisActionListener((Window)objects[2], (AbstractConditionPanel) objects[1], propertyListener));
				panel.append(visual);
				Map validators = new HashMap();
				validators.put(PROP_BEGIN_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				validators.put(PROP_END_APPROVE_LEVEL, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
			}
			
			panel.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getNewValue() == null && analysisListener != null) {
						analysisListener.closeWindow();
					}
				}
			});
			WeightedJoinCondition.attachListener(objects, propertyListener);
			
			return panel;
		}

		public Class getProducedComponentClass() {
			return EditDistance.class;
		}

		public String toString() {
			return "Edit distance";
		}

		public boolean validate(JDialog dialog) {
			if (!panel.doValidate()) return false;
			double a = Double.parseDouble(panel.getParameterValue(PROP_BEGIN_APPROVE_LEVEL));
			double d = Double.parseDouble(panel.getParameterValue(PROP_END_APPROVE_LEVEL));
			if (a > d) {
				JOptionPane.showMessageDialog(dialog, "Approve level value has to be less than or equal to disapprove level value");
				return false;
			} else {
				return true;
			}
		}

		public void configurationPanelClosed() {
			this.analysisListener.closeWindow();
		}
		
		
	}
	
	private double APPROVE = DEFAULT_BEGIN_APPROVE_LEVEL;
	private double DISAPPROVE = DEFAULT_END_APPROVE_LEVEL;
	
	public EditDistance() {
		super(null);
		Log.log(getClass(), "Approve level=" + APPROVE, 1);
	}
	
	public EditDistance(Map props) {
		super(props);
		if (getProperty(PROP_BEGIN_APPROVE_LEVEL) != null) {
			this.APPROVE = Double.parseDouble((String)getProperty(PROP_BEGIN_APPROVE_LEVEL));
		}
		if (getProperty(PROP_END_APPROVE_LEVEL) != null) {
			this.DISAPPROVE = Double.parseDouble((String)getProperty(PROP_END_APPROVE_LEVEL));
		}
		Log.log(getClass(), "Approve level=" + APPROVE, 1);
	}
	
	public boolean distanceSatisfied(DataCell cell1, DataCell cell2) {
		String str1 = cell1.getValue().toString();
		String str2 = cell2.getValue().toString();
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, "'" + str1 + "=?=" + str2 + "': " + distanceSatisfied(str1, str2), 2);
		}
		return distanceSatisfied(str1, str2);
	}

	public boolean distanceSatisfied(String s1, String s2) {
		double dist = distanceInt(s1, s2);
		double approveLevel = Math.max(s1.length(), s2.length()) * APPROVE;
		if (logLevel >= 3) {
			Log.log(getClass(), "Distance between '" + s1 + "' and '" + s2 + "' is '" + dist + "(approve level: " + approveLevel + ")", 3);
		}
		return  dist <= approveLevel;
	}
	
	public double edits(String s1, String s2) {
		return distanceInt(s1, s2);
	}
	
	private double distanceInt(String str1, String str2) {
		int m = str1.length();
		int n = str2.length();
		str1 = str1.toUpperCase();
		str2 = str2.toUpperCase();
		int mat[][] = new int[m][n];
		
		if(m==0 || n==0) {
			return Math.max(m, n);
		} else{
			for (int k = 0; k < str1.length(); k++) {
				mat[k][0] = k;
			}
			for (int k = 0; k < str2.length(); k++) {
				mat[0][k] = k;
			}
		    
			for (int k = 1; k < str1.length(); k++) {
		       for (int l = 1; l < str2.length(); l++) {
		    	   int cost = 0;
		           if (str1.charAt(k) == str2.charAt(l)) { 
		        	   cost = 0;
		           } else {
		               cost = 1;
		           }
		           mat[k][l] = minimum(mat[k-1][l] + 1, mat[k][l-1] + 1, mat[k-1][l-1] + cost);
		           if (k > 1 && l > 1 && str1.charAt(k) == str2.charAt(l-1) && str1.charAt(k-1) == str2.charAt(l)) {
		               mat[k][l] = Math.min(mat[k][l], mat[k-2][l-2] + cost);
		           }
		       }
		                                
			}
		   return mat[m - 1][n - 1];   
		}
	}

	private int minimum(int i, int j, int k) {
		return Math.min(i, Math.min(j, k));
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new EditDistanceVisibleComponent();
	}
	

	public String toString() {
		return "Edit distance " + getProperties();
	}
	
	public double distance(String s1, String s2) {
		double dist = distanceInt(s1, s2);
		double approveLevel = Math.max(s1.length(), s2.length()) * APPROVE;
		double disapproveLevel = Math.max(s1.length(), s2.length()) * DISAPPROVE;
		if (logLevel >= 2) {
			Log.log(getClass(), "Compared: " + s1 + "==" + s2);
			Log.log(getClass(), "Distance: " + dist + "; [" + approveLevel + "-" + disapproveLevel + "]", 2);
		}
		//System.out.println("Strings: " + s1 + "  " + s2 + "  ===>  " + dist);
		if (dist > disapproveLevel) {
			return 0;
		} else if (dist <= approveLevel) {
			return 100;
		} else {
			if (logLevel >= 2) {
				Log.log(getClass(), "Fuzzy distance: " + (100 + 100/(approveLevel - disapproveLevel)*(dist-approveLevel)));
			}
			return (100 + 100/(approveLevel - disapproveLevel)*(dist-approveLevel));
		}
	}

	public double distance(DataCell cell1, DataCell cell2) {
		String str1 = cell1.getValue().toString();
		String str2 = cell2.getValue().toString();
		return distance(str1, str2);
	}
}