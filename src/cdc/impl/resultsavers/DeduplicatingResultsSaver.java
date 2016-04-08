package cdc.impl.resultsavers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.PropertyBasedColumn;
import cdc.impl.MainApp;
import cdc.impl.datasource.wrappers.SortedData;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.comparators.StringComparator;
import edu.emory.mathcs.util.xml.DOMUtils;

public class DeduplicatingResultsSaver extends AbstractResultsSaver {

	private static final String DEDUPE_SRC = "dedupe-results";
	private static CompareFunctionInterface[] comps = new CompareFunctionInterface[] {new StringComparator()};
	
	private class SortedDataDataWriter implements DataWriter {
		private SortedData data;
		public SortedDataDataWriter(SortedData data) {
			this.data = data;
		}
		public void finish() throws IOException, RJException {
			data.complete();
		}
		public void writeRow(DataRow row) throws IOException {
			data.addRow(row);
		}
	}
	
	private class ResultsSaverDataWriter implements DataWriter {
		public void finish() throws IOException, RJException {
		}
		public void writeRow(DataRow row) throws IOException, RJException {
			savedCnt++;
			for (int i = 0; i < savers.length; i++) {
				savers[i].saveRow(row);
			}
		}
	}
	
	private int activePhase = 0;
	private DataColumnDefinition[] sortPhases;
	private boolean deleteDuplicates = false;
	
	private int duplicatesCnt = 0;
	private int savedCnt = 0;
	
	private AbstractResultsSaver[] savers;
	
	//internal writer
	private SortedData data;
	
	public DeduplicatingResultsSaver(AbstractResultsSaver[] savers, Map props) throws IOException {
		super(props);
		this.savers = savers;
		
		String dedupeConfig = null;
		if ((dedupeConfig = getProperty("deduplication")) != null) {
			//possible options: left, right, both
			if (dedupeConfig.equals("left")) {
				sortPhases = new DataColumnDefinition[1];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCA_ID, DEDUPE_SRC);
			} else if (dedupeConfig.equals("right")) {
				sortPhases = new DataColumnDefinition[1];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCB_ID, DEDUPE_SRC);
			} else {
				//assume both
				sortPhases = new DataColumnDefinition[2];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCA_ID, DEDUPE_SRC);
				sortPhases[1] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCB_ID, DEDUPE_SRC);
			}
		} else {
			throw new RuntimeException("DeduplicationResultsSaver requires attrubute 'deduplication'");
		}
		deleteDuplicates = "true".equals(getProperty("delete-duplicates"));
	}

	public void close() throws IOException, RJException {
		if (data != null) {
			data.cleanup();
		}
		data = null;
		for (int i = 0; i < savers.length; i++) {
			savers[i].close();
		}
	}

	private void doDeduplication() throws IOException, RJException {
		DataWriter writer;
		SortedData nextPhaseData = null;
		while (activePhase <= sortPhases.length) {
			Log.log(getClass(), "Results deduplication, phase " + activePhase + " out of " + sortPhases.length);
			if (activePhase < sortPhases.length) {
				nextPhaseData = new SortedData(DEDUPE_SRC, new DataColumnDefinition[] {sortPhases[activePhase]}, comps);
				writer = new SortedDataDataWriter(nextPhaseData);
			} else {
				writer = new ResultsSaverDataWriter();
			}
			identifyDuplicates(data, writer, sortPhases[activePhase - 1]);
			data.cleanup();
			data = nextPhaseData;
			activePhase++;
		}
	}

	public void flush() throws IOException, RJException {
		Log.log(getClass(), "Results deduplication starts.");
		
		doDeduplication();
		
		Log.log(getClass(), "Removed " + duplicatesCnt + " duplicate(s) from results.");
		MainApp.main.appendLinkageSummary("\nResults deduplication identified " + duplicatesCnt + " duplicate(s).\n");
		MainApp.main.appendLinkageSummary("Saved " + savedCnt + " linkage(s) to result files.\n");
		
		if (data != null) {
			data.cleanup();
		}
		
		for (int i = 0; i < savers.length; i++) {
			savers[i].flush();
		}
	}

	public void reset() throws IOException, RJException {
		if (data != null) {
			data.cleanup();
		}
		activePhase = 0;
		data = null;
		duplicatesCnt = 0;
		savedCnt = 0;
		for (int i = 0; i < savers.length; i++) {
			savers[i].reset();
		}
	}

	public void saveRow(DataRow row) throws RJException, IOException {
		if (data == null) {
			data = new SortedData(DEDUPE_SRC, new DataColumnDefinition[] {sortPhases[activePhase++]}, comps);
		}
		data.addRow(row);
	}

	public void saveToXML(Document doc, Element saver) {
		Configuration.appendParams(doc, saver, getProperties());
		Element saversXML = DOMUtils.createChildElement(doc, saver, "savers");
		for (int i = 0; i < savers.length; i++) {
			Element s = DOMUtils.createChildElement(doc, saversXML, Configuration.RESULTS_SAVER_TAG);
			DOMUtils.setAttribute(s, Configuration.CLASS_ATTR, savers[i].getClass().getName());
			savers[i].saveToXML(doc, s);
		}
		
	}
	
	public static AbstractResultsSaver fromXML(Element element) throws RJException, IOException {
		Element paramsElement = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsElement);
		
		Element saversXML = DOMUtils.getChildElement(element, "savers");
		Element[] children = DOMUtils.getChildElements(saversXML);
		AbstractResultsSaver[] savers = new AbstractResultsSaver[children.length];
		for (int i = 0; i < savers.length; i++) {
			savers[i] = AbstractResultsSaver.fromXML(children[i]);
		}
		
		return new DeduplicatingResultsSaver(savers, params);
	}

	public AbstractResultsSaver[] getChildren() {
		return savers;
	}
	
	private void identifyDuplicates(SortedData inputData, DataWriter outputData, DataColumnDefinition sortedKey) throws IOException, RJException {
		DataRow example = null;
		List buffer = new ArrayList();
		example = inputData.getNextSortedRow();
		//buffer.add(example);
		while (true) {
			if (example == null) {
				break;
			}
			buffer.add(example);
			DataRow row;
			while (isTheSameKey(row = inputData.getNextSortedRow(), example, sortedKey) && row != null) {
				buffer.add(row);
			}
			solveGroup(outputData, buffer, sortedKey);
			buffer.clear();
			example = row;
		}
	}

	private void solveGroup(DataWriter saver, List buffer, DataColumnDefinition sortedKey) throws RJException, IOException {
		int maxConfidence = 0;
		int cnt = 0;
		for (Iterator iterator = buffer.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			int conf = Integer.parseInt(row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE));
			if (maxConfidence < conf) {
				maxConfidence = conf;
				cnt = 0;
			} else if (maxConfidence == conf) {
				cnt++;
			}
		}
		boolean first = true;
		for (Iterator iterator = buffer.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			if ((first || !deleteDuplicates) && maxConfidence == Integer.parseInt(row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE))) {
				first = false;
				saver.writeRow(row);
			} else {
				duplicatesCnt++;
			}
		}
	}

	private boolean isTheSameKey(DataRow r1, DataRow r2, DataColumnDefinition sortedKey) {
		if (r1 == null || r2 == null) {
			return false;
		}
		return r1.getData(sortedKey).getValue().equals(r2.getData(sortedKey).getValue());
	}

}
