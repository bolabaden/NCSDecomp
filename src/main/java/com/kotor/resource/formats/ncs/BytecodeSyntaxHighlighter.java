// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighter for NCS bytecode output.
 * Provides syntax highlighting for bytecode format including:
 * - Instruction opcodes (JSR, RETN, CONSTI, CPDOWNSP, MOVSP, etc.)
 * - Hexadecimal addresses and values
 * - Function names (fn_*)
 * - Type indicators (T, I, F, S, O, etc.)
 */
public class BytecodeSyntaxHighlighter {

   // Color scheme
   private static final Color INSTRUCTION_COLOR = new Color(0, 0, 255); // Blue
   private static final Color ADDRESS_COLOR = new Color(128, 128, 128); // Gray
   private static final Color HEX_VALUE_COLOR = new Color(0, 128, 0); // Green
   private static final Color FUNCTION_COLOR = new Color(128, 0, 128); // Purple
   private static final Color TYPE_COLOR = new Color(255, 140, 0); // Orange

   // Bytecode instruction patterns
   private static final String[] INSTRUCTIONS = {
      "CPDOWNSP", "RSADD", "RSADDI", "CPTOPSP", "CONST", "CONSTI", "CONSTF", "CONSTS", "ACTION",
      "LOGANDII", "LOGORII", "INCORII", "EXCORII", "BOOLANDII",
      "EQUAL", "NEQUAL", "GEQ", "GT", "LT", "LEQ",
      "SHLEFTII", "SHRIGHTII", "USHRIGHTII",
      "ADD", "SUB", "MUL", "DIV", "MOD", "NEG", "COMP",
      "MOVSP", "STATEALL", "JMP", "JSR", "JZ", "JNZ", "RETN", "DESTRUCT",
      "NOT", "DECISP", "INCISP", "CPDOWNBP", "CPTOPBP", "DECIBP", "INCIBP",
      "SAVEBP", "RESTOREBP", "STORE_STATE", "NOP", "T"
   };

   // Build instruction pattern
   private static final String INSTRUCTION_PATTERN = "\\b(" + String.join("|", INSTRUCTIONS) + ")\\b";

   // Patterns for different bytecode elements
   private static final Pattern PATTERN_INSTRUCTION = Pattern.compile(INSTRUCTION_PATTERN);
   private static final Pattern PATTERN_ADDRESS = Pattern.compile("\\b[0-9A-Fa-f]{8}\\b");
   private static final Pattern PATTERN_HEX_VALUE = Pattern.compile("\\b[0-9A-Fa-f]{4,}\\b");
   private static final Pattern PATTERN_FUNCTION = Pattern.compile("\\bfn_[0-9A-Fa-f]+\\b");
   private static final Pattern PATTERN_TYPE = Pattern.compile("\\bT\\s+[0-9A-Fa-f]+\\b");

   /**
    * Applies syntax highlighting to a JTextPane displaying bytecode.
    *
    * @param textPane The JTextPane to apply highlighting to
    */
   public static void applyHighlighting(JTextPane textPane) {
      StyledDocument doc = textPane.getStyledDocument();
      String text;

      try {
         text = doc.getText(0, doc.getLength());
      } catch (BadLocationException e) {
         return;
      }

      // Remove all existing styles
      Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
      doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

      // Create styles
      Style instructionStyle = doc.addStyle("instruction", null);
      StyleConstants.setForeground(instructionStyle, INSTRUCTION_COLOR);
      StyleConstants.setBold(instructionStyle, true);

      Style addressStyle = doc.addStyle("address", null);
      StyleConstants.setForeground(addressStyle, ADDRESS_COLOR);

      Style hexValueStyle = doc.addStyle("hexValue", null);
      StyleConstants.setForeground(hexValueStyle, HEX_VALUE_COLOR);

      Style functionStyle = doc.addStyle("function", null);
      StyleConstants.setForeground(functionStyle, FUNCTION_COLOR);
      StyleConstants.setBold(functionStyle, true);

      Style typeStyle = doc.addStyle("type", null);
      StyleConstants.setForeground(typeStyle, TYPE_COLOR);
      StyleConstants.setBold(typeStyle, true);

      // Track which characters have been styled to avoid overlapping
      boolean[] styled = new boolean[text.length()];

      // Apply highlighting in order of priority

      // 1. Functions (highest priority - most specific)
      applyPattern(doc, PATTERN_FUNCTION, functionStyle, styled, text);

      // 2. Type indicators (T followed by hex)
      applyPattern(doc, PATTERN_TYPE, typeStyle, styled, text);

      // 3. Instructions
      applyPattern(doc, PATTERN_INSTRUCTION, instructionStyle, styled, text);

      // 4. Addresses (8 hex digits)
      applyPattern(doc, PATTERN_ADDRESS, addressStyle, styled, text);

      // 5. Other hex values (4+ hex digits, but not already styled as addresses)
      applyPattern(doc, PATTERN_HEX_VALUE, hexValueStyle, styled, text);
   }

   /**
    * Applies a pattern to the document with style, avoiding already styled regions.
    */
   private static void applyPattern(StyledDocument doc, Pattern pattern, Style style,
                                    boolean[] styled, String text) {
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
         int start = matcher.start();
         int end = matcher.end();

         // Check if any part is already styled
         boolean alreadyStyled = false;
         for (int i = start; i < end; i++) {
            if (i < styled.length && styled[i]) {
               alreadyStyled = true;
               break;
            }
         }

         if (!alreadyStyled) {
            try {
               doc.setCharacterAttributes(start, end - start, style, false);
               // Mark as styled
               for (int i = start; i < end && i < styled.length; i++) {
                  styled[i] = true;
               }
            } catch (Exception e) {
               // Ignore errors for individual pattern matches
            }
         }
      }
   }

   /**
    * Applies highlighting immediately without debouncing.
    * Use this for programmatic updates like setText().
    * Applies synchronously if on EDT, otherwise defers to EDT.
    */
   public static void applyHighlightingImmediate(JTextPane textPane) {
      if (textPane == null || !textPane.isDisplayable()) {
         return;
      }

      // If we're already on the EDT, apply immediately
      if (SwingUtilities.isEventDispatchThread()) {
         try {
            applyHighlighting(textPane);
         } catch (Exception ex) {
            System.err.println("DEBUG BytecodeSyntaxHighlighter: Error during highlighting: " + ex.getMessage());
            ex.printStackTrace();
         }
      } else {
         // Otherwise defer to EDT
         SwingUtilities.invokeLater(() -> {
            try {
               applyHighlighting(textPane);
            } catch (Exception ex) {
               System.err.println("DEBUG BytecodeSyntaxHighlighter: Error during highlighting: " + ex.getMessage());
               ex.printStackTrace();
            }
         });
      }
   }
}

