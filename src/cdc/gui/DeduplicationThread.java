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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataRow;
import cdc.gui.components.progress.DedupeInfoPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.Log;
import cdc.utils.RJException;

public class DeduplicationThread extends StoppableThread {
	
	//private Animation animation;
	private volatile DedupeInfoPanel info;
	private AbstractDataSource source;
	private String fileLocation;
	private volatile boolean stopped = false;
	private long t1;
	private long t2;
	private int n;
	
	public DeduplicationThread(AbstractDataSource source, String resultsFile, DedupeInfoPanel info) {
		this.source = source;
		this.info = info;
		this.fileLocation = resultsFile;
	}
	
	public void run() {
		n = 0;
		DataRow row;
		//JFrame frame = null;
		try {
			
			sleep(1000);
			
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.getProgressBar().setIndeterminate(false);
				}
			});
			
			System.gc();
			
			source = source.getPreprocessedDataSource();
			
			Map props = new HashMap();
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileLocation);
			AbstractResultsSaver saver = new CSVFileSaver(props);
			
			t1 = System.currentTimeMillis();
			while ((row = source.getNextRow()) != null) {
				n++;
				saver.saveRow(row);
				if (stopped) {
					break;
				}
				
			}
			t2 = System.currentTimeMillis();
			saver.flush();
			saver.close();
			
			Log.log(getClass(), "Deduplication completed. Elapsed time: " + (t2 - t1) + "ms.", 1);
			closeProgress();
			//animation.stopAnimation();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (stopped) {
						Log.log(getClass(), "Deduplication was cancelled", 1);
						JOptionPane.showMessageDialog(MainFrame.main, "Deduplication was cancelled.\nTime: " + (t2-t1) + "ms");
					} else {
						JOptionPane.showMessageDialog(MainFrame.main, "Deduplication complete." + "\nProcess took " + (t2-t1) + "ms");
					}
				}
			});
			
		} catch (RJException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} catch (IOException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} catch (Exception e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		
		info = null;
		source = null;
	}


	private void closeProgress() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.dedupeCompleted();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void scheduleStop() {
		this.stopped = true;
		//this.interrupt();
	}
	
}
