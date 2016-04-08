package cdc.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class AboutWindow extends JDialog {
	
	public AboutWindow() {
		super(MainFrame.main);
		setModal(true);
		setTitle("About FRIL");
		setSize(600, 400);
		setLocationRelativeTo(MainFrame.main);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JPanel main = new JPanel(new GridBagLayout());
		main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(main);
		
		main.add(new JLabel("FRIL: A Fine-Grained Record Integration and Linkage Tool"), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("Version: " + MainFrame.main.getPropertiesVersion().getProperty(MainFrame.VERSION_PROPERTY_V)), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("Author: Pawel Jurczyk"), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("List of changes:"), new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		
		String historyText = readHistoryText(MainFrame.VERSION_LIST_OF_CHANGES_FILE);
		JTextArea area = new JTextArea(historyText);
		area.setEditable(false);
		JScrollPane scroll = new JScrollPane(area);
		main.add(scroll, new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}

	private String readHistoryText(String versionListOfChangesFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(versionListOfChangesFile));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			return builder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "Error reading changelog file: " + versionListOfChangesFile;
		}
	}
	
}
