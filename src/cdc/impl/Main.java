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


package cdc.impl;

import java.io.IOException;

import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.datamodel.DataRow;
import cdc.utils.RJException;

public class Main {
	
	private Configuration join;
	
	public Main(String configFile) throws RJException, IOException {
		join = Configuration.getConfiguration(configFile);
	}
	
	public int runJoin() throws RJException, IOException {
		return startJoin(join.getJoin(), join.getResultsSavers());
	}
	
	public static void main(String[] args) throws IOException, RJException {
		
		//testSortingSource();
		//testFixedWidthDS();
		//testJDBCDS();
		
		Configuration configuration = Configuration.getConfiguration();
		
		AbstractJoin join = configuration.getJoin();

		long t1 = System.currentTimeMillis();
		int n = startJoin(join, configuration.getResultsSavers());
		long t2 = System.currentTimeMillis();
		System.out.println("\n" + join + ": Algorithm produced " + n + " joined tuples. Elapsed time: " + (t2 - t1) + "ms.");
	}

	private static int startJoin(AbstractJoin join, AbstractResultsSaver[] abstractResultsSavers) throws IOException, RJException {
		int n = 0;
		DataRow row;
		while ((row = join.joinNext()) != null) {
			n++;
			if (n % 1000 == 0) System.out.print(".");
			
			for (int i = 0; i < abstractResultsSavers.length; i++) {
				abstractResultsSavers[i].saveRow(row);
			}
		}
		for (int i = 0; i < abstractResultsSavers.length; i++) {
			abstractResultsSavers[i].close();
		}
		return n;
	}

	public int rerun() throws IOException, RJException {
		join.getJoin().reset();
		return startJoin(join.getJoin(), join.getResultsSavers());
	}
	
	public void close() throws IOException, RJException {
		join.getJoin().close();
	}
	
}
