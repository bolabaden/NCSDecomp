// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.io.IOException;

/**
 * Resolves and formats command-line arguments for nwnnsscomp across versions.
 * <p>
 * Different nwnnsscomp releases are not backwards compatible, so we detect the
 * executable by SHA256 ({@link HashUtil}) and then hydrate the correct
 * argument template from {@link KnownExternalCompilers}. Consumers only need
 * to supply the executable path plus the source/target files and game flavor.
 */
public class NwnnsscompConfig {
   /** Detected SHA256 fingerprint of the provided compiler binary. */
   private final String sha256Hash;
   /** Source script being compiled or decompiled. */
   private final File sourceFile;
   /** Destination file path to be produced by the compiler. */
   private final File outputFile;
   /** Parent directory of the output file, used by some argument templates. */
   private final File outputDir;
   /** Basename of the output file, used when templates require only a name. */
   private final String outputName;
   /** True when targeting KotOR 2 (TSL) arguments, false for KotOR 1. */
   private final boolean isK2;
   /** Selected compiler metadata derived from the SHA256 fingerprint. */
   private final KnownExternalCompilers chosenCompiler;

   /**
    * Creates a new configuration for nwnnsscomp execution.
    *
    * @param compilerPath Path to the nwnnsscomp.exe file
    * @param sourceFile The source file to compile/decompile
    * @param outputFile The output file path
    * @param isK2 true for KotOR 2 (TSL), false for KotOR 1
    * @throws IOException If the compiler file cannot be read or hashed
    * @throws IllegalArgumentException If the compiler version is not recognized
    */
   public NwnnsscompConfig(File compilerPath, File sourceFile, File outputFile, boolean isK2)
         throws IOException {
      this.sourceFile = sourceFile;
      this.outputFile = outputFile;
      this.outputDir = outputFile.getParentFile();
      this.outputName = outputFile.getName();
      this.isK2 = isK2;

      // Calculate hash of the compiler executable
      this.sha256Hash = HashUtil.calculateSHA256(compilerPath);

      // Look up the compiler version
      this.chosenCompiler = KnownExternalCompilers.fromSha256(this.sha256Hash);
      if (this.chosenCompiler == null) {
         throw new IllegalArgumentException(
            "Unknown compiler version with SHA256 hash: " + this.sha256Hash +
            ". This compiler may not be supported. Please use a known version of nwnnsscomp.exe.");
      }
   }

   /**
    * Gets the formatted compile command-line arguments.
    *
    * @param executable Path to the nwnnsscomp executable
    * @return Array of command-line arguments
    */
   public String[] getCompileArgs(String executable) {
      return formatArgs(chosenCompiler.getCompileArgs(), executable);
   }

   /**
    * Gets the formatted decompile command-line arguments.
    *
    * @param executable Path to the nwnnsscomp executable
    * @return Array of command-line arguments
    * @throws UnsupportedOperationException If decompilation is not supported
    */
   public String[] getDecompileArgs(String executable) {
      if (!chosenCompiler.supportsDecompilation()) {
         throw new UnsupportedOperationException(
            "Compiler '" + chosenCompiler.getName() + "' does not support decompilation");
      }
      return formatArgs(chosenCompiler.getDecompileArgs(), executable);
   }

   /**
    * Formats the argument template with actual values.
    *
    * @param argsList The argument template array
    * @param executable The executable path
   * @return Formatted argument array where placeholders ({@code {source}}, {@code {output}},
   * {@code {output_dir}}, {@code {output_name}}, {@code {game_value}}) are replaced
    */
   private String[] formatArgs(String[] argsList, String executable) {
      String[] formatted = new String[argsList.length];
      for (int i = 0; i < argsList.length; i++) {
         String arg = argsList[i];
         formatted[i] = arg
            .replace("{source}", sourceFile.getAbsolutePath())
            .replace("{output}", outputFile.getAbsolutePath())
            .replace("{output_dir}", outputDir != null ? outputDir.getAbsolutePath() : "")
            .replace("{output_name}", outputName)
            .replace("{game_value}", isK2 ? "2" : "1");
      }

      // Prepend the executable path
      String[] result = new String[formatted.length + 1];
      result[0] = executable;
      System.arraycopy(formatted, 0, result, 1, formatted.length);
      return result;
   }

   /**
    * Gets the detected compiler information.
    *
    * @return The detected compiler enum value
    */
   public KnownExternalCompilers getChosenCompiler() {
      return chosenCompiler;
   }

   /**
    * Gets the SHA256 hash of the compiler executable.
    *
    * @return The SHA256 hash
    */
   public String getSha256Hash() {
      return sha256Hash;
   }
}


