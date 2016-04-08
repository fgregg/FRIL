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

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.configuration.Configuration;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ui.TrimConverterField;
import cdc.datamodel.converters.ui.TrimConverterFieldCreator;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.dynamicanalysis.ConvAnalysisActionListener;
import cdc.gui.components.dynamicanalysis.ConvAnalysisRestartListener;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.NonEmptyValidator;
import cdc.utils.Log;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class TrimConverter extends AbstractColumnConverter {
	
	public static class TrimVisibleComponent extends GUIVisibleComponent {

		private static final String PARAM_END = "end";
		private static final String PARAM_FRONT = "front";
		private static final String PARAM_OUT_NAME = "out-name";
		private DataColumnDefinition column;
		private ParamsPanel panel;
		private JButton visual;
		private ConvAnalysisActionListener analysisListener = null;
		private static ConvAnalysisRestartListener propertyListener = new ConvAnalysisRestartListener();
	
		private static class CreatorName extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addPropertyChangeListener(propertyListener);
				return field;
			}
		}
		
		public Object generateSystemComponent() {
			
			Map properties = new HashMap();
			properties.put(PROPERTY_FRONT_TRIM, "-1");
			properties.put(PROPERTY_LEAVE_FIRST, "-1");
			properties.put(PROPERTY_END_TRIM, "-1");
			properties.put(PROPERTY_LEAVE_LAST, "-1");
			
			String f = panel.getParameterValue(PARAM_FRONT);
			String e = panel.getParameterValue(PARAM_END);
			String[] fSplit = f.split(",");
			String[] eSplit = e.split(",");
			
			if (fSplit[0].equals("1")) {
				if (fSplit[1].equals(TrimConverterField.OPTION_CUT + "")) {
					properties.put(PROPERTY_FRONT_TRIM, fSplit[2]);
				} else {
					properties.put(PROPERTY_LEAVE_FIRST, fSplit[2]);
				}
			} 
			if (eSplit[0].equals("1")) {
				if (eSplit[1].equals(TrimConverterField.OPTION_CUT + "")) {
					properties.put(PROPERTY_END_TRIM, eSplit[2]);
				} else {
					properties.put(PROPERTY_LEAVE_LAST, eSplit[2]);
				}
			}
			properties.put(PARAM_OUT_NAME, panel.getParameterValue(PARAM_OUT_NAME));
			
			return new TrimConverter(panel.getParameterValue(PARAM_OUT_NAME), properties, column);
		}

		public JPanel getConfigurationPanel(Object[] inParams, int sizeX, int sizeY) {
			
			String[] defs = new String[] {null, "0,1,0", "0,1,0"};
			if (getRestoredParam(PARAM_OUT_NAME) != null) {
				defs[0] = getRestoredParam(PARAM_OUT_NAME);
			}
			if (getRestoredParam(PROPERTY_FRONT_TRIM) != null) {
				if (!getRestoredParam(PROPERTY_FRONT_TRIM).equals("-1") || !getRestoredParam(PROPERTY_LEAVE_FIRST).equals("-1")) {
					String param = "1,";
					if (!getRestoredParam(PROPERTY_FRONT_TRIM).equals("-1")) {
						param += TrimConverterField.OPTION_CUT + "," + getRestoredParam(PROPERTY_FRONT_TRIM);
					} else {
						param += TrimConverterField.OPTION_LEAVE + "," + getRestoredParam(PROPERTY_LEAVE_FIRST);
					}
					defs[1] = param;
				}
			}
			if (getRestoredParam(PROPERTY_END_TRIM) != null) {
				if (!getRestoredParam(PROPERTY_END_TRIM).equals("-1") || !getRestoredParam(PROPERTY_LEAVE_LAST).equals("-1")) {
					String param = "1,";
					if (!getRestoredParam(PROPERTY_END_TRIM).equals("-1")) {
						param += TrimConverterField.OPTION_CUT + "," + getRestoredParam(PROPERTY_END_TRIM);
					} else {
						param += TrimConverterField.OPTION_LEAVE + "," + getRestoredParam(PROPERTY_LEAVE_LAST);
					}
					defs[2] = param;
				}
			}
			Map creators = new HashMap();
			creators.put(PARAM_FRONT, new TrimConverterFieldCreator(propertyListener));
			creators.put(PARAM_END, new TrimConverterFieldCreator(propertyListener));
			creators.put(PARAM_OUT_NAME, new CreatorName());
			panel = new ParamsPanel(new String[] {PARAM_OUT_NAME, PARAM_FRONT, PARAM_END}, 
					new String[] {"Output attribute name", "Modify front of string", "Modify end of string"}, 
					defs, creators);
			
			Map validators = new HashMap();
			validators.put(PARAM_OUT_NAME, new NonEmptyValidator());
			panel.setValidators(validators);
					
			visual = Configs.getAnalysisButton();
			//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
			panel.append(visual);
			
			
			this.column = (DataColumnDefinition) inParams[0];
			
			AbstractDataSource source = (AbstractDataSource) inParams[2];
			Window parent = (Window) inParams[3];
			JDataSource jDataSource = (JDataSource)inParams[4];
			
			if (analysisListener != null) {
				visual.removeActionListener(analysisListener);
			}
			
			visual.addActionListener(analysisListener = new ConvAnalysisActionListener(parent, source, this, propertyListener, jDataSource));
			
			return panel;
		}

		public String toString() {
			return "Trim converter";
		}
		
		public Class getProducedComponentClass() {
			return TrimConverter.class;
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}

	}

	public static final String PROPERTY_FRONT_TRIM = "trim-front";
	public static final String PROPERTY_END_TRIM = "trim-end";
	
	public static final String PROPERTY_LEAVE_LAST = "substring-end";
	public static final String PROPERTY_LEAVE_FIRST = "substring-front";
	
	
	private DataColumnDefinition[] in;
	private DataColumnDefinition[] out;
	private int trimFront = -1;
	private int trimEnd = -1;
	
	private int substringFront = -1;
	private int substringEnd = -1;
	
	public TrimConverter(String columnName, Map props, DataColumnDefinition column) {
		super(props);
		out = new DataColumnDefinition[1];
		out[0] = new ConverterColumnWrapper(columnName, DataColumnDefinition.TYPE_STRING, column.getSourceName());
		this.in = new DataColumnDefinition[] {column};
		props.put(TrimVisibleComponent.PARAM_OUT_NAME, columnName);
		if (props.containsKey(PROPERTY_FRONT_TRIM)) {
			trimFront = Integer.parseInt((String)props.get(PROPERTY_FRONT_TRIM));
		}
		if (props.containsKey(PROPERTY_END_TRIM)) {
			trimEnd = Integer.parseInt((String)props.get(PROPERTY_END_TRIM));
		}
		if (props.containsKey(PROPERTY_LEAVE_FIRST)) {
			substringFront = Integer.parseInt((String)props.get(PROPERTY_LEAVE_FIRST));
		}
		if (props.containsKey(PROPERTY_LEAVE_LAST)) {
			substringEnd = Integer.parseInt((String)props.get(PROPERTY_LEAVE_LAST));
		}
		Log.log(this.getClass(), "Converter created - trim-front=" + 
				trimFront + ", trim-end=" + trimEnd + ", substring-front=" + substringFront + 
				", substring-end=" + substringEnd, 2);
	}
	
//	public void updateName(String newName) {
//		setName(newName);
//		out = new DataColumnDefinition[] {new DataColumnDefinition(newName, out[0].getColumnType(), out[0].getSourceName())};
//	}

	public DataCell[] convert(DataCell[] dataCells) {
		String val = (String) dataCells[0].getValue();
		Log.log(this.getClass(), "Before trim: " + val, 3);
		if (trimFront != -1) {
			val = val.substring(trimFront > val.length() ? val.length() : trimFront);
		}
		if (trimEnd != -1) {
			val = val.substring(0, val.length() - trimEnd < 0 ? 0 : val.length() - trimEnd);
		}
		Log.log(this.getClass(), "After trim: " + val, 3);
		if (substringFront != -1) {
			val = val.substring(0, substringFront < val.length() ? substringFront : val.length());
		}
		if (substringEnd != -1) {
			val = val.substring(val.length() - substringEnd > 0 ? val.length() - substringEnd : 0, val.length());
		}
		Log.log(this.getClass(), "After substrings: " + val, 3);
		return new DataCell[] {new DataCell(dataCells[0].getValueType(), val)};
	}

	public DataColumnDefinition[] getExpectedColumns() {
		return in;
	}

	public DataColumnDefinition[] getOutputColumns() {
		return out;
	}

	public String getProperty(String propertyName) {
		super.getProperties().put(TrimVisibleComponent.PARAM_OUT_NAME, out[0].getColumnName());
		return super.getProperty(propertyName);
	}
	
	public Map getProperties() {
		super.getProperties().put(TrimVisibleComponent.PARAM_OUT_NAME, out[0].getColumnName());
		return super.getProperties();
	}
	
	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		String columnName = DOMUtils.getAttribute(element, "column");
		Element paramsNode = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsNode);
		DataColumnDefinition column = (DataColumnDefinition) genericColumns.get(columnName);
		return new TrimConverter(name, params, column);
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new TrimVisibleComponent();
	}
	
	public String toString() {
		return "Trim converter";
	}

	public void saveToXML(Document doc, Element conv) {
		DOMUtils.setAttribute(conv, Configuration.NAME_ATTR, out[0].getColumnName());
		DOMUtils.setAttribute(conv, "column", in[0].getColumnName());
		Configuration.appendParams(doc, conv, getProperties());
	}
}
