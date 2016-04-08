package cdc.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.JoinConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.Main;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;

public class UnitCoreTester {
	
	public static void main(String[] args) throws SecurityException, IllegalArgumentException, IOException, NoSuchMethodException, ClassNotFoundException, RJException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Log.logToFile("tests/log.txt");
		Props.disablePrinting();
		try {
			System.out.println("Unit tester of core FRIL functionality.");
			System.out.println("Testing converters and data sources");
			testConvertersAndSources();
			System.out.println("Testing joins...");
			testJoins();
		} catch (ComparisonFailure f) {
			System.out.println("...FAILED");
			System.out.println(f.getMessage());
			System.out.println("TESTING FAILED!");
			System.exit(0);
		} catch (AssertionFailedError f) {
			System.out.println("...FAILED");
			System.out.println(f.getMessage());
			System.out.println("TESTING FAILED!");
			System.exit(0);
		}
		System.out.println("All tests completed.");
	}

	private static void testJoins() throws IOException, RJException {
		ItemDescriptor[] joins = readDescriptors("tests/joins.txt");
		for (int i = 0; i < joins.length; i++) {
			System.out.print("   Testing " + joins[i].message);
			Main main = new Main((String) joins[i].properties.get("configuration"));
			int results = main.runJoin();
			Assert.assertEquals("ERROR in initial join", 3, results);
			results = main.rerun();
			Assert.assertEquals("ERROR in join after reset", 3, results);
			System.out.println(" OK");
		}
	}

	private static void testConvertersAndSources() throws IOException, SecurityException, NoSuchMethodException, ClassNotFoundException, RJException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		ItemDescriptor[] sourceDescriptors = readDescriptors("tests/sources.txt");
		
		for (int i = 0; i < sourceDescriptors.length; i++) {
			System.out.print("   Testing " + sourceDescriptors[i].message);
			Object[] constrParams = new Object[] {(String)sourceDescriptors[i].properties.get("source-name"), sourceDescriptors[i].properties};
			Constructor c = Class.forName(sourceDescriptors[i].className).getConstructor(new Class[] {String.class, Map.class});	
			AbstractDataSource source = (AbstractDataSource) c.newInstance(constrParams);
			
			ItemDescriptor[] converterDescriptors = readDescriptors("tests/converters.txt");
			ModelGenerator model = createModel(converterDescriptors, source.getAvailableColumns());
			source.setModel(model);
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size()", 3L, source.size());
			DataRow row;
			int n = 0;
			while ((row = source.getNextRow()) != null) {
				n++;
			}
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size() (source does not return number of rows it promised)", 3, n);
			source.reset();
			
			n = 0;
			while ((row = source.getNextRow()) != null) {
				for (int j = 0; j < converterDescriptors.length; j++) {
					Assert.assertEquals("ERROR in converter " + converterDescriptors[j].message, converterDescriptors[j].result[n], getResponse(row, model.getConverters()[j]));
				}
				n++;
			}
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in reset() (source does not return number of rows it promised after reset)", 3, n);
			source.close();
			row = null;
			Exception e = null;
			try {
				n = 0;
				while ((row = source.getNextRow()) != null) {
					n++;
				}
				Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in close() (source does not return size() records after closed and reopened)", 3, n);
			} catch (RJException ec) {
				e = ec;
			}
			Assert.assertTrue(sourceDescriptors[i].message + ": ERROR in close() (source should reopen after close and getNextRow were called)", row == null && e == null);
			System.out.println("...OK");
		}
	}
	
	private static ModelGenerator createModel(ItemDescriptor[] converterDescriptors, DataColumnDefinition[] cols) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, RJException {
		AbstractColumnConverter[] convs = new AbstractColumnConverter[converterDescriptors.length];
		System.out.println("\n      Tested converters:");
		for (int i = 0; i < convs.length; i++) {
			System.out.println("         " + converterDescriptors[i].message);
		}
		System.out.print("   Test starts");
		for (int i = 0; i < convs.length; i++) {
			if (converterDescriptors[i].className.endsWith("JoinConverter")) {
				String name = (String) converterDescriptors[i].basicProperties.get("name");
				DataColumnDefinition columns[] = readColumns((String)converterDescriptors[i].basicProperties.get("column"), cols);
				convs[i] = new JoinConverter(name, columns, converterDescriptors[i].properties);
			} else {
				Constructor c = Class.forName(converterDescriptors[i].className).getConstructor(new Class[] {String.class, Map.class, DataColumnDefinition.class});
				String name = (String) converterDescriptors[i].basicProperties.get("name");
				DataColumnDefinition col = search((String) converterDescriptors[i].basicProperties.get("column"), cols);
				convs[i] = (AbstractColumnConverter) c.newInstance(new Object[] {name, converterDescriptors[i].properties, col});
			}
		}
		return new ModelGenerator(convs);
	}

	private static DataColumnDefinition[] readColumns(String columnNames, DataColumnDefinition[] cols) {
		String[] columnNamesArray = columnNames.split(",");
		DataColumnDefinition[] columns = new DataColumnDefinition[columnNamesArray.length];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = search(columnNamesArray[i], cols);		
		}
		return columns;
	}

	private static DataColumnDefinition search(String string, DataColumnDefinition[] cols) {
		for (int i = 0; i < cols.length; i++) {
			if (cols[i].getColumnName().equals(string)) {
				return cols[i];
			}
		}
		return null;
	}

	private static String getResponse(DataRow row, AbstractColumnConverter conv) {
		StringBuffer response = new StringBuffer();
		for (int i = 0; i < conv.getOutputColumns().length; i++) {
			if (i > 0) {
				response.append(".");
			}
			response.append(row.getData(conv.getOutputColumns()[i]).getValue().toString());
		}
		return response.toString();
	}

	private static ItemDescriptor[] readDescriptors(String string) throws IOException {
		BufferedReader sources = new BufferedReader(new FileReader(string));
		String srcStr;
		List desc = new ArrayList();
		while ((srcStr = sources.readLine()) != null && !srcStr.trim().equals("")) {
			//System.out.println("Line: " + srcStr);
			ItemDescriptor descriptor = new ItemDescriptor(srcStr);
			desc.add(descriptor);
		}
		return (ItemDescriptor[])desc.toArray(new ItemDescriptor[] {});
	}

	private static class ItemDescriptor {
		
		String className;
		String message;
		Map properties;
		Map basicProperties;
		String[] result;
		
		public ItemDescriptor(String line) {
			Map props = parse(line);
			className = (String) props.get("class");
			message = (String) props.get("message");
			properties = parse((String)props.get("properties"));
			basicProperties = parse((String)props.get("basicProperties"));
			if (props.get("result") != null) {
				result = ((String)props.get("result")).split(",");
			}
		}
		
		private Map parse(String line) {
			if (line == null) {
				return null;
			}
			Map props = new HashMap();
			Pattern p = Pattern.compile("[a-zA-Z_0-9\\-]*=(\\[[^\\]]*\\]|\"[^\"]*\")");
			Matcher m = p.matcher(line);
			while (m.find()) {
				String match = m.group();
				int equals = match.indexOf('=');
				String name = match.substring(0, equals);
				props.put(name, match.substring(equals + 2, match.length() - 1));
			}
			return props;
		}
		
	}
	
}