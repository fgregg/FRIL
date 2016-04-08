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

import cdc.datamodel.DataColumnDefinition;
import cdc.impl.distance.SoundexDistance;

public class BlockingFunctionFactory {

	public static class BlockingFunctionDescriptor {
		
		public String function;
		public int[] arguments;
		
		public BlockingFunctionDescriptor(String fName) {
			this(fName, null);
		}
		
		public BlockingFunctionDescriptor(String fName, int[] args) {
			function = fName;
			arguments = args;
		}
		
	}

	public static final String EQUALITY = "equality";
	public static final String SOUNDEX = "soundex";
	public static final String PREFIX = "prefix";
	
	
	public static BlockingFunction createBlockingFunction(DataColumnDefinition[][] columns, String function) {
		BlockingFunctionDescriptor descriptor = parseBlockingFunctionDescriptor(function);
		if (descriptor.function.equals(EQUALITY)) {
			return new EqualityBlockingFunction(columns);
		} else if (descriptor.function.equals(SOUNDEX)) {
			return new SoundexBlockingFunction(columns, descriptor.arguments[0]);
		} else {
			return new PrefixBlockingFunction(columns, descriptor.arguments[0]);
		}
	}

	public static BlockingFunctionDescriptor parseBlockingFunctionDescriptor(String function) {
		if (function.startsWith(EQUALITY)) {
			return new BlockingFunctionDescriptor(EQUALITY);
		} else if (function.startsWith(SOUNDEX)) {
			return new BlockingFunctionDescriptor(SOUNDEX, getParameters(function));
		} else if (function.startsWith(PREFIX)) {
			return new BlockingFunctionDescriptor(PREFIX, getParameters(function));
		} else {
			throw new RuntimeException("Cannot decode blocking function: " + function);
		}
	}

	private static int[] getParameters(String function) {
		int firstParenthesis = function.indexOf('(');
		int lastParenthesis = function.lastIndexOf(')');
		if (firstParenthesis == -1 || lastParenthesis == -1) {
			throw new RuntimeException("Blocking function " + function + " requires arguments.");
		}
		String argsList = function.substring(firstParenthesis + 1, lastParenthesis);
		String[] args = argsList.split(",");
		int[] argsInt = new int[args.length];
		for (int i = 0; i < argsInt.length; i++) {
			argsInt[i] = Integer.parseInt(args[i]);
		}
		return argsInt;
	}

	public static String encodeBlockingFunction(BlockingFunction fnct) {
		if (fnct instanceof EqualityBlockingFunction) {
			return EQUALITY;
		} else if (fnct instanceof SoundexBlockingFunction) {
			return SOUNDEX + "(" + ((SoundexBlockingFunction)fnct).getSoundexDistance().getProperty(SoundexDistance.PROP_SIZE) + ")";
		} else if (fnct instanceof PrefixBlockingFunction) {
			return PREFIX + "(" + ((PrefixBlockingFunction)fnct).getPrefixLength() + ")";
		} else {
			throw new RuntimeException("Unknown blocking function: " + fnct.getClass());
		}
	}

}
