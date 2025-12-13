// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

/**
 * Centralized logging utility with color support and section separation.
 * <p>
 * Provides structured logging with:
 * <ul>
 *   <li>ANSI color codes for terminal output</li>
 *   <li>Section separation with visual dividers</li>
 *   <li>Indented compiler output</li>
 *   <li>Consistent formatting across all log types</li>
 * </ul>
 */
public final class Logger {
   private Logger() {
   }

   // ANSI color codes
   private static final String RESET = "\033[0m";
   private static final String BOLD = "\033[1m";
   private static final String DIM = "\033[2m";

   // Colors
   private static final String RED = "\033[31m";
   private static final String GREEN = "\033[32m";
   private static final String YELLOW = "\033[33m";
   private static final String BLUE = "\033[34m";
   private static final String MAGENTA = "\033[35m";
   private static final String CYAN = "\033[36m";
   private static final String WHITE = "\033[37m";

   // Background colors
   private static final String BG_BLUE = "\033[44m";
   private static final String BG_CYAN = "\033[46m";
   private static final String BG_YELLOW = "\033[43m";
   private static final String BG_RED = "\033[41m";

   // Check if colors are supported (Windows 10+ or Unix-like)
   private static final boolean COLORS_ENABLED = isColorSupported();

   private static boolean isColorSupported() {
      // Check if we're in a terminal that supports colors
      String term = System.getenv("TERM");
      String os = System.getProperty("os.name", "").toLowerCase();

      // Windows 10+ supports ANSI colors in modern terminals
      if (os.contains("windows")) {
         // Check for Windows 10+ (version 10.0+)
         String version = System.getProperty("os.version", "");
         try {
            double ver = Double.parseDouble(version);
            if (ver >= 10.0) {
               return true; // Windows 10+ supports ANSI
            }
         } catch (NumberFormatException e) {
            // Fall through to term check
         }
      }

      // Unix-like systems typically support colors
      return term != null && !term.equals("dumb") && !term.equals("unknown");
   }

   /**
    * Formats a string with color if colors are enabled.
    */
   private static String colorize(String text, String color) {
      if (COLORS_ENABLED) {
         return color + text + RESET;
      }
      return text;
   }

   /**
    * Formats a string with bold if colors are enabled.
    */
   private static String bold(String text) {
      if (COLORS_ENABLED) {
         return BOLD + text + RESET;
      }
      return text;
   }

   /**
    * Formats a string with dim if colors are enabled.
    */
   private static String dim(String text) {
      if (COLORS_ENABLED) {
         return DIM + text + RESET;
      }
      return text;
   }

   /**
    * Prints a section divider.
    */
   private static void printDivider(char ch, String color) {
      String line = "═══════════════════════════════════════════════════════════════════════════════";
      System.err.println(colorize(line, color));
   }

   /**
    * Prints a section header with background color.
    */
   private static void printSectionHeader(String title, String bgColor, String fgColor) {
      String padding = "═══════════════════════════════════════════════════════════════════════════════";
      int titleLen = title.length();
      int paddingLen = (padding.length() - titleLen - 4) / 2;
      String leftPad = padding.substring(0, Math.max(0, paddingLen));
      String rightPad = padding.substring(0, Math.max(0, padding.length() - leftPad.length() - titleLen - 4));

      if (COLORS_ENABLED) {
         System.err.println(bgColor + fgColor + " " + leftPad + " " + bold(title) + " " + rightPad + " " + RESET);
      } else {
         System.err.println("═══ " + title + " ═══");
      }
   }

   /**
    * Logs a DEBUG message (control flow, state transitions, etc.).
    */
   public static void debug(String message) {
      System.err.println(colorize("DEBUG", CYAN) + " " + dim(message));
   }

   /**
    * Logs an INFO message.
    */
   public static void info(String message) {
      System.err.println(colorize("INFO", BLUE) + "  " + message);
   }

   /**
    * Logs a WARNING message.
    */
   public static void warn(String message) {
      System.err.println(colorize("WARN", YELLOW) + " " + message);
   }

   /**
    * Logs an ERROR message.
    */
   public static void error(String message) {
      System.err.println(colorize("ERROR", RED) + " " + bold(message));
   }

   /**
    * Logs a SUCCESS message.
    */
   public static void success(String message) {
      System.err.println(colorize("SUCCESS", GREEN) + " " + message);
   }

   /**
    * Logs a NCSDecomp operation message.
    */
   public static void ncsdecomp(String message) {
      System.err.println(colorize("▶", GREEN) + " " + colorize("[NCSDecomp]", BOLD + CYAN) + " " + message);
   }

   /**
    * Logs a nwnnsscomp.exe output line (indented).
    */
   public static void compiler(String line) {
      System.err.println("  " + colorize("│", DIM + CYAN) + " " + dim(line));
   }

   /**
    * Starts a new section for control flow debugging.
    */
   public static void startControlFlowSection() {
      printDivider('═', CYAN);
      printSectionHeader("CONTROL FLOW DEBUG", BG_CYAN, WHITE);
      printDivider('═', CYAN);
   }

   /**
    * Starts a new section for compiler output.
    */
   public static void startCompilerSection() {
      printDivider('═', YELLOW);
      printSectionHeader("COMPILER OUTPUT", BG_YELLOW, WHITE);
      printDivider('═', YELLOW);
   }

   /**
    * Starts a new section for NCSDecomp operations.
    */
   public static void startNCSDecompSection() {
      printDivider('═', GREEN);
      printSectionHeader("NCSDECOMP OPERATIONS", BG_BLUE, WHITE);
      printDivider('═', GREEN);
   }

   /**
    * Starts a new section for errors.
    */
   public static void startErrorSection() {
      printDivider('═', RED);
      printSectionHeader("ERRORS", BG_RED, WHITE);
      printDivider('═', RED);
   }

   /**
    * Ends a section with a divider.
    */
   public static void endSection() {
      System.err.println();
   }

   /**
    * Logs a multi-line compiler output block.
    */
   public static void compilerOutput(String output) {
      if (output == null || output.trim().isEmpty()) {
         return;
      }

      String[] lines = output.split("\n");
      for (String line : lines) {
         compiler(line);
      }
   }
}

