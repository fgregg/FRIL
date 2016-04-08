package cdc.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class ConvertDataSource {
	public static void main(String[] args) throws RJException, IOException, SAXException {
		
		if (args.length != 2) {
			System.out.println("Usage: ConvertDataSource xml-source-config out-csv-file");
		}
		
		String fileName = args[0];
		String outFile = args[1];
		
		DocumentBuilder builder = DOMUtils.createDocumentBuilder(false, false);
		Document doc = builder.parse(fileName);
		Node baseNode = doc.getDocumentElement();
		AbstractDataSource source = AbstractDataSource.fromXML((Element)baseNode);
		if (source.getDataModel() == null) {
			source.setModel(new ModelGenerator(source.getAvailableColumns()));
		}
		Map props = new HashMap();
		props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, outFile);
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
		AbstractResultsSaver saver = new CSVFileSaver(props);
		DataRow row;
		while ((row = source.getNextRow()) != null) {
			saver.saveRow(row);
		}
		saver.close();
		source.close();
	}
}
