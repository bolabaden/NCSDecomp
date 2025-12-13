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

      // 2. Try NCSDecomp installation directory's tools/ FIRST (app directory)
      // This ensures the packaged app finds its bundled tools
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null) {
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : COMPILER_NAMES) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in NCSDecomp tools/: " + candidate.getAbsolutePath());
               return candidate;
            }
         }
         // Also try NCSDecomp directory itself
         for (String name : COMPILER_NAMES) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists() && candidate.isFile()) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in NCSDecomp dir: " + candidate.getAbsolutePath());
               return candidate;
            }
         }
      }

      // 3. Try CWD tools/ directory (if different from app directory)
      Path cwd = Paths.get(System.getProperty("user.dir"));
      if (ncsDecompDir == null || !ncsDecompDir.equals(cwd.toFile())) {
         Path toolsDir = cwd.resolve("tools");
         for (String name : COMPILER_NAMES) {
            Path candidate = toolsDir.resolve(name);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in CWD tools/: " + candidate);
               return candidate.toFile();
            }
         }

         // 4. Try current working directory itself
         for (String name : COMPILER_NAMES) {
            Path candidate = cwd.resolve(name);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
               System.out.println("[INFO] CompilerUtil.resolveCompilerPathWithFallbacks: Found in cwd: " + candidate);
               return candidate.toFile();
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

      // 2. Try NCSDecomp installation directory's tools/ FIRST (app directory)
      // This ensures the packaged app finds its bundled tools
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null) {
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : compilerNames) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp tools/ directory");
            }
         }
         // Also try NCSDecomp directory itself
         for (String name : compilerNames) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: NCSDecomp directory");
            }
         }
      }

      // 3. Try CWD tools/ directory - all compiler filenames (if different from app dir)
      File cwd = new File(System.getProperty("user.dir"));
      if (ncsDecompDir == null || !ncsDecompDir.equals(cwd)) {
         File toolsDir = new File(cwd, "tools");
         for (String name : compilerNames) {
            File candidate = new File(toolsDir, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: CWD tools/ directory");
            }
         }

         // 4. Try current working directory itself - all compiler filenames
         for (String name : compilerNames) {
            File candidate = new File(cwd, name);
            if (candidate.exists() && candidate.isFile()) {
               return new CompilerResolutionResult(candidate, true, "Fallback: current directory");
            }
         }
      }

      // Not found
      return null;
   }

   /**
    * Gets the NCSDecomp installation directory (the app root where config/ and tools/ are located).
    * <p>
    * This method handles multiple execution contexts:
    * <ul>
    *   <li>jpackage EXE: Returns the directory containing the EXE (e.g., NCSDecomp/)</li>
    *   <li>JAR in jpackage app/: Returns the app ROOT (parent of app/), not app/ itself</li>
    *   <li>Standalone JAR: Returns the directory containing the JAR</li>
    *   <li>Fallback: Returns current working directory</li>
    * </ul>
    *
    * @return The NCSDecomp base directory (where config/ and tools/ should exist), or user.dir as fallback
    */
   public static File getNCSDecompDirectory() {
      // Cache the result to avoid repeated filesystem checks
      if (cachedNCSDecompDirectory != null) {
         return cachedNCSDecompDirectory;
      }

      File result = detectNCSDecompDirectory();
      cachedNCSDecompDirectory = result;
      return result;
   }

   /** Cached result of getNCSDecompDirectory() to avoid repeated filesystem checks. */
   private static File cachedNCSDecompDirectory = null;

   /**
    * Clears the cached NCSDecomp directory. Useful for testing.
    */
   public static void clearNCSDecompDirectoryCache() {
      cachedNCSDecompDirectory = null;
   }

   /**
    * Internal method to detect the NCSDecomp installation directory.
    */
   private static File detectNCSDecompDirectory() {
      try {
         // Strategy 1: Try jpackage-specific system property (jdk.module.path or java.home)
         // For jpackage apps, java.home points to the bundled runtime inside the app
         // E.g., NCSDecomp/runtime/... so we go up to find NCSDecomp/
         String javaHome = System.getProperty("java.home");
         if (javaHome != null && !javaHome.isEmpty()) {
            File javaHomeDir = new File(javaHome);
            // Check if this looks like a jpackage runtime directory
            // jpackage structure: AppName/runtime/bin, AppName/runtime/lib, etc.
            // If java.home contains "runtime", it's likely a jpackage app
            String javaHomePath = javaHomeDir.getAbsolutePath().replace('\\', '/');
            if (javaHomePath.contains("/runtime")) {
               // Navigate up from runtime to app root
               File current = javaHomeDir;
               while (current != null) {
                  if ("runtime".equalsIgnoreCase(current.getName())) {
                     File appRoot = current.getParentFile();
                     if (appRoot != null && appRoot.exists()) {
                        // Verify this looks like an app root (has app/ or tools/ or config/)
                        if (new File(appRoot, "app").exists() ||
                            new File(appRoot, "tools").exists() ||
                            new File(appRoot, "config").exists()) {
                           System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using jpackage app root (from java.home): " + appRoot.getAbsolutePath());
                           return appRoot;
                        }
                     }
                     break;
                  }
                  current = current.getParentFile();
               }
            }
         }

         // Strategy 2: Try to get JAR/class location
         java.net.URL location = CompilerUtil.class.getProtectionDomain().getCodeSource().getLocation();
         if (location != null) {
            String path = location.toURI().getPath();
            if (path != null) {
               // Decode URL encoding (spaces become %20, etc.)
               try {
                  path = java.net.URLDecoder.decode(path, "UTF-8");
               } catch (java.io.UnsupportedEncodingException e) {
                  // Continue with original path if decoding fails
               }
               // On Windows, remove leading slash from paths like /C:/...
               if (path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':') {
                  path = path.substring(1);
               }

               File codeSourceFile = new File(path);
               // If it's a directory (running from classes), use it directly
               if (codeSourceFile.exists()) {
                  File parent;
                  if (codeSourceFile.isDirectory()) {
                     // Running from compiled classes (e.g., target/classes)
                     // Walk up to find project root
                     parent = codeSourceFile;
                     // Look for tools/ or config/ directory going up
                     for (int i = 0; i < 5 && parent != null; i++) {
                        if (new File(parent, "tools").exists() || new File(parent, "config").exists()) {
                           System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using classes root directory: " + parent.getAbsolutePath());
                           return parent;
                        }
                        parent = parent.getParentFile();
                     }
                     // Fallback to original directory
                     parent = codeSourceFile;
                  } else {
                     // It's a JAR file
                     parent = codeSourceFile.getParentFile();
                  }

                  if (parent != null && parent.exists()) {
                     // For jpackage apps, JAR is in app/ subdirectory, but config/tools are at app ROOT
                     // Detect jpackage structure: if parent is named "app", go up one more level
                     if ("app".equalsIgnoreCase(parent.getName())) {
                        File appRoot = parent.getParentFile();
                        if (appRoot != null && appRoot.exists()) {
                           System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using jpackage app root (from JAR): " + appRoot.getAbsolutePath());
                           return appRoot;
                        }
                     }
                     // Check if this directory has tools/ or config/ - good sign it's the app root
                     if (new File(parent, "tools").exists() || new File(parent, "config").exists()) {
                        System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using JAR parent directory: " + parent.getAbsolutePath());
                        return parent;
                     }
                     // Try going up one more level (JAR might be in a subdirectory)
                     File grandparent = parent.getParentFile();
                     if (grandparent != null && grandparent.exists()) {
                        if (new File(grandparent, "tools").exists() || new File(grandparent, "config").exists()) {
                           System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using JAR grandparent directory: " + grandparent.getAbsolutePath());
                           return grandparent;
                        }
                     }
                     // Return parent even if no tools/config found
                     System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using JAR parent directory (no tools/config): " + parent.getAbsolutePath());
                     return parent;
                  }
               }
            }
         }
      } catch (Exception e) {
         // Log but don't fail - fall through to user.dir
         System.err.println("[WARNING] CompilerUtil.getNCSDecompDirectory: Could not determine base directory: " + e.getMessage());
      }

      // Strategy 3: Check if CWD has tools/ or config/ directories
      File cwd = new File(System.getProperty("user.dir"));
      if (new File(cwd, "tools").exists() || new File(cwd, "config").exists()) {
         System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using CWD (has tools/config): " + cwd.getAbsolutePath());
         return cwd;
      }

      // Fallback: Use current working directory
      System.out.println("[INFO] CompilerUtil.getNCSDecompDirectory: Using CWD fallback: " + cwd.getAbsolutePath());
      return cwd;
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

   /**
    * Gets the tools directory (where compilers and nwscript files are located).
    * <p>
    * Searches in order:
    * <ol>
    *   <li>App directory's tools/ subdirectory (getNCSDecompDirectory()/tools)</li>
    *   <li>Current working directory's tools/ subdirectory</li>
    *   <li>Creates and returns app directory's tools/ if nothing exists</li>
    * </ol>
    *
    * @return The tools directory (may not exist yet)
    */
   public static File getToolsDirectory() {
      // First, try app directory
      File appDir = getNCSDecompDirectory();
      File appToolsDir = new File(appDir, "tools");
      if (appToolsDir.exists() && appToolsDir.isDirectory()) {
         return appToolsDir;
      }

      // Second, try CWD (if different from app dir)
      File cwd = new File(System.getProperty("user.dir"));
      if (!cwd.equals(appDir)) {
         File cwdToolsDir = new File(cwd, "tools");
         if (cwdToolsDir.exists() && cwdToolsDir.isDirectory()) {
            System.out.println("[INFO] CompilerUtil.getToolsDirectory: Using CWD tools directory: " + cwdToolsDir.getAbsolutePath());
            return cwdToolsDir;
         }
      }

      // Default to app directory's tools (will be created if needed)
      return appToolsDir;
   }

   /**
    * Resolves a file in the tools directory.
    * <p>
    * Searches for the file in:
    * <ol>
    *   <li>App directory's tools/ subdirectory</li>
    *   <li>Current working directory's tools/ subdirectory</li>
    *   <li>App directory itself</li>
    *   <li>Current working directory itself</li>
    * </ol>
    *
    * @param filename The filename to search for (e.g., "k1_nwscript.nss")
    * @return The resolved File (may not exist), preferring app directory locations
    */
   public static File resolveToolsFile(String filename) {
      File appDir = getNCSDecompDirectory();
      File cwd = new File(System.getProperty("user.dir"));

      // Search locations in priority order
      File[] candidates = {
         new File(appDir, "tools" + File.separator + filename),  // app/tools/
         new File(cwd, "tools" + File.separator + filename),      // cwd/tools/
         new File(appDir, filename),                               // app/
         new File(cwd, filename)                                   // cwd/
      };

      for (File candidate : candidates) {
         if (candidate.exists() && candidate.isFile()) {
            System.out.println("[INFO] CompilerUtil.resolveToolsFile: Found '" + filename + "' at: " + candidate.getAbsolutePath());
            return candidate;
         }
      }

      // Return the preferred location (app/tools/) even if it doesn't exist
      File defaultLocation = new File(appDir, "tools" + File.separator + filename);
      System.out.println("[INFO] CompilerUtil.resolveToolsFile: '" + filename + "' not found, default: " + defaultLocation.getAbsolutePath());
      return defaultLocation;
   }

   /**
    * Resolves a file in the config directory.
    * <p>
    * Searches for the file in:
    * <ol>
    *   <li>App directory's config/ subdirectory</li>
    *   <li>Current working directory's config/ subdirectory</li>
    * </ol>
    *
    * @param filename The filename to search for (e.g., "ncsdecomp.conf")
    * @return The resolved File (may not exist), preferring app directory locations
    */
   public static File resolveConfigFile(String filename) {
      File appDir = getNCSDecompDirectory();
      File cwd = new File(System.getProperty("user.dir"));

      // Search locations in priority order
      File[] candidates = {
         new File(appDir, "config" + File.separator + filename),  // app/config/
         new File(cwd, "config" + File.separator + filename)      // cwd/config/
      };

      for (File candidate : candidates) {
         if (candidate.exists() && candidate.isFile()) {
            System.out.println("[INFO] CompilerUtil.resolveConfigFile: Found '" + filename + "' at: " + candidate.getAbsolutePath());
            return candidate;
         }
      }

      // Return the preferred location (app/config/) even if it doesn't exist
      File defaultLocation = new File(appDir, "config" + File.separator + filename);
      System.out.println("[INFO] CompilerUtil.resolveConfigFile: '" + filename + "' not found, default: " + defaultLocation.getAbsolutePath());
      return defaultLocation;
   }
}
