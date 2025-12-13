// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.stack.Variable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import java.awt.FlowLayout;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Element;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.SwingUtilities;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 * Swing GUI application for decompiling NCS scripts and viewing results.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Load/save/open multiple scripts with tabbed views.</li>
 * <li>Display subroutine/variable tree, generated code, and bytecode
 * diffs.</li>
 * <li>Wire drag-and-drop, menus, keyboard shortcuts, and scroll/link
 * behavior.</li>
 * </ul>
 */
public class Decompiler extends JFrame implements DropTargetListener, KeyListener, ChangeListener,
      TreeSelectionListener, ActionListener, WindowListener, MouseListener, AdjustmentListener, CaretListener {
   public static Settings settings = new Settings();
   public static final double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
   public static final double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private static final long serialVersionUID = 1L;
   private JSplitPane upperJSplitPane;
   private JSplitPane mainJSplitPane;
   private JScrollPane jTreeScrollPane;
   private JTree jTree;
   private JTabbedPane jTB;
   private JTextComponent jTA; // Changed to JTextComponent to support both JTextArea and JTextPane
   private DropTarget dropTarget;
   private JPanel leftPanel;
   private JTextPane status;
   private JScrollPane statusScrollPane;
   private JComboBox<String> logLevelFilter;
   private static final String[] LOG_LEVELS = {"TRACE", "DEBUG", "INFO", "WARNING", "ERROR"};
   private static final int DEFAULT_LOG_LEVEL_INDEX = 2; // INFO

   // Store all log lines for filtering
   private static class LogLine {
      final String text;
      final LogSeverity severity;

      LogLine(String text, LogSeverity severity) {
         this.text = text;
         this.severity = severity;
      }
   }
   private final java.util.List<LogLine> allLogLines = new java.util.ArrayList<>();

   // Buffer last 1000 log lines for error display
   private static final int ERROR_BUFFER_SIZE = 1000;
   private final java.util.List<String> recentLogBuffer = new java.util.ArrayList<>();

   /**
    * Log severity levels.
    */
   private enum LogSeverity {
      TRACE,
      DEBUG,
      INFO,
      WARNING,
      ERROR,
   }
   private transient Element rootElement;
   private TitledBorder titledBorder;
   private int mark;
   private String temp;
   private JPanel panel;
   private File file;
   private Hashtable<String, Vector<Variable>> hash_Func2VarVec;
   private JComponent[] panels;
   private JTextPane origByteCodeJTA;
   private JTextPane newByteCodeJTA;
   private String currentNodeString;
   private transient Map<JComponent, File> hash_TabComponent2File;
   private transient Map<JComponent, Hashtable<String, Vector<Variable>>> hash_TabComponent2Func2VarVec;
   private transient Map<JComponent, TreeModel> hash_TabComponent2TreeModel;
   protected static List<File> unsavedFiles;
   private static Set<File> filesBeingLoaded = new HashSet<>();
   private transient FileDecompiler fileDecompiler = new FileDecompiler();
   private JToolBar commandBar;
   private JTextField treeFilterField;
   private JLabel statusBarLabel;
   private JPanel workspaceCards;
   private JLabel emptyStateLabel;
   private static final String CARD_EMPTY = "empty";
   private static final String CARD_TABS = "tabs";
   private PrintStream originalOut;
   private PrintStream originalErr;
   private static final String PROJECT_URL = "https://bolabaden.org";
   private static final String GITHUB_URL = "https://github.com/bolabaden";
   private static final String SPONSOR_URL = "https://github.com/sponsors/th3w1zard1";

   static {
      settings.load();
      String outputDir = settings.getProperty("Output Directory");
      // If output directory is not set or empty, use default: ./ncsdecomp_converted
      if (outputDir == null || outputDir.isEmpty() || !new File(outputDir).isDirectory()) {
         String defaultOutputDir = new File(System.getProperty("user.dir"), "ncsdecomp_converted").getAbsolutePath();
         // If default doesn't exist, try to create it, otherwise prompt user
         File defaultDir = new File(defaultOutputDir);
         if (!defaultDir.exists()) {
            System.out.println("[INFO] Decompiler: CREATING default output directory: " + defaultDir.getAbsolutePath());
            if (defaultDir.mkdirs()) {
               System.out.println("[INFO] Decompiler: Created default output directory: " + defaultDir.getAbsolutePath());
               settings.setProperty("Output Directory", defaultOutputDir);
            } else {
               System.out.println("[INFO] Decompiler: Failed to create default output directory: " + defaultDir.getAbsolutePath());
               // If we can't create it, prompt user
               settings.setProperty("Output Directory", chooseOutputDirectory());
            }
         } else {
            settings.setProperty("Output Directory", defaultOutputDir);
         }
         settings.save();
      }
      // Apply game variant setting to FileDecompiler
      String gameVariant = settings.getProperty("Game Variant", "k1").toLowerCase();
      FileDecompiler.isK2Selected = gameVariant.equals("k2") || gameVariant.equals("tsl") || gameVariant.equals("2");
      FileDecompiler.preferSwitches = Boolean.parseBoolean(settings.getProperty("Prefer Switches", "false"));
      FileDecompiler.strictSignatures = Boolean.parseBoolean(settings.getProperty("Strict Signatures", "false"));
   }

   public Decompiler() throws HeadlessException, DecompilerException {
      super("NCSDecomp");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setJMenuBar(this.buildMenuBar());
      this.registerKeyboardShortcuts();

      this.hash_TabComponent2Func2VarVec = new HashMap<>();
      this.hash_TabComponent2File = new HashMap<>();
      this.hash_TabComponent2TreeModel = new HashMap<>();
      unsavedFiles = new ArrayList<>();

      // Navigation tree with inline search
      this.jTree = new JTree(TreeModelFactory.getEmptyModel());
      this.jTree.setEditable(true);
      this.jTree.addKeyListener(this);
      this.jTree.addTreeSelectionListener(this);
      this.jTreeScrollPane = new JScrollPane(this.jTree);

      this.treeFilterField = new JTextField();
      this.treeFilterField.putClientProperty("JTextField.placeholderText", "Search functions/variables");
      this.treeFilterField.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            Decompiler.this.applyTreeFilter(treeFilterField.getText());
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            Decompiler.this.applyTreeFilter(treeFilterField.getText());
         }

         @Override
         public void changedUpdate(DocumentEvent e) {
            Decompiler.this.applyTreeFilter(treeFilterField.getText());
         }
      });

      JPanel navHeader = new JPanel(new BorderLayout());
      navHeader.add(new JLabel("Scripts"), BorderLayout.WEST);
      navHeader.add(this.treeFilterField, BorderLayout.CENTER);

      this.leftPanel = new JPanel(new BorderLayout());
      this.leftPanel.add(navHeader, BorderLayout.NORTH);
      this.leftPanel.add(this.jTreeScrollPane, BorderLayout.CENTER);

      // Workspace tabs
      this.jTB = new JTabbedPane();
      this.jTB.addChangeListener(this);
      this.jTB.setPreferredSize(new Dimension(900, 720));
      this.dropTarget = new DropTarget(this.jTB, this);

      // Workspace cards to show empty state when no files are open
      this.workspaceCards = new JPanel(new CardLayout());
      this.emptyStateLabel = new JLabel(
            "<html><div style='text-align:center;'><h2>Drop .ncs or .nss files here</h2><div>Or use File → Open to start decompiling</div></div></html>",
            SwingConstants.CENTER);
      this.emptyStateLabel.setBorder(new EmptyBorder(32, 16, 32, 16));

      // Create support card
      JPanel supportCard = createSupportCard();

      // Create empty panel with drag text and support card side by side
      JPanel emptyPanel = new JPanel(new BorderLayout());
      JPanel contentPanel = new JPanel(new BorderLayout(32, 0));
      contentPanel.setBorder(new EmptyBorder(32, 32, 32, 32));
      contentPanel.add(this.emptyStateLabel, BorderLayout.CENTER);
      contentPanel.add(supportCard, BorderLayout.EAST);
      emptyPanel.add(contentPanel, BorderLayout.CENTER);
      this.workspaceCards.add(emptyPanel, CARD_EMPTY);
      this.workspaceCards.add(this.jTB, CARD_TABS);
      this.dropTarget = new DropTarget(this.workspaceCards, this);

      this.upperJSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, this.workspaceCards);
      this.upperJSplitPane.setDividerLocation(260);
      this.upperJSplitPane.setResizeWeight(0.2);
      this.upperJSplitPane.setDividerSize(6);

      // Status/logging area with styled text support
      this.status = new JTextPane();
      this.status.setEditable(false);
      this.status.setFont(new Font("Consolas", Font.PLAIN, 11)); // Industry standard monospace font
      this.status.setBackground(java.awt.Color.WHITE);
      this.statusScrollPane = new JScrollPane(this.status);
      this.statusScrollPane.setVerticalScrollBarPolicy(22);
      this.statusScrollPane.setHorizontalScrollBarPolicy(32);
      JPopupMenu statusPopupMenu = new JPopupMenu();
      JMenuItem clearItem = new JMenuItem("Clear");
      clearItem.addActionListener(this);
      statusPopupMenu.add(clearItem);
      this.status.setComponentPopupMenu(statusPopupMenu);

      // Log level filter combobox
      this.logLevelFilter = new JComboBox<>(LOG_LEVELS);
      this.logLevelFilter.setSelectedIndex(DEFAULT_LOG_LEVEL_INDEX);
      this.logLevelFilter.addActionListener(e -> filterLogs());

      JPanel statusBar = new JPanel(new BorderLayout());
      this.statusBarLabel = new JLabel("Ready");
      statusBar.add(this.statusBarLabel, BorderLayout.WEST);
      JPanel filterPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 0));
      filterPanel.add(new JLabel("Log Level: "));
      filterPanel.add(this.logLevelFilter);
      statusBar.add(filterPanel, BorderLayout.EAST);

      JPanel statusWrapper = new JPanel(new BorderLayout());
      statusWrapper.add(statusBar, BorderLayout.NORTH);
      statusWrapper.add(this.statusScrollPane, BorderLayout.CENTER);

      this.mainJSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.upperJSplitPane, statusWrapper);
      this.mainJSplitPane.setDividerLocation(0.72);
      this.mainJSplitPane.setResizeWeight(0.8);
      this.mainJSplitPane.setDividerSize(6);

      // Toolbar
      this.commandBar = this.buildToolBar();
      this.commandBar.setFloatable(false);

      this.setLayout(new BorderLayout());
      this.add(this.commandBar, BorderLayout.NORTH);
      this.add(this.mainJSplitPane, BorderLayout.CENTER);

      this.setSize(new Dimension(1200, 820));
      this.setLocationRelativeTo(null);
      this.addWindowListener(this);
      this.updateWorkspaceCard();
      this.updateMenuAndToolbarState();

      // Redirect System.out and System.err to both terminal and GUI log area
      this.setupStreamRedirection();

      this.setVisible(true);
   }

   /**
    * Sets up redirection of System.out and System.err to both the terminal and the
    * GUI log area. This ensures all debug output appears in both places.
    */
   private void setupStreamRedirection() {
      // Store original streams
      this.originalOut = System.out;
      this.originalErr = System.err;

      // Create dual-output streams
      DualOutputPrintStream dualOut = new DualOutputPrintStream(this.originalOut, this.status, this);
      DualOutputPrintStream dualErr = new DualOutputPrintStream(this.originalErr, this.status, this);

      // Replace System.out and System.err
      System.setOut(dualOut);
      System.setErr(dualErr);
   }

   /**
    * A PrintStream that writes to both the original stream (terminal) and the GUI
    * log area. This ensures all output appears in both places with color coding.
    */
   private static class DualOutputPrintStream extends PrintStream {
      private final PrintStream original;
      private final JTextPane guiLog;
      private final javax.swing.text.StyledDocument doc;
      private final Decompiler decompiler;

      public DualOutputPrintStream(PrintStream original, JTextPane guiLog, Decompiler decompiler) {
         super(new DualOutputStream(original, guiLog, decompiler), true);
         this.original = original;
         this.guiLog = guiLog;
         this.doc = guiLog.getStyledDocument();
         this.decompiler = decompiler;
      }

      /**
       * Thread-safe method to append text to the GUI log area with color coding.
       * Uses SwingUtilities.invokeLater to ensure updates happen on the EDT.
       */
      private void appendToGui(String text) {
         appendToGuiLog(text, this.guiLog, this.doc, this.decompiler);
      }

      /**
       * Instance method to append text to GUI log with color coding.
       * Parses ANSI escape codes and converts them to Swing colors.
       * Stores all log lines for filtering.
       * Also extracts compilation errors and displays them prominently.
       */
      private static void appendToGuiLog(String text, JTextPane guiLog, javax.swing.text.StyledDocument doc, Decompiler decompiler) {
         if (text != null && guiLog != null && doc != null && decompiler != null) {
            // Parse severity first
            LogSeverity severity = parseLogSeverity(text);

            // Store the log line (always store, even if filtered)
            synchronized (decompiler.allLogLines) {
               decompiler.allLogLines.add(new LogLine(text, severity));
            }

            // Buffer recent log lines for error display
            synchronized (decompiler.recentLogBuffer) {
               decompiler.recentLogBuffer.add(text);
               // Keep only last 1000 lines
               if (decompiler.recentLogBuffer.size() > ERROR_BUFFER_SIZE) {
                  decompiler.recentLogBuffer.remove(0);
               }
            }

            // Check for error conditions and dump buffer
            checkAndDisplayErrors(text, decompiler);

            // Only append if it should be shown based on current filter
            if (shouldShowLog(severity, decompiler)) {
               SwingUtilities.invokeLater(() -> {
                  try {
                     // Parse ANSI codes and render with colors
                     parseAndAppendAnsiText(text, guiLog, doc);

                     // Auto-scroll to bottom
                     guiLog.setCaretPosition(doc.getLength());
                  } catch (Exception e) {
                     // Fallback to plain append if styled insert fails
                     try {
                        int start = doc.getLength();
                        doc.insertString(start, text, null);
                        guiLog.setCaretPosition(doc.getLength());
                     } catch (javax.swing.text.BadLocationException e2) {
                        // Ignore
                     }
                  }
               });
            }
         }
      }

      /**
       * Checks for error conditions and dumps the recent log buffer to error display.
       */
      private static void checkAndDisplayErrors(String text, Decompiler decompiler) {
         if (text == null || decompiler == null) {
            return;
         }

         // Strip ANSI codes for checking
         String textWithoutAnsi = text.replaceAll("\u001B\\[[0-9;]+m", "");
         String upper = textWithoutAnsi.toUpperCase();

         // Check for compilation errors
         boolean isCompilationError = upper.contains("COMPILATION ABORTED") ||
                                      upper.contains("COMPILATION FAILED") ||
                                      (upper.contains("ERROR") && upper.contains("COMPILATION"));

         // Check for decompilation errors
         boolean isDecompilationError = (upper.contains("DECOMPILE") && (upper.contains("FAILED") || upper.contains("ERROR"))) ||
                                        upper.contains("BYTECODE CAPTURE FAILED") ||
                                        upper.contains("EXCEPTION DURING EXTERNAL DECOMPILE");

         if (isCompilationError || isDecompilationError) {
            // Find the currently selected file
            SwingUtilities.invokeLater(() -> {
               JComponent tabComponent = decompiler.getSelectedTabComponent();
               if (tabComponent != null) {
                  File file = decompiler.hash_TabComponent2File.get(tabComponent);
                  if (file != null) {
                     // Dump the entire buffer
                     synchronized (decompiler.recentLogBuffer) {
                        if (!decompiler.recentLogBuffer.isEmpty()) {
                           if (isCompilationError) {
                              decompiler.dumpErrorBufferToCompilationView(tabComponent, decompiler.recentLogBuffer);
                           }
                           if (isDecompilationError) {
                              decompiler.dumpErrorBufferToDecompilationView(tabComponent, decompiler.recentLogBuffer);
                           }
                        }
                     }
                  }
               }
            });
         }
      }


      /**
       * Parses ANSI escape codes and appends text with appropriate colors.
       * Supports foreground colors, bold, dim, and reset codes.
       */
      static void parseAndAppendAnsiText(String text, JTextPane guiLog, javax.swing.text.StyledDocument doc) {
         try {
            // Pattern to match ANSI escape sequences: \033[...m or \x1B[...m
            java.util.regex.Pattern ansiPattern = java.util.regex.Pattern.compile("\u001B\\[([0-9;]+)m");
            java.util.regex.Matcher matcher = ansiPattern.matcher(text);

            int lastEnd = 0;
            javax.swing.text.SimpleAttributeSet currentAttr = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setFontFamily(currentAttr, "Consolas");
            javax.swing.text.StyleConstants.setFontSize(currentAttr, 11);

            // Default colors for light theme
            java.awt.Color defaultColor = new java.awt.Color(0, 0, 0); // Black for light theme
            java.awt.Color currentColor = defaultColor;
            boolean isBold = false;

            javax.swing.text.StyleConstants.setForeground(currentAttr, currentColor);

            while (matcher.find()) {
               // Append text before the ANSI code
               if (matcher.start() > lastEnd) {
                  String plainText = text.substring(lastEnd, matcher.start());
                  if (!plainText.isEmpty()) {
                     doc.insertString(doc.getLength(), plainText, currentAttr);
                  }
               }

               // Parse ANSI code
               String codes = matcher.group(1);
               String[] codeArray = codes.split(";");

               for (String codeStr : codeArray) {
                  try {
                     int code = Integer.parseInt(codeStr);

                     if (code == 0) {
                        // Reset
                        currentColor = defaultColor;
                        isBold = false;
                     } else if (code == 1) {
                        // Bold
                        isBold = true;
                     } else if (code == 2) {
                        // Dim - make color lighter (for light theme, we'll use a lighter shade)
                        // For now, just continue with current color
                     } else if (code >= 30 && code <= 37) {
                        // Foreground colors (light theme adjusted)
                        currentColor = getAnsiColor(code - 30, isBold, true);
                     } else if (code >= 90 && code <= 97) {
                        // Bright foreground colors
                        currentColor = getAnsiColor(code - 90, true, true);
                     }
                  } catch (NumberFormatException e) {
                     // Ignore invalid codes
                  }
               }

               // Update attribute set
               currentAttr = new javax.swing.text.SimpleAttributeSet();
               javax.swing.text.StyleConstants.setFontFamily(currentAttr, "Consolas");
               javax.swing.text.StyleConstants.setFontSize(currentAttr, 11);
               javax.swing.text.StyleConstants.setForeground(currentAttr, currentColor);
               if (isBold) {
                  javax.swing.text.StyleConstants.setBold(currentAttr, true);
               }

               lastEnd = matcher.end();
            }

            // Append remaining text
            if (lastEnd < text.length()) {
               String plainText = text.substring(lastEnd);
               if (!plainText.isEmpty()) {
                  doc.insertString(doc.getLength(), plainText, currentAttr);
               }
            }
         } catch (javax.swing.text.BadLocationException e) {
            // Fallback: just append without ANSI parsing
            try {
               // Strip ANSI codes and append plain text
               String plainText = text.replaceAll("\u001B\\[[0-9;]+m", "");
               doc.insertString(doc.getLength(), plainText, null);
            } catch (javax.swing.text.BadLocationException e2) {
               // Ignore
            }
         }
      }

      /**
       * Converts ANSI color code to Swing Color for light theme.
       * @param colorIndex 0-7 (black, red, green, yellow, blue, magenta, cyan, white)
       * @param bright Whether this is a bright color
       * @param lightTheme Whether we're using a light theme
       */
      private static java.awt.Color getAnsiColor(int colorIndex, boolean bright, boolean lightTheme) {
         if (lightTheme) {
            // Light theme colors (darker, more visible on white)
            switch (colorIndex) {
               case 0: return new java.awt.Color(0, 0, 0); // Black
               case 1: return new java.awt.Color(200, 0, 0); // Red
               case 2: return new java.awt.Color(0, 150, 0); // Green
               case 3: return new java.awt.Color(200, 120, 0); // Yellow/Orange
               case 4: return new java.awt.Color(0, 0, 200); // Blue
               case 5: return new java.awt.Color(200, 0, 200); // Magenta
               case 6: return new java.awt.Color(0, 150, 200); // Cyan
               case 7: return new java.awt.Color(100, 100, 100); // White (gray for light theme)
               default: return new java.awt.Color(0, 0, 0);
            }
         } else {
            // Dark theme colors
            switch (colorIndex) {
               case 0: return new java.awt.Color(0, 0, 0);
               case 1: return new java.awt.Color(255, 0, 0);
               case 2: return new java.awt.Color(0, 255, 0);
               case 3: return new java.awt.Color(255, 255, 0);
               case 4: return new java.awt.Color(0, 0, 255);
               case 5: return new java.awt.Color(255, 0, 255);
               case 6: return new java.awt.Color(0, 255, 255);
               case 7: return new java.awt.Color(255, 255, 255);
               default: return new java.awt.Color(255, 255, 255);
            }
         }
      }

      /**
       * Parses log severity from log line.
       * Strips ANSI codes first to properly detect severity levels.
       */
      private static LogSeverity parseLogSeverity(String text) {
         if (text == null) {
            return LogSeverity.INFO;
         }

         // Strip ANSI escape codes before parsing (they interfere with pattern matching)
         String textWithoutAnsi = text.replaceAll("\u001B\\[[0-9;]+m", "");
         String upper = textWithoutAnsi.toUpperCase();

         // Check for TRACE (must come before DEBUG check)
         if (upper.contains("TRACE") && (upper.contains("[TRACE]") || upper.matches(".*\\bTRACE\\b.*"))) {
            return LogSeverity.TRACE;
         } else if (upper.contains("DEBUG") && (upper.contains("[DEBUG]") || upper.matches(".*\\bDEBUG\\b.*"))) {
            // Check for specific DEBUG patterns that should be treated as INFO
            if (upper.contains("DEBUG COMPILER") || upper.contains("DEBUG FILEDECOMPILER")
                  || upper.contains("DEBUG EXTERNAL") || upper.contains("DEBUG REGISTRY")
                  || upper.contains("DEBUG COMPILEREXECUTION") || upper.contains("DEBUG LOADNSSFILE")
                  || upper.contains("DEBUG DECOMPILE:") || upper.contains("DEBUG CAPTURE")) {
               // These should have been changed to INFO, but handle legacy
               return LogSeverity.INFO;
            }
            // Check if it's a control flow DEBUG (transformJump, checkEnd, etc.) - these are TRACE level
            if (upper.contains("DEBUG TRANSFORM") || upper.contains("DEBUG CHECK")
                  || upper.contains("DEBUG REMOVE") || upper.contains("DEBUG ISAT")
                  || upper.contains("DEBUG ISRETURN") || upper.contains("DEBUG TRANSFORMJZ")
                  || upper.contains("DEBUG TRANSFORMCOPY") || upper.contains("DEBUG TRANSFORMMOVE")
                  || upper.contains("DEBUG TRANSFORMBINARY") || upper.contains("DEBUG REMOVELASTEXP")) {
               return LogSeverity.TRACE;
            }
            return LogSeverity.DEBUG;
         } else if (upper.contains("[INFO]") || upper.contains("INFO:") || upper.contains("[NCSDECOMP]")) {
            return LogSeverity.INFO;
         } else if (upper.contains("[WARN]") || upper.contains("WARNING:") || upper.contains("WARNING -")) {
            return LogSeverity.WARNING;
         } else if (upper.contains("[ERROR]") || upper.contains("ERROR:") || upper.contains("EXCEPTION")) {
            return LogSeverity.ERROR;
         }
         // Default to INFO for unmarked logs
         return LogSeverity.INFO;
      }

      /**
       * Checks if log should be shown based on current filter level.
       */
      private static boolean shouldShowLog(LogSeverity severity, Decompiler decompiler) {
         if (decompiler == null || decompiler.logLevelFilter == null) {
            return true;
         }
         String selectedLevel = (String) decompiler.logLevelFilter.getSelectedItem();
         if (selectedLevel == null) {
            return true;
         }

         int selectedIndex = getSeverityIndex(selectedLevel);
         int logIndex = severity.ordinal();

         // Show if log severity is >= selected filter level
         return logIndex >= selectedIndex;
      }

      /**
       * Gets severity index for comparison.
       */
      static int getSeverityIndex(String level) {
         for (int i = 0; i < LOG_LEVELS.length; i++) {
            if (LOG_LEVELS[i].equals(level)) {
               return i;
            }
         }
         return DEFAULT_LOG_LEVEL_INDEX;
      }

      /**
       * Custom OutputStream that writes to both the original PrintStream and the GUI
       * log area.
       */
      private static class DualOutputStream extends OutputStream {
         private final PrintStream original;
         private final JTextPane guiLog;
         private final Decompiler decompiler;
         private final StringBuilder lineBuffer = new StringBuilder();

         public DualOutputStream(PrintStream original, JTextPane guiLog, Decompiler decompiler) {
            this.original = original;
            this.guiLog = guiLog;
            this.decompiler = decompiler;
         }

         @Override
         public void write(int b) throws IOException {
            original.write(b);
            char c = (char) b;
            lineBuffer.append(c);
            if (c == '\n') {
               flushLine();
            }
         }

         @Override
         public void write(byte[] b, int off, int len) throws IOException {
            original.write(b, off, len);
            String text = new String(b, off, len);
            lineBuffer.append(text);
            if (text.contains("\n")) {
               flushLine();
            }
         }

         @Override
         public void flush() throws IOException {
            original.flush();
            flushLine();
         }

         @Override
         public void close() throws IOException {
            flushLine();
            // Don't close the original stream - it's System.out/err
         }

         private void flushLine() {
            if (lineBuffer.length() > 0) {
               String line = lineBuffer.toString();
               lineBuffer.setLength(0);
               appendToGui(line);
            }
         }

         private void appendToGui(String text) {
            // Delegate to static method for color coding
            if (decompiler != null && guiLog != null) {
               javax.swing.text.StyledDocument doc = guiLog.getStyledDocument();
               DualOutputPrintStream.appendToGuiLog(text, guiLog, doc, decompiler);
            }
         }
      }

      @Override
      public void print(String s) {
         if (s != null) {
            original.print(s);
            appendToGui(s);
         }
      }

      @Override
      public void println(String x) {
         original.println(x);
         appendToGui(x != null ? x : "null");
         appendToGui("\n");
      }

      @Override
      public void println() {
         original.println();
         appendToGui("\n");
      }

      @Override
      public void print(boolean b) {
         original.print(b);
         appendToGui(String.valueOf(b));
      }

      @Override
      public void print(char c) {
         original.print(c);
         appendToGui(String.valueOf(c));
      }

      @Override
      public void print(int i) {
         original.print(i);
         appendToGui(String.valueOf(i));
      }

      @Override
      public void print(long l) {
         original.print(l);
         appendToGui(String.valueOf(l));
      }

      @Override
      public void print(float f) {
         original.print(f);
         appendToGui(String.valueOf(f));
      }

      @Override
      public void print(double d) {
         original.print(d);
         appendToGui(String.valueOf(d));
      }

      @Override
      public void print(char[] s) {
         original.print(s);
         if (s != null) {
            appendToGui(new String(s));
         }
      }

      @Override
      public void print(Object obj) {
         original.print(obj);
         appendToGui(String.valueOf(obj));
      }

      @Override
      public void println(boolean x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(char x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(int x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(long x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(float x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(double x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

      @Override
      public void println(char[] x) {
         original.println(x);
         if (x != null) {
            appendToGui(new String(x));
         }
         appendToGui("\n");
      }

      @Override
      public void println(Object x) {
         original.println(x);
         appendToGui(String.valueOf(x));
         appendToGui("\n");
      }

   }

   public static void main(String[] args) {
      try {
         new Decompiler();
      } catch (DecompilerException var2) {
         JOptionPane.showMessageDialog(null, var2.getMessage());
      } catch (Exception var3) {
         JOptionPane.showMessageDialog(null, var3.getStackTrace());
      }
   }

   private JMenuBar buildMenuBar() {
      JMenuBar menuBar = new JMenuBar();

      JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);
      fileMenu.add(this.menuItem("Open", KeyEvent.VK_O, true));
      JMenuItem closeItem = this.menuItem("Close", KeyEvent.VK_W, true);
      closeItem.setEnabled(false);
      fileMenu.add(closeItem);
      JMenuItem closeAllItem = this.menuItem("Close All", KeyEvent.VK_E, true);
      closeAllItem.setEnabled(false);
      fileMenu.add(closeAllItem);
      JMenuItem saveItem = this.menuItem("Save", KeyEvent.VK_S, true);
      saveItem.setEnabled(false);
      fileMenu.add(saveItem);
      JMenuItem saveAllItem = this.menuItem("Save All", KeyEvent.VK_A, true);
      saveAllItem.setEnabled(false);
      fileMenu.add(saveAllItem);
      fileMenu.addSeparator();
      fileMenu.add(this.menuItem("Exit", KeyEvent.VK_Q, true));
      menuBar.add(fileMenu);

      JMenu viewMenu = new JMenu("View");
      viewMenu.setMnemonic(KeyEvent.VK_V);
      viewMenu.add(this.menuItem("View Decompiled Code", KeyEvent.VK_D, false));
      viewMenu.add(this.menuItem("View Byte Code", KeyEvent.VK_B, false));
      JCheckBoxMenuItem linkScrollBars = new JCheckBoxMenuItem("Link Scroll Bars");
      linkScrollBars.setSelected(Boolean.parseBoolean(settings.getProperty("Link Scroll Bars")));
      linkScrollBars.addActionListener(this);
      viewMenu.add(linkScrollBars);
      menuBar.add(viewMenu);

      JMenu optionsMenu = new JMenu("Options");
      optionsMenu.setMnemonic(KeyEvent.VK_O);
      optionsMenu.add(this.menuItem("Settings", KeyEvent.VK_P, true));
      menuBar.add(optionsMenu);

      JMenu helpMenu = new JMenu("Help");
      helpMenu.setMnemonic(KeyEvent.VK_H);
      helpMenu.add(this.menuItem("About", KeyEvent.VK_F1, false));
      helpMenu.addSeparator();
      helpMenu.add(this.menuItem("bolabaden.org", KeyEvent.VK_F2, false));
      helpMenu.add(this.menuItem("GitHub Repo", KeyEvent.VK_F3, false));
      helpMenu.add(this.menuItem("Sponsor NCSDecomp", KeyEvent.VK_F4, false));
      menuBar.add(helpMenu);

      return menuBar;
   }

   private JMenuItem menuItem(String text, int key, boolean withCtrl) {
      JMenuItem item = new JMenuItem(text);
      item.addActionListener(this);
      if (key > 0) {
         item.setAccelerator(
               withCtrl ? KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK) : KeyStroke.getKeyStroke(key, 0));
         item.setMnemonic(key);
      }
      return item;
   }

   /**
    * Registers comprehensive industry-standard keyboard shortcuts. These shortcuts
    * work globally across the application.
    */
   private void registerKeyboardShortcuts() {
      javax.swing.JRootPane rootPane = this.getRootPane();
      javax.swing.InputMap inputMap = rootPane.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
      javax.swing.ActionMap actionMap = rootPane.getActionMap();

      // File operations
      registerShortcut(inputMap, actionMap, "ctrl O", "Open", e -> open());
      registerShortcut(inputMap, actionMap, "ctrl S", "Save", e -> {
         int idx = jTB.getSelectedIndex();
         if (idx >= 0)
            save(idx);
      });
      registerShortcut(inputMap, actionMap, "ctrl shift S", "SaveAll", e -> saveAll());
      registerShortcut(inputMap, actionMap, "ctrl W", "Close", e -> {
         int idx = jTB.getSelectedIndex();
         if (idx >= 0)
            close(idx);
      });
      registerShortcut(inputMap, actionMap, "ctrl shift W", "CloseAll", e -> closeAll());
      registerShortcut(inputMap, actionMap, "ctrl F4", "CloseTab", e -> {
         int idx = jTB.getSelectedIndex();
         if (idx >= 0)
            close(idx);
      });
      registerShortcut(inputMap, actionMap, "ctrl Q", "Exit", e -> exit());

      // Tab navigation
      registerShortcut(inputMap, actionMap, "ctrl TAB", "NextTab", e -> {
         int count = jTB.getTabCount();
         if (count > 0)
            jTB.setSelectedIndex((jTB.getSelectedIndex() + 1) % count);
      });
      registerShortcut(inputMap, actionMap, "ctrl shift TAB", "PrevTab", e -> {
         int count = jTB.getTabCount();
         if (count > 0)
            jTB.setSelectedIndex((jTB.getSelectedIndex() - 1 + count) % count);
      });
      registerShortcut(inputMap, actionMap, "ctrl PAGE_DOWN", "NextTabAlt", e -> {
         int count = jTB.getTabCount();
         if (count > 0)
            jTB.setSelectedIndex((jTB.getSelectedIndex() + 1) % count);
      });
      registerShortcut(inputMap, actionMap, "ctrl PAGE_UP", "PrevTabAlt", e -> {
         int count = jTB.getTabCount();
         if (count > 0)
            jTB.setSelectedIndex((jTB.getSelectedIndex() - 1 + count) % count);
      });

      // Numbered tab access (Ctrl+1 through Ctrl+9)
      for (int i = 1; i <= 9; i++) {
         final int tabIndex = i - 1;
         registerShortcut(inputMap, actionMap, "ctrl " + i, "GoToTab" + i, e -> {
            if (tabIndex < jTB.getTabCount())
               jTB.setSelectedIndex(tabIndex);
         });
      }

      // View switching
      registerShortcut(inputMap, actionMap, "F2", "ViewDecompiledCode", e -> {
         if (jTB.getSelectedIndex() >= 0)
            setTabComponentPanel(0);
      });
      registerShortcut(inputMap, actionMap, "F3", "ViewByteCode", e -> {
         if (jTB.getSelectedIndex() >= 0)
            setTabComponentPanel(1);
      });

      // Refresh/Recompile
      registerShortcut(inputMap, actionMap, "F5", "Refresh", e -> {
         int idx = jTB.getSelectedIndex();
         if (idx >= 0)
            save(idx);
      });

      // Settings
      registerShortcut(inputMap, actionMap, "ctrl COMMA", "Settings", e -> settings.show());
      registerShortcut(inputMap, actionMap, "ctrl P", "SettingsAlt", e -> settings.show());

      // Find/Replace (placeholders for future implementation)
      registerShortcut(inputMap, actionMap, "ctrl F", "Find", e -> showFindDialog());
      registerShortcut(inputMap, actionMap, "ctrl H", "Replace", e -> showReplaceDialog());
      registerShortcut(inputMap, actionMap, "ctrl G", "GoToLine", e -> showGoToLineDialog());

      // Selection and editing (work on current text pane)
      registerShortcut(inputMap, actionMap, "ctrl A", "SelectAll",
            e -> getCurrentTextPane().ifPresent(pane -> pane.selectAll()));
      registerShortcut(inputMap, actionMap, "ctrl D", "DuplicateLine", e -> duplicateCurrentLine());
      registerShortcut(inputMap, actionMap, "ctrl SLASH", "ToggleComment", e -> toggleComment());

      // Zoom
      registerShortcut(inputMap, actionMap, "ctrl PLUS", "ZoomIn", e -> adjustFontSize(2));
      registerShortcut(inputMap, actionMap, "ctrl EQUALS", "ZoomInAlt", e -> adjustFontSize(2)); // Ctrl+= (same key as
                                                                                                 // +)
      registerShortcut(inputMap, actionMap, "ctrl MINUS", "ZoomOut", e -> adjustFontSize(-2));
      registerShortcut(inputMap, actionMap, "ctrl 0", "ResetZoom", e -> resetFontSize());

      // Full screen toggle
      registerShortcut(inputMap, actionMap, "F11", "ToggleFullScreen", e -> toggleFullScreen());

      // Help
      registerShortcut(inputMap, actionMap, "F1", "Help", e -> showAboutDialog());
   }

   private void registerShortcut(javax.swing.InputMap inputMap, javax.swing.ActionMap actionMap, String keystroke,
         String actionKey, java.awt.event.ActionListener action) {
      inputMap.put(javax.swing.KeyStroke.getKeyStroke(keystroke), actionKey);
      actionMap.put(actionKey, new javax.swing.AbstractAction() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent e) {
            action.actionPerformed(e);
         }
      });
   }

   private java.util.Optional<javax.swing.text.JTextComponent> getCurrentTextPane() {
      int selectedIndex = jTB.getSelectedIndex();
      if (selectedIndex < 0)
         return java.util.Optional.empty();

      javax.swing.JComponent tabComponent = (javax.swing.JComponent) jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null)
         return java.util.Optional.empty();

      Object clientProperty = jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof javax.swing.JComponent[]))
         return java.util.Optional.empty();

      javax.swing.JComponent[] panels = (javax.swing.JComponent[]) clientProperty;
      if (panels.length == 0 || !(panels[0] instanceof javax.swing.JSplitPane))
         return java.util.Optional.empty();

      javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) panels[0];
      java.awt.Component leftComp = splitPane.getLeftComponent();
      if (leftComp instanceof javax.swing.JScrollPane) {
         javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) leftComp;
         java.awt.Component view = scrollPane.getViewport().getView();
         if (view instanceof javax.swing.text.JTextComponent) {
            return java.util.Optional.of((javax.swing.text.JTextComponent) view);
         }
      }
      return java.util.Optional.empty();
   }

   private void showFindDialog() {
      javax.swing.JOptionPane.showMessageDialog(this, "Find functionality coming soon!", "Find",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
   }

   private void showReplaceDialog() {
      javax.swing.JOptionPane.showMessageDialog(this, "Replace functionality coming soon!", "Replace",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
   }

   private void showGoToLineDialog() {
      javax.swing.JOptionPane.showMessageDialog(this, "Go to Line functionality coming soon!", "Go to Line",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
   }

   private void duplicateCurrentLine() {
      getCurrentTextPane().ifPresent(pane -> {
         try {
            int caretPos = pane.getCaretPosition();
            int lineStart = pane.getDocument().getDefaultRootElement().getElementIndex(caretPos);
            javax.swing.text.Element line = pane.getDocument().getDefaultRootElement().getElement(lineStart);
            int start = line.getStartOffset();
            int end = line.getEndOffset();
            String lineText = pane.getDocument().getText(start, end - start);
            pane.getDocument().insertString(end, lineText, null);
         } catch (javax.swing.text.BadLocationException ex) {
            // Ignore
         }
      });
   }

   private void toggleComment() {
      getCurrentTextPane().ifPresent(pane -> {
         try {
            int start = pane.getSelectionStart();
            int end = pane.getSelectionEnd();
            if (start == end) {
               // No selection, comment current line
               int lineIndex = pane.getDocument().getDefaultRootElement().getElementIndex(start);
               javax.swing.text.Element line = pane.getDocument().getDefaultRootElement().getElement(lineIndex);
               int lineStart = line.getStartOffset();
               int lineEnd = line.getEndOffset();
               String lineText = pane.getDocument().getText(lineStart, lineEnd - lineStart);

               if (lineText.trim().startsWith("//")) {
                  // Uncomment
                  String uncommented = lineText.replaceFirst("//\\s*", "");
                  pane.getDocument().remove(lineStart, lineEnd - lineStart);
                  pane.getDocument().insertString(lineStart, uncommented, null);
               } else {
                  // Comment
                  pane.getDocument().insertString(lineStart, "// ", null);
               }
            }
         } catch (javax.swing.text.BadLocationException ex) {
            // Ignore
         }
      });
   }

   private void adjustFontSize(int delta) {
      getCurrentTextPane().ifPresent(pane -> {
         java.awt.Font currentFont = pane.getFont();
         int newSize = Math.max(8, Math.min(72, currentFont.getSize() + delta));
         pane.setFont(currentFont.deriveFont((float) newSize));
      });
   }

   private void resetFontSize() {
      getCurrentTextPane().ifPresent(pane -> {
         pane.setFont(pane.getFont().deriveFont(12f));
      });
   }

   private boolean isFullScreen = false;

   private void toggleFullScreen() {
      java.awt.GraphicsDevice device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
      if (isFullScreen) {
         device.setFullScreenWindow(null);
         isFullScreen = false;
      } else {
         this.dispose();
         this.setUndecorated(true);
         device.setFullScreenWindow(this);
         this.setVisible(true);
         isFullScreen = true;
      }
   }

   private JToolBar buildToolBar() {
      JToolBar bar = new JToolBar();
      bar.add(this.createToolbarButton("Open"));
      bar.add(this.createToolbarButton("Save"));
      bar.add(this.createToolbarButton("Save All"));
      bar.addSeparator();
      bar.add(this.createToolbarButton("View Decompiled Code"));
      bar.add(this.createToolbarButton("View Byte Code"));
      bar.addSeparator();
      bar.add(this.createToolbarButton("Settings"));
      return bar;
   }

   private JButton createToolbarButton(String action) {
      JButton button = new JButton(action);
      button.setActionCommand(action); // JButton doesn't set this from label text
      button.setFocusable(false);
      button.addActionListener(this);
      button.putClientProperty("action", action);
      return button;
   }

   private void applyTreeFilter(String query) {
      if (query == null) {
         return;
      }
      String normalized = query.trim().toLowerCase();
      TreeModel model = this.jTree.getModel();
      if (model == null || model.getRoot() == null) {
         return;
      }
      if (normalized.isEmpty()) {
         this.jTree.clearSelection();
         return;
      }

      List<TreePath> matches = new ArrayList<>();
      this.collectMatchingPaths(model, new TreePath(model.getRoot()), normalized, matches);
      if (!matches.isEmpty()) {
         TreePath first = matches.get(0);
         this.jTree.setSelectionPath(first);
         this.jTree.scrollPathToVisible(first);
      }
   }

   private void collectMatchingPaths(TreeModel model, TreePath path, String query, List<TreePath> matches) {
      Object node = path.getLastPathComponent();
      if (node != null && node.toString().toLowerCase().contains(query)) {
         matches.add(path);
      }
      int childCount = model.getChildCount(node);
      for (int i = 0; i < childCount; i++) {
         Object child = model.getChild(node, i);
         collectMatchingPaths(model, path.pathByAddingChild(child), query, matches);
      }
   }

   private void updateWorkspaceCard() {
      CardLayout cl = (CardLayout) this.workspaceCards.getLayout();
      if (this.jTB.getTabCount() == 0) {
         cl.show(this.workspaceCards, CARD_EMPTY);
      } else {
         cl.show(this.workspaceCards, CARD_TABS);
      }
      this.updateMenuAndToolbarState();
   }

   private void updateMenuAndToolbarState() {
      boolean hasTabs = this.jTB.getTabCount() > 0;
      int selectedIndex = this.jTB.getSelectedIndex();
      boolean hasSelection = selectedIndex >= 0 && selectedIndex < this.jTB.getTabCount();

      JMenuBar menuBar = this.getJMenuBar();
      if (menuBar != null && menuBar.getMenuCount() > 0) {
         JMenu fileMenu = menuBar.getMenu(0);
         if (fileMenu != null) {
            if (fileMenu.getItemCount() > 1) {
               fileMenu.getItem(1).setEnabled(hasSelection); // Close
            }
            if (fileMenu.getItemCount() > 2) {
               fileMenu.getItem(2).setEnabled(hasTabs); // Close All
            }
            if (fileMenu.getItemCount() > 3) {
               fileMenu.getItem(3).setEnabled(hasSelection); // Save
            }
            if (fileMenu.getItemCount() > 4) {
               fileMenu.getItem(4).setEnabled(hasTabs); // Save All
            }
         }
      }

      // Update toolbar buttons
      if (this.commandBar != null) {
         for (java.awt.Component comp : this.commandBar.getComponents()) {
            if (comp instanceof JButton) {
               JButton button = (JButton) comp;
               String action = (String) button.getClientProperty("action");
               if (action != null) {
                  if (action.equals("Save") || action.equals("Close")) {
                     button.setEnabled(hasSelection);
                  } else if (action.equals("Save All") || action.equals("Close All")) {
                     button.setEnabled(hasTabs);
                  } else if (action.equals("View Decompiled Code") || action.equals("View Byte Code")) {
                     button.setEnabled(hasSelection);
                  }
               }
            }
         }
      }
   }

   private void updateTabLabel(JPanel tabPanel, boolean unsaved) {
      if (tabPanel == null || tabPanel.getComponentCount() == 0) {
         return;
      }
      java.awt.Component center = tabPanel.getComponent(0);
      if (center instanceof JLabel) {
         JLabel label = (JLabel) center;
         String text = label.getText();
         if (text == null) {
            return;
         }
         String clean = text.endsWith(" *") ? text.substring(0, text.length() - 2) : text;
         label.setText(unsaved ? clean + " *" : clean);
      }
   }

   /**
    * Safely gets the tab component for the currently selected tab.
    *
    * @return The tab component, or null if no valid tab is selected
    */
   JComponent getSelectedTabComponent() {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex < 0 || selectedIndex >= this.jTB.getTabCount()) {
         return null;
      }
      return (JComponent) this.jTB.getTabComponentAt(selectedIndex);
   }

   private void openLink(String url, String statusMessage) {
      try {
         if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
            this.setStatus(statusMessage);
         } else {
            this.setStatus("Desktop browsing not supported on this platform.");
         }
      } catch (Exception ex) {
         this.setStatus("Unable to open link: " + url);
      }
   }

   private void setStatus(String message) {
      if (this.statusBarLabel != null) {
         this.statusBarLabel.setText(message);
      }
   }

   private void showAboutDialog() {
      String aboutHtml = "<html>" + "<h2>NCSDecomp</h2>"
            + "<p>KotOR / TSL NCS script decompiler rebuilt with modern workflows. "
            + "Origins in the classic DeNCS tooling; re-implemented with improved heuristics, UI, and headless/CLI support.</p>"
            + "<ul>"
            + "<li><b>What</b>: Decompile .ncs bytecode to readable .nss; inspect bytecode and regenerated output side-by-side.</li>"
            + "<li><b>How</b>: Swing GUI with drag/drop, multi-tab workspaces, and bytecode diffing; CLI for batch/headless runs.</li>"
            + "<li><b>Why</b>: Provide a reliable, transparent pipeline for modders and reverse-engineers working on KotOR/TSL.</li>"
            + "<li><b>When</b>: Use for day-to-day modding, audits, or batch decompilation workflows.</li>"
            + "<li><b>Links</b>: " + "<a href='https://bolabaden.org'>bolabaden.org</a> · "
            + "<a href='https://github.com/OldRepublicDevs'>github.com/OldRepublicDevs</a> · "
            + "<a href='https://github.com/bolabaden'>github.com/bolabaden</a></li>" + "</ul>" + "</html>";

      JLabel content = new JLabel(aboutHtml);
      content.setBorder(new EmptyBorder(12, 12, 12, 12));
      JOptionPane.showMessageDialog(this, content, "About NCSDecomp", JOptionPane.INFORMATION_MESSAGE);
   }

   @Override
   public void dragEnter(DropTargetDragEvent dtde) {
   }

   @Override
   public void dragExit(DropTargetEvent dte) {
   }

   @Override
   public void dragOver(DropTargetDragEvent dtde) {
   }

   @Override
   public void dropActionChanged(DropTargetDragEvent dtde) {
   }

   @Override
   public void drop(DropTargetDropEvent dtde) {
      try {
         Transferable tr = dtde.getTransferable();
         DataFlavor[] flavors = tr.getTransferDataFlavors();

         for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].isFlavorJavaFileListType()) {
               dtde.acceptDrop(3);
               @SuppressWarnings("unchecked")
               final List<File> rawList = (List<File>) tr.getTransferData(flavors[i]);
               final List<File> list = new ArrayList<>();
               for (File file : rawList) {
                  if (file != null) {
                     list.add(file);
                  }
               }
               Thread openThread = new Thread(() -> {
                  Decompiler.this.open(list.toArray(new File[0]));
               });
               openThread.setDaemon(true);
               openThread.setName("FileOpen-" + System.currentTimeMillis());
               openThread.start();
               dtde.dropComplete(true);
               return;
            }
         }

         dtde.rejectDrop();
      } catch (Exception var6) {
         var6.printStackTrace();
         dtde.rejectDrop();
      }
   }

   @Override
   public void stateChanged(ChangeEvent arg0) {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex >= 0 && selectedIndex < this.jTB.getTabCount()) {
         JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(selectedIndex);
         if (tabComponent != null) {
            TreeModel model = this.hash_TabComponent2TreeModel.get(tabComponent);
            if (model != null) {
               this.jTree.setModel(model);
               return;
            }
         }
      }
      this.jTree.setModel(TreeModelFactory.getEmptyModel());
   }

   @Override
   public void keyPressed(KeyEvent arg0) {
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex < 0 || selectedIndex >= this.jTB.getTabCount()) {
         return; // No valid tab selected
      }

      JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }

      if (arg0.getSource() instanceof JTree) {
         TreePath changedPath = ((JTree) arg0.getSource()).getSelectionPath();
         File file = this.hash_TabComponent2File.get(tabComponent);
         if (file == null) {
            return; // File not found for this tab
         }

         Object clientProperty = this.jTB.getClientProperty(tabComponent);
         if (!(clientProperty instanceof JComponent[])) {
            return; // Invalid client property
         }

         JComponent[] panels = (JComponent[]) clientProperty;
         if (changedPath != null && changedPath.getPathCount() == 2) {
            Hashtable<String, Vector<Variable>> func2VarVec = this.fileDecompiler.updateSubName(file,
                  this.currentNodeString, changedPath.getLastPathComponent().toString());
            this.hash_TabComponent2Func2VarVec.put(tabComponent, func2VarVec);
            this.hash_TabComponent2TreeModel.put(tabComponent, this.jTree.getModel());
            if (panels.length > 0 && panels[0] instanceof JSplitPane) {
               JSplitPane decompSplitPane = (JSplitPane) panels[0];
               java.awt.Component leftComp = decompSplitPane.getLeftComponent();
               if (leftComp instanceof JScrollPane) {
                  JScrollPane scrollPane = (JScrollPane) leftComp;
                  if (scrollPane.getViewport().getView() instanceof JTextComponent) {
                     this.jTA = (JTextComponent) scrollPane.getViewport().getView();
                     // Disable highlighting and dirty marking during setText to prevent freeze and false dirty flags
                     if (this.jTA instanceof JTextPane) {
                        NWScriptSyntaxHighlighter.setSkipHighlighting((JTextPane) this.jTA, true);
                        this.jTA.putClientProperty("Decompiler.programmaticUpdate", true);
                     }

                     this.jTA.setText(this.fileDecompiler.getGeneratedCode(file));
                     this.jTA.setCaretPosition(0);

                     // Re-enable highlighting and dirty marking, then apply highlighting immediately
                     if (this.jTA instanceof JTextPane) {
                        JTextPane textPane = (JTextPane) this.jTA;
                        NWScriptSyntaxHighlighter.setSkipHighlighting(textPane, false);
                        textPane.putClientProperty("Decompiler.programmaticUpdate", false);
                        NWScriptSyntaxHighlighter.applyHighlightingImmediate(textPane);
                     }
                  }
               }
            }
         } else if (changedPath != null) {
            TreeNode subroutineNode = (TreeNode) changedPath.getParentPath().getLastPathComponent();
            Hashtable<String, Vector<Variable>> func2VarVec = this.hash_TabComponent2Func2VarVec.get(tabComponent);
            if (func2VarVec != null) {
               Vector<Variable> variables = func2VarVec.get(subroutineNode.toString());
               if (variables != null) {
                  int nodeIndex = subroutineNode.getIndex((TreeNode) changedPath.getLastPathComponent());
                  if (nodeIndex >= 0 && nodeIndex < variables.size()) {
                     Variable changedVar = variables.get(nodeIndex);
                     changedVar.name(changedPath.getLastPathComponent().toString());
                     if (panels.length > 0 && panels[0] instanceof JSplitPane) {
                        JSplitPane decompSplitPane = (JSplitPane) panels[0];
                        java.awt.Component leftComp = decompSplitPane.getLeftComponent();
                        if (leftComp instanceof JScrollPane) {
                           JScrollPane scrollPane = (JScrollPane) leftComp;
                           if (scrollPane.getViewport().getView() instanceof JTextComponent) {
                              this.jTA = (JTextComponent) scrollPane.getViewport().getView();
                              // Disable highlighting and dirty marking during setText to prevent freeze and false dirty flags
                              if (this.jTA instanceof JTextPane) {
                                 NWScriptSyntaxHighlighter.setSkipHighlighting((JTextPane) this.jTA, true);
                                 this.jTA.putClientProperty("Decompiler.programmaticUpdate", true);
                              }

                              this.jTA.setText(this.fileDecompiler.regenerateCode(file));
                              this.jTA.setCaretPosition(0);

                              // Re-enable highlighting and dirty marking, then apply highlighting immediately
                              if (this.jTA instanceof JTextPane) {
                                 JTextPane textPane = (JTextPane) this.jTA;
                                 NWScriptSyntaxHighlighter.setSkipHighlighting(textPane, false);
                                 textPane.putClientProperty("Decompiler.programmaticUpdate", false);
                                 NWScriptSyntaxHighlighter.applyHighlightingImmediate(textPane);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (changedPath != null) {
            this.currentNodeString = changedPath.getLastPathComponent().toString();
         }
      } else if (arg0.getSource() instanceof JTextComponent) {
         File file = this.hash_TabComponent2File.get(tabComponent);
         if (file != null && !unsavedFiles.contains(file)) {
            unsavedFiles.add(file);
         }
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel) tabComponent, true);
         }
      }
   }

   @Override
   public void keyTyped(KeyEvent arg0) {
   }

   @Override
   public void valueChanged(TreeSelectionEvent arg0) {
      if (((JTree) arg0.getSource()).getSelectionPath() != null) {
         this.currentNodeString = ((JTree) arg0.getSource()).getSelectionPath().getLastPathComponent().toString();
      }
   }

   @Override
   public void actionPerformed(ActionEvent arg0) {
      String cmd = arg0.getActionCommand();
      if (cmd == null) {
         return;
      }

      if (cmd.equals("Open")) {
         this.open();
      } else if (cmd.equals("Close")) {
         int selectedIndex = this.jTB.getSelectedIndex();
         if (selectedIndex >= 0) {
            this.close(selectedIndex);
         }
      } else if (cmd.equals("Close All")) {
         this.closeAll();
      } else if (cmd.equals("Save")) {
         int selectedIndex = this.jTB.getSelectedIndex();
         if (selectedIndex >= 0) {
            this.save(selectedIndex);
         }
      } else if (cmd.equals("Save All")) {
         this.saveAll();
      } else if (cmd.equals("Exit")) {
         exit();
      } else if (cmd.equals("Settings")) {
         settings.show();
      } else if (cmd.equals("Clear")) {
         // Clear both the display and stored log lines
         if (this.status != null) {
            try {
               javax.swing.text.StyledDocument doc = this.status.getStyledDocument();
               doc.remove(0, doc.getLength());
            } catch (javax.swing.text.BadLocationException e) {
               // Ignore
            }
         }
         synchronized (this.allLogLines) {
            this.allLogLines.clear();
         }
      } else if (cmd.equals("FilterLogs")) {
         filterLogs();
      } else if (cmd.equals("View Byte Code")) {
         if (this.jTB.getSelectedIndex() >= 0) {
            this.setTabComponentPanel(1);
         }
      } else if (cmd.equals("View Decompiled Code")) {
         if (this.jTB.getSelectedIndex() >= 0) {
            this.setTabComponentPanel(0);
         }
      } else if (cmd.equals("Link Scroll Bars")) {
         this.toggleLinkScrollBars();
      } else if (cmd.equals("About")) {
         this.showAboutDialog();
      } else if (cmd.equals("bolabaden.org")) {
         this.openLink(PROJECT_URL, "Opening bolabaden.org");
      } else if (cmd.equals("GitHub Repo")) {
         this.openLink(GITHUB_URL, "Opening GitHub repository");
      } else if (cmd.equals("Sponsor NCSDecomp")) {
         this.openLink(SPONSOR_URL, "Opening sponsor page");
      }
   }

   @Override
   public void windowActivated(WindowEvent arg0) {
   }

   @Override
   public void windowClosed(WindowEvent arg0) {
   }

   @Override
   public void windowClosing(WindowEvent arg0) {
      settings.save();
      exit();
   }

   @Override
   public void windowDeactivated(WindowEvent arg0) {
   }

   @Override
   public void windowDeiconified(WindowEvent arg0) {
   }

   @Override
   public void windowIconified(WindowEvent arg0) {
   }

   @Override
   public void windowOpened(WindowEvent arg0) {
   }

   @Override
   public void mouseClicked(MouseEvent arg0) {
   }

   @Override
   public void mouseEntered(MouseEvent arg0) {
   }

   @Override
   public void mouseExited(MouseEvent arg0) {
   }

   @Override
   public void mousePressed(MouseEvent arg0) {
   }

   @Override
   public void mouseReleased(MouseEvent arg0) {
      if (!(arg0.getSource() instanceof JLabel)) {
         return;
      }
      JLabel closeLabel = (JLabel) arg0.getSource();
      if (!(closeLabel.getParent() instanceof JPanel)) {
         return;
      }
      JPanel tabPanel = (JPanel) closeLabel.getParent();

      // Find the tab index that matches this panel
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(i);
         if (tabComponent == tabPanel) {
            this.close(i);
            break;
         }
      }
   }

   @Override
   public void adjustmentValueChanged(AdjustmentEvent arg0) {
      if (!Boolean.parseBoolean(settings.getProperty("Link Scroll Bars"))) {
         return;
      }

      java.awt.Component selectedComponent = this.jTB.getSelectedComponent();
      if (selectedComponent == null || !(selectedComponent instanceof JSplitPane)) {
         return; // No valid component selected
      }

      JSplitPane splitPane = (JSplitPane) selectedComponent;
      Object linkProperty = this.jTB.getClientProperty(selectedComponent);
      if (linkProperty == null) {
         return; // No link property set
      }

      java.awt.Component leftComp = splitPane.getLeftComponent();
      java.awt.Component rightComp = splitPane.getRightComponent();

      if (!(leftComp instanceof JScrollPane) || !(rightComp instanceof JScrollPane)) {
         return; // Invalid component structure
      }

      JScrollPane leftScroll = (JScrollPane) leftComp;
      JScrollPane rightScroll = (JScrollPane) rightComp;

      if (linkProperty.equals("left")) {
         rightScroll.getVerticalScrollBar().setValue(leftScroll.getVerticalScrollBar().getValue());
      } else {
         leftScroll.getVerticalScrollBar().setValue(rightScroll.getVerticalScrollBar().getValue());
      }
   }

   @Override
   public void caretUpdate(CaretEvent arg0) {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex < 0 || selectedIndex >= this.jTB.getTabCount()) {
         return; // No tab selected or invalid index
      }

      JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }

      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }

      JComponent[] panels = (JComponent[]) clientProperty;
      if (panels.length == 0 || !(panels[0] instanceof JPanel)) {
         return; // Invalid panel structure
      }

      // Line number display is now on the scroll pane border, not the split pane
      // Get the left scroll pane from the split pane
      if (!(panels[0] instanceof JSplitPane)) {
         return;
      }
      JSplitPane decompSplitPane = (JSplitPane) panels[0];
      java.awt.Component leftComp = decompSplitPane.getLeftComponent();
      if (!(leftComp instanceof JScrollPane)) {
         return;
      }
      JScrollPane leftScrollPane = (JScrollPane) leftComp;
      if (!(leftScrollPane.getBorder() instanceof TitledBorder)) {
         return; // Invalid border type
      }

      this.jTA = (JTextComponent) arg0.getSource();
      this.mark = this.jTA.getCaretPosition();
      this.rootElement = this.jTA.getDocument().getDefaultRootElement();
      this.titledBorder = (TitledBorder) leftScrollPane.getBorder();
      if (!(this.temp = Integer.toString(this.rootElement.getElementIndex(this.mark) + 1))
            .equals(this.titledBorder.getTitle())) {
         this.titledBorder.setTitle(this.temp);
         this.repaint();
      }
   }

   private JComponent[] newNCSTab(String text) {
      return this.newNCSTab(text, this.jTB.getTabCount());
   }

   /**
    * Creates a new tab in the editor for viewing and editing NCS decompiled code
    * and bytecode comparison.
    *
    * @param text  The label to display on the tab (usually the filename without
    *              extension).
    * @param index The index at which to insert the new tab.
    * @return Array of components for this tab: [0]=code panel, [1]=bytecode split
    *         pane.
    */
   private JComponent[] newNCSTab(String text, int index) {
      // Array for holding the panels: [0] = Decompilation panel, [1] = Bytecode split
      // view
      JComponent[] tabComponents = new JComponent[2];

      // --- Decompiled Code Split Pane ---
      JSplitPane decompSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      decompSplitPane.setDividerLocation(500);
      decompSplitPane.setDividerSize(5);

      // ---- Left: Original Decompiled Code Panel (Editable) ----
      JTextPane textPane = new JTextPane();
      textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
      textPane.setEditable(true);

      // Add undo/redo support
      UndoManager undoManager = new UndoManager();
      textPane.getDocument().addUndoableEditListener(new UndoableEditListener() {
         @Override
         public void undoableEditHappened(UndoableEditEvent e) {
            undoManager.addEdit(e.getEdit());
         }
      });
      textPane.putClientProperty("undoManager", undoManager);

      // Add undo/redo keyboard shortcuts
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
      textPane.getActionMap().put("undo", new AbstractAction("undo") {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (undoManager.canUndo()) {
               undoManager.undo();
            }
         }
      });
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
      textPane.getActionMap().put("redo", new AbstractAction("redo") {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (undoManager.canRedo()) {
               undoManager.redo();
            }
         }
      });
      // Also support Ctrl+Shift+Z for redo (common alternative)
      textPane.getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "redo");

      // Add document listener to mark files as dirty when text changes
      // Use a client property to track programmatic changes (similar to syntax highlighter)
      final String PROP_PROGRAMMATIC_UPDATE = "Decompiler.programmaticUpdate";
      DocumentListener dirtyMarker = new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            markFileAsDirty();
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            markFileAsDirty();
         }

         @Override
         public void changedUpdate(DocumentEvent e) {
            markFileAsDirty();
         }

         private void markFileAsDirty() {
            // Skip if this is a programmatic update (setText, etc.)
            Boolean isProgrammatic = (Boolean)textPane.getClientProperty(PROP_PROGRAMMATIC_UPDATE);
            if (isProgrammatic != null && isProgrammatic) {
               return;
            }

            JComponent tabComponent = Decompiler.this.getSelectedTabComponent();
            if (tabComponent != null) {
               File file = Decompiler.this.hash_TabComponent2File.get(tabComponent);
               // Don't mark as dirty if file is currently being loaded OR if it's a programmatic update
               if (file != null && !Decompiler.filesBeingLoaded.contains(file) && !Decompiler.unsavedFiles.contains(file)) {
                  Decompiler.unsavedFiles.add(file);
                  if (tabComponent instanceof JPanel) {
                     Decompiler.this.updateTabLabel((JPanel) tabComponent, true);
                  }
               }
            }
         }
      };
      textPane.getDocument().addDocumentListener(dirtyMarker);
      // Store reference to listener for potential removal if needed
      textPane.putClientProperty("dirtyMarkerListener", dirtyMarker);
      textPane.putClientProperty(PROP_PROGRAMMATIC_UPDATE, false);

      // Syntax highlighting, caret/keyboard event hooks, drag and drop, etc.
      textPane.addCaretListener(this);
      textPane.addKeyListener(this);
      this.dropTarget = new DropTarget(textPane, this);
      textPane.putClientProperty("dropTarget", this.dropTarget);
      textPane.getDocument().addDocumentListener(NWScriptSyntaxHighlighter.createHighlightingListener(textPane));

      JScrollPane codeScrollPane = new JScrollPane(textPane);
      codeScrollPane.setBorder(new TitledBorder("Decompiled Code"));
      codeScrollPane.getVerticalScrollBar().addAdjustmentListener(this);

      decompSplitPane.setLeftComponent(codeScrollPane);

      // Context menu for switching to bytecode view
      JPopupMenu codePopupMenu = new JPopupMenu();
      JMenuItem viewByteCodeItem = new JMenuItem("View Byte Code");
      viewByteCodeItem.addActionListener(this);
      codePopupMenu.add(viewByteCodeItem);
      textPane.setComponentPopupMenu(codePopupMenu);

      // ---- Right: Round-Trip Decompiled Code Panel (Read-Only) ----
      JTextPane roundTripTextPane = new JTextPane();
      roundTripTextPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
      roundTripTextPane.setEditable(false);

      this.dropTarget = new DropTarget(roundTripTextPane, this);
      roundTripTextPane.putClientProperty("dropTarget", this.dropTarget);
      roundTripTextPane.getDocument()
            .addDocumentListener(NWScriptSyntaxHighlighter.createHighlightingListener(roundTripTextPane));

      JScrollPane roundTripScrollPane = new JScrollPane(roundTripTextPane);
      roundTripScrollPane.setBorder(new TitledBorder("Round-Trip Decompiled Code"));
      roundTripScrollPane.getVerticalScrollBar().addAdjustmentListener(this);

      decompSplitPane.setRightComponent(roundTripScrollPane);

      // Context menu for round-trip pane
      JPopupMenu roundTripPopupMenu = new JPopupMenu();
      JMenuItem viewByteCodeItem2 = new JMenuItem("View Byte Code");
      viewByteCodeItem2.addActionListener(this);
      roundTripPopupMenu.add(viewByteCodeItem2);
      roundTripTextPane.setComponentPopupMenu(roundTripPopupMenu);

      tabComponents[0] = decompSplitPane;

      // --- Bytecode Comparison Split Pane ---
      JSplitPane byteCodeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      byteCodeSplitPane.setDividerLocation(320);
      byteCodeSplitPane.setDividerSize(5);

      // ---- Left: Original Bytecode Panel ----
      JTextPane origByteCodeArea = new JTextPane();
      origByteCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      origByteCodeArea.setEditable(false);

      this.dropTarget = new DropTarget(origByteCodeArea, this);
      origByteCodeArea.putClientProperty("dropTarget", this.dropTarget);
      origByteCodeArea.getDocument().addDocumentListener(BytecodeSyntaxHighlighter.createHighlightingListener(origByteCodeArea));

      JScrollPane origScrollPane = new JScrollPane(origByteCodeArea);
      origScrollPane.setBorder(new TitledBorder("Original Byte Code"));
      origScrollPane.getVerticalScrollBar().addAdjustmentListener(this);

      byteCodeSplitPane.setLeftComponent(origScrollPane);

      // Context menu for switching to decompiled code view, and scrollbars
      JPopupMenu bytecodePopupMenu = new JPopupMenu();

      JMenuItem viewDecompiledCodeItem = new JMenuItem("View Decompiled Code");
      viewDecompiledCodeItem.addActionListener(this);
      bytecodePopupMenu.add(viewDecompiledCodeItem);

      JCheckBoxMenuItem linkScrollBarsItem = new JCheckBoxMenuItem("Link Scroll Bars");
      linkScrollBarsItem.setSelected(Boolean.parseBoolean(settings.getProperty("Link Scroll Bars")));
      linkScrollBarsItem.addActionListener(this);
      bytecodePopupMenu.add(linkScrollBarsItem);

      origByteCodeArea.setComponentPopupMenu(bytecodePopupMenu);

      // ---- Right: Recompiled Bytecode Panel or Error Display ----
      JTextPane newByteCodeArea = new JTextPane();
      newByteCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      newByteCodeArea.setEditable(false);

      this.dropTarget = new DropTarget(newByteCodeArea, this);
      newByteCodeArea.putClientProperty("dropTarget", this.dropTarget);
      newByteCodeArea.getDocument().addDocumentListener(BytecodeSyntaxHighlighter.createHighlightingListener(newByteCodeArea));

      JScrollPane newScrollPane = new JScrollPane(newByteCodeArea);
      newScrollPane.setBorder(new TitledBorder("Recompiled Byte Code"));
      newScrollPane.getVerticalScrollBar().addAdjustmentListener(this);

      // Create error display panel (initially hidden, shown when errors occur)
      JTextPane errorDisplayArea = new JTextPane();
      errorDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      errorDisplayArea.setEditable(false);
      errorDisplayArea.setBackground(new java.awt.Color(255, 240, 240)); // Light red background
      errorDisplayArea.setForeground(new java.awt.Color(200, 0, 0)); // Dark red text

      JScrollPane errorScrollPane = new JScrollPane(errorDisplayArea);
      errorScrollPane.setBorder(new TitledBorder("Compilation Errors"));
      errorScrollPane.setVisible(false); // Hidden by default

      // Store references for later use
      newByteCodeArea.putClientProperty("errorDisplay", errorScrollPane);
      newByteCodeArea.putClientProperty("errorTextPane", errorDisplayArea);
      newByteCodeArea.putClientProperty("bytecodeDisplay", newScrollPane);
      newByteCodeArea.putClientProperty("bytecodeSplitPane", byteCodeSplitPane);

      byteCodeSplitPane.setRightComponent(newScrollPane);

      newByteCodeArea.setComponentPopupMenu(bytecodePopupMenu);

      tabComponents[1] = byteCodeSplitPane;

      // --- Tab Header Panel (label + close box) ---
      JPanel tabHeaderPanel = new JPanel(new BorderLayout());
      tabHeaderPanel.setOpaque(false);

      JLabel titleLabel = new JLabel(text);
      titleLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
      titleLabel.setOpaque(false);

      JLabel closeLabel = new JLabel("X");
      closeLabel.setOpaque(false);
      closeLabel.addMouseListener(this);

      tabHeaderPanel.add(titleLabel, BorderLayout.CENTER);
      tabHeaderPanel.add(closeLabel, BorderLayout.EAST);

      // --- Insert Tab and Tab Management ---
      this.jTB.insertTab(null, null, tabComponents[0], null, index);
      this.jTB.setTabComponentAt(index, tabHeaderPanel);
      this.jTB.putClientProperty(tabHeaderPanel, tabComponents);
      this.jTB.setSelectedIndex(index);

      updateWorkspaceCard();

      return tabComponents;
   }

   /**
    * Loads an NSS file directly (it's already source code, no decompilation
    * needed).
    *
    * @param file The NSS file to load
    */
   private void loadNssFile(File file) {
      this.appendStatus("[INFO] Loading NSS file...\n");
      this.statusScrollPane.getVerticalScrollBar().setValue(this.statusScrollPane.getVerticalScrollBar().getMaximum());
      this.appendStatus("[INFO] " + file.getName() + ": ");

      String fileContent = null;
      try {
      // Verify file exists before reading
      if (!file.exists() || !file.isFile()) {
         this.appendStatus("[ERROR] File does not exist or is not a file: " + file.getAbsolutePath() + "\n");
         return;
      }
      // Read the file content
      System.out.println("[INFO] Decompiler: READING file: " + file.getAbsolutePath());
      fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()),
         java.nio.charset.StandardCharsets.UTF_8);
      System.out.println("[INFO] Decompiler: Read file: " + file.getAbsolutePath() + " (length: " + fileContent.length() + " chars)");
      } catch (java.io.IOException e) {
      this.appendStatus("[ERROR] error reading file: " + e.getMessage() + "\n");
      JOptionPane.showMessageDialog(null, "Error reading NSS file: " + e.getMessage());
      return;
      }

      // Create tab and display content (keep original filename in tab name)
      String fileName = file.getName();
      // Mark file as being loaded to prevent marking as dirty during initial setText
      filesBeingLoaded.add(file);
      this.panels = this.newNCSTab(fileName);

      // Get the left side text pane from the split pane
      if (this.panels[0] instanceof JSplitPane) {
      JSplitPane decompSplitPane = (JSplitPane) this.panels[0];
      java.awt.Component leftComp = decompSplitPane.getLeftComponent();
      if (leftComp instanceof JScrollPane) {
         JScrollPane leftScrollPane = (JScrollPane) leftComp;
         JTextComponent codeArea = (JTextComponent) leftScrollPane.getViewport().getView();

         // Disable highlighting and dirty marking during setText to prevent freeze and false dirty flags
         if (codeArea instanceof JTextPane) {
            NWScriptSyntaxHighlighter.setSkipHighlighting((JTextPane) codeArea, true);
            codeArea.putClientProperty("Decompiler.programmaticUpdate", true);
         }

         codeArea.setText(fileContent);

         // Re-enable highlighting and dirty marking, then apply highlighting immediately
         if (codeArea instanceof JTextPane) {
            JTextPane textPane = (JTextPane) codeArea;
            NWScriptSyntaxHighlighter.setSkipHighlighting(textPane, false);
            textPane.putClientProperty("Decompiler.programmaticUpdate", false);
            NWScriptSyntaxHighlighter.applyHighlightingImmediate(textPane);
         }

         // NOTE: Do NOT remove from filesBeingLoaded yet - keep file protected until
         // fully loaded

         // Populate round-trip decompiled code panel (NSS -> NCS -> NSS)
         // For NSS files, we need to compile them first, then decompile the result
         try {
            java.awt.Component rightComp = decompSplitPane.getRightComponent();
            if (rightComp instanceof JScrollPane) {
            JScrollPane rightScrollPane = (JScrollPane) rightComp;
            if (rightScrollPane.getViewport().getView() instanceof JTextPane) {
               JTextPane roundTripPane = (JTextPane) rightScrollPane.getViewport().getView();

               System.out.println("[INFO] loadNssFile: Starting round-trip for NSS file: " + file.getAbsolutePath());

               // Compile the NSS file to NCS, then decompile it
               File compiledNcs = null;
               try {
                  // Use CompilerUtil to get compiler from Settings (GUI mode - NO FALLBACKS)
                  File compiler = CompilerUtil.getCompilerFromSettingsOrNull();
                  System.out.println("[INFO] loadNssFile: Found compiler via CompilerUtil: " +
                  (compiler != null ? compiler.getAbsolutePath() : "null"));

                  if (compiler != null && compiler.exists()) {
                  // Create output NCS file in same directory as input
                  // Extract base name without extension
                  String nssFileName = file.getName();
                  String compiledBaseName = nssFileName;
                  int lastDot = nssFileName.lastIndexOf('.');
                  if (lastDot > 0) {
                     compiledBaseName = nssFileName.substring(0, lastDot);
                  }
                  compiledNcs = new File(file.getParentFile(), compiledBaseName + ".ncs");

                  // Use NwnnsscompConfig to compile (same as FileDecompiler.externalCompile)
                  // Read isK2 from Settings directly (not from static field which might be stale)
                  String gameVariant = Decompiler.settings.getProperty("Game Variant", "k1").toLowerCase();
                  boolean isK2 = gameVariant.equals("k2") || gameVariant.equals("tsl") || gameVariant.equals("2");
                  // Also update the static field for consistency
                  FileDecompiler.isK2Selected = isK2;
                  System.out.println("[INFO] loadNssFile: Compiling NSS with isK2: " + isK2 + " (from Settings: " + gameVariant + ")");

                  // nwscript.nss handling is now done by CompilerExecutionWrapper.prepareExecutionEnvironment()

                  // Use unified compiler execution wrapper - abstracts ALL compiler quirks
                  CompilerExecutionWrapper wrapper = new CompilerExecutionWrapper(compiler, file, compiledNcs, isK2);

                  // Include the source file's parent directory for relative #include resolution
                  java.util.List < File > includeDirs = new java.util.ArrayList < > ();
                  includeDirs.add(file.getParentFile());

                  // Prepare execution environment (handles include files, nwscript.nss, etc.)
                  wrapper.prepareExecutionEnvironment(includeDirs);

                  // Declare variables outside try block so they're accessible in finally
                  StringBuilder output = new StringBuilder();
                  boolean finished = false;
                  Process proc = null;

                  // Try compilation first without registry spoofing
                  boolean needsRegistrySpoof = false;
                  output.setLength(0);

                  try {
                     // First attempt: compile without registry spoofing
                     String[] cmd = wrapper.getCompileArgs(includeDirs);
                     System.out.println("[INFO] loadNssFile: Running compiler (first attempt, no registry spoofing): " + java.util.Arrays.toString(cmd));
                     ProcessBuilder pb = new ProcessBuilder(cmd);
                     pb.directory(wrapper.getWorkingDirectory());
                     pb.environment().putAll(wrapper.getEnvironmentOverrides());
                     pb.redirectErrorStream(true);
                     proc = pb.start();

                     // Read output
                     try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        }
                     }

                     finished = proc.waitFor(25, java.util.concurrent.TimeUnit.SECONDS);
                     if (!finished) {
                        proc.destroyForcibly();
                        System.out.println("[INFO] loadNssFile: Compiler timed out");
                     } else {
                        int exitCode = proc.exitValue();
                        System.out.println("[INFO] loadNssFile: Compiler exit code: " + exitCode);
                        String compilerOutput = output.toString();
                        if (!compilerOutput.trim().isEmpty()) {
                        System.out.println("[INFO] loadNssFile: Compiler output:\n" + compilerOutput.trim());
                        }

                        // Check if we need registry spoofing (look for NwnStdLoader error)
                        if (!compiledNcs.exists() && compilerOutput.contains("Error: Couldn't initialize the NwnStdLoader")) {
                        System.out.println("[INFO] loadNssFile: Detected NwnStdLoader error - registry spoofing required");
                        needsRegistrySpoof = true;
                        }
                     }

                     // If registry spoofing is needed, retry with spoofer activated
                     if (needsRegistrySpoof) {
                        try (AutoCloseable spoofer = wrapper.createRegistrySpoofer()) {
                        if (spoofer instanceof RegistrySpoofer) {
                           try {
                              ((RegistrySpoofer) spoofer).activate();
                              System.out.println("[INFO] loadNssFile: Registry spoofing activated, retrying compilation");
                           } catch (SecurityException e) {
                              System.out.println("[INFO] loadNssFile: Registry spoofing failed (permission denied): " + e.getMessage());
                              System.out.println("[INFO] loadNssFile: Continuing without registry spoofing");
                           }
                        }

                        // Retry compilation with registry spoofing
                        output.setLength(0);
                        String[] cmd2 = wrapper.getCompileArgs(includeDirs);
                        System.out.println("[INFO] loadNssFile: Running compiler (retry with registry spoofing): " + java.util.Arrays.toString(cmd2));
                        ProcessBuilder pb2 = new ProcessBuilder(cmd2);
                        pb2.directory(wrapper.getWorkingDirectory());
                        pb2.environment().putAll(wrapper.getEnvironmentOverrides());
                        pb2.redirectErrorStream(true);
                        proc = pb2.start();

                        // Read output
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                           new java.io.InputStreamReader(proc.getInputStream()))) {
                           String line;
                           while ((line = reader.readLine()) != null) {
                              output.append(line).append("\n");
                           }
                        }

                        finished = proc.waitFor(25, java.util.concurrent.TimeUnit.SECONDS);
                        if (!finished) {
                           proc.destroyForcibly();
                           System.out.println("[INFO] loadNssFile: Compiler timed out (retry)");
                        } else {
                           int exitCode = proc.exitValue();
                           System.out.println("[INFO] loadNssFile: Compiler exit code (retry): " + exitCode);
                           String compilerOutput = output.toString().trim();
                           if (!compilerOutput.isEmpty()) {
                              System.out.println("[INFO] loadNssFile: Compiler output (retry):\n" + compilerOutput);
                           }
                        }
                        } catch (Exception spooferEx) {
                        System.out.println("[INFO] loadNssFile: Exception with registry spoofer: " + spooferEx.getMessage());
                        // Continue - we've already tried once without spoofing
                        }
                     }

                     System.out.println("[INFO] loadNssFile: Checking for compiled NCS at: " + compiledNcs.getAbsolutePath());
                     if (compiledNcs.exists()) {
                        System.err.println(
                        "DEBUG loadNssFile: Compiled NCS exists at: " + compiledNcs.getAbsolutePath());

                        // Capture bytecode from the first compiled NCS (NSS->NCS) as "original bytecode" (left panel)
                        boolean firstBytecodeCaptured = this.fileDecompiler.captureBytecodeForNssFile(file, compiledNcs, isK2, true);
                        if (firstBytecodeCaptured) {
                        System.out.println("[INFO] loadNssFile: Successfully captured first bytecode from compiled NCS (left panel)");
                        } else {
                        System.out.println("[INFO] loadNssFile: Warning - Could not capture first bytecode (left panel will show placeholder)");
                        }

                        // Decompile the compiled NCS to get round-trip NSS
                        String gameFlag = isK2 ? "k2" : "k1";
                        String roundTripCode = RoundTripUtil.decompileNcsToNss(compiledNcs, gameFlag);
                        System.out.println("[INFO] loadNssFile: Round-trip code result: " +
                        (roundTripCode != null ? "not null, length=" + roundTripCode.length() : "null"));

                        if (roundTripCode != null && !roundTripCode.trim().isEmpty()) {
                        System.out.println("[INFO] loadNssFile: Setting round-trip code in right panel");
                        NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, true);
                        roundTripPane.setText(roundTripCode);
                        NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, false);
                        NWScriptSyntaxHighlighter.applyHighlightingImmediate(roundTripPane);

                        // Now compile the round-trip NSS to get second NCS, then capture its bytecode (right panel)
                        try {
                           // Write round-trip NSS to temp file
                           File tempDir = new File(System.getProperty("java.io.tmpdir"), "ncsdecomp_roundtrip");
                           if (!tempDir.exists()) {
                              System.out.println("[INFO] Decompiler: CREATING directory: " + tempDir.getAbsolutePath());
                              if (!tempDir.mkdirs()) {
                                 System.err.println("[ERROR] Decompiler: Failed to create temp directory: " + tempDir.getAbsolutePath());
                                 // Skip rest of round-trip processing if directory creation fails
                                 throw new IOException("Failed to create temp directory: " + tempDir.getAbsolutePath());
                              }
                              System.out.println("[INFO] Decompiler: Created directory: " + tempDir.getAbsolutePath());
                           }
                           String baseName = file.getName();
                           if (baseName.endsWith(".nss")) {
                              baseName = baseName.substring(0, baseName.length() - 4);
                           }
                           File roundTripNssFile = new File(tempDir, baseName + "_roundtrip.nss");
                           // Ensure parent directory exists
                           File parentDir = roundTripNssFile.getParentFile();
                           if (parentDir != null && !parentDir.exists()) {
                              if (!parentDir.mkdirs()) {
                                 System.err.println("[ERROR] Decompiler: Failed to create parent directory: " + parentDir.getAbsolutePath());
                                 // Skip rest of round-trip processing if directory creation fails
                                 throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
                              }
                           }
                           System.out.println("[INFO] Decompiler: WRITING round-trip NSS file: " + roundTripNssFile.getAbsolutePath() + " (length: " + roundTripCode.getBytes().length + " bytes)");
                           java.nio.file.Files.write(roundTripNssFile.toPath(), roundTripCode.getBytes());
                           System.out.println("[INFO] Decompiler: Wrote round-trip NSS file: " + roundTripNssFile.getAbsolutePath());

                           // Compile the round-trip NSS to NCS
                           File secondCompiledNcs = this.fileDecompiler.compileNssToNcs(roundTripNssFile, tempDir);
                           if (secondCompiledNcs != null && secondCompiledNcs.exists()) {
                              // Capture bytecode from second compiled NCS (NSS->NCS->NSS->NCS) as "new bytecode" (right panel)
                              boolean secondBytecodeCaptured = this.fileDecompiler.captureBytecodeForNssFile(file, secondCompiledNcs, isK2, false);
                              if (secondBytecodeCaptured) {
                              System.out.println("[INFO] loadNssFile: Successfully captured second bytecode from round-trip compiled NCS (right panel)");
                              } else {
                              System.out.println("[INFO] loadNssFile: Warning - Could not capture second bytecode (right panel will show placeholder)");
                              }
                           } else {
                              System.out.println("[INFO] loadNssFile: Round-trip NSS compilation failed, cannot capture second bytecode");
                           }
                        } catch (Exception secondBytecodeEx) {
                           System.out.println("[INFO] loadNssFile: Error capturing second bytecode: " + secondBytecodeEx.getMessage());
                           // Continue - right panel will show placeholder
                        }
                        } else {
                        System.out.println("[INFO] loadNssFile: Round-trip code is null or empty");
                        roundTripPane.setText(
                           "// Round-trip decompiled code not available.\n// The compiled NCS could not be decompiled.");
                        }
                     } else {
                        System.out.println("[INFO] loadNssFile: Compiled NCS does not exist after compilation");
                        // Show actual compiler error output instead of generic message
                        StringBuilder errorMsg = new StringBuilder();
                        errorMsg.append("// Round-trip decompiled code not available.\n");
                        errorMsg.append("// Compilation failed.\n\n");
                        String compilerOutput = output.toString().trim();
                        if (!compilerOutput.isEmpty()) {
                        errorMsg.append("// Compiler output:\n");
                        // Format compiler output as comments
                        String[] lines = compilerOutput.split("\n");
                        for (String line: lines) {
                           if (!line.trim().isEmpty()) {
                              errorMsg.append("// ").append(line).append("\n");
                           }
                        }
                        } else {
                        errorMsg.append("// No compiler output available.\n");
                        if (proc != null && finished) {
                           errorMsg.append("// Exit code: ").append(proc.exitValue()).append("\n");
                        } else {
                           errorMsg.append("// Compiler timed out or process not started.\n");
                        }
                        }
                        roundTripPane.setText(errorMsg.toString());
                     }
                  } catch (Exception compileEx) {
                     System.out.println("[INFO] loadNssFile: Exception during compilation: " + compileEx.getMessage());
                     compileEx.printStackTrace();
                     // Don't throw - let finally block execute cleanup
                  } finally {
                     // Clean up all temporary files (include files, nwscript.nss, etc.)
                     wrapper.cleanup();
                  }
                  } else {
                  System.out.println("[INFO] loadNssFile: Compiler not found");
                  roundTripPane.setText(
                     "// Round-trip decompiled code not available.\n// Compiler (nwnnsscomp.exe) not found.");
                  }
               } catch (Exception e) {
                  System.out.println("[INFO] loadNssFile: Error during round-trip: " + e.getMessage());
                  e.printStackTrace();
                  roundTripPane
                  .setText("// Round-trip decompiled code not available.\n// Error: " + e.getMessage());
               }
            }
            }
         } catch (Exception e) {
            System.err.println("DEBUG loadNssFile: Error populating round-trip panel: " + e.getMessage());
            e.printStackTrace();
         }
      }
      }

      // For NSS files, we don't have variable data from decompilation
      // Create an empty tree model
      this.hash_Func2VarVec = new Hashtable<>();
      this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));

      // Map the file to the tab component
      JComponent tabComponent = this.getSelectedTabComponent();
      if (tabComponent != null) {
         // Store file as absolute path to ensure correct directory resolution in save
         // dialog
         this.hash_TabComponent2File.put(tabComponent, file != null ? file.getAbsoluteFile() : null);
         this.hash_TabComponent2Func2VarVec.put(tabComponent, this.hash_Func2VarVec);
         this.hash_TabComponent2TreeModel.put(tabComponent, this.jTree.getModel());
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel) tabComponent, false);
         }
      }

      // NOW remove file from filesBeingLoaded - AFTER all setup is complete
      // This prevents the file from being marked as dirty during initial load
      filesBeingLoaded.remove(file);

      this.appendStatus("[INFO] loaded successfully\n");

      // Update workspace visibility after adding a file
      this.updateWorkspaceCard();

      // NSS files: default to "View Decompiled Code" (source view)
      this.setTabComponentPanel(0);
   }

   private void decompile(File file) {
      this.appendStatus("[INFO] Decompiling...\n");
      this.statusScrollPane.getVerticalScrollBar().setValue(this.statusScrollPane.getVerticalScrollBar().getMaximum());
      this.appendStatus("[INFO] " + file.getName() + ": ");
      int result = 2;
      String generatedCode = null;
      Hashtable<String, Vector<Variable>> vars = null;

      try {
         result = this.fileDecompiler.decompile(file);
         // Always try to get generated code, even if result indicates failure
         generatedCode = this.fileDecompiler.getGeneratedCode(file);
         vars = this.fileDecompiler.getVariableData(file);
      } catch (Exception unexpected) {
         // Catch any other unexpected exceptions
         this.appendStatus("[ERROR] unexpected error: " + unexpected.getMessage() + "\n");
         generatedCode = "// Unexpected Decompilation Error\n" + "// File: " + file.getName() + "\n" + "// Error: "
               + unexpected.getMessage() + "\n" + "void main() {\n    // Unexpected error occurred\n}\n";
         result = 2; // PARTIAL_COMPILE - we're showing something
      }

      // ALWAYS show source code - we guarantee generatedCode is never null/empty at
      // this point
      // If decompilation completely failed, we created a fallback stub above
      if (generatedCode == null || generatedCode.trim().isEmpty()) {
         // Ultimate fallback - should never happen, but ensure we always show something
         generatedCode = "// No code available\n" + "// File: " + file.getName() + "\n"
               + "void main() {\n    // No decompiled code\n}\n";
         result = 2;
      }

      // Now we're guaranteed to have code - always show it
      // CRITICAL: Store generatedCode in final variable to prevent any overwrites
      final String finalGeneratedCode = generatedCode;
      {
         // Create tab with original filename (keep .ncs extension for NCS files)
         String fileName = file.getName();
         // Mark file as being loaded to prevent marking as dirty during initial setText
         filesBeingLoaded.add(file);
         this.panels = this.newNCSTab(fileName);

         // Get the left side text pane from the split pane
         // CRITICAL: Make final so it can be used in lambda expressions for error
         // recovery
         final JTextComponent[] leftCodeAreaRef = new JTextComponent[1];
         if (this.panels[0] instanceof JSplitPane) {
            JSplitPane decompSplitPane = (JSplitPane) this.panels[0];
            java.awt.Component leftComp = decompSplitPane.getLeftComponent();
            if (leftComp instanceof JScrollPane) {
               JScrollPane leftScrollPane = (JScrollPane) leftComp;
               leftCodeAreaRef[0] = (JTextComponent) leftScrollPane.getViewport().getView();

               // Disable highlighting and dirty marking during setText to prevent freeze and false dirty flags
               if (leftCodeAreaRef[0] instanceof JTextPane) {
                  NWScriptSyntaxHighlighter.setSkipHighlighting((JTextPane) leftCodeAreaRef[0], true);
                  leftCodeAreaRef[0].putClientProperty("Decompiler.programmaticUpdate", true);
               }

               // CRITICAL: Set decompiled code ONCE - this should NEVER be overwritten
               leftCodeAreaRef[0].setText(finalGeneratedCode);

               // Re-enable highlighting and dirty marking, then apply highlighting immediately
               if (leftCodeAreaRef[0] instanceof JTextPane) {
                  JTextPane textPane = (JTextPane) leftCodeAreaRef[0];
                  NWScriptSyntaxHighlighter.setSkipHighlighting(textPane, false);
                  textPane.putClientProperty("Decompiler.programmaticUpdate", false);
                  NWScriptSyntaxHighlighter.applyHighlightingImmediate(textPane);
               }

               // NOTE: Do NOT remove from filesBeingLoaded yet - keep file protected until
               // fully loaded
            }
         }

         // Try to get bytecode if available
         try {
            String origByteCode = this.fileDecompiler.getOriginalByteCode(file);
            if (origByteCode != null && !origByteCode.trim().isEmpty()) {
               this.origByteCodeJTA = (JTextPane) ((JScrollPane) ((JSplitPane) this.panels[1]).getLeftComponent())
                     .getViewport().getView();
               if (this.origByteCodeJTA != null) {
                  this.origByteCodeJTA.setText(origByteCode);
                  // Apply highlighting after text is set
                  SwingUtilities.invokeLater(() -> BytecodeSyntaxHighlighter.applyHighlighting(this.origByteCodeJTA));
               }
            }
            String newByteCode = this.fileDecompiler.getNewByteCode(file);
            if (newByteCode != null && !newByteCode.trim().isEmpty()) {
               this.newByteCodeJTA = (JTextPane) ((JScrollPane) ((JSplitPane) this.panels[1]).getRightComponent())
                     .getViewport().getView();
               if (this.newByteCodeJTA != null) {
                  this.newByteCodeJTA.setText(newByteCode);
                  // Apply highlighting after text is set
                  SwingUtilities.invokeLater(() -> BytecodeSyntaxHighlighter.applyHighlighting(this.newByteCodeJTA));
               }
               if (this.origByteCodeJTA != null && this.origByteCodeJTA.getDocument().getLength() >= this.newByteCodeJTA
                     .getDocument().getLength()) {
                  this.jTB.putClientProperty(this.panels[1], "left");
               } else {
                  this.jTB.putClientProperty(this.panels[1], "right");
               }
            }
         } catch (Exception e) {
            // Bytecode not available, that's okay
            System.out.println("Bytecode comparison not available: " + e.getMessage());
         }

         // Populate round-trip decompiled code panel (NCS -> NSS -> NCS -> NSS)
         // Auto-trigger round-trip on load by saving to temp directory
         // CRITICAL: All round-trip code is wrapped in try-catch to ensure it NEVER
         // affects the left pane
         try {
            // Get the decomp split pane from panels[0]
            if (this.panels[0] instanceof JSplitPane) {
               JSplitPane decompSplitPane = (JSplitPane) this.panels[0];
               java.awt.Component rightComp = decompSplitPane.getRightComponent();
               if (rightComp instanceof JScrollPane) {
                  JScrollPane rightScrollPane = (JScrollPane) rightComp;
                  if (rightScrollPane.getViewport().getView() instanceof JTextPane) {
                     JTextPane roundTripPane = (JTextPane) rightScrollPane.getViewport().getView();

                     // Auto-trigger round-trip validation on load using temp directory
                     System.out.println("[INFO] decompile: Auto-triggering round-trip validation on load");

                     try {
                        // Create temp directory for round-trip
                        File tempDir = new File(System.getProperty("java.io.tmpdir"), "ncsdecomp_roundtrip");
                        if (!tempDir.exists()) {
                           System.out.println("[INFO] Decompiler: CREATING directory: " + tempDir.getAbsolutePath());
                           if (!tempDir.mkdirs()) {
                              System.err.println("[ERROR] Decompiler: Failed to create temp directory: " + tempDir.getAbsolutePath());
                              // Skip rest of round-trip processing if directory creation fails
                              throw new IOException("Failed to create temp directory: " + tempDir.getAbsolutePath());
                           }
                           System.out.println("[INFO] Decompiler: Created directory: " + tempDir.getAbsolutePath());
                        }

                        // Save decompiled code to temp file
                        // CRITICAL: Use finalGeneratedCode to ensure we use the original decompiled
                        // code
                        String baseName2 = file.getName().substring(0, file.getName().length() - 4);
                        File tempNssFile = new File(tempDir, baseName2 + ".nss");

                        // Ensure parent directory exists
                        File parentDir = tempNssFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                           if (!parentDir.mkdirs()) {
                              System.err.println("[ERROR] Decompiler: Failed to create parent directory: " + parentDir.getAbsolutePath());
                              // Skip rest of round-trip processing if directory creation fails
                              throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
                           }
                        }

                        // Write the generated code to temp file
                        byte[] codeBytes = finalGeneratedCode.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        System.out.println("[INFO] Decompiler: WRITING temp NSS file: " + tempNssFile.getAbsolutePath() + " (length: " + codeBytes.length + " bytes)");
                        java.nio.file.Files.write(tempNssFile.toPath(), codeBytes);
                        System.out.println("[INFO] decompile: Saved temp NSS to: " + tempNssFile.getAbsolutePath());

                        // Compile the temp NSS file directly (without cleanup) for round-trip display
                        // Use tempDir explicitly to ensure output is in temp
                        File recompiledNcs = this.fileDecompiler.compileNssToNcs(tempNssFile, tempDir);
                        System.out.println("[INFO] decompile: Compilation result: "
                              + (recompiledNcs != null ? recompiledNcs.getAbsolutePath() : "null"));
                        System.out.println("[INFO] decompile: Recompiled NCS exists: "
                              + (recompiledNcs != null && recompiledNcs.exists()));

                        if (recompiledNcs != null && recompiledNcs.exists()) {
                           // Capture bytecode from recompiled NCS (NCS->NSS->NCS) as "new bytecode" (right panel)
                           boolean newBytecodeCaptured = this.fileDecompiler.captureBytecodeFromNcs(file, recompiledNcs, FileDecompiler.isK2Selected, false);
                           if (newBytecodeCaptured) {
                              System.out.println("[INFO] decompile: Successfully captured new bytecode from recompiled NCS (right panel)");
                           } else {
                              System.out.println("[INFO] decompile: Warning - Could not capture new bytecode (right panel will show placeholder)");
                           }

                           // Decompile the recompiled NCS to show round-trip result
                           String gameFlag = FileDecompiler.isK2Selected ? "k2" : "k1";
                           System.out.println("[INFO] decompile: Decompiling recompiled NCS with gameFlag: " + gameFlag);
                           String roundTripCode = RoundTripUtil.decompileNcsToNss(recompiledNcs, gameFlag);
                           System.out.println("[INFO] decompile: Round-trip code result: "
                                 + (roundTripCode != null ? "not null, length=" + roundTripCode.length() : "null"));

                           if (roundTripCode != null && !roundTripCode.trim().isEmpty()) {
                              System.out.println("[INFO] decompile: Setting round-trip code in right panel");
                              NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, true);
                              roundTripPane.setText(roundTripCode);
                              NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, false);
                              NWScriptSyntaxHighlighter.applyHighlightingImmediate(roundTripPane);
                           } else {
                              System.err
                                    .println("DEBUG decompile: Round-trip code is null or empty, showing placeholder");
                              roundTripPane.setText(
                                    "// Round-trip decompiled code not available.\n// The recompiled NCS could not be decompiled.");
                           }
                        } else {
                           String errorPath = recompiledNcs != null ? recompiledNcs.getAbsolutePath() : "null";
                           System.err.println(
                                 "DEBUG decompile: Recompiled NCS not found after auto-trigger at: " + errorPath);
                           roundTripPane.setText(
                                 "// Round-trip decompiled code not available.\n// Compilation failed or compiler not configured.");
                        }

                        // Clean up temp files after use (they're only needed for round-trip validation)
                        try {
                           if (recompiledNcs.exists()) {
                              // Keep the NCS temporarily in case user wants to inspect it
                              // Will be cleaned up on next load or can be manually deleted
                              // recompiledNcs.delete();
                           }
                           // Keep temp NSS for now in case of errors, can be cleaned up later
                           // tempNssFile.delete();
                        } catch (Exception cleanupEx) {
                           // Ignore cleanup errors
                        }
                     } catch (Exception ex) {
                        System.out.println("[INFO] decompile: Error during auto-trigger round-trip: " + ex.getMessage());
                        ex.printStackTrace();
                        // CRITICAL: Only set error message in RIGHT pane (round-trip panel), NEVER
                        // touch left pane
                        // Build helpful error message for elevation errors
                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.contains("error=740")) {
                           errorMsg = "Compiler requires administrator privileges.\n"
                                 + "Please run NCSDecomp as administrator.";
                        }
                        roundTripPane.setText("// Round-trip validation error: " + errorMsg
                              + "\n// Check that nwnnsscomp.exe is configured in Settings.");
                        // CRITICAL: Left pane is NEVER touched by nwnnsscomp errors - it's completely
                        // independent
                        // The left pane already has the decompiled code set above and should never be
                        // modified here
                     }
                  }
               }
            }
         } catch (Exception e) {
            System.err.println("DEBUG decompile: Error populating round-trip panel: " + e.getMessage());
            e.printStackTrace();
            // CRITICAL: Left pane is NEVER touched by nwnnsscomp errors - it's completely
            // independent
            // The left pane already has the decompiled code set above and should never be
            // modified here
            // Only set error in right pane if we can access it
            try {
               if (this.panels[0] instanceof JSplitPane) {
                  JSplitPane decompSplitPane = (JSplitPane) this.panels[0];
                  java.awt.Component rightComp = decompSplitPane.getRightComponent();
                  if (rightComp instanceof JScrollPane) {
                     JScrollPane rightScrollPane = (JScrollPane) rightComp;
                     if (rightScrollPane.getViewport().getView() instanceof JTextPane) {
                        JTextPane roundTripPane = (JTextPane) rightScrollPane.getViewport().getView();
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("error=740")) {
                           errorMsg = "Compiler requires administrator privileges.";
                        }
                        roundTripPane.setText("// Round-trip validation error: " + errorMsg
                              + "\n// Left side decompiled code is still available and independent of nwnnsscomp.");
                     }
                  }
               }
            } catch (Exception ignored) {
               // Ignore errors in error handling
            }
         }

         if (vars != null) {
            this.hash_Func2VarVec = vars;
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
         }
         JComponent tabComponent = this.getSelectedTabComponent();
         if (tabComponent != null) {
            // Store file as absolute path to ensure correct directory resolution in save
            // dialog
            this.hash_TabComponent2File.put(tabComponent, file.getAbsoluteFile());
            this.hash_TabComponent2Func2VarVec.put(tabComponent, this.hash_Func2VarVec);
            this.hash_TabComponent2TreeModel.put(tabComponent, this.jTree.getModel());
            if (tabComponent instanceof JPanel) {
               this.updateTabLabel((JPanel) tabComponent, false);
            }
         }

         // NOW remove file from filesBeingLoaded - AFTER all setup is complete
         // This prevents the file from being marked as dirty during initial load
         filesBeingLoaded.remove(file);
      }

      // Update status based on result - code is already shown above
      switch (result) {
      case 0:
         this.appendStatus("[WARNING] failure - validation failed\n");
         break;
      case 1:
         this.appendStatus("[INFO] success - full round-trip validation passed\n");
         break;
      case 2:
         this.appendStatus("[INFO] partial - decompiled successfully (nwnnsscomp validation skipped or failed)\n");
         break;
      case 3:
         this.appendStatus("[WARNING] partial - decompiled but bytecode comparison showed differences\n");
         break;
      default:
         this.appendStatus("[WARNING] unknown result code: " + result + "\n");
         break;
      }

      // Update workspace visibility after adding a file
      this.updateWorkspaceCard();

      // NCS files: default to "View Byte Code" (bytecode view). DO NOT MODIFY THIS
      // LINE UNDER ANY CIRCUMSTANCES -- THIS IS CORRECT LOGIC
      this.setTabComponentPanel(1);
   }

   public static void exit() {
      System.exit(0);
   }

   public static String chooseOutputDirectory() {
      JFileChooser jFC = new JFileChooser(settings.getProperty("Output Directory"));
      jFC.setFileSelectionMode(1);
      switch (jFC.showDialog(null, "Select output directory")) {
      case 0:
         try {
            return jFC.getSelectedFile().getCanonicalPath();
         } catch (IOException var2) {
            var2.printStackTrace();
            return settings.getProperty("Output Directory").equals("") ? System.getProperty("user.dir")
                  : settings.getProperty("Output Directory");
         }
      default:
         return settings.getProperty("Output Directory").equals("") ? System.getProperty("user.dir")
               : settings.getProperty("Output Directory");
      }
   }

   private File saveBuffer(JTextComponent buffer, String canonicalPath) {
      try {
         // Get encoding from settings, default to Windows-1252 (standard for KotOR/TSL)
         String encodingName = settings.getProperty("Encoding", "Windows-1252");
         java.nio.charset.Charset charset;
         try {
            charset = java.nio.charset.Charset.forName(encodingName);
         } catch (Exception e) {
            // Fallback to Windows-1252 if encoding is invalid
            try {
               charset = java.nio.charset.Charset.forName("Windows-1252");
            } catch (Exception e2) {
               charset = java.nio.charset.StandardCharsets.UTF_8;
            }
         }

         // Ensure parent directory exists before writing
         File outputFile = new File(canonicalPath);
         File parentDir = outputFile.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
            System.out.println("[INFO] Decompiler: CREATING parent directory: " + parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
               throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }
            System.out.println("[INFO] Decompiler: Created parent directory: " + parentDir.getAbsolutePath());
         }
         System.out.println("[INFO] Decompiler: WRITING file: " + canonicalPath + " (encoding: " + charset.name() + ")");
         BufferedWriter bw = new BufferedWriter(
               new java.io.OutputStreamWriter(new java.io.FileOutputStream(canonicalPath), charset));
         String text = buffer.getText();
         bw.write(text);
         bw.close();
         System.out.println("[INFO] Decompiler: Wrote file: " + canonicalPath + " (length: " + text.length() + " chars)");
         return new File(canonicalPath);
      } catch (FileNotFoundException var4) {
         File toDel = new File(canonicalPath);
         JOptionPane.showMessageDialog(null,
               "Error saving " + toDel.getName() + "\nOutput directory does not exist; change in settings");
         toDel.delete();
      } catch (IOException var5) {
         File toDelx = new File(canonicalPath);
         JOptionPane.showMessageDialog(null, "Error saving " + toDelx.getName());
         toDelx.delete();
      }

      return null;
   }

   /**
    * Builds output filename using settings for prefix, suffix, and extension.
    */
   private String buildOutputFilename(String baseName) {
      String prefix = settings.getProperty("Filename Prefix", "");
      String suffix = settings.getProperty("Filename Suffix", "");
      String extension = settings.getProperty("File Extension", ".nss");
      // Ensure extension starts with a dot
      if (!extension.startsWith(".")) {
         extension = "." + extension;
      }
      return prefix + baseName + suffix + extension;
   }

   private void setTabComponentPanel(int index) {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex < 0 || selectedIndex >= this.jTB.getTabCount()) {
         return; // No tab selected or invalid index
      }

      JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }

      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }

      JComponent[] panels = (JComponent[]) clientProperty;
      if (index < 0 || index >= panels.length || panels[index] == null) {
         return; // Invalid panel index
      }

      // If switching to decompiled code view (index 0), ensure left side has
      // decompiled code
      // This is CRITICAL: The left side decompiled code is independent of nwnnsscomp
      // and should ALWAYS be available
      if (index == 0 && panels[0] instanceof JSplitPane) {
         JSplitPane decompSplitPane = (JSplitPane) panels[0];
         java.awt.Component leftComp = decompSplitPane.getLeftComponent();
         java.awt.Component rightComp = decompSplitPane.getRightComponent();

         // Set link property for scrollbar synchronization (always set, even if content loading fails)
         if (leftComp instanceof JScrollPane && rightComp instanceof JScrollPane) {
            JScrollPane leftScroll = (JScrollPane) leftComp;
            JScrollPane rightScroll = (JScrollPane) rightComp;
            JTextPane leftPane = (JTextPane) leftScroll.getViewport().getView();
            JTextPane rightPane = (JTextPane) rightScroll.getViewport().getView();
            if (leftPane != null && rightPane != null) {
               // Set link property based on which has more content
               if (leftPane.getDocument().getLength() >= rightPane.getDocument().getLength()) {
                  this.jTB.putClientProperty(panels[0], "left");
               } else {
                  this.jTB.putClientProperty(panels[0], "right");
               }
            } else {
               // Default to left if we can't determine
               this.jTB.putClientProperty(panels[0], "left");
            }
         } else {
            // Default to left if components aren't scroll panes
            this.jTB.putClientProperty(panels[0], "left");
         }

         try {
            File file = this.hash_TabComponent2File.get(tabComponent);
            if (file != null && leftComp instanceof JScrollPane) {
               JTextPane codePane = (JTextPane) ((JScrollPane) leftComp).getViewport().getView();
               if (codePane != null) {
                  // Get the decompiled code - this is independent of nwnnsscomp
                  String generatedCode = this.fileDecompiler.getGeneratedCode(file);
                  if (generatedCode != null && !generatedCode.trim().isEmpty()) {
                     // Only update if the pane is empty or contains error messages
                     String currentText = codePane.getText();
                     if (currentText == null || currentText.trim().isEmpty() || currentText.contains("// Round-trip")
                           || currentText.contains("// Error") || currentText.contains("// Unexpected")
                           || currentText.contains("// No code")) {
                        NWScriptSyntaxHighlighter.setSkipHighlighting(codePane, true);
                        codePane.putClientProperty("Decompiler.programmaticUpdate", true);
                        codePane.setText(generatedCode);
                        NWScriptSyntaxHighlighter.setSkipHighlighting(codePane, false);
                        codePane.putClientProperty("Decompiler.programmaticUpdate", false);
                        NWScriptSyntaxHighlighter.applyHighlightingImmediate(codePane);
                     }
                  }
               }

               // Update link property after content is loaded
               if (rightComp instanceof JScrollPane) {
                  JScrollPane leftScroll = (JScrollPane) leftComp;
                  JScrollPane rightScroll = (JScrollPane) rightComp;
                  JTextPane leftPane = (JTextPane) leftScroll.getViewport().getView();
                  JTextPane rightPane = (JTextPane) rightScroll.getViewport().getView();
                  if (leftPane != null && rightPane != null) {
                     // Update link property based on which has more content
                     if (leftPane.getDocument().getLength() >= rightPane.getDocument().getLength()) {
                        this.jTB.putClientProperty(panels[0], "left");
                     } else {
                        this.jTB.putClientProperty(panels[0], "right");
                     }
                  }
               }
            }
         } catch (Exception e) {
            System.out.println("Error populating decompiled code view: " + e.getMessage());
            e.printStackTrace();
         }
      }

      // If switching to bytecode view (index 1), populate bytecode data if available
      if (index == 1 && panels[1] instanceof JSplitPane) {
         try {
            File file = this.hash_TabComponent2File.get(tabComponent);
            if (file != null) {
               JSplitPane byteCodePane = (JSplitPane) panels[1];

               // Get left component (original bytecode)
               java.awt.Component leftComp = byteCodePane.getLeftComponent();
               if (leftComp instanceof JScrollPane) {
                  JTextPane origTextArea = (JTextPane) ((JScrollPane) leftComp).getViewport().getView();
                  if (origTextArea != null && origTextArea.getText().trim().isEmpty()) {
                     String origByteCode = this.fileDecompiler.getOriginalByteCode(file);
                     if (origByteCode != null && !origByteCode.trim().isEmpty()) {
                        origTextArea.setText(origByteCode);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(origTextArea);
                     } else {
                        origTextArea.setText(
                              "// Original bytecode not available.\n// Bytecode is only captured during round-trip validation (save/recompile).");
                     }
                  }
               }

               // Get right component (recompiled bytecode or error display)
               java.awt.Component rightComp = byteCodePane.getRightComponent();

               if (rightComp instanceof JScrollPane) {
                  JTextPane newTextArea = (JTextPane) ((JScrollPane) rightComp).getViewport().getView();
                  if (newTextArea != null && newTextArea.getText().trim().isEmpty()) {
                     String newByteCode = this.fileDecompiler.getNewByteCode(file);
                     if (newByteCode != null && !newByteCode.trim().isEmpty()) {
                        newTextArea.setText(newByteCode);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(newTextArea);
                        // Set focus based on which has more content
                        if (leftComp instanceof JScrollPane) {
                           JTextPane origTextArea = (JTextPane) ((JScrollPane) leftComp).getViewport().getView();
                           if (origTextArea != null
                                 && origTextArea.getDocument().getLength() >= newTextArea.getDocument().getLength()) {
                              this.jTB.putClientProperty(panels[1], "left");
                           } else {
                              this.jTB.putClientProperty(panels[1], "right");
                           }
                        }
                     } else {
                        newTextArea.setText(
                              "// Recompiled bytecode not available.\n// Save the file to trigger round-trip validation and bytecode capture.");
                     }
                  }
               }
            }
         } catch (Exception e) {
            // If there's an error, show a message but still switch to the view
            System.out.println("Error populating bytecode view: " + e.getMessage());
            e.printStackTrace();
            try {
               JSplitPane byteCodePane = (JSplitPane) panels[1];
               java.awt.Component leftComp = byteCodePane.getLeftComponent();
               if (leftComp instanceof JScrollPane) {
                  JTextPane origTextArea = (JTextPane) ((JScrollPane) leftComp).getViewport().getView();
                  if (origTextArea != null) {
                     origTextArea.setText("// Error loading bytecode: " + e.getMessage());
                  }
               }
            } catch (Exception ex) {
               // Ignore errors in error handling
            }
         }
      }

      this.jTB.setComponentAt(selectedIndex, panels[index]);
      this.repaint();
   }

   private void open() {
      JFileChooser jFC = new JFileChooser();
      jFC.setCurrentDirectory(
            settings.getProperty("Open Directory").equals("") ? new File(settings.getProperty("Output Directory"))
                  : new File(settings.getProperty("Open Directory")));
      jFC.setMultiSelectionEnabled(true);
      jFC.setFileFilter(new FileFilter() {
         @Override
         public boolean accept(File f) {
            if (f.isDirectory()) {
               return true;
            }
            String name = f.getName().toLowerCase();
            return name.endsWith(".ncs") || name.endsWith(".nss");
         }

         @Override
         public String getDescription() {
            return "NWScript Files (*.ncs, *.nss)";
         }
      });
      switch (jFC.showDialog(null, "Open")) {
      case 0:
         final File[] files = jFC.getSelectedFiles();
         settings.setProperty("Open Directory", files[0].getParent());
         Thread openThread = new Thread(() -> {
            Decompiler.this.open(files);
         });
         openThread.setDaemon(true);
         openThread.setName("FileOpenDialog-" + System.currentTimeMillis());
         openThread.start();
         break;
      case 1:
         break;
      default:
         JOptionPane.showMessageDialog(null, "Error opening file(s)");
      }
   }

   void open(File[] files) {
      for (int j = 0; j < files.length; j++) {
         File fileToOpen = files[j];
         String fileName = fileToOpen.getName().toLowerCase();

         // Ensure we use the absolute path of the dropped/opened file
         File absoluteFile = fileToOpen.getAbsoluteFile();
         if (!absoluteFile.exists()) {
            this.appendStatus("[WARNING] File does not exist: " + absoluteFile.getAbsolutePath() + "\n");
            continue;
         }

         // Update Open Directory setting to the file's parent directory
         File parentDir = absoluteFile.getParentFile();
         if (parentDir != null && parentDir.exists()) {
            settings.setProperty("Open Directory", parentDir.getAbsolutePath());
         }

         if (fileName.endsWith(".ncs")) {
            // Decompile NCS file
            this.decompile(absoluteFile);
         } else if (fileName.endsWith(".nss")) {
            // Load NSS file directly (it's already source code)
            this.loadNssFile(absoluteFile);
         }
      }
   }

   private void toggleLinkScrollBars() {
      if (Boolean.parseBoolean(settings.getProperty("Link Scroll Bars"))) {
         settings.setProperty("Link Scroll Bars", "false");
      } else {
         settings.setProperty("Link Scroll Bars", "true");
         java.awt.Component selectedComponent = this.jTB.getSelectedComponent();
         if (selectedComponent != null && selectedComponent instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) selectedComponent;
            java.awt.Component leftComp = splitPane.getLeftComponent();
            java.awt.Component rightComp = splitPane.getRightComponent();
            if (leftComp instanceof JScrollPane) {
               ((JScrollPane) leftComp).getVerticalScrollBar().setValue(0);
            }
            if (rightComp instanceof JScrollPane) {
               ((JScrollPane) rightComp).getVerticalScrollBar().setValue(0);
            }
         }
      }
   }

   private void close(int index) {
      this.panel = (JPanel) this.jTB.getTabComponentAt(index);

      try {
         this.file = this.hash_TabComponent2File.get(this.panel);
         if (unsavedFiles.contains(this.file)) {
            switch (JOptionPane.showConfirmDialog(null,
                  (this.temp = this.file.getName()).substring(0, this.temp.length() - 4)
                        + ".nss is unsaved.  Would you like to save it?")) {
            case 0:
               this.save(index);
               break;
            case 1:
               break;
            case 2:
            default:
               return;
            }
         }
      } catch (HeadlessException var3) {
         var3.printStackTrace();
      }

      this.jTB.putClientProperty(this.panel, null);
      this.fileDecompiler.closeFile(this.hash_TabComponent2File.get(this.panel));
      this.hash_TabComponent2File.remove(this.panel);
      this.hash_TabComponent2Func2VarVec.remove(this.panel);
      this.hash_TabComponent2TreeModel.remove(this.panel);
      this.jTB.remove(index);
      this.panel = null;
      this.updateWorkspaceCard();
   }

   private void closeAll() {
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         this.close(i);
      }
   }

   private void save(int index) {
      if (index < 0 || index >= this.jTB.getTabCount()) {
         return; // Invalid index
      }

      JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(index);
      if (tabComponent == null) {
         return; // Tab component is null
      }

      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }

      JComponent[] panels = (JComponent[]) clientProperty;
      if (panels.length == 0 || !(panels[0] instanceof JPanel)) {
         return; // Invalid panel structure
      }

      if (!(panels[0] instanceof JSplitPane)) {
         return; // Invalid component structure
      }
      JSplitPane decompSplitPane = (JSplitPane) panels[0];
      java.awt.Component leftComp = decompSplitPane.getLeftComponent();
      if (!(leftComp instanceof JScrollPane)) {
         return; // Invalid component structure
      }

      JScrollPane scrollPane = (JScrollPane) leftComp;
      if (!(scrollPane.getViewport().getView() instanceof JTextComponent)) {
         return; // Invalid view type
      }

      JTextComponent textArea = (JTextComponent) scrollPane.getViewport().getView();
      if (!(tabComponent instanceof JPanel)) {
         return; // Tab component is not a panel
      }

      JPanel tabPanel = (JPanel) tabComponent;
      if (tabPanel.getComponentCount() == 0 || !(tabPanel.getComponent(0) instanceof JLabel)) {
         return; // Invalid tab label structure
      }

      String fileName = ((JLabel) tabPanel.getComponent(0)).getText();
      // Remove unsaved marker if present
      if (fileName.endsWith(" *")) {
         fileName = fileName.substring(0, fileName.length() - 2);
      }

      // Remove any existing extension to avoid double extensions
      int lastDot = fileName.lastIndexOf('.');
      if (lastDot > 0) {
         fileName = fileName.substring(0, lastDot);
      }

      File originalFile = this.hash_TabComponent2File.get(tabComponent);
      // Ensure we use absolute path for the file
      this.file = originalFile != null ? originalFile.getAbsoluteFile() : null;

      // Show file chooser dialog starting in the NCS file's directory
      File newFile;
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save NSS File");

      // Set initial directory to NCS file's directory if available, otherwise use
      // output directory setting
      File initialDir = null;
      if (this.file != null && this.file.exists()) {
         File parentDir = this.file.getParentFile();
         if (parentDir != null && parentDir.exists()) {
            initialDir = parentDir.getAbsoluteFile();
         }
      }
      if (initialDir == null) {
         String outputDir = settings.getProperty("Output Directory");
         if (outputDir != null && !outputDir.isEmpty()) {
            File outputDirFile = new File(outputDir);
            if (outputDirFile.exists()) {
               initialDir = outputDirFile.getAbsoluteFile();
            }
         }
      }
      if (initialDir == null) {
         initialDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
      }
      fileChooser.setCurrentDirectory(initialDir);

      // Set default filename
      fileChooser.setSelectedFile(new File(initialDir, buildOutputFilename(fileName)));

      // Show save dialog
      int result = fileChooser.showSaveDialog(this);
      if (result != JFileChooser.APPROVE_OPTION) {
         // User cancelled
         return;
      }

      newFile = fileChooser.getSelectedFile();
      if (newFile == null) {
         return;
      }

      // Ensure file has .nss extension
      if (!newFile.getName().toLowerCase().endsWith(".nss")) {
         newFile = new File(newFile.getParentFile(), newFile.getName() + ".nss");
      }

      newFile = this.saveBuffer(textArea, newFile.getAbsolutePath());
      if (newFile == null) {
         // saveBuffer failed (e.g., directory doesn't exist)
         return;
      }

      // If file was null or didn't exist, update the mapping to point to the newly
      // saved file
      if (this.file == null || !this.file.exists()) {
         this.file = newFile.getAbsoluteFile();
         this.hash_TabComponent2File.put(tabComponent, this.file);
         // File was created from scratch - just mark as saved (no round-trip validation
         // needed)
         unsavedFiles.remove(this.file);
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel) tabComponent, false);
         }
         this.appendStatus("[INFO] Saved " + newFile.getName() + " to " + newFile.getParent() + "\n");
         return;
      }

      // Only do round-trip validation if the original file exists
      if (unsavedFiles.contains(this.file)) {
         this.appendStatus("[INFO] Recompiling..." + this.file.getName() + ": ");
         int result2 = 2;

         try {
            result2 = this.fileDecompiler.compileAndCompare(this.file, newFile);
         } catch (DecompilerException var5) {
            JOptionPane.showMessageDialog(null, var5.getMessage());
         }

         switch (result2) {
         case 0:
            this.appendStatus("[ERROR] failure\n");
            break;
         case 1:
            this.panels = (JComponent[]) this.jTB.getClientProperty((JComponent) this.jTB.getTabComponentAt(index));
            if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
               try {
                  String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                  String newByteCode = this.fileDecompiler.getNewByteCode(this.file);

                  JSplitPane byteCodePane = (JSplitPane) this.panels[1];
                  java.awt.Component leftComp1 = byteCodePane.getLeftComponent();
                  if (leftComp1 instanceof JScrollPane) {
                     this.origByteCodeJTA = (JTextPane) ((JScrollPane) leftComp1).getViewport().getView();
                     if (this.origByteCodeJTA != null) {
                        String code = origByteCode != null ? origByteCode : "// Original bytecode not available";
                        this.origByteCodeJTA.setText(code);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(this.origByteCodeJTA);
                     }
                  }

                  java.awt.Component rightComp1 = byteCodePane.getRightComponent();
                  if (rightComp1 instanceof JScrollPane) {
                     this.newByteCodeJTA = (JTextPane) ((JScrollPane) rightComp1).getViewport().getView();
                     if (this.newByteCodeJTA != null) {
                        String code = newByteCode != null ? newByteCode : "// Recompiled bytecode not available";
                        this.newByteCodeJTA.setText(code);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(this.newByteCodeJTA);
                     }
                  }

                  if (this.origByteCodeJTA != null && this.newByteCodeJTA != null) {
                     if (this.origByteCodeJTA.getDocument().getLength() >= this.newByteCodeJTA.getDocument()
                           .getLength()) {
                        this.jTB.putClientProperty(this.panels[1], "left");
                     } else {
                        this.jTB.putClientProperty(this.panels[1], "right");
                     }
                  }
               } catch (Exception e) {
                  System.out.println("Error updating bytecode view: " + e.getMessage());
                  e.printStackTrace();
               }
            }

            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(this.file);
            JComponent selectedTab = this.getSelectedTabComponent();
            if (selectedTab != null) {
               this.hash_TabComponent2File.put(selectedTab, this.file);
               this.hash_TabComponent2Func2VarVec.put(selectedTab, this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put(selectedTab, this.jTree.getModel());
            }
            this.appendStatus("[INFO] success\n");
            break;
         case 2:
            // Refresh bytecode views
            this.panels = (JComponent[]) this.jTB.getClientProperty((JComponent) this.jTB.getTabComponentAt(index));
            if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
               try {
                  String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                  JSplitPane byteCodePane = (JSplitPane) this.panels[1];
                  java.awt.Component leftComp2 = byteCodePane.getLeftComponent();
                  if (leftComp2 instanceof JScrollPane) {
                     this.origByteCodeJTA = (JTextPane) ((JScrollPane) leftComp2).getViewport().getView();
                     if (this.origByteCodeJTA != null) {
                        String code = origByteCode != null ? origByteCode : "// Original bytecode not available";
                        this.origByteCodeJTA.setText(code);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(this.origByteCodeJTA);
                     }
                  }
                  this.jTB.putClientProperty(this.panels[1], "left");
               } catch (Exception e) {
                  System.out.println("Error updating bytecode view: " + e.getMessage());
               }
            }

            // Refresh decompiled code view with newly generated code and round-trip code
            this.refreshDecompiledCodeView(index, newFile);

            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(this.file);
            JComponent selectedTab2 = this.getSelectedTabComponent();
            if (selectedTab2 != null) {
               this.hash_TabComponent2File.put(selectedTab2, this.file);
               this.hash_TabComponent2Func2VarVec.put(selectedTab2, this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put(selectedTab2, this.jTree.getModel());
            }

            // Mark as saved
            unsavedFiles.remove(this.file);
            if (tabComponent instanceof JPanel) {
               this.updateTabLabel((JPanel) tabComponent, false);
            }

            this.setTabComponentPanel(1);
            this.appendStatus("[WARNING] partial-could not recompile\n");
            break;
         case 3:
            // Refresh bytecode views
            this.panels = (JComponent[]) this.jTB.getClientProperty((JComponent) this.jTB.getTabComponentAt(index));
            if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
               try {
                  String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                  String newByteCode = this.fileDecompiler.getNewByteCode(this.file);

                  JSplitPane byteCodePane = (JSplitPane) this.panels[1];
                  java.awt.Component leftComp3 = byteCodePane.getLeftComponent();
                  if (leftComp3 instanceof JScrollPane) {
                     this.origByteCodeJTA = (JTextPane) ((JScrollPane) leftComp3).getViewport().getView();
                     if (this.origByteCodeJTA != null) {
                        String code = origByteCode != null ? origByteCode : "// Original bytecode not available";
                        this.origByteCodeJTA.setText(code);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(this.origByteCodeJTA);
                     }
                  }

                  java.awt.Component rightComp3 = byteCodePane.getRightComponent();
                  if (rightComp3 instanceof JScrollPane) {
                     this.newByteCodeJTA = (JTextPane) ((JScrollPane) rightComp3).getViewport().getView();
                     if (this.newByteCodeJTA != null) {
                        String code = newByteCode != null ? newByteCode : "// Recompiled bytecode not available";
                        this.newByteCodeJTA.setText(code);
                        BytecodeSyntaxHighlighter.applyHighlightingImmediate(this.newByteCodeJTA);
                     }
                  }

                  if (this.origByteCodeJTA != null && this.newByteCodeJTA != null) {
                     if (this.origByteCodeJTA.getDocument().getLength() >= this.newByteCodeJTA.getDocument()
                           .getLength()) {
                        this.jTB.putClientProperty(this.panels[1], "left");
                     } else {
                        this.jTB.putClientProperty(this.panels[1], "right");
                     }
                  }
               } catch (Exception e) {
                  System.out.println("Error updating bytecode view: " + e.getMessage());
               }
            }

            // Refresh decompiled code view with newly generated code and round-trip code
            this.refreshDecompiledCodeView(index, newFile);

            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(this.file);
            JComponent selectedTab3 = this.getSelectedTabComponent();
            if (selectedTab3 != null) {
               this.hash_TabComponent2File.put(selectedTab3, this.file);
               this.hash_TabComponent2Func2VarVec.put(selectedTab3, this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put(selectedTab3, this.jTree.getModel());
            }

            // Mark as saved
            unsavedFiles.remove(this.file);
            if (tabComponent instanceof JPanel) {
               this.updateTabLabel((JPanel) tabComponent, false);
            }

            this.setTabComponentPanel(1);
            this.appendStatus("[WARNING] partial-byte code does not match\n");
         }

         // Mark as saved (already done in case statements above, but ensure it's done
         // here too)
         unsavedFiles.remove(this.file);
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel) tabComponent, false);
         }
         newFile = null;
      }
   }

   /**
    * Refreshes the decompiled code view with the newly generated code after
    * save/recompilation. Also populates the round-trip decompiled code on the
    * right side.
    *
    * @param index        The tab index to refresh
    * @param savedNssFile The saved NSS file (used to find the recompiled NCS for
    *                     round-trip decompilation)
    */
   private void refreshDecompiledCodeView(int index, File savedNssFile) {
      try {
         JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(index);
         if (tabComponent == null) {
            return;
         }

         File file = this.hash_TabComponent2File.get(tabComponent);
         if (file == null) {
            return;
         }

         Object clientProperty = this.jTB.getClientProperty(tabComponent);
         if (!(clientProperty instanceof JComponent[])) {
            return;
         }

         JComponent[] panels = (JComponent[]) clientProperty;
         if (panels.length == 0 || !(panels[0] instanceof JSplitPane)) {
            return;
         }

         JSplitPane decompSplitPane = (JSplitPane) panels[0];

         // Get left side (original decompiled code)
         java.awt.Component leftComp = decompSplitPane.getLeftComponent();
         if (!(leftComp instanceof JScrollPane)) {
            return;
         }
         JScrollPane leftScrollPane = (JScrollPane) leftComp;
         if (!(leftScrollPane.getViewport().getView() instanceof JTextPane)) {
            return;
         }
         JTextPane codePane = (JTextPane) leftScrollPane.getViewport().getView();

         // Get the newly generated code
         String generatedCode = this.fileDecompiler.getGeneratedCode(file);
         if (generatedCode != null) {
            // Temporarily disable highlighting and dirty marking during setText
            NWScriptSyntaxHighlighter.setSkipHighlighting(codePane, true);
            codePane.putClientProperty("Decompiler.programmaticUpdate", true);

            // Save caret position
            int caretPos = codePane.getCaretPosition();

            // Update the code
            codePane.setText(generatedCode);

            // Restore caret position (clamped to document length)
            int newLength = codePane.getDocument().getLength();
            codePane.setCaretPosition(Math.min(caretPos, newLength));

            // Re-enable highlighting and dirty marking, then apply highlighting
            NWScriptSyntaxHighlighter.setSkipHighlighting(codePane, false);
            codePane.putClientProperty("Decompiler.programmaticUpdate", false);
            NWScriptSyntaxHighlighter.applyHighlightingImmediate(codePane);

            // Reset undo manager since we're loading new content
            UndoManager undoManager = (UndoManager) codePane.getClientProperty("undoManager");
            if (undoManager != null) {
               undoManager.discardAllEdits();
            }
         }

         // Get right side (round-trip decompiled code)
         java.awt.Component rightComp = decompSplitPane.getRightComponent();
         if (rightComp instanceof JScrollPane) {
            JScrollPane rightScrollPane = (JScrollPane) rightComp;
            if (rightScrollPane.getViewport().getView() instanceof JTextPane) {
               JTextPane roundTripPane = (JTextPane) rightScrollPane.getViewport().getView();

               // Get round-trip decompiled code using shared utility (same logic as test)
               String roundTripCode = null;
               if (savedNssFile != null) {
                  roundTripCode = getRoundTripDecompiledCode(savedNssFile);
               }
               if (roundTripCode != null) {
                  NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, true);
                  roundTripPane.setText(roundTripCode);
                  NWScriptSyntaxHighlighter.setSkipHighlighting(roundTripPane, false);
                  NWScriptSyntaxHighlighter.applyHighlightingImmediate(roundTripPane);
               } else {
                  roundTripPane.setText(
                        "// Round-trip decompiled code not available.\n// Save the file to trigger round-trip validation and decompilation.");
               }
            }
         }
      } catch (Exception e) {
         System.err.println("Error refreshing decompiled code view: " + e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * Gets the round-trip decompiled code using the shared RoundTripUtil. This
    * ensures we use the exact same logic as the test suite.
    *
    * @param savedNssFile The saved NSS file (after compilation, this should have a
    *                     corresponding .ncs file)
    * @return Round-trip decompiled NSS code, or null if not available
    */
   private String getRoundTripDecompiledCode(File savedNssFile) {
      String gameFlag = FileDecompiler.isK2Selected ? "k2" : "k1";
      return RoundTripUtil.getRoundTripDecompiledCode(savedNssFile, gameFlag);
   }

   private void saveAll() {
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         JComponent tabComponent = (JComponent) this.jTB.getTabComponentAt(i);
         if (tabComponent == null) {
            continue;
         }

         String fileName = ((JLabel) ((JPanel) tabComponent).getComponent(0)).getText();
         // Remove unsaved marker if present
         if (fileName.endsWith(" *")) {
            fileName = fileName.substring(0, fileName.length() - 2);
         }
         // Remove any existing extension to avoid double extensions
         int lastDot = fileName.lastIndexOf('.');
         if (lastDot > 0) {
            fileName = fileName.substring(0, lastDot);
         }

         File file = this.hash_TabComponent2File.get(tabComponent);

         // Determine output file path (same logic as save())
         File newFile;
         if (file != null && file.exists()) {
            // File exists - use its directory as the save location
            File parentDir = file.getParentFile();
            if (parentDir != null && parentDir.exists()) {
               newFile = new File(parentDir, buildOutputFilename(fileName));
            } else {
               // Fallback to default output directory
               String outputDir = settings.getProperty("Output Directory");
               if (outputDir == null || outputDir.isEmpty()) {
                  outputDir = System.getProperty("user.dir");
               }
               newFile = new File(outputDir, buildOutputFilename(fileName));
            }
         } else {
            // File doesn't exist (created from scratch) - save to default output directory
            String outputDir = settings.getProperty("Output Directory");
            if (outputDir == null || outputDir.isEmpty()) {
               outputDir = System.getProperty("user.dir");
            }
            newFile = new File(outputDir, buildOutputFilename(fileName));
            // Ensure output directory exists
            File outputDirFile = newFile.getParentFile();
            if (outputDirFile != null && !outputDirFile.exists()) {
               System.out.println("[INFO] Decompiler: CREATING output directory: " + outputDirFile.getAbsolutePath());
               if (!outputDirFile.mkdirs()) {
                  System.err.println("[ERROR] Decompiler: Failed to create output directory: " + outputDirFile.getAbsolutePath());
               } else {
                  System.out.println("[INFO] Decompiler: Created output directory: " + outputDirFile.getAbsolutePath());
               }
            }
         }

         // Get the text component from the JSplitPane (same as save() method)
         Object clientProperty = this.jTB.getClientProperty(tabComponent);
         if (!(clientProperty instanceof JComponent[])) {
            continue; // Invalid client property
         }
         JComponent[] panels = (JComponent[]) clientProperty;
         if (panels.length == 0 || !(panels[0] instanceof JSplitPane)) {
            continue; // Invalid panel structure
         }
         JSplitPane decompSplitPane = (JSplitPane) panels[0];
         java.awt.Component leftComp = decompSplitPane.getLeftComponent();
         if (!(leftComp instanceof JScrollPane)) {
            continue; // Invalid component structure
         }
         JScrollPane scrollPane = (JScrollPane) leftComp;
         if (!(scrollPane.getViewport().getView() instanceof JTextComponent)) {
            continue; // Invalid view type
         }
         JTextComponent textArea = (JTextComponent) scrollPane.getViewport().getView();
         newFile = this.saveBuffer(textArea, newFile.getAbsolutePath());
         if (newFile == null) {
            continue; // saveBuffer failed
         }

         // If file was null or didn't exist, update the mapping
         if (file == null || !file.exists()) {
            file = newFile.getAbsoluteFile();
            this.hash_TabComponent2File.put(tabComponent, file);
         }

         int compileResult = 2;

         // Only do round-trip validation if the original file exists
         if (file != null && file.exists()) {
            try {
               compileResult = this.fileDecompiler.compileAndCompare(file, newFile);
            } catch (DecompilerException var6) {
               JOptionPane.showMessageDialog(null, var6.getMessage());
               newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
            }
         } else {
            // File was created from scratch - just mark as saved
            compileResult = 1; // SUCCESS
         }

         switch (compileResult) {
         case 0:
            newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
            break;
         case 1:
            this.updateTabLabel((JPanel) this.jTB.getTabComponentAt(i), false);
            break;
         case 2:
            newFile.renameTo(new File(this.getShortName(newFile) + "_compile_fails.nss"));
            break;
         case 3:
            newFile.renameTo(new File(this.getShortName(newFile) + "_compare_fails.nss"));
            break;
         default:
            break;
         }
      }
      this.updateWorkspaceCard();
   }

   private String getShortName(File in) {
      int i = in.getAbsolutePath().lastIndexOf(46);
      return i == -1 ? in.getAbsolutePath() : in.getAbsolutePath().substring(0, i);
   }

   /**
    * Filters the log display based on the selected log level.
    * Re-applies all log entries with proper filtering.
    */
   /**
    * Re-filters and re-renders all log lines based on current filter level.
    * This ensures that changing the filter shows/hides previously logged messages in real-time.
    */
   private void filterLogs() {
      if (this.status == null) {
         return;
      }

      SwingUtilities.invokeLater(() -> {
         try {
            javax.swing.text.StyledDocument doc = this.status.getStyledDocument();

            // Clear the entire log
            doc.remove(0, doc.getLength());

            // Get current filter level
            String selectedLevel = (String) this.logLevelFilter.getSelectedItem();
            if (selectedLevel == null) {
               selectedLevel = LOG_LEVELS[DEFAULT_LOG_LEVEL_INDEX];
            }
            int selectedIndex = DualOutputPrintStream.getSeverityIndex(selectedLevel);

            // Re-render all log lines that match the current filter
            synchronized (this.allLogLines) {
               for (LogLine logLine : this.allLogLines) {
                  int logIndex = logLine.severity.ordinal();
                  // Show if log severity is >= selected filter level
                  if (logIndex >= selectedIndex) {
                     // Parse ANSI codes and render with colors
                     DualOutputPrintStream.parseAndAppendAnsiText(logLine.text, this.status, doc);
                  }
                  // If filtered out, don't append anything (no blank lines)
               }
            }

            // Auto-scroll to bottom
            this.status.setCaretPosition(doc.getLength());
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }


   /**
    * Helper method to append text to status log with INFO severity.
    */
   private void appendStatus(String text) {
      if (text != null && this.status != null) {
         try {
            javax.swing.text.StyledDocument doc = this.status.getStyledDocument();
            javax.swing.text.SimpleAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(attr, java.awt.Color.BLACK);
            javax.swing.text.StyleConstants.setFontFamily(attr, "Consolas");
            javax.swing.text.StyleConstants.setFontSize(attr, 11);
            int start = doc.getLength();
            doc.insertString(start, text, attr);
            this.status.setCaretPosition(doc.getLength());
         } catch (javax.swing.text.BadLocationException e) {
            // Fallback
            try {
               javax.swing.text.StyledDocument doc = this.status.getStyledDocument();
               doc.insertString(doc.getLength(), text, null);
               this.status.setCaretPosition(doc.getLength());
            } catch (javax.swing.text.BadLocationException e2) {
               // Ignore
            }
         }
      }
   }

   /**
    * Dumps the error buffer to the compilation error view.
    * @param tabComponent The tab component to show errors for
    * @param buffer The log buffer to dump
    */
   private void dumpErrorBufferToCompilationView(JComponent tabComponent, java.util.List<String> buffer) {
      if (tabComponent == null || buffer == null || buffer.isEmpty()) {
         return;
      }

      // Get the bytecode split pane
      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return;
      }

      JComponent[] panels = (JComponent[]) clientProperty;
      if (panels.length < 2 || !(panels[1] instanceof JSplitPane)) {
         return;
      }

      JSplitPane byteCodeSplitPane = (JSplitPane) panels[1];

      // Create error display
      JTextPane errorTextPane = new JTextPane();
      errorTextPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
      errorTextPane.setEditable(false);
      errorTextPane.setBackground(new java.awt.Color(255, 240, 240)); // Light red background
      errorTextPane.setForeground(new java.awt.Color(200, 0, 0)); // Dark red text

      JScrollPane errorScrollPane = new JScrollPane(errorTextPane);
      errorScrollPane.setBorder(new TitledBorder("Compilation Errors"));

      // Dump entire buffer
      StringBuilder errorText = new StringBuilder();
      for (String line : buffer) {
         // Strip ANSI codes for display
         String cleanLine = line.replaceAll("\u001B\\[[0-9;]+m", "");
         errorText.append(cleanLine);
         if (!cleanLine.endsWith("\n")) {
            errorText.append("\n");
         }
      }

      // Set error text
      errorTextPane.setText(errorText.toString());
      errorTextPane.setCaretPosition(0);

      // Show error panel
      byteCodeSplitPane.setRightComponent(errorScrollPane);
      byteCodeSplitPane.setDividerLocation(0.5); // Split evenly
   }

   /**
    * Dumps the error buffer to the decompilation error view.
    * @param tabComponent The tab component to show errors for
    * @param buffer The log buffer to dump
    */
   private void dumpErrorBufferToDecompilationView(JComponent tabComponent, java.util.List<String> buffer) {

      if (tabComponent == null || buffer == null || buffer.isEmpty()) {
         return;
      }

      // Get the bytecode split pane (panels[1])
      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return;
      }

      JComponent[] panels = (JComponent[]) clientProperty;
      if (panels.length < 2 || !(panels[1] instanceof JSplitPane)) {
         return;
      }

      JSplitPane byteCodeSplitPane = (JSplitPane) panels[1];

      // Create error display
      JTextPane errorTextPane = new JTextPane();
      errorTextPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
      errorTextPane.setEditable(false);
      errorTextPane.setBackground(new java.awt.Color(255, 240, 240)); // Light red background
      errorTextPane.setForeground(new java.awt.Color(200, 0, 0)); // Dark red text

      JScrollPane errorScrollPane = new JScrollPane(errorTextPane);
      errorScrollPane.setBorder(new TitledBorder("Decompilation Errors"));

      // Dump entire buffer
      StringBuilder errorText = new StringBuilder();
      for (String line : buffer) {
         // Strip ANSI codes for display
         String cleanLine = line.replaceAll("\u001B\\[[0-9;]+m", "");
         errorText.append(cleanLine);
         if (!cleanLine.endsWith("\n")) {
            errorText.append("\n");
         }
      }

      // Set error text
      errorTextPane.setText(errorText.toString());
      errorTextPane.setCaretPosition(0);

      // Show error panel
      byteCodeSplitPane.setRightComponent(errorScrollPane);
      byteCodeSplitPane.setDividerLocation(0.5); // Split evenly
   }

   /**
    * Creates a support development card for the empty state.
    * @return A panel styled as a support card with light theme colors
    */
   private JPanel createSupportCard() {
      JPanel card = new JPanel(new BorderLayout());
      card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)));
      card.setBackground(java.awt.Color.WHITE);
      card.setPreferredSize(new Dimension(320, 200));

      // Title with heart icon
      JLabel titleLabel = new JLabel("<html><div style='text-align:center;'>" +
            "<span style='color:#0066CC; font-size:18px; font-weight:bold;'>" +
            "❤ Support Development</span></div></html>");
      titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
      card.add(titleLabel, BorderLayout.NORTH);

      // Main message
      JLabel messageLabel = new JLabel("<html><div style='text-align:center; margin-top:12px; margin-bottom:16px;'>" +
            "<span style='color:#333333; font-size:13px;'>" +
            "If you enjoy using NCSDecomp, please consider supporting its development!</span></div></html>");
      messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
      card.add(messageLabel, BorderLayout.CENTER);

      // Sponsor button
      JButton sponsorButton = new JButton("<html><div style='text-align:center;'>" +
            "<span style='color:#FFFFFF; font-size:13px;'>❤ Sponsor on GitHub</span></div></html>");
      sponsorButton.setBackground(new java.awt.Color(0, 102, 204)); // Blue background
      sponsorButton.setForeground(java.awt.Color.WHITE);
      sponsorButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(0, 80, 160), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
      sponsorButton.setFocusPainted(false);
      sponsorButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
      sponsorButton.addActionListener(e -> this.openLink(SPONSOR_URL, "Opening sponsor page"));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      buttonPanel.setOpaque(false);
      buttonPanel.add(sponsorButton);

      // Footer text
      JLabel footerLabel = new JLabel("<html><div style='text-align:center; margin-top:16px;'>" +
            "<span style='color:#666666; font-size:11px;'>" +
            "Your support helps maintain and improve this tool</span></div></html>");
      footerLabel.setHorizontalAlignment(SwingConstants.CENTER);

      // Create a container for button and footer
      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.setOpaque(false);
      bottomPanel.add(buttonPanel, BorderLayout.CENTER);
      bottomPanel.add(footerLabel, BorderLayout.SOUTH);
      card.add(bottomPanel, BorderLayout.SOUTH);

      return card;
   }

}
