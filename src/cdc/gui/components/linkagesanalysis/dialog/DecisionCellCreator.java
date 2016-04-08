package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import cdc.gui.Configs;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;

public class DecisionCellCreator extends FirstColumnComponentCreator {

	private JPanel r;
	private JPanel e;
	//private SpanTableModel model;
	private JButton accept;
	private JButton reject;
	
	private ActionListener acceptListener;
	private ActionListener rejectListener;
	
	public DecisionCellCreator(ActionListener acceptListener, ActionListener rejectListener) {
		this.acceptListener = acceptListener;
		this.rejectListener = rejectListener;
	}

	public JComponent getRenderer() {
		if (r != null) {
			return r;
		}
		r = newPanel();
		return r;
	}

	public JComponent getEditor(int row, SpanTableModel model) {
		if (e != null) {
			return e;
		}
		e = newPanel();
		//this.model = model;
		return e;
	}

	private JPanel newPanel() {
		JPanel panel = new JPanel(new FlowLayout());
		accept = new JButton(Configs.addButtonIcon);
		reject = new JButton(Configs.removeButtonIcon);
		accept.setToolTipText("Accept linkage");
		reject.setToolTipText("Reject linkage");
		accept.setPreferredSize(Configs.PREFERRED_SIZE);
		reject.setPreferredSize(Configs.PREFERRED_SIZE);
		accept.addActionListener(acceptListener);
		reject.addActionListener(rejectListener);
		panel.add(accept);
		panel.add(reject);
		panel.setBackground(Color.WHITE);
		return panel;
	}
	

}
