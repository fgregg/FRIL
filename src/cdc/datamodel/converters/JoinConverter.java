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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.codehaus.janino.CompileException;
import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.configuration.Configuration;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ui.ScriptPanel;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.components.datasource.JDataSource.Connection;
import cdc.gui.components.dynamicanalysis.ConvAnalysisActionListener;
import cdc.gui.components.dynamicanalysis.ConvAnalysisRestartListener;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.paramspanel.SeparatorPanelFieldCreator;
import cdc.gui.validation.NonEmptyValidator;
import cdc.gui.validation.Validator;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import cdc.utils.StringUtils;
import edu.emory.mathcs.util.xml.DOMUtils;

public class JoinConverter extends AbstractColumnConverter {
	
	private static final String[] seps = new String[] {" ", "<empty>", ",", "-"};
	private static final String[] labels = new String[] {"Space", "None", "Comma", "Hyphen"};
	
	public static class JoinVisibleComponent extends GUIVisibleComponent {
	
		private static class Creator extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addPropertyChangeListener(propertyListener);
				return field;
			}
		}
		
		private DefaultListModel columnsModel = new DefaultListModel();
		private JList columnsList = new JList(columnsModel);
		private ParamsPanel paramsPanel;
		private DataColumnDefinition column;
		private JButton visual;
		private ConvAnalysisActionListener analysisListener = null;
		private static ConvAnalysisRestartListener propertyListener = new ConvAnalysisRestartListener();
		private ScriptPanel scriptPanel;
		
		public Object generateSystemComponent() throws RJException {
			Object[] selected = columnsList.getSelectedValues();
			DataColumnDefinition[] cols = new DataColumnDefinition[selected.length + 1];
			for (int i = 0; i < selected.length; i++) {
				cols[i + 1] = (DataColumnDefinition)selected[i];
			}
			cols[0] = column;
			Map props = paramsPanel.getParams();
			props.put(PARAM_SCRIPT, scriptPanel.getScript ());
			return new JoinConverter(paramsPanel.getParameterValue(PARAM_OUT_NAME), cols, props);
		}

		public JPanel getConfigurationPanel(Object[] params, int sizeX, int sizeY) {
			
			columnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			
			scriptPanel = new ScriptPanel(AbstractColumnConverter.getDefaultScript(JoinConverter.class), 
					String.class, new String[] {"columns", "connector"}, new Class[] {String[].class, String.class});
			
			String[] defaults = new String[] {null, seps[0]};
			if (getRestoredParam(PARAM_OUT_NAME) != null) {
				defaults[0] = getRestoredParam(PARAM_OUT_NAME);
			}
			if (getRestoredParam(PARAM_COUPLER) != null) {
				defaults[1] = getRestoredParam(PARAM_COUPLER);
			}
			if (getRestoredParam(PARAM_SCRIPT) != null) {
				scriptPanel.setScript(getRestoredParam(PARAM_SCRIPT));
			}
			
			Map creators = new HashMap();
			creators.put(PARAM_COUPLER, new SeparatorPanelFieldCreator(seps, labels, 2, 3, propertyListener));
			creators.put(PARAM_OUT_NAME, new Creator());
			
			paramsPanel = new ParamsPanel(
					new String[] {PARAM_OUT_NAME, PARAM_COUPLER}, 
					new String[] {"Out atribute name", "Join fields using"}, 
					defaults, creators);
			Map validators = new HashMap();
			validators.put(PARAM_OUT_NAME, new NonEmptyValidator());
			validators.put(PARAM_COUPLER, new Validator() {
				public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
					return !StringUtils.isNullOrEmptyNoTrim(parameterValue);
				}});
			paramsPanel.setValidators(validators);
			
			visual = Configs.getAnalysisButton();
			//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
			
			
			column = (DataColumnDefinition)params[0];
			
			//DataColumnDefinition columns[] = (DataColumnDefinition[])params[1];
			AbstractDataSource source = (AbstractDataSource) params[2];
			Window parent = (Window) params[3];
			JDataSource jDataSource = (JDataSource)params[4];
			
			columnsModel.removeAllElements();
			Connection[] connections = jDataSource.getConnections();
			List genericCols = new ArrayList(Arrays.asList(source.getAvailableColumns()));
			for (int j = 0; j < connections.length; j++) {
				if (!(connections[j].conv.conv instanceof DummyConverter)) {
					Brick[] to = connections[j].to;
					for (int i = 0; i < to.length; i++) {
						if (!to[i].col.equals(column)) {
							columnsModel.addElement(to[i].col);
						}
					}
				} else {
					DataColumnDefinition from = connections[j].from[0].col;
					DataColumnDefinition to = connections[j].to[0].col;
					genericCols.remove(from);
					if (!(to.equals(column) || from.equals(column))) {
						columnsModel.addElement(to);
					}
				}
			}
			for (Iterator iterator = genericCols.iterator(); iterator.hasNext();) {
				DataColumnDefinition object = (DataColumnDefinition) iterator.next();
				if (!object.equals(column)) {
					columnsModel.addElement(object);
				}
			}
			
			JScrollPane listScroll = new JScrollPane(columnsList); 
			listScroll.setPreferredSize(new Dimension(250, 100));
			
			JPanel otherColumns = new JPanel(new FlowLayout());
			JLabel label = new JLabel("Joined columns:");
			label.setPreferredSize(new Dimension(150, (int)label.getPreferredSize().getHeight()));
			otherColumns.add(label);
			otherColumns.add(listScroll);
			columnsList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					propertyListener.propertyChange(new PropertyChangeEvent(e.getSource(), "selected-columns", null, null));
				}
			});
			
			JPanel visualPanel = new JPanel();
			visualPanel.add(visual);
			
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(this.paramsPanel, BorderLayout.NORTH);
			panel.add(otherColumns, BorderLayout.CENTER);
			panel.add(visualPanel, BorderLayout.SOUTH);
			
			if (analysisListener != null) {
				visual.removeActionListener(analysisListener);
			}
			visual.addActionListener(analysisListener = new ConvAnalysisActionListener(parent, source, this, propertyListener, jDataSource));
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Configuration", panel);
			tabs.addTab("Converter script (advanced)", scriptPanel);
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(tabs, BorderLayout.CENTER);
			return mainPanel;
		}

		public String toString() {
			return "Merge converter";
		}

		public Class getProducedComponentClass() {
			return JoinConverter.class;
		}

		public boolean validate(JDialog parent) {
			if (columnsList.getSelectedIndex() == -1) {
				JOptionPane.showMessageDialog(parent, "Select at least one column to merge.");
				return false;
			}
			return paramsPanel.doValidate();
		}

	}

	private static final String PARAM_OUT_NAME = "out-name";
	private static final String PARAM_COUPLER = "coupler-param";
	private static final String PARAM_SCRIPT = "script";
	private static final String DEFAULT_COUPLER = " ";
	
	private DataColumnDefinition[] columns;
	private DataColumnDefinition[] outFormat;
	private String coupler = DEFAULT_COUPLER;
	
	private ScriptEvaluator scriptEvaluator;
	
	public JoinConverter(String name, DataColumnDefinition[] columns, Map props) throws RJException {
		super(props);
		this.columns = columns;
		this.outFormat = new DataColumnDefinition[] {new ConverterColumnWrapper(name, columns[0].getColumnType(), columns[0].getSourceName())};
		if (props.containsKey(PARAM_COUPLER)) {
			this.coupler = (String) props.get(PARAM_COUPLER);
			if (coupler != null && coupler.equals(seps[1])) {
				coupler = "";
			}
		}
		try {
			if (props.get(PARAM_SCRIPT) == null) {
				props.put(PARAM_SCRIPT, AbstractColumnConverter.getDefaultScript(JoinConverter.class));
			}
			scriptEvaluator = new ScriptEvaluator((String)props.get(PARAM_SCRIPT), String.class, new String[] {"columns", "connector"}, new Class[] {String[].class, String.class});
		} catch (CompileException e) {
			throw new RJException("Compilation exception for script", e);
		} catch (ParseException e) {
			throw new RJException("Parse exception for script", e);
		} catch (ScanException e) {
			throw new RJException("Scan exception for script", e);
		}
		Log.log(getClass(), "Converter created - joined columns: " + PrintUtils.printArray(columns), 2);
	}
	
//	public void updateName(String newName) {
//		setName(newName);
//		outFormat = new DataColumnDefinition[] {new DataColumnDefinition(newName, outFormat[0].getColumnType(), outFormat[0].getSourceName())};
//	}

	public DataCell[] convert(DataCell[] dataCells) throws RJException {
//		StringBuffer buffer = new StringBuffer();
//		for (int i = 0; i < dataCells.length; i++) {
//			if (i > 0) {
//				buffer.append(coupler);
//			}
//			buffer.append(dataCells[i].getValue());
//		}
		String[] cells = new String[dataCells.length];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = dataCells[i].getValue().toString();
		}
		try {
			return new DataCell[] {new DataCell(dataCells[0].getValueType(), scriptEvaluator.evaluate(new Object[] {cells, coupler}))};
		} catch (InvocationTargetException e) {
			throw new RJException("Error when executing converter script", e);
		}
	}
	
	public DataColumnDefinition[] getExpectedColumns() {
		return columns;
	}

	public DataColumnDefinition[] getOutputColumns() {
		return outFormat;
	}
	
	public String getProperty(String propertyName) {
		super.getProperties().put(PARAM_OUT_NAME, outFormat[0].getColumnName());
		return super.getProperty(propertyName);
	}
	
	public Map getProperties() {
		super.getProperties().put(PARAM_OUT_NAME, outFormat[0].getColumnName());
		return super.getProperties();
	}
	
	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		Map params = null;
		List childCols = new ArrayList();
		Element rows = DOMUtils.getChildElement(element, Configuration.ROW_MODEL_TAG);
		Element[] columns = DOMUtils.getChildElements(rows);
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getNodeName().equals(Configuration.ROW_TAG)) {
				String subcolumnName = DOMUtils.getAttribute(columns[i], Configuration.NAME_ATTR);
				if (!genericColumns.containsKey(subcolumnName)) {
					throw new RJException("Column " + subcolumnName + " is not provided by source...");
				}
				childCols.add(genericColumns.get(subcolumnName));
			}
		}
		
		Element paramsEl = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		if (paramsEl != null) {
			params = Configuration.parseParams(paramsEl);
		}
		
		return new JoinConverter(name, (DataColumnDefinition[])childCols.toArray(new DataColumnDefinition[] {}), params);
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new JoinVisibleComponent();
	}
	
	public String toString() {
		return "Merge converter";
	}

	public void saveToXML(Document doc, Element node) {
		DOMUtils.setAttribute(node, Configuration.NAME_ATTR, outFormat[0].getColumnName());
		Configuration.appendParams(doc, node, getProperties());
		Element columnsTag = DOMUtils.createChildElement(doc, node, Configuration.ROW_MODEL_TAG);
		for (int i = 0; i < columns.length; i++) {
			Element child = DOMUtils.createChildElement(doc, columnsTag, Configuration.ROW_TAG);
			DOMUtils.setAttribute(child, Configuration.NAME_ATTR, columns[i].getColumnName());
		}
	}
}
