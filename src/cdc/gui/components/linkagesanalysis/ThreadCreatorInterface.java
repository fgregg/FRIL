package cdc.gui.components.linkagesanalysis;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.utils.RJException;

public interface ThreadCreatorInterface {
	
	public LoadingThread createNewThread(ThreadCreatorInterface provider, LinkagesWindowPanel parent, Filter filter, DataColumnDefinition[] sort, int[] order);
	public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException;
	
}
