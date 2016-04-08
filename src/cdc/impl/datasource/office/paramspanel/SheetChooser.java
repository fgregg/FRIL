package cdc.impl.datasource.office.paramspanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.utils.GuiUtils;
import cdc.utils.StringUtils;

public class SheetChooser extends ParamPanelField {

	public class DocumentChangedAction implements DocumentListener {
		private ChangedConfigurationListener listener;
		public DocumentChangedAction(ChangedConfigurationListener listener) {
				this.listener = listener;
		}
		public void changedUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void insertUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void removeUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
	}

	private Map listeners = new HashMap();
	
	private JTextField field = new JTextField(GuiUtils.EMPTY);
	private JCheckBox selectAll = new JCheckBox("Use all sheets");
	private JLabel userLabel = new JLabel();
	private JLabel error;
	private JPanel mainPanel;
	private String param;
	
	
	public SheetChooser(String param, String label, String defaultValue) {
		
		userLabel.setText(label);
		this.param = param;
		
		field.setPreferredSize(new Dimension(200, (int)field.getPreferredSize().getHeight()));
		field.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent arg0) {
				field.selectAll();
			}
			public void focusLost(FocusEvent arg0) {
				if (StringUtils.isNullOrEmptyNoTrim(field.getText())) {
					field.setText(GuiUtils.EMPTY);
				}
			}
		});
		
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}

			public void insertUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}

			public void removeUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}
			private void handleEdit(DocumentEvent evt) {
				if (evt.getDocument().toString().equals("")) {
					field.setText(GuiUtils.EMPTY);
				}
			}
		});
		
		error = new JLabel(Configs.errorInfoIcon);
		error.setHorizontalAlignment(JLabel.CENTER);
		error.setVerticalAlignment(JLabel.CENTER);
		error.setVisible(false);
		error.setForeground(Color.red);
		
		JPanel errorPanel = new JPanel(null);
		error.setPreferredSize(new Dimension(20, 20));
		error.setBounds(0,0,20,20);
		errorPanel.add(error);
		errorPanel.setPreferredSize(new Dimension(20, 20));
		
		JPanel p1 = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		p1.add(field, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		p1.add(selectAll, c);
		
		mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		mainPanel.add(errorPanel);
		mainPanel.add(p1);
		
		selectAll.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (selectAll.isSelected()) {
					field.setEnabled(false);
				} else {
					field.setEnabled(true);
				}
			}
		});
		//selectAll.setSelected(false);
		
		setValue(defaultValue);
		
	}

	public void error(String message) {
		if (message == null) {
			field.setBackground(Color.WHITE);
			selectAll.setBackground(Color.WHITE);
		} else {
			
		}
	}

	public JComponent getComponentInputField() {
		return mainPanel;
	}

	public JComponent getComponentLabel() {
		return userLabel;
	}

	public String getUserLabel() {
		return param;
	}

	public String getValue() {
		if (selectAll.isSelected()) {
			return null;
		} else {
			return field.getText();
		}
	}

	public void setValue(String val) {
		if (val == null || val.isEmpty()) {
			selectAll.setSelected(true);
		} else {
			field.setText(val);
		}
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		DocumentListener l = new DocumentChangedAction(configurationListener);
		listeners.put(configurationListener, l);
		this.field.getDocument().addDocumentListener(l);
	}
	
	public void removeConfigurationChangeListener(ChangedConfigurationListener listener) {
		this.field.getDocument().removeDocumentListener((DocumentListener) listeners.remove(listener));
	}
	
}
