package cdc.components;

import java.util.ArrayList;
import java.util.List;

import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.DuplicateLinkageDecisionProvider;
import cdc.gui.components.linkagesanalysis.dialog.DecisionListener;

public class ManualDecisionModule implements DecisionListener {
	
	private DuplicateLinkageDecisionProvider decisionWindowProvider;
	
	private List toBeDecided = new ArrayList(); 
	private List decided = new ArrayList();
	
	public ManualDecisionModule() {
	}
	
	public void addRow(DataRow row) {
		//System.out.println("Adding to manual decision: " + row);
		synchronized (toBeDecided) {
			if (decisionWindowProvider == null) {
				decisionWindowProvider = new DuplicateLinkageDecisionProvider("Linkages - manual decision", this);
			}
			toBeDecided.add(row);
		}
		decisionWindowProvider.addUndecidedRecords(new DataRow[] {row});
	}
	
	public ManualDecision getNextDecidedRow() {
		try {
			synchronized (toBeDecided) {
				while (true) {
					if (decided.size() != 0) {
						ManualDecision row = (ManualDecision) decided.remove(0);
						toBeDecided.remove(row.row);
						//System.out.println("size=" + toBeDecided.size());
						return row;
					} else if (toBeDecided.size() == 0) {
						if (decisionWindowProvider != null) {
							decisionWindowProvider.closeDecisionWindow();
							decisionWindowProvider = null;
						}
						return null;
					} else {
						toBeDecided.wait();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public class ManualDecision {

		private DataRow row;
		private boolean accepted;
		
		public ManualDecision(DataRow linkage, boolean b) {
			row = linkage;
			accepted = b;
		}

		public boolean isAccepted() {
			return accepted;
		}

		public DataRow getRow() {
			return row;
		}
		
	}

	public void linkageAccepted(DataRow linkage) {
		//RowUtils.linkageManuallyAccepted(linkage);
		synchronized (toBeDecided) {
			decided.add(new ManualDecision(linkage, true));
			toBeDecided.notifyAll();
		}
	}

	public void linkageRejected(DataRow linkage) {
		//RowUtils.linkageManuallyRejected(linkage);
		synchronized (toBeDecided) {
			decided.add(new ManualDecision(linkage, false));
			toBeDecided.notifyAll();
		}
	}
	
}
