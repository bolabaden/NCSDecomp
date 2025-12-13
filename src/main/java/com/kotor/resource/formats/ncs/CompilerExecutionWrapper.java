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
   @SuppressWarnings("unused")
   private final File outputFile; // Used by NwnnsscompConfig internally
   private final boolean isK2;
   private final KnownExternalCompilers compiler;
   private final NwnnsscompConfig config;
   
   // Files/directories that need cleanup
   private final List<File> copiedIncludeFiles = new ArrayList<>();
   private final List<File> copiedNwscriptFiles = new ArrayList<>();
   private File originalNwscriptBackup = null;
   
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
   }
   
   /**
    * Prepares the execution environment by handling all compiler-specific quirks.
    * This must be called before execute().
    *
    * @param includeDirs Optional list of include directories
    * @throws IOException If preparation fails
    */
   public void prepareExecutionEnvironment(List<File> includeDirs) throws IOException {
      // Pattern 1: Include file abstraction
      prepareIncludeFiles(includeDirs);
      
      // Pattern 2: nwscript.nss abstraction
      prepareNwscriptFile();
      
      // Additional patterns handled automatically during execution
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
                     Files.copy(includeFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                     copiedIncludeFiles.add(destFile);
                     System.err.println("DEBUG CompilerExecutionWrapper: Copied include file: " + includeName + " from " + includeFile.getAbsolutePath());
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
      
      File compilerNwscript = new File(compilerDir, "nwscript.nss");
      
      // Determine which nwscript.nss to use
      File nwscriptSource = determineNwscriptSource();
      if (nwscriptSource == null || !nwscriptSource.exists()) {
         System.err.println("DEBUG CompilerExecutionWrapper: Warning: nwscript.nss source not found");
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
               backup.delete();
            }
            Files.copy(compilerNwscript.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalNwscriptBackup = backup;
         }
         
         // Copy the appropriate nwscript.nss
         Files.copy(nwscriptSource.toPath(), compilerNwscript.toPath(), StandardCopyOption.REPLACE_EXISTING);
         copiedNwscriptFiles.add(compilerNwscript);
         System.err.println("DEBUG CompilerExecutionWrapper: Copied nwscript.nss: " + nwscriptSource.getName() + " -> " + compilerNwscript.getAbsolutePath());
      }
   }
   
   /**
    * Determines which nwscript.nss file to use based on game version and script requirements.
    */
   private File determineNwscriptSource() {
      File toolsDir = new File(System.getProperty("user.dir"), "tools");
      
      if (isK2) {
         // For K2, use tsl_nwscript.nss
         return new File(toolsDir, "tsl_nwscript.nss");
      } else {
         // For K1, check if script needs ASC nwscript (ActionStartConversation with 11 params)
         boolean needsAsc = checkNeedsAscNwscript(sourceFile);
         if (needsAsc) {
            // Try k1_asc_nwscript.nss first, then k1_asc_donotuse_nwscript.nss
            File ascNwscript = new File(toolsDir, "k1_asc_nwscript.nss");
            if (!ascNwscript.exists()) {
               ascNwscript = new File(toolsDir, "k1_asc_donotuse_nwscript.nss");
            }
            if (ascNwscript.exists()) {
               return ascNwscript;
            }
         }
         // Default to k1_nwscript.nss
         return new File(toolsDir, "k1_nwscript.nss");
      }
   }
   
   /**
    * Checks if a script needs ASC nwscript (for ActionStartConversation with 11 parameters).
    */
   private boolean checkNeedsAscNwscript(File nssFile) {
      try {
         String content = new String(Files.readAllBytes(nssFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
         // Look for ActionStartConversation calls with 11 parameters (10 commas)
         Pattern pattern = Pattern.compile(
               "ActionStartConversation\\s*\\(([^,)]*,\\s*){10}[^)]*\\)",
               Pattern.MULTILINE);
         return pattern.matcher(content).find();
      } catch (Exception e) {
         System.err.println("DEBUG CompilerExecutionWrapper: Failed to check for ASC nwscript requirement: " + e.getMessage());
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
         System.err.println("DEBUG CompilerExecutionWrapper: Failed to parse includes from source: " + e.getMessage());
      }
      return includes;
   }
   
   /**
    * Gets the working directory for the compiler process.
    * Pattern 3: Working directory normalization.
    */
   public File getWorkingDirectory() {
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
      // Final fallback to current directory
      return new File(System.getProperty("user.dir"));
   }
   
   /**
    * Gets the formatted compile command arguments.
    * Pattern 4: Output path normalization is handled by NwnnsscompConfig.
    */
   public String[] getCompileArgs(List<File> includeDirs) {
      return config.getCompileArgs(compilerFile.getAbsolutePath(), includeDirs);
   }
   
   /**
    * Cleans up all temporary files created during preparation.
    * Pattern 5: Temporary file management.
    */
   public void cleanup() {
      // Clean up copied include files
      for (File copiedFile : copiedIncludeFiles) {
         try {
            if (copiedFile.exists()) {
               copiedFile.delete();
               System.err.println("DEBUG CompilerExecutionWrapper: Cleaned up include file: " + copiedFile.getName());
            }
         } catch (Exception e) {
            System.err.println("DEBUG CompilerExecutionWrapper: Failed to clean up include file " + copiedFile.getName() + ": " + e.getMessage());
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
                  Files.copy(originalNwscriptBackup.toPath(), compilerNwscript.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  System.err.println("DEBUG CompilerExecutionWrapper: Restored original nwscript.nss");
               }
            }
            originalNwscriptBackup.delete();
         } catch (Exception e) {
            System.err.println("DEBUG CompilerExecutionWrapper: Failed to restore original nwscript.nss: " + e.getMessage());
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
}

