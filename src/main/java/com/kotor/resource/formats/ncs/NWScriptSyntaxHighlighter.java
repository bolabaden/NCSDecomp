// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighter for NWScript code.
 * Provides comprehensive syntax highlighting for NWScript language features including:
 * - Keywords (int, void, float, string, object, etc.)
 * - Control flow statements (if, else, while, for, switch, case, break, return, etc.)
 * - Comments (single-line and multi-line)
 * - Strings (single and double quoted)
 * - Numbers (integers and floats)
 * - Operators
 * - Function calls
 */
public class NWScriptSyntaxHighlighter {

   // Color scheme
   private static final Color KEYWORD_COLOR = new Color(0, 0, 255); // Blue
   private static final Color TYPE_COLOR = new Color(128, 0, 128); // Purple
   private static final Color STRING_COLOR = new Color(0, 128, 0); // Green
   private static final Color COMMENT_COLOR = new Color(128, 128, 128); // Gray
   private static final Color NUMBER_COLOR = new Color(255, 0, 0); // Red
   private static final Color FUNCTION_COLOR = new Color(0, 128, 128); // Teal

   // NWScript keywords
   private static final String[] KEYWORDS = {
      "if", "else", "while", "for", "do", "switch", "case", "default", "break", "continue",
      "return", "void", "int", "float", "string", "object", "vector", "location", "effect",
      "event", "talent", "action", "const", "struct"
   };

   // NWScript types
   private static final String[] TYPES = {
      "int", "float", "string", "object", "vector", "location", "effect", "event", "talent",
      "action", "void"
   };

   // Build keyword pattern
   private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";

   // Build type pattern
   private static final String TYPE_PATTERN = "\\b(" + String.join("|", TYPES) + ")\\b";

   // Patterns for different syntax elements
   // Fixed patterns to prevent catastrophic backtracking:
   // - Multi-line comments: use possessive quantifiers and negated character classes
   // - Strings: use possessive quantifiers to prevent excessive backtracking
   private static final Pattern PATTERN_COMMENT_SINGLE = Pattern.compile("//.*$", Pattern.MULTILINE);
   private static final Pattern PATTERN_COMMENT_MULTI = Pattern.compile("/\\*(?:[^*]|\\*(?!/))*+\\*/", Pattern.MULTILINE);
   private static final Pattern PATTERN_STRING_DOUBLE = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*+\"");
   private static final Pattern PATTERN_STRING_SINGLE = Pattern.compile("'(?:[^'\\\\]|\\\\.)*+'");
   private static final Pattern PATTERN_NUMBER = Pattern.compile("\\b\\d+\\.?\\d*[fF]?\\b");
   private static final Pattern PATTERN_KEYWORD = Pattern.compile(KEYWORD_PATTERN);
   private static final Pattern PATTERN_TYPE = Pattern.compile(TYPE_PATTERN);
   private static final Pattern PATTERN_FUNCTION = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*+\\(");

   // Maximum text size for highlighting (500KB - prevents regex catastrophic backtracking on huge files)
   private static final int MAX_HIGHLIGHT_SIZE = 500000;
   
   /**
    * Applies syntax highlighting to a JTextPane.
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
      
      // Skip highlighting for very large files to prevent regex catastrophic backtracking
      if (text.length() > MAX_HIGHLIGHT_SIZE) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: File too large for highlighting (" + text.length() + " chars), skipping");
         return;
      }
      
      // Wrap entire highlighting in try-catch to prevent crashes
      try {
         applyHighlightingInternal(textPane, doc, text);
      } catch (Exception | StackOverflowError e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Error during highlighting (likely regex catastrophic backtracking): " + e.getClass().getName());
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Text length: " + text.length() + " chars");
         // Don't rethrow - just skip highlighting for this file
      }
   }
   
   /**
    * Internal highlighting implementation - can throw exceptions.
    */
   private static void applyHighlightingInternal(JTextPane textPane, StyledDocument doc, String text) {

      // Remove all existing styles
      Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
      doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

      // Create styles
      Style keywordStyle = doc.addStyle("keyword", null);
      StyleConstants.setForeground(keywordStyle, KEYWORD_COLOR);
      StyleConstants.setBold(keywordStyle, true);

      Style typeStyle = doc.addStyle("type", null);
      StyleConstants.setForeground(typeStyle, TYPE_COLOR);
      StyleConstants.setBold(typeStyle, true);

      Style stringStyle = doc.addStyle("string", null);
      StyleConstants.setForeground(stringStyle, STRING_COLOR);

      Style commentStyle = doc.addStyle("comment", null);
      StyleConstants.setForeground(commentStyle, COMMENT_COLOR);
      StyleConstants.setItalic(commentStyle, true);

      Style numberStyle = doc.addStyle("number", null);
      StyleConstants.setForeground(numberStyle, NUMBER_COLOR);

      Style functionStyle = doc.addStyle("function", null);
      StyleConstants.setForeground(functionStyle, FUNCTION_COLOR);

      // Track which characters have been styled to avoid overlapping
      boolean[] styled = new boolean[text.length()];

      // Apply highlighting in order of priority (comments and strings first, then keywords)
      // Each pattern wrapped in try-catch to isolate failures

      try {
         // 1. Multi-line comments (highest priority)
         applyPattern(doc, PATTERN_COMMENT_MULTI, commentStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight multi-line comments: " + e.getMessage());
      }

      try {
         // 2. Single-line comments
         applyPattern(doc, PATTERN_COMMENT_SINGLE, commentStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight single-line comments: " + e.getMessage());
      }

      try {
         // 3. Double-quoted strings
         applyPattern(doc, PATTERN_STRING_DOUBLE, stringStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight double-quoted strings: " + e.getMessage());
      }

      try {
         // 4. Single-quoted strings
         applyPattern(doc, PATTERN_STRING_SINGLE, stringStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight single-quoted strings: " + e.getMessage());
      }

      try {
         // 5. Numbers (only if not already styled)
         applyPattern(doc, PATTERN_NUMBER, numberStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight numbers: " + e.getMessage());
      }

      try {
         // 6. Keywords (only if not already styled)
         applyPattern(doc, PATTERN_KEYWORD, keywordStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight keywords: " + e.getMessage());
      }

      try {
         // 7. Types (only if not already styled)
         applyPattern(doc, PATTERN_TYPE, typeStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight types: " + e.getMessage());
      }

      try {
         // 8. Function calls (only if not already styled)
         applyFunctionPattern(doc, PATTERN_FUNCTION, functionStyle, styled, text);
      } catch (Exception e) {
         System.err.println("DEBUG NWScriptSyntaxHighlighter: Failed to highlight function calls: " + e.getMessage());
      }
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
            doc.setCharacterAttributes(start, end - start, style, false);
            // Mark as styled
            for (int i = start; i < end && i < styled.length; i++) {
               styled[i] = true;
            }
         }
      }
   }

   /**
    * Applies function pattern highlighting (function name only, not the parentheses).
    */
   private static void applyFunctionPattern(StyledDocument doc, Pattern pattern, Style style,
                                            boolean[] styled, String text) {
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
         int start = matcher.start(1); // Group 1 is the function name
         int end = matcher.end(1);

         // Check if already styled
         boolean alreadyStyled = false;
         for (int i = start; i < end; i++) {
            if (i < styled.length && styled[i]) {
               alreadyStyled = true;
               break;
            }
         }

         // Don't highlight if it's a keyword
         if (!alreadyStyled) {
            String funcName = text.substring(start, end);
            boolean isKeyword = false;
            for (String keyword : KEYWORDS) {
               if (keyword.equals(funcName)) {
                  isKeyword = true;
                  break;
               }
            }

            if (!isKeyword) {
               doc.setCharacterAttributes(start, end - start, style, false);
               // Mark as styled
               for (int i = start; i < end && i < styled.length; i++) {
                  styled[i] = true;
               }
            }
         }
      }
   }

   // Debounce delay in milliseconds (300ms = highlight after user stops typing for 300ms)
   private static final int HIGHLIGHT_DELAY_MS = 300;

   // Client property key to track if we're doing a programmatic update
   private static final String PROP_SKIP_HIGHLIGHTING = "NWScriptSyntaxHighlighter.skipHighlighting";

   /**
    * Creates a DocumentListener that re-applies highlighting when the document changes.
    * Uses debouncing to avoid excessive highlighting operations that could freeze the UI.
    * The highlighting is deferred and only runs after the user stops making changes.
    */
   public static javax.swing.event.DocumentListener createHighlightingListener(JTextPane textPane) {
      // Use a Timer for debouncing - restart timer on each change
      Timer[] debounceTimer = new Timer[1];

      ActionListener highlightAction = new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Only highlight if the text pane is still valid and visible, and not in programmatic update
            if (textPane != null && textPane.isDisplayable()) {
               Boolean skipHighlighting = (Boolean)textPane.getClientProperty(PROP_SKIP_HIGHLIGHTING);
               if (skipHighlighting == null || !skipHighlighting) {
                  SwingUtilities.invokeLater(() -> {
                     try {
                        applyHighlighting(textPane);
                     } catch (Exception ex) {
                        // Silently ignore errors during highlighting to prevent UI freeze
                        System.err.println("DEBUG NWScriptSyntaxHighlighter: Error during highlighting: " + ex.getMessage());
                     }
                  });
               }
            }
         }
      };

      return new javax.swing.event.DocumentListener() {
         @Override
         public void insertUpdate(javax.swing.event.DocumentEvent e) {
            scheduleHighlighting();
         }

         @Override
         public void removeUpdate(javax.swing.event.DocumentEvent e) {
            scheduleHighlighting();
         }

         @Override
         public void changedUpdate(javax.swing.event.DocumentEvent e) {
            scheduleHighlighting();
         }

         private void scheduleHighlighting() {
            // Skip if we're in a programmatic update
            Boolean skipHighlighting = (Boolean)textPane.getClientProperty(PROP_SKIP_HIGHLIGHTING);
            if (skipHighlighting != null && skipHighlighting) {
               return;
            }

            // Cancel existing timer if any
            if (debounceTimer[0] != null) {
               debounceTimer[0].stop();
            }

            // Create new timer that will fire after delay
            debounceTimer[0] = new Timer(HIGHLIGHT_DELAY_MS, highlightAction);
            debounceTimer[0].setRepeats(false);
            debounceTimer[0].start();
         }
      };
   }

   /**
    * Applies highlighting immediately without debouncing.
    * Use this for programmatic updates like setText().
    */
   public static void applyHighlightingImmediate(JTextPane textPane) {
      if (textPane != null && textPane.isDisplayable()) {
         SwingUtilities.invokeLater(() -> {
            try {
               applyHighlighting(textPane);
            } catch (Exception ex) {
               // Silently ignore errors during highlighting to prevent UI freeze
               System.err.println("DEBUG NWScriptSyntaxHighlighter: Error during immediate highlighting: " + ex.getMessage());
            }
         });
      }
   }

   /**
    * Sets a flag to temporarily disable highlighting during programmatic updates.
    * Call this before setText() and clear it after.
    */
   public static void setSkipHighlighting(JTextPane textPane, boolean skip) {
      if (textPane != null) {
         textPane.putClientProperty(PROP_SKIP_HIGHLIGHTING, skip ? Boolean.TRUE : null);
      }
   }
}

