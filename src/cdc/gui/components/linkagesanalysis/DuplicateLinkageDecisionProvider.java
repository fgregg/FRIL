package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.PropertyBasedColumn;
import cdc.gui.MainFrame;
import cdc.gui.components.linkagesanalysis.dialog.DecisionListener;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.RJException;

public class DuplicateLinkageDecisionProvider implements ThreadCreatorInterface {

	private DataColumnDefinition[][] dataModel;
	private DataColumnDefinition confidence;
	private DataColumnDefinition stratum;
	private DataColumnDefinition[][] comparedColumns;
	
	private ViewLinkagesDialog dialog;
	private List internalData = new ArrayList();
	
	private DuplicateLinkageLoadingThread activeThread;
	
	public DuplicateLinkageDecisionProvider(DecisionListener decisionListener) {
		
		AbstractJoin join = MainFrame.main.getJoin().getJoin();
		dataModel = readModel(join.getOutColumns(), join.getJoinCondition());
		
		AbstractJoinCondition cond = join.getJoinCondition();
		comparedColumns = new DataColumnDefinition[2][cond.getDistanceFunctions().length];
		for (int i = 0; i < comparedColumns[0].length; i++) {
			comparedColumns[0][i] = cond.getLeftJoinColumns()[i];
			comparedColumns[1][i] = cond.getRightJoinColumns()[i];
		}
		
		comparedColumns = removeNotAvailableColumns(comparedColumns, dataModel);
		
		//do all the transformations of data column definitions...
		confidence = new PropertyBasedColumn(AbstractJoin.PROPERTY_CONFIDNCE, "src", "Confidence");
		if (join instanceof StrataJoinWrapper) {
			stratum = new PropertyBasedColumn(StrataJoinWrapper.PROPERTY_STRATUM_NAME, "src", "Stratum name");
		}
		
		dialog = new ViewLinkagesDialog(dataModel, true, confidence, stratum, comparedColumns, this, true);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addDecisionListener(decisionListener);
		dialog.addDecisionListener(new DecisionListener() {
			public void linkageAccepted(DataRow linkage) {
				activeThread.removeLinkage(linkage);
			}
			public void linkageRejected(DataRow linkage) {
				activeThread.removeLinkage(linkage);
			}
		});
		new Thread() {
		 public void run() {
			 dialog.setVisible(true);
		 }
		}.start();
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
	
	public LoadingThread createNewThread(ThreadCreatorInterface provider, ViewLinkagesDialog parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
		return activeThread = new DuplicateLinkageLoadingThread(internalData, provider, parent, filter, sort, order);
	}

	public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException {
		return null;
	}
	
	public void addUndecidedRecords(DataRow[] linkages) {
		synchronized (internalData) {
			internalData.addAll(Arrays.asList(linkages));
			internalData.notifyAll();
		}
	}

	public boolean isDone() {
		synchronized (internalData) {
			return internalData.isEmpty();
		}
	}
	
	public void closeDecisionWindow() {
		dialog.setVisible(false);
	}

}
