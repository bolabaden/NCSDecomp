// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Shared utility class for round-trip decompilation operations.
 * <p>
 * This class provides the same round-trip logic used by the test suite,
 * allowing both the GUI and CLI to perform consistent NSS->NCS->NSS round-trips.
 * <p>
 * The round-trip process:
 * 1. Compile NSS to NCS (done externally via nwnnsscomp)
 * 2. Decompile NCS back to NSS (using FileDecompiler)
 * <p>
 * This matches the exact logic in NCSDecompCLIRoundTripTest.runDecompile().
 */
public class RoundTripUtil {

   /**
    * Decompiles an NCS file to NSS using the same logic as the round-trip test.
    * This is the standard method for getting round-trip decompiled code.
    *
    * @param ncsFile The NCS file to decompile
    * @param gameFlag The game flag ("k1" or "k2")
    * @return The decompiled NSS code as a string, or null if decompilation fails
    * @throws DecompilerException If decompilation fails
    */
   public static String decompileNcsToNss(File ncsFile, String gameFlag) throws DecompilerException {
      if (ncsFile == null || !ncsFile.exists()) {
         return null;
      }

      // Set game flag (matches test behavior)
      boolean wasK2 = FileDecompiler.isK2Selected;
      try {
         FileDecompiler.isK2Selected = "k2".equals(gameFlag);

         // Create a temporary output file (matches test pattern)
         File tempNssFile;
         try {
            tempNssFile = File.createTempFile("roundtrip_", ".nss");
            tempNssFile.deleteOnExit();
         } catch (java.io.IOException e) {
            throw new DecompilerException("Failed to create temp file: " + e.getMessage(), e);
         }

         try {
            // Use the same decompile method as the test
            FileDecompiler decompiler = new FileDecompiler();
            // Ensure actions are loaded before decompiling (required for decompilation)
            try {
               decompiler.loadActionsData("k2".equals(gameFlag));
            } catch (DecompilerException e) {
               throw new DecompilerException("Failed to load actions data: " + e.getMessage(), e);
            }
            try {
               decompiler.decompileToFile(ncsFile, tempNssFile, StandardCharsets.UTF_8, true);
            } catch (java.io.IOException e) {
               throw new DecompilerException("Failed to decompile file: " + e.getMessage(), e);
            }

            // Read the decompiled code
            if (tempNssFile.exists() && tempNssFile.isFile() && tempNssFile.length() > 0) {
               try {
                  return new String(java.nio.file.Files.readAllBytes(tempNssFile.toPath()), StandardCharsets.UTF_8);
               } catch (java.io.IOException e) {
                  throw new DecompilerException("Failed to read decompiled file: " + e.getMessage(), e);
               }
            }
         } finally {
            // Clean up temp file
            try {
               if (tempNssFile.exists()) {
                  tempNssFile.delete();
               }
            } catch (Exception e) {
               // Ignore cleanup errors
            }
         }

         return null;
      } finally {
         // Restore original game flag
         FileDecompiler.isK2Selected = wasK2;
      }
   }

   /**
    * Decompiles an NCS file to NSS and writes to the specified output file.
    * This matches the test's runDecompile method exactly.
    *
    * @param ncsFile The NCS file to decompile
    * @param nssOutputFile The output NSS file
    * @param gameFlag The game flag ("k1" or "k2")
    * @param charset The charset to use for writing (defaults to UTF-8 if null)
    * @throws DecompilerException If decompilation fails
    * @throws java.io.IOException If file I/O fails
    */
   public static void decompileNcsToNssFile(File ncsFile, File nssOutputFile, String gameFlag, Charset charset) throws DecompilerException, java.io.IOException {
      if (ncsFile == null || !ncsFile.exists()) {
         throw new DecompilerException("NCS file does not exist: " + (ncsFile != null ? ncsFile.getAbsolutePath() : "null"));
      }

      if (charset == null) {
         charset = StandardCharsets.UTF_8;
      }

      // Set game flag (matches test behavior)
      boolean wasK2 = FileDecompiler.isK2Selected;
      try {
         FileDecompiler.isK2Selected = "k2".equals(gameFlag);

         // Ensure output directory exists
         File parentDir = nssOutputFile.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
            System.out.println("[INFO] RoundTripUtil: CREATING parent directory: " + parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
               System.err.println("[ERROR] RoundTripUtil: Failed to create parent directory: " + parentDir.getAbsolutePath());
               throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }
            System.out.println("[INFO] RoundTripUtil: Created parent directory: " + parentDir.getAbsolutePath());
         }

         // Use the same decompile method as the test
         FileDecompiler decompiler = new FileDecompiler();
         // Ensure actions are loaded before decompiling (required for decompilation)
         decompiler.loadActionsData("k2".equals(gameFlag));
         decompiler.decompileToFile(ncsFile, nssOutputFile, charset, true);

         if (!nssOutputFile.exists()) {
            throw new DecompilerException("Decompile did not produce output file: " + nssOutputFile.getAbsolutePath());
         }
      } finally {
         // Restore original game flag
         FileDecompiler.isK2Selected = wasK2;
      }
   }

   /**
    * Gets the round-trip decompiled code by finding and decompiling the recompiled NCS file.
    * After compileAndCompare runs, the recompiled NCS should be in the same directory as the saved NSS file.
    *
    * @param savedNssFile The saved NSS file (after compilation, this should have a corresponding .ncs file)
    * @param gameFlag The game flag ("k1" or "k2")
    * @return Round-trip decompiled NSS code, or null if not available
    */
   public static String getRoundTripDecompiledCode(File savedNssFile, String gameFlag) {
      try {
         if (savedNssFile == null || !savedNssFile.exists()) {
            return null;
         }

         // Find the recompiled NCS file (should be in same directory, with .ncs extension)
         // This matches how FileDecompiler.externalCompile creates the output
         String nssName = savedNssFile.getName();
         String baseName = nssName;
         int lastDot = nssName.lastIndexOf('.');
         if (lastDot > 0) {
            baseName = nssName.substring(0, lastDot);
         }
         File recompiledNcsFile = new File(savedNssFile.getParentFile(), baseName + ".ncs");

         if (!recompiledNcsFile.exists()) {
            return null;
         }

         // Decompile the recompiled NCS file using the same method as the test
         return decompileNcsToNss(recompiledNcsFile, gameFlag);
      } catch (DecompilerException e) {
         System.err.println("Error getting round-trip decompiled code: " + e.getMessage());
         e.printStackTrace();
         return null;
      } catch (Exception e) {
         System.err.println("Error getting round-trip decompiled code: " + e.getMessage());
         e.printStackTrace();
         return null;
      }
   }
}

