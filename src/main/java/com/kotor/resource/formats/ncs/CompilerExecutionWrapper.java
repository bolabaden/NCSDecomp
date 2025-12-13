// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Unified execution wrapper for all nwnnsscomp.exe variants.
 * <p>
 * This class abstracts away ALL compiler-specific quirks and differences,
 * providing a completely unified interface. All compiler differences are
 * handled transparently through file manipulation and environment setup.
 * <p>
 * Patterns implemented:
 * <ul>
 *    <li>Include file abstraction: Copies include files to source directory for compilers that don't support -i</li>
 *    <li>nwscript.nss abstraction: Copies/renames appropriate nwscript.nss for compilers that don't handle -g properly</li>
 *    <li>Working directory normalization: Sets appropriate working directory for each compiler</li>
 *    <li>Output path normalization: Handles different output path formats transparently</li>
 *    <li>Temporary file management: Automatically cleans up all temporary files</li>
 *    <li>Process execution: Unified process execution with proper error handling</li>
 * </ul>
 */
public class CompilerExecutionWrapper {
   private final File compilerFile;
   private final File sourceFile;
   private final File outputFile; // Used by NwnnsscompConfig internally
   private final boolean isK2;
   private final KnownExternalCompilers compiler;
   private final NwnnsscompConfig config;
   /** Process-level environment overrides applied during compiler invocation. */
   private final java.util.Map<String, String> envOverrides = new java.util.HashMap<>();

   // Files/directories that need cleanup
   private final List<File> copiedIncludeFiles = new ArrayList<>();
   private final List<File> copiedNwscriptFiles = new ArrayList<>();
   private File originalNwscriptBackup = null;
   private File copiedSourceFile = null; // When using registry spoofing, source is copied to spoofed directory
   private File actualSourceFile = null; // The actual source file to use (original or copied)

   /**
    * Creates a new compiler execution wrapper.
    *
    * @param compilerFile Path to the compiler executable
    * @param sourceFile Source NSS file to compile
    * @param outputFile Output NCS file path
    * @param isK2 true for KotOR 2 (TSL), false for KotOR 1
    * @throws IOException If configuration cannot be created
    */
   public CompilerExecutionWrapper(File compilerFile, File sourceFile, File outputFile, boolean isK2) throws IOException {
      this.compilerFile = compilerFile;
      this.sourceFile = sourceFile;
      this.outputFile = outputFile;
      this.isK2 = isK2;
      this.config = new NwnnsscompConfig(compilerFile, sourceFile, outputFile, isK2);
      this.compiler = config.getChosenCompiler();
      buildEnvironmentOverrides();
   }

   /**
    * Prepares the execution environment by handling all compiler-specific quirks.
    * This must be called before execute().
    *
    * @param includeDirs Optional list of include directories
    * @throws IOException If preparation fails
    */
   public void prepareExecutionEnvironment(List<File> includeDirs) throws IOException {
      // Pattern 2: nwscript.nss abstraction (must be done first for registry spoofing logic)
      prepareNwscriptFile();

      // If registry spoofing is needed, copy everything to the spoofed directory
      if (needsRegistrySpoofing()) {
         prepareRegistrySpoofedEnvironment(includeDirs);
      } else {
         // Pattern 1: Include file abstraction (normal path)
         prepareIncludeFiles(includeDirs);
         actualSourceFile = sourceFile;
      }

      // Additional patterns handled automatically during execution
   }

   /**
    * Checks if registry spoofing will be used for this compiler.
    */
   private boolean needsRegistrySpoofing() {
      return compiler == KnownExternalCompilers.KOTOR_TOOL || compiler == KnownExternalCompilers.KOTOR_SCRIPTING_TOOL;
   }

   /**
    * Prepares environment for registry spoofing by copying source file, includes, and nwscript.nss
    * to the registry-spoofed directory (tools/).
    */
   private void prepareRegistrySpoofedEnvironment(List<File> includeDirs) throws IOException {
      File toolsDir = compilerFile.getParentFile(); // tools/ directory (where registry is spoofed)
      if (toolsDir == null) {
         throw new IOException("Compiler directory is null");
      }
      // Create tools directory if it doesn't exist
      if (!toolsDir.exists()) {
         System.out.println("[INFO] CompilerExecutionWrapper: CREATING tools directory: " + toolsDir.getAbsolutePath());
         if (!toolsDir.mkdirs()) {
            throw new IOException("Failed to create compiler directory: " + toolsDir.getAbsolutePath());
         }
         System.out.println("[INFO] CompilerExecutionWrapper: Created tools directory: " + toolsDir.getAbsolutePath());
      }

      System.out.println("[INFO] CompilerExecutionWrapper: Preparing registry-spoofed environment in: " + toolsDir.getAbsolutePath());

      // Copy source file to tools directory
      copiedSourceFile = new File(toolsDir, sourceFile.getName());
      System.out.println("[INFO] CompilerExecutionWrapper: COPYING source file: " + sourceFile.getAbsolutePath() + " -> " + copiedSourceFile.getAbsolutePath());
      Files.copy(sourceFile.toPath(), copiedSourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      actualSourceFile = copiedSourceFile;
      System.out.println("[INFO] CompilerExecutionWrapper: Copied source file to spoofed directory: " + copiedSourceFile.getAbsolutePath());

      // Copy include files to tools directory
      if (includeDirs != null && !includeDirs.isEmpty()) {
         Set<String> neededIncludes = extractIncludeFiles(sourceFile);
         for (String includeName : neededIncludes) {
            File destFile = new File(toolsDir, includeName);
            // Skip if already exists
            if (destFile.exists()) {
               System.out.println("[INFO] CompilerExecutionWrapper: Include file already exists in spoofed directory: " + includeName);
               continue;
            }

            // Search for include file in include directories
            for (File includeDir : includeDirs) {
               if (includeDir != null && includeDir.exists()) {
                  File includeFile = new File(includeDir, includeName);
                  if (includeFile.exists() && includeFile.isFile()) {
                     System.out.println("[INFO] CompilerExecutionWrapper: COPYING include file: " + includeFile.getAbsolutePath() + " -> " + destFile.getAbsolutePath());
                     Files.copy(includeFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                     copiedIncludeFiles.add(destFile);
                     System.out.println("[INFO] CompilerExecutionWrapper: Copied include file to spoofed directory: " + includeName + " -> " + destFile.getAbsolutePath());
                     break;
                  }
               }
            }
         }
      }

      // nwscript.nss should already be in tools directory from prepareNwscriptFile()
      System.out.println("[INFO] CompilerExecutionWrapper: Registry-spoofed environment ready. Source: " + actualSourceFile.getAbsolutePath());
   }

   /**
    * Pattern 1: Include file abstraction.
    * For compilers that don't support -i flag, copy include files to source directory.
    */
   private void prepareIncludeFiles(List<File> includeDirs) throws IOException {
      if (includeDirs == null || includeDirs.isEmpty()) {
         return;
      }

      // Check if compiler supports -i flag
      boolean supportsIncludeFlag = (compiler != KnownExternalCompilers.KOTOR_TOOL
            && compiler != KnownExternalCompilers.KOTOR_SCRIPTING_TOOL);

      if (!supportsIncludeFlag) {
         // Compiler doesn't support -i, copy include files to source directory
         File sourceDir = sourceFile.getParentFile();
         if (sourceDir == null) {
            return;
         }
         // Create source directory if it doesn't exist
         if (!sourceDir.exists()) {
            System.out.println("[INFO] CompilerExecutionWrapper: CREATING source directory: " + sourceDir.getAbsolutePath());
            if (!sourceDir.mkdirs()) {
               System.err.println("[ERROR] CompilerExecutionWrapper: Failed to create source directory: " + sourceDir.getAbsolutePath());
               return;
            }
            System.out.println("[INFO] CompilerExecutionWrapper: Created source directory: " + sourceDir.getAbsolutePath());
         }

         // Parse source file to find which includes are needed
         Set<String> neededIncludes = extractIncludeFiles(sourceFile);

         // Copy needed include files from include directories to source directory
         for (String includeName : neededIncludes) {
            File destFile = new File(sourceDir, includeName);
            // Skip if already exists in source directory
            if (destFile.exists()) {
               continue;
            }

            // Search for include file in include directories
            for (File includeDir : includeDirs) {
               if (includeDir != null && includeDir.exists()) {
                  File includeFile = new File(includeDir, includeName);
                  if (includeFile.exists() && includeFile.isFile()) {
                     System.out.println("[INFO] CompilerExecutionWrapper: COPYING include file: " + includeFile.getAbsolutePath() + " -> " + destFile.getAbsolutePath());
                     Files.copy(includeFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                     copiedIncludeFiles.add(destFile);
                     System.out.println("[INFO] CompilerExecutionWrapper: Copied include file: " + includeName + " from " + includeFile.getAbsolutePath());
                     break;
                  }
               }
            }
         }
      }
   }

   /**
    * Pattern 2: nwscript.nss abstraction.
    * For compilers that don't handle -g properly or need nwscript.nss in specific location,
    * copy/rename the appropriate nwscript file.
    */
   private void prepareNwscriptFile() throws IOException {
      File compilerDir = compilerFile.getParentFile();
      if (compilerDir == null) {
         return;
      }
      // Create compiler directory if it doesn't exist
      if (!compilerDir.exists()) {
         System.out.println("[INFO] CompilerExecutionWrapper: CREATING compiler directory: " + compilerDir.getAbsolutePath());
         if (!compilerDir.mkdirs()) {
            throw new IOException("Failed to create compiler directory: " + compilerDir.getAbsolutePath());
         }
         System.out.println("[INFO] CompilerExecutionWrapper: Created compiler directory: " + compilerDir.getAbsolutePath());
      }

      File compilerNwscript = new File(compilerDir, "nwscript.nss");

      // Determine which nwscript.nss to use
      File nwscriptSource = determineNwscriptSource();
      if (nwscriptSource == null || !nwscriptSource.exists()) {
         System.out.println("[INFO] CompilerExecutionWrapper: Warning: nwscript.nss source not found");
         return;
      }

      // Check if we need to update the compiler's nwscript.nss
      boolean needsUpdate = true;
      if (compilerNwscript.exists()) {
         // Check if it's the same file (by content hash or path)
         try {
            if (Files.isSameFile(nwscriptSource.toPath(), compilerNwscript.toPath())) {
               needsUpdate = false;
            }
         } catch (IOException e) {
            // Files might be on different drives, compare by content
            needsUpdate = true;
         }
      }

      if (needsUpdate) {
         // Backup original if it exists and is different
         if (compilerNwscript.exists()) {
            File backup = new File(compilerDir, "nwscript.nss.backup");
            if (backup.exists()) {
               System.out.println("[INFO] CompilerExecutionWrapper: DELETING existing backup file: " + backup.getAbsolutePath());
               backup.delete();
            }
            System.out.println("[INFO] CompilerExecutionWrapper: COPYING nwscript.nss to backup: " + compilerNwscript.getAbsolutePath() + " -> " + backup.getAbsolutePath());
            Files.copy(compilerNwscript.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalNwscriptBackup = backup;
            System.out.println("[INFO] CompilerExecutionWrapper: Created backup of original nwscript.nss: " + backup.getAbsolutePath());
         }

         // Copy the appropriate nwscript.nss
         System.out.println("[INFO] CompilerExecutionWrapper: COPYING nwscript.nss (RENAME): " + nwscriptSource.getAbsolutePath() + " -> " + compilerNwscript.getAbsolutePath());
         System.out.println("[INFO] CompilerExecutionWrapper: Source file: " + nwscriptSource.getName() + " (K2=" + isK2 + ")");
         Files.copy(nwscriptSource.toPath(), compilerNwscript.toPath(), StandardCopyOption.REPLACE_EXISTING);
         copiedNwscriptFiles.add(compilerNwscript);
         System.out.println("[INFO] CompilerExecutionWrapper: Copied nwscript.nss: " + nwscriptSource.getName() + " -> " + compilerNwscript.getAbsolutePath());
      }
   }

   /**
    * Determines which nwscript.nss file to use based on game version and script requirements.
    * Uses CompilerUtil.resolveToolsFile() to search in app directory first, then CWD.
    */
   private File determineNwscriptSource() {
      if (isK2) {
         // For K2, use tsl_nwscript.nss
         return CompilerUtil.resolveToolsFile("tsl_nwscript.nss");
      } else {
         // For K1, check if script needs ASC nwscript (ActionStartConversation with 11 params)
         boolean needsAsc = checkNeedsAscNwscript(sourceFile);
         if (needsAsc) {
            // Try k1_asc_donotuse_nwscript.nss first
            File ascNwscript = CompilerUtil.resolveToolsFile("k1_asc_donotuse_nwscript.nss");
            if (ascNwscript.exists()) {
               return ascNwscript;
            }
         }
         // Default to k1_nwscript.nss
         return CompilerUtil.resolveToolsFile("k1_nwscript.nss");
      }
   }

   /**
    * Checks if a script needs ASC nwscript (for ActionStartConversation with 11 parameters).
    */
   private boolean checkNeedsAscNwscript(File nssFile) {
      try {
         // Verify file exists before reading
         if (!nssFile.exists() || !nssFile.isFile()) {
            System.err.println("[WARNING] CompilerExecutionWrapper: File does not exist for ASC check: " + nssFile.getAbsolutePath());
            return false;
         }
         String content = new String(Files.readAllBytes(nssFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
         // Look for ActionStartConversation calls with 11 parameters (10 commas)
         Pattern pattern = Pattern.compile(
               "ActionStartConversation\\s*\\(([^,)]*,\\s*){10}[^)]*\\)",
               Pattern.MULTILINE);
         return pattern.matcher(content).find();
      } catch (Exception e) {
         System.out.println("[INFO] CompilerExecutionWrapper: Failed to check for ASC nwscript requirement: " + e.getMessage());
         return false;
      }
   }

   /**
    * Extracts include file names from a source file.
    */
   private Set<String> extractIncludeFiles(File sourceFile) {
      Set<String> includes = new HashSet<>();
      try {
         String content = new String(Files.readAllBytes(sourceFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
         Pattern includePattern = Pattern.compile("#include\\s+[\"<]([^\">]+)[\">]");
         Matcher matcher = includePattern.matcher(content);
         while (matcher.find()) {
            String includeName = matcher.group(1);
            // Normalize: add .nss extension if missing
            if (!includeName.endsWith(".nss") && !includeName.endsWith(".h")) {
               includeName = includeName + ".nss";
            }
            includes.add(includeName);
         }
      } catch (Exception e) {
         System.out.println("[INFO] CompilerExecutionWrapper: Failed to parse includes from source: " + e.getMessage());
      }
      return includes;
   }

   /**
    * Gets the working directory for the compiler process.
    * Pattern 3: Working directory normalization.
    */
   public File getWorkingDirectory() {
      // Some legacy compilers (KOTOR Tool / Scripting Tool) behave more reliably
      // when run from their own directory because they probe for nwscript.nss and
      // other resources relative to the executable instead of the source file.
      if (compiler == KnownExternalCompilers.KOTOR_TOOL || compiler == KnownExternalCompilers.KOTOR_SCRIPTING_TOOL
            || !supportsGameFlag()) {
         File compilerDir = compilerFile.getParentFile();
         if (compilerDir != null && compilerDir.exists()) {
            System.out.println("[INFO] CompilerExecutionWrapper: Using compiler directory as working dir: "
                  + compilerDir.getAbsolutePath());
            return compilerDir;
         }
      }

      // Most compilers work best when run from the source file's directory
      File sourceDir = sourceFile.getParentFile();
      if (sourceDir != null && sourceDir.exists()) {
         return sourceDir;
      }
      // Fallback to compiler directory
      File compilerDir = compilerFile.getParentFile();
      if (compilerDir != null && compilerDir.exists()) {
         return compilerDir;
      }
      // Final fallback to app directory (NOT CWD)
      return CompilerUtil.getNCSDecompDirectory();
   }

   /**
    * Gets the formatted compile command arguments.
    * Pattern 4: Output path normalization is handled by NwnnsscompConfig.
    * When registry spoofing is used, this uses the copied source file in the spoofed directory.
    */
   public String[] getCompileArgs(List<File> includeDirs) {
      // If we copied the source file for registry spoofing, we need to create a new config with the copied file
      if (actualSourceFile != null && !actualSourceFile.equals(sourceFile)) {
         try {
            // Create a temporary config with the copied source file
            NwnnsscompConfig spoofedConfig = new NwnnsscompConfig(compilerFile, actualSourceFile, outputFile, isK2);
            return spoofedConfig.getCompileArgs(compilerFile.getAbsolutePath(), includeDirs);
         } catch (IOException e) {
            System.out.println("[INFO] CompilerExecutionWrapper: Failed to create spoofed config, using original: " + e.getMessage());
            // Fall back to original config
            return config.getCompileArgs(compilerFile.getAbsolutePath(), includeDirs);
         }
      }
      return config.getCompileArgs(compilerFile.getAbsolutePath(), includeDirs);
   }

   /**
    * Cleans up all temporary files created during preparation.
    * Pattern 5: Temporary file management.
    */
   public void cleanup() {
      // Clean up copied source file (if registry spoofing was used)
      if (copiedSourceFile != null && copiedSourceFile.exists()) {
         try {
            System.out.println("[INFO] CompilerExecutionWrapper: DELETING copied source file: " + copiedSourceFile.getAbsolutePath());
            copiedSourceFile.delete();
            System.out.println("[INFO] CompilerExecutionWrapper: Cleaned up copied source file: " + copiedSourceFile.getName());
         } catch (Exception e) {
            System.out.println("[INFO] CompilerExecutionWrapper: Failed to clean up copied source file " + copiedSourceFile.getName() + ": " + e.getMessage());
         }
         copiedSourceFile = null;
      }

      // Clean up copied include files
      for (File copiedFile : copiedIncludeFiles) {
         try {
            if (copiedFile.exists()) {
               System.out.println("[INFO] CompilerExecutionWrapper: DELETING include file: " + copiedFile.getAbsolutePath());
               copiedFile.delete();
               System.out.println("[INFO] CompilerExecutionWrapper: Cleaned up include file: " + copiedFile.getName());
            }
         } catch (Exception e) {
            System.out.println("[INFO] CompilerExecutionWrapper: Failed to clean up include file " + copiedFile.getName() + ": " + e.getMessage());
         }
      }
      copiedIncludeFiles.clear();

      // Restore original nwscript.nss if we backed it up
      if (originalNwscriptBackup != null && originalNwscriptBackup.exists()) {
         try {
            File compilerDir = compilerFile.getParentFile();
            if (compilerDir != null) {
               File compilerNwscript = new File(compilerDir, "nwscript.nss");
               if (compilerNwscript.exists()) {
                  System.out.println("[INFO] CompilerExecutionWrapper: COPYING (RESTORE) nwscript.nss from backup: " + originalNwscriptBackup.getAbsolutePath() + " -> " + compilerNwscript.getAbsolutePath());
                  Files.copy(originalNwscriptBackup.toPath(), compilerNwscript.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  System.out.println("[INFO] CompilerExecutionWrapper: Restored original nwscript.nss");
               }
            }
            System.out.println("[INFO] CompilerExecutionWrapper: DELETING backup file: " + originalNwscriptBackup.getAbsolutePath());
            originalNwscriptBackup.delete();
         } catch (Exception e) {
            System.out.println("[INFO] CompilerExecutionWrapper: Failed to restore original nwscript.nss: " + e.getMessage());
         }
      }

      // Note: We don't delete the copied nwscript.nss files because they might be needed
      // for subsequent compilations. They'll be overwritten on next use.
   }

   /**
    * Gets the detected compiler.
    */
   public KnownExternalCompilers getCompiler() {
      return compiler;
   }

   /**
    * Returns any environment overrides required for the chosen compiler.
    * Legacy compilers that rely on registry/game install probing can be coaxed
    * to use the bundled tools directory by setting common root variables.
    */
   public java.util.Map<String, String> getEnvironmentOverrides() {
      return java.util.Collections.unmodifiableMap(envOverrides);
   }

   /**
    * Builds environment overrides for compilers that don't accept -g or that
    * expect registry-based installation paths. This is a best-effort shim to
    * keep everything self-contained in the tools directory.
    */
   private void buildEnvironmentOverrides() {
      File toolsDir = CompilerUtil.getToolsDirectory();

      // Only apply overrides for legacy compilers that ignore -g or probe registry
      boolean needsRootOverride = compiler == KnownExternalCompilers.KOTOR_TOOL
            || compiler == KnownExternalCompilers.KOTOR_SCRIPTING_TOOL
            || !supportsGameFlag();

      if (!needsRootOverride) {
         return;
      }

      String resolvedRoot = toolsDir.getAbsolutePath();
      envOverrides.put("NWN_ROOT", resolvedRoot);
      envOverrides.put("NWNDir", resolvedRoot);
      envOverrides.put("KOTOR_ROOT", resolvedRoot);
      System.out.println("[INFO] CompilerExecutionWrapper: Applied environment overrides for legacy compiler. "
            + "NWN_ROOT=" + resolvedRoot + ", compiler=" + compiler.getName());
   }

   /**
    * Checks whether the selected compiler template exposes a game flag.
    */
   private boolean supportsGameFlag() {
      String[] args = compiler.getCompileArgs();
      for (String arg : args) {
         if (arg.contains("{game_value}") || "-g".equals(arg) || arg.startsWith("-g")) {
            return true;
         }
      }
      return false;
   }

   /**
    * Creates a registry spoofer for compilers that require registry spoofing.
    * <p>
    * Legacy compilers (KOTOR Tool, KOTOR Scripting Tool) read the game installation
    * path from the Windows registry. This method returns a RegistrySpoofer for those
    * compilers, or a NoOpRegistrySpoofer for compilers that don't need it.
    * <p>
    * The registry spoofer should be used in a try-with-resources block around the
    * compilation process:
    * <pre>
    * try (AutoCloseable spoofer = wrapper.createRegistrySpoofer()) {
    *    if (spoofer instanceof RegistrySpoofer) {
    *       ((RegistrySpoofer) spoofer).activate();
    *    }
    *    // ... perform compilation ...
    * }
    * </pre>
    *
    * @return A RegistrySpoofer for legacy compilers, or NoOpRegistrySpoofer otherwise
    * @throws UnsupportedOperationException If registry spoofing is needed but not on Windows
    */
   public AutoCloseable createRegistrySpoofer() {
      // Only KOTOR Tool and KOTOR Scripting Tool require registry spoofing
      if (compiler == KnownExternalCompilers.KOTOR_TOOL || compiler == KnownExternalCompilers.KOTOR_SCRIPTING_TOOL) {
         // Use the tools directory as the installation path (where compiler and nwscript files are)
         File toolsDir = CompilerUtil.getToolsDirectory();
         try {
            RegistrySpoofer spoofer = new RegistrySpoofer(toolsDir, isK2);
            System.out.println("[INFO] CompilerExecutionWrapper: Created RegistrySpoofer for " + compiler.getName());
            return spoofer;
         } catch (UnsupportedOperationException e) {
            // Not on Windows - fall back to NoOp
            System.out.println("[INFO] CompilerExecutionWrapper: Registry spoofing not supported, using NoOp: " + e.getMessage());
            return new NoOpRegistrySpoofer();
         }
      } else {
         // Compiler doesn't need registry spoofing
         return new NoOpRegistrySpoofer();
      }
   }
}

