// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Persisted user preferences for the DeNCS GUI.
 * <p>
 * Stores directories and offers a small Swing dialog for editing the output
 * path. Backed by a simple {@code dencs.conf} properties file in the working
 * directory.
 */
public class Settings extends Properties implements ActionListener {
   private static final long serialVersionUID = 1L;
   private JFrame frame;
   private JButton save;
   private JButton cancel;
   private JButton browseOutputDir;
   private JLabel Output_Directory_JLabel;
   private JTextField Output_Directory_JTextField;

   public static void main(String[] args) {
   }

   @Override
   public void actionPerformed(ActionEvent arg0) {
      JButton src = (JButton)arg0.getSource();
      if (src == this.save) {
         this.setProperty(this.Output_Directory_JLabel.getText(), this.Output_Directory_JTextField.getText());
         this.save();
         this.frame.dispose();
      } else if (src == this.cancel) {
         this.frame.dispose();
      } else if (src == this.browseOutputDir) {
         this.Output_Directory_JTextField.setText(Decompiler.chooseOutputDirectory());
      }
   }

   /**
    * Loads preferences from disk, creating a default config if none exists.
    */
   public void load() {
      try {
         try (FileInputStream fis = new FileInputStream("dencs.conf")) {
            this.load(fis);
         }
      } catch (Exception var4) {
         try {
            new File("dencs.conf").createNewFile();
         } catch (FileNotFoundException var2) {
            var2.printStackTrace();
            System.exit(1);
         } catch (IOException var3) {
            var3.printStackTrace();
            System.exit(1);
         }

         this.reset();
         this.setProperty("Output Directory", "");
         this.save();
      }
   }

   /**
    * Writes preferences to {@code dencs.conf}.
    */
   public void save() {
      try {
         FileOutputStream fos = new FileOutputStream("dencs.conf");
         this.store(fos, null);
      } catch (FileNotFoundException var2) {
         var2.printStackTrace();
      } catch (IOException var3) {
         var3.printStackTrace();
      }
   }

   /**
    * Resets directory preferences to the current working directory.
    */
   public void reset() {
      this.setProperty("Output Directory", System.getProperty("user.dir"));
      this.setProperty("Open Directory", System.getProperty("user.dir"));
   }

   /**
    * Opens the Swing dialog that allows editing the output directory.
    */
   public void show() {
      this.frame = new JFrame();
      this.frame.setDefaultCloseOperation(2);
      this.frame.getContentPane().setLayout(new GridLayout(2, 3));
      this.frame.getContentPane().add(this.Output_Directory_JLabel = new JLabel("Output Directory"));
      this.frame.getContentPane().add(this.Output_Directory_JTextField = new JTextField(Decompiler.settings.getProperty("Output Directory")));
      this.frame.getContentPane().add(this.browseOutputDir = new JButton("Browse"));
      this.frame.getContentPane().add(this.save = new JButton("Save"));
      this.frame.getContentPane().add(this.cancel = new JButton("Cancel"));
      this.browseOutputDir.addActionListener(this);
      this.save.addActionListener(this);
      this.cancel.addActionListener(this);
      this.frame.pack();
      this.frame
         .setLocation((int)(Decompiler.screenWidth / 2.0 - this.frame.getWidth() / 2), (int)(Decompiler.screenHeight / 2.0 - this.frame.getHeight() / 2));
      this.frame.setVisible(true);
   }
}

