package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cdc.gui.components.uicomponents.FileLocationPanel;

public class RecordSavingPanel extends JPanel {

	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 1;
	
	private FileLocationPanel filePanel = new FileLocationPanel("File name:", "saved.csv", 20, FileLocationPanel.SAVE);
	private JRadioButton buttonAll;
	private JRadioButton buttonCur;
	
	public RecordSavingPanel() {
		
		setLayout(new GridBagLayout());
		
		filePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(filePanel, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
		JLabel saveType = new JLabel("Save:");
		buttonAll = new JRadioButton("All records");
		buttonCur = new JRadioButton("Only records from current page");
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonAll);
		group.add(buttonCur);
		buttonCur.setSelected(true);
		
		add(saveType, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(buttonAll, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(buttonCur, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
	}

	public String getFileName() {
		return filePanel.getFileName();
	}

	public boolean isSaveAll() {
		return buttonAll.isSelected();
	}

}
