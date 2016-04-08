package cdc.gui.components.uicomponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;

public class FileLocationPanel extends JPanel {

	public static final int SAVE = FileChoosingPanelFieldCreator.SAVE;
	public static final int OPEN = FileChoosingPanelFieldCreator.OPEN;
	
	private JTextField file;
	private JButton button;
	private int type;
	private JLabel fileLabel;
	
	public FileLocationPanel(String fLabel, String defFileName, int inputWidth, int type) {
		this.type = type;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		fileLabel = new JLabel(fLabel);
		file = new JTextField("", 20);
		button = new JButton("...");
		button.setPreferredSize(new Dimension(30, 20));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(new File("."));	
				int retVal = FileLocationPanel.this.type == FileChoosingPanelFieldCreator.OPEN ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					file.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		if (defFileName != null) {
			file.setText(defFileName);
		}
		add(fileLabel);
		add(file);
		add(button);
	}

	public FileLocationPanel(String fLabel, String defFileName, int inputWidth) {
		this(fLabel, defFileName, inputWidth, OPEN);
	}
	
	public FileLocationPanel(String fLabel, String defFileName) {
		this(fLabel, defFileName, 20, OPEN);
	}
	
	public FileLocationPanel(String fLabel) {
		this(fLabel, null);
	}

	public String getFileName() {
		return file.getText().trim();
	}
	
	public void setFileName(String loc) {
		file.setText(loc);
	}
	
	public void setEnabled(boolean enabled) {
		file.setEditable(enabled);
		button.setEnabled(enabled);
		fileLabel.setEnabled(enabled);
	}

	public void setError(boolean error) {
		if (error) {
			file.setBackground(Color.red);
		} else {
			file.setBackground(Color.white);
		}
	}

}
