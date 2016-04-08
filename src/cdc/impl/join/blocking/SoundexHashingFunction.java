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


package cdc.impl.join.blocking;

import java.util.HashMap;
import java.util.Map;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.distance.SoundexDistance;
import cdc.utils.StringUtils;

public class SoundexHashingFunction implements HashingFunction {
	
	private SoundexDistance soundex;
	private DataColumnDefinition[][] columns;
	
	public SoundexHashingFunction(DataColumnDefinition[][] columns, int length) {
		Map props = new HashMap();
		props.put(SoundexDistance.PROP_SIZE, String.valueOf(length));
		this.soundex = new SoundexDistance(props);
		this.columns = columns;
	}
	
	public String hash(DataRow value, int id) {
		StringBuffer buffer = new StringBuffer();
		boolean empty = true;
		for (int i = 0; i < columns.length; i++) {
			if (i != 0) {
				buffer.append("_");
			}
			String val = value.getData(columns[i][id]).getValue().toString();
			if (!StringUtils.isNullOrEmpty(val)) {
				empty = false;
			}
			buffer.append(soundex.encodeToSoundex(val));
		}
		if (empty) return null;
		return buffer.toString();
	}

	public DataColumnDefinition[][] getColumns() {
		return columns;
	}
	
	public SoundexDistance getSoundexDistance() {
		return soundex;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof SoundexHashingFunction)) {
			return false;
		}
		SoundexHashingFunction that = (SoundexHashingFunction)obj;
		return soundex.getProperty(SoundexDistance.PROP_SIZE).equals(that.soundex.getProperty(SoundexDistance.PROP_SIZE)) && equalAttributes(that);
	}

	private boolean equalAttributes(SoundexHashingFunction that) {
		if (columns[0].length != that.columns[0].length) {
			return false;
		}
		for (int i = 0; i < columns[0].length; i++) {
			if (!columns[0][i].equals(that.columns[0][i]) || !columns[1][i].equals(that.columns[1][i])) {
				return false;
			}
		}
		return true;
	}
	
}