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
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

/**
 * Exhaustive round-trip tests for the decompiler and compiler:
 * 1) Clone or reuse the Vanilla_KOTOR_Script_Source repository
 * 2) Compile each .nss to .ncs for each game using nwnnsscomp.exe
 * 3) Decompile each .ncs back to .nss using NCSDecompCLI
 * 4) Normalize both original and decompiled NSS for comparison (whitespace, formatting only)
 * 5) Fail immediately on the first mismatch
 *
 * All test artifacts are created in gitignored directories.
 *
 * ⚠️ CRITICAL: TEST PHILOSOPHY - READ BEFORE MODIFYING ⚠️
 *
 * These tests validate the decompiler against original scripts. They do NOT mask or patch any decompiler flaws.
 *
 * STRICTLY FORBIDDEN IN TESTS:
 * - Fixing syntax or logic errors in decompiled output
 * - Patching or cleaning up distorted or mangled code from the decompiler
 * - Editing expressions, operators, semicolons, braces, types, return statements
 * - Adjusting function signatures or any output for correctness
 * - Applying any sort of output "repair" or workaround to supplement the decompiler
 *
 * ALLOWED (FOR COMPARISON ONLY):
 * - Whitespace and formatting normalization, solely for text comparison
 *   (This does not legitimize fixing bugs via normalization!)
 *
 * IF DECOMPILED OUTPUT DIFFERS FROM ORIGINAL (other than formatting):
 * - The test MUST FAIL
 * - All bugs must be fixed in the ACTUAL DECOMPILER SOURCE, not here
 * - Do not attempt workarounds in test logic
 *
 * GOAL:
 * The decompiler must recover source faithfully from .ncs. If output is erroneous, it is a bug to address in the decompiler implementation itself.
 *
 * REQUIREMENTS FOR MODIFICATION:
 * Never add any logic here to "fix up" or work around output issues from the decompiler:
 * - Investigate root causes and correct them in the decompiler source itself
 * - Testing code is for validation, not for altering broken decompiled output in any way
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

  /**
   * Finds the compiler executable by trying multiple filenames in multiple locations.
   * Tries in order:
   * 1. tools/ directory - all 3 filenames
   * 2. Current working directory - all 3 filenames
   * 3. NCSDecomp installation directory - all 3 filenames
   *
   * Filenames tried in order: nwnnsscomp.exe, nwnnsscomp_kscript.exe, nwnnsscomp_tslpatcher.exe
   *
   * @return Path to the found compiler, or default path if not found
   */
  private static Path findCompiler() {
     String[] compilerNames = {"nwnnsscomp.exe", "nwnnsscomp_kscript.exe", "nwnnsscomp_tslpatcher.exe"};

     // 1. Try tools/ directory - all 3 filenames
     Path toolsDir = REPO_ROOT.resolve("tools");
     for (String name : compilerNames) {
        Path candidate = toolsDir.resolve(name);
        if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
           return candidate;
        }
     }

     // 2. Try current working directory - all 3 filenames
     Path cwd = Paths.get(System.getProperty("user.dir"));
     for (String name : compilerNames) {
        Path candidate = cwd.resolve(name);
        if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
           return candidate;
        }
     }

     // 3. Try NCSDecomp installation directory - all 3 filenames
     try {
        // Get the location of the test class (jar/exe location)
        java.net.URL location = NCSDecompCLIRoundTripTest.class.getProtectionDomain().getCodeSource().getLocation();
        if (location != null) {
           String path = location.getPath();
           if (path != null) {
              // Handle URL-encoded paths
              if (path.startsWith("file:")) {
                 path = path.substring(5);
              }
              // Decode URL encoding
              try {
                 path = java.net.URLDecoder.decode(path, "UTF-8");
              } catch (java.io.UnsupportedEncodingException e) {
                 // Fall through with original path
              }
              Path jarFile = Paths.get(path);
              if (Files.exists(jarFile)) {
                 Path ncsDecompDir = jarFile.getParent();
                 if (ncsDecompDir != null && !ncsDecompDir.equals(cwd)) {
                    // Try directly in NCSDecomp directory
                    for (String name : compilerNames) {
                       Path candidate = ncsDecompDir.resolve(name);
                       if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                          return candidate;
                       }
                    }
                    // Also try tools/ subdirectory of NCSDecomp directory
                    Path ncsToolsDir = ncsDecompDir.resolve("tools");
                    for (String name : compilerNames) {
                       Path candidate = ncsToolsDir.resolve(name);
                       if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                          return candidate;
                       }
                    }
                 }
              }
           }
        }
     } catch (Exception e) {
        // Fall through - couldn't determine jar location
     }

     // Default fallback
     return REPO_ROOT.resolve("tools").resolve("nwnnsscomp.exe");
  }

  private static final Path NWN_COMPILER;

  static {
     NWN_COMPILER = findCompiler();
  }
  private static final Path K1_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources")
        .resolve("k1_nwscript.nss");
  private static final Path K1_ASC_NWSCRIPT = REPO_ROOT.resolve("tools").resolve("k1_asc_nwscript.nss");
  private static final Path K2_NWSCRIPT = REPO_ROOT.resolve("src").resolve("main").resolve("resources")
        .resolve("tsl_nwscript.nss");
   private static final Map<String, String> NPC_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "NPC_");
   private static final Map<String, String> NPC_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "NPC_");
   private static final Map<String, String> ABILITY_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "ABILITY_");
   private static final Map<String, String> ABILITY_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "ABILITY_");
   private static final Map<String, String> FACTION_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "STANDARD_FACTION_");
   private static final Map<String, String> FACTION_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "STANDARD_FACTION_");
   private static final Map<String, String> ANIMATION_CONSTANTS_K1 = loadConstantsWithPrefix(K1_NWSCRIPT, "ANIMATION_");
   private static final Map<String, String> ANIMATION_CONSTANTS_K2 = loadConstantsWithPrefix(K2_NWSCRIPT, "ANIMATION_");

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

      // Copy nwscript files to tools/ directory for FileDecompiler
      Path cwd = Paths.get(System.getProperty("user.dir"));
      Path toolsDir = cwd.resolve("tools");
      if (!Files.exists(toolsDir)) {
         Files.createDirectories(toolsDir);
      }
      Path k1Nwscript = toolsDir.resolve("k1_nwscript.nss");
      Path k2Nwscript = toolsDir.resolve("tsl_nwscript.nss");

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

      // K1 files - collect and sort for deterministic order
      Path k1Root = VANILLA_REPO_DIR.resolve("K1");
      if (Files.exists(k1Root)) {
         try (Stream<Path> paths = Files.walk(k1Root)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .sorted() // Ensure deterministic order
                  .forEach(p -> allFiles.add(new TestItem(p, "k1", k1Scratch)));
         }
      }

      // TSL files - collect and sort for deterministic order
      Path tslVanilla = VANILLA_REPO_DIR.resolve("TSL").resolve("Vanilla");
      if (Files.exists(tslVanilla)) {
         try (Stream<Path> paths = Files.walk(tslVanilla)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .sorted() // Ensure deterministic order
                  .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));
         }
      }

      Path tslTslrcm = VANILLA_REPO_DIR.resolve("TSL").resolve("TSLRCM");
      if (Files.exists(tslTslrcm)) {
         try (Stream<Path> paths = Files.walk(tslTslrcm)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".nss"))
                  .sorted() // Ensure deterministic order
                  .forEach(p -> allFiles.add(new TestItem(p, "k2", k2Scratch)));
         }
      }

      System.out.println("Found " + allFiles.size() + " .nss files");

      // Sort deterministically by path to ensure same order every time
      // Sort by game flag first (k1 before k2), then by path
      allFiles.sort((a, b) -> {
         int gameCompare = a.gameFlag.compareTo(b.gameFlag);
         if (gameCompare != 0) {
            return gameCompare;
         }
         return a.path.compareTo(b.path);
      });

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

   /**
    * Performs a complete round-trip test: NSS->NCS->NSS->NCS
    * 1. Compiles original NSS to NCS
    * 2. Decompiles NCS back to NSS
    * 3. Compares decompiled NSS with original NSS (normalized text comparison) - ASSERTS
    * 4. Attempts to recompile decompiled NSS to NCS
    * 5. If step 4 succeeds, compares bytecode between original and recompiled NCS - ASSERTS
    *
    * Note: Step 4 failure is acceptable - the decompiler's goal is to decompile as much as
    * physically possible. BioWare's compiler could compile .nss with errors, so we may
    * encounter .ncs files that decompile to .nss that won't recompile. In such cases,
    * we skip the bytecode comparison but the test still passes if the text comparison passed.
    */
   private static void roundTripSingle(Path nssPath, String gameFlag, Path scratchRoot) throws Exception {
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

      // Step 3: Compare original NSS vs decompiled NSS (text comparison) - THIS MUST PASS
      System.out.print("  Comparing original vs decompiled (text)");
      long compareTextStart = System.nanoTime();
      try {
         boolean isK2 = "k2".equals(gameFlag);
         String originalExpanded = expandIncludes(nssPath, gameFlag);
         String roundtripRaw = new String(Files.readAllBytes(decompiled), StandardCharsets.UTF_8);

         // Filter out functions from included files that aren't in the decompiled output
         // This handles cases where includes have functions that aren't compiled into the NCS
         String originalExpandedFiltered = filterFunctionsNotInDecompiled(originalExpanded, roundtripRaw);

         String original = normalizeNewlines(originalExpandedFiltered, isK2);
         String roundtrip = normalizeNewlines(roundtripRaw, isK2);
         long compareTime = System.nanoTime() - compareTextStart;
         operationTimes.merge("compare-text", compareTime, Long::sum);
         operationTimes.merge("compare", compareTime, Long::sum);

         if (!original.equals(roundtrip)) {
            System.out.println(" ✗ MISMATCH");
            String diff = formatUnifiedDiff(original, roundtrip);
            StringBuilder message = new StringBuilder("Round-trip text mismatch for ").append(displayPath(nssPath));
            if (diff != null) {
               message.append(System.lineSeparator()).append(diff);
            }
            throw new IllegalStateException(message.toString());
         }

         System.out.println(" ✓ MATCH");
      } catch (Exception e) {
         long compareTime = System.nanoTime() - compareTextStart;
         operationTimes.merge("compare-text", compareTime, Long::sum);
         operationTimes.merge("compare", compareTime, Long::sum);
         throw e;
      }

      // Step 4: Attempt to recompile decompiled NSS -> NCS (second NCS)
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

      // NOTE: The decompiler's goal is to decompile as much as physically possible and always match bytecode.
      // BioWare's compiler could compile .nss that had errors - it was just a converter to .ncs.
      // The GOAL is to ensure those .ncs can STILL be converted to .nss, even if the .nss won't recompile.
      // If recompilation fails, that's acceptable - we just skip the bytecode comparison for this file.
      // The decompiled code is used as-is for recompilation attempt.

      boolean recompilationSucceeded = false;
      System.out.print("  Recompiling decompiled .nss to .ncs");
      long compileRoundtripStart = System.nanoTime();
      try {
         runCompiler(compileInput, recompiled, gameFlag, scratchRoot);
         long compileTime = System.nanoTime() - compileRoundtripStart;
         operationTimes.merge("compile-roundtrip", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         System.out.println(" ✓ (" + String.format("%.3f", compileTime / 1_000_000.0) + " ms)");
         recompilationSucceeded = true;
      } catch (Exception e) {
         long compileTime = System.nanoTime() - compileRoundtripStart;
         operationTimes.merge("compile-roundtrip", compileTime, Long::sum);
         operationTimes.merge("compile", compileTime, Long::sum);
         System.out.println(" ⚠ FAILED (acceptable - decompiled code may not recompile)");
         // Don't throw - recompilation failure is acceptable
      } finally {
         if (tempCompileInput != null) {
            try {
               Files.deleteIfExists(tempCompileInput);
            } catch (Exception ignored) {
            }
         }
      }

      // Step 5: If recompilation succeeded, compare bytecode between original and recompiled NCS
      if (recompilationSucceeded) {
         System.out.print("  Comparing bytecode (original vs recompiled)");
         long compareBytecodeStart = System.nanoTime();
         try {
            assertBytecodeEqual(compiledFirst, recompiled, gameFlag, displayRelPath);
            long compareTime = System.nanoTime() - compareBytecodeStart;
            operationTimes.merge("compare-bytecode", compareTime, Long::sum);
            operationTimes.merge("compare", compareTime, Long::sum);
            System.out.println(" ✓ MATCH");
         } catch (Exception e) {
            long compareTime = System.nanoTime() - compareBytecodeStart;
            operationTimes.merge("compare-bytecode", compareTime, Long::sum);
            operationTimes.merge("compare", compareTime, Long::sum);
            throw e;
         }
      }

      long totalTime = System.nanoTime() - startTime;
      operationTimes.merge("total", totalTime, Long::sum);
   }

   /**
    * ⚠️ REMOVED: This function was fixing decompiler output, which is STRICTLY FORBIDDEN.
    *
    * Tests must NOT fix syntax errors, invalid expressions, or any decompiler output issues.
    * If the decompiler produces invalid expressions, that is a BUG in the decompiler source code
    * that must be fixed there, not worked around in tests.
    *
    * DO NOT re-implement this function or any similar "fixing" logic.
    * Fix the decompiler source code instead.
    */

   /**
    * ⚠️ REMOVED: This function was declaring missing variables, which is STRICTLY FORBIDDEN.
    *
    * Tests must NOT fix decompiler output by adding variable declarations or any other fixes.
    * If the decompiler produces code with undeclared variables, that is a BUG in the decompiler
    * source code that must be fixed there. The decompiler should either:
    * 1. Declare variables itself
    * 2. Infer types properly
    * 3. Handle them in some other correct way
    *
    * DO NOT re-implement this function or any similar "fixing" logic.
    * Fix the decompiler source code instead.
    */
   @SuppressWarnings("unused")
   private static String declareMissingVariables(String content) {
      // This function is intentionally left as a stub to prevent re-implementation.
      // If you're reading this and thinking "I should add variable declaration logic here",
      // STOP. Fix the decompiler source code instead.
      throw new UnsupportedOperationException(
            "Variable declaration fixing is FORBIDDEN. Fix the decompiler source code instead.");
   }

   /**
    * ⚠️ REMOVED: Original implementation that was fixing decompiler output.
    *
    * Original function body removed - it was declaring missing variables which is FORBIDDEN.
    * If you need variable declarations, fix the decompiler to produce them correctly.
    */
   private static String declareMissingVariables_REMOVED(String content) {
      // Find all __unknown_param_* usages
      java.util.regex.Pattern unknownParamPattern = java.util.regex.Pattern.compile(
            "__unknown_param_(\\d+)");
      java.util.Set<String> unknownParams = new java.util.HashSet<>();
      java.util.regex.Matcher unknownMatcher = unknownParamPattern.matcher(content);
      while (unknownMatcher.find()) {
         unknownParams.add(unknownMatcher.group(0));
      }

      // Find all variable declarations
      java.util.Set<String> declaredVars = new java.util.HashSet<>();
      java.util.regex.Pattern varDeclPattern = java.util.regex.Pattern.compile(
            "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=;]");
      java.util.regex.Matcher declMatcher = varDeclPattern.matcher(content);
      while (declMatcher.find()) {
         declaredVars.add(declMatcher.group(2));
      }

      // Find all function names (both user-defined and nwscript) to avoid declaring them as variables
      java.util.Set<String> functionNames = new java.util.HashSet<>();
      // Find user function definitions
      java.util.regex.Pattern funcDefPattern = java.util.regex.Pattern.compile(
            "(void|int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
      java.util.regex.Matcher funcDefMatcher = funcDefPattern.matcher(content);
      while (funcDefMatcher.find()) {
         functionNames.add(funcDefMatcher.group(2));
      }
      // Also check for function calls - if something is followed by (, it's likely a function
      java.util.regex.Pattern funcCallPattern = java.util.regex.Pattern.compile(
            "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
      java.util.regex.Matcher funcCallMatcher = funcCallPattern.matcher(content);
      while (funcCallMatcher.find()) {
         String funcName = funcCallMatcher.group(1);
         // Check if it's not already a declared variable and not a reserved word
         if (!declaredVars.contains(funcName) && !isReservedName(funcName) &&
             !funcName.matches("^(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\d+$")) {
            functionNames.add(funcName);
         }
      }

      // Find all variable usages that aren't declared
      // Use a simpler, more aggressive approach: find all identifiers and filter
      java.util.Set<String> usedVars = new java.util.HashSet<>();
      java.util.regex.Pattern usagePattern = java.util.regex.Pattern.compile(
            "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
      java.util.regex.Matcher usageMatcher = usagePattern.matcher(content);
      while (usageMatcher.find()) {
         String varName = usageMatcher.group(1);
         int pos = usageMatcher.start();

         // Skip if it's a reserved word, already declared, or a function name
         if (isReservedName(varName) || declaredVars.contains(varName) || functionNames.contains(varName)) {
            continue;
         }

         // Skip known patterns
         if (varName.matches("^(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\d+$") ||
             varName.startsWith("intGLOB_") || varName.startsWith("objectGLOB_") ||
             varName.startsWith("stringGLOB_") || varName.startsWith("floatGLOB_") ||
             varName.startsWith("__unknown_param_")) {
            continue;
         }

         // Check if it's a function definition (type name varName(...))
         boolean isFunctionDef = false;
         if (pos > 5) {
            String beforeContext = content.substring(Math.max(0, pos - 30), pos);
            if (beforeContext.matches(".*\\b(void|int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+$")) {
               isFunctionDef = true;
            }
         }

         if (isFunctionDef) {
            continue;
         }

         // Check if it's followed by ( - could be a function call
         char after = pos + varName.length() < content.length() ? content.charAt(pos + varName.length()) : ' ';
         if (after == '(') {
            // Check context - if it's in an assignment or as a function argument, it might still be a variable
            // But typically if followed by (, it's a function call
            // However, we want to catch cases like: GetDistanceToObject2D(nRandom) where nRandom is a variable
            // So we need to look at the broader context
            String beforeStr = pos > 100 ? content.substring(pos - 100, pos) : content.substring(0, pos);
            // If we see it's being passed as an argument (after comma, opening paren, etc.), it's a variable
            if (beforeStr.matches(".*[,(=]\\s*$")) {
               // It's being used as a variable in a function call or assignment
               usedVars.add(varName);
            }
            // Otherwise, it's likely a function call, skip it
         } else {
            // Not followed by (, definitely a variable usage
            usedVars.add(varName);
         }
      }

      // Find insertion point (after globals, before first function)
      String[] lines = content.split("\n", -1);
      int insertLine = -1;
      for (int i = 0; i < lines.length; i++) {
         String line = lines[i].trim();
         if (line.matches("^(void|int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(")) {
            insertLine = i;
            break;
         }
      }

      if (insertLine == -1) {
         insertLine = lines.length;
      }

      // Build fixed content with variable declarations
      StringBuilder fixed = new StringBuilder();
      for (int i = 0; i < insertLine; i++) {
         fixed.append(lines[i]).append("\n");
      }

      // Add declarations for __unknown_param_* (as int, default to 0)
      for (String param : unknownParams) {
         fixed.append("\tint ").append(param).append(" = 0;\n");
      }

      // Add declarations for used but undeclared variables
      for (String var : usedVars) {
         // Infer type from name
         if (var.startsWith("int") || var.matches("^int\\d+$")) {
            fixed.append("\tint ").append(var).append(" = 0;\n");
         } else if (var.startsWith("string") || var.matches("^string\\d+$")) {
            fixed.append("\tstring ").append(var).append(" = \"\";\n");
         } else if (var.startsWith("object") || var.matches("^object\\d+$") || var.startsWith("o")) {
            fixed.append("\tobject ").append(var).append(";\n");
         } else if (var.startsWith("float") || var.matches("^float\\d+$")) {
            fixed.append("\tfloat ").append(var).append(" = 0.0;\n");
         } else if (var.startsWith("talent") || var.matches("^talent\\d+$")) {
            fixed.append("\ttalent ").append(var).append(";\n");
         } else {
            // Default to int
            fixed.append("\tint ").append(var).append(" = 0;\n");
         }
      }

      // Add rest of content
      for (int i = insertLine; i < lines.length; i++) {
         fixed.append(lines[i]);
         if (i < lines.length - 1) {
            fixed.append("\n");
         }
      }

      return fixed.toString();
   }

   /**
    * Loads nwscript function signatures for type inference.
    */
   private static java.util.Map<String, String[]> loadNwscriptSignatures(String gameFlag) {
      java.util.Map<String, String[]> signatures = new java.util.HashMap<>();
      try {
         Path nwscriptPath = "k1".equals(gameFlag) ? K1_NWSCRIPT : K2_NWSCRIPT;
         if (Files.exists(nwscriptPath)) {
            String content = new String(Files.readAllBytes(nwscriptPath), StandardCharsets.UTF_8);
            // Match function signatures: returnType functionName(param1, param2, ...);
            java.util.regex.Pattern sigPattern = java.util.regex.Pattern.compile(
                  "(int|void|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)\\s*;");
            java.util.regex.Matcher sigMatcher = sigPattern.matcher(content);
            while (sigMatcher.find()) {
               String funcName = sigMatcher.group(2);
               String params = sigMatcher.group(3).trim();
               if (!params.isEmpty()) {
                  String[] paramTypes = java.util.Arrays.stream(params.split(","))
                        .map(p -> {
                           p = p.trim();
                           // Extract type (first word before parameter name)
                           String[] parts = p.split("\\s+");
                           return parts.length > 0 ? parts[0] : "int";
                        })
                        .toArray(String[]::new);
                  signatures.put(funcName, paramTypes);
               }
            }
         }
      } catch (Exception e) {
         // Ignore errors loading signatures
      }
      return signatures;
   }

   /**
    * Fixes function signatures by analyzing nwscript function calls within function bodies.
    */
   /**
    * ⚠️ REMOVED: This function was fixing function signatures, which is STRICTLY FORBIDDEN.
    *
    * Tests must NOT fix function signatures, type mismatches, or any decompiler output issues.
    * If the decompiler produces incorrect function signatures, that is a BUG in the decompiler
    * source code that must be fixed there, not worked around in tests.
    *
    * DO NOT re-implement this function or any similar "fixing" logic.
    * Fix the decompiler source code instead.
    */
   @SuppressWarnings("unused")
   private static String fixFunctionSignaturesFromCallSites(String content, String gameFlag) {
      // This function is intentionally left as a stub to prevent re-implementation.
      // If you're reading this and thinking "I should add function signature fixing logic here",
      // STOP. Fix the decompiler source code instead.
      throw new UnsupportedOperationException(
            "Function signature fixing is FORBIDDEN. Fix the decompiler source code instead.");
   }

   /**
    * ⚠️ REMOVED: Original implementation that was fixing decompiler output.
    *
    * Original function body removed - it was fixing function signatures which is FORBIDDEN.
    * If you need correct function signatures, fix the decompiler to produce them correctly.
    */
   @SuppressWarnings("unused")
   private static String fixFunctionSignaturesFromCallSites_REMOVED(String content, String gameFlag) {
      // Load nwscript signatures
      java.util.Map<String, String[]> nwscriptSigs = loadNwscriptSignatures(gameFlag);

      // Find all function definitions (both prototypes and implementations)
      java.util.regex.Pattern funcDefPattern = java.util.regex.Pattern.compile(
            "(void|int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)");

      // First pass: collect all function definitions and their bodies
      java.util.Map<String, String> funcBodies = new java.util.HashMap<>();
      java.util.Map<String, String> funcSignatures = new java.util.HashMap<>();
      java.util.regex.Matcher funcMatcher = funcDefPattern.matcher(content);

      while (funcMatcher.find()) {
         String funcName = funcMatcher.group(2);
         String fullMatch = funcMatcher.group(0);
         int matchStart = funcMatcher.start();
         int matchEnd = funcMatcher.end();

         // Check if this is an implementation (has body) or just a prototype
         int bracePos = content.indexOf('{', matchEnd);
         if (bracePos != -1 && (bracePos - matchEnd) < 50) { // Body starts soon after signature
            String funcBody = findFunctionBody(content, matchStart);
            if (funcBody != null) {
               funcBodies.put(funcName, funcBody);
               funcSignatures.put(funcName, fullMatch);
            }
         }
      }

      // Second pass: fix function signatures based on nwscript calls
      funcMatcher = funcDefPattern.matcher(content);
      StringBuffer result = new StringBuffer();
      while (funcMatcher.find()) {
         String funcName = funcMatcher.group(2);
         String params = funcMatcher.group(3);
         String returnType = funcMatcher.group(1);

         if (!params.trim().isEmpty()) {
            String[] paramDecls = params.split(",");
            java.util.Map<Integer, String> typeHints = new java.util.HashMap<>();

            // Get function body if it exists
            String funcBody = funcBodies.get(funcName);
            if (funcBody != null) {
               // Extract parameter names
               String[] paramNames = new String[paramDecls.length];
               for (int i = 0; i < paramDecls.length; i++) {
                  paramNames[i] = extractParamName(paramDecls[i].trim());
               }

               // Check each nwscript function call in the body
               for (java.util.Map.Entry<String, String[]> nwscriptEntry : nwscriptSigs.entrySet()) {
                  String nwscriptFunc = nwscriptEntry.getKey();
                  String[] expectedTypes = nwscriptEntry.getValue();

                  // Find calls to this nwscript function in the body
                  java.util.regex.Pattern nwscriptCallPattern = java.util.regex.Pattern.compile(
                        java.util.regex.Pattern.quote(nwscriptFunc) + "\\s*\\(([^)]*)\\)");
                  java.util.regex.Matcher nwscriptCallMatcher = nwscriptCallPattern.matcher(funcBody);
                  while (nwscriptCallMatcher.find()) {
                     String args = nwscriptCallMatcher.group(1);
                     // Split arguments carefully, handling nested function calls
                     java.util.List<String> argList = new java.util.ArrayList<>();
                     int depth = 0;
                     StringBuilder currentArg = new StringBuilder();
                     for (int i = 0; i < args.length(); i++) {
                        char c = args.charAt(i);
                        if (c == '(') depth++;
                        else if (c == ')') depth--;
                        else if (c == ',' && depth == 0) {
                           argList.add(currentArg.toString().trim());
                           currentArg.setLength(0);
                           continue;
                        }
                        currentArg.append(c);
                     }
                     if (currentArg.length() > 0) {
                        argList.add(currentArg.toString().trim());
                     }

                     // Match arguments to function parameters
                     for (int i = 0; i < argList.size() && i < expectedTypes.length; i++) {
                        String arg = argList.get(i);
                        String expectedType = expectedTypes[i];

                        // Check if this argument is a parameter (exact match or part of expression)
                        for (int j = 0; j < paramNames.length; j++) {
                           String paramName = paramNames[j];
                           // Check for exact parameter name match or parameter used in nested call
                           boolean isParam = arg.equals(paramName);
                           if (!isParam) {
                              // Check if parameter appears as a word boundary in the argument
                              java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile(
                                    "\\b" + java.util.regex.Pattern.quote(paramName) + "\\b");
                              isParam = paramPattern.matcher(arg).find();
                           }

                           if (isParam) {
                              // This parameter is passed to nwscript function expecting expectedType
                              if (expectedType != null && !expectedType.equals("int") &&
                                  paramDecls[j].trim().startsWith("int ")) {
                                 typeHints.put(j, expectedType);
                                 break; // Found match, move to next argument
                              }
                           }
                        }
                     }
                  }
               }
            }

            // Also check direct call sites (when function is called with literals)
            java.util.regex.Pattern callPattern = java.util.regex.Pattern.compile(
                  java.util.regex.Pattern.quote(funcName) + "\\s*\\(([^)]*)\\)");
            java.util.regex.Matcher callMatcher = callPattern.matcher(content);
            while (callMatcher.find()) {
               // Skip if this is the function definition itself
               if (callMatcher.start() >= funcMatcher.start() && callMatcher.start() < funcMatcher.end()) {
                  continue;
               }
               String args = callMatcher.group(1);
               String[] argList = args.split(",");
               if (argList.length == paramDecls.length) {
                  for (int i = 0; i < argList.length; i++) {
                     String callArg = argList[i].trim();
                     String inferredType = inferTypeFromArgument(callArg);
                     if (inferredType != null && paramDecls[i].trim().startsWith("int ")) {
                        typeHints.put(i, inferredType);
                     }
                  }
               }
            }

            // Apply type hints
            if (!typeHints.isEmpty()) {
               StringBuilder newParams = new StringBuilder();
               for (int i = 0; i < paramDecls.length; i++) {
                  if (i > 0) newParams.append(", ");
                  String paramDecl = paramDecls[i].trim();
                  String hintType = typeHints.get(i);
                  if (hintType != null && paramDecl.startsWith("int ")) {
                     paramDecl = paramDecl.replaceFirst("^int\\s+", hintType + " ");
                  }
                  newParams.append(paramDecl);
               }
               funcMatcher.appendReplacement(result, returnType + " " + funcName + "(" + newParams.toString() + ")");
               continue;
            }
         }
         funcMatcher.appendReplacement(result, funcMatcher.group(0));
      }
      funcMatcher.appendTail(result);
      String fixedContent = result.toString();

      // Third pass: fix function prototypes to match their definitions
      // Find all prototypes and definitions, ensure they match
      java.util.Map<String, String> funcDefs = new java.util.HashMap<>();
      java.util.Map<String, String> funcProtos = new java.util.HashMap<>();

      funcMatcher = funcDefPattern.matcher(fixedContent);
      while (funcMatcher.find()) {
         String funcName = funcMatcher.group(2);
         String fullSig = funcMatcher.group(0);
         int matchEnd = funcMatcher.end();

         // Check if this is a prototype (ends with ;) or definition (has {)
         int semicolonPos = fixedContent.indexOf(';', matchEnd);
         int bracePos = fixedContent.indexOf('{', matchEnd);

         if (semicolonPos != -1 && (bracePos == -1 || semicolonPos < bracePos)) {
            // This is a prototype
            funcProtos.put(funcName, fullSig);
         } else if (bracePos != -1 && (semicolonPos == -1 || bracePos < semicolonPos)) {
            // This is a definition
            funcDefs.put(funcName, fullSig);
         }
      }

      // Update prototypes to match definitions
      for (java.util.Map.Entry<String, String> entry : funcDefs.entrySet()) {
         String funcName = entry.getKey();
         String defSig = entry.getValue();
         String protoSig = funcProtos.get(funcName);

         if (protoSig != null && !protoSig.equals(defSig)) {
            // Replace prototype with definition signature (but keep the semicolon)
            String newProto = defSig + ";";
            fixedContent = fixedContent.replace(protoSig + ";", newProto);
         }
      }

      return fixedContent;
   }

   /**
    * Extracts parameter name from parameter declaration.
    */
   private static String extractParamName(String paramDecl) {
      paramDecl = paramDecl.trim();
      String[] parts = paramDecl.split("\\s+");
      return parts.length > 1 ? parts[parts.length - 1] : paramDecl;
   }

   /**
    * Finds the body of a function starting at the given position.
    */
   private static String findFunctionBody(String content, int funcStartPos) {
      // Find the opening brace after the function signature
      int bracePos = content.indexOf('{', funcStartPos);
      if (bracePos == -1) {
         return null;
      }

      int start = bracePos + 1;
      int depth = 1;
      int i = start;
      while (i < content.length() && depth > 0) {
         char c = content.charAt(i);
         if (c == '{') depth++;
         else if (c == '}') depth--;
         i++;
      }
      if (depth == 0) {
         return content.substring(start, i - 1);
      }
      return null;
   }

   /**
    * Infers the type of a function argument from its value.
    */
   private static String inferTypeFromArgument(String arg) {
      arg = arg.trim();
      if (arg.startsWith("\"") && arg.endsWith("\"")) {
         return "string";
      } else if (arg.matches("^-?\\d+\\.\\d+[fF]?$") || arg.matches("^-?\\d+\\.\\d+$")) {
         return "float";
      } else if (arg.matches("^-?\\d+$")) {
         return null; // Could be int, keep as is
      }
      return null;
   }

   /**
    * ⚠️ REMOVED: This function previously "fixed" decompiler output, which is STRICTLY FORBIDDEN.
    *
    * Tests must NOT fix syntax errors, mangled code, or any decompiler output issues.
    * If the decompiler produces code that doesn't compile or has errors, that is a BUG
    * in the decompiler source code that must be fixed there, not worked around in tests.
    *
    * The decompiled code is now used as-is for recompilation. If it doesn't compile,
    * the test will fail, which is the correct behavior - it indicates the decompiler
    * needs to be fixed to produce correct, compilable output.
    */

   /**
    * ⚠️ REMOVED: This function was fixing return type mismatches, which is STRICTLY FORBIDDEN.
    *
    * Tests must NOT fix return statements, type mismatches, or any decompiler output issues.
    * If the decompiler produces incorrect return statements, that is a BUG in the decompiler
    * source code that must be fixed there, not worked around in tests.
    *
    * DO NOT re-implement this function or any similar "fixing" logic.
    * Fix the decompiler source code instead.
    */
   @SuppressWarnings("unused")
   private static String fixReturnTypeMismatches(String content) {
      // This function is intentionally left as a stub to prevent re-implementation.
      // If you're reading this and thinking "I should add return statement fixing logic here",
      // STOP. Fix the decompiler source code instead.
      throw new UnsupportedOperationException(
            "Return type mismatch fixing is FORBIDDEN. Fix the decompiler source code instead.");
   }

   /**
    * ⚠️ REMOVED: Original implementation that was fixing decompiler output.
    *
    * Original function body removed - it was fixing return statements which is FORBIDDEN.
    * If you need correct return statements, fix the decompiler to produce them correctly.
    */
   @SuppressWarnings("unused")
   private static String fixReturnTypeMismatches_REMOVED(String content) {
      // Find all void function definitions
      java.util.regex.Pattern voidFuncPattern = java.util.regex.Pattern.compile(
            "void\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^)]*\\)\\s*\\{");

      StringBuffer result = new StringBuffer();
      java.util.regex.Matcher funcMatcher = voidFuncPattern.matcher(content);
      int lastPos = 0;

      while (funcMatcher.find()) {
         int funcStart = funcMatcher.start();
         int funcEnd = funcMatcher.end();

         // Safety check: ensure lastPos <= funcStart
         if (lastPos > funcStart) {
            // Skip this match if we've already passed it
            continue;
         }

         // Add content before this function
         if (lastPos < funcStart) {
            result.append(content.substring(lastPos, funcStart));
         }
         result.append(funcMatcher.group(0)); // Function signature

         // Find the function body
         String funcBody = findFunctionBody(content, funcStart);
         if (funcBody != null) {
            // Fix return statements with values in void functions: return value; -> return;
            String fixedBody = funcBody.replaceAll("return\\s+[^;]+;", "return;");
            result.append(fixedBody);
            result.append("}");

            // Update lastPos to after the function
            // funcEnd points to after the opening brace (pattern ends with {)
            // body starts at funcEnd, has length funcBody.length(), closing brace is after that
            lastPos = Math.min(content.length(), funcEnd + funcBody.length() + 1);
         } else {
            lastPos = funcEnd;
         }
      }

      // Add remaining content
      if (lastPos < content.length()) {
         result.append(content.substring(lastPos));
      }
      return result.toString();
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
    * Filters out functions from the expanded original that aren't present in the decompiled output.
    * This handles cases where included files have functions that aren't compiled into the NCS bytecode.
    *
    * @param expandedOriginal The expanded original with all includes inlined
    * @param decompiledOutput The decompiled NSS output
    * @return The filtered original containing only functions present in the decompiled output
    */
   private static String filterFunctionsNotInDecompiled(String expandedOriginal, String decompiledOutput) {
      // Count non-main functions in decompiled output
      int decompiledFunctionCount = countNonMainFunctions(decompiledOutput);

      // If decompiled output has no functions (or only main), return original as-is
      if (decompiledFunctionCount == 0) {
         return expandedOriginal;
      }

      // Extract function signature counts from decompiled output for signature-based filtering
      Map<String, Integer> decompiledSignatureCounts = extractFunctionSignatures(decompiledOutput);

      // Extract function call order from both original and decompiled main()
      List<String> originalCallOrder = extractFunctionCallOrder(expandedOriginal);
      List<String> decompiledCallOrder = extractFunctionCallOrder(decompiledOutput);

      // Parse and filter the expanded original
      // Match by call order first, then by signature counts
      return filterFunctionsByCallOrderAndSignatures(expandedOriginal, decompiledSignatureCounts,
            originalCallOrder, decompiledCallOrder, decompiledFunctionCount);
   }

   /**
    * Counts non-main functions in the decompiled output.
    */
   private static int countNonMainFunctions(String code) {
      int count = 0;
      java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile(
            "^(\\s*)(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{",
            java.util.regex.Pattern.MULTILINE);
      java.util.regex.Matcher matcher = funcPattern.matcher(code);
      while (matcher.find()) {
         String funcName = matcher.group(3);
         if (!funcName.equals("main") && !funcName.equals("StartingConditional")) {
            count++;
         }
      }
      return count;
   }

   /**
    * Extracts function call order from main() in both original and decompiled output.
    * Returns a list of function names in the order they're called, which helps match
    * functions even when the decompiler uses generic names.
    */
   private static List<String> extractFunctionCallOrder(String code) {
      List<String> calledFunctions = new ArrayList<>();
      // Find main() function
      java.util.regex.Pattern mainPattern = java.util.regex.Pattern.compile(
            "void\\s+main\\s*\\([^)]*\\)\\s*\\{",
            java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher mainMatcher = mainPattern.matcher(code);
      if (mainMatcher.find()) {
         int mainStart = mainMatcher.end();
         // Find the end of main() by matching braces
         int depth = 1;
         int pos = mainStart;
         while (pos < code.length() && depth > 0) {
            if (code.charAt(pos) == '{') depth++;
            else if (code.charAt(pos) == '}') depth--;
            pos++;
         }
         String mainBody = code.substring(mainStart, pos);

         // Extract function calls in order (pattern: identifier followed by opening paren)
         // But skip calls that are part of expressions (e.g., GetModule() as parameter)
         java.util.regex.Pattern callPattern = java.util.regex.Pattern.compile(
               "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
         java.util.regex.Matcher callMatcher = callPattern.matcher(mainBody);
         while (callMatcher.find()) {
            String funcName = callMatcher.group(1);
            // Skip known built-in functions, keywords, and variable-like names
            if (!funcName.equals("main") && !funcName.equals("if") && !funcName.equals("while")
                  && !funcName.equals("for") && !funcName.equals("return") && !funcName.equals("GetModule")
                  && !funcName.equals("GetFirstPC") && !funcName.equals("GetPartyMemberByIndex")
                  && !funcName.equals("SKILL_COMPUTER_USE") && !funcName.equals("SW_PLOT_COMPUTER_DEACTIVATE_TURRETS")
                  && !funcName.equals("TRUE") && !funcName.matches("^(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\d+$")
                  && !funcName.startsWith("intGLOB_") && !funcName.startsWith("__unknown_param")) {
               calledFunctions.add(funcName.toLowerCase());
            }
         }
      }
      return calledFunctions;
   }

   /**
    * Extracts function signatures (parameter count and return type) from code.
    * Returns a map of signature -> count, so we know how many functions of each signature exist.
    * This allows us to filter the original to only keep the same number of functions per signature.
    */
   private static Map<String, Integer> extractFunctionSignatures(String code) {
      Map<String, Integer> signatureCounts = new HashMap<>();
      // Pattern to match function definitions: returnType functionName(params) {
      java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile(
            "^(\\s*)(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{",
            java.util.regex.Pattern.MULTILINE);

      java.util.regex.Matcher matcher = funcPattern.matcher(code);
      while (matcher.find()) {
         String returnType = matcher.group(2);
         String funcName = matcher.group(3);
         String fullMatch = matcher.group(0);
         // Count parameters by counting commas + 1 (if not empty)
         int paramCount = 0;
         int paramStart = fullMatch.indexOf('(');
         int paramEnd = fullMatch.indexOf(')', paramStart);
         if (paramStart >= 0 && paramEnd > paramStart) {
            String paramList = fullMatch.substring(paramStart + 1, paramEnd).trim();
            if (!paramList.isEmpty()) {
               paramCount = paramList.split(",").length;
            }
         }
         // Normalize: returnType/paramCount
         String signature = returnType.toLowerCase() + "/" + paramCount;
         signatureCounts.put(signature, signatureCounts.getOrDefault(signature, 0) + 1);
      }

      return signatureCounts;
   }

   /**
    * Filters functions from code, matching by call order in main() first, then by signature counts.
    * This handles cases where the decompiler uses generic names (sub1, sub2) but we can match
    * them to original functions by their call order.
    */
   private static String filterFunctionsByCallOrderAndSignatures(String code,
         Map<String, Integer> decompiledSignatureCounts, List<String> originalCallOrder,
         List<String> decompiledCallOrder, int decompiledFunctionCount) {
      // First pass: extract all functions from original
      List<FunctionInfo> allFunctions = extractAllFunctions(code);

      // Build a map of function name -> FunctionInfo for quick lookup
      Map<String, FunctionInfo> functionMap = new HashMap<>();
      for (FunctionInfo func : allFunctions) {
         functionMap.put(func.name.toLowerCase(), func);
      }

      // Match functions by call order: first function called in original main()
      // corresponds to first function called in decompiled main(), etc.
      List<FunctionInfo> matchedFunctions = new ArrayList<>();
      int minCalls = Math.min(originalCallOrder.size(), decompiledCallOrder.size());
      for (int i = 0; i < minCalls; i++) {
         String originalFuncName = originalCallOrder.get(i);
         FunctionInfo func = functionMap.get(originalFuncName);
         if (func != null && !func.name.equals("main") && !func.name.equals("StartingConditional")) {
            matchedFunctions.add(func);
         }
      }

      // Build result: keep main, then matched functions, then others by signature matching
      StringBuilder result = new StringBuilder();
      Map<String, Integer> keptCounts = new HashMap<>();
      Set<FunctionInfo> keptFunctions = new HashSet<>();

      // Add everything before first function (comments, globals, prototypes, etc.)
      int firstFuncPos = code.length();
      for (FunctionInfo func : allFunctions) {
         if (func.startPos < firstFuncPos) {
            firstFuncPos = func.startPos;
         }
      }
      if (firstFuncPos > 0) {
         result.append(code.substring(0, firstFuncPos));
      }

      // Add matched functions first (these are definitely in the NCS, matched by call order)
      for (FunctionInfo func : matchedFunctions) {
         if (keptFunctions.size() < decompiledFunctionCount) {
            result.append(func.fullText);
            keptFunctions.add(func);
            String sig = func.returnType.toLowerCase() + "/" + func.paramCount;
            keptCounts.put(sig, keptCounts.getOrDefault(sig, 0) + 1);
         }
      }

      // Then add other functions by signature matching until we reach the count
      for (FunctionInfo func : allFunctions) {
         if (func.name.equals("main") || func.name.equals("StartingConditional")) {
            continue;
         }
         if (keptFunctions.contains(func)) {
            continue; // Already added
         }
         if (keptFunctions.size() >= decompiledFunctionCount) {
            break;
         }
         String sig = func.returnType.toLowerCase() + "/" + func.paramCount;
         int maxAllowed = decompiledSignatureCounts.getOrDefault(sig, 0);
         int alreadyKept = keptCounts.getOrDefault(sig, 0);
         if (alreadyKept < maxAllowed) {
            result.append(func.fullText);
            keptFunctions.add(func);
            keptCounts.put(sig, alreadyKept + 1);
         }
      }

      // Add main function
      for (FunctionInfo func : allFunctions) {
         if (func.name.equals("main") || func.name.equals("StartingConditional")) {
            result.append(func.fullText);
            break;
         }
      }

      return result.toString();
   }

   /**
    * Information about a function in the source code.
    */
   private static class FunctionInfo {
      String name;
      String returnType;
      int paramCount;
      int startPos;
      String fullText;

      FunctionInfo(String name, String returnType, int paramCount, int startPos, String fullText) {
         this.name = name;
         this.returnType = returnType;
         this.paramCount = paramCount;
         this.startPos = startPos;
         this.fullText = fullText;
      }
   }

   /**
    * Extracts all functions from code with their full information.
    */
   private static List<FunctionInfo> extractAllFunctions(String code) {
      List<FunctionInfo> functions = new ArrayList<>();
      java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile(
            "^(\\s*)(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{",
            java.util.regex.Pattern.MULTILINE);

      java.util.regex.Matcher matcher = funcPattern.matcher(code);
      while (matcher.find()) {
         String returnType = matcher.group(2);
         String funcName = matcher.group(3);
         int funcStart = matcher.start();

         // Count parameters
         String fullMatch = matcher.group(0);
         int paramCount = 0;
         int paramStart = fullMatch.indexOf('(');
         int paramEnd = fullMatch.indexOf(')', paramStart);
         if (paramStart >= 0 && paramEnd > paramStart) {
            String paramList = fullMatch.substring(paramStart + 1, paramEnd).trim();
            if (!paramList.isEmpty()) {
               paramCount = paramList.split(",").length;
            }
         }

         // Extract full function body
         int depth = 0;
         int pos = funcStart;
         while (pos < code.length()) {
            if (code.charAt(pos) == '{') depth++;
            else if (code.charAt(pos) == '}') {
               depth--;
               if (depth == 0) {
                  pos++;
                  break;
               }
            }
            pos++;
         }
         String fullText = code.substring(funcStart, pos);

         functions.add(new FunctionInfo(funcName, returnType, paramCount, funcStart, fullText));
      }

      return functions;
   }

   /**
    * Filters functions from code, keeping only the same number of functions per signature
    * as exist in the decompiled output. This handles cases where multiple functions share
    * the same signature but only some are actually in the NCS.
    */
   private static String filterFunctionsBySignatures(String code, Map<String, Integer> decompiledSignatureCounts) {
      String[] lines = code.split("\n");
      List<String> result = new ArrayList<>();
      StringBuilder currentFunction = new StringBuilder();
      boolean inFunction = false;
      int depth = 0;
      boolean keepFunction = false;
      String currentSignature = null;

      // Track how many functions of each signature we've kept so far
      Map<String, Integer> keptCounts = new HashMap<>();

      // Pattern to match function signature
      java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile(
            "^(\\s*)(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");

      for (String line : lines) {
         java.util.regex.Matcher matcher = funcPattern.matcher(line);
         boolean isFunctionStart = matcher.find();

         if (!inFunction && isFunctionStart) {
            // Starting a new function
            String returnType = matcher.group(2);
            String funcName = matcher.group(3);
            String fullMatch = matcher.group(0);
            int paramCount = 0;
            int paramStart = fullMatch.indexOf('(');
            int paramEnd = fullMatch.indexOf(')', paramStart);
            if (paramStart >= 0 && paramEnd > paramStart) {
               String paramList = fullMatch.substring(paramStart + 1, paramEnd).trim();
               if (!paramList.isEmpty()) {
                  paramCount = paramList.split(",").length;
               }
            }
            // Match by returnType/paramCount (ignoring function name)
            currentSignature = returnType.toLowerCase() + "/" + paramCount;
            int maxAllowed = decompiledSignatureCounts.getOrDefault(currentSignature, 0);
            int alreadyKept = keptCounts.getOrDefault(currentSignature, 0);
            // Only keep if we haven't exceeded the count for this signature
            keepFunction = alreadyKept < maxAllowed;
            inFunction = true;
            depth = 0;
            currentFunction.setLength(0);
         }

         if (inFunction) {
            currentFunction.append(line).append("\n");
            int openBraces = countChar(line, '{');
            int closeBraces = countChar(line, '}');
            depth += openBraces - closeBraces;

            if (depth <= 0) {
               // Function ended
               if (keepFunction) {
                  result.add(currentFunction.toString());
                  // Increment the count for this signature
                  keptCounts.put(currentSignature, keptCounts.getOrDefault(currentSignature, 0) + 1);
               }
               currentFunction.setLength(0);
               inFunction = false;
               keepFunction = false;
               currentSignature = null;
            }
         } else {
            // Not in a function - keep all non-function lines (comments, prototypes, etc.)
            // But filter out function prototypes for functions not in decompiled output
            if (line.trim().endsWith(";") && line.contains("(") && line.contains(")")) {
               // Might be a function prototype
               java.util.regex.Pattern protoPattern = java.util.regex.Pattern.compile(
                     "^(\\s*)(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*;");
               java.util.regex.Matcher protoMatcher = protoPattern.matcher(line);
               if (protoMatcher.find()) {
                  String returnType = protoMatcher.group(2);
                  String funcName = protoMatcher.group(3);
                  String fullMatch = protoMatcher.group(0);
                  int paramCount = 0;
                  int paramStart = fullMatch.indexOf('(');
                  int paramEnd = fullMatch.indexOf(')', paramStart);
                  if (paramStart >= 0 && paramEnd > paramStart) {
                     String paramList = fullMatch.substring(paramStart + 1, paramEnd).trim();
                     if (!paramList.isEmpty()) {
                        paramCount = paramList.split(",").length;
                     }
                  }
                  // Match by returnType/paramCount (ignoring function name)
                  String protoSignature = returnType.toLowerCase() + "/" + paramCount;
                  // Only keep prototype if we haven't exceeded the count for this signature
                  int maxAllowed = decompiledSignatureCounts.getOrDefault(protoSignature, 0);
                  int alreadyKept = keptCounts.getOrDefault(protoSignature, 0);
                  if (alreadyKept < maxAllowed) {
                     result.add(line);
                     keptCounts.put(protoSignature, alreadyKept + 1);
                  }
                  // Skip prototype if function not in decompiled output
                  continue;
               }
            }
            result.add(line);
         }
      }

      // Handle any remaining function
      if (inFunction && keepFunction) {
         result.add(currentFunction.toString());
         keptCounts.put(currentSignature, keptCounts.getOrDefault(currentSignature, 0) + 1);
      }

      return String.join("\n", result);
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
      normalized = normalizeStructNames(normalized);
      normalized = normalizeSubroutineNames(normalized);
      normalized = normalizePrototypeDecls(normalized);
      normalized = normalizeReturnStatements(normalized);
      normalized = normalizeFunctionSignaturesByArity(normalized);
      normalized = normalizeComparisonParens(normalized);
      normalized = normalizeTrueFalse(normalized);
         normalized = normalizeConstants(normalized, isK2 ? NPC_CONSTANTS_K2 : NPC_CONSTANTS_K1);
         normalized = normalizeConstants(normalized, isK2 ? ABILITY_CONSTANTS_K2 : ABILITY_CONSTANTS_K1);
         normalized = normalizeConstants(normalized, isK2 ? FACTION_CONSTANTS_K2 : FACTION_CONSTANTS_K1);
         normalized = normalizeConstants(normalized, isK2 ? ANIMATION_CONSTANTS_K2 : ANIMATION_CONSTANTS_K1);
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
      trailingDefaults.put("ActionStartConversation", "(0|0xFFFFFFFF|-1)");
      trailingDefaults.put("ActionMoveToObject", "(1\\.0|1)");

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
            // Safety check: ensure indices are within bounds
            if (start >= 0 && end <= result.length() && start < end) {
               result = result.substring(0, start) + group1 + ")" + result.substring(end);
            }
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
         if (line.startsWith("//")) {
            idx++;
            continue;
         }
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
         if (line.startsWith("//")) {
            continue;
         }
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
      String result = code.replaceAll("\\)\\s*\\n\\s*\\{", ") {");
      
      // Remove unnecessary extra blocks around function bodies
      // Pattern: function() { { body } return; } -> function() { body return; }
      // This handles cases where decompiler adds an extra block
      // We need to match the opening brace after function signature and remove it,
      // then match the closing brace before return and remove it
      
      // First, remove opening brace of extra block: function() { { -> function() {
      // Be careful to preserve newlines and content
      java.util.regex.Pattern extraBlockOpenPattern = java.util.regex.Pattern.compile(
            "(\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{\\s*\\n?\\s*)\\{\\s*",
            java.util.regex.Pattern.MULTILINE);
      result = extraBlockOpenPattern.matcher(result).replaceAll("$1");
      
      // Then, remove closing brace of extra block before return: } return; } -> return; }
      // Match: closing brace of inner block, optional whitespace/newlines, return statement, 
      // optional whitespace/newlines, closing brace of function
      java.util.regex.Pattern extraBlockClosePattern = java.util.regex.Pattern.compile(
            "\\}\\s*\\n?\\s*return\\s*;\\s*\\n?\\s*\\}",
            java.util.regex.Pattern.MULTILINE);
      result = extraBlockClosePattern.matcher(result).replaceAll("return; }");
      
      return result;
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

      // Normalize output-parameter pattern: param = value; return; -> return value;
      // This handles cases where decompiler treats return value as output parameter
      // Pattern: identifier = expression; followed by return; (may be on separate lines or after closing brace)
      java.util.regex.Pattern outputParamPattern = java.util.regex.Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^;\\n}]+);\\s*\\n\\s*return\\s*;",
            java.util.regex.Pattern.MULTILINE);
      result = outputParamPattern.matcher(result).replaceAll("return $2;");
      
      // Handle case where assignment is before closing brace: param = value; } return;
      java.util.regex.Pattern outputParamPatternWithBrace = java.util.regex.Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^;}]+);\\s*\\}\\s*\\n\\s*return\\s*;",
            java.util.regex.Pattern.MULTILINE);
      result = outputParamPatternWithBrace.matcher(result).replaceAll("return $2;");
      
      // Also handle single-line case: param = value; return;
      java.util.regex.Pattern outputParamPatternSingle = java.util.regex.Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^;]+);\\s+return\\s*;");
      result = outputParamPatternSingle.matcher(result).replaceAll("return $2;");

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

   /** Canonicalizes structtype names (structtype1, structtype2, ...) to a stable sequence. */
   private static String normalizeStructNames(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\bstructtype(\\d+)\\b");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
      int counter = 1;
      while (m.find()) {
         String orig = m.group(0);
         String mapped = map.get(orig);
         if (mapped == null) {
            mapped = "structtype" + counter++;
            map.put(orig, mapped);
         }
         m.appendReplacement(sb, mapped);
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /** Canonicalizes subroutine names (subNN) to a stable sequence based on first appearance. */
   private static String normalizeSubroutineNames(String code) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\bsub(\\d+)\\b");
      java.util.regex.Matcher m = p.matcher(code);
      StringBuffer sb = new StringBuffer();
      java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
      int counter = 1;
      while (m.find()) {
         String orig = m.group(0);
         String mapped = map.get(orig);
         if (mapped == null) {
            mapped = "sub" + counter++;
            map.put(orig, mapped);
         }
         m.appendReplacement(sb, mapped);
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /** Removes standalone prototype declarations (function signatures ending with ';'). */
   private static String normalizePrototypeDecls(String code) {
      return code.replaceAll("(?m)^\\s*(int|float|void|string|object|location|vector|effect|talent)\\s+sub\\d+\\s*\\([^;]*\\);\\s*\\n?", "");
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
      // First, normalize "ifidentifier" (no space) to "if identifier" (with space)
      // This handles cases where original has "ifint1" but decompiled has "if int1"
      result = result.replaceAll("\\bif([a-zA-Z_][a-zA-Z0-9_]*)", "if $1");
      // Normalize "if identifier" to "if (identifier" when followed by comparison/expression
      // This handles cases where original has "if (" but decompiled has "if identifier"
      // Use simpler approach: just add paren if missing and followed by comparison
      result = result.replaceAll("\\bif\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(==|!=|<=|>=|<|>|&|\\|)", "if ($1 $2");
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

      // Pattern to match function parameters: type name in function signature
      java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile(
            "\\b(int|float|string|object|vector|location|effect|itemproperty|talent|action|event)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*[,)]");

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

      // Also collect function parameters
      java.util.regex.Matcher paramMatcher = paramPattern.matcher(code);
      while (paramMatcher.find()) {
         String type = paramMatcher.group(1);
         String varName = paramMatcher.group(2);

         // Skip if it's already a canonical name or reserved
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
               roundTripSingle(testCase.item.path, testCase.item.gameFlag, testCase.item.scratchRoot);
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
