// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 * Persisted user preferences for the NCSDecomp GUI.
 * <p>
 * Stores comprehensive settings matching all CLI options and offers a modern
 * tabbed dialog for editing preferences. Backed by a simple {@code ncsdecomp.conf}
 * properties file in the working directory (with legacy {@code dencs.conf} support
 * for backward compatibility).
 */
public class Settings extends Properties implements ActionListener {
   private static final long serialVersionUID = 1L;
   private static final String CONFIG_FILE = "ncsdecomp.conf";
   private static final String LEGACY_CONFIG_FILE = "dencs.conf";
   
   // UI Components
   private JFrame frame;
   private JTabbedPane tabbedPane;
   private JButton saveButton;
   private JButton cancelButton;
   
   // File/Directory Settings
   private JTextField outputDirectoryField;
   private JButton browseOutputDirButton;
   private JTextField openDirectoryField;
   private JButton browseOpenDirButton;
   private JTextField k1NwscriptPathField;
   private JButton browseK1NwscriptButton;
   private JTextField k2NwscriptPathField;
   private JButton browseK2NwscriptButton;
   
   // Game Settings
   private JRadioButton gameK1Radio;
   private JRadioButton gameK2Radio;
   
   // Decompilation Options
   private JCheckBox preferSwitchesCheckBox;
   private JCheckBox strictSignaturesCheckBox;
   private JCheckBox overwriteFilesCheckBox;
   
   // Output Settings
   private JComboBox<String> encodingComboBox;
   private JTextField fileExtensionField;
   private JTextField filenamePrefixField;
   private JTextField filenameSuffixField;
   
   // UI Settings
   private JCheckBox linkScrollBarsCheckBox;

   public static void main(String[] args) {
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();
      
      if (src == this.saveButton) {
         saveSettings();
         this.save();
         this.frame.dispose();
      } else if (src == this.cancelButton) {
         this.frame.dispose();
      } else if (src == this.browseOutputDirButton) {
         String path = Decompiler.chooseOutputDirectory();
         if (path != null && !path.isEmpty()) {
            this.outputDirectoryField.setText(path);
         }
      } else if (src == this.browseOpenDirButton) {
         JFileChooser chooser = new JFileChooser(this.openDirectoryField.getText());
         chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         chooser.setDialogTitle("Select Default Open Directory");
         if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            this.openDirectoryField.setText(chooser.getSelectedFile().getAbsolutePath());
         }
      } else if (src == this.browseK1NwscriptButton) {
         JFileChooser chooser = new JFileChooser();
         chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         chooser.setDialogTitle("Select k1_nwscript.nss File");
         chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
               return f.isDirectory() || f.getName().toLowerCase().endsWith(".nss");
            }
            
            @Override
            public String getDescription() {
               return "NSS Files (*.nss)";
            }
         });
         if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            this.k1NwscriptPathField.setText(chooser.getSelectedFile().getAbsolutePath());
         }
      } else if (src == this.browseK2NwscriptButton) {
         JFileChooser chooser = new JFileChooser();
         chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         chooser.setDialogTitle("Select tsl_nwscript.nss File");
         chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
               return f.isDirectory() || f.getName().toLowerCase().endsWith(".nss");
            }
            
            @Override
            public String getDescription() {
               return "NSS Files (*.nss)";
            }
         });
         if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            this.k2NwscriptPathField.setText(chooser.getSelectedFile().getAbsolutePath());
         }
      }
   }
   
   private void saveSettings() {
      // File/Directory Settings
      this.setProperty("Output Directory", this.outputDirectoryField.getText());
      this.setProperty("Open Directory", this.openDirectoryField.getText());
      String k1NwscriptPath = this.k1NwscriptPathField.getText().trim();
      if (k1NwscriptPath.isEmpty()) {
         this.remove("K1 nwscript Path");
      } else {
         this.setProperty("K1 nwscript Path", k1NwscriptPath);
      }
      String k2NwscriptPath = this.k2NwscriptPathField.getText().trim();
      if (k2NwscriptPath.isEmpty()) {
         this.remove("K2 nwscript Path");
      } else {
         this.setProperty("K2 nwscript Path", k2NwscriptPath);
      }
      
      // Game Settings
      if (this.gameK1Radio.isSelected()) {
         this.setProperty("Game Variant", "k1");
         FileDecompiler.isK2Selected = false;
      } else if (this.gameK2Radio.isSelected()) {
         this.setProperty("Game Variant", "k2");
         FileDecompiler.isK2Selected = true;
      }
      
      // Decompilation Options
      this.setProperty("Prefer Switches", String.valueOf(this.preferSwitchesCheckBox.isSelected()));
      FileDecompiler.preferSwitches = this.preferSwitchesCheckBox.isSelected();
      this.setProperty("Strict Signatures", String.valueOf(this.strictSignaturesCheckBox.isSelected()));
      FileDecompiler.strictSignatures = this.strictSignaturesCheckBox.isSelected();
      this.setProperty("Overwrite Files", String.valueOf(this.overwriteFilesCheckBox.isSelected()));
      
      // Output Settings
      this.setProperty("Encoding", (String) this.encodingComboBox.getSelectedItem());
      this.setProperty("File Extension", this.fileExtensionField.getText());
      this.setProperty("Filename Prefix", this.filenamePrefixField.getText());
      this.setProperty("Filename Suffix", this.filenameSuffixField.getText());
      
      // UI Settings
      this.setProperty("Link Scroll Bars", String.valueOf(this.linkScrollBarsCheckBox.isSelected()));
   }
   
   private void loadSettingsIntoUI() {
      // File/Directory Settings
      // Default output directory: ./ncsdecomp_converted relative to current working directory
      String defaultOutputDir = new File(System.getProperty("user.dir"), "ncsdecomp_converted").getAbsolutePath();
      this.outputDirectoryField.setText(this.getProperty("Output Directory", defaultOutputDir));
      this.openDirectoryField.setText(this.getProperty("Open Directory", System.getProperty("user.dir")));
      
      // Default nwscript paths: tools/ directory + filename
      String defaultK1Path = new File(new File(System.getProperty("user.dir"), "tools"), "k1_nwscript.nss").getAbsolutePath();
      String defaultK2Path = new File(new File(System.getProperty("user.dir"), "tools"), "tsl_nwscript.nss").getAbsolutePath();
      this.k1NwscriptPathField.setText(this.getProperty("K1 nwscript Path", defaultK1Path));
      this.k2NwscriptPathField.setText(this.getProperty("K2 nwscript Path", defaultK2Path));
      
      // Game Settings
      String gameVariant = this.getProperty("Game Variant", "k1").toLowerCase();
      if (gameVariant.equals("k2") || gameVariant.equals("tsl") || gameVariant.equals("2")) {
         this.gameK2Radio.setSelected(true);
         FileDecompiler.isK2Selected = true;
      } else {
         this.gameK1Radio.setSelected(true);
         FileDecompiler.isK2Selected = false;
      }
      
      // Decompilation Options
      this.preferSwitchesCheckBox.setSelected(Boolean.parseBoolean(this.getProperty("Prefer Switches", "false")));
      FileDecompiler.preferSwitches = this.preferSwitchesCheckBox.isSelected();
      this.strictSignaturesCheckBox.setSelected(Boolean.parseBoolean(this.getProperty("Strict Signatures", "false")));
      FileDecompiler.strictSignatures = this.strictSignaturesCheckBox.isSelected();
      this.overwriteFilesCheckBox.setSelected(Boolean.parseBoolean(this.getProperty("Overwrite Files", "false")));
      
      // Output Settings
      // Default encoding: Windows-1252 (standard for KotOR/TSL)
      String encoding = this.getProperty("Encoding", "Windows-1252");
      this.encodingComboBox.setSelectedItem(encoding);
      this.fileExtensionField.setText(this.getProperty("File Extension", ".nss"));
      this.filenamePrefixField.setText(this.getProperty("Filename Prefix", ""));
      this.filenameSuffixField.setText(this.getProperty("Filename Suffix", ""));
      
      // UI Settings
      this.linkScrollBarsCheckBox.setSelected(Boolean.parseBoolean(this.getProperty("Link Scroll Bars", "false")));
   }

   /**
    * Loads preferences from disk, creating a default config if none exists.
    */
   public void load() {
      File configToLoad = new File(CONFIG_FILE);
      if (!configToLoad.exists()) {
         File legacy = new File(LEGACY_CONFIG_FILE);
         if (legacy.exists()) {
            configToLoad = legacy;
         }
      }

      try {
         try (FileInputStream fis = new FileInputStream(configToLoad)) {
            this.load(fis);
         }
      } catch (Exception var4) {
         try {
            new File(CONFIG_FILE).createNewFile();
         } catch (FileNotFoundException var2) {
            var2.printStackTrace();
            System.exit(1);
         } catch (IOException var3) {
            var3.printStackTrace();
            System.exit(1);
         }

         this.reset();
         this.save();
      }
      
      // Apply loaded settings to static flags
      String gameVariant = this.getProperty("Game Variant", "k1").toLowerCase();
      FileDecompiler.isK2Selected = gameVariant.equals("k2") || gameVariant.equals("tsl") || gameVariant.equals("2");
      FileDecompiler.preferSwitches = Boolean.parseBoolean(this.getProperty("Prefer Switches", "false"));
      FileDecompiler.strictSignatures = Boolean.parseBoolean(this.getProperty("Strict Signatures", "false"));
   }

   /**
    * Writes preferences to {@code ncsdecomp.conf}.
    */
   public void save() {
      try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
         this.store(fos, "NCSDecomp Configuration");
      } catch (FileNotFoundException var2) {
         var2.printStackTrace();
      } catch (IOException var3) {
         var3.printStackTrace();
      }
   }

   /**
    * Resets all preferences to their default values.
    */
   public void reset() {
      // Default output directory: ./ncsdecomp_converted relative to current working directory
      String defaultOutputDir = new File(System.getProperty("user.dir"), "ncsdecomp_converted").getAbsolutePath();
      this.setProperty("Output Directory", defaultOutputDir);
      this.setProperty("Open Directory", System.getProperty("user.dir"));
      String defaultK1Path = new File(new File(System.getProperty("user.dir"), "tools"), "k1_nwscript.nss").getAbsolutePath();
      String defaultK2Path = new File(new File(System.getProperty("user.dir"), "tools"), "tsl_nwscript.nss").getAbsolutePath();
      this.setProperty("K1 nwscript Path", defaultK1Path);
      this.setProperty("K2 nwscript Path", defaultK2Path);
      this.setProperty("Game Variant", "k1");
      this.setProperty("Prefer Switches", "false");
      this.setProperty("Strict Signatures", "false");
      this.setProperty("Overwrite Files", "false");
      this.setProperty("Encoding", "Windows-1252");
      this.setProperty("File Extension", ".nss");
      this.setProperty("Filename Prefix", "");
      this.setProperty("Filename Suffix", "");
      this.setProperty("Link Scroll Bars", "false");
   }

   /**
    * Opens the comprehensive settings dialog with all options organized into tabs.
    */
   public void show() {
      this.frame = new JFrame("NCSDecomp Settings");
      this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.frame.setResizable(true);
      
      // Create tabbed pane
      this.tabbedPane = new JTabbedPane();
      
      // File/Directory Settings Tab
      this.tabbedPane.addTab("Files & Directories", createFilesDirectoriesPanel());
      
      // Game Settings Tab
      this.tabbedPane.addTab("Game", createGameSettingsPanel());
      
      // Decompilation Options Tab
      this.tabbedPane.addTab("Decompilation", createDecompilationPanel());
      
      // Output Settings Tab
      this.tabbedPane.addTab("Output", createOutputSettingsPanel());
      
      // UI Settings Tab
      this.tabbedPane.addTab("Interface", createUISettingsPanel());
      
      // Load current settings into UI
      this.loadSettingsIntoUI();
      
      // Buttons panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      this.saveButton = new JButton("Save");
      this.cancelButton = new JButton("Cancel");
      this.saveButton.addActionListener(this);
      this.cancelButton.addActionListener(this);
      buttonPanel.add(this.cancelButton);
      buttonPanel.add(this.saveButton);
      
      // Main panel
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(this.tabbedPane, BorderLayout.CENTER);
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);
      mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      
      this.frame.getContentPane().add(mainPanel);
      this.frame.pack();
      this.frame.setSize(600, 500);
      this.frame.setLocationRelativeTo(null);
      this.frame.setVisible(true);
   }
   
   private JPanel createFilesDirectoriesPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      
      // Output Directory
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Output Directory:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.outputDirectoryField = new JTextField(30);
      this.outputDirectoryField.setToolTipText("Default directory for saving decompiled NSS files");
      panel.add(this.outputDirectoryField, gbc);
      gbc.gridx = 2;
      gbc.weightx = 0.0;
      this.browseOutputDirButton = new JButton("Browse...");
      this.browseOutputDirButton.addActionListener(this);
      panel.add(this.browseOutputDirButton, gbc);
      
      // Open Directory
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Default Open Directory:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.openDirectoryField = new JTextField(30);
      this.openDirectoryField.setToolTipText("Default directory for file open dialogs");
      panel.add(this.openDirectoryField, gbc);
      gbc.gridx = 2;
      gbc.weightx = 0.0;
      this.browseOpenDirButton = new JButton("Browse...");
      this.browseOpenDirButton.addActionListener(this);
      panel.add(this.browseOpenDirButton, gbc);
      
      // K1 nwscript Path
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weightx = 0.0;
      panel.add(new JLabel("KotOR 1 nwscript.nss:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.k1NwscriptPathField = new JTextField(30);
      this.k1NwscriptPathField.setToolTipText("Path to k1_nwscript.nss file for KotOR 1 decompilation");
      panel.add(this.k1NwscriptPathField, gbc);
      gbc.gridx = 2;
      gbc.weightx = 0.0;
      this.browseK1NwscriptButton = new JButton("Browse...");
      this.browseK1NwscriptButton.addActionListener(this);
      panel.add(this.browseK1NwscriptButton, gbc);
      
      // K2 nwscript Path
      gbc.gridx = 0;
      gbc.gridy = 3;
      gbc.weightx = 0.0;
      panel.add(new JLabel("KotOR 2 nwscript.nss:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.k2NwscriptPathField = new JTextField(30);
      this.k2NwscriptPathField.setToolTipText("Path to tsl_nwscript.nss file for KotOR 2/TSL decompilation");
      panel.add(this.k2NwscriptPathField, gbc);
      gbc.gridx = 2;
      gbc.weightx = 0.0;
      this.browseK2NwscriptButton = new JButton("Browse...");
      this.browseK2NwscriptButton.addActionListener(this);
      panel.add(this.browseK2NwscriptButton, gbc);
      
      // Spacer
      gbc.gridx = 0;
      gbc.gridy = 4;
      gbc.weighty = 1.0;
      panel.add(new JLabel(), gbc);
      
      return panel;
   }
   
   private JPanel createGameSettingsPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.anchor = GridBagConstraints.WEST;
      
      // Game Variant
      gbc.gridx = 0;
      gbc.gridy = 0;
      panel.add(new JLabel("Game Variant:"), gbc);
      
      ButtonGroup gameGroup = new ButtonGroup();
      this.gameK1Radio = new JRadioButton("KotOR 1");
      this.gameK1Radio.setToolTipText("Use KotOR 1 (Knights of the Old Republic) action definitions");
      this.gameK2Radio = new JRadioButton("KotOR 2");
      this.gameK2Radio.setToolTipText("Use KotOR 2 (The Sith Lords) action definitions");
      
      gameGroup.add(this.gameK1Radio);
      gameGroup.add(this.gameK2Radio);
      
      JPanel radioPanel = new JPanel(new GridBagLayout());
      GridBagConstraints radioGbc = new GridBagConstraints();
      radioGbc.insets = new Insets(2, 5, 2, 5);
      radioGbc.anchor = GridBagConstraints.WEST;
      radioGbc.gridx = 0;
      radioGbc.gridy = 0;
      radioPanel.add(this.gameK1Radio, radioGbc);
      radioGbc.gridy = 1;
      radioPanel.add(this.gameK2Radio, radioGbc);
      
      gbc.gridx = 1;
      gbc.gridy = 0;
      panel.add(radioPanel, gbc);
      
      // Info label
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.gridwidth = 2;
      JLabel infoLabel = new JLabel("<html><i>Note: Game variant determines which nwscript.nss file is used.<br>" +
                                    "Configure nwscript paths in the \"Files & Directories\" tab.</i></html>");
      infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      panel.add(infoLabel, gbc);
      
      // Spacer
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weighty = 1.0;
      panel.add(new JLabel(), gbc);
      
      return panel;
   }
   
   private JPanel createDecompilationPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.anchor = GridBagConstraints.WEST;
      
      // Prefer Switches
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 2;
      this.preferSwitchesCheckBox = new JCheckBox("Prefer switch structures over if-elseif chains");
      this.preferSwitchesCheckBox.setToolTipText("When possible, generate switch statements instead of if-elseif chains for better readability");
      panel.add(this.preferSwitchesCheckBox, gbc);
      
      // Strict Signatures
      gbc.gridy = 1;
      this.strictSignaturesCheckBox = new JCheckBox("Strict signatures (fail if any signature remains unknown)");
      this.strictSignaturesCheckBox.setToolTipText("Abort decompilation if any subroutine signature cannot be fully determined");
      panel.add(this.strictSignaturesCheckBox, gbc);
      
      // Overwrite Files
      gbc.gridy = 2;
      this.overwriteFilesCheckBox = new JCheckBox("Overwrite existing files without prompting");
      this.overwriteFilesCheckBox.setToolTipText("Automatically overwrite existing output files without confirmation");
      panel.add(this.overwriteFilesCheckBox, gbc);
      
      // Spacer
      gbc.gridy = 3;
      gbc.weighty = 1.0;
      panel.add(new JLabel(), gbc);
      
      return panel;
   }
   
   private JPanel createOutputSettingsPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      
      // Encoding
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0.0;
      panel.add(new JLabel("File Encoding:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      // Put Windows-1252 first as it's the default for KotOR/TSL
      String[] encodings = {"Windows-1252", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "ISO-8859-1", "US-ASCII"};
      this.encodingComboBox = new JComboBox<>(encodings);
      this.encodingComboBox.setToolTipText("Character encoding for output files");
      this.encodingComboBox.setEditable(true);
      panel.add(this.encodingComboBox, gbc);
      
      // File Extension
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.weightx = 0.0;
      panel.add(new JLabel("File Extension:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.fileExtensionField = new JTextField(10);
      this.fileExtensionField.setToolTipText("Extension for output files (e.g., .nss)");
      panel.add(this.fileExtensionField, gbc);
      
      // Filename Prefix
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Filename Prefix:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.filenamePrefixField = new JTextField(20);
      this.filenamePrefixField.setToolTipText("Prefix to add to output filenames (e.g., \"decompiled_\")");
      panel.add(this.filenamePrefixField, gbc);
      
      // Filename Suffix
      gbc.gridx = 0;
      gbc.gridy = 3;
      gbc.weightx = 0.0;
      panel.add(new JLabel("Filename Suffix:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.filenameSuffixField = new JTextField(20);
      this.filenameSuffixField.setToolTipText("Suffix to add to output filenames (e.g., \"_decompiled\")");
      panel.add(this.filenameSuffixField, gbc);
      
      // Spacer
      gbc.gridx = 0;
      gbc.gridy = 4;
      gbc.weighty = 1.0;
      panel.add(new JLabel(), gbc);
      
      return panel;
   }
   
   private JPanel createUISettingsPanel() {
      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.anchor = GridBagConstraints.WEST;
      
      // Link Scroll Bars
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 2;
      this.linkScrollBarsCheckBox = new JCheckBox("Link scroll bars between bytecode and decompiled code views");
      this.linkScrollBarsCheckBox.setToolTipText("Synchronize scrolling between original and decompiled bytecode views");
      panel.add(this.linkScrollBarsCheckBox, gbc);
      
      // Spacer
      gbc.gridy = 1;
      gbc.weighty = 1.0;
      panel.add(new JLabel(), gbc);
      
      return panel;
   }
}

