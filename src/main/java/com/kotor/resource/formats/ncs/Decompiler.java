// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.stack.Variable;
import java.awt.BorderLayout;
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
import java.io.FileWriter;
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
   private static final String PROJECT_URL = "https://bolabaden.org";
   private static final String GITHUB_URL = "https://github.com/bolabaden";
   private static final String SPONSOR_URL = "https://github.com/sponsors/th3w1zard1";

   static {
      settings.load();
      if (!new File(settings.getProperty("Output Directory")).isDirectory()) {
         settings.setProperty("Output Directory", chooseOutputDirectory());
         settings.save();
      }
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

      this.upperJSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, this.jTB);
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
      helpMenu.add(this.menuItem("Project Website", KeyEvent.VK_F1, false));
      helpMenu.add(this.menuItem("GitHub Repo", KeyEvent.VK_F2, false));
      helpMenu.add(this.menuItem("Sponsor NCSDecomp", KeyEvent.VK_F3, false));
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
      if (((JTabbedPane)arg0.getSource()).getSelectedIndex() >= 0) {
         TreeModel model = this.hash_TabComponent2TreeModel.get((JComponent)((JTabbedPane)arg0.getSource()).getTabComponentAt(this.jTB.getSelectedIndex()));
         this.jTree.setModel(model);
      } else {
         this.jTree.setModel(TreeModelFactory.getEmptyModel());
      }
   }

   @Override
   public void keyPressed(KeyEvent arg0) {
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      if (this.jTB.getSelectedIndex() != -1) {
         if (arg0.getSource() instanceof JTree) {
            TreePath changedPath = ((JTree)arg0.getSource()).getSelectionPath();
            File file = this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()));
            JComponent[] panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()));
            if (changedPath != null && changedPath.getPathCount() == 2) {
               Hashtable<String, Vector<Variable>> func2VarVec = this.fileDecompiler.updateSubName(file, this.currentNodeString, changedPath.getLastPathComponent().toString());
               this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), func2VarVec);
               this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
               this.jTA = (JTextArea)((JScrollPane)((JPanel)panels[0]).getComponent(0)).getViewport().getComponent(0);
               this.jTA.setText(this.fileDecompiler.getGeneratedCode(file));
               this.jTA.setCaretPosition(0);
            } else if (changedPath != null) {
               TreeNode subroutineNode = (TreeNode)changedPath.getParentPath().getLastPathComponent();
               Hashtable<String, Vector<Variable>> func2VarVec = this.hash_TabComponent2Func2VarVec.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()));
               Vector<Variable> variables = func2VarVec.get(subroutineNode.toString());
               int nodeIndex = subroutineNode.getIndex((TreeNode)changedPath.getLastPathComponent());
               Variable changedVar = variables.get(nodeIndex);
               changedVar.name(changedPath.getLastPathComponent().toString());
               this.jTA = (JTextArea)((JScrollPane)((JPanel)panels[0]).getComponent(0)).getViewport().getComponent(0);
               this.jTA
                  .setText(this.fileDecompiler.regenerateCode(this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()))));
               this.jTA.setCaretPosition(0);
            }

            if (changedPath != null) {
               this.currentNodeString = changedPath.getLastPathComponent().toString();
            }
         } else if (arg0.getSource() instanceof JTextArea) {
            unsavedFiles.add(this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())));
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
         this.close(this.jTB.getSelectedIndex());
      } else if (cmd.equals("Close All")) {
         this.closeAll();
      } else if (cmd.equals("Save")) {
         this.save(this.jTB.getSelectedIndex());
      } else if (cmd.equals("Save All")) {
         this.saveAll();
      } else if (cmd.equals("Exit")) {
         exit();
      } else if (cmd.equals("Settings")) {
         settings.show();
      } else if (cmd.equals("Clear")) {
         this.status.setText("");
      } else if (cmd.equals("View Byte Code")) {
         this.setTabComponentPanel(1);
      } else if (cmd.equals("View Decompiled Code")) {
         this.setTabComponentPanel(0);
      } else if (cmd.equals("Link Scroll Bars")) {
         this.toggleLinkScrollBars();
      } else if (cmd.equals("Project Website")) {
         this.openLink(PROJECT_URL, "Opening project website");
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
      if (Boolean.parseBoolean(settings.getProperty("Link Scroll Bars"))) {
         if (this.jTB.getClientProperty(this.jTB.getSelectedComponent()).equals("left")) {
            ((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getRightComponent())
               .getVerticalScrollBar()
               .setValue(((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getLeftComponent()).getVerticalScrollBar().getValue());
         } else {
            ((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getLeftComponent())
               .getVerticalScrollBar()
               .setValue(((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getRightComponent()).getVerticalScrollBar().getValue());
         }
      }
   }

   @Override
   public void caretUpdate(CaretEvent arg0) {
      this.jTA = (JTextArea)arg0.getSource();
      this.mark = this.jTA.getCaretPosition();
      this.rootElement = this.jTA.getDocument().getDefaultRootElement();
      this.titledBorder = (TitledBorder)((JPanel)((JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())))[0])
         .getBorder();
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
      return tabComponents;
   }

   private void decompile(File file) {
      this.status.append("Decompiling...");
      this.statusScrollPane.getVerticalScrollBar().setValue(this.statusScrollPane.getVerticalScrollBar().getMaximum());
      this.status.append(file.getName() + ": ");
      int result = 2;

      try {
         result = this.fileDecompiler.decompile(file);
      } catch (DecompilerException var4) {
         JOptionPane.showMessageDialog(null, var4.getMessage());
      }

      switch (result) {
         case 0:
            this.status.append("failure\n");
            break;
         case 1:
            this.panels = this.newNCSTab(file.getName().substring(0, file.getName().length() - 4));
            ((JTextArea)((JScrollPane)((JPanel)this.panels[0]).getComponent(0)).getViewport().getView()).append(this.fileDecompiler.getGeneratedCode(file));
            this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
            this.origByteCodeJTA.append(this.fileDecompiler.getOriginalByteCode(file));
            this.newByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getRightComponent()).getViewport().getComponent(0);
            this.newByteCodeJTA.append(this.fileDecompiler.getNewByteCode(file));
            if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
               this.jTB.putClientProperty(this.panels[1], "left");
            } else {
               this.jTB.putClientProperty(this.panels[1], "right");
            }

            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(file);
            this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), file);
            this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
            this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
            this.status.append("success\n");
            break;
         case 2:
            this.panels = this.newNCSTab(file.getName().substring(0, file.getName().length() - 4));
            ((JTextArea)((JScrollPane)((JPanel)this.panels[0]).getComponent(0)).getViewport().getView()).append(this.fileDecompiler.getGeneratedCode(file));
            this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
            this.origByteCodeJTA.append(this.fileDecompiler.getOriginalByteCode(file));
            this.jTB.putClientProperty(this.panels[1], "left");
            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(file);
            this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), file);
            this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
            this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
            this.setTabComponentPanel(1);
            this.status.append("partial-could not recompile\n");
            break;
         case 3:
            this.panels = this.newNCSTab(file.getName().substring(0, file.getName().length() - 4));
            ((JTextArea)((JScrollPane)((JPanel)this.panels[0]).getComponent(0)).getViewport().getView()).append(this.fileDecompiler.getGeneratedCode(file));
            this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
            this.origByteCodeJTA.append(this.fileDecompiler.getOriginalByteCode(file));
            this.newByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getRightComponent()).getViewport().getComponent(0);
            this.newByteCodeJTA.append(this.fileDecompiler.getNewByteCode(file));
            if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
               this.jTB.putClientProperty(this.panels[1], "left");
            } else {
               this.jTB.putClientProperty(this.panels[1], "right");
            }

            this.hash_Func2VarVec = this.fileDecompiler.getVariableData(file);
            this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
            this.fileDecompiler.getOriginalByteCode(file);
            this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), file);
            this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
            this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
            this.setTabComponentPanel(1);
            this.status.append("partial-byte code does not match\n");
      }
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
         BufferedWriter bw = new BufferedWriter(new FileWriter(canonicalPath));
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

   private void setTabComponentPanel(int index) {
      this.jTB
         .setComponentAt(
            this.jTB.getSelectedIndex(), ((JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())))[index]
         );
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
      if (this.jTB.getTabCount() == 0) {
         this.getJMenuBar().getMenu(0).getItem(1).setEnabled(true);
         this.getJMenuBar().getMenu(0).getItem(2).setEnabled(true);
         this.getJMenuBar().getMenu(0).getItem(3).setEnabled(true);
         this.getJMenuBar().getMenu(0).getItem(4).setEnabled(true);
      }

      for (int j = 0; j < files.length; j++) {
         if ((this.temp = files[j].getName()).substring(this.temp.length() - 3).equalsIgnoreCase("ncs")) {
            this.decompile(files[j]);
            unsavedFiles.add(this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())));
         }
      }
   }

   private void toggleLinkScrollBars() {
      if (Boolean.parseBoolean(settings.getProperty("Link Scroll Bars"))) {
         settings.setProperty("Link Scroll Bars", "false");
      } else {
         settings.setProperty("Link Scroll Bars", "true");
         ((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getLeftComponent()).getVerticalScrollBar().setValue(0);
         ((JScrollPane)((JSplitPane)this.jTB.getSelectedComponent()).getRightComponent()).getVerticalScrollBar().setValue(0);
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
      if (this.jTB.getSelectedIndex() < 0) {
         this.getJMenuBar().getMenu(0).getItem(1).setEnabled(false);
         this.getJMenuBar().getMenu(0).getItem(2).setEnabled(false);
         this.getJMenuBar().getMenu(0).getItem(3).setEnabled(false);
         this.getJMenuBar().getMenu(0).getItem(4).setEnabled(false);
      }

      this.panel = null;
   }

   private void closeAll() {
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         this.close(i);
      }
   }

   private void save(int index) {
      File newFile = this.saveBuffer(
         (JTextArea)((JScrollPane)((JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())))[0].getComponent(0))
            .getViewport()
            .getView(),
         settings.getProperty("Output Directory")
            + "/"
            + ((JLabel)((JPanel)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex())).getComponent(0)).getText()
            + ".nss"
      );
      this.file = this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()));
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
               this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
               this.origByteCodeJTA.setText(this.fileDecompiler.getOriginalByteCode(this.file));
               this.newByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getRightComponent()).getViewport().getComponent(0);
               this.newByteCodeJTA.setText(this.fileDecompiler.getNewByteCode(this.file));
               if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
                  this.jTB.putClientProperty(this.panels[1], "left");
               } else {
                  this.jTB.putClientProperty(this.panels[1], "right");
               }

               this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
               this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
               this.fileDecompiler.getOriginalByteCode(this.file);
               this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.file);
               this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
               this.status.append("success\n");
               break;
            case 2:
               this.panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(index));
               this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
               this.origByteCodeJTA.setText(this.fileDecompiler.getOriginalByteCode(this.file));
               this.jTB.putClientProperty(this.panels[1], "left");
               this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
               this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
               this.fileDecompiler.getOriginalByteCode(this.file);
               this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.file);
               this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
               this.setTabComponentPanel(1);
               this.status.append("partial-could not recompile\n");
               break;
            case 3:
               this.panels = (JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(index));
               this.origByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getLeftComponent()).getViewport().getComponent(0);
               this.origByteCodeJTA.setText(this.fileDecompiler.getOriginalByteCode(this.file));
               this.newByteCodeJTA = (JTextArea)((JScrollPane)((JSplitPane)this.panels[1]).getRightComponent()).getViewport().getComponent(0);
               this.newByteCodeJTA.setText(this.fileDecompiler.getNewByteCode(this.file));
               if (this.origByteCodeJTA.getLineCount() >= this.newByteCodeJTA.getLineCount()) {
                  this.jTB.putClientProperty(this.panels[1], "left");
               } else {
                  this.jTB.putClientProperty(this.panels[1], "right");
               }

               this.hash_Func2VarVec = this.fileDecompiler.getVariableData(this.file);
               this.jTree.setModel(TreeModelFactory.createTreeModel(this.hash_Func2VarVec));
               this.fileDecompiler.getOriginalByteCode(this.file);
               this.hash_TabComponent2File.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.file);
               this.hash_TabComponent2Func2VarVec.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.hash_Func2VarVec);
               this.hash_TabComponent2TreeModel.put((JComponent)this.jTB.getTabComponentAt(this.jTB.getSelectedIndex()), this.jTree.getModel());
               this.setTabComponentPanel(1);
               this.status.append("partial-byte code does not match\n");
         }

         unsavedFiles.remove(this.file);
         newFile = null;
      }
   }

   private void saveAll() {
      for (int i = 0; i < this.jTB.getTabCount(); i++) {
         File newFile = this.saveBuffer(
            (JTextArea)((JScrollPane)((JComponent[])this.jTB.getClientProperty((JComponent)this.jTB.getTabComponentAt(i)))[0].getComponent(0)).getViewport().getView(),
            settings.getProperty("Output Directory") + "/" + ((JLabel)((JPanel)this.jTB.getTabComponentAt(i)).getComponent(0)).getText() + ".nss"
         );
         File file = this.hash_TabComponent2File.get((JComponent)this.jTB.getTabComponentAt(i));
         int result = 2;

         try {
            result = this.fileDecompiler.compileAndCompare(file, newFile);
         } catch (DecompilerException var6) {
            JOptionPane.showMessageDialog(null, var6.getMessage());
            newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
         }

         switch (result) {
            case 0:
               newFile.renameTo(new File(this.getShortName(newFile) + "_failed.nss"));
               break;
            case 1:
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
   }

   private String getShortName(File in) {
      int i = in.getAbsolutePath().lastIndexOf(46);
      return i == -1 ? in.getAbsolutePath() : in.getAbsolutePath().substring(0, i);
   }
}

