// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * Exhaustive round-trip tests:
 *  1) Clone or use existing Vanilla_KOTOR_Script_Source repository
 *  2) Use nwnnsscomp.exe to compile each .nss -> .ncs (per game)
 *  3) Use NCSDecompCLI to decompile NCS -> NSS
 *  4) Compare normalized text with the original NSS
 *  5) Fast-fail on first failure
 *
 * All test artifacts are created in gitignored directories.
 */
public class NCSDecompCLIRoundTripTest {

   // Working directory (gitignored)
  private static final Path TEST_WORK_DIR = Paths.get(".").toAbsolutePath().normalize()
        .resolve("test-work");
  private static final Path VANILLA_REPO_DIR = TEST_WORK_DIR.resolve("Vanilla_KOTOR_Script_Source");
  private static final java.util.List<String> VANILLA_REPO_URLS = java.util.Arrays.asList(
        "https://github.com/KOTORCommunityPatches/Vanilla_KOTOR_Script_Source.git",
        "https://github.com/th3w1zard1/Vanilla_KOTOR_Script_Source.git"
  );

   // Paths relative to DeNCS directory
   private static final Path REPO_ROOT = Paths.get(".").toAbsolutePath().normalize();
   private static final Path NWN_COMPILER = REPO_ROOT.resolve("tools").resolve("nwnnsscomp.exe");
   private static final Path K1_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources").resolve("k1_nwscript.nss");
   private static final Path K1_ASC_NWSCRIPT = REPO_ROOT.resolve("k1_asc_nwscript.nss");
   private static final Path K2_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources").resolve("tsl_nwscript.nss");

   // Test output directories (gitignored)
   private static final Path WORK_ROOT = TEST_WORK_DIR.resolve("roundtrip-work");
   private static final Path PROFILE_OUTPUT = TEST_WORK_DIR.resolve("test_profile.txt");
   private static final Path COMPILE_TEMP_ROOT = TEST_WORK_DIR.resolve("compile-temp");

   private static final Duration PROC_TIMEOUT = Duration.ofSeconds(25);

   // Performance tracking
   private static final Map<String, Long> operationTimes = new HashMap<>();
   private static long testStartTime;
   private static int totalTests = 0;
   private static int testsProcessed = 0;

   private static Path k1Scratch;
   private static Path k2Scratch;

   /**
    * Clone or update the Vanilla_KOTOR_Script_Source repository.
    */
   static void ensureVanillaRepo() throws IOException, InterruptedException {
      if (Files.exists(VANILLA_REPO_DIR) && Files.isDirectory(VANILLA_REPO_DIR)) {
         // Check if it's a valid git repo
         Path gitDir = VANILLA_REPO_DIR.resolve(".git");
         if (Files.exists(gitDir) && Files.isDirectory(gitDir)) {
            System.out.println("Using existing Vanilla_KOTOR_Script_Source repository at: " + VANILLA_REPO_DIR);
            // Optionally update: git pull
            return;
         } else {
            // Directory exists but isn't a git repo, remove it
            System.out.println("Removing non-git directory: " + VANILLA_REPO_DIR);
            deleteDirectory(VANILLA_REPO_DIR);
         }
      }

      // Clone the repository - try each URL in order until one succeeds
      System.out.println("Cloning Vanilla_KOTOR_Script_Source repository...");
      System.out.println("  Destination: " + VANILLA_REPO_DIR);

      Files.createDirectories(VANILLA_REPO_DIR.getParent());

      IOException lastException = null;
      for (int i = 0; i < VANILLA_REPO_URLS.size(); i++) {
         String repoUrl = VANILLA_REPO_URLS.get(i);
         System.out.println("  Attempting URL " + (i + 1) + "/" + VANILLA_REPO_URLS.size() + ": " + repoUrl);

         ProcessBuilder pb = new ProcessBuilder("git", "clone", repoUrl, VANILLA_REPO_DIR.toString());
         pb.redirectErrorStream(true);
         Process proc = pb.start();

         // Capture output
         StringBuilder output = new StringBuilder();
         try (java.io.BufferedReader reader = new java.io.BufferedReader(
               new java.io.InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
               output.append(line).append("\n");
            }
         }

         int exitCode = proc.waitFor();
         if (exitCode == 0) {
            System.out.println("Repository cloned successfully from: " + repoUrl);
            return;
         }

         // Clone failed, save error and try next URL
         lastException = new IOException("Failed to clone from " + repoUrl + ". Exit code: " + exitCode +
               "\nOutput: " + output.toString());
         System.out.println("  Failed: " + lastException.getMessage().split("\n")[0]);

         // Clean up any partial clone attempt
         if (Files.exists(VANILLA_REPO_DIR)) {
            try {
               deleteDirectory(VANILLA_REPO_DIR);
            } catch (Exception e) {
               // Ignore cleanup errors
            }
         }
      }

      // All URLs failed
      throw new IOException("Failed to clone repository from all " + VANILLA_REPO_URLS.size() + " URLs:\n" +
            String.join("\n", VANILLA_REPO_URLS) + "\n\nLast error: " +
            (lastException != null ? lastException.getMessage() : "Unknown error"));
   }

   static void preflight() throws IOException, InterruptedException {
      System.out.println("=== Preflight Checks ===");

      // Ensure vanilla repo exists
      ensureVanillaRepo();

      // Check for required files
      if (!Files.isRegularFile(NWN_COMPILER)) {
         throw new IOException("nwnnsscomp.exe missing at: " + NWN_COMPILER);
      }
      System.out.println("✓ Found compiler: " + NWN_COMPILER);

      if (!Files.isRegularFile(K1_NWSCRIPT)) {
         throw new IOException("k1_nwscript.nss missing at: " + K1_NWSCRIPT);
      }
      System.out.println("✓ Found K1 nwscript: " + K1_NWSCRIPT);

      if (!Files.isRegularFile(K1_ASC_NWSCRIPT)) {
         throw new IOException("k1_asc_nwscript.nss missing at: " + K1_ASC_NWSCRIPT);
      }
      System.out.println("✓ Found K1 ASC nwscript: " + K1_ASC_NWSCRIPT);

      if (!Files.isRegularFile(K2_NWSCRIPT)) {
         throw new IOException("tsl_nwscript.nss missing at: " + K2_NWSCRIPT);
      }
      System.out.println("✓ Found TSL nwscript: " + K2_NWSCRIPT);

      // Verify vanilla repo structure
      Path k1Root = VANILLA_REPO_DIR.resolve("K1");
      Path tslRoot = VANILLA_REPO_DIR.resolve("TSL");
      if (!Files.exists(k1Root) || !Files.isDirectory(k1Root)) {
         throw new IOException("K1 directory not found in vanilla repo: " + k1Root);
      }
      if (!Files.exists(tslRoot) || !Files.isDirectory(tslRoot)) {
         throw new IOException("TSL directory not found in vanilla repo: " + tslRoot);
      }
      System.out.println("✓ Vanilla repo structure verified");

      // Prepare scratch directories
      k1Scratch = prepareScratch("k1", K1_NWSCRIPT);
      k2Scratch = prepareScratch("k2", K2_NWSCRIPT);

      // Copy nwscript files to current working directory for FileDecompiler
      Path cwd = Paths.get(System.getProperty("user.dir"));
      Path k1Nwscript = cwd.resolve("k1_nwscript.nss");
      Path k2Nwscript = cwd.resolve("tsl_nwscript.nss");

      if (!Files.exists(k1Nwscript) || !Files.isSameFile(K1_NWSCRIPT, k1Nwscript)) {
         Files.copy(K1_NWSCRIPT, k1Nwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      if (!Files.exists(k2Nwscript) || !Files.isSameFile(K2_NWSCRIPT, k2Nwscript)) {
         Files.copy(K2_NWSCRIPT, k2Nwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      System.out.println("=== Preflight Complete ===\n");
   }

   List<RoundTripCase> buildRoundTripCases() throws IOException {
      System.out.println("=== Discovering Test Files ===");

      List<TestItem> allFiles = new ArrayList<>();

      // K1 files
      Path k1Root = VANILLA_REPO_DIR.resolve("K1");
      if (Files.exists(k1Root)) {
         try (Stream<Path> paths = Files.walk(k1Root)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .forEach(p -> allFiles.add(new TestItem(p, "k1", k1Scratch)));
         }
      }

      // TSL files
      Path tslVanilla = VANILLA_REPO_DIR.resolve("TSL").resolve("Vanilla");
      if (Files.exists(tslVanilla)) {
         try (Stream<Path> paths = Files.walk(tslVanilla)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));
         }
      }

      Path tslTslrcm = VANILLA_REPO_DIR.resolve("TSL").resolve("TSLRCM");
      if (Files.exists(tslTslrcm)) {
         try (Stream<Path> paths = Files.walk(tslTslrcm)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));
         }
      }

      System.out.println("Found " + allFiles.size() + " .nss files");

      // Shuffle for better distribution
      Collections.shuffle(allFiles);

      List<RoundTripCase> tests = new ArrayList<>();
      for (TestItem item : allFiles) {
         Path relPath = VANILLA_REPO_DIR.relativize(item.path);
         String displayName = item.gameFlag.equals("k1")
               ? "K1: " + relPath
               : "TSL: " + relPath;
         tests.add(new RoundTripCase(displayName, item));
      }

      totalTests = tests.size();
      System.out.println("=== Test Discovery Complete ===\n");

      return tests;
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
      long startTime = System.nanoTime();

      Path rel = VANILLA_REPO_DIR.relativize(nssPath);
      String displayPath = rel.toString().replace('\\', '/');

      Path outDir = scratchRoot.resolve(rel.getParent() == null ? Paths.get("") : rel.getParent());
      Files.createDirectories(outDir);

      // Compile: NSS -> NCS
      Path compiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".ncs");
      System.out.print("  Compiling " + displayPath + " to .ncs with nwnnsscomp.exe");
      long compileStart = System.nanoTime();
      try {
         runCompiler(nssPath, compiled, gameFlag, scratchRoot);
         long compileTime = System.nanoTime() - compileStart;
         operationTimes.merge("compile", compileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", compileTime / 1_000_000.0) + " ms)");
      } catch (Exception e) {
         long compileTime = System.nanoTime() - compileStart;
         operationTimes.merge("compile", compileTime, Long::sum);
         throw e; // Re-throw after recording time
      }

      // Decompile: NCS -> NSS
      Path decompiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".dec.nss");
      System.out.print("  Decompiling " + compiled.getFileName() + " back to .nss");
      long decompileStart = System.nanoTime();
      try {
         runDecompile(compiled, decompiled, gameFlag);
         long decompileTime = System.nanoTime() - decompileStart;
         operationTimes.merge("decompile", decompileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", decompileTime / 1_000_000.0) + " ms)");
      } catch (Exception e) {
         long decompileTime = System.nanoTime() - decompileStart;
         operationTimes.merge("decompile", decompileTime, Long::sum);
         throw e; // Re-throw after recording time
      }

      System.out.print("  Comparing original vs round-trip");

      // Compare
      long compareStart = System.nanoTime();
      try {
         String original = normalizeNewlines(new String(Files.readAllBytes(nssPath), StandardCharsets.UTF_8));
         String roundtrip = normalizeNewlines(new String(Files.readAllBytes(decompiled), StandardCharsets.UTF_8));
         long compareTime = System.nanoTime() - compareStart;
         operationTimes.merge("compare", compareTime, Long::sum);

         if (!original.equals(roundtrip)) {
            System.out.println(" ✗ MISMATCH");
            String diff = formatUnifiedDiff(original, roundtrip);
            StringBuilder message = new StringBuilder("Round-trip mismatch for ").append(nssPath);
            if (diff != null) {
               message.append(System.lineSeparator()).append(diff);
            }
            throw new IllegalStateException(message.toString());
         }

         System.out.println(" ✓ MATCH");
      } catch (Exception e) {
         long compareTime = System.nanoTime() - compareStart;
         operationTimes.merge("compare", compareTime, Long::sum);
         throw e; // Re-throw after recording time
      }
      long totalTime = System.nanoTime() - startTime;
      operationTimes.merge("total", totalTime, Long::sum);
   }

   /**
    * Detects if a script file needs the ASC nwscript (for ActionStartConversation with 11 parameters).
    * Checks if the file contains ActionStartConversation calls with exactly 11 parameters
    * by counting commas in the parameter list. A call with 10 commas indicates 11 parameters.
    */
   private static boolean needsAscNwscript(Path nssPath) throws Exception {
      String content = new String(Files.readAllBytes(nssPath), StandardCharsets.UTF_8);
      // Look for ActionStartConversation calls with 11 parameters (10 commas)
      // Pattern matches ActionStartConversation( ... ) where the content between parens
      // contains exactly 10 commas (indicating 11 parameters)
      // This is more flexible than requiring specific parameter values
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
         "ActionStartConversation\\s*\\(([^,)]*,\\s*){10}[^)]*\\)",
         java.util.regex.Pattern.MULTILINE
      );
      return pattern.matcher(content).find();
   }

   /**
    * Extracts include file names from a script file.
    * Parses #include statements and returns the include file names (without quotes).
    */
   private static List<String> extractIncludes(Path nssPath) throws Exception {
      String content = new String(Files.readAllBytes(nssPath), StandardCharsets.UTF_8);
      List<String> includes = new ArrayList<>();
      java.util.regex.Pattern includePattern = java.util.regex.Pattern.compile(
         "#include\\s+[\"<]([^\">]+)[\">]",
         java.util.regex.Pattern.MULTILINE
      );
      java.util.regex.Matcher matcher = includePattern.matcher(content);
      while (matcher.find()) {
         includes.add(matcher.group(1));
      }
      return includes;
   }

   /**
    * Finds an include file in the repository structure.
    * Checks common locations: same directory as source, K1/Data/scripts.bif, TSL/Data/Scripts, etc.
    * Handles include names with or without .nss extension.
    */
   private static Path findIncludeFile(String includeName, Path sourceFile, String gameFlag) {
      // Normalize include name - add .nss extension if missing
      String normalizedName = includeName;
      if (!normalizedName.endsWith(".nss") && !normalizedName.endsWith(".h")) {
         normalizedName = includeName + ".nss";
      }

      // Check same directory as source file
      Path sourceDir = sourceFile.getParent();
      Path localInc = sourceDir.resolve(normalizedName);
      if (Files.exists(localInc)) {
         return localInc;
      }
      // Also try without extension
      localInc = sourceDir.resolve(includeName);
      if (Files.exists(localInc)) {
         return localInc;
      }

      // Check K1/Data/scripts.bif (common location for includes, used by both K1 and TSL)
      Path k1IncludesDir = VANILLA_REPO_DIR.resolve("K1").resolve("Data").resolve("scripts.bif");
      Path k1Inc = k1IncludesDir.resolve(normalizedName);
      if (Files.exists(k1Inc)) {
         return k1Inc;
      }
      // Also try without extension
      k1Inc = k1IncludesDir.resolve(includeName);
      if (Files.exists(k1Inc)) {
         return k1Inc;
      }

      // For TSL, also check TSL/Data/Scripts
      if ("k2".equals(gameFlag)) {
         Path tslScriptsDir = VANILLA_REPO_DIR.resolve("TSL").resolve("Vanilla").resolve("Data").resolve("Scripts");
         Path tslInc = tslScriptsDir.resolve(normalizedName);
         if (Files.exists(tslInc)) {
            return tslInc;
         }
         // Also try without extension
         tslInc = tslScriptsDir.resolve(includeName);
         if (Files.exists(tslInc)) {
            return tslInc;
         }

         // Check TSLRCM if it exists
         Path tslRcmScriptsDir = VANILLA_REPO_DIR.resolve("TSL").resolve("TSLRCM").resolve("Data").resolve("Scripts");
         Path tslRcmInc = tslRcmScriptsDir.resolve(normalizedName);
         if (Files.exists(tslRcmInc)) {
            return tslRcmInc;
         }
         // Also try without extension
         tslRcmInc = tslRcmScriptsDir.resolve(includeName);
         if (Files.exists(tslRcmInc)) {
            return tslRcmInc;
         }
      }

      return null;
   }

   /**
    * Creates a temporary working directory with the source file and all its includes.
    * Returns the temp directory path and the path to the copied source file.
    */
   private static Path setupTempCompileDir(Path originalNssPath, String gameFlag) throws Exception {
      // Ensure parent directory exists
      Files.createDirectories(COMPILE_TEMP_ROOT);
      // Create unique temp directory for this compilation
      Path tempDir = Files.createTempDirectory(COMPILE_TEMP_ROOT, "compile_");

      // Copy source file to temp directory
      Path tempSourceFile = tempDir.resolve(originalNssPath.getFileName());
      Files.copy(originalNssPath, tempSourceFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      // Find and copy all include files
      List<String> includes = extractIncludes(originalNssPath);
      for (String includeName : includes) {
         Path includeFile = findIncludeFile(includeName, originalNssPath, gameFlag);
         if (includeFile != null && Files.exists(includeFile)) {
            // Use the include name exactly as specified in the source file
            // This ensures the compiler can find it with the exact name it expects
            Path tempInclude = tempDir.resolve(includeName);
            
            // Copy the file - if source has extension but include name doesn't,
            // Files.copy will create the file with the exact name specified in tempInclude
            Files.copy(includeFile, tempInclude, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Verify the file was copied successfully
            if (!Files.exists(tempInclude)) {
               throw new IOException("Failed to copy include file: " + includeFile + " to " + tempInclude);
            }
         } else {
            // Include file not found - this might cause compilation to fail, but let compiler report it
         }
      }

      return tempDir;
   }

   private static void runCompiler(Path originalNssPath, Path compiledOut, String gameFlag, Path workDir) throws Exception {
      // Store original path for logging (show path relative to vanilla repo)
      Path relPath = VANILLA_REPO_DIR.relativize(originalNssPath);

      // Determine which nwscript.nss to use based on game
      // K1 files (from test-work\Vanilla_KOTOR_Script_Source\K1) use k1_nwscript.nss
      // K1 files with ActionStartConversation(11 params) use k1_asc_nwscript.nss
      // TSL files (from test-work\Vanilla_KOTOR_Script_Source\TSL) use tsl_nwscript.nss
      Path nwscriptSource;
      if ("k1".equals(gameFlag)) {
         // Check if this script needs ASC nwscript
         if (needsAscNwscript(originalNssPath)) {
            nwscriptSource = K1_ASC_NWSCRIPT;
            if (!Files.exists(nwscriptSource)) {
               throw new IllegalStateException("K1 ASC nwscript file not found: " + nwscriptSource);
            }
         } else {
            nwscriptSource = K1_NWSCRIPT;
            if (!Files.exists(nwscriptSource)) {
               throw new IllegalStateException("K1 nwscript file not found: " + nwscriptSource);
            }
         }
      } else if ("k2".equals(gameFlag)) {
         nwscriptSource = K2_NWSCRIPT;
         if (!Files.exists(nwscriptSource)) {
            throw new IllegalStateException("TSL nwscript file not found: " + nwscriptSource);
         }
      } else {
         throw new IllegalArgumentException("Invalid game flag: " + gameFlag + " (expected 'k1' or 'k2')");
      }

      // Ensure nwscript.nss is in the compiler's directory
      Path compilerDir = NWN_COMPILER.getParent();
      Path compilerNwscript = compilerDir.resolve("nwscript.nss");
      if (!Files.exists(compilerNwscript) || !Files.isSameFile(nwscriptSource, compilerNwscript)) {
         Files.copy(nwscriptSource, compilerNwscript, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      // Create temp directory with source file and all includes
      Path tempDir = null;
      Path tempSourceFile = null;
      try {
         tempDir = setupTempCompileDir(originalNssPath, gameFlag);
         tempSourceFile = tempDir.resolve(originalNssPath.getFileName());

         Files.createDirectories(compiledOut.getParent());

         java.io.File compilerFile = NWN_COMPILER.toAbsolutePath().toFile();
         java.io.File sourceFile = tempSourceFile.toAbsolutePath().toFile();
         java.io.File outputFile = compiledOut.toAbsolutePath().toFile();
         boolean isK2 = "k2".equals(gameFlag);

         NwnnsscompConfig config = new NwnnsscompConfig(compilerFile, sourceFile, outputFile, isK2);
         // Don't use -i flag - compiler expects includes in same directory as source
         // All includes are already copied to tempDir alongside the source file
         String[] cmd = config.getCompileArgs(compilerFile.getAbsolutePath());

         // Log compilation command and args (but show original path in the log)
         System.out.print(" (");
         for (int i = 0; i < cmd.length; i++) {
            if (i > 0) System.out.print(" ");
            String arg = cmd[i];
            // Replace temp path with original path in log output for readability
            if (arg.equals(sourceFile.getAbsolutePath())) {
               arg = originalNssPath.toAbsolutePath().toString();
            }
            // Quote arguments with spaces
            if (arg.contains(" ") && !arg.startsWith("\"")) {
               System.out.print("\"" + arg + "\"");
            } else {
               System.out.print(arg);
            }
         }
         System.out.print(")");

         ProcessBuilder pb = new ProcessBuilder(cmd);
         // Set working directory to temp directory so includes are found
         pb.directory(tempDir.toFile());
         pb.redirectErrorStream(true);
         Process proc = pb.start();

         java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
         StringBuilder output = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
         }

         boolean finished = proc.waitFor(PROC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
         if (!finished) {
            proc.destroyForcibly();
            System.out.println(" ✗ TIMEOUT");
            throw new RuntimeException("nwnnsscomp timed out for " + originalNssPath);
         }

         int exitCode = proc.exitValue();
         boolean fileExists = Files.isRegularFile(compiledOut);

         if (exitCode != 0 || !fileExists) {
            System.out.println(" ✗ FAILED");
            String errorMsg = "nwnnsscomp failed (exit=" + exitCode + ", fileExists=" + fileExists + ") for " + originalNssPath;
            if (output.length() > 0) {
               // Show relevant error lines
               String[] outputLines = output.toString().split("\n");
               boolean foundError = false;
               for (String outputLine : outputLines) {
                  if (outputLine.toLowerCase().contains("error") ||
                      outputLine.toLowerCase().contains("unable") ||
                      outputLine.toLowerCase().contains("include")) {
                     if (!foundError) {
                        errorMsg += "\nCompiler errors:";
                        foundError = true;
                     }
                     // Replace temp path with original path in error messages
                     String displayLine = outputLine.replace(tempSourceFile.toAbsolutePath().toString(), originalNssPath.toAbsolutePath().toString());
                     errorMsg += "\n  " + displayLine;
                  }
               }
               // If no errors found, show all output
               if (!foundError && outputLines.length > 0) {
                  String displayOutput = output.toString().replace(tempSourceFile.toAbsolutePath().toString(), originalNssPath.toAbsolutePath().toString());
                  errorMsg += "\nCompiler output:\n" + displayOutput;
               }
            }
            throw new RuntimeException(errorMsg);
         }
      } finally {
         // Clean up temp directory
         if (tempDir != null) {
            try {
               deleteDirectory(tempDir);
            } catch (Exception e) {
               // Log but don't fail on cleanup errors
               System.err.println("Warning: Failed to clean up temp directory " + tempDir + ": " + e.getMessage());
            }
         }
      }
   }

   private static void runDecompile(Path ncsPath, Path nssOut, String gameFlag) throws Exception {
      FileDecompiler.isK2Selected = "k2".equals(gameFlag);
      System.out.print(" (game=" + gameFlag + ", output=" + nssOut.getFileName() + ")");

      try {
         FileDecompiler fd = new FileDecompiler();
         File ncsFile = ncsPath.toFile();
         File nssFile = nssOut.toFile();

         Files.createDirectories(nssFile.getParentFile().toPath());

         fd.decompileToFile(ncsFile, nssFile, StandardCharsets.UTF_8, true);

         if (!Files.isRegularFile(nssOut)) {
            System.out.println(" ✗ FAILED - no output file created");
            throw new RuntimeException("Decompile did not produce output: " + nssOut);
         }
      } catch (DecompilerException ex) {
         System.out.println(" ✗ FAILED - " + ex.getMessage());
         throw new RuntimeException("Decompile failed for " + ncsPath + ": " + ex.getMessage(), ex);
      }
   }

   private static Path prepareScratch(String gameLabel, Path nwscriptSource) throws IOException {
      Path scratch = WORK_ROOT.resolve(gameLabel);
      Files.createDirectories(scratch);
      Files.copy(nwscriptSource, scratch.resolve("nwscript.nss"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return scratch;
   }

   private static String stripExt(String name) {
      int dot = name.lastIndexOf('.');
      return dot == -1 ? name : name.substring(0, dot);
   }

   private static String normalizeNewlines(String s) {
      String normalized = s.replace("\r\n", "\n").replace("\r", "\n");
      normalized = stripComments(normalized);
      normalized = normalizeVariableNames(normalized);
      normalized = normalizeDeclarationAssignment(normalized);
      normalized = normalizeTrailingZeroParams(normalized);
      normalized = normalizeReturnStatements(normalized);
      normalized = normalizeTrueFalse(normalized);
      normalized = normalizeBitwiseOperators(normalized);
      normalized = normalizePlaceholderNames(normalized);
      normalized = normalizeFunctionOrder(normalized);

      String[] lines = normalized.split("\n", -1);
      StringBuilder result = new StringBuilder();
      boolean lastWasBlank = false;

      for (String line : lines) {
         String trimmed = line.replaceFirst("\\s+$", "");

         if (trimmed.isEmpty()) {
            if (!lastWasBlank) {
               result.append("\n");
               lastWasBlank = true;
            }
         } else {
            trimmed = trimmed.replace("\t", "    ").replaceAll(" +", " ");
            result.append(trimmed).append("\n");
            lastWasBlank = false;
         }
      }

      String finalResult = result.toString();
      while (finalResult.endsWith("\n")) {
         finalResult = finalResult.substring(0, finalResult.length() - 1);
      }

      return finalResult;
   }

   /**
    * Normalizes trailing zero parameters in function calls.
    * Removes trailing ", 0" or ", 0x0" parameters since the decompiler may omit them.
    */
   private static String normalizeTrailingZeroParams(String code) {
      // Pattern to match function calls with trailing zero parameters
      // Match: functionName(...), 0) or functionName(...), 0x0)
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
         "([a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^)]*),\\s*(0|0x0)\\s*\\)"
      );
      
      String result = code;
      java.util.regex.Matcher matcher = pattern.matcher(result);
      
      // Build replacement string by processing matches in reverse order
      // Store start, end, and the actual group 1 content to avoid substring issues
      java.util.List<Object[]> matches = new java.util.ArrayList<>();
      while (matcher.find()) {
         matches.add(new Object[]{matcher.start(), matcher.end(), matcher.group(1)});
      }
      
      // Replace in reverse order to avoid offset issues
      for (int i = matches.size() - 1; i >= 0; i--) {
         Object[] match = matches.get(i);
         int start = (Integer) match[0];
         int end = (Integer) match[1];
         String group1 = (String) match[2];
         String before = result.substring(0, start);
         String after = result.substring(end);
         // Use the captured group 1 content directly, then append closing paren
         String replacement = group1 + ")";
         result = before + replacement + after;
      }
      
      return result;
   }

   /**
    * Normalizes return statements by removing unnecessary outer parentheses.
    * Converts: return (expression); to: return expression;
    */
   private static String normalizeReturnStatements(String code) {
      // Pattern to match: return (expression);
      // This handles cases where the decompiler adds unnecessary parentheses around return expressions
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
         "return\\s+\\(([^()]+(?:\\.[^()]*)*)\\);"
      );
      
      String result = code;
      java.util.regex.Matcher matcher = pattern.matcher(result);
      
      // Replace return (simple_expression); with return simple_expression;
      result = matcher.replaceAll("return $1;");
      
      return result;
   }

   /**
    * Normalizes TRUE/FALSE to 1/0 for comparison.
    */
   private static String normalizeTrueFalse(String code) {
      // Replace TRUE with 1 and FALSE with 0
      String result = code;
      result = result.replaceAll("\\bTRUE\\b", "1");
      result = result.replaceAll("\\bFALSE\\b", "0");
      return result;
   }

   /**
    * Normalizes placeholder variable names that come from incomplete stack recovery.
    */
   private static String normalizePlaceholderNames(String code) {
      return code.replaceAll("__unknown_param_\\d+", "__unknown_param");
   }

   /**
    * Sorts functions by signature to avoid order-related diffs in decompiler output.
    * <p>
    * <b>Limitation:</b> This method uses simple character counting for braces and does not
    * account for braces within string literals or comments. This may lead to incorrect
    * function parsing in edge cases where braces appear in strings or comments. For the
    * typical decompiled NCS output, this is sufficient, but a more robust parser would
    * be needed for general-purpose code parsing.
    */
   private static String normalizeFunctionOrder(String code) {
      String[] lines = code.split("\n");
      List<String> functions = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      int depth = 0;
      StringBuilder preamble = new StringBuilder();
      boolean inFunction = false;
      // Simple regex for function signature (adjust as needed for your language)
      // Example: void foo() {, int bar(int x) {, etc.
      String functionSignatureRegex = "^(\\s*\\w[\\w\\s\\*]+\\w\\s*\\([^)]*\\)\\s*\\{)";

      for (String line : lines) {
         boolean isFunctionSignature = line.matches(functionSignatureRegex);
         if (!inFunction && depth == 0 && !isFunctionSignature) {
            preamble.append(line).append("\n");
            continue;
         }

         if (!inFunction && isFunctionSignature) {
            inFunction = true;
            current.setLength(0);
         }

         if (inFunction) {
            current.append(line).append("\n");
            int openBraces = countChar(line, '{');
            int closeBraces = countChar(line, '}');
            depth += openBraces;
            depth -= closeBraces;
            // Handle single-line function: opening and closing brace on same line
            if (openBraces > 0 && closeBraces > 0 && depth == 0) {
               functions.add(current.toString().trim());
               current.setLength(0);
               inFunction = false;
            } else if (inFunction && depth == 0) {
               functions.add(current.toString().trim());
               current.setLength(0);
               inFunction = false;
            }
         }
      }

      if (current.length() > 0) {
         functions.add(current.toString().trim());
      }

      functions.sort(String::compareTo);
      String preambleStr = preamble.toString().trim();
      String functionsStr = String.join("\n", functions);
      StringBuilder rebuilt = new StringBuilder();
      if (!preambleStr.isEmpty()) {
         rebuilt.append(preambleStr);
         if (!functionsStr.isEmpty()) {
            rebuilt.append("\n");
         }
      }
      if (!functionsStr.isEmpty()) {
         rebuilt.append(functionsStr);
      }
      return rebuilt.toString().trim();
   }

   private static int countChar(String line, char ch) {
      int count = 0;
      for (int i = 0; i < line.length(); i++) {
         if (line.charAt(i) == ch) {
            count++;
         }
      }
      return count;
   }

   /**
    * Normalizes bitwise operator formatting.
    * Ensures consistent spacing around & and | operators.
    */
   private static String normalizeBitwiseOperators(String code) {
      // Normalize spacing around bitwise operators
      String result = code;
      // Ensure space before and after & and | when used as bitwise operators
      // But be careful not to break && and || (logical operators)
      result = result.replaceAll("\\s*&\\s+(?!=)", " & ");
      result = result.replaceAll("\\s*\\|\\s+(?!=)", " | ");
      return result;
   }

   /**
    * Normalizes separate declaration and assignment to initialization.
    * Converts patterns like:
    *   int var1;
    *   var1 = value;
    * to:
    *   int var1 = value;
    */
   private static String normalizeDeclarationAssignment(String code) {
      // Pattern to match: type var; followed by var = value;
      java.util.regex.Pattern declPattern = java.util.regex.Pattern.compile(
         "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;"
      );
      java.util.regex.Pattern assignPattern = java.util.regex.Pattern.compile(
         "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+?);"
      );
      
      String[] lines = code.split("\n");
      StringBuilder result = new StringBuilder();
      
      for (int i = 0; i < lines.length; i++) {
         String line = lines[i].trim();
         
         // Check if this is a variable declaration
         java.util.regex.Matcher declMatcher = declPattern.matcher(line);
         if (declMatcher.matches() && i + 1 < lines.length) {
            String type = declMatcher.group(1);
            String varName = declMatcher.group(2);
            
            // Check if next non-empty line is an assignment of this variable
            int nextLineIdx = i + 1;
            while (nextLineIdx < lines.length && lines[nextLineIdx].trim().isEmpty()) {
               nextLineIdx++;
            }
            
            if (nextLineIdx < lines.length) {
               String nextLine = lines[nextLineIdx].trim();
               java.util.regex.Matcher assignMatcher = assignPattern.matcher(nextLine);
               if (assignMatcher.matches() && assignMatcher.group(1).equals(varName)) {
                  // Combine declaration and assignment into initialization
                  String value = assignMatcher.group(2);
                  result.append(type).append(" ").append(varName).append(" = ").append(value).append(";");
                  // Skip the assignment line
                  i = nextLineIdx;
                  result.append("\n");
                  continue;
               }
            }
         }
         
         result.append(lines[i]).append("\n");
      }
      
      return result.toString();
   }

   /**
    * Normalizes variable names to a canonical form for comparison.
    * Maps local variable names to canonical forms (int1, int2, etc.) based on type and order.
    * This handles the fact that decompilers can't recover original variable names.
    */
   private static String normalizeVariableNames(String code) {
      // Pattern to match variable declarations: type name [= value];
      java.util.regex.Pattern varDeclPattern = java.util.regex.Pattern.compile(
         "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=;]"
      );
      
      java.util.Map<String, String> varMap = new java.util.HashMap<>();
      java.util.Map<String, Integer> typeCounters = new java.util.HashMap<>();
      java.util.List<String> varOrder = new java.util.ArrayList<>();
      
      // First pass: collect all variable declarations
      java.util.regex.Matcher matcher = varDeclPattern.matcher(code);
      while (matcher.find()) {
         String type = matcher.group(1);
         String varName = matcher.group(2);
         
         // Skip if it's already a canonical name (int1, int2, etc.) or if it's a keyword/function
         if (varName.matches("^(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\d+$")) {
            continue;
         }
         if (isReservedName(varName)) {
            continue;
         }
         
         // Skip if already mapped
         if (varMap.containsKey(varName)) {
            continue;
         }
         
         // Create canonical name based on type
         String canonicalType = type.toLowerCase();
         int counter = typeCounters.getOrDefault(canonicalType, 0) + 1;
         typeCounters.put(canonicalType, counter);
         String canonicalName = canonicalType + counter;
         
         varMap.put(varName, canonicalName);
         varOrder.add(varName);
      }
      
      // Second pass: replace variable names in the code (in reverse order to avoid partial matches)
      String result = code;
      for (int i = varOrder.size() - 1; i >= 0; i--) {
         String originalName = varOrder.get(i);
         String canonicalName = varMap.get(originalName);
         // Use word boundaries to avoid partial matches
         result = result.replaceAll("\\b" + java.util.regex.Pattern.quote(originalName) + "\\b", canonicalName);
      }
      
      return result;
   }
   
   private static boolean isReservedName(String name) {
      // Keywords and common nwscript functions/constants that shouldn't be normalized
      String[] reserved = {
         "int", "float", "string", "object", "void", "vector", "location", 
         "effect", "itemproperty", "talent", "action", "event", "struct",
         "if", "else", "for", "while", "do", "switch", "case", "default",
         "return", "break", "continue", "main", "StartingConditional",
         "GetGlobalNumber", "GetGlobalBoolean", "GetGlobalString",
         "SetGlobalNumber", "SetGlobalBoolean", "SetGlobalString",
         "GetObjectByTag", "GetPartyMemberByIndex", "SetPartyLeader",
         "NoClicksFor", "DelayCommand", "SignalEvent", "EventUserDefined",
         "OBJECT_SELF", "GetLastOpenedBy", "IsObjectPartyMember"
      };
      for (String reservedName : reserved) {
         if (reservedName.equals(name)) {
            return true;
         }
      }
      return false;
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
               i++;
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
            while (i < chars.length && chars[i] != '\n') {
               i++;
            }
            if (i < chars.length) {
               result.append('\n');
            }
         } else if (i < chars.length - 1 && chars[i] == '/' && chars[i + 1] == '*') {
            inBlockComment = true;
            i++;
         } else {
            result.append(chars[i]);
         }
      }

      return result.toString();
   }

   private static String formatUnifiedDiff(String expected, String actual) {
      String[] expectedLines = expected.split("\n", -1);
      String[] actualLines = actual.split("\n", -1);

      DiffResult diffResult = computeDiff(expectedLines, actualLines);

      if (diffResult.isEmpty()) {
         return null;
      }

      StringBuilder diff = new StringBuilder();
      diff.append("    --- expected\n");
      diff.append("    +++ actual\n");

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

   private static DiffResult computeDiff(String[] expected, String[] actual) {
      DiffResult result = new DiffResult();

      int m = expected.length;
      int n = actual.length;
      int[][] dp = new int[m + 1][n + 1];

      for (int i = 1; i <= m; i++) {
         for (int j = 1; j <= n; j++) {
            if (expected[i - 1].equals(actual[j - 1])) {
               dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
               dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
         }
      }

      int i = m, j = n;
      List<DiffLine> tempLines = new ArrayList<>();

      while (i > 0 || j > 0) {
         if (i > 0 && j > 0 && expected[i - 1].equals(actual[j - 1])) {
            tempLines.add(new DiffLine(DiffLineType.CONTEXT, expected[i - 1]));
            i--;
            j--;
         } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            tempLines.add(new DiffLine(DiffLineType.ADDED, actual[j - 1]));
            j--;
         } else if (i > 0) {
            tempLines.add(new DiffLine(DiffLineType.REMOVED, expected[i - 1]));
            i--;
         }
      }

      for (int k = tempLines.size() - 1; k >= 0; k--) {
         result.lines.add(tempLines.get(k));
      }

      return result;
   }

   /**
    * Entry point for running the round-trip suite.
    */
   public static void main(String[] args) {
      NCSDecompCLIRoundTripTest runner = new NCSDecompCLIRoundTripTest();
      int exitCode = runner.runRoundTripSuite();
      if (exitCode != 0) {
         System.exit(exitCode);
      }
   }

   /**
    * JUnit test method that runs the round-trip test suite.
    * This allows Maven to discover and run the test during the test phase.
    */
   @Test
   public void testRoundTripSuite() {
      int exitCode = runRoundTripSuite();
      assertEquals(0, exitCode, "Round-trip test suite should pass with exit code 0");
   }

   private int runRoundTripSuite() {
      testStartTime = System.nanoTime();

      try {
         preflight();
         List<RoundTripCase> tests = buildRoundTripCases();

         if (tests.isEmpty()) {
            System.err.println("ERROR: No test files found!");
            return 1;
         }

         System.out.println("=== Running Round-Trip Tests ===");
         System.out.println("Total tests: " + tests.size());
         System.out.println("Fast-fail: enabled (will stop on first failure)");
         System.out.println();

         for (RoundTripCase testCase : tests) {
            testsProcessed++;
            Path relPath = VANILLA_REPO_DIR.relativize(testCase.item.path);
            String displayPath = relPath.toString().replace('\\', '/');
            System.out.println(String.format("[%d/%d] %s", testsProcessed, totalTests, displayPath));

            try {
               roundTripSingle(testCase.item.path, testCase.item.gameFlag, testCase.item.scratchRoot);
               System.out.println("  Result: ✓ PASSED");
               System.out.println();
            } catch (Exception ex) {
               System.out.println("  Result: ✗ FAILED");
               System.out.println();
               System.out.println("═══════════════════════════════════════════════════════════");
               System.out.println("FAILURE: " + testCase.displayName);
               System.out.println("═══════════════════════════════════════════════════════════");
               System.out.println("Exception: " + ex.getClass().getSimpleName());
               String message = ex.getMessage();
               if (message != null && !message.isEmpty()) {
                  String diff = extractAndFormatDiff(message);
                  if (diff != null) {
                     System.out.println("\nDiff:");
                     System.out.println(diff);
                  } else {
                     System.out.println("Message: " + message);
                  }
               }
               if (ex.getCause() != null && ex.getCause() != ex) {
                  System.out.println("Cause: " + ex.getCause().getMessage());
               }
               System.out.println("═══════════════════════════════════════════════════════════");
               System.out.println();

               // Fast-fail: exit immediately on first failure
               printPerformanceSummary();
               return 1;
            }
         }

         System.out.println();
         System.out.println("═══════════════════════════════════════════════════════════");
         System.out.println("ALL TESTS PASSED!");
         System.out.println("═══════════════════════════════════════════════════════════");
         System.out.println("Tests run: " + tests.size());
         System.out.println("Tests passed: " + tests.size());
         System.out.println("Tests failed: 0");
         System.out.println();

         printPerformanceSummary();
         return 0;
      } catch (Exception e) {
         System.err.println("FATAL ERROR: " + e.getMessage());
         e.printStackTrace();
         printPerformanceSummary();
         return 1;
      }
   }

   private void printPerformanceSummary() {
      long totalTime = System.nanoTime() - testStartTime;

      System.out.println("═══════════════════════════════════════════════════════════");
      System.out.println("PERFORMANCE SUMMARY");
      System.out.println("═══════════════════════════════════════════════════════════");
      System.out.println(String.format("Total test time: %.2f seconds", totalTime / 1_000_000_000.0));
      System.out.println(String.format("Tests processed: %d / %d", testsProcessed, totalTests));

      if (testsProcessed > 0) {
         System.out.println();
         System.out.println("Operation breakdown (cumulative):");
         for (Map.Entry<String, Long> entry : operationTimes.entrySet()) {
            double seconds = entry.getValue() / 1_000_000_000.0;
            double percentage = (entry.getValue() * 100.0) / totalTime;
            System.out.println(String.format("  %-12s: %8.2f s (%5.1f%%)",
                  entry.getKey(), seconds, percentage));
         }

         System.out.println();
         System.out.println("Average per test:");
         double avgTotal = (operationTimes.getOrDefault("total", 0L) / 1_000_000_000.0) / testsProcessed;
         double avgCompile = (operationTimes.getOrDefault("compile", 0L) / 1_000_000_000.0) / testsProcessed;
         double avgDecompile = (operationTimes.getOrDefault("decompile", 0L) / 1_000_000_000.0) / testsProcessed;
         double avgCompare = (operationTimes.getOrDefault("compare", 0L) / 1_000_000_000.0) / testsProcessed;

         System.out.println(String.format("  Total:      %.3f s", avgTotal));
         System.out.println(String.format("  Compile:    %.3f s", avgCompile));
         System.out.println(String.format("  Decompile:  %.3f s", avgDecompile));
         System.out.println(String.format("  Compare:    %.3f s", avgCompare));
      }

      System.out.println();
      System.out.println("Profile log: " + PROFILE_OUTPUT);
      System.out.println("═══════════════════════════════════════════════════════════");
   }

   private String extractAndFormatDiff(String message) {
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

   private static void deleteDirectory(Path dir) throws IOException {
      if (Files.exists(dir)) {
         try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                  .forEach(path -> {
                     try {
                        Files.delete(path);
                     } catch (IOException e) {
                        // Ignore deletion errors
                     }
                  });
         }
      }
   }
}
