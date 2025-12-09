// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exhaustive round-trip tests:
 *  1) Use nwnnsscomp.exe to compile each Vanilla NSS -> NCS (per game).
+ *  2) Use NCSDecompCLI (headless) to decompile NCS -> NSS.
 *  3) Compare normalized text with the original NSS.
 *
 * The suite dynamically generates a test per source file so failures identify the
 * exact script that diverges.
 */
public class NCSDecompCLIRoundTripTest {

   private static final Path REPO_ROOT = Paths.get(".").toAbsolutePath().normalize().getParent();
   private static final Path VANILLA_ROOT = REPO_ROOT.resolve("vendor/Vanilla_KOTOR_Script_Source");
   private static final Path K1_ROOT = VANILLA_ROOT.resolve("K1");
   private static final Path TSL_VANILLA_ROOT = VANILLA_ROOT.resolve("TSL/Vanilla");
   private static final Path TSL_TSLRCM_ROOT = VANILLA_ROOT.resolve("TSL/TSLRCM");

   private static final Path NWN_COMPILER = REPO_ROOT.resolve("nwnnsscomp.exe");
   private static final Path K1_NWSCRIPT = REPO_ROOT.resolve("k1_nwscript.nss");
   private static final Path K2_NWSCRIPT = REPO_ROOT.resolve("tsl_nwscript.nss");

   private static final Path WORK_ROOT = REPO_ROOT.resolve("build/cli-roundtrip");
   private static final Duration PROC_TIMEOUT = Duration.ofSeconds(25);

   private static Path k1Scratch;
   private static Path k2Scratch;

   static void preflight() throws IOException {
      if (!Files.isRegularFile(NWN_COMPILER)) {
         throw new IOException("nwnnsscomp.exe missing");
      }
      if (!Files.isRegularFile(K1_NWSCRIPT)) {
         throw new IOException("k1_nwscript.nss missing");
      }
      if (!Files.isRegularFile(K2_NWSCRIPT)) {
         throw new IOException("tsl_nwscript.nss missing");
      }

      k1Scratch = prepareScratch("k1", K1_NWSCRIPT);
      k2Scratch = prepareScratch("k2", K2_NWSCRIPT);

      // Copy nwscript files to current working directory once for FileDecompiler
      // FileDecompiler uses System.getProperty("user.dir") which is the NCSDecomp directory
      Path cwd = Paths.get(System.getProperty("user.dir"));
      Path k1Nwscript = cwd.resolve("k1_nwscript.nss");
      Path k2Nwscript = cwd.resolve("tsl_nwscript.nss");

      if (!Files.exists(k1Nwscript) || !Files.isSameFile(K1_NWSCRIPT, k1Nwscript)) {
         Files.copy(K1_NWSCRIPT, k1Nwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      if (!Files.exists(k2Nwscript) || !Files.isSameFile(K2_NWSCRIPT, k2Nwscript)) {
         Files.copy(K2_NWSCRIPT, k2Nwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
   }

   List<RoundTripCase> buildRoundTripCases() throws IOException {
      // Collect all files from both games
      List<TestItem> allFiles = new ArrayList<>();

      // K1 files
      Files.walk(K1_ROOT)
         .filter(p -> p.toString().toLowerCase().endsWith(".nss"))
         .forEach(p -> allFiles.add(new TestItem(p, "k1", k1Scratch)));

      // TSL files
      Files.walk(TSL_VANILLA_ROOT)
         .filter(p -> p.toString().toLowerCase().endsWith(".nss"))
         .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));

      Files.walk(TSL_TSLRCM_ROOT)
         .filter(p -> p.toString().toLowerCase().endsWith(".nss"))
         .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));

      // Randomly shuffle
      Collections.shuffle(allFiles);

      // Test all files - skip the expensive canCompile check and let the test itself handle failures
      // This is much faster as we don't compile every file twice
      List<RoundTripCase> tests = new ArrayList<>();
      for (TestItem item : allFiles) {
         String displayName = item.gameFlag.equals("k1")
            ? "k1: " + K1_ROOT.relativize(item.path)
            : "tsl: " + VANILLA_ROOT.relativize(item.path);
         tests.add(new RoundTripCase(displayName, item));
      }

      return tests;
   }

   @SuppressWarnings("unused")
   private static boolean canCompile(Path nssPath, Path compiledOut, String gameFlag, Path workDir) {
      try {
         runCompiler(nssPath, compiledOut, gameFlag, workDir);
         return Files.isRegularFile(compiledOut);
      } catch (Exception e) {
         return false;
      }
   }

   private static class TestItem {
      final Path path;
      final String gameFlag;
      final Path scratchRoot;

      TestItem(Path path, String gameFlag, Path scratchRoot) {
         this.path = path;
         this.gameFlag = gameFlag;
         this.scratchRoot = scratchRoot;
      }
   }

   private static class RoundTripCase {
      final String displayName;
      final TestItem item;

      RoundTripCase(String displayName, TestItem item) {
         this.displayName = displayName;
         this.item = item;
      }
   }

   private static void roundTripSingle(Path nssPath, String gameFlag, Path scratchRoot) throws Exception {
      Path rel = sourceRoot(gameFlag).relativize(nssPath);
      Path outDir = scratchRoot.resolve(rel.getParent() == null ? Paths.get("") : rel.getParent());
      Files.createDirectories(outDir);

      Path compiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".ncs");
      runCompiler(nssPath, compiled, gameFlag, scratchRoot);

      Path decompiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".dec.nss");
      runDecompile(compiled, decompiled, gameFlag);

      String original = normalizeNewlines(Files.readString(nssPath, StandardCharsets.UTF_8));
      String roundtrip = normalizeNewlines(Files.readString(decompiled, StandardCharsets.UTF_8));

      if (!original.equals(roundtrip)) {
         String diff = formatUnifiedDiff(original, roundtrip);
         StringBuilder message = new StringBuilder("Round-trip mismatch for ").append(nssPath);
         if (diff != null) {
            message.append(System.lineSeparator()).append(diff);
         }
         throw new IllegalStateException(message.toString());
      }
   }

   private static void runCompiler(Path nssPath, Path compiledOut, String gameFlag, Path workDir) throws Exception {
      // Ensure nwscript.nss is in the compiler's directory (required by this version)
      Path compilerDir = NWN_COMPILER.getParent();
      Path nwscriptSource = "k2".equals(gameFlag) ? K2_NWSCRIPT : K1_NWSCRIPT;
      Path compilerNwscript = compilerDir.resolve("nwscript.nss");
      if (!Files.exists(compilerNwscript) || !Files.isSameFile(nwscriptSource, compilerNwscript)) {
         Files.copy(nwscriptSource, compilerNwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      // Ensure output directory exists
      Files.createDirectories(compiledOut.getParent());

      // Use compiler detection to get correct command-line arguments
      java.io.File compilerFile = NWN_COMPILER.toAbsolutePath().toFile();
      java.io.File sourceFile = nssPath.toAbsolutePath().toFile();
      java.io.File outputFile = compiledOut.toAbsolutePath().toFile();
      boolean isK2 = "k2".equals(gameFlag);

      NwnnsscompConfig config = new NwnnsscompConfig(compilerFile, sourceFile, outputFile, isK2);
      String[] cmd = config.getCompileArgs(compilerFile.getAbsolutePath());

      System.out.println("Using compiler: " + config.getChosenCompiler().getName() +
         " (SHA256: " + config.getSha256Hash().substring(0, 16) + "...)");

      ProcessBuilder pb = new ProcessBuilder(cmd);
      // Don't set working directory - use absolute paths instead
      pb.redirectErrorStream(true);
      Process proc = pb.start();

      // Capture output for debugging
      java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
         output.append(line).append("\n");
      }

      boolean finished = proc.waitFor(PROC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
      if (!finished) {
         proc.destroyForcibly();
         throw new RuntimeException("nwnnsscomp timed out for " + nssPath);
      }
      if (proc.exitValue() != 0 || !Files.isRegularFile(compiledOut)) {
         String errorMsg = "nwnnsscomp failed (exit=" + proc.exitValue() + ") for " + nssPath;
         if (output.length() > 0) {
            errorMsg += "\nCompiler output:\n" + output.toString();
         }
         throw new RuntimeException(errorMsg);
      }
   }

   private static void runDecompile(Path ncsPath, Path nssOut, String gameFlag) throws Exception {
      // Set game flag before creating FileDecompiler
      FileDecompiler.isK2Selected = "k2".equals(gameFlag);

      // nwscript.nss files are already copied to current working directory in @BeforeAll
      // FileDecompiler uses System.getProperty("user.dir") which is the NCSDecomp directory

      try {
         FileDecompiler fd = new FileDecompiler();
         File ncsFile = ncsPath.toFile();
         File nssFile = nssOut.toFile();

         // Ensure parent directory exists
         Files.createDirectories(nssFile.getParentFile().toPath());

         fd.decompileToFile(ncsFile, nssFile, StandardCharsets.UTF_8, true);

         if (!Files.isRegularFile(nssOut)) {
            throw new RuntimeException("Decompile did not produce output: " + nssOut);
         }
      } catch (DecompilerException ex) {
         throw new RuntimeException("Decompile failed for " + ncsPath + ": " + ex.getMessage(), ex);
      }
   }

   private static Path prepareScratch(String gameLabel, Path nwscriptSource) throws IOException {
      Path scratch = WORK_ROOT.resolve(gameLabel);
      Files.createDirectories(scratch);
      Files.copy(nwscriptSource, scratch.resolve("nwscript.nss"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return scratch;
   }

   private static Path sourceRoot(String gameFlag) {
      return "k1".equals(gameFlag) ? K1_ROOT : VANILLA_ROOT;
   }

   private static String stripExt(String name) {
      int dot = name.lastIndexOf('.');
      return dot == -1 ? name : name.substring(0, dot);
   }

   private static String normalizeNewlines(String s) {
      // Normalize line endings
      String normalized = s.replace("\r\n", "\n").replace("\r", "\n");

      // Strip single-line comments (// ...) and block comments (/* ... */)
      normalized = stripComments(normalized);

      // Remove trailing whitespace from each line and normalize multiple blank lines
      String[] lines = normalized.split("\n", -1);
      StringBuilder result = new StringBuilder();
      boolean lastWasBlank = false;

      for (String line : lines) {
         // Remove only trailing whitespace, preserve leading whitespace (indentation)
         String trimmed = line.replaceFirst("\\s+$", "");

         // Collapse multiple consecutive blank lines into a single blank line
         if (trimmed.isEmpty()) {
            if (!lastWasBlank) {
               result.append("\n");
               lastWasBlank = true;
            }
         } else {
            // Normalize multiple spaces/tabs to single space, but preserve structure
            // Replace tabs with spaces, then collapse multiple spaces
            trimmed = trimmed.replace("\t", "    ").replaceAll(" +", " ");
            result.append(trimmed).append("\n");
            lastWasBlank = false;
         }
      }

      // Remove trailing newlines
      String finalResult = result.toString();
      while (finalResult.endsWith("\n")) {
         finalResult = finalResult.substring(0, finalResult.length() - 1);
      }

      return finalResult;
   }

   private static String stripComments(String code) {
      StringBuilder result = new StringBuilder();
      boolean inBlockComment = false;
      boolean inString = false;
      char[] chars = code.toCharArray();

      for (int i = 0; i < chars.length; i++) {
         if (inBlockComment) {
            if (i < chars.length - 1 && chars[i] == '*' && chars[i + 1] == '/') {
               inBlockComment = false;
               i++; // Skip the '/'
            }
            continue;
         }

         if (inString) {
            result.append(chars[i]);
            if (chars[i] == '"' && (i == 0 || chars[i - 1] != '\\')) {
               inString = false;
            }
            continue;
         }

         if (chars[i] == '"') {
            inString = true;
            result.append(chars[i]);
         } else if (i < chars.length - 1 && chars[i] == '/' && chars[i + 1] == '/') {
            // Single-line comment - skip to end of line
            while (i < chars.length && chars[i] != '\n') {
               i++;
            }
            if (i < chars.length) {
               result.append('\n');
            }
         } else if (i < chars.length - 1 && chars[i] == '/' && chars[i + 1] == '*') {
            // Block comment start
            inBlockComment = true;
            i++; // Skip the '*'
         } else {
            result.append(chars[i]);
         }
      }

      return result.toString();
   }

   /**
    * Extract expected and actual values from assertion failure message and format as unified diff.
    */
   private static String extractAndFormatDiff(String message) {
      // Pattern: "expected: <...> but was: <...>"
      int expectedStart = message.indexOf("expected: <");
      int butWasStart = message.indexOf(" but was: <");

      if (expectedStart == -1 || butWasStart == -1) {
         return null;
      }

      int expectedValueStart = expectedStart + "expected: <".length();
      int expectedValueEnd = message.indexOf(">", expectedValueStart);
      int actualValueStart = butWasStart + " but was: <".length();
      int actualValueEnd = message.lastIndexOf(">");

      if (expectedValueEnd == -1 || actualValueEnd == -1 || actualValueEnd <= actualValueStart) {
         return null;
      }

      String expected = message.substring(expectedValueStart, expectedValueEnd);
      String actual = message.substring(actualValueStart, actualValueEnd);

      return formatUnifiedDiff(expected, actual);
   }

   /**
    * Format two strings as a unified diff using proper diff algorithm.
    */
   private static String formatUnifiedDiff(String expected, String actual) {
      String[] expectedLines = expected.split("\n", -1);
      String[] actualLines = actual.split("\n", -1);

      // Compute the diff using a simple longest common subsequence approach
      DiffResult diffResult = computeDiff(expectedLines, actualLines);

      if (diffResult.isEmpty()) {
         return null; // No differences
      }

      StringBuilder diff = new StringBuilder();
      diff.append("    --- expected\n");
      diff.append("    +++ actual\n");

      // Calculate actual hunk ranges
      // Track line numbers as we go through the diff
      int oldLineNum = 1;
      int newLineNum = 1;
      int firstOldLine = -1;
      int firstNewLine = -1;
      int lastOldLine = -1;
      int lastNewLine = -1;

      for (DiffLine line : diffResult.lines) {
         if (line.type == DiffLineType.REMOVED) {
            if (firstOldLine == -1) firstOldLine = oldLineNum;
            lastOldLine = oldLineNum;
            oldLineNum++;
         } else if (line.type == DiffLineType.ADDED) {
            if (firstNewLine == -1) firstNewLine = newLineNum;
            lastNewLine = newLineNum;
            newLineNum++;
         } else {
            // Context line - include in range if we've seen changes
            if (firstOldLine != -1 && lastOldLine == oldLineNum - 1) {
               lastOldLine = oldLineNum;
            }
            if (firstNewLine != -1 && lastNewLine == newLineNum - 1) {
               lastNewLine = newLineNum;
            }
            oldLineNum++;
            newLineNum++;
         }
      }

      // Calculate hunk header - use full range if no changes found
      int oldStart, oldCount, newStart, newCount;
      if (firstOldLine == -1) {
         oldStart = 1;
         oldCount = expectedLines.length;
      } else {
         oldStart = firstOldLine;
         oldCount = lastOldLine - firstOldLine + 1;
      }

      if (firstNewLine == -1) {
         newStart = 1;
         newCount = actualLines.length;
      } else {
         newStart = firstNewLine;
         newCount = lastNewLine - firstNewLine + 1;
      }

      diff.append("    @@ -").append(oldStart);
      if (oldCount != 1) {
         diff.append(",").append(oldCount);
      }
      diff.append(" +").append(newStart);
      if (newCount != 1) {
         diff.append(",").append(newCount);
      }
      diff.append(" @@\n");

      // Output the diff lines
      for (DiffLine line : diffResult.lines) {
         switch (line.type) {
            case CONTEXT:
               diff.append("     ").append(line.content).append("\n");
               break;
            case REMOVED:
               diff.append("    -").append(line.content).append("\n");
               break;
            case ADDED:
               diff.append("    +").append(line.content).append("\n");
               break;
         }
      }

      return diff.toString();
   }

   private enum DiffLineType {
      CONTEXT, REMOVED, ADDED
   }

   private static class DiffLine {
      final DiffLineType type;
      final String content;

      DiffLine(DiffLineType type, String content) {
         this.type = type;
         this.content = content;
      }
   }

   private static class DiffResult {
      final List<DiffLine> lines = new ArrayList<>();

      boolean isEmpty() {
         return lines.stream().allMatch(l -> l.type == DiffLineType.CONTEXT);
      }
   }

   /**
    * Compute diff between two string arrays using a simple algorithm.
    * This implements a basic longest common subsequence approach.
    */
   private static DiffResult computeDiff(String[] expected, String[] actual) {
      DiffResult result = new DiffResult();

      // Use dynamic programming to find longest common subsequence
      int m = expected.length;
      int n = actual.length;
      int[][] dp = new int[m + 1][n + 1];

      // Build LCS table
      for (int i = 1; i <= m; i++) {
         for (int j = 1; j <= n; j++) {
            if (expected[i - 1].equals(actual[j - 1])) {
               dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
               dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
         }
      }

      // Reconstruct the diff
      int i = m, j = n;
      List<DiffLine> tempLines = new ArrayList<>();

      while (i > 0 || j > 0) {
         if (i > 0 && j > 0 && expected[i - 1].equals(actual[j - 1])) {
            // Match - context line
            tempLines.add(new DiffLine(DiffLineType.CONTEXT, expected[i - 1]));
            i--;
            j--;
         } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            // Added line
            tempLines.add(new DiffLine(DiffLineType.ADDED, actual[j - 1]));
            j--;
         } else if (i > 0) {
            // Removed line
            tempLines.add(new DiffLine(DiffLineType.REMOVED, expected[i - 1]));
            i--;
         }
      }

      // Reverse to get correct order
      for (int k = tempLines.size() - 1; k >= 0; k--) {
         result.lines.add(tempLines.get(k));
      }

      return result;
   }

   /**
    * Entry point for running the round-trip suite without external test frameworks.
    */
   public static void main(String[] args) {
      NCSDecompCLIRoundTripTest runner = new NCSDecompCLIRoundTripTest();
      int exitCode = runner.runRoundTripSuite();
      if (exitCode != 0) {
         System.exit(exitCode);
      }
   }

   private int runRoundTripSuite() {
      try {
         preflight();
         List<RoundTripCase> tests = buildRoundTripCases();
         int failures = 0;

         for (RoundTripCase testCase : tests) {
            try {
               roundTripSingle(testCase.item.path, testCase.item.gameFlag, testCase.item.scratchRoot);
            } catch (Exception ex) {
               failures++;
               System.out.println();
               System.out.println(formatFailure(testCase.displayName, ex));
            }
         }

         System.out.println();
         System.out.println("Round-trip summary:");
         System.out.println("Tests run: " + tests.size());
         System.out.println("Tests succeeded: " + (tests.size() - failures));
         System.out.println("Tests failed: " + failures);

         return failures == 0 ? 0 : 1;
      } catch (Exception e) {
         e.printStackTrace();
         return 1;
      }
   }

   private String formatFailure(String displayName, Exception ex) {
      StringBuilder sb = new StringBuilder("FAILURE: ").append(displayName);
      sb.append("\nException: ").append(ex.getClass().getName());
      String message = ex.getMessage();
      if (message != null && !message.isEmpty()) {
         String diff = extractAndFormatDiff(message);
         if (diff != null) {
            sb.append("\n").append(diff);
         } else {
            sb.append("\nMessage: ").append(message);
         }
      }
      if (ex.getCause() != null && ex.getCause() != ex) {
         sb.append("\nCause: ").append(ex.getCause().getMessage());
      }
      return sb.toString();
   }
}

