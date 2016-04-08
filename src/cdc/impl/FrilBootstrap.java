package cdc.impl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.util.classloader.URIClassLoader;

public class FrilBootstrap {
	
	public static final String JARS = "jars";
	
	public static class Arg {
		public String name;
		public String value;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, MalformedURLException, URISyntaxException {
		if (args.length == 0) {
			args = new String[] {JARS};
		}
		
		String dir = args[0];
		File f = new File(dir);
		String[] files = f.list();
		List urls = new ArrayList();
		urls.add(new URI("jar:/Users/pjurczy/Apps/FRIL/join.jar"));
		for (int i = 0; i < files.length; i++) {
			if (!files[i].endsWith(".jar")) {
				continue;
			}
			File file = new File(files[i]);
			urls.add(new URI("jar:" + file.getAbsolutePath()));
		}
		System.out.println("Jar files: " + urls);
		URIClassLoader classLoader = new URIClassLoader((URI[]) urls.toArray(new URI[] {}), ClassLoader.getSystemClassLoader().getParent());
		Thread.currentThread().setContextClassLoader(classLoader);
		Class mainClass = classLoader.loadClass("cdc.impl.MainGUI");
		Method main = mainClass.getMethod("main", new Class[] {String[].class});
		
		String[] argsApp = new String[args.length - 1];
		System.arraycopy(args, 1, argsApp, 0, argsApp.length);
		main.invoke(null, new Object[] {argsApp});
	}
	
}
