package cdc.gui.components.expressionbuilder;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AtomicCondition;
import cdc.components.CompoundCondition;
import cdc.components.Condition;
import cdc.datamodel.DataColumnDefinition;

public class ExpressionBuilderPanel extends JPanel {
	
	ExpressionComponent component = null;
	
	public ExpressionBuilderPanel(DataColumnDefinition[] attributes, JComponent root) {
		init(attributes, null, root);
	}
	
	public ExpressionBuilderPanel(DataColumnDefinition[] attributes, Condition condition, JComponent root) {	
		init(attributes, condition, root);
	}

	private void init(DataColumnDefinition[] attributes, Condition condition, JComponent root) {
		setLayout(new GridBagLayout());
		component = new ExpressionComponent(0, attributes, condition, root);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(component, c);
	}
	
	public static void main(String[] args) {
		
		DataColumnDefinition[] attrs = new DataColumnDefinition[3];
		attrs[0] = new DataColumnDefinition("first name", DataColumnDefinition.TYPE_STRING, "src");
		attrs[1] = new DataColumnDefinition("last name", DataColumnDefinition.TYPE_STRING, "src");
		attrs[2] = new DataColumnDefinition("ZIP", DataColumnDefinition.TYPE_STRING, "src");
		Condition expr1 = new AtomicCondition(attrs[0], "==", "hello", "test");
		Condition expr2 = new AtomicCondition(attrs[1], "!=", "world", "test");
		Condition expr3 = new AtomicCondition(attrs[2], "==", "30030", "test");
		
		Condition expr4 = new CompoundCondition(new Condition[] {expr1, expr2}, new int[] {1});
		Condition expr = new CompoundCondition(new Condition[] {expr4, expr3}, new int[] {1});
		
		JFrame frame = new JFrame("Test");
		frame.setSize(500, 400);
		//frame.setLayout(new GridBagLayout());
		
		JScrollPane scroll = new JScrollPane();
		ExpressionBuilderPanel comp = new ExpressionBuilderPanel(attrs, expr, scroll);
		comp.setBackground(Color.red);
		scroll.setViewportView(comp);
		
		frame.add(scroll);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
	}
	
}
