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


package cdc.configuration;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.utils.RJException;

public class ConfiguredSystem {
	
	private static ConfiguredSystem configuredSystem;
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private AbstractJoin join;
	private AbstractResultsSaver[] resultSavers;
	
	public ConfiguredSystem(AbstractDataSource left, AbstractDataSource right, AbstractJoin join, AbstractResultsSaver[] resultSavers) {
		this.sourceA = left;
		this.sourceB = right;
		this.join = join;
		this.resultSavers = resultSavers;
		configuredSystem = this;
	}

	public AbstractJoin getJoin() {
		return join;
	}

	public AbstractResultsSaver[] getResultSavers() {
		return resultSavers;
	}

	public AbstractDataSource getSourceA() {
		return sourceA;
	}

	public AbstractDataSource getSourceB() {
		return sourceB;
	}

	protected void finalize() throws Throwable {
		
		
		this.sourceA = null;
		this.sourceB = null;
		this.join = null;
		this.resultSavers = null;
		
		super.finalize();
	}

	public void close() {
		if (join != null) {
			try {
				join.close();
			} catch (RJException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			join = null;
		}
		if (resultSavers != null) {
			resultSavers.clone();
			resultSavers = null;
		}
		if (sourceA != null) {
			try {
				sourceA.close();
			} catch (RJException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			sourceA = null;
		}
		if (sourceB != null) {
			try {
				sourceB.close();
			} catch (RJException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			sourceB = null;
		}
		System.out.println("System closed.");
	}
	
	public static ConfiguredSystem getActiveSystem() {
		return configuredSystem;
	}
	
}