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
