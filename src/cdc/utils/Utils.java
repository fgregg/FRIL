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


package cdc.utils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.MainFrame;
import cdc.impl.deduplication.DeduplicationDataSource;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;

public class Utils {

	public static Properties mapToProperties(Map params) {
		Properties p = new Properties();
		for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			String value = (String) params.get(name);
			if (value != null) {
				p.setProperty(name, value);
			}
		}
		return p;
	}
	
	public static String getParam(Map params, String paramName, boolean b) throws RJException {
		if (params.containsKey(paramName)) {
			return (String)params.get(paramName);
		}
		if (b) {
			throw new RJException("Mandatory attribute not set: " + paramName);
		}
		return null;
	}
	
	public static boolean isWindowMode() {
		return MainFrame.main != null;
	}
	
	public static boolean isTextMode() {
		return !isWindowMode();
	}

	public synchronized static File createBufferFile(Object ref) {
		File fout = new File(System.currentTimeMillis()  + "_" + ref.hashCode() + ".bin");
		while (fout.exists()) {
			fout = new File(System.currentTimeMillis()  + "_" + ref.hashCode() + ".bin");
		}
		return fout;
	}

	public static String getSummaryMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		if (system.isDeduplication()) {
			return prepareDeduplicationMessage(system, cancelled, elapsedTime, nn);
		} else {
			return prepareLinkageMessage(system, cancelled, elapsedTime, nn);
		}
	}

	private static String prepareLinkageMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		String msg = cancelled ? "Linkage cancelled by user.\n\n" : "Linkage successfully completed :)\n\n";
		
		if (!(system.getJoin() instanceof StrataJoinWrapper)) {
			msg += getSourceSummary(system.getJoin().getLinkageSummary().getCntReadSrcA(), system.getSourceA());
			msg += "\n\n";
			msg += getSourceSummary(system.getJoin().getLinkageSummary().getCntReadSrcB(), system.getSourceB());
			msg += "\n\n";
		}
		msg += getLinkageSummary(system.getJoin(), system.getResultSaver());
		msg += "\n\n";
		msg += (cancelled ? "The linkage process interrupted after " : "Overall the linkage process took ") + elapsedTime + "ms.";
		
		return msg;
	}

	private static String getLinkageSummary(AbstractJoin join, AbstractResultsSaver resultSaver) {
		String msg = "";
		if (resultSaver instanceof DeduplicatingResultsSaver) {
			DeduplicatingResultsSaver dedupe = (DeduplicatingResultsSaver)resultSaver;
			msg += "Linkage process initially identified " + join.getLinkageSummary().getCntLinked() + " linkages.";
			msg += "\nThe results deduplication identified " + dedupe.getDuplicatesCnt() + " duplicates.";
			msg += "\n" + dedupe.getSavedCnt() + " final linkages were saved.";
		} else {
			msg += "Linkage process identified " + join.getLinkageSummary().getCntLinked() + " linkages.";
		}
		if (join.isSummaryForLeftSourceEnabled() && join.isSummaryForRightSourceEnabled()) {
			msg += "\nSummary information for not joined data for both \nsources was generated.";
		} else if (join.isSummaryForLeftSourceEnabled()) {
			msg += "\nSummary information for not joined data for source \n" + join.getSourceA().getSourceName() + " was generated.";
		} else if (join.isSummaryForRightSourceEnabled()) {
			msg += "\nSummary information for not joined data for source \n" + join.getSourceB().getSourceName() + " was generated.";
		} else {
			msg += "\nNo summary information for not joined data was generated.";
		}
		return msg;
	}

	private static String getSourceSummary(int srcRecordsCnt, AbstractDataSource src) {
		if (!(src.getPreprocessedDataSource() instanceof DeduplicationDataSource)) {
			return "Source " + src.getSourceName() + " provided " + srcRecordsCnt + " records.\nNo deduplication of data source was performed.";
		} else {
			DeduplicationDataSource dedupe = (DeduplicationDataSource)src.getPreprocessedDataSource();
			String msg = "Source " + src.getSourceName() + " provided " + dedupe.getInputRecordsCount() + " records.\n" + 
					"The deduplication process identified " + dedupe.getDuplicatesCount() + " duplicates";
			if (src.getDeduplicationConfig().getMinusFile() != null) {
				msg += "\nThe report containing duplicates was saved into file.";
			}
			return msg;
		}
	}

	private static String prepareDeduplicationMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		AbstractDataSource src = system.getSourceA();
		
		String msg = cancelled ? "Deduplication cancelled by user.\n\n" : "Deduplication successfully completed :)\n\n";
		msg += getSourceSummary(-1, src);
		msg += "\n\n";
		msg += (cancelled ? "The deduplication process interrupted after " : "Overall the deduplication process took ") + elapsedTime + "ms.";
		
		return msg;
	}
	
}
