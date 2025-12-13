// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for compiler path resolution.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>GUI mode: Getting compiler Settings</li>
 *   <li>CLI mode: Resolving compiler path with fallbacks</li>
 *   <li>Shared: Combining folder path + filename into a full compiler path</li>
 * </ul>
 * <p>
 * The architecture is:
 * <ul>
 *   <li>Settings stores "nwnnsscomp Folder Path" (directory) and "nwnnsscomp Filename" (filename)</li>
 *   <li>GUI mode: MUST use getCompilerFromSettings() - NO FALLBACKS</li>
 *   <li>CLI mode: Can use resolveCompilerPathWithFallbacks() - WITH FALLBACKS</li>
 * </ul>
 */
public class CompilerUtil {

   /** Standard compiler filenames in priority order */
   private static final String[] COMPILER_NAMES = {
      "nwnnsscomp.exe",              // Primary - generic name (highest priority)
      "nwnnsscomp_kscript.exe",      // Secondary - KOTOR Scripting Tool
//      "nwnnsscomp_tslpatcher.exe",   // TSLPatcher variant
//      "nwnnsscomp_v1.exe"            // v1.3 first public release
      "nwnnsscomp_ktool.exe"         // KOTOR Tool variant
   };

   /**
    * Resolves a compiler path from folder path and filename.
    * <p>
    * This is the SHARED function used by both Settings and CLI for path resolution.
    * It simply combines the folder path and filename into a full path.
    * <p>
    * NO VALIDATION - just combines the paths. Caller should check if file exists.
    *
    * @param folderPath The folder path (must be a directory path, not a file path)
    * @param filename The compiler filename (e.g., "nwnnsscomp.exe")
    * @return The combined File, or null if either parameter is null/empty
    */
   public static File resolveCompilerPath(String folderPath, String filename) {
      if (folderPath == null || folderPath.trim().isEmpty()) {
         return null;
      }
      if (filename == null || filename.trim().isEmpty()) {
         return null;
      }

      folderPath = folderPath.trim();
      filename = filename.trim();

      // Normalize folder path (ensure it's a directory path, not a file path)
      File folder = new File(folderPath);
      if (folder.isFile()) {
         // If it's a file, use its parent directory
         File parent = folder.getParentFile();
         if (parent != null) {
            folder = parent;
         } else {
            return null;
         }
      }

      return new File(folder, filename);
   }

   /**
    * Gets the compiler file EXCLUSIVELY from Settings.
    * <p>
    * Reads "nwnnsscomp Folder Path" (directory) and "nwnnsscomp Filename" (filename)
    * from Settings and constructs the full path using resolveCompilerPath().
    * <p>
    * FOR GUI MODE ONLY - NO FALLBACKS.
    * If Settings doesn't have a valid compiler configured, returns null.
    *
    * @return The compiler File, or null if not configured in Settings
    */
   public static File getCompilerFromSettings() {
      // Get folder path and filename from Settings
      String folderPath = Decompiler.settings.getProperty("nwnnsscomp Folder Path", "");
      String filename = Decompiler.settings.getProperty("nwnnsscomp Filename", "");

      System.out.println("[INFO] CompilerUtil.getCompilerFromSettings: folderPath='" + folderPath + "', filename='" + filename + "'");

      // Use shared resolution function
      File compilerFile = resolveCompilerPath(folderPath, filename);

      if (compilerFile == null) {
         System.out.println("[INFO] CompilerUtil.getCompilerFromSettings: folderPath or filename is empty/invalid");
         return null;
      }

      System.out.println("[INFO] CompilerUtil.getCompilerFromSettings: compilerFile='" + compilerFile.getAbsolutePath() + "', exists=" + compilerFile.exists());

      // Return the file (even if it doesn't exist - caller can check)
      return compilerFile;
   }

   /**
    * Gets the compiler file EXCLUSIVELY from Settings and verifies it exists.
    * <p>
    * Same as getCompilerFromSettings() but also checks that the file exists.
    * <p>
    * FOR GUI MODE ONLY - NO FALLBACKS.
    *
    * @return The compiler File if it exists, or null if not configured or doesn't exist
    */
   public static File getCompilerFromSettingsOrNull() {
      File compiler = getCompilerFromSettings();
      if (compiler != null && compiler.exists() && compiler.isFile()) {
         return compiler;
      }
      return null;
   }

   /**
    * Resolves compiler path with fallbacks.
    * <p>
    * FOR CLI MODE ONLY. This method tries multiple fallback locations.
    * <p>
    * Fallback order:
    * <ol>
    *   <li>If cliPath is specified and valid, use it</li>
    *   <li>Try tools/ directory with all known compiler names</li>
    *   <li>Try current working directory with all known compiler names</li>
    *   <li>Try NCSDecomp installation directory (jar location)</li>
    * </ol>
    *
    * @param cliPath Path specified via CLI argument (can be null or empty)
    * @return The compiler File, or null if not found anywhere
    */
   public static File resolveCompilerPathWithFallbacks(String cliPath) {
      // 1. If CLI path is specified, use it (could be full path or just filename)
      if (cliPath != null && !cliPath.trim().isEmpty()) {
         File cliFile = new File(cliPath.trim());
         if (cliFile.exists() && cliFile.isFile()) {
            System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Using CLI path: " + cliFile.getAbsolutePath());
            return cliFile;
         }
         // If CLI path is a directory, try known filenames in it
         if (cliFile.isDirectory()) {
            for (String name : COMPILER_NAMES) {
               File candidate = new File(cliFile, name);
               if (candidate.exists() && candidate.isFile()) {
                  System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in CLI dir: " + candidate.getAbsolutePath());
                  return candidate;
               }
            }
         }
      }

      // 2. Try tools/ directory
      Path toolsDir = Paths.get(System.getProperty("user.dir")).resolve("tools");
      for (String name : COMPILER_NAMES) {
         Path candidate = toolsDir.resolve(name);
         if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
            System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in tools/: " + candidate);
            return candidate.toFile();
         }
      }

      // 3. Try current working directory
      Path cwd = Paths.get(System.getProperty("user.dir"));
      for (String name : COMPILER_NAMES) {
         Path candidate = cwd.resolve(name);
         if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
            System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in cwd: " + candidate);
            return candidate.toFile();
         }
      }

      // 4. Try NCSDecomp installation directory
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null && !ncsDecompDir.equals(cwd.toFile())) {
         for (String name : COMPILER_NAMES) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists() && candidate.isFile()) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in NCSDecomp dir: " + candidate.getAbsolutePath());
               return candidate;
            }
         }
         // Also try tools/ subdirectory of NCSDecomp directory
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : COMPILER_NAMES) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in NCSDecomp tools/: " + candidate.getAbsolutePath());
               return candidate;
            }
         }
      }

      System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: No compiler found anywhere");
      return null;
   }

   /**
    * Result of compiler file resolution with metadata.
    * Used by Settings to show whether a compiler was found via configured path or fallback.
    */
   public static class CompilerResolutionResult {
      private final File file;
      private final boolean isFallback;
      private final String source;

      public CompilerResolutionResult(File file, boolean isFallback, String source) {
         this.file = file;
         this.isFallback = isFallback;
         this.source = source;
      }

      public File getFile() {
         return file;
      }

      public boolean isFallback() {
         return isFallback;
      }

      public String getSource() {
         return source;
      }
   }

   /**
    * Finds the compiler executable using fallback logic.
    * This method tries the configured path first, then falls back to standard locations.
    * Used by Settings to show which compiler will actually be used.
    *
    * @param configuredPath The configured path from settings (may be empty)
    * @return A CompilerResolutionResult containing the found compiler file and whether it's a fallback, or null if not found
    */
   public static CompilerResolutionResult findCompilerFileWithResult(String configuredPath) {
      String[] compilerNames = COMPILER_NAMES;

      String configuredPathTrimmed = configuredPath != null ? configuredPath.trim() : "";
      boolean isConfigured = !configuredPathTrimmed.isEmpty();

      // 1. Try configured path (if set) - all compiler filenames
      if (isConfigured) {
         File configuredDir = new File(configuredPathTrimmed);
         if (configuredDir.isDirectory()) {
            // If it's a directory, try all filenames in it
            for (String name : compilerNames) {
               File candidate = new File(configuredDir, name);
               if (candidate.exists() && candidate.isFile()) {
                  return new CompilerResolutionResult(candidate, false, "Configured directory: " + configuredPathTrimmed);
               }
            }
         } else {
            // If it's a file, check if it exists
            if (configuredDir.exists() && configuredDir.isFile()) {
               return new CompilerResolutionResult(configuredDir, false, "Configured path: " + configuredPathTrimmed);
            }
            // Also try other filenames in the same directory
            File parent = configuredDir.getParentFile();
            if (parent != null) {
               for (String name : compilerNames) {
                  File candidate = new File(parent, name);
                  if (candidate.exists() && candidate.isFile()) {
                     return new CompilerResolutionResult(candidate, true, "Fallback in configured directory: " + parent.getAbsolutePath());
                  }
               }
            }
         }
      }

      // 2. Try tools/ directory - all compiler filenames
      File toolsDir = new File(System.getProperty("user.dir"), "tools");
      for (String name : compilerNames) {
         File candidate = new File(toolsDir, name);
         if (candidate.exists() && candidate.isFile()) {
            return new CompilerResolutionResult(candidate, true, "Fallback: tools/ directory");
         }
      }

      // 3. Try current working directory - all compiler filenames
      File cwd = new File(System.getProperty("user.dir"));
      for (String name : compilerNames) {
         File candidate = new File(cwd, name);
         if (candidate.exists() && candidate.isFile()) {
            return new CompilerResolutionResult(candidate, true, "Fallback: current directory");
         }
      }

      // 4. Try NCSDecomp installation directory - all compiler filenames
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null && !ncsDecompDir.equals(cwd)) {
         for (String name : compilerNames) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp directory");
            }
         }
         // Also try tools/ subdirectory of NCSDecomp directory
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : compilerNames) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp tools/ directory");
            }
         }
      }

      // Not found
      return null;
   }

   /**
    * Gets the NCSDecomp installation directory.
    * Dynamically determines the base directory from the executable or JAR location.
    * Creates directories as needed rather than traversing directory structures.
    *
    * @return The NCSDecomp base directory (where config/ and tools/ should be created), or user.dir as fallback
    */
   public static File getNCSDecompDirectory() {
      try {
         // Strategy 1: Try to get the executable path (for jpackage EXEs or any native executable)
         String exePath = System.getProperty("java.launcher.path");
         if (exePath != null) {
            File exeFile = new File(exePath);
            if (exeFile.exists() && exeFile.isFile()) {
               File exeDir = exeFile.getParentFile();
               if (exeDir != null && exeDir.exists()) {
                  // Use the directory containing the executable as the base directory
                  // This works for any directory structure - we don't care about subdirectories
                  return exeDir;
               }
            }
         }

         // Strategy 2: Try to get JAR location (for JAR execution)
         java.net.URL location = CompilerUtil.class.getProtectionDomain().getCodeSource().getLocation();
         if (location != null) {
            String path = location.getPath();
            if (path != null) {
               // Handle file: protocol
               if (path.startsWith("file:")) {
                  path = path.substring(5);
               }
               // Decode URL encoding
               try {
                  path = java.net.URLDecoder.decode(path, "UTF-8");
               } catch (java.io.UnsupportedEncodingException e) {
                  // Continue with original path if decoding fails
               }

               File codeSourceFile = new File(path);
               if (codeSourceFile.exists()) {
                  File parent = codeSourceFile.getParentFile();
                  if (parent != null && parent.exists()) {
                     // Use the directory containing the code source (JAR or class file) as base
                     // Don't traverse - just use what we have
                     return parent;
                  }
               }
            }
         }
      } catch (Exception e) {
         // Log but don't fail - fall through to user.dir
         System.err.println("[WARNING] CompilerUtil.getNCSDecompDirectory: Could not determine base directory: " + e.getMessage());
      }

      // Fallback: Use current working directory
      // This ensures the application can always create config/ and tools/ directories
      // relative to where it's being run from
      return new File(System.getProperty("user.dir"));
   }

   /**
    * Returns the standard compiler filenames in priority order.
    * Can be used by CLI or tests that need to search for compilers.
    *
    * @return Array of compiler filenames
    */
   public static String[] getCompilerNames() {
      return COMPILER_NAMES.clone();
   }
}
