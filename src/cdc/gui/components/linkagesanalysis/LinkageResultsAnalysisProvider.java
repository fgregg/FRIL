package cdc.gui.components.linkagesanalysis;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.AbstractResultsSaver;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.MainFrame;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;
import cdc.impl.resultsavers.ResultSaversGroup;
import cdc.utils.RJException;

public class LinkageResultsAnalysisProvider implements ThreadCreatorInterface {
	
	private DataColumnDefinition[][] dataModel;
	private DataColumnDefinition confidence;
	private DataColumnDefinition stratum;
	private DataColumnDefinition[][] comparedColumns;
	
	private CSVFileSaver fileSaver;
	
	private AbstractDataSource source;
	
	public LinkageResultsAnalysisProvider(AbstractJoin join) throws IOException, RJException {
		
		dataModel = readModel(join.getOutColumns(), join.getJoinCondition());
		AbstractJoinCondition cond = join.getJoinCondition();
		comparedColumns = new DataColumnDefinition[2][cond.getDistanceFunctions().length];
		for (int i = 0; i < comparedColumns[0].length; i++) {
			comparedColumns[0][i] = cond.getLeftJoinColumns()[i];
			comparedColumns[1][i] = cond.getRightJoinColumns()[i];
		}
		
		fileSaver = getRersultSaver();
		
		if (fileSaver == null) {
			throw new RJException("Cannot open linkage results file.\nMake sure at least one of your results saver saves data into a file.");
		}
		
		String file = fileSaver.getProperty(CSVFileSaver.OUTPUT_FILE_PROPERTY);
		Map props = new HashMap();
		props.put(CSVDataSource.PARAM_INPUT_FILE, file);
		props.put(CSVDataSource.PARAM_DELIM, ",");
		source = new CSVDataSource("results", props);
		source.setModel(new ModelGenerator(source.getAvailableColumns()));
		
		//do all the transformations of data column definitions...
		DataColumnDefinition[] fileColumns = getDataColumnDefinitions();
		confidence = getConfidence(fileColumns);
		stratum = getStratum(fileColumns);
		
		comparedColumns = removeNotAvailableColumns(comparedColumns, dataModel);
		
		dataModel[0] = matchColumns(dataModel[0], fileColumns);
		dataModel[1] = matchColumns(dataModel[1], fileColumns);
		comparedColumns[0] = matchColumns(comparedColumns[0], fileColumns);
		comparedColumns[1] = matchColumns(comparedColumns[1], fileColumns);
		
	}
	
	private DataColumnDefinition[][] removeNotAvailableColumns(DataColumnDefinition[][] compared, DataColumnDefinition[][] model) {
		List lList = new ArrayList();
		List rList = new ArrayList();
		for (int i = 0; i < compared[0].length; i++) {
			if (isAvailable(compared[0][i], model[0]) && isAvailable(compared[1][i], model[1])) {
				lList.add(compared[0][i]);
				rList.add(compared[1][i]);
			}
		}
		return new DataColumnDefinition[][] {(DataColumnDefinition[]) lList.toArray(new DataColumnDefinition[] {}), (DataColumnDefinition[]) rList.toArray(new DataColumnDefinition[] {})};
	}
	
	private boolean isAvailable(DataColumnDefinition dataColumnDefinition, DataColumnDefinition[] dataColumnDefinitions) {
		for (int i = 0; i < dataColumnDefinitions.length; i++) {
			if (dataColumnDefinition.equals(dataColumnDefinitions[i])) {
				return true;
			}
		}
		return false;
	}
	
	private DataColumnDefinition getConfidence(DataColumnDefinition[] fileColumns) {
		for (int i = 0; i < fileColumns.length; i++) {
			if (fileColumns[i].getColumnName().equals("Confidence")) {
				return fileColumns[i];
			}
		}
		return null;
	}
	
	private DataColumnDefinition getStratum(DataColumnDefinition[] fileColumns) {
		for (int i = 0; i < fileColumns.length; i++) {
			if (fileColumns[i].getColumnName().equals("Stratum name")) {
				return fileColumns[i];
			}
		}
		return null;
	}

	private DataColumnDefinition[][] readModel(DataColumnDefinition[] outColumns, AbstractJoinCondition cond) {
		String src1 = cond.getLeftJoinColumns()[0].getSourceName();
		List l1 = new ArrayList();
		List l2 = new ArrayList();
		for (int i = 0; i < outColumns.length; i++) {
			if (outColumns[i].getSourceName().equals(src1)) {
				l1.add(outColumns[i]);
			} else {
				l2.add(outColumns[i]);
			}
		}
		return new DataColumnDefinition[][] {(DataColumnDefinition[])l1.toArray(new DataColumnDefinition[] {}), (DataColumnDefinition[])l2.toArray(new DataColumnDefinition[] {})};
	}

	private DataColumnDefinition[] matchColumns(DataColumnDefinition[] model, DataColumnDefinition[] fileColumns) {
		List out = new ArrayList();
		for (int i = 0; i < model.length; i++) {
			for (int j = 0; j < fileColumns.length; j++) {
				if (fileColumns[j].getColumnName().equals(model[i].toString())) {
					out.add(fileColumns[j]);
					break;
				}
			}
		}
		return (DataColumnDefinition[]) out.toArray(new DataColumnDefinition[] {});
	}

	public JDialog getDialogWindow() {
		ViewLinkagesDialog viewLinkagesDialog = new ViewLinkagesDialog(dataModel, false, confidence, stratum, comparedColumns, this);
		viewLinkagesDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				close();
			}
			public void windowClosing(WindowEvent e) {
				close();
			}
			private void close() {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (RJException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					source = null;
				}
			}
		});
		return viewLinkagesDialog;
	}
	
	public LoadingThread createNewThread(ThreadCreatorInterface provider, ViewLinkagesDialog parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
		return new LinkageLoadingThread(provider, parent, filter, sort, order);
	}

	public DataColumnDefinition[] getDataColumnDefinitions() throws IOException, RJException {
		return source.getDataModel().getOutputFormat();
	}
	
	private CSVFileSaver getRersultSaver() {
		AbstractResultsSaver saver = MainFrame.main.getJoin().getResultSaver();
		if (saver instanceof CSVFileSaver) {
			CSVFileSaver fileSaver = (CSVFileSaver)saver;
			return fileSaver;
		} else if (saver instanceof ResultSaversGroup) {
			ResultSaversGroup group = (ResultSaversGroup)saver;
			AbstractResultsSaver[] children = group.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof CSVFileSaver) {
					return (CSVFileSaver)children[i];
				}
			}
		} else if (saver instanceof DeduplicatingResultsSaver) {
			DeduplicatingResultsSaver dedupe = (DeduplicatingResultsSaver)saver;
			AbstractResultsSaver[] children = dedupe.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof CSVFileSaver) {
					return (CSVFileSaver)children[i];
				}
			}
		}
		return null;
	}

	public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException {
		AbstractDataSource source = this.source.copy();
		if (filter != null) {
			source.setFilter(filter);
		}
		return source;
	}

}
