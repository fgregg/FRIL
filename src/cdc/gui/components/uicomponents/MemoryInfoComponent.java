package cdc.gui.components.uicomponents;

import java.awt.Color;
import java.awt.Dimension;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.gui.components.dialogs.OneTimeTipDialog;
import cdc.utils.Log;

public class MemoryInfoComponent extends JPanel {
	
	private static final double COLOR_TRESHOLD = 0.7;

	public static final Dimension SIZE = new Dimension(250, 20);
	public static final int[] BORDER = {3,3,3,3};
	private int[] realSize = {SIZE.width - BORDER[0] - BORDER[2], SIZE.height - BORDER[1] - BORDER[3]};

	private static final double TRESHOLD = 0.9;
	
	private MeasurementThread thread;
	private volatile boolean stop = false;
	private int interval;
	
	boolean firstWarning = true;

	private JLabel stat = new JLabel("", JLabel.LEFT);
	private JPanel back = new JPanel();
	
	public MemoryInfoComponent(int measureInterval) {
		this.interval = measureInterval;
		setSize(SIZE);
		setPreferredSize(SIZE);
		setLayout(null);
		
		stat.setBounds(3, 0, SIZE.width, SIZE.height);
		back.setBounds(BORDER[0], BORDER[1], realSize[0], realSize[1]);
		add(stat);
		add(back);
		
		thread = new MeasurementThread();
		thread.start();
		
	}
	
	public void stopMeasurement() {
		stop = true;
		thread.interrupt();
	}
	
	private void setView(long cur, long max) {
		float fraction = cur / (float)max;
		
		String curMB = getMB(cur, max);
		String maxMB = getMB(max, max);
		
		stat.setText("Memory: Used " + curMB + " out of " + maxMB);
		back.setSize((int) (fraction * realSize[0]), realSize[1]);
		
		if (fraction < COLOR_TRESHOLD) {
			back.setBackground(new Color(0, 1.0F, 0));
		} else {
			fraction = (float) ((fraction - COLOR_TRESHOLD) / (1 - COLOR_TRESHOLD));
			back.setBackground(new Color(fraction, 1 - fraction, 0));
		}
		
		repaint();
		
		if (fraction > TRESHOLD) {
			if (firstWarning) {
				System.out.println("There might be insufficient memory available for Java. Consider changing value Xmx in startup script (the script you used to start FRIL).");
				Log.log(getClass(), "There might be insufficient memory available for Java. Consider changing value Xmx in startup script (the script you used to start FRIL).");
				OneTimeTipDialog.showInfoDialogIfNeeded(OneTimeTipDialog.MEMORY, OneTimeTipDialog.MEMORY_DIALOG);
				firstWarning = false;
			}
		}
	}
	
	private String getMB(long bytes, long max) {
		if (max < 1024 * 1024) {
			return (bytes / 1024) + "KB";
		} else if (max < 1024 * 1024 * 1024) {
			return (bytes / 1024 / 1024) + "MB";
		} else {
			return (Math.round(bytes * 10 / 1024.0 / 1024 / 1024) / 10.0) + "GB";
		}
	}

	private class MeasurementThread extends Thread {
		
		public MeasurementThread() {
			setName("Memory-Measurement-Thread");
		}
		
		public void run() {
			MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
			while (!stop) {
				//do the measurement
				MemoryUsage mu = memory.getHeapMemoryUsage();
				long max = mu.getMax();
				long cur = mu.getUsed();
				setView(cur, max);
				
				//pause
				synchronized (this) {
					try {
						wait(interval);
					} catch (InterruptedException e) {}
				}
			}
		}
		
	}
	
}
