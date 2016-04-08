package cdc.gui.components.uicomponents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.gui.Configs;

public class InformationPanel extends JPanel {
	
	public InformationPanel(String information) {
		
		String[] lines = information.split("\n");
		
		setLayout(new GridBagLayout());		

		JLabel icon = new JLabel(Configs.warnIcon);
		add(icon, new GridBagConstraints(0, 0, 1, lines.length, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		for (int i = 0; i < lines.length; i++) {
			JLabel label = new JLabel(lines[i], JLabel.LEFT);
			label.setEnabled(false);
			add(label, new GridBagConstraints(1, i, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		}
		
	}
}
