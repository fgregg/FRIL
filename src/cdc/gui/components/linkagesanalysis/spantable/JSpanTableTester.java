package cdc.gui.components.linkagesanalysis.spantable;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class JSpanTableTester {
	
	
	public static void main(String[] args) {
		SpanTableModel model = new SpanTableModel(new String[] {"Col 1", "Col 2", "Col 3"});
		
		addData(model);
		
		JSpanTable spanTable = new JSpanTable(model);
		
		JScrollPane scroll = new JScrollPane(spanTable);
		JFrame frame = new JFrame();
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		frame.add(scroll);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	private static void addData(SpanTableModel model) {
		Object[] r1 = new String[] {"10", "11", "12", "13"};
		Object[] r2 = new String[] {"20", "21", "22", "23"};
		Object[] r3 = new String[] {"30", "31", "32", "33"};
		
		SpanInfo[] span = new SpanInfo[3];
		span[0] = new SpanInfo(0, 0, 1, 3);
		span[1] = new SpanInfo(1, 1, 2, 2);
		span[2] = new SpanInfo(0, 1, 2, 1);
		
		model.addSpannedRows(new Object[][] {r1, r2, r3}, span);
		
	}
	
}
