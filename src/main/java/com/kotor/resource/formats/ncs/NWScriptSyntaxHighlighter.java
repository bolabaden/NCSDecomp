// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import javax.swing.JTextPane;
import javax.swing.text.*;
import java.awt.Color;
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
   private static final Pattern PATTERN_COMMENT_SINGLE = Pattern.compile("//.*$", Pattern.MULTILINE);
   private static final Pattern PATTERN_COMMENT_MULTI = Pattern.compile("/\\*[\\s\\S]*?\\*/", Pattern.MULTILINE);
   private static final Pattern PATTERN_STRING_DOUBLE = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
   private static final Pattern PATTERN_STRING_SINGLE = Pattern.compile("'([^'\\\\]|\\\\.)*'");
   private static final Pattern PATTERN_NUMBER = Pattern.compile("\\b\\d+\\.?\\d*[fF]?\\b");
   private static final Pattern PATTERN_KEYWORD = Pattern.compile(KEYWORD_PATTERN);
   private static final Pattern PATTERN_TYPE = Pattern.compile(TYPE_PATTERN);
   private static final Pattern PATTERN_FUNCTION = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

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

      // 1. Multi-line comments (highest priority)
      applyPattern(doc, PATTERN_COMMENT_MULTI, commentStyle, styled, text);

      // 2. Single-line comments
      applyPattern(doc, PATTERN_COMMENT_SINGLE, commentStyle, styled, text);

      // 3. Double-quoted strings
      applyPattern(doc, PATTERN_STRING_DOUBLE, stringStyle, styled, text);

      // 4. Single-quoted strings
      applyPattern(doc, PATTERN_STRING_SINGLE, stringStyle, styled, text);

      // 5. Numbers (only if not already styled)
      applyPattern(doc, PATTERN_NUMBER, numberStyle, styled, text);

      // 6. Keywords (only if not already styled)
      applyPattern(doc, PATTERN_KEYWORD, keywordStyle, styled, text);

      // 7. Types (only if not already styled)
      applyPattern(doc, PATTERN_TYPE, typeStyle, styled, text);

      // 8. Function calls (only if not already styled)
      applyFunctionPattern(doc, PATTERN_FUNCTION, functionStyle, styled, text);
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

   /**
    * Creates a DocumentListener that re-applies highlighting when the document changes.
    */
   public static javax.swing.event.DocumentListener createHighlightingListener(JTextPane textPane) {
      return new javax.swing.event.DocumentListener() {
         @Override
         public void insertUpdate(javax.swing.event.DocumentEvent e) {
            applyHighlighting(textPane);
         }

         @Override
         public void removeUpdate(javax.swing.event.DocumentEvent e) {
            applyHighlighting(textPane);
         }

         @Override
         public void changedUpdate(javax.swing.event.DocumentEvent e) {
            applyHighlighting(textPane);
         }
      };
   }
}

