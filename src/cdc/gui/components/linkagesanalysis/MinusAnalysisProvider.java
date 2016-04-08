package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.Filter;
import cdc.components.JoinListener;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.components.dialogs.OneTimeTipDialog;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesFrame;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesMultiFileFrame;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.impl.join.common.DataSourceNotJoinedJoinListener;
import cdc.utils.RJException;

public class MinusAnalysisProvider {
	
	private DataColumnDefinition[][] dataModel;
	private AbstractDataSource[] source;
	private String requiredSource;
	private String minusFiles[];
	private String windowTitle;
	
	public MinusAnalysisProvider(ConfiguredSystem system, int srcId) throws IOException, RJException {
		
		//If system is deduplication and srcID == -1, then this is deduplicated file request
		//If system is deduplication and srcID == 0, then this is duplicates file request
		
		List notJoinedListeners = new ArrayList();
		
		if (system.isDeduplication()) {
			if (srcId == 0) {
				OneTimeTipDialog.showInfoDialogIfNeeded("Duplicate records data", OneTimeTipDialog.DUPLICATES_VIEWER, OneTimeTipDialog.DUPLICATES_VIEWER_MESSAGE);
				windowTitle = "Duplicates";
				minusFiles = new String[] {system.getSourceA().getDeduplicationConfig().getMinusFile()};
			} else {
				OneTimeTipDialog.showInfoDialogIfNeeded("Deduplicated data", OneTimeTipDialog.DEDUPLICATED_VIEWER, OneTimeTipDialog.DEDUPLICATED_VIEWER_MESSAGE);
				windowTitle = "Deduplicated data";
				minusFiles = new String[] {system.getSourceA().getDeduplicationConfig().getDeduplicatedFileName()};
			}
			requiredSource = system.getSourceA().getSourceName();
		} else {
			OneTimeTipDialog.showInfoDialogIfNeeded("Not joined records data", OneTimeTipDialog.MINUS_VIEWER, OneTimeTipDialog.MINUS_VIEWER_MESSAGE);
			AbstractJoin join = system.getJoin();
			requiredSource = srcId == 1 ? join.getSourceA().getSourceName() : join.getSourceB().getSourceName();
			windowTitle = "Not joined records (" + requiredSource + ")";
			List listeners = join.getListeners();
			for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
				JoinListener listener = (JoinListener) iterator.next();
				if (listener instanceof DataSourceNotJoinedJoinListener) {
					//potential listener
					DataSourceNotJoinedJoinListener notJoinedListener = (DataSourceNotJoinedJoinListener)listener;
					AbstractDataSource src = notJoinedListener.getSource();
					if (src.getSourceName().equals(requiredSource)) {
						notJoinedListeners.add(notJoinedListener);
					}
				}
			}
			
			if (notJoinedListeners.size() == 0) {
				throw new RuntimeException("Proper join listener was not found. Required minus join listener for source " + requiredSource);
			}
			
			DataSourceNotJoinedJoinListener[] arrayListeners = (DataSourceNotJoinedJoinListener[]) notJoinedListeners.toArray(new DataSourceNotJoinedJoinListener[] {});
			minusFiles = new String[arrayListeners.length];
			for (int i = 0; i < minusFiles.length; i++) {
				minusFiles[i] = arrayListeners[i].getFileName();
			}
		}
		
		source = new AbstractDataSource[minusFiles.length];
		dataModel = new DataColumnDefinition[minusFiles.length][];
		for (int i = 0; i < minusFiles.length; i++) {
			Map props = new HashMap();
			props.put(CSVDataSource.PARAM_INPUT_FILE, minusFiles[i]);
			props.put(CSVDataSource.PARAM_DELIM, ",");
			source[i] = new CSVDataSource("m-" + requiredSource, props);
			source[i].setModel(new ModelGenerator(source[i].getAvailableColumns()));
			dataModel[i] = source[i].getDataModel().getOutputFormat();
		}
		
	}

	public JFrame getFrame() {
		if (source.length == 1) {
			JFrame frame = new ViewLinkagesFrame(new DataColumnDefinition[][] {dataModel[0]}, false, null, null, null, null, new ThreadCreator(0));
			frame.setTitle(windowTitle);
			return frame;
	 	} else {
	 		DataColumnDefinition[][][] model = new DataColumnDefinition[source.length][1][];
	 		for (int i = 0; i < model.length; i++) {
				model[i][0] = dataModel[i];
			}
	 		ThreadCreatorInterface[] ths = new ThreadCreatorInterface[source.length];
	 		for (int i = 0; i < ths.length; i++) {
				ths[i] = new ThreadCreator(i);
			}
	 		DataColumnDefinition[] conf = new DataColumnDefinition[ths.length];
	 		DataColumnDefinition[] strat = new DataColumnDefinition[ths.length];
	 		DataColumnDefinition[][][] compared = new DataColumnDefinition[ths.length][][];
	 		JFrame frame = new ViewLinkagesMultiFileFrame(minusFiles, model, true, conf, strat, compared, ths);
			frame.setTitle(windowTitle);
			return frame;
	 	}
	}
	
	private class ThreadCreator implements ThreadCreatorInterface {
		
		private int id;
		
		public ThreadCreator(int id) {
			this.id = id;
		}
		
		public LoadingThread createNewThread(ThreadCreatorInterface provider, LinkagesWindowPanel parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
			return new MinusLoadingThread(provider, parent, filter, sort, order);
		}

		public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException {
			AbstractDataSource src = source[id].copy();
			if (filter != null) {
				src.setFilter(filter);
			}
			return src;
		}
		
	}
	
}
