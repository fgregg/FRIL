package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.components.table.TablePanel;

public class ColumnConfigDialogSingleSource extends AbstractColumnConfigDialog {

	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 2;
	private static final Dimension PREFERRED_SIZE = new Dimension(100, 20);
	
	private int result = RESULT_CANCEL;
	
	private TablePanel leftSrcAttributes;
	
	private JPanel colorOddPick;
	private JPanel colorEvenPick;
	private JPanel colorDiffPick;
	private JPanel colorMouseOverPick;
	
	private DataColumnDefinition[][] dataModel;
	
	private ColorConfig colors = ColorConfig.getDefault();
	
	public ColumnConfigDialogSingleSource(Window viewLinkagesDialog, ColorConfig colors, DataColumnDefinition[][] dataModel, DataColumnDefinition[][] usedModel) {
		super(viewLinkagesDialog, "Preferences");
		setModal(true);
		setSize(400, 500);
		this.colors = colors;
		
		this.dataModel = dataModel;
		
		JPanel buttons = createButtons();
		
		JPanel lists = createTables();
		lists.setBorder(BorderFactory.createTitledBorder("Visible attributes"));
		
		JPanel colorsPanel = createColorConfig();
		colorsPanel.setBorder(BorderFactory.createTitledBorder("Colors"));
		
		setLayout(new GridBagLayout());
		add(lists, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(colorsPanel, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(buttons, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		
		fillInLists(usedModel);
		
	}

	private JPanel createColorConfig() {
		JPanel panel = new JPanel(new GridBagLayout());
		colorOddPick = new JPanel();
		colorEvenPick = new JPanel();
		colorDiffPick = new JPanel();
		colorMouseOverPick = new JPanel();
		colorOddPick.setBackground(colors.getOddRowColor());
		colorOddPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorOddPick.setPreferredSize(new Dimension(60, 20));
		colorEvenPick.setBackground(colors.getEvenRowColor());
		colorEvenPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorEvenPick.setPreferredSize(new Dimension(60, 20));
		colorDiffPick.setBackground(colors.getDiffColor());
		colorDiffPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorDiffPick.setPreferredSize(new Dimension(60, 20));
		colorMouseOverPick.setBackground(colors.getMouseOverColor());
		colorMouseOverPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorMouseOverPick.setPreferredSize(new Dimension(60, 20));
		
		JButton oddButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Odd row color", JLabel.LEFT), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorOddPick, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(oddButton, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		oddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getOddRowColor());
				colors.setOddRowColor(c);
				colorOddPick.setBackground(c);
			}
		});
		
		JButton evenButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Even row color", JLabel.LEFT), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorEvenPick, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(evenButton, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		evenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getEvenRowColor());
				colors.setEvenRowColor(c);
				colorEvenPick.setBackground(c);
			}
		});
		
		JButton diffButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Difference color", JLabel.LEFT), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorDiffPick, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(diffButton, new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		diffButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getDiffColor());
				colors.setDiffColor(c);
				colorDiffPick.setBackground(c);
			}
		});
		
		JButton moverButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Highlighted row color", JLabel.LEFT), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorMouseOverPick, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(moverButton, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		moverButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getMouseOverColor());
				colors.setMouseOverColor(c);
				colorMouseOverPick.setBackground(c);
			}
		});
		
		return panel;
	}
	
	private Color getColor(Color initial) {
		return JColorChooser.showDialog(this, "Choose color", initial);
	}

	private void fillInLists(DataColumnDefinition[][] usedModel) {
		for (int i = 0; i < dataModel[0].length; i++) {
			leftSrcAttributes.addRow(new Object[] {new Boolean(isInOption(usedModel, new DataColumnDefinition[] {dataModel[0][i], null})), dataModel[0][i].getColumnName(), new Integer(i)});
		}
	}

	private boolean isInOption(DataColumnDefinition[][] usedModel, DataColumnDefinition[] col) {
		for (int i = 0; i < usedModel[0].length; i++) {
			if (usedModel[0][i].equals(col[0])) {
					return true;
			}
		}
		return false;
	}

	private JPanel createTables() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		panel.add(new JLabel("Available attributes"), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,10,0,5), 0, 0));
		
		panel.add(leftSrcAttributes = new TablePanel(new String[] {"", "Attribute"}, false, false), new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,10,5,5), 0, 0));
		
		leftSrcAttributes.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		
		leftSrcAttributes.setEditableColumns(new int[] {0});
		leftSrcAttributes.setColumnClasses(new Class[] {Boolean.class, Object.class});
		leftSrcAttributes.getTable().getColumnModel().getColumn(0).setPreferredWidth(7);
		
		return panel;
	}

	private JPanel createButtons() {
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton OK = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		OK.setPreferredSize(PREFERRED_SIZE);
		cancel.setPreferredSize(PREFERRED_SIZE);
		buttons.add(OK);
		buttons.add(cancel);
		OK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = RESULT_OK;
				setVisible(false);
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = RESULT_CANCEL;
				setVisible(false);
			}
		});
		return buttons;
	}

	public int getResult() {
		setVisible(true);
		return result;
	}

	public DataColumnDefinition[][] getConfiguredColumns() {
		
		List cols = new ArrayList();
		Object[] rows = leftSrcAttributes.getRows();
		for (int i = 0; i < rows.length; i++) {
			if (((Boolean)((Object[])rows[i])[0]).booleanValue()) {
				int id = getColumn((String)((Object[])rows[i])[1], 0);
				cols.add(dataModel[0][id]);
			}
		}
		
		return new DataColumnDefinition[][] {(DataColumnDefinition[]) cols.toArray(new DataColumnDefinition[] {})};
	}

	private int getColumn(String string, int src) {
		for (int i = 0; i < dataModel[src].length; i++) {
			if (string.equals(dataModel[src][i].getColumnName())) {
				return i;
			}
		}
		return -1;
	}

	public ColorConfig getColorConfig() {
		return colors;
	}

}
