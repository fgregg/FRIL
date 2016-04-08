/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.components.AbstractJoin;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.progress.JoinDetailsPanel;
import cdc.gui.components.progress.JoinInfoPanel;
import cdc.gui.components.progress.ProgressDialog;
import cdc.gui.components.summary.SummaryWindow;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class ProcessPanel extends JPanel {
	
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

	private static final String STATUS_IDLE = "idle";
	private static final String STATUS_BUSY = "busy";
	
	private JLabel working = new JLabel(Configs.busyIcon);
	private JButton start = new JButton(Configs.playIcon);
	private JLabel status = new JLabel(STATUS_IDLE);
	
	private ConfiguredSystem system = new ConfiguredSystem(null, null, null, null);
	
	private ProgressDialog progressReporter;
	
	private SystemPanel systemPanel;
	
	private JoinThread thread;
	
	private SummaryWindow activeSummaryWindow = null;
	
	private DetailsActionListener detailsListener;
	
	public ProcessPanel(MainFrame frame, SystemPanel panel) {
		setLayout(null);
		setSize(240, 100);
		
		this.systemPanel = panel;
		
		start.setEnabled(false);
		start.setBorder(BorderFactory.createEmptyBorder());
		start.setBounds(30, 20, 42, 42);
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				progressReporter = new ProgressDialog(MainFrame.main, "Joining records...");
				JoinInfoPanel infoPanel = new JoinInfoPanel(progressReporter);
				thread = new JoinThread(system, infoPanel);
				progressReporter.setInfoPanel(infoPanel);
				progressReporter.addCancelListener(new CancelThreadListener(thread));
				if (detailsListener != null) {
					detailsListener.detailsPanel = null;
					detailsListener.dialog = null;
					detailsListener.join = null;
				}
				progressReporter.addDetailsActionListener(detailsListener = new DetailsActionListener(progressReporter, system.getJoin()));
				infoPanel.addPriorityListener(new ThreadPriorityChangedListener(thread));
				status.setText(STATUS_BUSY);
				working.setVisible(true);
				start.setEnabled(false);
				progressReporter.setLocation(GuiUtils.getCenterLocation(MainFrame.main, progressReporter));
				thread.start();
				progressReporter.setVisible(true);
				if (detailsListener.detailsPanel != null) {
					detailsListener.detailsPanel.windowClosed();
				}
				working.setVisible(false);
				status.setText(STATUS_IDLE);
				start.setEnabled(true);
				systemPanel.invalidate();
				systemPanel.repaint();
			}
		});
		
		status.setBounds(150, 27, 50, 25);
		JLabel label = new JLabel("Status: ");
		label.setBounds(105, 27, 50, 25);
		
		working.setVisible(false);
		working.setBounds(65, 20, 40, 40);
		
		JCheckBox summary = new JCheckBox("Enable system summary");
		summary.setOpaque(false);
		summary.setBounds(30, 60, 190, 20);
		summary.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				activeSummaryWindow = new SummaryWindow((JCheckBox)arg0.getSource());
				activeSummaryWindow.setCurrentSystem(system);
			}
		});
		
		add(start);
		add(status);
		add(working);
		add(label);
		add(summary);
		
		JLabel background = new JLabel(Configs.systemControlPanel);
		background.setLocation(0, 0);
		background.setSize(getSize());
		add(background);
	}

	public void setReady(boolean b) {
		start.setEnabled(b);
	}
	
	public void setConfiguredSystem(ConfiguredSystem system) {
		//System.out.println("System new");
		this.system = system;
		if (activeSummaryWindow != null && activeSummaryWindow.isVisible()) {
			activeSummaryWindow.setCurrentSystem(system);
		}
	}
	
	public void appendSummaryMessage(String msg) {
		thread.appendSummaryMessage(msg);
	}
	
}
