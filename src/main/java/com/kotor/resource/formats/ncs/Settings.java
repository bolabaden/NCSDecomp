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
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import javax.swing.SwingUtilities;

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
   private JTextField nwnnsscompPathField;
   private JButton browseNwnnsscompButton;
   private JComboBox<String> nwnnsscompComboBox;
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
      } else if (src == this.browseNwnnsscompButton) {
         // Browse for directory (folder)
         String currentPath = this.nwnnsscompPathField.getText().trim();
         File currentDir = null;
         if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.isFile()) {
               // If it's a file, use its parent directory
               currentDir = currentFile.getParentFile();
            } else if (currentFile.isDirectory()) {
               currentDir = currentFile;
            }
         }
         JFileChooser chooser = new JFileChooser(currentDir);
         chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         chooser.setDialogTitle("Select nwnnsscomp Directory");
         if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            this.nwnnsscompPathField.setText(selectedDir.getAbsolutePath());
            populateCompilerComboBox(selectedDir);
            updateCompilerInfo();
         }
      }
   }

   private void saveSettings() {
      // File/Directory Settings
      this.setProperty("Output Directory", this.outputDirectoryField.getText());
      this.setProperty("Open Directory", this.openDirectoryField.getText());

      // Save the full path to the selected compiler (from combobox selection)
      String selectedCompiler = (String) this.nwnnsscompComboBox.getSelectedItem();
      String folderPath = this.nwnnsscompPathField.getText().trim();
      if (!folderPath.isEmpty() && selectedCompiler != null && !selectedCompiler.isEmpty()) {
         File folder = new File(folderPath);
         if (folder.isFile()) {
            folder = folder.getParentFile();
         }
         if (folder != null && folder.isDirectory()) {
            File compilerFile = new File(folder, selectedCompiler);
            String fullPath = compilerFile.getAbsolutePath();
            this.setProperty("nwnnsscomp Path", fullPath);
            FileDecompiler.nwnnsscompPath = fullPath;
         } else {
            // Fallback: just save the folder path
            this.setProperty("nwnnsscomp Path", folderPath);
            FileDecompiler.nwnnsscompPath = folderPath;
         }
      } else if (!folderPath.isEmpty()) {
         this.setProperty("nwnnsscomp Path", folderPath);
         FileDecompiler.nwnnsscompPath = folderPath;
      } else {
         this.remove("nwnnsscomp Path");
         FileDecompiler.nwnnsscompPath = null;
      }
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

      // Apply nwnnsscomp path to FileDecompiler
      String nwnnsscompPathValue = this.nwnnsscompPathField.getText().trim();
      FileDecompiler.nwnnsscompPath = nwnnsscompPathValue.isEmpty() ? null : nwnnsscompPathValue;

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

      // Default nwnnsscomp path: tools/ directory
      String defaultNwnnsscompPath = new File(System.getProperty("user.dir"), "tools").getAbsolutePath();
      // Check FileDecompiler.nwnnsscompPath first to ensure synchronization with actual runtime state
      String nwnnsscompPath = FileDecompiler.nwnnsscompPath != null ? FileDecompiler.nwnnsscompPath : this.getProperty("nwnnsscomp Path", defaultNwnnsscompPath);

      // If path looks like a file (ends with .exe), extract parent directory and filename
      String selectedCompilerNameTemp = null;
      String originalPath = nwnnsscompPath;
      File pathFile = new File(nwnnsscompPath);
      if (nwnnsscompPath.toLowerCase().endsWith(".exe") || pathFile.isFile()) {
         // It's a file path - extract folder and filename
         File parent = pathFile.getParentFile();
         if (parent != null) {
            nwnnsscompPath = parent.getAbsolutePath();
            selectedCompilerNameTemp = pathFile.getName();
         } else {
            // Fallback: try to extract from string
            int lastSlash = Math.max(originalPath.lastIndexOf('\\'), originalPath.lastIndexOf('/'));
            if (lastSlash >= 0 && lastSlash < originalPath.length() - 1) {
               nwnnsscompPath = originalPath.substring(0, lastSlash);
               selectedCompilerNameTemp = originalPath.substring(lastSlash + 1);
            }
         }
      }
      final String selectedCompilerName = selectedCompilerNameTemp;

      this.nwnnsscompPathField.setText(nwnnsscompPath);
      // Populate combobox and update compiler info after setting the path
      File folder = new File(nwnnsscompPath);
      File actualFolder = null;
      
      if (folder.exists() && folder.isDirectory()) {
         actualFolder = folder;
      } else if (folder.exists() && folder.isFile()) {
         // If it's a file, get parent
         File parent = folder.getParentFile();
         if (parent != null && parent.isDirectory()) {
            actualFolder = parent;
         }
      } else {
         // Folder doesn't exist, but use it anyway (might be created later)
         // Check if it looks like a directory path (doesn't end with .exe)
         if (!nwnnsscompPath.toLowerCase().endsWith(".exe")) {
            actualFolder = folder;
         }
      }
      
      if (actualFolder != null) {
         populateCompilerComboBox(actualFolder);
         // If we had a specific compiler selected, try to select it in the combobox
         if (selectedCompilerName != null && !selectedCompilerName.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
               for (int i = 0; i < this.nwnnsscompComboBox.getItemCount(); i++) {
                  String item = (String) this.nwnnsscompComboBox.getItemAt(i);
                  if (item.equals(selectedCompilerName)) {
                     this.nwnnsscompComboBox.setSelectedIndex(i);
                     break;
                  }
               }
            });
         }
      }
      updateCompilerInfo();

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
      String nwnnsscompPath = this.getProperty("nwnnsscomp Path", "");
      FileDecompiler.nwnnsscompPath = nwnnsscompPath.isEmpty() ? null : nwnnsscompPath;
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
      String defaultNwnnsscompPath = new File(new File(System.getProperty("user.dir"), "tools"), "nwnnsscomp.exe").getAbsolutePath();
      this.setProperty("nwnnsscomp Path", defaultNwnnsscompPath);
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

      // nwnnsscomp Path
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weightx = 0.0;
      panel.add(new JLabel("nwnnsscomp.exe:"), gbc);
      gbc.gridx = 1;
      gbc.weightx = 1.0;
      this.nwnnsscompPathField = new JTextField(30);
      this.nwnnsscompPathField.setToolTipText("Directory containing nwnnsscomp.exe files");
      // Add document listener to update compiler combobox when path changes
      this.nwnnsscompPathField.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            handlePathFieldChange();
         }
         @Override
         public void removeUpdate(DocumentEvent e) {
            handlePathFieldChange();
         }
         @Override
         public void changedUpdate(DocumentEvent e) {
            handlePathFieldChange();
         }
      });
      panel.add(this.nwnnsscompPathField, gbc);
      gbc.gridx = 2;
      gbc.weightx = 0.0;
      this.browseNwnnsscompButton = new JButton("Browse...");
      this.browseNwnnsscompButton.addActionListener(this);
      panel.add(this.browseNwnnsscompButton, gbc);

      // Compiler selection combobox
      gbc.gridx = 3;
      gbc.weightx = 0.0;
      this.nwnnsscompComboBox = new JComboBox<>();
      this.nwnnsscompComboBox.setToolTipText("Select which nwnnsscomp.exe to use");
      this.nwnnsscompComboBox.addActionListener(e -> {
         if (e.getSource() == this.nwnnsscompComboBox && this.nwnnsscompComboBox.getSelectedItem() != null) {
            String selectedCompiler = (String) this.nwnnsscompComboBox.getSelectedItem();
            String folderPath = this.nwnnsscompPathField.getText().trim();
            if (!folderPath.isEmpty() && !selectedCompiler.isEmpty()) {
               // Update the path to point to the selected compiler
               File folder = new File(folderPath);
               if (folder.isFile()) {
                  folder = folder.getParentFile();
               }
               if (folder != null && folder.isDirectory()) {
                  File compilerFile = new File(folder, selectedCompiler);
                  // Save the full path to the compiler file
                  String fullPath = compilerFile.getAbsolutePath();
                  // Update FileDecompiler to use this specific compiler
                  FileDecompiler.nwnnsscompPath = fullPath;
                  // Save to settings
                  this.setProperty("nwnnsscomp Path", fullPath);
                  updateCompilerInfo();
               }
            }
         }
      });
      panel.add(this.nwnnsscompComboBox, gbc);

      // K1 nwscript Path
      gbc.gridx = 0;
      gbc.gridy = 3;
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
      gbc.gridy = 4;
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

   /**
    * Finds the compiler executable using the same fallback logic as FileDecompiler.getCompilerFile().
    * This ensures the settings window shows the compiler that will actually be used.
    *
    * @param configuredPath The configured path from the settings field (may be empty)
    * @return A CompilerResolutionResult containing the found compiler file and whether it's a fallback
    */
   private CompilerResolutionResult findCompilerFile(String configuredPath) {
      // Priority order: primary first, then secondary, then others
      String[] compilerNames = {
         "nwnnsscomp.exe",              // Primary - generic name (highest priority)
         "nwnnsscomp_kscript.exe",      // Secondary - KOTOR Scripting Tool
         "nwnnsscomp_tslpatcher.exe",   // TSLPatcher variant
         "nwnnsscomp_v1.exe"            // v1.3 first public release
      };

      String configuredPathTrimmed = configuredPath != null ? configuredPath.trim() : "";
      boolean isConfigured = !configuredPathTrimmed.isEmpty();

      // 1. Try configured path (if set) - all compiler filenames
      if (isConfigured) {
         File configuredDir = new File(configuredPathTrimmed);
         if (configuredDir.isDirectory()) {
            // If it's a directory, try all filenames in it
            for (String name : compilerNames) {
               File candidate = new File(configuredDir, name);
               if (candidate.exists() && candidate.isFile()) {
                  return new CompilerResolutionResult(candidate, false, "Configured directory: " + configuredPathTrimmed);
               }
            }
         } else {
            // If it's a file, check if it exists
            if (configuredDir.exists() && configuredDir.isFile()) {
               return new CompilerResolutionResult(configuredDir, false, "Configured path: " + configuredPathTrimmed);
            }
            // Also try other filenames in the same directory
            File parent = configuredDir.getParentFile();
            if (parent != null) {
               for (String name : compilerNames) {
                  File candidate = new File(parent, name);
                  if (candidate.exists() && candidate.isFile()) {
                     return new CompilerResolutionResult(candidate, true, "Fallback in configured directory: " + parent.getAbsolutePath());
                  }
               }
            }
         }
      }

      // 2. Try tools/ directory - all compiler filenames
      File toolsDir = new File(System.getProperty("user.dir"), "tools");
      for (String name : compilerNames) {
         File candidate = new File(toolsDir, name);
         if (candidate.exists() && candidate.isFile()) {
            return new CompilerResolutionResult(candidate, true, "Fallback: tools/ directory");
         }
      }

      // 3. Try current working directory - all compiler filenames
      File cwd = new File(System.getProperty("user.dir"));
      for (String name : compilerNames) {
         File candidate = new File(cwd, name);
         if (candidate.exists() && candidate.isFile()) {
            return new CompilerResolutionResult(candidate, true, "Fallback: current directory");
         }
      }

      // 4. Try NCSDecomp installation directory - all compiler filenames
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null && !ncsDecompDir.equals(cwd)) {
         for (String name : compilerNames) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp directory");
            }
         }
         // Also try tools/ subdirectory of NCSDecomp directory
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : compilerNames) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp tools/ directory");
            }
         }
      }

      // Not found
      return null;
   }

   /**
    * Gets the NCSDecomp installation directory using the same logic as FileDecompiler.
    */
   private File getNCSDecompDirectory() {
      try {
         // Try to get the location of the jar/exe file
         java.net.URL location = Settings.class.getProtectionDomain().getCodeSource().getLocation();
         if (location != null) {
            String path = location.getPath();
            if (path != null) {
               // Handle URL-encoded paths
               if (path.startsWith("file:")) {
                  path = path.substring(5);
               }
               // Decode URL encoding
               try {
                  path = java.net.URLDecoder.decode(path, "UTF-8");
               } catch (java.io.UnsupportedEncodingException e) {
                  // Fall through with original path
               }
               File jarFile = new File(path);
               if (jarFile.exists()) {
                  File parent = jarFile.getParentFile();
                  if (parent != null) {
                     return parent;
                  }
               }
            }
         }
      } catch (Exception e) {
         // Fall through to user.dir
      }
      // Fallback to user.dir if we can't determine jar location
      return new File(System.getProperty("user.dir"));
   }

   /**
    * Result of compiler file resolution.
    */
   private static class CompilerResolutionResult {
      final File file;
      final boolean isFallback;
      final String source;

      CompilerResolutionResult(File file, boolean isFallback, String source) {
         this.file = file;
         this.isFallback = isFallback;
         this.source = source;
      }
   }

   /**
    * Handles changes to the nwnnsscomp path field.
    * Extracts parent folder if a file path is entered, then populates the combobox.
    */
   private void handlePathFieldChange() {
      String path = this.nwnnsscompPathField.getText().trim();
      if (path.isEmpty()) {
         this.nwnnsscompComboBox.removeAllItems();
         return;
      }

      File pathFile = new File(path);
      final File folder;

      if (pathFile.isFile()) {
         // If it's a file, extract parent folder
         File parentFolder = pathFile.getParentFile();
         folder = parentFolder;
         // Update the field to show the folder path
         if (parentFolder != null) {
            final String folderPath = parentFolder.getAbsolutePath();
            SwingUtilities.invokeLater(() -> {
               if (!this.nwnnsscompPathField.getText().equals(folderPath)) {
                  this.nwnnsscompPathField.setText(folderPath);
               }
            });
         } else {
            this.nwnnsscompComboBox.removeAllItems();
            return;
         }
      } else if (pathFile.isDirectory()) {
         folder = pathFile;
      } else {
         this.nwnnsscompComboBox.removeAllItems();
         return;
      }

      if (folder != null && folder.exists()) {
         populateCompilerComboBox(folder);
      } else {
         this.nwnnsscompComboBox.removeAllItems();
      }
      updateCompilerInfo();
   }

   /**
    * Populates the compiler combobox with detected nwnnsscomp.exe files in the specified folder.
    * Only includes files that start with "nwnnsscomp" prefix.
    */
   private void populateCompilerComboBox(File folder) {
      if (folder == null || !folder.isDirectory()) {
         this.nwnnsscompComboBox.removeAllItems();
         return;
      }

      // Get all files in the directory that start with "nwnnsscomp"
      File[] files = folder.listFiles((dir, name) -> {
         String lowerName = name.toLowerCase();
         return lowerName.startsWith("nwnnsscomp") && lowerName.endsWith(".exe");
      });

      String currentSelection = (String) this.nwnnsscompComboBox.getSelectedItem();
      this.nwnnsscompComboBox.removeAllItems();

      if (files != null && files.length > 0) {
         // Sort files by name for consistent display
         java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

         for (File file : files) {
            this.nwnnsscompComboBox.addItem(file.getName());
         }

         // Try to restore previous selection if it still exists
         if (currentSelection != null) {
            for (int i = 0; i < this.nwnnsscompComboBox.getItemCount(); i++) {
               if (this.nwnnsscompComboBox.getItemAt(i).equals(currentSelection)) {
                  this.nwnnsscompComboBox.setSelectedIndex(i);
                  return;
               }
            }
         }

         // If no previous selection or it doesn't exist, select the first item
         if (this.nwnnsscompComboBox.getItemCount() > 0) {
            this.nwnnsscompComboBox.setSelectedIndex(0);
         }
      }
   }

   /**
    * Updates the compiler combobox tooltip based on the selected compiler.
    * Detects the compiler version by SHA256 hash and displays metadata.
    */
   private void updateCompilerInfo() {
      String selectedCompiler = (String) this.nwnnsscompComboBox.getSelectedItem();
      String folderPath = this.nwnnsscompPathField.getText().trim();

      if (selectedCompiler == null || selectedCompiler.isEmpty() || folderPath.isEmpty()) {
         this.nwnnsscompComboBox.setToolTipText("No compiler selected");
         return;
      }

      File folder = new File(folderPath);
      if (folder.isFile()) {
         folder = folder.getParentFile();
      }

      if (folder == null || !folder.isDirectory()) {
         this.nwnnsscompComboBox.setToolTipText("Invalid directory: " + folderPath);
         return;
      }

      File compilerFile = new File(folder, selectedCompiler);

      if (!compilerFile.exists() || !compilerFile.isFile()) {
         this.nwnnsscompComboBox.setToolTipText("Compiler file not found: " + compilerFile.getAbsolutePath());
         return;
      }

      try {
         // Calculate SHA256 hash
         String sha256 = HashUtil.calculateSHA256(compilerFile);

         // Look up compiler
         KnownExternalCompilers compiler = KnownExternalCompilers.fromSha256(sha256);

         if (compiler != null) {
            // Format tooltip with metadata
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html><b>").append(compiler.getName()).append("</b><br>");
            tooltip.append("Path: ").append(compilerFile.getAbsolutePath()).append("<br>");
            tooltip.append("<br>Author: ").append(compiler.getAuthor()).append("<br>");
            tooltip.append("Release Date: ").append(compiler.getReleaseDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("<br>");
            tooltip.append("SHA256: ").append(sha256.substring(0, 16)).append("...<br>");
            tooltip.append("<br><b>Compile Args:</b><br>");
            String[] compileArgs = compiler.getCompileArgs();
            if (compileArgs.length > 0) {
               tooltip.append(String.join(" ", compileArgs));
            } else {
               tooltip.append("(not supported)");
            }
            tooltip.append("<br><br><b>Decompile Args:</b><br>");
            if (compiler.supportsDecompilation()) {
               tooltip.append(String.join(" ", compiler.getDecompileArgs()));
            } else {
               tooltip.append("(not supported)");
            }
            tooltip.append("</html>");

            this.nwnnsscompComboBox.setToolTipText(tooltip.toString());
         } else {
            // Unknown compiler
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html><b>Unknown Compiler</b><br>");
            tooltip.append("Path: ").append(compilerFile.getAbsolutePath()).append("<br>");
            tooltip.append("<br>SHA256: ").append(sha256).append("<br>");
            tooltip.append("This compiler version is not recognized.<br>");
            tooltip.append("It may not be fully supported.</html>");
            this.nwnnsscompComboBox.setToolTipText(tooltip.toString());
         }
      } catch (IOException e) {
         this.nwnnsscompComboBox.setToolTipText("Error reading compiler file: " + e.getMessage());
      }
   }

   /**
    * Returns an emoji representing the compiler type.
    */
   private String getCompilerEmoji(KnownExternalCompilers compiler) {
      switch (compiler) {
         case TSLPATCHER:
            return "🔧";
         case KOTOR_TOOL:
            return "🛠️";
         case V1:
            return "📦";
         case KOTOR_SCRIPTING_TOOL:
            return "⚙️";
         case XOREOS:
            return "🌐";
         case KNSSCOMP:
            return "✨";
         default:
            return "📄";
      }
   }
}

