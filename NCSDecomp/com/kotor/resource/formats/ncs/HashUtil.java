// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating cryptographic hashes of files.
 * <p>
 * The primary consumer is {@link NwnnsscompConfig}, which fingerprints
 * external nwnnsscomp.exe binaries so we can choose the correct argument
 * schema for that compiler version. Keeping the hashing logic isolated here
 * makes it easy to swap algorithms or add diagnostics later.
 */
public class HashUtil {
   /**
    * Calculates the SHA256 hash of a file.
    *
   * @param file The file to hash
   * @return The SHA256 hash as an uppercase hexadecimal string
   * @throws IOException If the file cannot be read
    */
   public static String calculateSHA256(File file) throws IOException {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
               digest.update(buffer, 0, bytesRead);
            }
         }
         byte[] hashBytes = digest.digest();
         return bytesToHex(hashBytes).toUpperCase();
      } catch (NoSuchAlgorithmException e) {
         throw new IOException("SHA-256 algorithm not available", e);
      }
   }

   /**
    * Converts a byte array to a hexadecimal string without delimiters.
    * <p>
    * Kept private because callers should prefer a stable, high-level API
    * such as {@link #calculateSHA256(File)} that dictates the hashing
    * algorithm and casing.
    *
    * @param bytes The byte array to convert
    * @return The hexadecimal string representation
    */
   private static String bytesToHex(byte[] bytes) {
      StringBuilder hexString = new StringBuilder(2 * bytes.length);
      for (byte b : bytes) {
         String hex = Integer.toHexString(0xff & b);
         if (hex.length() == 1) {
            hexString.append('0');
         }
         hexString.append(hex);
      }
      return hexString.toString();
   }
}


