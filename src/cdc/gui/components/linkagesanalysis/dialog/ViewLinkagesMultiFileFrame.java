package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;

public class ViewLinkagesMultiFileFrame extends JFrame {

	private LinkagesWindowPanel[] panels;
	
	public ViewLinkagesMultiFileFrame(String[] tabNames, DataColumnDefinition[][][] columns, boolean showDataSourceName, DataColumnDefinition[] confidenceColumn, DataColumnDefinition[] stratum, DataColumnDefinition[][][] comparedColumns, ThreadCreatorInterface[] threadCreator) {
		this(tabNames, columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, threadCreator, false);
	}
	
	public ViewLinkagesMultiFileFrame(String[] tabNames, DataColumnDefinition[][][] columns, boolean showDataSourceName, DataColumnDefinition[] confidenceColumn, DataColumnDefinition stratum[], DataColumnDefinition[][][] comparedColumns, ThreadCreatorInterface[] threadCreator, boolean acceptRejectOption) {
	
		super.setIconImage(Configs.appIcon);
		
		//create multiple panels...
		panels = new LinkagesWindowPanel[tabNames.length];
		for (int i = 0; i < threadCreator.length; i++) {
			panels[i] = new LinkagesWindowPanel(this, columns[i], showDataSourceName, confidenceColumn[i], stratum[i], comparedColumns[i], null, threadCreator[i], acceptRejectOption);
		}
	
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					return;
				}
				for (int i = 0; i < panels.length; i++) {
					panels[i].cancelThread();
				}
			}
			public void windowClosed(WindowEvent e) {
				for (int i = 0; i < panels.length; i++) {
					panels[i].cancelThread();
				}
			}
		});
		
		setSize(700, 550);
		setLayout(new BorderLayout());
		
		JTabbedPane tabs = new JTabbedPane();
		for (int i = 0; i < panels.length; i++) {
			panels[i].setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			tabs.addTab(tabNames[i], panels[i]);
		}
		add(tabs, BorderLayout.CENTER);
		
		setLocationRelativeTo(null);
		
	}

}
