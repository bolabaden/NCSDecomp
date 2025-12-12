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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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

/**
 * Swing GUI application for decompiling NCS scripts and viewing results.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Load/save/open multiple scripts with tabbed views.</li>
 *   <li>Display subroutine/variable tree, generated code, and bytecode diffs.</li>
 *   <li>Wire drag-and-drop, menus, keyboard shortcuts, and scroll/link behavior.</li>
 * </ul>
 */
public class Decompiler
   extends JFrame
   implements DropTargetListener,
   KeyListener,
   ChangeListener,
   TreeSelectionListener,
   ActionListener,
   WindowListener,
   MouseListener,
   AdjustmentListener,
   CaretListener {
   public static Settings settings = new Settings();
   public static final double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
   public static final double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private static final long serialVersionUID = 1L;
   private JSplitPane upperJSplitPane;
   private JSplitPane mainJSplitPane;
   private JScrollPane jTreeScrollPane;
   private JTree jTree;
   private JTabbedPane jTB;
   private JTextArea jTA;
   private DropTarget dropTarget;
   private JPanel leftPanel;
   private JTextArea status;
   private JScrollPane statusScrollPane;
   private transient Element rootElement;
   private TitledBorder titledBorder;
   private int mark;
   private String temp;
   private JPanel panel;
   private File file;
   private Hashtable<String, Vector<Variable>> hash_Func2VarVec;
   private JComponent[] panels;
   private JTextArea origByteCodeJTA;
   private JTextArea newByteCodeJTA;
   private String currentNodeString;
   private transient Map<JComponent, File> hash_TabComponent2File;
   private transient Map<JComponent, Hashtable<String, Vector<Variable>>> hash_TabComponent2Func2VarVec;
   private transient Map<JComponent, TreeModel> hash_TabComponent2TreeModel;
   protected static List<File> unsavedFiles;
   private transient FileDecompiler fileDecompiler = new FileDecompiler();
   private JToolBar commandBar;
   private JTextField treeFilterField;
   private JLabel statusBarLabel;
   private JPanel workspaceCards;
   private JLabel emptyStateLabel;
   private static final String CARD_EMPTY = "empty";
   private static final String CARD_TABS = "tabs";
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
            if (defaultDir.mkdirs()) {
               settings.setProperty("Output Directory", defaultOutputDir);
            } else {
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
         "<html><div style='text-align:center;'><h2>Drop .ncs files here</h2><div>Or use File → Open to start decompiling</div></div></html>",
         SwingConstants.CENTER
      );
      this.emptyStateLabel.setBorder(new EmptyBorder(32, 16, 32, 16));
      JPanel emptyPanel = new JPanel(new BorderLayout());
      emptyPanel.add(this.emptyStateLabel, BorderLayout.CENTER);
      this.workspaceCards.add(emptyPanel, CARD_EMPTY);
      this.workspaceCards.add(this.jTB, CARD_TABS);
      this.dropTarget = new DropTarget(this.workspaceCards, this);

      this.upperJSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, this.workspaceCards);
      this.upperJSplitPane.setDividerLocation(260);
      this.upperJSplitPane.setResizeWeight(0.2);
      this.upperJSplitPane.setDividerSize(6);

      // Status/logging area
      this.status = new JTextArea(8, 80);
      this.status.setEditable(false);
      this.statusScrollPane = new JScrollPane(this.status);
      this.statusScrollPane.setVerticalScrollBarPolicy(22);
      this.statusScrollPane.setHorizontalScrollBarPolicy(32);
      JPopupMenu statusPopupMenu = new JPopupMenu();
      JMenuItem clearItem = new JMenuItem("Clear");
      clearItem.addActionListener(this);
      statusPopupMenu.add(clearItem);
      this.status.setComponentPopupMenu(statusPopupMenu);

      JPanel statusBar = new JPanel(new BorderLayout());
      this.statusBarLabel = new JLabel("Ready");
      statusBar.add(this.statusBarLabel, BorderLayout.WEST);

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
      this.setVisible(true);
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
      CardLayout cl = (CardLayout)this.workspaceCards.getLayout();
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
               JButton button = (JButton)comp;
               String action = (String)button.getClientProperty("action");
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
         JLabel label = (JLabel)center;
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
    * @return The tab component, or null if no valid tab is selected
    */
   private JComponent getSelectedTabComponent() {
      int selectedIndex = this.jTB.getSelectedIndex();
      if (selectedIndex < 0 || selectedIndex >= this.jTB.getTabCount()) {
         return null;
      }
      return (JComponent)this.jTB.getTabComponentAt(selectedIndex);
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
      String aboutHtml =
         "<html>"
            + "<h2>NCSDecomp</h2>"
            + "<p>KotOR / TSL NCS script decompiler rebuilt with modern workflows. "
            + "Origins in the classic DeNCS tooling; re-implemented with improved heuristics, UI, and headless/CLI support.</p>"
            + "<ul>"
            + "<li><b>What</b>: Decompile .ncs bytecode to readable .nss; inspect bytecode and regenerated output side-by-side.</li>"
            + "<li><b>How</b>: Swing GUI with drag/drop, multi-tab workspaces, and bytecode diffing; CLI for batch/headless runs.</li>"
            + "<li><b>Why</b>: Provide a reliable, transparent pipeline for modders and reverse-engineers working on KotOR/TSL.</li>"
            + "<li><b>When</b>: Use for day-to-day modding, audits, or batch decompilation workflows.</li>"
            + "<li><b>Links</b>: "
            + "<a href='https://bolabaden.org'>bolabaden.org</a> · "
            + "<a href='https://github.com/OldRepublicDevs'>github.com/OldRepublicDevs</a> · "
            + "<a href='https://github.com/bolabaden'>github.com/bolabaden</a></li>"
            + "</ul>"
            + "</html>";

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
               final List<File> rawList = (List<File>)tr.getTransferData(flavors[i]);
               final List<File> list = new ArrayList<>();
               for (File file : rawList) {
                  if (file != null) {
                     list.add(file);
                  }
               }
               (new Thread() {
                  @Override
                  public void run() {
                     Decompiler.this.open(list.toArray(new File[0]));
                  }
               }).start();
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
         JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(selectedIndex);
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
      
      JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }
      
      if (arg0.getSource() instanceof JTree) {
         TreePath changedPath = ((JTree)arg0.getSource()).getSelectionPath();
         File file = this.hash_TabComponent2File.get(tabComponent);
         if (file == null) {
            return; // File not found for this tab
         }
         
         Object clientProperty = this.jTB.getClientProperty(tabComponent);
         if (!(clientProperty instanceof JComponent[])) {
            return; // Invalid client property
         }
         
         JComponent[] panels = (JComponent[])clientProperty;
            if (changedPath != null && changedPath.getPathCount() == 2) {
               Hashtable<String, Vector<Variable>> func2VarVec = this.fileDecompiler.updateSubName(file, this.currentNodeString, changedPath.getLastPathComponent().toString());
               this.hash_TabComponent2Func2VarVec.put(tabComponent, func2VarVec);
               this.hash_TabComponent2TreeModel.put(tabComponent, this.jTree.getModel());
               if (panels.length > 0 && panels[0] instanceof JPanel) {
                  JPanel panel = (JPanel)panels[0];
                  if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JScrollPane) {
                     JScrollPane scrollPane = (JScrollPane)panel.getComponent(0);
                     if (scrollPane.getViewport().getView() instanceof JTextArea) {
                        this.jTA = (JTextArea)scrollPane.getViewport().getView();
                        this.jTA.setText(this.fileDecompiler.getGeneratedCode(file));
                        this.jTA.setCaretPosition(0);
                     }
                  }
               }
            } else if (changedPath != null) {
               TreeNode subroutineNode = (TreeNode)changedPath.getParentPath().getLastPathComponent();
               Hashtable<String, Vector<Variable>> func2VarVec = this.hash_TabComponent2Func2VarVec.get(tabComponent);
               if (func2VarVec != null) {
                  Vector<Variable> variables = func2VarVec.get(subroutineNode.toString());
                  if (variables != null) {
                     int nodeIndex = subroutineNode.getIndex((TreeNode)changedPath.getLastPathComponent());
                     if (nodeIndex >= 0 && nodeIndex < variables.size()) {
                        Variable changedVar = variables.get(nodeIndex);
                        changedVar.name(changedPath.getLastPathComponent().toString());
                        if (panels.length > 0 && panels[0] instanceof JPanel) {
                           JPanel panel = (JPanel)panels[0];
                           if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JScrollPane) {
                              JScrollPane scrollPane = (JScrollPane)panel.getComponent(0);
                              if (scrollPane.getViewport().getView() instanceof JTextArea) {
                                 this.jTA = (JTextArea)scrollPane.getViewport().getView();
                                 this.jTA.setText(this.fileDecompiler.regenerateCode(file));
                                 this.jTA.setCaretPosition(0);
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
         } else if (arg0.getSource() instanceof JTextArea) {
            File file = this.hash_TabComponent2File.get(tabComponent);
            if (file != null && !unsavedFiles.contains(file)) {
               unsavedFiles.add(file);
            }
            if (tabComponent instanceof JPanel) {
               this.updateTabLabel((JPanel)tabComponent, true);
            }
         }
   }

   @Override
   public void keyTyped(KeyEvent arg0) {
   }

   @Override
   public void valueChanged(TreeSelectionEvent arg0) {
      if (((JTree)arg0.getSource()).getSelectionPath() != null) {
         this.currentNodeString = ((JTree)arg0.getSource()).getSelectionPath().getLastPathComponent().toString();
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
         this.status.setText("");
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
      this.panel = (JPanel)((JLabel)arg0.getSource()).getParent();

      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         if (this.jTB.getTabComponentAt(i) == this.panel) {
            this.close(i);
            break;
         }
      }

      ((JLabel)arg0.getSource()).removeMouseListener(this);
      this.panel = null;
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
      
      JSplitPane splitPane = (JSplitPane)selectedComponent;
      Object linkProperty = this.jTB.getClientProperty(selectedComponent);
      if (linkProperty == null) {
         return; // No link property set
      }
      
      java.awt.Component leftComp = splitPane.getLeftComponent();
      java.awt.Component rightComp = splitPane.getRightComponent();
      
      if (!(leftComp instanceof JScrollPane) || !(rightComp instanceof JScrollPane)) {
         return; // Invalid component structure
      }
      
      JScrollPane leftScroll = (JScrollPane)leftComp;
      JScrollPane rightScroll = (JScrollPane)rightComp;
      
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
      
      JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }
      
      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }
      
      JComponent[] panels = (JComponent[])clientProperty;
      if (panels.length == 0 || !(panels[0] instanceof JPanel)) {
         return; // Invalid panel structure
      }
      
      JPanel panel = (JPanel)panels[0];
      if (!(panel.getBorder() instanceof TitledBorder)) {
         return; // Invalid border type
      }
      
      this.jTA = (JTextArea)arg0.getSource();
      this.mark = this.jTA.getCaretPosition();
      this.rootElement = this.jTA.getDocument().getDefaultRootElement();
      this.titledBorder = (TitledBorder)panel.getBorder();
      if (!(this.temp = Integer.toString(this.rootElement.getElementIndex(this.mark) + 1)).equals(this.titledBorder.getTitle())) {
         this.titledBorder.setTitle(this.temp);
         this.repaint();
      }
   }

   private JComponent[] newNCSTab(String text) {
      return this.newNCSTab(text, this.jTB.getTabCount());
   }

   private JComponent[] newNCSTab(String text, int index) {
      JComponent[] tabComponents = new JComponent[2];
      JPanel decompPanel = new JPanel(new BorderLayout());
      TitledBorder border = new TitledBorder("1");
      border.setTitlePosition(5);
      border.setTitleJustification(4);
      decompPanel.setBorder(border);
      JTextArea textArea = new JTextArea();
      textArea.addCaretListener(this);
      textArea.addKeyListener(this);
      this.dropTarget = new DropTarget(textArea, this);
      textArea.putClientProperty("dropTarget", this.dropTarget);
      JScrollPane scrollPane = new JScrollPane(textArea);
      decompPanel.add(scrollPane, "Center");
      JPopupMenu panelSwitchPopup = new JPopupMenu();
      JMenuItem menuItem = new JMenuItem("View Byte Code");
      menuItem.addActionListener(this);
      panelSwitchPopup.add(menuItem);
      textArea.setComponentPopupMenu(panelSwitchPopup);
      tabComponents[0] = decompPanel;
      JSplitPane byteCodePane = new JSplitPane(1);
      byteCodePane.setDividerLocation(320);
      byteCodePane.setDividerSize(5);
      textArea = new JTextArea();
      textArea.setFont(new Font("Monospaced", 0, 12));
      textArea.setEditable(false);
      this.dropTarget = new DropTarget(textArea, this);
      textArea.putClientProperty("dropTarget", this.dropTarget);
      scrollPane = new JScrollPane(textArea);
      scrollPane.setBorder(new TitledBorder("Original Byte Code"));
      scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
      byteCodePane.setLeftComponent(scrollPane);
      panelSwitchPopup = new JPopupMenu();
      menuItem = new JMenuItem("View Decompiled Code");
      menuItem.addActionListener(this);
      panelSwitchPopup.add(menuItem);
      JCheckBoxMenuItem jCBMI = new JCheckBoxMenuItem("Link Scroll Bars");
      jCBMI.setSelected(Boolean.parseBoolean(settings.getProperty("Link Scroll Bars")));
      jCBMI.addActionListener(this);
      panelSwitchPopup.add(jCBMI);
      textArea.setComponentPopupMenu(panelSwitchPopup);
      textArea = new JTextArea();
      textArea.setFont(new Font("Monospaced", 0, 12));
      textArea.setEditable(false);
      this.dropTarget = new DropTarget(textArea, this);
      textArea.putClientProperty("dropTarget", this.dropTarget);
      scrollPane = new JScrollPane(textArea);
      scrollPane.setBorder(new TitledBorder("Recompiled Byte Code"));
      scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
      byteCodePane.setRightComponent(scrollPane);
      textArea.setComponentPopupMenu(panelSwitchPopup);
      tabComponents[1] = byteCodePane;
      JPanel panel = new JPanel(new BorderLayout());
      this.jTB.insertTab(null, null, tabComponents[0], null, index);
      panel.setOpaque(false);
      JLabel label = new JLabel(text);
      label.setBorder(new EmptyBorder(0, 0, 0, 10));
      label.setOpaque(false);
      JLabel closeLabel = new JLabel("X");
      closeLabel.setOpaque(false);
      closeLabel.addMouseListener(this);
      panel.add(label, "Center");
      panel.add(closeLabel, "East");
      this.jTB.setTabComponentAt(index, panel);
      this.jTB.putClientProperty(panel, tabComponents);
      this.jTB.setSelectedIndex(index);
      this.updateWorkspaceCard();
      return tabComponents;
   }

   private void decompile(File file) {
      this.status.append("Decompiling...");
      this.statusScrollPane.getVerticalScrollBar().setValue(this.statusScrollPane.getVerticalScrollBar().getMaximum());
      this.status.append(file.getName() + ": ");
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
         this.status.append("unexpected error: " + unexpected.getMessage() + "\n");
         generatedCode = "// Unexpected Decompilation Error\n" +
                        "// File: " + file.getName() + "\n" +
                        "// Error: " + unexpected.getMessage() + "\n" +
                        "void main() {\n    // Unexpected error occurred\n}\n";
         result = 2; // PARTIAL_COMPILE - we're showing something
      }

      // ALWAYS show source code - we guarantee generatedCode is never null/empty at this point
      // If decompilation completely failed, we created a fallback stub above
      if (generatedCode == null || generatedCode.trim().isEmpty()) {
         // Ultimate fallback - should never happen, but ensure we always show something
         generatedCode = "// No code available\n" +
                        "// File: " + file.getName() + "\n" +
                        "void main() {\n    // No decompiled code\n}\n";
         result = 2;
      }
      
      // Now we're guaranteed to have code - always show it
      {
         this.panels = this.newNCSTab(file.getName().substring(0, file.getName().length() - 4));
         JTextArea codeArea = (JTextArea)((JScrollPane)((JPanel)this.panels[0]).getComponent(0)).getViewport().getView();
         codeArea.append(generatedCode);
         
         // Try to get bytecode if available
         try {
            String origByteCode = this.fileDecompiler.getOriginalByteCode(file);
            if (origByteCode != null && !origByteCode.trim().isEmpty()) {
               this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
               this.origByteCodeJTA.append(origByteCode);
            }
            String newByteCode = this.fileDecompiler.getNewByteCode(file);
            if (newByteCode != null && !newByteCode.trim().isEmpty()) {
               this.newByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getRightComponent()).getViewport().getComponent(0);
               this.newByteCodeJTA.append(newByteCode);
               if (this.origByteCodeJTA != null && this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
                  this.jTB.putClientProperty(this.panels[1], "left");
               } else {
                  this.jTB.putClientProperty(this.panels[1], "right");
               }
            }
         } catch (Exception e) {
            // Bytecode not available, that's okay
            System.out.println("Bytecode comparison not available: " + e.getMessage());
         }

         if (vars != null) {
            this.hash_Func2VarVec = vars;
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
         }
         JComponent tabComponent = this.getSelectedTabComponent();
         if (tabComponent != null) {
            this.hash_TabComponent2File.put(tabComponent, file);
            this.hash_TabComponent2Func2VarVec.put(tabComponent, this.hash_Func2VarVec);
            this.hash_TabComponent2TreeModel.put(tabComponent, this.jTree.getModel());
            if (tabComponent instanceof JPanel) {
               this.updateTabLabel((JPanel)tabComponent, false);
            }
         }
      }

      // Update status based on result - code is already shown above
      switch (result) {
         case 0:
            this.status.append("failure - validation failed\n");
            break;
         case 1:
            this.status.append("success - full round-trip validation passed\n");
            break;
         case 2:
            this.status.append("partial - decompiled successfully (nwnnsscomp validation skipped or failed)\n");
            break;
         case 3:
            this.status.append("partial - decompiled but bytecode comparison showed differences\n");
            break;
         default:
            this.status.append("unknown result code: " + result + "\n");
            break;
      }
      
      // Update workspace visibility after adding a file
      this.updateWorkspaceCard();
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
               return settings.getProperty("Output Directory").equals("") ? System.getProperty("user.dir") : settings.getProperty("Output Directory");
            }
         default:
            return settings.getProperty("Output Directory").equals("") ? System.getProperty("user.dir") : settings.getProperty("Output Directory");
      }
   }

   private File saveBuffer(JTextArea buffer, String canonicalPath) {
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
         
         BufferedWriter bw = new BufferedWriter(
            new java.io.OutputStreamWriter(
               new java.io.FileOutputStream(canonicalPath),
               charset
            )
         );
         bw.write(buffer.getText());
         bw.close();
         return new File(canonicalPath);
      } catch (FileNotFoundException var4) {
         File toDel = new File(canonicalPath);
         JOptionPane.showMessageDialog(null, "Error saving " + toDel.getName() + "\nOutput directory does not exist; change in settings");
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
      
      JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(selectedIndex);
      if (tabComponent == null) {
         return; // Tab component is null
      }
      
      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }
      
      JComponent[] panels = (JComponent[])clientProperty;
      if (index < 0 || index >= panels.length || panels[index] == null) {
         return; // Invalid panel index
      }
      
      // If switching to bytecode view (index 1), populate bytecode data if available
      if (index == 1 && panels[1] instanceof JSplitPane) {
         try {
            File file = this.hash_TabComponent2File.get(tabComponent);
            if (file != null) {
               JSplitPane byteCodePane = (JSplitPane)panels[1];
               
               // Get left component (original bytecode)
               java.awt.Component leftComp = byteCodePane.getLeftComponent();
               if (leftComp instanceof JScrollPane) {
                  JTextArea origTextArea = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
                  if (origTextArea != null && origTextArea.getText().trim().isEmpty()) {
                     String origByteCode = this.fileDecompiler.getOriginalByteCode(file);
                     if (origByteCode != null && !origByteCode.trim().isEmpty()) {
                        origTextArea.setText(origByteCode);
                     } else {
                        origTextArea.setText("// Original bytecode not available.\n// Bytecode is only captured during round-trip validation (save/recompile).");
                     }
                  }
               }
               
               // Get right component (recompiled bytecode)
               java.awt.Component rightComp = byteCodePane.getRightComponent();
               if (rightComp instanceof JScrollPane) {
                  JTextArea newTextArea = (JTextArea)((JScrollPane)rightComp).getViewport().getView();
                  if (newTextArea != null && newTextArea.getText().trim().isEmpty()) {
                     String newByteCode = this.fileDecompiler.getNewByteCode(file);
                     if (newByteCode != null && !newByteCode.trim().isEmpty()) {
                        newTextArea.setText(newByteCode);
                        // Set focus based on which has more content
                        if (leftComp instanceof JScrollPane) {
                           JTextArea origTextArea = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
                           if (origTextArea != null && origTextArea.getLineCount() >= newTextArea.getLineCount()) {
                              this.jTB.putClientProperty(panels[1], "left");
                           } else {
                              this.jTB.putClientProperty(panels[1], "right");
                           }
                        }
                     } else {
                        newTextArea.setText("// Recompiled bytecode not available.\n// Save the file to trigger round-trip validation and bytecode capture.");
                     }
                  }
               }
            }
         } catch (Exception e) {
            // If there's an error, show a message but still switch to the view
            System.out.println("Error populating bytecode view: " + e.getMessage());
            e.printStackTrace();
            try {
               JSplitPane byteCodePane = (JSplitPane)panels[1];
               java.awt.Component leftComp = byteCodePane.getLeftComponent();
               if (leftComp instanceof JScrollPane) {
                  JTextArea origTextArea = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
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
         settings.getProperty("Open Directory").equals("")
            ? new File(settings.getProperty("Output Directory"))
            : new File(settings.getProperty("Open Directory"))
      );
      jFC.setMultiSelectionEnabled(true);
      jFC.setFileFilter(new FileFilter() {
         @Override
         public boolean accept(File f) {
            Decompiler var10000 = Decompiler.this;
            String var10002 = f.getAbsolutePath();
            var10000.temp = var10002;
            return var10002.substring(Decompiler.this.temp.length() - 3).equalsIgnoreCase("ncs") || f.isDirectory();
         }

         @Override
         public String getDescription() {
            return ".ncs";
         }
      });
      switch (jFC.showDialog(null, "Open")) {
         case 0:
            final File[] files = jFC.getSelectedFiles();
            settings.setProperty("Open Directory", files[0].getParent());
            (new Thread() {
               @Override
               public void run() {
                  Decompiler.this.open(files);
               }
            }).start();
            break;
         case 1:
            break;
         default:
            JOptionPane.showMessageDialog(null, "Error opening file(s)");
      }
   }

   private void open(File[] files) {
      for (int j = 0; j < files.length; j++) {
         File fileToOpen = files[j];
         if ((this.temp = fileToOpen.getName()).substring(this.temp.length() - 3).equalsIgnoreCase("ncs")) {
            // Ensure we use the absolute path of the dropped/opened file
            File absoluteFile = fileToOpen.getAbsoluteFile();
            if (!absoluteFile.exists()) {
               this.status.append("Warning: File does not exist: " + absoluteFile.getAbsolutePath() + "\n");
               continue;
            }
            
            // Update Open Directory setting to the file's parent directory
            File parentDir = absoluteFile.getParentFile();
            if (parentDir != null && parentDir.exists()) {
               settings.setProperty("Open Directory", parentDir.getAbsolutePath());
            }
            
            this.decompile(absoluteFile);
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
            JSplitPane splitPane = (JSplitPane)selectedComponent;
            java.awt.Component leftComp = splitPane.getLeftComponent();
            java.awt.Component rightComp = splitPane.getRightComponent();
            if (leftComp instanceof JScrollPane) {
               ((JScrollPane)leftComp).getVerticalScrollBar().setValue(0);
            }
            if (rightComp instanceof JScrollPane) {
               ((JScrollPane)rightComp).getVerticalScrollBar().setValue(0);
            }
         }
      }
   }

   private void close(int index) {
      this.panel = (JPanel)this.jTB.getTabComponentAt(index);

      try {
         this.file = this.hash_TabComponent2File.get(this.panel);
         if (unsavedFiles.contains(this.file)) {
            switch (JOptionPane.showConfirmDialog(
               null, (this.temp = this.file.getName()).substring(0, this.temp.length() - 4) + ".nss is unsaved.  Would you like to save it?"
            )) {
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
      
      JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(index);
      if (tabComponent == null) {
         return; // Tab component is null
      }
      
      Object clientProperty = this.jTB.getClientProperty(tabComponent);
      if (!(clientProperty instanceof JComponent[])) {
         return; // Invalid client property
      }
      
      JComponent[] panels = (JComponent[])clientProperty;
      if (panels.length == 0 || !(panels[0] instanceof JPanel)) {
         return; // Invalid panel structure
      }
      
      JPanel panel = (JPanel)panels[0];
      if (panel.getComponentCount() == 0 || !(panel.getComponent(0) instanceof JScrollPane)) {
         return; // Invalid component structure
      }
      
      JScrollPane scrollPane = (JScrollPane)panel.getComponent(0);
      if (!(scrollPane.getViewport().getView() instanceof JTextArea)) {
         return; // Invalid view type
      }
      
      JTextArea textArea = (JTextArea)scrollPane.getViewport().getView();
      if (!(tabComponent instanceof JPanel)) {
         return; // Tab component is not a panel
      }
      
      JPanel tabPanel = (JPanel)tabComponent;
      if (tabPanel.getComponentCount() == 0 || !(tabPanel.getComponent(0) instanceof JLabel)) {
         return; // Invalid tab label structure
      }
      
      String fileName = ((JLabel)tabPanel.getComponent(0)).getText();
      // Remove unsaved marker if present
      if (fileName.endsWith(" *")) {
         fileName = fileName.substring(0, fileName.length() - 2);
      }
      
      // Remove any existing extension to avoid double extensions
      int lastDot = fileName.lastIndexOf('.');
      if (lastDot > 0) {
         fileName = fileName.substring(0, lastDot);
      }
      
      this.file = this.hash_TabComponent2File.get(tabComponent);
      
      // Determine output file path
      File newFile;
      if (this.file != null && this.file.exists()) {
         // File exists - use its directory as the save location (preserve original path)
         File parentDir = this.file.getParentFile();
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
            if (!outputDirFile.mkdirs()) {
               this.status.append("Warning: Could not create output directory: " + outputDirFile.getAbsolutePath() + "\n");
            }
         }
      }
      
      newFile = this.saveBuffer(textArea, newFile.getAbsolutePath());
      if (newFile == null) {
         // saveBuffer failed (e.g., directory doesn't exist)
         return;
      }
      
      // If file was null or didn't exist, update the mapping to point to the newly saved file
      if (this.file == null || !this.file.exists()) {
         this.file = newFile;
         this.hash_TabComponent2File.put(tabComponent, this.file);
         // File was created from scratch - just mark as saved (no round-trip validation needed)
         unsavedFiles.remove(this.file);
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel)tabComponent, false);
         }
         this.status.append("Saved " + newFile.getName() + " to " + newFile.getParent() + "\n");
         return;
      }
      
      // Only do round-trip validation if the original file exists
      if (unsavedFiles.contains(this.file)) {
         this.status.append("Recompiling..." + this.file.getName() + ": ");
         int result = 2;

         try {
            result = this.fileDecompiler.compileAndCompare(this.file, newFile);
         } catch (DecompilerException var5) {
            JOptionPane.showMessageDialog(null, var5.getMessage());
         }

         switch (result) {
            case 0:
               this.status.append("failure\n");
               break;
            case 1:
               this.panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(index));
               if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
                  try {
                     String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                     String newByteCode = this.fileDecompiler.getNewByteCode(this.file);
                     
                     JSplitPane byteCodePane = (JSplitPane)this.panels[1];
                     java.awt.Component leftComp = byteCodePane.getLeftComponent();
                     if (leftComp instanceof JScrollPane) {
                        this.origByteCodeJTA = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
                        if (this.origByteCodeJTA != null) {
                           this.origByteCodeJTA.setText(origByteCode != null ? origByteCode : "// Original bytecode not available");
                        }
                     }
                     
                     java.awt.Component rightComp = byteCodePane.getRightComponent();
                     if (rightComp instanceof JScrollPane) {
                        this.newByteCodeJTA = (JTextArea)((JScrollPane)rightComp).getViewport().getView();
                        if (this.newByteCodeJTA != null) {
                           this.newByteCodeJTA.setText(newByteCode != null ? newByteCode : "// Recompiled bytecode not available");
                        }
                     }
                     
                     if (this.origByteCodeJTA != null && this.newByteCodeJTA != null) {
                        if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
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
               this.status.append("success\n");
               break;
            case 2:
               this.panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(index));
               if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
                  try {
                     String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                     JSplitPane byteCodePane = (JSplitPane)this.panels[1];
                     java.awt.Component leftComp = byteCodePane.getLeftComponent();
                     if (leftComp instanceof JScrollPane) {
                        this.origByteCodeJTA = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
                        if (this.origByteCodeJTA != null) {
                           this.origByteCodeJTA.setText(origByteCode != null ? origByteCode : "// Original bytecode not available");
                        }
                     }
                     this.jTB.putClientProperty(this.panels[1], "left");
                  } catch (Exception e) {
                     System.out.println("Error updating bytecode view: " + e.getMessage());
                  }
               }
               this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
               this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
               this.fileDecompiler.getOriginalByteCode(this.file);
               JComponent selectedTab2 = this.getSelectedTabComponent();
               if (selectedTab2 != null) {
                  this.hash_TabComponent2File.put(selectedTab2, this.file);
                  this.hash_TabComponent2Func2VarVec.put(selectedTab2, this.hash_Func2VarVec);
                  this.hash_TabComponent2TreeModel.put(selectedTab2, this.jTree.getModel());
               }
               this.setTabComponentPanel(1);
               this.status.append("partial-could not recompile\n");
               break;
            case 3:
               this.panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(index));
               if (this.panels != null && this.panels.length > 1 && this.panels[1] instanceof JSplitPane) {
                  try {
                     String origByteCode = this.fileDecompiler.getOriginalByteCode(this.file);
                     String newByteCode = this.fileDecompiler.getNewByteCode(this.file);
                     
                     JSplitPane byteCodePane = (JSplitPane)this.panels[1];
                     java.awt.Component leftComp = byteCodePane.getLeftComponent();
                     if (leftComp instanceof JScrollPane) {
                        this.origByteCodeJTA = (JTextArea)((JScrollPane)leftComp).getViewport().getView();
                        if (this.origByteCodeJTA != null) {
                           this.origByteCodeJTA.setText(origByteCode != null ? origByteCode : "// Original bytecode not available");
                        }
                     }
                     
                     java.awt.Component rightComp = byteCodePane.getRightComponent();
                     if (rightComp instanceof JScrollPane) {
                        this.newByteCodeJTA = (JTextArea)((JScrollPane)rightComp).getViewport().getView();
                        if (this.newByteCodeJTA != null) {
                           this.newByteCodeJTA.setText(newByteCode != null ? newByteCode : "// Recompiled bytecode not available");
                        }
                     }
                     
                     if (this.origByteCodeJTA != null && this.newByteCodeJTA != null) {
                        if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
                           this.jTB.putClientProperty(this.panels[1], "left");
                        } else {
                           this.jTB.putClientProperty(this.panels[1], "right");
                        }
                     }
                  } catch (Exception e) {
                     System.out.println("Error updating bytecode view: " + e.getMessage());
                  }
               }

               this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
               this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
               this.fileDecompiler.getOriginalByteCode(this.file);
               JComponent selectedTab3 = this.getSelectedTabComponent();
               if (selectedTab3 != null) {
                  this.hash_TabComponent2File.put(selectedTab3, this.file);
                  this.hash_TabComponent2Func2VarVec.put(selectedTab3, this.hash_Func2VarVec);
                  this.hash_TabComponent2TreeModel.put(selectedTab3, this.jTree.getModel());
               }
               this.setTabComponentPanel(1);
               this.status.append("partial-byte code does not match\n");
         }

         unsavedFiles.remove(this.file);
         if (tabComponent instanceof JPanel) {
            this.updateTabLabel((JPanel)tabComponent, false);
         }
         newFile = null;
      }
   }

   private void saveAll() {
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         JComponent tabComponent = (JComponent)this.jTB.getTabComponentAt(i);
         if (tabComponent == null) {
            continue;
         }
         
         String fileName = ((JLabel)((JPanel)tabComponent).getComponent(0)).getText();
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
               outputDirFile.mkdirs();
            }
         }
         
         JTextArea textArea = (JTextArea)((JScrollPane)((JComponent[])this.jTB.getClientProperty(tabComponent))[0].getComponent(0)).getViewport().getView();
         newFile = this.saveBuffer(textArea, newFile.getAbsolutePath());
         if (newFile == null) {
            continue; // saveBuffer failed
         }
         
         // If file was null or didn't exist, update the mapping
         if (file == null || !file.exists()) {
            file = newFile;
            this.hash_TabComponent2File.put(tabComponent, file);
         }
         
         int result = 2;

         // Only do round-trip validation if the original file exists
         if (file != null && file.exists()) {
            try {
               result = this.fileDecompiler.compileAndCompare(file, newFile);
            } catch (DecompilerException var6) {
               JOptionPane.showMessageDialog(null, var6.getMessage());
               newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
            }
         } else {
            // File was created from scratch - just mark as saved
            result = 1; // SUCCESS
         }

         switch (result) {
            case 0:
               newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
               break;
            case 1:
               this.updateTabLabel((JPanel)this.jTB.getTabComponentAt(i), false);
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
}

