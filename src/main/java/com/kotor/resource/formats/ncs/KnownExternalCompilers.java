// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known external NSS compilers and their command-line schemas.
 * <p>
 * Each entry encapsulates the SHA256 fingerprint for a particular
 * nwnnsscomp.exe (or compatible) build plus the exact argument templates
 * required to compile or decompile. This allows {@link NwnnsscompConfig} to
 * build the right invocation without hard-coding switch sets per version.
 * <p>
 * References for recorded hashes and argument shapes:
 * <ul>
 *    <li>vendor/TSLPatcher/TSLPatcher.pl - Original Perl TSLPatcher implementation</li>
 *    <li>vendor/Kotor.NET/Kotor.NET.Patcher/ - Incomplete C# patcher</li>
 *    <li>vendor/xoreos-tools/src/nwscript/compiler.cpp - Xoreos compiler</li>
 * </ul>
 */
public enum KnownExternalCompilers {
   /**
    * TSLPatcher compiler version.
    */
   TSLPATCHER(
      "539EB689D2E0D3751AEED273385865278BEF6696C46BC0CAB116B40C3B2FE820",
      "TSLPatcher",
      LocalDate.of(2009, 1, 1),
      "todo",
      new String[]{"-c", "{source}", "-o", "{output}"},
      new String[]{"-d", "{source}", "-o", "{output}"}
   ),

   /**
    * KOTOR Tool compiler version.
    */
   KOTOR_TOOL(
      "E36AA3172173B654AE20379888EDDC9CF45C62FBEB7AB05061C57B52961C824D",
      "KOTOR Tool",
      LocalDate.of(2005, 1, 1),
      "Fred Tetra",
      new String[]{"-c", "--outputdir", "{output_dir}", "-o", "{output_name}", "-g", "{game_value}", "{source}"},
      new String[]{"-d", "--outputdir", "{output_dir}", "-o", "{output_name}", "-g", "{game_value}", "{source}"}
   ),

   /**
    * v1.3 first public release.
    */
   V1(
      "EC3E657C18A32AD13D28DA0AA3A77911B32D9661EA83CF0D9BCE02E1C4D8499D",
      "v1.3 first public release",
      LocalDate.of(2003, 12, 31),
      "todo",
      new String[]{"-c", "{source}", "{output}"},
      new String[]{"-d", "{source}", "{output}"}
   ),

   /**
    * KOTOR Scripting Tool compiler.
    */
   KOTOR_SCRIPTING_TOOL(
      "B7344408A47BE8780816CF68F5A171A09640AB47AD1A905B7F87DE30A50A0A92",
      "KOTOR Scripting Tool",
      LocalDate.of(2016, 5, 18),
      "James Goad", // TODO: double check
      new String[]{"-c", "--outputdir", "{output_dir}", "-o", "{output_name}", "-g", "{game_value}", "{source}"},
      new String[]{"-d", "--outputdir", "{output_dir}", "-o", "{output_name}", "-g", "{game_value}", "{source}"}
   ),

   /**
    * DeNCS compiler (same hash as TSLPATCHER in original Python code).
    */
   DENCS(
      "539EB689D2E0D3751AEED273385865278BEF6696C46BC0CAB116B40C3B2FE820",
      "DeNCS",
      LocalDate.of(2006, 5, 30),
      "todo",
      new String[]{"-c", "{source}", "-o", "{output}"},
      new String[]{"-d", "{source}", "-o", "{output}"}
   ),

   /**
    * Xoreos Tools compiler (primarily for engine reimplementation).
    */
   XOREOS(
      "",
      "Xoreos Tools",
      LocalDate.of(2016, 1, 1), // Approximate based on project history
      "Xoreos Team",
      new String[]{}, // Xoreos tools are primarily for engine reimplementation
      new String[]{}
   ),

   /**
    * knsscomp - Nick Hugi's modern NSS compiler.
    */
   KNSSCOMP(
      "", // TODO: Obtain hash from actual knsscomp binary
      "knsscomp",
      LocalDate.of(2022, 1, 1), // Approximate
      "Nick Hugi",
      new String[]{"-c", "{source}", "-o", "{output}"},
      new String[]{} // knsscomp doesn't support decompilation
   );

   private final String sha256;
   /** Human-readable compiler name for logs and UI. */
   private final String name;
   private final LocalDate releaseDate;
   /** Author/maintainer attribution for traceability. */
   private final String author;
   /** Argument template to compile; placeholders resolved by {@link NwnnsscompConfig}. */
   private final String[] compileArgs;
   /** Argument template to decompile; empty when the compiler does not support it. */
   private final String[] decompileArgs;

   /** Cache for quick lookup by SHA256 to avoid repeated iteration of enum values. */
   private static final Map<String, KnownExternalCompilers> BY_HASH = new HashMap<>();

   static {
      for (KnownExternalCompilers compiler : values()) {
         if (compiler.sha256 != null && !compiler.sha256.isEmpty()) {
            BY_HASH.put(compiler.sha256.toUpperCase(), compiler);
         }
      }
   }

   /**
    * Records immutable metadata for a compiler version.
    *
    * @param sha256 Fingerprint of the binary to match against
    * @param name Display name used in logs and UI
    * @param releaseDate Approximate release date for context
    * @param author Attribution of the compiler build
    * @param compileArgs Argument template for compilation; placeholders are resolved later
    * @param decompileArgs Argument template for decompilation; may be empty if unsupported
    */
   KnownExternalCompilers(String sha256, String name, LocalDate releaseDate, String author,
         String[] compileArgs, String[] decompileArgs) {
      this.sha256 = sha256;
      this.name = name;
      this.releaseDate = releaseDate;
      this.author = author;
      this.compileArgs = compileArgs;
      this.decompileArgs = decompileArgs;
   }

   /**
    * Gets the SHA256 hash of this compiler version.
    *
    * @return The SHA256 hash
    */
   public String getSha256() {
      return sha256;
   }

   /**
    * Gets the display name of this compiler.
    *
    * @return The compiler name
    */
   public String getName() {
      return name;
   }

   /**
    * Gets the release date of this compiler version.
    *
    * @return The release date
    */
   public LocalDate getReleaseDate() {
      return releaseDate;
   }

   /**
    * Gets the author of this compiler.
    *
    * @return The author name
    */
   public String getAuthor() {
      return author;
   }

   /**
    * Gets the compile command-line arguments template.
    *
    * @return Array of argument templates
    */
   public String[] getCompileArgs() {
      return compileArgs.clone();
   }

   /**
    * Gets the decompile command-line arguments template.
    *
    * @return Array of argument templates
    */
   public String[] getDecompileArgs() {
      return decompileArgs.clone();
   }

   /**
    * Looks up a compiler by its SHA256 hash.
    *
    * @param sha256 The SHA256 hash (case-insensitive)
    * @return The matching compiler, or null if not found
    */
   public static KnownExternalCompilers fromSha256(String sha256) {
      if (sha256 == null || sha256.isEmpty()) {
         return null;
      }
      return BY_HASH.get(sha256.toUpperCase());
   }

   /**
    * Checks if this compiler supports decompilation.
    *
    * @return true if decompilation is supported, false otherwise
    */
   public boolean supportsDecompilation() {
      return decompileArgs != null && decompileArgs.length > 0;
   }
}


