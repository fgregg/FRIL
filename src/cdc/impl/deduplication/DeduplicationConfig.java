package cdc.impl.deduplication;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.distance.EqualFieldsDistance;
import cdc.impl.distance.SoundexDistance;
import cdc.impl.join.blocking.EqualityHashingFunction;
import cdc.impl.join.blocking.HashingFunction;
import cdc.impl.join.blocking.SoundexHashingFunction;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class DeduplicationConfig {
	
	public static final String CONDITIONS_TAG = "deduplication-condition";
	public static final String CONDITION_TAG = "condition";
	private static final String COLUMN_TAG = "column";
	private static final String HASHING_FUNCTION_TAG = "hashing-function";
	private static final String COLUMNS_TAG = "columns";
	private static final String HASH_TAG = "hash";
	private static final String SOUNDEX_PREFIX = "soundex";
	private static final String EQUALITY_PREFIX = "equality";
	private DataColumnDefinition[] testedColumns;
	private AbstractDistance[] testCondition;
	
	private boolean hashing = false;
	private HashingFunction hashingFunction;
	
	private boolean snm = false;
	
	public DeduplicationConfig(DataColumnDefinition[] testedColumns, AbstractDistance[] distances) {
		this.testCondition = distances;
		this.testedColumns = testedColumns;
	}
	
	public DeduplicationConfig(AbstractDataSource dataSource) {
		ModelGenerator generator = dataSource.getDataModel();
		this.testedColumns = generator.getOutputFormat();
		this.testCondition = new AbstractDistance[this.testedColumns.length];
		for (int i = 0; i < testCondition.length; i++) {
			this.testCondition[i] = new EqualFieldsDistance();
		}
		this.hashingFunction = new EqualityHashingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {testedColumns[0]}});
	}

	public void setHashingConfig(HashingFunction function) {
		this.snm = false;
		this.hashing = true;
		this.hashingFunction = function;
	}

	public DataColumnDefinition[] getTestedColumns() {
		return testedColumns;
	}

	public AbstractDistance[] getTestCondition() {
		return testCondition;
	}

	public boolean isHashing() {
		return hashing;
	}

	public HashingFunction getHashingFunction() {
		return hashingFunction;
	}

	public boolean isSnm() {
		return snm;
	}
	
	public static DeduplicationConfig fromXML(AbstractDataSource source, Element dedupElement) throws RJException {
		Element cond = DOMUtils.getChildElement(dedupElement, CONDITIONS_TAG);
		Element[] children = DOMUtils.getChildElements(cond);
		DataColumnDefinition[] cols = new DataColumnDefinition[children.length];
		AbstractDistance[] dists = new AbstractDistance[children.length];
		for (int i = 0; i < children.length; i++) {
			Element child = children[i];
			cols[i] = source.getDataModel().getColumnByName(DOMUtils.getAttribute(child, COLUMN_TAG));
			Map params = Configuration.parseParams(DOMUtils.getChildElement(child, Configuration.PARAMS_TAG));
			String className = DOMUtils.getAttribute(child, Configuration.CLASS_ATTR);
			try {
				Class clazz = Class.forName(className);
				Constructor constr = clazz.getConstructor(new Class[] {Map.class});
				dists[i] = (AbstractDistance) constr.newInstance(new Object[] {params});
			} catch (Exception e) {
				throw new RJException("Error reading join configuration", e);
			}
		}
		Element hashingFunct = DOMUtils.getChildElement(dedupElement, HASHING_FUNCTION_TAG);
		String function = DOMUtils.getAttribute(hashingFunct, HASH_TAG);
		String columns = DOMUtils.getAttribute(hashingFunct, COLUMNS_TAG);
		HashingFunction blockingFunction;
		if (function.startsWith(SOUNDEX_PREFIX)) {
			String paramsStr = function.substring(function.indexOf("(") + 1, function.length()-1);
			blockingFunction = new SoundexHashingFunction(new DataColumnDefinition[][] {decode(columns, source), decode(columns, source)}, Integer.parseInt(paramsStr));
		} else if (function.startsWith(EQUALITY_PREFIX)) {
			blockingFunction = new EqualityHashingFunction(new DataColumnDefinition[][] {decode(columns, source), decode(columns, source)});
		} else {
			throw new RuntimeException("Property " + HASH_TAG + " accepts only soundex or equality options.");
		}
		DeduplicationConfig config = new DeduplicationConfig(cols, dists);
		config.setHashingConfig(blockingFunction);
		return config;
	}

	private static DataColumnDefinition[] decode(String columns, AbstractDataSource source) {
		String[] cols = columns.split(",");
		DataColumnDefinition[] colDefs = new DataColumnDefinition[cols.length];
		for (int i = 0; i < colDefs.length; i++) {
			colDefs[i] = source.getDataModel().getColumnByName(cols[i]);
		}
		return colDefs;
	}

	public void saveToXML(Document doc, Element dedupElement) {
		Element cond = DOMUtils.createChildElement(doc, dedupElement, CONDITIONS_TAG);
		for (int i = 0; i < testedColumns.length; i++) {
			Element condition = DOMUtils.createChildElement(doc, cond, CONDITION_TAG);
			DOMUtils.setAttribute(condition, Configuration.CLASS_ATTR, testCondition[i].getClass().getName());
			DOMUtils.setAttribute(condition, COLUMN_TAG, testedColumns[i].getColumnName());
			Configuration.appendParams(doc, condition, testCondition[i].getProperties());
		}
		
		Element hashingFunct = DOMUtils.createChildElement(doc, dedupElement, HASHING_FUNCTION_TAG);
		DOMUtils.setAttribute(hashingFunct, COLUMNS_TAG, encode(hashingFunction.getColumns()[0]));
		if (hashingFunction instanceof SoundexHashingFunction) {
			SoundexHashingFunction shf = (SoundexHashingFunction)hashingFunction;
			DOMUtils.setAttribute(hashingFunct, HASH_TAG, SOUNDEX_PREFIX + "(" + shf.getSoundexDistance().getProperty(SoundexDistance.PROP_SIZE) + ")");
		} else {
			DOMUtils.setAttribute(hashingFunct, HASH_TAG, EQUALITY_PREFIX);
		}
			
	}

	private String encode(DataColumnDefinition[] dataColumnDefinitions) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < dataColumnDefinitions.length; i++) {
			if (i != 0) {
				buffer.append(",");
			}
			buffer.append(dataColumnDefinitions[i].getColumnName());
		}
		return buffer.toString();
	}

	public void fixIfNeeded(AbstractDataSource originalDataSource) {
		DataColumnDefinition[] sourceColumns = originalDataSource.getDataModel().getOutputFormat();
		int nulls = 0;
		main: for (int i = 0; i < testedColumns.length; i++) {
			DataColumnDefinition col = testedColumns[i];
			for (int j = 0; j < sourceColumns.length; j++) {
				if (col != null && col.equals(sourceColumns[j])) {
					continue main;
				}
			}
			testedColumns[i] = null;
			testCondition[i] = null;
			nulls++;
		}
		
		int skipped = 0;
		if (nulls != 0) {
			DataColumnDefinition[] newTestedColumns = new DataColumnDefinition[testedColumns.length - nulls];
			AbstractDistance[] newTestCondition = new AbstractDistance[testCondition.length - nulls];
			//clean arrays
			for (int i = 0; i < testedColumns.length; i++) {
				if (testedColumns[i] == null) {
					skipped++;
				} else {
					newTestedColumns[i - skipped] = testedColumns[i];
					newTestCondition[i - skipped] = testCondition[i];
				}
			}
			testedColumns = newTestedColumns;
			testCondition = newTestCondition; 
		}
		
	}
	
}
