package cdc.datamodel.converters.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.codehaus.janino.ScriptEvaluator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ScriptPanel extends JPanel {

	private RSyntaxTextArea scriptTextArea;
	private RTextScrollPane scriptScrollPane;
	
	private Class output;
	private Class[] paramTypes;
	private String[] paramNames;
	
	public ScriptPanel(String script, Class output, String[] attributes, Class[] attrTypes) {
		this.output = output;
		this.paramNames = attributes;
		this.paramTypes = attrTypes;
		
		setLayout(new GridBagLayout());
		
		scriptTextArea = new RSyntaxTextArea();
		scriptTextArea.restoreDefaultSyntaxHighlightingColorScheme();
		scriptTextArea.setSyntaxEditingStyle(SyntaxConstants.JAVA_SYNTAX_STYLE);
		scriptTextArea.setText(script);
		scriptScrollPane = new RTextScrollPane(0, 0, scriptTextArea, true);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		add(scriptScrollPane, c);
		
		JButton button = new JButton("Validate");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validateScript();
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(button);
		c = new GridBagConstraints();
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		add(buttonPanel, c);
	}
	
	public String getScript() {
		return scriptTextArea.getText();
	}
	
	public void validateScript() {
		try {
			new ScriptEvaluator(getScript(), output, paramNames, paramTypes);
			JOptionPane.showMessageDialog(this, "Script syntax is correct.");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}

	public void setScript(String script) {
		scriptTextArea.setText(script);
	}
	
}
