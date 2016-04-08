package cdc.gui.components.expressionbuilder;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.components.AtomicCondition;
import cdc.components.CompoundCondition;
import cdc.components.Condition;
import cdc.datamodel.DataColumnDefinition;

public class ExpressionComponent extends JPanel {
	
	private static JComboBox getOperators() {
		return new JComboBox(AtomicCondition.conds);
	}
	
	private class AddSimpleConditionListener implements ActionListener {

		private ExpressionComponent component;
		
		public AddSimpleConditionListener(ExpressionComponent component) {
			this.component = component;
		}
		
		public void actionPerformed(ActionEvent e) {
			ExpressionComponent[] ch = new ExpressionComponent[children.length + 1];
			int position = component.getPosition();
			System.arraycopy(children, 0, ch, 0, position);
			System.arraycopy(children, position, ch, position + 1, children.length - position);
			ch[position] = new ExpressionComponent(level + 1, attrs, null, 0, root);
			ch[position].setAddButtonListener(new AddSimpleConditionListener(ch[position]));
			for (int i = 0; i < ch.length; i++) {
				ch[i].setPosition(i);
			}
			children = ch;
			if (root != null) {
				//System.out.println(root);
				repaint();
				invalidate();
				root.validate();
				root.repaint();
			}
		}
		
	}
	
	private class SplitConditionListener implements ActionListener {

		private ExpressionComponent component;
		
		public SplitConditionListener(ExpressionComponent component) {
			this.component = component;
		}
		
		public void actionPerformed(ActionEvent e) {
			ExpressionComponent[] ch = new ExpressionComponent[children.length];
			int position = component.getPosition() + 1;
			System.arraycopy(children, 0, ch, 0, position);
			System.arraycopy(children, position + 1, ch, position + 1, children.length - position - 1);
			
			Condition c = new CompoundCondition(new Condition[] {children[position].getCondition(), null}, new int[] {1});
			ExpressionComponent newChild = new ExpressionComponent(level + 1, attrs, c, 0, root);
			ch[position] = newChild;
			for (int i = 0; i < ch.length; i++) {
				ch[i].setPosition(i);
			}
			children = ch;
			if (root != null) {
				//System.out.println(root);
				repaint();
				invalidate();
				root.validate();
				root.repaint();
			}
		}
		
	}
	
	private JComboBox logicOp = new JComboBox(new String [] {"AND", "OR"});
	private JButton add = new JButton("Add criteria");
	private JButton split = new JButton("Split");
	private ExpressionComponent[] children = new ExpressionComponent[] {};
	
	private DataColumnDefinition[] attrs;
	private JComboBox attributes;
	private JComboBox operators;
	private JTextField text;
	
	private JComponent root;
	
	private int level = 0;
	private int position = 0;
	
	public ExpressionComponent(int level, DataColumnDefinition[] attributes, JComponent root) {
		this.level = level;
		this.root = root;
		init(level, attributes, null, -1);
	}

	public ExpressionComponent(int level, DataColumnDefinition[] attributes, Condition condition, JComponent root) {
		this.root = root;
		this.level = level;
		init(level, attributes, condition, -1);
	}

	public ExpressionComponent(int level, DataColumnDefinition[] attributes, Condition condition, int logicOperator, JComponent root) {
		this.root = root;
		this.level = level;
		init(level, attributes, condition, logicOperator);
	}
	
	public void init(int level, DataColumnDefinition[] attributes, Condition condition, int logicOperator) {
		attrs = attributes;
		if (condition == null || condition instanceof AtomicCondition) {
			children = null;
			this.attributes = new JComboBox(attributes);
			operators = getOperators();
			text = new JTextField("");
			if (condition != null) {
				this.attributes.setSelectedItem(((AtomicCondition)condition).getColumn());
				operators.setSelectedItem(((AtomicCondition)condition).getCondition());
				text.setText(((AtomicCondition)condition).getValue());
			}
		} else {
			CompoundCondition cCond = (CompoundCondition)condition;
			children = new ExpressionComponent[cCond.getChildren().length];
			for (int i = 0; i < children.length - 1; i++) {
				children[i] = new ExpressionComponent(level + 1, attributes, cCond.getChildren()[i], cCond.getOperators()[i], root);
				children[i].setAddButtonListener(new AddSimpleConditionListener(children[i])); 
				children[i].setSplitButtonListener(new SplitConditionListener(children[i])); 
			}
			children[children.length - 1] = new ExpressionComponent(level + 1, attributes, cCond.getChildren()[children.length - 1], root);
			children[children.length - 1].setAddButtonListener(new AddSimpleConditionListener(children[children.length - 1]));
			children[children.length - 1].setSplitButtonListener(new SplitConditionListener(children[children.length - 1]));
		}
		if (logicOperator != -1) {
			logicOp.setSelectedIndex(logicOperator - 1);
			logicOp.setVisible(true);
		} else {
			logicOp.setVisible(false);
		}
		//buttons.add(logicOp);
		//buttons.add(add);
		setBorder(BorderFactory.createEtchedBorder());
		repaint();
	}
	
	private void setAddButtonListener(AddSimpleConditionListener addSimpleConditionListener) {
		add.addActionListener(addSimpleConditionListener);
	}
	
	private void setSplitButtonListener(SplitConditionListener splitConditionListener) {
		split.addActionListener(splitConditionListener);
	}

	public void repaint() {
		layoutConditions();
		super.repaint();
//		if (children != null) {
//			for (int i = 0; i < children.length; i++) {
//				children[i].layoutConditions();
//				children[i].validate();
//				children[i].repaint();
//			}
//			super.repaint();
//		}
	}

	private void layoutConditions() {
		//System.out.println("Layout...");
		removeAll();
		setLayout(new GridBagLayout());
		JPanel buttons = new JPanel(new FlowLayout());
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = level;
				c.gridy = i;
				c.weightx = 1;
				c.fill = GridBagConstraints.BOTH;
				add(children[i], c);
			}
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = level;
			c.gridy = children.length;
			c.anchor = GridBagConstraints.WEST;
			buttons.add(logicOp);
			add(buttons, c);
		} else if (attributes != null) {
			JPanel condPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = level;
			c.gridy = 0;
			c.fill = GridBagConstraints.BOTH;
			condPanel.add(attributes, c);
			c = new GridBagConstraints();
			c.gridx = level + 1;
			c.gridy = 0;
			c.fill = GridBagConstraints.BOTH;
			condPanel.add(operators, c);
			c = new GridBagConstraints();
			c.gridx = level + 2;
			c.gridy = 0;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			condPanel.add(text, c);
			c = new GridBagConstraints();
			c.gridx = level;
			c.gridy = 0;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			add(condPanel, c);
			c = new GridBagConstraints();
			c.gridx = level;
			c.gridy = 1;
			c.anchor = GridBagConstraints.WEST;
			buttons.add(logicOp);
			buttons.add(add);
			buttons.add(split);
			add(buttons, c);
		}
		//System.out.println("Level: " + level);
		//filler
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = children == null ? 2 : children.length + 1;
		c.gridwidth = level;
		c.ipadx = 50;
		c.weighty = 1;
		c.fill = GridBagConstraints.VERTICAL;
		JPanel p = new JPanel();
		//p.setBackground(Color.blue);
		add(p, c);
	}
	
	public Condition getCondition() {
		if (children == null) {
			return new AtomicCondition((DataColumnDefinition)attributes.getSelectedItem(), operators.getSelectedItem().toString(), text.getText(), null);
		} else {
			Condition[] c = new Condition[this.children.length];
			for (int i = 0; i < c.length; i++) {
				c[i] = children[i].getCondition();
			}
			int[] logicOps = new int[c.length - 1];
			for (int i = 0; i < logicOps.length; i++) {
				logicOps[i] = children[i].getChosenLogicOperator();
			}
			return new CompoundCondition(c, logicOps);
		}
	}
	
	private int getChosenLogicOperator() {
		return logicOp.getSelectedIndex() + 1;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int i) {
		this.position = i;
	}
	
}
