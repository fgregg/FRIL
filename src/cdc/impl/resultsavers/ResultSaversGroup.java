package cdc.impl.resultsavers;

import java.io.IOException;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.datamodel.DataRow;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class ResultSaversGroup extends AbstractResultsSaver {

	private AbstractResultsSaver[] savers;
	
	public ResultSaversGroup(AbstractResultsSaver[] savers) {
		super(new HashMap());
		this.savers = savers;
	}

	public void close() throws IOException, RJException {
		for (int i = 0; i < savers.length; i++) {
			savers[i].close();
		}
	}

	public void flush() throws IOException, RJException {
		for (int i = 0; i < savers.length; i++) {
			savers[i].flush();
		}
	}

	public void reset() throws IOException, RJException {
		for (int i = 0; i < savers.length; i++) {
			savers[i].reset();
		}
	}

	public void saveRow(DataRow row) throws RJException, IOException {
		for (int i = 0; i < savers.length; i++) {
			savers[i].saveRow(row);
		}
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
		
		Element saversXML = DOMUtils.getChildElement(element, "savers");
		Element[] children = DOMUtils.getChildElements(saversXML);
		AbstractResultsSaver[] savers = new AbstractResultsSaver[children.length];
		for (int i = 0; i < savers.length; i++) {
			savers[i] = AbstractResultsSaver.fromXML(children[i]);
		}
		
		return new ResultSaversGroup(savers);
	}

	public AbstractResultsSaver[] getChildren() {
		return savers;
	}
}
