/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.configuration.ConfigurationPhase;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.properties.PropertiesPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CPUInfo;
import cdc.utils.GuiUtils;
import cdc.utils.Log;
import cdc.utils.LogSink;
import cdc.utils.Props;
import cdc.utils.RJException;

public class MainFrame extends JFrame {

	private static int MAX_LOG_LINES = Props.getInteger("max-log-lines");
	private static final String CONFIG_DIR = "./config";
	private static final String PERSISTENT_PARAM_RECENT_PATH = "recent-path";
	private static final String PERSISTENT_PARAM_RECENT_CONFIG = "recent-config";
	private static final String PERSISTENT_PROPERTIES_FILE_NAME = "properties.bin";
	private static final String VERSION_CODENAME = "codename";
	private static final String VERSION_V = "version";
	private static final String VERSION_CHANGES = "changes";

	// private static final String PERSISTENT_PARAM_CPU_NUMBER = "cpu-number";

	// private static final String[] cpusLabels = new String[] {"--", "1", "2",
	// "4", "8+"};

	private class GUILogSink extends LogSink {

		private JTextArea log;

		public GUILogSink(JTextArea log) {
			this.log = log;
		}

		public void log(String msg) {
			synchronized (log) {
				log.append(msg + "\n");
				log.setCaretPosition(log.getText().length());
				if (log.getLineCount() > MAX_LOG_LINES) {
					Element el = log.getDocument().getRootElements()[0]
							.getElement(0);
					try {
						log.getDocument().remove(0, el.getEndOffset());
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static MainFrame main;

	private MenuBar menuBar = new MenuBar();

	private SystemPanel appPanel;
	private JPanel logPanel;

	private List closingListeners = new ArrayList();

	private Map persistentParams = null;

	private Properties propertiesVersion = new Properties();

	private int cpus;

	private boolean configurationRead = false;

	public void setPersistentParam(String paramName, String paramValue) {
		if (persistentParams == null) {
			if (!attemptLoad()) {
				persistentParams = new HashMap();
			}
		}
		persistentParams.put(paramName, paramValue);
		try {
			ObjectOutputStream os = new ObjectOutputStream(
					new FileOutputStream(PERSISTENT_PROPERTIES_FILE_NAME));
			os.writeObject(persistentParams);
			os.flush();
			os.close();
		} catch (IOException e) {
			System.out.println("WARNING: CANNOT save to file: "
					+ PERSISTENT_PROPERTIES_FILE_NAME);
		}
	}

	public String getPersistentParam(String paramName) {
		if (persistentParams == null) {
			if (persistentParams == null) {
				if (!attemptLoad()) {
					persistentParams = new HashMap();
				}
			}
		}
		return (String) persistentParams.get(paramName);
	}

	private boolean attemptLoad() {
		try {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(
					PERSISTENT_PROPERTIES_FILE_NAME));
			persistentParams = (Map) is.readObject();
			return true;
		} catch (Exception e) {
			System.out.println("WARNING: CANNOT read file: "
					+ PERSISTENT_PROPERTIES_FILE_NAME);
		}
		return false;
	}

	public MainFrame() {
		super("FRIL: A Fine-Grained Record Linkage Tool");
		super.setSize(800, 600);
		super.setIconImage(Configs.appIcon);

		main = this;
		try {
			propertiesVersion.load(new FileInputStream("version.properties"));
			setTitle(getTitle() + " "
					+ propertiesVersion.getProperty(VERSION_CODENAME));
		} catch (IOException e) {
			System.out.println("ERROR reading version.properties....");
			e.printStackTrace();
		}

		createMenu();
		createMainWindow();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - (getSize().width / 2),
				screenSize.height / 2 - (getSize().height / 2));
		this.setVisible(true);

		doStartup();
	}

	private void doStartup() {

		this.cpus = CPUInfo.testNumberOfCPUs();
		Log.log(getClass(), "Number of available CPUs: " + this.cpus);

		if (getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG) != null) {
			if (JOptionPane
					.showConfirmDialog(
							this,
							"The system was closed using the following configuration file:\n"
									+ getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG)
									+ "\nWould you like to load it?",
							"Load last active configuration?",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				loadConfiguration(new File(
						getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG)));
			}
		}
	}

	private void createMainWindow() {

		this.appPanel = new SystemPanel(this);
		JScrollPane appScroll = new JScrollPane(appPanel);
		JPanel appPanel = new JPanel(new BorderLayout());
		appPanel.add(appScroll, BorderLayout.CENTER);

		logPanel = new JPanel(new BorderLayout());
		JTextArea logArea = new JTextArea();
		logArea.setEditable(false);
		JScrollPane scroll = new JScrollPane(logArea);
		// scroll.setPreferredSize(new Dimension(800, 200));
		logPanel.add(scroll, BorderLayout.CENTER);
		Log.setSinks(new LogSink[] { new GUILogSink(logArea) });

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				appPanel, logPanel);
		splitPane.setDividerLocation(400);

		getContentPane().add(splitPane);
	}

	private void createMenu() {
		super.setMenuBar(menuBar);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		super.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
			}

			public void windowClosing(WindowEvent e) {
				if (closing()) {
					Log.log(getClass(), "Closing application");
					main.dispose();
					System.exit(0);
				}
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
			}
		});

		Menu[] menu = new Menu[3];

		menu[0] = new Menu("File");
		loadFileMenu(menu[0]);

		menu[1] = new Menu("Tools");
		loadToolsMenu(menu[1]);

		menu[2] = new Menu("Help");
		loadHelpMenu(menu[2]);

		for (int i = 0; i < menu.length; i++) {
			menuBar.add(menu[i]);
		}
	}

	private void loadFileMenu(Menu menu) {
		MenuItem open = new MenuItem("Open");
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File dir = null;
				if (getPersistentParam(PERSISTENT_PARAM_RECENT_PATH) != null) {
					dir = new File(
							getPersistentParam(PERSISTENT_PARAM_RECENT_PATH));
				} else {
					dir = new File(CONFIG_DIR);
				}
				if (!dir.exists() || !dir.isDirectory()) {
					dir = new File(".");
				}
				JFileChooser chooser = new JFileChooser(dir);
				if (chooser.showOpenDialog(main) == JFileChooser.APPROVE_OPTION) {
					// will load configuration
					if (appPanel.getSystem() != null
							&& appPanel.getSystem().getJoin() != null) {
						try {
							appPanel.getSystem().getJoin().close();
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (RJException e1) {
							e1.printStackTrace();
						}
					}
					File f = chooser.getSelectedFile();
					try {
						setPersistentParam(PERSISTENT_PARAM_RECENT_PATH,
								chooser.getCurrentDirectory()
										.getCanonicalPath());
						loadConfiguration(f);
						setPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG, f
								.getCanonicalPath());
					} catch (IOException ex) {
						JXErrorDialog.showDialog(main,
								"Error saving properties file", ex);
					}
				}
			}
		});
		MenuItem save = new MenuItem("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCurrentConfiguration(false);
			}
		});
		MenuItem saveAs = new MenuItem("Save as...");
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveCurrentConfiguration(true);
			}
		});
		MenuItem exit = new MenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (closing()) {
					main.dispose();
				}
			}
		});

		menu.add(open);
		menu.add(save);
		menu.add(saveAs);
		menu.add(exit);

	}

	private void loadToolsMenu(Menu menu) {
//		MenuItem wizard = new MenuItem("Start wizard");
//		wizard.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				try {
//					SystemWizard wizard = new SystemWizard(MainFrame.this);
//					if (wizard.getResult() == AbstractWizard.RESULT_OK) {
//						appPanel.setSystem(wizard.getConfiguredSystem());
//					}
//				} catch (RJException ex) {
//					JXErrorDialog.showDialog(main, "Error", ex);
//				}
//			}
//		});
//		menu.add(wizard);

		MenuItem gc = new MenuItem("Run garbage collection");
		gc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.gc();
			}
		});
		menu.add(gc);

		MenuItem prefs = new MenuItem("Preferences");
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JPanel mainPanel = new JPanel(new BorderLayout());
				JTabbedPane tabs = new JTabbedPane();
				PropertiesPanel prefs = new PropertiesPanel(Props
						.getProperties());
				PropertiesPanel logs = new PropertiesPanel(Log.getProperties());
				tabs.addTab("General preferences", prefs);
				tabs.addTab("Logging preferences", logs);
				mainPanel.add(tabs, BorderLayout.CENTER);
				mainPanel
						.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				OptionDialog dialog = new OptionDialog(main, "Preferences");
				dialog.setPreferredSize(new Dimension(500, 450));
				dialog.setMainPanel(mainPanel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					if (JOptionPane
							.showConfirmDialog(
									main,
									"This operation requires restarting the application.\nDo you want to close it now?",
									"Restart required",
									JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
						try {
							if (closing()) {
								Props.saveProperties(prefs.getProperties());
								Log.saveProperties(logs.getProperties());
								main.setVisible(false);
								main.dispose();
							}
						} catch (IOException e) {
							JXErrorDialog.showDialog(main, e
									.getLocalizedMessage(), e);
						}
					}
				}
			}
		});
		menu.add(prefs);
	}

	private void loadHelpMenu(Menu menu) {
		MenuItem help = new MenuItem("Help");
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(MainFrame.this,
						"No help is currently available");
			}
		});
		MenuItem about = new MenuItem("About...");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String msg = "FRIL: A Fine-Grained Record Linkage Software";
				msg += "\n" + propertiesVersion.getProperty(VERSION_V);
				msg += "\n\nList of changes:\n"
						+ propertiesVersion.getProperty(VERSION_CHANGES);
				msg += "\n\nAuthor: Pawel Jurczyk";
				JOptionPane.showMessageDialog(MainFrame.this, msg,
						"About FRIL...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menu.add(help);
		menu.add(about);
	}

	public void addClosingListener(ClosingListener listener) {
		closingListeners.add(listener);
	}

	private boolean closing() {
		System.out.println("Application closing. Please wait for cleanup.");
		Log.log(MainFrame.this.getClass(),
				"Application is being closed. Please wait for cleanup...", 1);
		for (Iterator iterator = closingListeners.iterator(); iterator
				.hasNext();) {
			ClosingListener listener = (ClosingListener) iterator.next();
			if (!listener.closing()) {
				return false;
			}
		}
		if (MainFrame.this.getSystem() != null) {
			MainFrame.this.getSystem().close();
		}
		Log.log(MainFrame.this.getClass(), "Cleanup completed.", 1);

		return true;
	}

	public boolean saveCurrentConfiguration(boolean newName) {
		if (appPanel.getSystem() == null) {
			return true;
		}

		File dir = null;
		while (true) {
			File f = null;
			String recentPath = null;
			try {
				if (newName || !configurationRead) {
					if (getPersistentParam(PERSISTENT_PARAM_RECENT_PATH) != null) {
						dir = new File(
								getPersistentParam(PERSISTENT_PARAM_RECENT_PATH));
					} else {
						dir = new File(CONFIG_DIR);
					}
					if (!dir.exists() || !dir.isDirectory()) {
						dir = new File(".");
					}
					JFileChooser chooser = new JFileChooser(dir);
					if (chooser.showSaveDialog(main) == JFileChooser.APPROVE_OPTION) {
						// will save configuration
						f = chooser.getSelectedFile();
						if (!f.getName().endsWith(".xml")) {
							f = new File(f.getAbsolutePath() + ".xml");
						}
						if (f.exists()) {
							if (JOptionPane.showConfirmDialog(this, "File "
									+ f.getPath()
									+ " already exists.\nOverwrite?",
									"File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
								continue;
							}
						}
						recentPath = chooser.getCurrentDirectory()
								.getCanonicalPath();
					} else {
						return false;
					}
				} else {
					f = new File(
							getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG));
				}
				ConfiguredSystem system = appPanel.getSystem();
				Configuration.saveToXML(system, f);
				JOptionPane.showMessageDialog(main,
						"Configuration was successfully saved.");
				appPanel.systemSaved();
				if (recentPath != null) {
					setPersistentParam(PERSISTENT_PARAM_RECENT_PATH, recentPath);
				}
				setPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG, f
						.getCanonicalPath());
				configurationReadDone();
			} catch (IOException ex) {
				JXErrorDialog.showDialog(main, "Error saving properties file",
						ex);
			}

			return true;
		}
	}

	private void loadConfiguration(File f) {
		appPanel.unloadConfiguration();
		String[] phases = new String[ConfigurationPhase.phases.length];
		for (int i = 0; i < phases.length; i++) {
			phases[i] = ConfigurationPhase.phases[i].getPhaseName();
		}
		ConfigLoadDialog progressReporter = new ConfigLoadDialog(phases);
		ConfigLoaderThread thread = new ConfigLoaderThread(f, appPanel,
				progressReporter);
		progressReporter.addCancelListener(new CancelThreadListener(thread));
		thread.start();
		progressReporter.setLocation(GuiUtils.getCenterLocation(this,
				progressReporter));
		progressReporter.started();
	}

	public ConfiguredSystem getSystem() {
		return appPanel.getSystem();
	}

	public String getMinusDirectory() {
		AbstractResultsSaver[] savers = this.appPanel.getSystem()
				.getResultSavers();
		for (int i = 0; i < savers.length; i++) {
			if (savers[i] instanceof CSVFileSaver) {
				return ((CSVFileSaver) savers[i]).getActiveDirectory();
			}
		}
		return "";
	}

	public void configurationReadDone() {
		configurationRead = true;
	}

}