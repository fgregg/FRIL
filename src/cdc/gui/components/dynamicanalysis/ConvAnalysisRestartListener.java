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


package cdc.gui.components.dynamicanalysis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Timer;

import cdc.components.AbstractDataSource;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;

public class ConvAnalysisRestartListener implements PropertyChangeListener {
	
	private DynamicAnalysisFrame frame;
	private AbstractDataSource source;
	private GUIVisibleComponent convCreator;
	private Timer timer;
	private JDataSource dataSource;
	
	public ConvAnalysisRestartListener() {
	}
	
	public void setFrame(DynamicAnalysisFrame frame) {
		this.frame = frame;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (convCreator == null) return;
		if (timer != null && timer.isRunning()) timer.stop();
		timer = new Timer(700, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (convCreator.validate(null)) {
						AbstractColumnConverter conv = (AbstractColumnConverter) convCreator.generateSystemComponent();
						frame.setParameters(ConvAnalysisActionListener.getColumns(conv), new Object[] {source, conv, new ModelGenerator(dataSource.getConverters())});
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				timer.stop();
			}
		});
		timer.start();
	}

	public void setConvCreator(GUIVisibleComponent convCreator) {
		this.convCreator = convCreator;
	}
	
	public void setSource(AbstractDataSource source) {
		this.source = source;
	}
	
	public void setJDataSource(JDataSource source) {
		this.dataSource = source;
	}
}
