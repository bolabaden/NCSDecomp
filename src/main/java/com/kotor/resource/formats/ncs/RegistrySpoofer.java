// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;

/**
 * Temporarily spoofs Windows registry keys to point legacy compilers to the correct installation path.
 * <p>
 * Some legacy nwnnsscomp variants (KOTOR Tool, KOTOR Scripting Tool) read the game installation
 * path from the Windows registry instead of accepting command-line arguments. This class provides
 * a context manager pattern to temporarily set the registry keys during compilation/decompilation.
 * <p>
 * Registry paths:
 * <ul>
 *   <li>KotOR 1: HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\BioWare\SW\KOTOR (64-bit) or
 *       HKEY_LOCAL_MACHINE\SOFTWARE\BioWare\SW\KOTOR (32-bit)</li>
 *   <li>KotOR 2: HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\LucasArts\KotOR2 (64-bit) or
 *       HKEY_LOCAL_MACHINE\SOFTWARE\LucasArts\KotOR2 (32-bit)</li>
 * </ul>
 * Key name: "Path"
 */
public class RegistrySpoofer implements AutoCloseable {
   private final String registryPath;
   private final String keyName;
   private final String spoofedPath;
   private String originalValue;
   private boolean wasModified = false;

   /**
    * Creates a new registry spoofer for the specified game.
    *
    * @param installationPath The path to spoof in the registry (typically the tools directory)
    * @param isK2 true for KotOR 2 (TSL), false for KotOR 1
    * @throws UnsupportedOperationException If not running on Windows
    */
   public RegistrySpoofer(File installationPath, boolean isK2) {
      if (!System.getProperty("os.name").toLowerCase().contains("win")) {
         throw new UnsupportedOperationException("Registry spoofing is only supported on Windows");
      }

      this.spoofedPath = installationPath.getAbsolutePath();
      this.keyName = "Path";

      // Determine registry path based on game and architecture
      boolean is64Bit = is64BitArchitecture();
      if (isK2) {
         if (is64Bit) {
            this.registryPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\LucasArts\\KotOR2";
         } else {
            this.registryPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\LucasArts\\KotOR2";
         }
      } else {
         if (is64Bit) {
            this.registryPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\BioWare\\SW\\KOTOR";
         } else {
            this.registryPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\BioWare\\SW\\KOTOR";
         }
      }

      // Read original value
      this.originalValue = readRegistryValue(this.registryPath, this.keyName);
      System.err.println("DEBUG RegistrySpoofer: Created spoofer for " + (isK2 ? "K2" : "K1") +
            ", registryPath=" + this.registryPath + ", spoofedPath=" + this.spoofedPath +
            ", originalValue=" + (this.originalValue != null ? this.originalValue : "(null)"));
   }

   /**
    * Activates the registry spoof by setting the registry key to the spoofed path.
    * This should be called at the start of a try-with-resources block.
    *
    * @return this instance for method chaining
    * @throws SecurityException If registry modification fails due to permissions
    */
   public RegistrySpoofer activate() {
      if (this.originalValue != null && this.originalValue.equals(this.spoofedPath)) {
         System.err.println("DEBUG RegistrySpoofer: Registry value already matches spoofed path, skipping");
         return this;
      }

      try {
         writeRegistryValue(this.registryPath, this.keyName, this.spoofedPath);
         this.wasModified = true;
         System.err.println("DEBUG RegistrySpoofer: Successfully set registry key " + this.registryPath +
               "\\" + this.keyName + " to " + this.spoofedPath);
      } catch (SecurityException e) {
         System.err.println("DEBUG RegistrySpoofer: Failed to set registry key: " + e.getMessage());
         throw new SecurityException("Permission denied. Administrator privileges required to spoof registry. " +
               "Error: " + e.getMessage(), e);
      }
      return this;
   }

   /**
    * Restores the original registry value.
    * This is automatically called when the spoofer is closed in a try-with-resources block.
    */
   @Override
   public void close() {
      if (this.wasModified && this.originalValue != null && !this.originalValue.equals(this.spoofedPath)) {
         try {
            writeRegistryValue(this.registryPath, this.keyName, this.originalValue);
            System.err.println("DEBUG RegistrySpoofer: Restored registry key " + this.registryPath +
                  "\\" + this.keyName + " to original value: " + this.originalValue);
         } catch (Exception e) {
            System.err.println("DEBUG RegistrySpoofer: Failed to restore registry key: " + e.getMessage());
            // Don't throw - we've done our best to restore
         }
      } else if (this.wasModified && this.originalValue == null) {
         // Original value didn't exist - we could try to delete it, but that's more complex
         // Just log a warning
         System.err.println("DEBUG RegistrySpoofer: Registry key was created but original value was null. " +
               "Key will remain set to spoofed path.");
      }
   }

   /**
    * Gets the registry path being spoofed.
    */
   public String getRegistryPath() {
      return registryPath;
   }

   /**
    * Gets the original registry value before spoofing.
    */
   public String getOriginalValue() {
      return originalValue;
   }

   /**
    * Determines if the system is running 64-bit architecture.
    */
   private static boolean is64BitArchitecture() {
      String arch = System.getProperty("os.arch");
      return arch != null && (arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64"));
   }

   /**
    * Reads a registry value using Windows registry commands.
    *
    * @param registryPath Full registry path (e.g., "HKEY_LOCAL_MACHINE\\SOFTWARE\\BioWare\\SW\\KOTOR")
    * @param keyName Name of the value to read
    * @return The registry value, or null if not found
    */
   private static String readRegistryValue(String registryPath, String keyName) {
      try {
         // Parse registry path
         int firstBackslash = registryPath.indexOf('\\');
         if (firstBackslash < 0) {
            return null;
         }
         String hive = registryPath.substring(0, firstBackslash);
         String keyPath = registryPath.substring(firstBackslash + 1);

         // Convert hive name to reg.exe format
         String regHive;
         if (hive.equals("HKEY_LOCAL_MACHINE")) {
            regHive = "HKLM";
         } else if (hive.equals("HKEY_CURRENT_USER")) {
            regHive = "HKCU";
         } else if (hive.equals("HKEY_CLASSES_ROOT")) {
            regHive = "HKCR";
         } else if (hive.equals("HKEY_USERS")) {
            regHive = "HKU";
         } else if (hive.equals("HKEY_CURRENT_CONFIG")) {
            regHive = "HKCC";
         } else {
            return null;
         }

         // Use reg.exe query to read the value
         ProcessBuilder pb = new ProcessBuilder("reg", "query", regHive + "\\" + keyPath, "/v", keyName);
         Process proc = pb.start();

         // Read output
         StringBuilder output = new StringBuilder();
         try (java.io.BufferedReader reader = new java.io.BufferedReader(
               new java.io.InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
               output.append(line).append("\n");
            }
         }

         // Read error output
         StringBuilder errorOutput = new StringBuilder();
         try (java.io.BufferedReader reader = new java.io.BufferedReader(
               new java.io.InputStreamReader(proc.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
               errorOutput.append(line).append("\n");
            }
         }

         int exitCode = proc.waitFor();
         if (exitCode != 0) {
            // Key doesn't exist or access denied
            return null;
         }

         // Parse output: "    Path    REG_SZ    C:\path\to\install"
         String outputStr = output.toString();
         String[] lines = outputStr.split("\n");
         for (String line : lines) {
            if (line.trim().startsWith(keyName)) {
               // Find the value (third column)
               String[] parts = line.trim().split("\\s+", 3);
               if (parts.length >= 3) {
                  return parts[2].trim();
               }
            }
         }
         return null;
      } catch (Exception e) {
         System.err.println("DEBUG RegistrySpoofer: Error reading registry value: " + e.getMessage());
         return null;
      }
   }

   /**
    * Writes a registry value using Windows registry commands.
    *
    * @param registryPath Full registry path (e.g., "HKEY_LOCAL_MACHINE\\SOFTWARE\\BioWare\\SW\\KOTOR")
    * @param keyName Name of the value to write
    * @param value The value to write
    * @throws SecurityException If registry modification fails due to permissions
    */
   private static void writeRegistryValue(String registryPath, String keyName, String value) {
      try {
         // Parse registry path
         int firstBackslash = registryPath.indexOf('\\');
         if (firstBackslash < 0) {
            throw new IllegalArgumentException("Invalid registry path: " + registryPath);
         }
         String hive = registryPath.substring(0, firstBackslash);
         String keyPath = registryPath.substring(firstBackslash + 1);

         // Convert hive name to reg.exe format
         String regHive;
         if (hive.equals("HKEY_LOCAL_MACHINE")) {
            regHive = "HKLM";
         } else if (hive.equals("HKEY_CURRENT_USER")) {
            regHive = "HKCU";
         } else if (hive.equals("HKEY_CLASSES_ROOT")) {
            regHive = "HKCR";
         } else if (hive.equals("HKEY_USERS")) {
            regHive = "HKU";
         } else if (hive.equals("HKEY_CURRENT_CONFIG")) {
            regHive = "HKCC";
         } else {
            throw new IllegalArgumentException("Unsupported registry hive: " + hive);
         }

         // Create the registry path if it doesn't exist
         ProcessBuilder createPb = new ProcessBuilder("reg", "add", regHive + "\\" + keyPath, "/f");
         Process createProc = createPb.start();
         createProc.waitFor(); // Ignore exit code - path might already exist

         // Set the registry value using reg.exe add (creates key if needed)
         // Note: reg.exe add requires /f flag to overwrite existing values
         ProcessBuilder pb = new ProcessBuilder("reg", "add", regHive + "\\" + keyPath,
               "/v", keyName, "/t", "REG_SZ", "/d", value, "/f");
         Process proc = pb.start();

         // Read error output to detect permission errors
         StringBuilder errorOutput = new StringBuilder();
         try (java.io.BufferedReader reader = new java.io.BufferedReader(
               new java.io.InputStreamReader(proc.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
               errorOutput.append(line).append("\n");
            }
         }

         int exitCode = proc.waitFor();
         if (exitCode != 0) {
            String errorMsg = errorOutput.toString();
            if (errorMsg.contains("access") || errorMsg.contains("denied") || errorMsg.contains("privilege")) {
               throw new SecurityException("Access denied. Administrator privileges required. " + errorMsg);
            }
            throw new RuntimeException("Failed to set registry value. Exit code: " + exitCode + ", Error: " + errorMsg);
         }
      } catch (SecurityException e) {
         throw e;
      } catch (Exception e) {
         if (e.getCause() instanceof SecurityException) {
            throw (SecurityException) e.getCause();
         }
         throw new RuntimeException("Error writing registry value: " + e.getMessage(), e);
      }
   }
}
