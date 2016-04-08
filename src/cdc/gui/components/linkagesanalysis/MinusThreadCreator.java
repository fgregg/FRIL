package cdc.gui.components.linkagesanalysis;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.utils.RJException;

public class MinusThreadCreator implements ThreadCreatorInterface {

	public LoadingThread createNewThread(ThreadCreatorInterface provider, ViewLinkagesDialog parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
		// TODO Auto-generated method stub
		return null;
	}

	public AbstractDataSource getDataSource(Filter filter) throws IOException,
			RJException {
		// TODO Auto-generated method stub
		return null;
	}


}
