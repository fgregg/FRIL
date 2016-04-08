package cdc.impl.resultsavers;

import java.io.IOException;

import cdc.datamodel.DataRow;
import cdc.utils.RJException;

public interface DataWriter {

	public void writeRow(DataRow row) throws IOException, RJException;
	public void finish() throws IOException, RJException;
	
}
