package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;

public class ViewLinkagesDialog extends JDialog {
	
	private LinkagesWindowPanel panel;
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, AbstractDistance[] dst, ThreadCreatorInterface threadCreator) {
		this(columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, dst, threadCreator, false);
	}
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, AbstractDistance[] dst, ThreadCreatorInterface threadCreator, boolean acceptRejectOption) {
	
		setModal(true);
	
		panel = new LinkagesWindowPanel(this, columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, dst, threadCreator, acceptRejectOption);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					return;
				}
				panel.cancelThread();
			}
			public void windowClosed(WindowEvent e) {
				panel.cancelThread();
			}
		});
		
		setSize(700, 500);
		setLayout(new BorderLayout());
		//add(panel.getToolBar(), BorderLayout.PAGE_START);
		add(panel, BorderLayout.CENTER);
		//add(panel.getStatusBar(), BorderLayout.SOUTH);
		
		setLocationRelativeTo(null);
		
	}

	public LinkagesWindowPanel getLinkageWindowPanel() {
		return panel;
	}
	
}