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


package cdc.impl.resultsavers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import au.com.bytecode.opencsv.CSVWriter;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.Log;
import cdc.utils.RJException;

public class CSVFileSaver extends AbstractResultsSaver {

	public static final String DEFAULT_FILE = "results.csv";
	public static final String OUTPUT_FILE_PROPERTY = "output-file";
	public static final String SAVE_SOURCE_NAME = "save-source-name";
	public static final String SAVE_CONFIDENCE = "save-confidence";
	
	private static class CSVFileSaverVisibleComponent extends GUIVisibleComponent {
		
		private ParamsPanel panel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new CSVFileSaver(panel.getParams());
		}
		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			
			String[] defs = new String[] {DEFAULT_FILE};
			if (getRestoredParam(OUTPUT_FILE_PROPERTY) != null) {
				defs[0] = getRestoredParam(OUTPUT_FILE_PROPERTY);
			}
			Map map = new HashMap();
			map.put(OUTPUT_FILE_PROPERTY, new FileChoosingPanelFieldCreator());
			panel = new ParamsPanel(
					new String[] {OUTPUT_FILE_PROPERTY},
					new String[] {"Output file"},
					defs,
					map
			);
				//panel.setPreferredSize(new Dimension(400, 100));
			
			return panel;
		}
		public Class getProducedComponentClass() {
			return CSVFileSaver.class;
		}
		public String toString() {
			return "CSV file data saver";
		}
		public boolean validate(JDialog dialog) {
			return true;
		}
	}
	
	private File file;
	private CSVWriter printer;
	private boolean saveConfidence = true;
	private boolean closed = false;
	private boolean saveSourceName = true;
	
	public CSVFileSaver(Map properties) throws RJException {
		super(properties);
		if (!properties.containsKey(OUTPUT_FILE_PROPERTY)) {
			file = new File(DEFAULT_FILE);
		} else {
			file = new File((String) properties.get(OUTPUT_FILE_PROPERTY));
		}
		if (properties.containsKey(SAVE_SOURCE_NAME)) {
			saveSourceName = Boolean.parseBoolean((String)properties.get(SAVE_SOURCE_NAME));
		}
		if (file.exists() && !file.isFile()) {
			throw new RJException("Output file cannot be directory or other special file");
		}
		if (properties.containsKey(SAVE_CONFIDENCE)) {
			saveConfidence = properties.get(SAVE_CONFIDENCE).equals("true");
		}
	}
	
	public void saveRow(DataRow row) throws RJException, IOException {
		//System.out.println(file.getName() + ": Adding row: " + row);
			//wait until we can safely write
			
			String stratum = row.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME);
			if (printer == null) {
//				if (file.exists()) {
//					file.delete();
//				}
				printer = new CSVWriter(new BufferedWriter(new FileWriter(file)));
				String[] header = new String[row.getData().length + (saveConfidence ? 1 : 0) + (stratum != null?1:0)];
				for (int i = 0; i < header.length - (stratum != null?1:0) - (saveConfidence ? 1 : 0); i++) {
					if (saveSourceName) {
						header[i] = row.getRowModel()[i].toString();
					} else {
						header[i] = row.getRowModel()[i].getColumnName();
					}
				}
				if (stratum != null) {
					header[header.length - 2] = "Stratum name";
				}
				if (saveConfidence) {
					header[header.length - 1] = "Confidence";
				}
				printer.writeNext(header);
			}
			DataCell[] cells = row.getData();
			//System.out.println("Cells were in row (" + row.hashCode() + "):" + PrintUtils.printArray(cells));
			String[] strRow = new String[cells.length + (saveConfidence ? 1 : 0) + (stratum != null ? 1 : 0)];
			for (int i = 0; i < strRow.length - (stratum != null ? 1 : 0) - (saveConfidence ? 1 : 0); i++) {
				strRow[i] = cells[i].getValue().toString();
			}
			if (stratum != null) {
				strRow[strRow.length - 2] = stratum;
			}
			if (saveConfidence) {
				strRow[strRow.length - 1] = row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE);
			}
			printer.writeNext(strRow);

	}
	
	public void flush() throws IOException {
		if (printer != null) {
			printer.flush();
		}
	}

	public void close() throws IOException {
		Log.log(getClass(), "Close in CSV saver for file " + file);
		if (closed) {
			return;
		}
		closed = true;
		if (printer != null) {
			printer.flush();
			printer.close();
			printer = null;
		}
	}
	
	public void reset() throws IOException {
		if (printer != null) {
			printer.close();
			printer = null;
		}
		closed = false;
	}
	
	//Method moved to AbstractResultsSaver
//	public static AbstractResultsSaver fromXML(Element element) throws RJException {
//		Element params = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
//		Map parameters = null;
//		if (params != null) {
//			parameters = Configuration.parseParams(params);
//		}
//		return new CSVFileSaver(parameters);
//	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new CSVFileSaverVisibleComponent();
	}
	
	public String toString() {
		return "CSV file saver";
	}
	
	public String toHTMLString() {
		return "CSV result saver (file=" + file.getName() + ")";
	}

	public String getActiveDirectory() {
		return new File(file.getAbsolutePath()).getParent();
	}

	public boolean isClosed() {
		return closed;
	}
}
