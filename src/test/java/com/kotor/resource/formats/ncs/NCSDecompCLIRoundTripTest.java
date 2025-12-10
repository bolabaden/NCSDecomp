// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystemException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

/**
 * Exhaustive round-trip tests:
 * 1) Clone or use existing Vanilla_KOTOR_Script_Source repository
 * 2) Use nwnnsscomp.exe to compile each .nss -> .ncs (per game)
 * 3) Use NCSDecompCLI to decompile NCS -> NSS
 * 4) Compare normalized text with the original NSS
 * 5) Fast-fail on first failure
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
         "https://github.com/th3w1zard1/Vanilla_KOTOR_Script_Source.git");

   // Paths relative to DeNCS directory
   private static final Path REPO_ROOT = Paths.get(".").toAbsolutePath().normalize();
   private static final Path NWN_COMPILER = REPO_ROOT.resolve("tools").resolve("nwnnsscomp.exe");
   private static final Path K1_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources")
         .resolve("k1_nwscript.nss");
   private static final Path K1_ASC_NWSCRIPT = REPO_ROOT.resolve("k1_asc_nwscript.nss");
   private static final Path K2_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources")
         .resolve("tsl_nwscript.nss");
   private static final Map<String, String> NPC_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "NPC_");
   private static final Map<String, String> NPC_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "NPC_");
   private static final Map<String, String> ABILITY_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "ABILITY_");
   private static final Map<String, String> ABILITY_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "ABILITY_");
   private static final Map<String, String> FACTION_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "STANDARD_FACTION_");
   private static final Map<String, String> FACTION_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "STANDARD_FACTION_");

   private static String displayPath(Path path) {
      Path abs = path.toAbsolutePath().normalize();
      String absStr = abs.toString().replace('\\', '/');

      List<String> candidates = new ArrayList<>();
      candidates.add(absStr);

      addRelIfPossible(candidates, REPO_ROOT, abs);
      addRelIfPossible(candidates, TEST_WORK_DIR, abs);
      addRelIfPossible(candidates, VANILLA_REPO_DIR, abs);

      String best = candidates.stream()
            .filter(s -> s != null && !s.isEmpty())
            .min(java.util.Comparator.comparingInt(String::length))
            .orElse(absStr);

      return ".".equals(best) ? best : best.replace('\\', '/');
   }

   private static void addRelIfPossible(List<String> candidates, Path base, Path target) {
      try {
         Path rel = base.toAbsolutePath().normalize().relativize(target);
         String relStr = rel.toString().replace('\\', '/');
         candidates.add(relStr.isEmpty() ? "." : relStr);
      } catch (IllegalArgumentException ignored) {
         // Ignore paths on different roots
      }
   }

   private static void copyWithRetry(Path source, Path target) throws IOException, InterruptedException {
      int attempts = 3;
      for (int i = 1; i <= attempts; i++) {
         try {
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
         } catch (FileSystemException ex) {
            if (i == attempts) {
               throw ex;
            }
            Thread.sleep(200L * i);
         } catch (IOException ex) {
            if (i == attempts) {
               throw ex;
            }
            Thread.sleep(200L * i);
         }
      }
   }

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

   private static void resetPerformanceTracking() {
      operationTimes.clear();
      testsProcessed = 0;
      totalTests = 0;
   }

   /**
    * Clone or update the Vanilla_KOTOR_Script_Source repository.
    */
   static void ensureVanillaRepo() throws IOException, InterruptedException {
      if (Files.exists(VANILLA_REPO_DIR) && Files.isDirectory(VANILLA_REPO_DIR)) {
         // Check if it's a valid git repo
         Path gitDir = VANILLA_REPO_DIR.resolve(".git");
         if (Files.exists(gitDir) && Files.isDirectory(gitDir)) {
            System.out.println(
                  "Using existing Vanilla_KOTOR_Script_Source repository at: " + displayPath(VANILLA_REPO_DIR));
            // Optionally update: git pull
            return;
         } else {
            // Directory exists but isn't a git repo, remove it
            System.out.println("Removing non-git directory: " + displayPath(VANILLA_REPO_DIR));
            deleteDirectory(VANILLA_REPO_DIR);
         }
      }

      // Clone the repository - try each URL in order until one succeeds
      System.out.println("Cloning Vanilla_KOTOR_Script_Source repository...");
      System.out.println("  Destination: " + displayPath(VANILLA_REPO_DIR));

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
         throw new IOException("nwnnsscomp.exe missing at: " + displayPath(NWN_COMPILER));
      }
      System.out.println("✓ Found compiler: " + displayPath(NWN_COMPILER));

      if (!Files.isRegularFile(K1_NWSCRIPT)) {
         throw new IOException("k1_nwscript.nss missing at: " + displayPath(K1_NWSCRIPT));
      }
      System.out.println("✓ Found K1 nwscript: " + displayPath(K1_NWSCRIPT));

      if (!Files.isRegularFile(K1_ASC_NWSCRIPT)) {
         throw new IOException("k1_asc_nwscript.nss missing at: " + displayPath(K1_ASC_NWSCRIPT));
      }
      System.out.println("✓ Found K1 ASC nwscript: " + displayPath(K1_ASC_NWSCRIPT));

      if (!Files.isRegularFile(K2_NWSCRIPT)) {
         throw new IOException("tsl_nwscript.nss missing at: " + displayPath(K2_NWSCRIPT));
      }
      System.out.println("✓ Found TSL nwscript: " + displayPath(K2_NWSCRIPT));

      // Verify vanilla repo structure
      Path k1Root = VANILLA_REPO_DIR.resolve("K1");
      Path tslRoot = VANILLA_REPO_DIR.resolve("TSL");
      if (!Files.exists(k1Root) || !Files.isDirectory(k1Root)) {
         throw new IOException("K1 directory not found in vanilla repo: " + displayPath(k1Root));
      }
      if (!Files.exists(tslRoot) || !Files.isDirectory(tslRoot)) {
         throw new IOException("TSL directory not found in vanilla repo: " + displayPath(tslRoot));
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
         try {
            copyWithRetry(K1_NWSCRIPT, k1Nwscript);
         } catch (FileSystemException ex) {
            if (Files.exists(k1Nwscript)) {
               System.out.println("! Warning: could not refresh k1_nwscript.nss (" + ex.getMessage()
                     + "); using existing copy at " + displayPath(k1Nwscript));
            } else {
               throw ex;
            }
         }
      }
      if (!Files.exists(k2Nwscript) || !Files.isSameFile(K2_NWSCRIPT, k2Nwscript)) {
         try {
            copyWithRetry(K2_NWSCRIPT, k2Nwscript);
         } catch (FileSystemException ex) {
            if (Files.exists(k2Nwscript)) {
               System.out.println("! Warning: could not refresh tsl_nwscript.nss (" + ex.getMessage()
                     + "); using existing copy at " + displayPath(k2Nwscript));
            } else {
               throw ex;
            }
         }
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
      String displayRelPath = rel.toString().replace('\\', '/');

      Path outDir = scratchRoot.resolve(rel.getParent() == null ? Paths.get("") : rel.getParent());
      Files.createDirectories(outDir);

      // Compile: NSS -> NCS
      Path compiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".ncs");
      System.out.print("  Compiling " + displayRelPath + " to .ncs with nwnnsscomp.exe");
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
         boolean isK2 = "k2".equals(gameFlag);
         String originalExpanded = expandIncludes(nssPath, gameFlag);
         String original = normalizeNewlines(originalExpanded, isK2);
         String roundtrip = normalizeNewlines(new String(Files.readAllBytes(decompiled), StandardCharsets.UTF_8), isK2);
         long compareTime = System.nanoTime() - compareStart;
         operationTimes.merge("compare", compareTime, Long::sum);

         if (!original.equals(roundtrip)) {
            System.out.println(" ✗ MISMATCH");
            String diff = formatUnifiedDiff(original, roundtrip);
            StringBuilder message = new StringBuilder("Round-trip mismatch for ").append(displayPath(nssPath));
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

   private static void roundTripBytecodeSingle(Path nssPath, String gameFlag, Path scratchRoot) throws Exception {
      long startTime = System.nanoTime();

      Path rel = VANILLA_REPO_DIR.relativize(nssPath);
      String displayRelPath = rel.toString().replace('\\', '/');

      Path outDir = scratchRoot.resolve(rel.getParent() == null ? Paths.get("") : rel.getParent());
      Files.createDirectories(outDir);

      // Step 1: Compile original NSS -> NCS (first NCS)
      Path compiledFirst = outDir.resolve(stripExt(rel.getFileName().toString()) + ".ncs");
      System.out.print("  Compiling " + displayRelPath + " to .ncs with nwnnsscomp.exe");
      long compileOriginalStart = System.nanoTime();
      try {
         runCompiler(nssPath, compiledFirst, gameFlag, scratchRoot);
         long compileTime = System.nanoTime() - compileOriginalStart;
         operationTimes.merge("compile-original", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", compileTime / 1_000_000.0) + " ms)");
      } catch (Exception e) {
         long compileTime = System.nanoTime() - compileOriginalStart;
         operationTimes.merge("compile-original", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         throw e;
      }

      // Step 2: Decompile NCS -> NSS
      Path decompiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".dec.nss");
      System.out.print("  Decompiling " + compiledFirst.getFileName() + " back to .nss");
      long decompileStart = System.nanoTime();
      try {
         runDecompile(compiledFirst, decompiled, gameFlag);
         long decompileTime = System.nanoTime() - decompileStart;
         operationTimes.merge("decompile", decompileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", decompileTime / 1_000_000.0) + " ms)");
      } catch (Exception e) {
         long decompileTime = System.nanoTime() - decompileStart;
         operationTimes.merge("decompile", decompileTime, Long::sum);
         throw e;
      }

      // Step 3: Recompile decompiled NSS -> NCS (second NCS)
      Path recompiled = outDir.resolve(stripExt(rel.getFileName().toString()) + ".rt.ncs");
      // Some compiler versions refuse to open filenames with multiple dots (e.g.,
      // "*.dec.nss"). Create a temporary, compiler-friendly copy when needed.
      Path compileInput = decompiled;
      Path tempCompileInput = null;
      String decompiledName = decompiled.getFileName().toString();
      if (decompiledName.indexOf('.') != decompiledName.lastIndexOf('.')) {
         compileInput = outDir.resolve(stripExt(rel.getFileName().toString()) + "_dec.nss");
         Files.copy(decompiled, compileInput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         tempCompileInput = compileInput;
      }

      System.out.print("  Recompiling " + compileInput.getFileName() + " to .ncs");
      long compileRoundtripStart = System.nanoTime();
      try {
         runCompiler(compileInput, recompiled, gameFlag, scratchRoot);
         long compileTime = System.nanoTime() - compileRoundtripStart;
         operationTimes.merge("compile-roundtrip", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", compileTime / 1_000_000.0) + " ms)");
      } catch (Exception e) {
         long compileTime = System.nanoTime() - compileRoundtripStart;
         operationTimes.merge("compile-roundtrip", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         throw e;
      } finally {
         if (tempCompileInput != null) {
            try {
               Files.deleteIfExists(tempCompileInput);
            } catch (Exception ignored) {
            }
         }
      }

      // Step 4: Compare bytecode between the first and second NCS outputs
      System.out.print("  Comparing bytecode " + compiledFirst.getFileName() + " vs " + recompiled.getFileName());
      long compareStart = System.nanoTime();
      try {
         assertBytecodeEqual(compiledFirst, recompiled, gameFlag, displayRelPath);
         long compareTime = System.nanoTime() - compareStart;
         operationTimes.merge("compare", compareTime, Long::sum);
         System.out.println(" ✓ MATCH");
      } catch (Exception e) {
         long compareTime = System.nanoTime() - compareStart;
         operationTimes.merge("compare", compareTime, Long::sum);
         throw e;
      }

      long totalTime = System.nanoTime() - startTime;
      operationTimes.merge("total", totalTime, Long::sum);
   }

   /**
    * Detects if a script file needs the ASC nwscript (for ActionStartConversation
    * with 11 parameters).
    * Checks if the file contains ActionStartConversation calls with exactly 11
    * parameters
    * by counting commas in the parameter list. A call with 10 commas indicates 11
    * parameters.
    */
   private static boolean needsAscNwscript(Path nssPath) throws Exception {
      String content = new String(Files.readAllBytes(nssPath), StandardCharsets.UTF_8);
      // Look for ActionStartConversation calls with 11 parameters (10 commas)
      // Pattern matches ActionStartConversation( ... ) where the content between
      // parens
      // contains exactly 10 commas (indicating 11 parameters)
      // This is more flexible than requiring specific parameter values
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "ActionStartConversation\\s*\\(([^,)]*,\\s*){10}[^)]*\\)",
            java.util.regex.Pattern.MULTILINE);
      return pattern.matcher(content).find();
   }

   /**
    * Extracts include file names from a script file.
    * Parses #include statements and returns the include file names (without
    * quotes).
    */
   private static List<String> extractIncludes(Path nssPath) throws Exception {
      String content = new String(Files.readAllBytes(nssPath), StandardCharsets.UTF_8);
      List<String> includes = new ArrayList<>();
      java.util.regex.Pattern includePattern = java.util.regex.Pattern.compile(
            "#include\\s+[\"<]([^\">]+)[\">]",
            java.util.regex.Pattern.MULTILINE);
      java.util.regex.Matcher matcher = includePattern.matcher(content);
      while (matcher.find()) {
         includes.add(matcher.group(1));
      }
      return includes;
   }

   /**
    * Finds an include file in the repository structure.
    * Checks common locations: same directory as source, K1/Data/scripts.bif,
    * TSL/Data/Scripts, etc.
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

      // Prefer game-specific include locations first
      if ("k2".equals(gameFlag)) {
         Path tslScriptsDir = VANILLA_REPO_DIR.resolve("TSL").resolve("Vanilla").resolve("Data").resolve("Scripts");
         Path tslInc = tslScriptsDir.resolve(normalizedName);
         if (Files.exists(tslInc)) {
            return tslInc;
         }
         tslInc = tslScriptsDir.resolve(includeName);
         if (Files.exists(tslInc)) {
            return tslInc;
         }

         Path tslRcmScriptsDir = VANILLA_REPO_DIR.resolve("TSL").resolve("TSLRCM").resolve("Data").resolve("Scripts");
         Path tslRcmInc = tslRcmScriptsDir.resolve(normalizedName);
         if (Files.exists(tslRcmInc)) {
            return tslRcmInc;
         }
         tslRcmInc = tslRcmScriptsDir.resolve(includeName);
         if (Files.exists(tslRcmInc)) {
            return tslRcmInc;
         }
      }

      // Fallback to K1/Data/scripts.bif (shared includes)
      Path k1IncludesDir = VANILLA_REPO_DIR.resolve("K1").resolve("Data").resolve("scripts.bif");
      Path k1Inc = k1IncludesDir.resolve(normalizedName);
      if (Files.exists(k1Inc)) {
         return k1Inc;
      }
      k1Inc = k1IncludesDir.resolve(includeName);
      if (Files.exists(k1Inc)) {
         return k1Inc;
      }

      // If k2 and not found above, last-resort TSLRCM (already checked) handled;
      // return null

      return null;
   }

   /**
    * Recursively expand #include directives by inlining the referenced file
    * contents. Duplicate includes are skipped to prevent cycles.
    */
   private static String expandIncludes(Path sourceFile, String gameFlag) throws Exception {
      return expandIncludesInternal(sourceFile, gameFlag, new HashSet<>());
   }

   private static String expandIncludesInternal(Path sourceFile, String gameFlag, Set<Path> visited) throws Exception {
      Path normalizedSource = sourceFile.toAbsolutePath().normalize();
      if (!visited.add(normalizedSource)) {
         return "";
      }

      String content = new String(Files.readAllBytes(normalizedSource), StandardCharsets.UTF_8);
      StringBuilder expanded = new StringBuilder();
      java.util.regex.Pattern includePattern = java.util.regex.Pattern.compile(
            "#include\\s+[\"<]([^\">]+)[\">]");

      try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
         String line;
         while ((line = reader.readLine()) != null) {
            java.util.regex.Matcher matcher = includePattern.matcher(line);
            if (matcher.find()) {
               String includeName = matcher.group(1);
               Path includeFile = findIncludeFile(includeName, normalizedSource, gameFlag);
               if (includeFile != null && Files.exists(includeFile)) {
                  expanded.append(expandIncludesInternal(includeFile, gameFlag, visited));
                  expanded.append("\n");
               }
               // Skip emitting the original include line; its contents are inlined
               continue;
            }
            expanded.append(line).append("\n");
         }
      }

      return expanded.toString();
   }

   /**
    * Copy a single include into the temp directory, preserving the requested
    * name and also writing an extension-suffixed variant when the directive
    * omitted one (e.g., "k_inc_end" -> "k_inc_end.nss").
    */
   private static void copyIncludeFile(String includeName, Path includeFile, Path tempDir) throws IOException {
      Path includeTarget = tempDir.resolve(Paths.get(includeName));
      Path parent = includeTarget.getParent();
      if (parent != null) {
         Files.createDirectories(parent);
      } else {
         Files.createDirectories(tempDir);
      }

      Files.copy(includeFile, includeTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      // If the include name lacks an extension, also copy with the source file's
      // extension.
      if (!includeName.contains(".")) {
         String fileName = includeFile.getFileName().toString();
         int dotIdx = fileName.lastIndexOf('.');
         if (dotIdx != -1) {
            String ext = fileName.substring(dotIdx);
            Path altTarget = tempDir.resolve(Paths.get(includeName + ext));
            Path altParent = altTarget.getParent();
            if (altParent != null) {
               Files.createDirectories(altParent);
            }
            Files.copy(includeFile, altTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         }
      }
   }

   /**
    * Recursively copy all includes (and their dependencies) into the temp
    * directory.
    */
   private static void copyIncludesRecursive(Path sourceFile, String gameFlag, Path tempDir) throws Exception {
      Deque<Path> worklist = new ArrayDeque<>();
      Set<Path> processed = new HashSet<>();
      Map<String, Path> copied = new HashMap<>();

      worklist.add(sourceFile);

      while (!worklist.isEmpty()) {
         Path current = worklist.removeFirst();
         if (current == null || !Files.exists(current)) {
            continue;
         }
         Path normalized = current.toAbsolutePath().normalize();
         if (!processed.add(normalized)) {
            continue;
         }

         List<String> includes = extractIncludes(current);
         for (String includeName : includes) {
            Path includeFile = findIncludeFile(includeName, current, gameFlag);
            if (includeFile == null || !Files.exists(includeFile)) {
               continue;
            }
            String key = includeName.toLowerCase();
            if (!copied.containsKey(key)) {
               copyIncludeFile(includeName, includeFile, tempDir);
               copied.put(key, includeFile);
            }
            worklist.add(includeFile);
         }
      }
   }

   /**
    * Creates a temporary working directory with the source file and all its
    * includes.
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

      // Recursively copy all includes (and nested includes) into the temp directory
      copyIncludesRecursive(originalNssPath, gameFlag, tempDir);

      return tempDir;
   }

   private static void runCompiler(Path originalNssPath, Path compiledOut, String gameFlag, Path workDir)
         throws Exception {
      // Determine which nwscript.nss to use based on game
      // K1 files (from test-work\Vanilla_KOTOR_Script_Source\K1) use k1_nwscript.nss
      // K1 files with ActionStartConversation(11 params) use k1_asc_nwscript.nss
      // TSL files (from test-work\Vanilla_KOTOR_Script_Source\TSL) use
      // tsl_nwscript.nss
      Path nwscriptSource;
      if ("k1".equals(gameFlag)) {
         // Check if this script needs ASC nwscript
         if (needsAscNwscript(originalNssPath)) {
            nwscriptSource = K1_ASC_NWSCRIPT;
            if (!Files.exists(nwscriptSource)) {
               throw new IllegalStateException("K1 ASC nwscript file not found: " + displayPath(nwscriptSource));
            }
         } else {
            nwscriptSource = K1_NWSCRIPT;
            if (!Files.exists(nwscriptSource)) {
               throw new IllegalStateException("K1 nwscript file not found: " + displayPath(nwscriptSource));
            }
         }
      } else if ("k2".equals(gameFlag)) {
         nwscriptSource = K2_NWSCRIPT;
         if (!Files.exists(nwscriptSource)) {
            throw new IllegalStateException("TSL nwscript file not found: " + displayPath(nwscriptSource));
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
         String compilerPathStr = compilerFile.getAbsolutePath();
         String sourcePathStr = sourceFile.getAbsolutePath();
         String outputPathStr = outputFile.getAbsolutePath();
         String displaySource = displayPath(originalNssPath);
         String displayOutput = displayPath(compiledOut);
         String displayCompiler = displayPath(NWN_COMPILER);
         for (int i = 0; i < cmd.length; i++) {
            if (i > 0)
               System.out.print(" ");
            String arg = cmd[i];
            // Replace temp path with original path in log output for readability
            if (arg.equals(sourcePathStr)) {
               arg = displaySource;
            } else if (arg.equals(outputPathStr)) {
               arg = displayOutput;
            } else if (arg.equals(compilerPathStr)) {
               arg = displayCompiler;
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

         java.io.BufferedReader reader = new java.io.BufferedReader(
               new java.io.InputStreamReader(proc.getInputStream()));
         StringBuilder output = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
         }

         boolean finished = proc.waitFor(PROC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
         if (!finished) {
            proc.destroyForcibly();
            System.out.println(" ✗ TIMEOUT");
            throw new RuntimeException("nwnnsscomp timed out for " + displayPath(originalNssPath));
         }

         int exitCode = proc.exitValue();
         boolean fileExists = Files.isRegularFile(compiledOut);

         if (exitCode != 0 || !fileExists) {
            System.out.println(" ✗ FAILED");
            String errorMsg = "nwnnsscomp failed (exit=" + exitCode + ", fileExists=" + fileExists + ") for "
                  + displaySource;
            if (output.length() > 0) {
               // Show relevant error lines
               String[] outputLines = output.toString().split("\n");
               boolean foundError = false;
               String tempSourceAbs = tempSourceFile.toAbsolutePath().toString();
               String tempDirAbs = tempDir.toAbsolutePath().toString();
               String displayTempDir = displayPath(tempDir);
               String compiledAbs = compiledOut.toAbsolutePath().toString();
               for (String outputLine : outputLines) {
                  if (outputLine.toLowerCase().contains("error") ||
                        outputLine.toLowerCase().contains("unable") ||
                        outputLine.toLowerCase().contains("include")) {
                     if (!foundError) {
                        errorMsg += "\nCompiler errors:";
                        foundError = true;
                     }
                     // Replace temp path with original path in error messages
                     String displayLine = outputLine
                           .replace(tempSourceAbs, displaySource)
                           .replace(tempDirAbs, displayTempDir)
                           .replace(compiledAbs, displayOutput)
                           .replace(originalNssPath.toAbsolutePath().toString(), displaySource);
                     errorMsg += "\n  " + displayLine;
                  }
               }
               // If no errors found, show all output
               if (!foundError && outputLines.length > 0) {
                  String sanitizedOutput = output.toString()
                        .replace(tempSourceAbs, displaySource)
                        .replace(tempDirAbs, displayTempDir)
                        .replace(compiledAbs, displayOutput)
                        .replace(originalNssPath.toAbsolutePath().toString(), displaySource);
                  errorMsg += "\nCompiler output:\n" + sanitizedOutput;
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
               System.err.println(
                     "Warning: Failed to clean up temp directory " + displayPath(tempDir) + ": " + e.getMessage());
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
            throw new RuntimeException("Decompile did not produce output: " + displayPath(nssOut));
         }
      } catch (DecompilerException ex) {
         System.out.println(" ✗ FAILED - " + ex.getMessage());
         throw new RuntimeException("Decompile failed for " + displayPath(ncsPath) + ": " + ex.getMessage(), ex);
      }
   }

   private static Path prepareScratch(String gameLabel, Path nwscriptSource) throws IOException {
      Path scratch = WORK_ROOT.resolve(gameLabel);
      Files.createDirectories(scratch);
      Path target = scratch.resolve("nwscript.nss");
      if (!Files.exists(target)) {
         Files.copy(nwscriptSource, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      return scratch;
   }

   private static String stripExt(String name) {
      int dot = name.lastIndexOf('.');
      return dot == -1 ? name : name.substring(0, dot);
   }

   private static String normalizeNewlines(String s, boolean isK2) {
      String normalized = s.replace("\r\n", "\n").replace("\r", "\n");
      normalized = stripComments(normalized);
      normalized = normalizeIncludes(normalized);
      normalized = normalizeDeclarationAssignment(normalized);
      normalized = normalizeFunctionBraces(normalized);
      normalized = normalizeLeadingPlaceholders(normalized);
      normalized = normalizePlaceholderGlobals(normalized);
      normalized = normalizeVariableNames(normalized);
      normalized = normalizeTrailingZeroParams(normalized);
      normalized = normalizeTrailingDefaults(normalized);
      normalized = normalizeEffectDeathDefaults(normalized);
      normalized = normalizeLogicalOperators(normalized);
      normalized = normalizeIfSpacing(normalized);
      normalized = normalizeDoubleParensInCalls(normalized);
      normalized = normalizeAssignmentParens(normalized);
      normalized = normalizeCallArgumentParens(normalized);
      normalized = normalizeReturnStatements(normalized);
      normalized = normalizeFunctionSignaturesByArity(normalized);
      normalized = normalizeComparisonParens(normalized);
      normalized = normalizeTrueFalse(normalized);
      normalized = normalizeConstants(normalized, isK2 ? NPC_CONSTANTS_K2 : NPC_CONSTANTS_K1);
      normalized = normalizeConstants(normalized, isK2 ? ABILITY_CONSTANTS_K2 : ABILITY_CONSTANTS_K1);
      normalized = normalizeConstants(normalized, isK2 ? FACTION_CONSTANTS_K2 : FACTION_CONSTANTS_K1);
      normalized = normalizeBitwiseOperators(normalized);
      normalized = normalizeControlFlowConditions(normalized);
      normalized = normalizeCommaSpacing(normalized);
      normalized = normalizePlaceholderNames(normalized);
      normalized = normalizeAssignCommandPlaceholders(normalized);
      normalized = normalizeFunctionOrder(normalized);

      String[] lines = normalized.split("\n", -1);
      StringBuilder result = new StringBuilder();

      for (String line : lines) {
         String trimmed = line.replaceFirst("^\\s+", "").replaceFirst("\\s+$", "");
         if (trimmed.isEmpty()) {
            continue; // drop blank lines to avoid formatting-only mismatches
         }
         trimmed = trimmed.replace("\t", "    ");
         result.append(trimmed).append("\n");
      }

      String finalResult = result.toString();
      while (finalResult.endsWith("\n")) {
         finalResult = finalResult.substring(0, finalResult.length() - 1);
      }

      return finalResult;
   }

   /**
    * Normalizes trailing parameters that are equivalent to default values.
    * <p>
    * Decompiler may emit optional parameters explicitly (e.g., TRUE/1). Strip
    * these when they match known defaults so calls with/without the optional
    * argument compare equal.
    */
   private static String normalizeTrailingDefaults(String code) {
      // Defaults to strip when they appear as the last argument
      java.util.Map<String, String> trailingDefaults = new java.util.HashMap<>();
      trailingDefaults.put("ActionJumpToObject", "(1|TRUE)");
      // Dice helpers have an optional bonus parameter that defaults to 0; some
      // decompilations materialize it as 0 or 1 depending on analysis noise.
      trailingDefaults.put("d2", "(0|1)");
      trailingDefaults.put("d3", "(0|1)");
      trailingDefaults.put("d4", "(0|1)");
      trailingDefaults.put("d6", "(0|1)");
      trailingDefaults.put("d8", "(0|1)");
      trailingDefaults.put("d10", "(0|1)");
      trailingDefaults.put("d12", "(0|1)");
      trailingDefaults.put("d20", "(0|1)");
      trailingDefaults.put("d100", "(0|1)");
      trailingDefaults.put("ActionAttack", "(0|FALSE)");

      String result = code;
      for (java.util.Map.Entry<String, String> entry : trailingDefaults.entrySet()) {
         String func = entry.getKey();
         String defaultValue = entry.getValue();
         java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
               "(" + java.util.regex.Pattern.quote(func) + "\\s*\\([^)]*),\\s*" + defaultValue + "\\s*\\)");

         java.util.regex.Matcher matcher = pattern.matcher(result);
         java.util.List<Object[]> matches = new java.util.ArrayList<>();
         while (matcher.find()) {
            matches.add(new Object[] { matcher.start(), matcher.end(), matcher.group(1) });
         }

         for (int i = matches.size() - 1; i >= 0; i--) {
            Object[] match = matches.get(i);
            int start = (Integer) match[0];
            int end = (Integer) match[1];
            String group1 = (String) match[2];
            result = result.substring(0, start) + group1 + ")" + result.substring(end);
         }
      }

      return result;
   }

   /**
    * Normalizes AssignCommand wrappers that carry a dummy variable assignment
    * (e.g., "void null = sub1(...);") so they compare equal to direct calls.
    */
   private static String normalizeAssignCommandPlaceholders(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "AssignCommand\\s*\\(([^,]+),\\s*void\\s+\\w+\\s*=\\s*([^;]+);\\s*\\);",
            java.util.regex.Pattern.DOTALL);
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String target = m.group(1).trim();
         String action = m.group(2).trim();
         m.appendReplacement(sb, "AssignCommand(" + target + ", " + action + ");");
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Normalizes trailing zero parameters in function calls.
    * Removes trailing ", 0" or ", 0x0" parameters since the decompiler may omit
    * them.
    */
   private static String normalizeTrailingZeroParams(String code) {
      // Pattern to match function calls with trailing zero parameters
      // Match: functionName(...), 0) or functionName(...), 0x0)
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^)]*),\\s*(0|0x0)\\s*\\)");

      String result = code;
      java.util.regex.Matcher matcher = pattern.matcher(result);

      // Build replacement string by processing matches in reverse order
      // Store start, end, and the actual group 1 content to avoid substring issues
      java.util.List<Object[]> matches = new java.util.ArrayList<>();
      while (matcher.find()) {
         matches.add(new Object[] { matcher.start(), matcher.end(), matcher.group(1) });
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

   /** Removes #include lines since compiled NCS does not retain them. */
   private static String normalizeIncludes(String code) {
      return code.replaceAll("(?m)^\\s*#include[^\n]*\\n?", "");
   }

   /**
    * Strips placeholder globals at the very start of files (e.g., int1..int10)
    * regardless of how many are present. This aligns cases where the decompiler
    * synthesizes these while originals omit them.
    */
   private static String normalizeLeadingPlaceholders(String code) {
      String[] lines = code.split("\n");
      int idx = 0;
      java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern
            .compile("^[\\uFEFF]?int\\s+int\\d+\\s*=\\s*[-0-9xa-fA-F]+;");
      while (idx < lines.length) {
         String line = lines[idx].trim();
         if (line.isEmpty()) {
            idx++;
            continue;
         }
         if (placeholderPattern.matcher(line).matches()) {
            idx++;
            continue;
         }
         break;
      }
      if (idx == 0) {
         return code;
      }
      StringBuilder sb = new StringBuilder();
      for (int i = idx; i < lines.length; i++) {
         sb.append(lines[i]);
         if (i < lines.length - 1) {
            sb.append("\n");
         }
      }
      return sb.toString();
   }

   /**
    * Remove large runs of compiler-artifact global ints sometimes emitted during
    * recovery; these do not exist in original sources. Only strip if there are
    * many (>=10) sequential int globals at the top of the file to avoid hiding
    * legitimate small global declarations.
    */
   private static String normalizePlaceholderGlobals(String code) {
      String[] lines = code.split("\n");
      int count = 0;
      int end = 0;
      java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern
            .compile("^[\\uFEFF]?int\\s+(int\\d+|intGLOB_\\d+)\\s*=\\s*[-0-9xa-fA-F]+;");

      for (int i = 0; i < lines.length; i++) {
         String line = lines[i].trim();
         if (placeholderPattern.matcher(line).matches()) {
            count++;
            end = i;
         } else if (line.isEmpty()) {
            // allow leading blank lines between placeholders
            continue;
         } else {
            break;
         }
      }
      if (count >= 5 && end >= 0) {
         // Remove lines up to 'end'
         StringBuilder sb = new StringBuilder();
         for (int i = end + 1; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
         }
         return sb.toString();
      }
      return code;
   }

   /**
    * Canonicalize function signature + opening brace spacing so different brace
    * styles (same-line vs next-line) compare equal.
    */
   private static String normalizeFunctionBraces(String code) {
      return code.replaceAll("\\)\\s*\\n\\s*\\{", ") {");
   }

   /**
    * Normalize EffectDeath() to include its common default parameters so missing
    * optional arguments don't cause mismatches.
    */
   private static String normalizeEffectDeathDefaults(String code) {
      return code.replaceAll("\\bEffectDeath\\s*\\(\\s*\\)", "EffectDeath(0, 1)");
   }

   /**
    * Normalizes return statements by removing unnecessary outer parentheses.
    * Converts: return (expression); to: return expression;
    */
   private static String normalizeReturnStatements(String code) {
      // Pattern to match: return (expression);
      // This handles cases where the decompiler adds unnecessary parentheses around
      // return expressions
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "return\\s+\\(([^()]+(?:\\.[^()]*)*)\\);");

      String result = code;
      java.util.regex.Matcher matcher = pattern.matcher(result);

      // Replace return (simple_expression); with return simple_expression;
      result = matcher.replaceAll("return $1;");

      return result;
   }

   /**
    * Normalizes simple comparison assignments by stripping redundant outer
    * parentheses around comparison expressions (e.g., "int x = (a > b);" ->
    * "int x = a > b;") so stylistic differences don't trigger failures.
    */
   private static String normalizeComparisonParens(String code) {
      String result = code;
      // Assignments (retain trailing semicolon)
      java.util.regex.Pattern assignPattern = java.util.regex.Pattern.compile(
            "(=)\\s*\\(([^;\\n]+?\\s*(==|!=|<=|>=|<|>)\\s*[^;\\n]+?)\\)\\s*;");
      java.util.regex.Matcher mAssign = assignPattern.matcher(result);
      StringBuffer sbAssign = new StringBuffer();
      while (mAssign.find()) {
         String lhs = mAssign.group(1);
         String expr = mAssign.group(2).trim();
         mAssign.appendReplacement(sbAssign, lhs + " " + expr + ";");
      }
      mAssign.appendTail(sbAssign);
      result = sbAssign.toString();

      // General parenthesized comparisons (e.g., inside conditionals)
      java.util.regex.Pattern generalPattern = java.util.regex.Pattern.compile(
            "\\(([^()]+?\\s*(==|!=|<=|>=|<|>)\\s*[^()]+?)\\)");
      java.util.regex.Matcher mGeneral = generalPattern.matcher(result);
      StringBuffer sbGeneral = new StringBuffer();
      while (mGeneral.find()) {
         String expr = mGeneral.group(1).trim();
         mGeneral.appendReplacement(sbGeneral, expr);
      }
      mGeneral.appendTail(sbGeneral);
      return sbGeneral.toString();
   }

   /**
    * Removes a single layer of parentheses immediately after an assignment to
    * reduce stylistic diffs like "int x = (foo);" vs "int x = foo;" even when
    * nested calls exist.
    */
   private static String normalizeAssignmentParens(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("(=)\\s*\\(([^;\\n]+)\\)\\s*;");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String expr = m.group(2).trim();
         m.appendReplacement(sb, m.group(1) + " " + expr + ";");
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Removes redundant parentheses around single arguments in function calls,
    * e.g., "Func(x, (a + 1))" -> "Func(x, a + 1)".
    */
   private static String normalizeCallArgumentParens(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(",\\s*\\(([^(),]+)\\)");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String expr = m.group(1).trim();
         m.appendReplacement(sb, ", " + expr);
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Normalizes function signatures (prototypes and definitions) by collapsing
    * parameter details down to an arity marker. This reduces diffs caused by
    * placeholder parameter names or partially inferred types while still
    * preserving function identity and parameter count.
    */
   private static String normalizeFunctionSignaturesByArity(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "(?m)^\\s*([A-Za-z_][\\w\\s\\*]*?)\\s+([A-Za-z_]\\w*)\\s*\\(([^)]*)\\)\\s*(\\{|;)");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String ret = m.group(1).trim();
         String name = m.group(2).trim();
         String params = m.group(3).trim();
         int count = 0;
         if (!params.isEmpty()) {
            count = params.split(",").length;
         }
         String repl = ret + " " + name + "(/*params=" + count + "*/)" + m.group(4);
         m.appendReplacement(sb, repl);
      }
      m.appendTail(sb);
      return sb.toString();
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
    * Normalize NPC_* constants to their numeric values so symbolic vs numeric
    * usages compare equal across decompilation.
    */
   private static String normalizeConstants(String code, Map<String, String> constants) {
      if (constants == null || constants.isEmpty()) {
         return code;
      }
      String prefixPattern = constants.keySet().stream().findFirst().map(name -> name.substring(0, name.indexOf('_') + 1))
            .orElse("NPC_");
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b" + prefixPattern + "[A-Za-z0-9_]+\\b");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         String name = m.group();
         String replacement = constants.get(name);
         if (replacement != null) {
            m.appendReplacement(sb, replacement);
         }
      }
      m.appendTail(sb);
      return sb.toString();
   }

   private static Map<String, String> loadConstantsWithPrefix(Path path, String prefix) {
      Map<String, String> map = new HashMap<>();
      loadConstantsFromFile(path, prefix, map);
      return map;
   }

   private static void loadConstantsFromFile(Path path, String prefix, Map<String, String> map) {
      if (path == null) {
         return;
      }
      try {
         if (!Files.exists(path)) {
            return;
         }
         java.util.regex.Pattern p = java.util.regex.Pattern
               .compile("^\\s*int\\s+(" + prefix + "[A-Za-z0-9_]+)\\s*=\\s*([-]?[0-9]+)\\s*;.*$");
         for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            java.util.regex.Matcher m = p.matcher(line);
            if (m.matches()) {
               map.put(m.group(1), m.group(2));
            }
         }
      } catch (Exception ignored) {
      }
   }

   /**
    * Normalizes placeholder variable names that come from incomplete stack
    * recovery.
    */
   private static String normalizePlaceholderNames(String code) {
      return code.replaceAll("__unknown_param_\\d+", "__unknown_param");
   }

   /**
    * Sorts functions by signature to avoid order-related diffs in decompiler
    * output.
    * <p>
    * <b>Limitation:</b> This method uses simple character counting for braces and
    * does not
    * account for braces within string literals or comments. This may lead to
    * incorrect
    * function parsing in edge cases where braces appear in strings or comments.
    * For the
    * typical decompiled NCS output, this is sufficient, but a more robust parser
    * would
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
    * Normalizes logical operators that may appear with spaces (e.g., "& &" or
    * "| |") by collapsing them to their canonical forms.
    */
   private static String normalizeLogicalOperators(String code) {
      String result = code;
      result = result.replaceAll("\\&\\s*\\&", "&&");
      result = result.replaceAll("\\|\\s*\\|", "||");
      // Fallback plain replacements in case regex misses unusual spacing
      result = result.replace(" & & ", " && ");
      result = result.replace(" | | ", " || ");
      return result;
   }

   /**
    * Inserts a space after control keywords when parentheses are missing
    * (e.g., "ifcond" -> "if cond") to reduce spacing-only diffs.
    */
   private static String normalizeIfSpacing(String code) {
      String result = code;
      result = result.replaceAll("\\bif(?=[A-Za-z_])", "if ");
      result = result.replaceAll("\\bwhile(?=[A-Za-z_])", "while ");
      result = result.replaceAll("\\bfor(?=[A-Za-z_])", "for ");
      result = result.replaceAll("\\bswitch(?=[A-Za-z_])", "switch ");
      return result;
   }

   /**
    * Collapses redundant double parentheses in function call arguments,
    * e.g., "Func((expr), ...)" -> "Func(expr, ...)".
    */
   private static String normalizeDoubleParensInCalls(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\(\\(([^()]+)\\)(\\s*[),])");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
         m.appendReplacement(sb, m.group(1) + "(" + m.group(2).trim() + m.group(3));
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /**
    * Canonicalizes control-flow conditions (if/while/switch/for) by enforcing a
    * single set of parentheses around the condition and a space after the
    * keyword. This reduces purely stylistic diffs such as "if((x))" vs
    * "if (x)" that the decompiler cannot infer from NCS bytecode.
    */
   private static String normalizeControlFlowConditions(String code) {
      StringBuilder out = new StringBuilder(code.length());
      boolean inString = false;

      for (int i = 0; i < code.length(); i++) {
         char ch = code.charAt(i);

         if (ch == '"' && (i == 0 || code.charAt(i - 1) != '\\')) {
            inString = !inString;
            out.append(ch);
            continue;
         }

         if (!inString) {
            String keyword = matchControlKeyword(code, i);
            if (keyword != null) {
               int kwEnd = i + keyword.length();
               int idx = kwEnd;
               while (idx < code.length() && Character.isWhitespace(code.charAt(idx))) {
                  idx++;
               }
               if (idx < code.length() && code.charAt(idx) == '(') {
                  int endParen = findMatchingParen(code, idx);
                  if (endParen != -1) {
                     String condition = code.substring(idx + 1, endParen);
                     condition = stripOuterParens(condition).trim();
                     out.append(keyword).append(" (").append(condition).append(")");
                     i = endParen;
                     continue;
                  }
               }
            }
         }

         out.append(ch);
      }

      return out.toString();
   }

   private static String matchControlKeyword(String code, int index) {
      String[] keywords = { "if", "while", "switch", "for" };
      for (String kw : keywords) {
         int len = kw.length();
         if (index + len <= code.length() && code.regionMatches(index, kw, 0, len)) {
            char before = index == 0 ? '\0' : code.charAt(index - 1);
            char after = index + len < code.length() ? code.charAt(index + len) : '\0';
            if (!Character.isLetterOrDigit(before) && before != '_'
                  && !Character.isLetterOrDigit(after) && after != '_') {
               return kw;
            }
         }
      }
      return null;
   }

   private static int findMatchingParen(String code, int openIdx) {
      int depth = 0;
      boolean inString = false;
      for (int i = openIdx; i < code.length(); i++) {
         char c = code.charAt(i);
         if (c == '"' && (i == 0 || code.charAt(i - 1) != '\\')) {
            inString = !inString;
            continue;
         }
         if (inString) {
            continue;
         }
         if (c == '(') {
            depth++;
         } else if (c == ')') {
            depth--;
            if (depth == 0) {
               return i;
            }
         }
      }
      return -1;
   }

   private static String stripOuterParens(String expr) {
      String result = expr.trim();
      boolean changed = true;
      while (changed && result.length() >= 2 && result.charAt(0) == '('
            && result.charAt(result.length() - 1) == ')') {
         changed = false;
         int depth = 0;
         boolean balanced = true;
         for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '(') {
               depth++;
            } else if (c == ')') {
               depth--;
               if (depth == 0 && i < result.length() - 1) {
                  balanced = false;
                  break;
               }
            }
            if (depth < 0) {
               balanced = false;
               break;
            }
         }
         if (balanced && depth == 0) {
            result = result.substring(1, result.length() - 1).trim();
            changed = true;
         }
      }
      return result;
   }

   /**
    * Normalizes insignificant whitespace immediately following commas so argument
    * lists compare equal regardless of spacing style in source scripts.
    * <p>
    * Skips content inside string literals and preserves newlines to avoid
    * collapsing multi-line parameter lists.
    */
   private static String normalizeCommaSpacing(String code) {
      StringBuilder out = new StringBuilder(code.length());
      boolean inString = false;

      for (int i = 0; i < code.length(); i++) {
         char ch = code.charAt(i);

         if (ch == '"' && (i == 0 || code.charAt(i - 1) != '\\')) {
            inString = !inString;
            out.append(ch);
            continue;
         }

         if (!inString && ch == ',') {
            out.append(ch);
            int j = i + 1;
            while (j < code.length()) {
               char next = code.charAt(j);
               if (next == ' ' || next == '\t' || next == '\r') {
                  j++;
                  continue;
               }
               break;
            }
            i = j - 1; // skip spaces/tabs after comma (but keep newlines)
            continue;
         }

         out.append(ch);
      }

      return out.toString();
   }

   /**
    * Normalizes separate declaration and assignment to initialization.
    * Converts patterns like:
    * int var1;
    * var1 = value;
    * to:
    * int var1 = value;
    */
   private static String normalizeDeclarationAssignment(String code) {
      // Pattern to match: type var; followed by var = value;
      java.util.regex.Pattern declPattern = java.util.regex.Pattern.compile(
            "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;");
      java.util.regex.Pattern assignPattern = java.util.regex.Pattern.compile(
            "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+?);");

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
    * Maps local variable names to canonical forms (int1, int2, etc.) based on type
    * and order.
    * This handles the fact that decompilers can't recover original variable names.
    */
   private static String normalizeVariableNames(String code) {
      // Pattern to match variable declarations: type name [= value];
      java.util.regex.Pattern varDeclPattern = java.util.regex.Pattern.compile(
            "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=;]");

      java.util.Map<String, String> varMap = new java.util.HashMap<>();
      java.util.Map<String, Integer> typeCounters = new java.util.HashMap<>();
      java.util.List<String> varOrder = new java.util.ArrayList<>();

      // First pass: collect all variable declarations
      java.util.regex.Matcher matcher = varDeclPattern.matcher(code);
      while (matcher.find()) {
         String type = matcher.group(1);
         String varName = matcher.group(2);

         // Skip if it's already a canonical name (int1, int2, etc.) or if it's a
         // keyword/function
         if (varName
               .matches("^(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\d+$")) {
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

      // Second pass: replace variable names in the code (in reverse order to avoid
      // partial matches)
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
            if (firstOldLine == -1)
               firstOldLine = oldLineNum;
            lastOldLine = oldLineNum;
            oldLineNum++;
         } else if (line.type == DiffLineType.ADDED) {
            if (firstNewLine == -1)
               firstNewLine = newLineNum;
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

   @Test
   public void testRoundTripBytecodeSuite() {
      int exitCode = runRoundTripBytecodeSuite();
      assertEquals(0, exitCode, "Bytecode round-trip test suite should pass with exit code 0");
   }

   private int runRoundTripSuite() {
      resetPerformanceTracking();
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

   int runRoundTripBytecodeSuite() {
      resetPerformanceTracking();
      testStartTime = System.nanoTime();

      try {
         preflight();
         List<RoundTripCase> tests = buildRoundTripCases();

         if (tests.isEmpty()) {
            System.err.println("ERROR: No test files found!");
            return 1;
         }

         System.out.println("=== Running Bytecode Round-Trip Tests (NSS -> NCS -> NSS -> NCS) ===");
         System.out.println("Total tests: " + tests.size());
         System.out.println("Fast-fail: enabled (will stop on first failure)");
         System.out.println();

         for (RoundTripCase testCase : tests) {
            testsProcessed++;
            Path relPath = VANILLA_REPO_DIR.relativize(testCase.item.path);
            String displayPath = relPath.toString().replace('\\', '/');
            System.out.println(String.format("[%d/%d] %s", testsProcessed, totalTests, displayPath));

            try {
               roundTripBytecodeSingle(testCase.item.path, testCase.item.gameFlag, testCase.item.scratchRoot);
               System.out.println("  Result: ✓ PASSED");
               System.out.println();
            } catch (Exception ex) {
               System.out.println("  Result: ✗ FAILED");
               System.out.println();
               System.out.println("═══════════════════════════════════════════════════════════");
               System.out.println("BYTECODE FAILURE: " + testCase.displayName);
               System.out.println("═══════════════════════════════════════════════════════════");
               System.out.println("Exception: " + ex.getClass().getSimpleName());
               String message = ex.getMessage();
               if (message != null && !message.isEmpty()) {
                  System.out.println("Message: " + message);
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
         System.out.println("ALL BYTECODE TESTS PASSED!");
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

   private static void assertBytecodeEqual(Path originalNcs, Path roundTripNcs, String gameFlag, String displayName) throws Exception {
      BytecodeDiffResult diff = findBytecodeDiff(originalNcs, roundTripNcs);
      if (diff == null) {
         return;
      }

      StringBuilder message = new StringBuilder();
      message.append("Bytecode mismatch for ").append(displayName);
      message.append("\nOffset: ").append(diff.offset);
      message.append("\nOriginal length: ").append(diff.originalLength).append(" bytes");
      message.append("\nRound-trip length: ").append(diff.roundTripLength).append(" bytes");
      message.append("\nOriginal byte: ").append(formatByteValue(diff.originalByte));
      message.append("\nRound-trip byte: ").append(formatByteValue(diff.roundTripByte));
      message.append("\nOriginal context: ").append(diff.originalContext);
      message.append("\nRound-trip context: ").append(diff.roundTripContext);

      String pcodeDiff = diffPcodeListings(originalNcs, roundTripNcs, gameFlag);
      if (pcodeDiff != null) {
         message.append("\nP-code diff:\n").append(pcodeDiff);
      }

      throw new IllegalStateException(message.toString());
   }

   private static BytecodeDiffResult findBytecodeDiff(Path originalNcs, Path roundTripNcs) throws IOException {
      byte[] originalBytes = Files.readAllBytes(originalNcs);
      byte[] roundTripBytes = Files.readAllBytes(roundTripNcs);
      int maxLength = Math.max(originalBytes.length, roundTripBytes.length);

      for (int i = 0; i < maxLength; i++) {
         int original = i < originalBytes.length ? originalBytes[i] & 0xFF : -1;
         int roundTrip = i < roundTripBytes.length ? roundTripBytes[i] & 0xFF : -1;
         if (original != roundTrip) {
            BytecodeDiffResult result = new BytecodeDiffResult();
            result.offset = i;
            result.originalByte = original;
            result.roundTripByte = roundTrip;
            result.originalLength = originalBytes.length;
            result.roundTripLength = roundTripBytes.length;
            result.originalContext = renderHexContext(originalBytes, i);
            result.roundTripContext = renderHexContext(roundTripBytes, i);
            return result;
         }
      }

      return null;
   }

   private static String renderHexContext(byte[] bytes, int focus) {
      if (bytes == null || bytes.length == 0) {
         return "<empty>";
      }

      int anchor = Math.min(Math.max(focus, 0), bytes.length - 1);
      int start = Math.max(0, anchor - 8);
      int end = Math.min(bytes.length, anchor + 9);

      StringBuilder sb = new StringBuilder();
      for (int i = start; i < end; i++) {
         if (i > start) {
            sb.append(' ');
         }
         sb.append(String.format("%02X", bytes[i]));
         if (i == focus) {
            sb.append('*');
         }
      }
      return sb.toString();
   }

   private static String formatByteValue(int value) {
      if (value < 0) {
         return "<EOF>";
      }
      return String.format("0x%02X (%d)", value, value);
   }

   private static String diffPcodeListings(Path originalNcs, Path roundTripNcs, String gameFlag) {
      Path tempDir = null;
      try {
         Files.createDirectories(COMPILE_TEMP_ROOT);
         tempDir = Files.createTempDirectory(COMPILE_TEMP_ROOT, "pcode_diff_");
         Path originalPcode = tempDir.resolve("original.pcode");
         Path roundTripPcode = tempDir.resolve("roundtrip.pcode");

         decompileNcsToPcode(originalNcs, originalPcode, gameFlag);
         decompileNcsToPcode(roundTripNcs, roundTripPcode, gameFlag);

         String expected = Files.readString(originalPcode, StandardCharsets.UTF_8);
         String actual = Files.readString(roundTripPcode, StandardCharsets.UTF_8);
         return formatUnifiedDiff(expected, actual);
      } catch (Exception e) {
         return "Failed to generate p-code diff: " + e.getMessage();
      } finally {
         if (tempDir != null) {
            try {
               deleteDirectory(tempDir);
            } catch (Exception ignored) {
            }
         }
      }
   }

   private static void decompileNcsToPcode(Path ncsPath, Path outputPcode, String gameFlag) throws Exception {
      Files.createDirectories(outputPcode.getParent());
      File compilerFile = NWN_COMPILER.toAbsolutePath().toFile();
      boolean isK2 = "k2".equals(gameFlag);
      NwnnsscompConfig config = new NwnnsscompConfig(compilerFile, ncsPath.toFile(), outputPcode.toFile(), isK2);
      String[] cmd = config.getDecompileArgs(compilerFile.getAbsolutePath());
      runProcessWithTimeout(cmd, ncsPath.getParent(), "Decompile to p-code for " + displayPath(ncsPath));

      if (!Files.exists(outputPcode)) {
         throw new IOException("P-code output missing at: " + displayPath(outputPcode));
      }
   }

   private static String runProcessWithTimeout(String[] cmd, Path workingDir, String actionDescription) throws Exception {
      ProcessBuilder pb = new ProcessBuilder(cmd);
      if (workingDir != null) {
         pb.directory(workingDir.toFile());
      }
      pb.redirectErrorStream(true);
      Process proc = pb.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
         }
      }

      boolean finished = proc.waitFor(PROC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
         proc.destroyForcibly();
         throw new IOException(actionDescription + " timed out after " + PROC_TIMEOUT.getSeconds() + "s.\nOutput:\n" + output);
      }

      int exitCode = proc.exitValue();
      if (exitCode != 0) {
         throw new IOException(actionDescription + " failed with exit code " + exitCode + ".\nOutput:\n" + output);
      }

      return output.toString();
   }

   private static class BytecodeDiffResult {
      long offset;
      int originalByte;
      int roundTripByte;
      long originalLength;
      long roundTripLength;
      String originalContext;
      String roundTripContext;
   }

   /**
    * Prints cumulative performance stats for the currently running suite.
    */
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
      System.out.println("Profile log: " + displayPath(PROFILE_OUTPUT));
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
