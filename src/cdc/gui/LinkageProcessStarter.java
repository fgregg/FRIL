package cdc.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;

import cdc.components.AbstractJoin;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.progress.JoinDetailsPanel;
import cdc.gui.components.progress.JoinInfoPanel;
import cdc.gui.components.progress.ProgressDialog;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class LinkageProcessStarter implements ProcessStarterInterface {

	private ProgressDialog progressReporter;
	private LinkageThread thread;
	private DetailsActionListener detailsListener;
	
	public void startProcessAndWait(ConfiguredSystem system) {
		
		progressReporter = new ProgressDialog(MainFrame.main, "Joining records...");
		JoinInfoPanel infoPanel = new JoinInfoPanel(progressReporter);
		thread = new LinkageThread(system, infoPanel);
		progressReporter.setInfoPanel(infoPanel);
		progressReporter.addCancelListener(new CancelThreadListener(thread));
		if (detailsListener != null) {
			detailsListener.detailsPanel = null;
			detailsListener.dialog = null;
			detailsListener.join = null;
		}
		progressReporter.addDetailsActionListener(detailsListener = new DetailsActionListener(progressReporter, system.getJoin()));
		infoPanel.addPriorityListener(new ThreadPriorityChangedListener(thread));
		progressReporter.setLocation(GuiUtils.getCenterLocation(MainFrame.main, progressReporter));
		thread.start();
		progressReporter.setVisible(true);
		if (detailsListener.detailsPanel != null) {
			detailsListener.detailsPanel.windowClosed();
		}
		
	}
	
	public class DetailsActionListener implements ActionListener {

		private AbstractJoin join;
		private ProgressDialog dialog;
		private JoinDetailsPanel detailsPanel;
		
		public DetailsActionListener(ProgressDialog dialog, AbstractJoin join) {
			this.join = join;
			this.dialog = dialog;
		}

		public void actionPerformed(ActionEvent e) {
			if (!dialog.isDetailsOn()) {
				join.getJoinCondition().setCanUseOptimisticEval(false);
				try {
					if (detailsPanel != null) {
						detailsPanel.windowClosed();
					}
					detailsPanel = new JoinDetailsPanel(join);
					detailsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
					dialog.setDetailsPanel(detailsPanel);
				} catch (RJException ex) {
					JXErrorDialog.showDialog(MainFrame.main, "Error registering join listener", ex);
				}
			} else {
				if (detailsPanel != null) {
					try {
						detailsPanel.close();
					} catch (RJException e1) {
						e1.printStackTrace();
						JXErrorDialog.showDialog(MainFrame.main, "Error", e1);
					}
					join.getJoinCondition().setCanUseOptimisticEval(true);
				}
			}
		}

	}

//	public void appendSummaryMessage(String msg) {
//		thread.appendSummaryMessage(msg);
//	}

}
