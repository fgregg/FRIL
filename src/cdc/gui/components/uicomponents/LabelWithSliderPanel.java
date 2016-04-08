package cdc.gui.components.uicomponents;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LabelWithSliderPanel extends JPanel {
	
	private JLabel label;
	private JSlider slider;
	private JTextField value;
	
	private double min = 0;
	private double max = 0;
	
	public LabelWithSliderPanel(String label, double initVal) {
		this(label, 0, 100, initVal);
	}
	
	public LabelWithSliderPanel(String label, double minVal, double maxVal) {
		this(label, minVal, maxVal, 0);
	}
	
	public LabelWithSliderPanel(String label, int minVal, int maxVal, int initVal) {
		this(label, (double)minVal, maxVal, initVal);
	}
	
	public LabelWithSliderPanel(String label, int initVal) {
		this(label, 0, 100, initVal);
	}
	
	public LabelWithSliderPanel(String label, int minVal, int maxVal) {
		this(label, minVal, maxVal, (double)0);
	}
	
	public LabelWithSliderPanel(String label, double minVal, double maxVal, double initVal) {
		this.label = new JLabel(label, JLabel.LEFT);
		this.slider = new JSlider(0, 100, (int) (initVal * 100));
		this.value = new JTextField(String.valueOf(initVal), 3);
		this.value.setHorizontalAlignment(JTextField.CENTER);
		this.value.setEnabled(false);
		this.label.setMinimumSize(new Dimension(100, this.label.getHeight()));
		this.min = minVal;
		this.max = maxVal;
		//setBackground(Color.RED);
		
		this.slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				value.setText(String.valueOf(decodeSliderValue()));
			}
		});
		this.setLayout(new GridBagLayout());
		this.add(this.label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		this.add(this.slider, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
		this.add(this.value, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
	}
	
	public double getValueDouble() {
		return decodeSliderValue();
	}
	
	public int getValueInt() {
		return (int)decodeSliderValue();
	}
	
	public void setValue(double val) {
		slider.setValue((int) (val / (max - min) * 100));
		value.setText(String.valueOf(val));
	}
	
	private double decodeSliderValue() {
		int sliderVal = slider.getValue();
		return min + (max - min) * sliderVal / (double)100;
	}
	
}
