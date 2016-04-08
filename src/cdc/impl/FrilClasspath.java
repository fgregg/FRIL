package cdc.impl;

import java.io.File;

public class FrilClasspath {
	
	public static final String JARS = "jars";
	
	public static class Arg {
		public String name;
		public String value;
	}
	
	public static void main(String[] args) {
		System.out.println("A.B AcB".replaceAll("A\\.B", "CD"));
		if (args.length == 0) {
			printUsage();
			return;
		}
		
		String dir = args[0];
		File f = new File(dir);
		String[] files = f.list();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].endsWith(".jar")) {
				continue;
			}
			if (i > 0) {
				buffer.append(File.pathSeparator);
			}
			buffer.append(dir).append(File.separator).append(files[i]);
		}
		System.out.println(buffer.toString());
	}

	private static void printUsage() {
		System.out.println("Usage: FrilStartup dir-with-jars");
	}
	
}
