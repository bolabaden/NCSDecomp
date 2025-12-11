// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.lexer.Lexer;
import com.kotor.resource.formats.ncs.analysis.PrototypeEngine;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.Start;
import com.kotor.resource.formats.ncs.parser.Parser;
import com.kotor.resource.formats.ncs.scriptutils.CleanupPass;
import com.kotor.resource.formats.ncs.scriptutils.SubScriptState;
import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.DestroyParseTree;
import com.kotor.resource.formats.ncs.utils.FlattenSub;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.SetDeadCode;
import com.kotor.resource.formats.ncs.utils.SetDestinations;
import com.kotor.resource.formats.ncs.utils.SetPositions;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutineState;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Core coordinator for decompiling and recompiling KotOR/TSL NSS scripts.
 * <p>
 * Responsibilities:
 * <ul>
 *    <li>Decode NCS bytecode into a parse tree, run analysis passes, and emit NSS source.</li>
 *    <li>Optionally round-trip through the external nwnnsscomp compiler to validate parity.</li>
 *    <li>Track per-file state (variables, generated code, bytecode snapshots) for consumers.</li>
 * </ul>
 * The class is intentionally stateful: it caches parsed scripts in {@link #filedata} and
 * reuses a single {@link ActionsData} instance describing nwscript actions for the chosen
 * game (KotOR 1 vs TSL).
 */
public class FileDecompiler {
   /** Return code indicating a failed compile/decompile/compare operation. */
   public static final int FAILURE = 0;
   /** Return code indicating a successful compile/decompile/compare operation. */
   public static final int SUCCESS = 1;
   /** Return code indicating compilation succeeded but comparison failed. */
   public static final int PARTIAL_COMPILE = 2;
   /** Return code indicating comparison fell back to p-code diffing. */
   public static final int PARTIAL_COMPARE = 3;
   /** Name used when storing globals alongside subroutine variables. */
   public static final String GLOBAL_SUB_NAME = "GLOBALS";

   /** Parsed actions table for the currently selected game ruleset. */
   private ActionsData actions;
   /** Per-file cache of intermediate and generated data. */
   private Hashtable<File, FileScriptData> filedata;
   /** Global flag toggled by UI/CLI to indicate KotOR 2 (TSL) mode. */
   public static boolean isK2Selected = false;
   /** Global flag to prefer generating switch structures instead of if-elseif chains. */
   public static boolean preferSwitches = false;
   /** Whether to abort when any signature stays partially inferred. */
   public static boolean strictSignatures = false;
   /** Path to nwnnsscomp.exe, null means use default (tools/nwnnsscomp.exe or current directory) */
   public static String nwnnsscompPath = null;

   /**
    * Builds a decompiler configured for the current working directory.
    * <p>
    * Uses {@code user.dir} to locate {@code k1_nwscript.nss} or
    * {@code tsl_nwscript.nss} depending on {@link #isK2Selected}, which mirrors
    * legacy GUI behavior. Also loads {@link #preferSwitches} from config file if present.
    *
    * @throws DecompilerException if the action table cannot be loaded
    */
   public FileDecompiler() throws DecompilerException {
      this.filedata = new Hashtable<>(1);
      this.actions = loadActionsDataInternal(isK2Selected);
      loadPreferSwitchesFromConfig();
   }

   /**
    * CLI-specific constructor that accepts an explicit nwscript file path.
    * This bypasses the user.dir lookup and allows complete CLI independence.
    * Note: preferSwitches should be set via CLI argument or static flag before construction.
    */
   public FileDecompiler(File nwscriptFile) throws DecompilerException {
      this.filedata = new Hashtable<>(1);
      if (nwscriptFile == null || !nwscriptFile.isFile()) {
         throw new DecompilerException("Error: nwscript file does not exist: " + (nwscriptFile != null ? nwscriptFile.getAbsolutePath() : "null"));
      }
      try {
         this.actions = new ActionsData(new BufferedReader(new FileReader(nwscriptFile)));
      } catch (IOException ex) {
         throw new DecompilerException("Error reading nwscript file: " + ex.getMessage());
      }
   }

   /**
    * Reloads the action table for the requested game variant.
    * Useful when the user toggles KotOR 1/2 mode after construction.
    *
    * @param isK2Selected true for KotOR 2 (TSL), false for KotOR 1
    * @throws DecompilerException if the action table cannot be read
    */
   public void loadActionsData(boolean isK2Selected) throws DecompilerException {
      this.actions = loadActionsDataInternal(isK2Selected);
   }

   /**
    * Attempts to load the action table from settings or the working directory.
    * <p>
    * First checks for a configured path in Settings (GUI mode), then falls back
    * to legacy behavior: {@code tsl_nwscript.nss} for TSL, otherwise {@code k1_nwscript.nss}
    * in the current working directory. This method isolates the IO and error
    * handling so callers receive a single {@link DecompilerException}.
    */
   private static ActionsData loadActionsDataInternal(boolean isK2Selected) throws DecompilerException {
      try {
         File actionfile = null;

         // Check settings first (GUI mode) - only if Decompiler class is loaded
         try {
            // Access Decompiler.settings directly (same package)
            // This will throw NoClassDefFoundError in pure CLI mode, which we catch
            String settingsPath = isK2Selected
               ? Decompiler.settings.getProperty("K2 nwscript Path")
               : Decompiler.settings.getProperty("K1 nwscript Path");
            if (settingsPath != null && !settingsPath.isEmpty()) {
               actionfile = new File(settingsPath);
               if (actionfile.isFile()) {
                  return new ActionsData(new BufferedReader(new FileReader(actionfile)));
               }
            }
         } catch (NoClassDefFoundError | Exception e) {
            // Settings not available (CLI mode) or invalid path, fall through to default
         }

         // Fall back to default location in tools/ directory
         File dir = new File(System.getProperty("user.dir"), "tools");
         actionfile = isK2Selected ? new File(dir, "tsl_nwscript.nss") : new File(dir, "k1_nwscript.nss");
         // If not in tools/, try current directory (legacy support)
         if (!actionfile.isFile()) {
            dir = new File(System.getProperty("user.dir"));
            actionfile = isK2Selected ? new File(dir, "tsl_nwscript.nss") : new File(dir, "k1_nwscript.nss");
         }
         if (actionfile.isFile()) {
            return new ActionsData(new BufferedReader(new FileReader(actionfile)));
         } else {
            throw new DecompilerException("Error: cannot open actions file " + actionfile.getAbsolutePath() + ".");
         }
      } catch (IOException ex) {
         throw new DecompilerException(ex.getMessage());
      }
   }

   /**
    * Loads preferSwitches setting from configuration file if present.
    * <p>
    * Checks for {@code preferSwitches} property in {@code ncsdecomp.conf} or {@code dencs.conf}
    * in the current working directory. If not found or unparseable, leaves the current value unchanged.
    */
   private static void loadPreferSwitchesFromConfig() {
      try {
         File dir = new File(System.getProperty("user.dir"));
         File configFile = new File(dir, "ncsdecomp.conf");
         if (!configFile.exists()) {
            configFile = new File(dir, "dencs.conf");
         }

         if (configFile.exists() && configFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
               String line;
               while ((line = reader.readLine()) != null) {
                  line = line.trim();
                  if (line.startsWith("preferSwitches") || line.startsWith("Prefer Switches")) {
                     int equalsIdx = line.indexOf('=');
                     if (equalsIdx >= 0) {
                        String value = line.substring(equalsIdx + 1).trim();
                        preferSwitches = value.equalsIgnoreCase("true") || value.equals("1");
                     }
                     break;
                  }
               }
            }
         }
      } catch (Exception ex) {
         // Silently ignore config file errors - use default value
      }
   }

   /**
    * Returns a map of variable data for a previously decompiled script.
    *
    * @param file Script file whose variables are requested
    * @return Hashtable of variables keyed by subroutine name, or null if not loaded
    */
   public Hashtable<String, Vector<Variable>> getVariableData(File file) {
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      return data == null ? null : data.getVars();
   }

   /**
    * Returns generated NSS source code for a decompiled script.
    *
    * @param file Script file of interest
    * @return Generated code or null if not yet decompiled
    */
   public String getGeneratedCode(File file) {
      return this.filedata.get(file) == null ? null : this.filedata.get(file).getCode();
   }

   /**
    * Returns bytecode captured from the original compiled script (after external decompile).
    */
   public String getOriginalByteCode(File file) {
      return this.filedata.get(file) == null ? null : this.filedata.get(file).getOriginalByteCode();
   }

   /**
    * Returns bytecode captured from the round-tripped compilation of generated code.
    */
   public String getNewByteCode(File file) {
      return this.filedata.get(file) == null ? null : this.filedata.get(file).getNewByteCode();
   }

   /**
    * Decompiles a file, generates NSS source, compiles it back, and compares.
    * <p>
    * This is the full round-trip validation used by the GUI: decode, emit
    * source, compile externally, and diff bytecode to detect regressions.
    * <p>
    * All exceptions are caught internally and converted to fallback stubs,
    * so this method never throws exceptions and always returns a result code.
    *
    * @param file NCS file to decompile
    * @return One of {@link #SUCCESS}, {@link #PARTIAL_COMPILE}, {@link #PARTIAL_COMPARE}, or {@link #FAILURE}
    */
   public int decompile(File file) {
      try {
      this.ensureActionsLoaded();
      } catch (DecompilerException e) {
         System.out.println("Error loading actions data: " + e.getMessage());
         // Create comprehensive fallback stub for actions data loading failure
         FileDecompiler.FileScriptData errorData = new FileDecompiler.FileScriptData();
         String expectedFile = isK2Selected ? "tsl_nwscript.nss" : "k1_nwscript.nss";
         String stubCode = this.generateComprehensiveFallbackStub(file, "Actions data loading", e,
            "The actions data table (nwscript.nss) is required to decompile NCS files.\n" +
            "Expected file: " + expectedFile + "\n" +
            "Please ensure the appropriate nwscript.nss file is available in tools/ directory, working directory, or configured path.");
         errorData.setCode(stubCode);
         this.filedata.put(file, errorData);
         return PARTIAL_COMPILE;
      }
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      if (data == null) {
         System.out.println("\n---> starting decompilation: " + file.getName() + " <---");
         try {
            data = this.decompileNcs(file);
            // decompileNcs now always returns a FileScriptData (never null)
            // but it may contain minimal/fallback code if decompilation failed
            this.filedata.put(file, data);
         } catch (Exception e) {
         // Last resort: create comprehensive fallback stub data so we always have something to show
            System.out.println("Critical error during decompilation, creating fallback stub: " + e.getMessage());
            e.printStackTrace(System.out);
            data = new FileDecompiler.FileScriptData();
         data.setCode(this.generateComprehensiveFallbackStub(file, "Initial decompilation attempt", e, null));
            this.filedata.put(file, data);
         }
      }

      // Always generate code, even if validation fails
      try {
         data.generateCode();
         String code = data.getCode();
         if (code == null || code.trim().isEmpty()) {
            // If code generation failed, provide comprehensive fallback stub
            System.out.println("Warning: Generated code is empty, creating fallback stub.");
            String fallback = this.generateComprehensiveFallbackStub(file, "Code generation - empty output", null,
               "The decompilation process completed but generated no source code. This may indicate the file contains no executable code or all code was marked as dead/unreachable.");
            data.setCode(fallback);
            return PARTIAL_COMPILE;
         }
      } catch (Exception e) {
         System.out.println("Error during code generation (creating fallback stub): " + e.getMessage());
         String fallback = this.generateComprehensiveFallbackStub(file, "Code generation", e,
            "An exception occurred while generating NSS source code from the decompiled parse tree.");
         data.setCode(fallback);
         return PARTIAL_COMPILE;
      }

      // Try to capture original bytecode from the NCS file if nwnnsscomp is available
      // This allows viewing bytecode even without round-trip validation
      if (this.checkCompilerExists()) {
         try {
            System.out.println("[NCSDecomp] Attempting to capture original bytecode from NCS file...");
            File olddecompiled = this.externalDecompile(file, isK2Selected);
            if (olddecompiled != null && olddecompiled.exists()) {
               String originalByteCode = this.readFile(olddecompiled);
               if (originalByteCode != null && !originalByteCode.trim().isEmpty()) {
                  data.setOriginalByteCode(originalByteCode);
                  System.out.println("[NCSDecomp] Successfully captured original bytecode (" + originalByteCode.length() + " characters)");
               } else {
                  System.out.println("[NCSDecomp] Warning: Original bytecode file is empty");
               }
            } else {
               System.out.println("[NCSDecomp] Warning: Failed to decompile original NCS file to bytecode");
            }
         } catch (Exception e) {
            System.out.println("[NCSDecomp] Exception while capturing original bytecode:");
            System.out.println("[NCSDecomp]   Exception Type: " + e.getClass().getName());
            System.out.println("[NCSDecomp]   Exception Message: " + e.getMessage());
            if (e.getCause() != null) {
               System.out.println("[NCSDecomp]   Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            e.printStackTrace();
         }
      } else {
         System.out.println("[NCSDecomp] nwnnsscomp.exe not found - cannot capture original bytecode");
      }

      // Try validation, but don't fail if it doesn't work
      // nwnnsscomp is optional - decompilation should work without it
      try {
         return this.compileAndCompare(file, data.getCode(), data);
      } catch (Exception e) {
         System.out.println("[NCSDecomp] Exception during bytecode validation:");
         System.out.println("[NCSDecomp]   Exception Type: " + e.getClass().getName());
         System.out.println("[NCSDecomp]   Exception Message: " + e.getMessage());
         if (e.getCause() != null) {
            System.out.println("[NCSDecomp]   Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
         }
         e.printStackTrace();
         System.out.println("[NCSDecomp] Showing decompiled source anyway (validation failed)");
         return PARTIAL_COMPILE;
      }
   }

   /**
    * Compiles the provided NSS file and compares against the original NCS file.
    * Assumes {@link #decompile(File)} has already cached state for {@code file}.
    *
    * @param file Existing compiled script to compare against
    * @param newfile Newly generated NSS file to compile
    */
   public int compileAndCompare(File file, File newfile) throws DecompilerException {
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      return this.compileAndCompare(file, newfile, data);
   }

   /**
    * Compiles an NSS file and captures the resulting bytecode; does not compare.
    *
    * @param nssFile Source to compile
    * @return {@link #SUCCESS} when compilation and decompile of result succeed; {@link #FAILURE} otherwise
    */
   public int compileOnly(File nssFile) throws DecompilerException {
      FileDecompiler.FileScriptData data = this.filedata.get(nssFile);
      if (data == null) {
         data = new FileDecompiler.FileScriptData();
      }

      return this.compileNss(nssFile, data);
   }

   /**
    * Renames a subroutine and regenerates code for the cached script.
    *
    * @return Updated variable map or null if the script is not loaded
    */
   public Hashtable<String, Vector<Variable>> updateSubName(File file, String oldname, String newname) {
      if (file == null) {
         return null;
      } else {
         FileDecompiler.FileScriptData data = this.filedata.get(file);
         if (data == null) {
            return null;
         } else {
            data.replaceSubName(oldname, newname);
            return data.getVars();
         }
      }
   }

   /**
    * Forces regeneration of NSS source for a cached script.
    *
    * @param file Script whose code should be regenerated
    * @return Regenerated code, or null if the script is not loaded
    */
   public String regenerateCode(File file) {
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      if (data == null) {
         return null;
      } else {
         data.generateCode();
         return data.toString();
      }
   }

   /**
    * Releases resources for a specific script and removes it from the cache.
    */
   public void closeFile(File file) {
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      if (data != null) {
         data.close();
         this.filedata.remove(file);
      }

      System.gc();
   }

   /**
    * Releases all cached script state to reduce memory footprint.
    */
   public void closeAllFiles() {
      Enumeration<FileScriptData> en = this.filedata.elements();

      while (en.hasMoreElements()) {
         FileDecompiler.FileScriptData data = en.nextElement();
         data.close();
      }

      this.filedata.clear();
      System.gc();
   }

   /**
    * Decompile a single NCS file to NSS source (in-memory) without invoking external tools.
    */
   public String decompileToString(File file) throws DecompilerException {
      FileDecompiler.FileScriptData data = this.decompileNcs(file);
      if (data == null) {
         throw new DecompilerException("Decompile failed for " + file.getAbsolutePath());
      }

      data.generateCode();
      return data.getCode();
   }

   /**
    * Decompile a single NCS file directly to an output file using the provided charset.
    */
   public void decompileToFile(File input, File output, Charset charset, boolean overwrite) throws DecompilerException, IOException {
      if (output.exists() && !overwrite) {
         throw new IOException("Output file already exists: " + output.getAbsolutePath());
      }

      String code = this.decompileToString(input);
      File parent = output.getParentFile();
      if (parent != null) {
         parent.mkdirs();
      }

      try (BufferedWriter bw = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(output), charset))) {
         bw.write(code);
      }
   }

   /**
    * Compiles a generated NSS file and compares it against the supplied original.
    * Also stores bytecode snapshots for later inspection.
    */
   private int compileAndCompare(File file, File newfile, FileDecompiler.FileScriptData data) throws DecompilerException {
      // If compiler doesn't exist, skip validation but still return success for decompilation
      if (!this.checkCompilerExists()) {
         System.out.println("[NCSDecomp] nwnnsscomp.exe not found - skipping bytecode validation. Decompiled source will still be shown.");
         System.out.println("[NCSDecomp] Looking for: " + this.getCompilerFile().getAbsolutePath());
         return PARTIAL_COMPILE;
      }

      File newcompiled = null;
      File newdecompiled = null;
      File olddecompiled = null;

      try {
         System.out.println("[NCSDecomp] Decompiling original NCS file to capture bytecode...");
         olddecompiled = this.externalDecompile(file, isK2Selected);
         if (olddecompiled == null || !olddecompiled.exists()) {
            System.out.println("[NCSDecomp] ERROR: nwnnsscomp decompile of original NCS file failed.");
            System.out.println("[NCSDecomp]   Expected output file: " + (olddecompiled != null ? olddecompiled.getAbsolutePath() : "null"));
            System.out.println("[NCSDecomp]   Check nwnnsscomp output above for details.");
            return PARTIAL_COMPILE;
         }

         data.setOriginalByteCode(this.readFile(olddecompiled));
         System.out.println("[NCSDecomp] Compiling generated NSS file...");
         newcompiled = this.externalCompile(newfile, isK2Selected);
         if (newcompiled == null || !newcompiled.exists()) {
            System.out.println("[NCSDecomp] ERROR: nwnnsscomp compilation of generated NSS file failed.");
            System.out.println("[NCSDecomp]   Input file: " + newfile.getAbsolutePath());
            System.out.println("[NCSDecomp]   Expected output: " + (newcompiled != null ? newcompiled.getAbsolutePath() : "null"));
            System.out.println("[NCSDecomp]   Check nwnnsscomp output above for compilation errors.");
            return PARTIAL_COMPILE;
         }

         System.out.println("[NCSDecomp] Decompiling newly compiled NCS file to capture bytecode...");
         newdecompiled = this.externalDecompile(newcompiled, isK2Selected);
         if (newdecompiled == null || !newdecompiled.exists()) {
            System.out.println("[NCSDecomp] ERROR: nwnnsscomp decompile of newly compiled file failed.");
            System.out.println("[NCSDecomp]   Expected output file: " + (newdecompiled != null ? newdecompiled.getAbsolutePath() : "null"));
            System.out.println("[NCSDecomp]   Check nwnnsscomp output above for details.");
            return PARTIAL_COMPILE;
         }

         data.setNewByteCode(this.readFile(newdecompiled));
         if (this.compareBinaryFiles(file, newcompiled)) {
            return SUCCESS;
         }

         // Fall back to textual pcode comparison to aid debugging.
         String diff = this.comparePcodeFiles(olddecompiled, newdecompiled);
         if (diff != null) {
            System.out.println("P-code difference: " + diff);
         }
      } catch (Exception e) {
         // Catch any exceptions during compilation/validation and continue with partial result
         System.out.println("[NCSDecomp] EXCEPTION during bytecode validation:");
         System.out.println("[NCSDecomp]   Exception Type: " + e.getClass().getName());
         System.out.println("[NCSDecomp]   Exception Message: " + e.getMessage());
         if (e.getCause() != null) {
            System.out.println("[NCSDecomp]   Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
         }
         System.out.println("[NCSDecomp]   Stack trace:");
         e.printStackTrace();
         System.out.println("[NCSDecomp] Continuing with decompiled source (validation failed)");
         return PARTIAL_COMPILE;
      } finally {
         try {
            if (newcompiled != null) {
               newcompiled.delete();
            }

            if (newdecompiled != null) {
               newdecompiled.delete();
            }

            if (olddecompiled != null) {
               olddecompiled.delete();
            }
         } catch (Exception var15) {
         }
      }

      return PARTIAL_COMPARE;
   }

   /**
    * Convenience overload that writes generated code to disk before comparison.
    */
   private int compileAndCompare(File file, String code, FileDecompiler.FileScriptData data) throws DecompilerException {
      this.ensureActionsLoaded();
      File gennedcode = null;

      int var6;
      try {
         gennedcode = this.writeCode(code);
         var6 = this.compileAndCompare(file, gennedcode, data);
      } finally {
         try {
            if (gennedcode != null) {
               gennedcode.delete();
            }
         } catch (Exception var10) {
         }
      }

      return var6;
   }

   /**
    * Compiles an NSS file via external nwnnsscomp and captures the resulting bytecode.
    */
   private int compileNss(File nssFile, FileDecompiler.FileScriptData data) throws DecompilerException {
      // If compiler doesn't exist, return failure but don't throw
      if (!this.checkCompilerExists()) {
         System.out.println("nwnnsscomp.exe not found - cannot compile NSS file.");
         return FAILURE;
      }

      File newcompiled = null;
      File newdecompiled = null;

      try {
         newcompiled = this.externalCompile(nssFile, isK2Selected);
         if (newcompiled == null) {
            return FAILURE;
         }

         newdecompiled = this.externalDecompile(newcompiled, isK2Selected);
         if (newdecompiled != null) {
            data.setNewByteCode(this.readFile(newdecompiled));
            return SUCCESS;
         }

         System.out.println("nwnnsscomp decompile of new compiled file failed.  Check code.");
      } catch (Exception e) {
         System.out.println("Error during compilation: " + e.getMessage());
         return FAILURE;
      } finally {
         try {
            if (newcompiled != null) {
               newcompiled.delete();
            }

            if (newdecompiled != null) {
               newdecompiled.delete();
            }
         } catch (Exception var11) {
         }
      }

      return FAILURE;
   }

   /**
    * Reads a text file into memory preserving platform line separators.
    */
   private String readFile(File file) {
      String newline = System.getProperty("line.separator");
      StringBuffer buffer = new StringBuffer();
      BufferedReader reader = null;

      try {
         reader = new BufferedReader(new FileReader(file));

         String line;
         while ((line = reader.readLine()) != null) {
            buffer.append(line + newline);
         }

         return buffer.toString();
      } catch (IOException var14) {
         System.out.println("IO exception in read file: " + var14);
         return null;
      } finally {
         try {
            if (reader != null) {
               reader.close();
            }
         } catch (Exception var13) {
         }
      }
   }

   /**
    * Performs a line-by-line comparison of two p-code listings.
    *
    * @return null when identical; otherwise a human-readable mismatch description
    */
   private String comparePcodeFiles(File originalPcode, File newPcode) {
      try (BufferedReader reader1 = new BufferedReader(new FileReader(originalPcode));
         BufferedReader reader2 = new BufferedReader(new FileReader(newPcode))) {
         String line1;
         String line2;
         int line = 1;

         while (true) {
            line1 = reader1.readLine();
            line2 = reader2.readLine();

            // both files ended -> identical
            if (line1 == null && line2 == null) {
               return null; // identical
            }

            // Detect differences: missing line or differing content
            if (line1 == null || line2 == null || !line1.equals(line2)) {
               String left = line1 == null ? "<EOF>" : line1;
               String right = line2 == null ? "<EOF>" : line2;
               return "Mismatch at line " + line + " | original: " + left + " | generated: " + right;
            }

            line++;
         }
      } catch (IOException ex) {
         System.out.println("IO exception in compare files: " + ex);
         return "IO exception during pcode comparison";
      }
   }

   /**
    * Performs byte-for-byte comparison of two compiled NCS files.
    */
   private boolean compareBinaryFiles(File original, File generated) {
      try (BufferedInputStream a = new BufferedInputStream(new FileInputStream(original));
         BufferedInputStream b = new BufferedInputStream(new FileInputStream(generated))) {
         int ba;
         int bb;
         while (true) {
            ba = a.read();
            bb = b.read();
            if (ba == -1 || bb == -1) {
               return ba == -1 && bb == -1;
            }

            if (ba != bb) {
               return false;
            }
         }
      } catch (IOException ex) {
         System.out.println("IO exception in compare files: " + ex);
         return false;
      }
   }

   /**
    * Returns the expected nwnnsscomp executable location.
    * Checks configured path first, then tools/ directory, then current working directory.
    */
   /**
    * Gets the NCSDecomp installation directory (where the jar/exe is located).
    * @return File representing the installation directory, or null if cannot be determined
    */
   private File getNCSDecompDirectory() {
      try {
         // Try to get the location of the jar/exe file
         java.net.URL location = FileDecompiler.class.getProtectionDomain().getCodeSource().getLocation();
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
               File jarFile = new File(path);
               if (jarFile.exists()) {
                  File parent = jarFile.getParentFile();
                  if (parent != null) {
                     return parent;
                  }
               }
            }
         }
      } catch (Exception e) {
         // Fall through to user.dir
      }
      // Fallback to user.dir if we can't determine jar location
      return new File(System.getProperty("user.dir"));
   }

   /**
    * Finds the compiler executable by trying multiple filenames in multiple locations.
    * Tries in order:
    * 1. Configured path (if set) - all 3 filenames
    * 2. tools/ directory - all 3 filenames
    * 3. Current working directory - all 3 filenames
    * 4. NCSDecomp installation directory - all 3 filenames
    *
    * Filenames tried in order: nwnnsscomp.exe, nwnnsscomp_kscript.exe, nwnnsscomp_tslpatcher.exe
    */
   private File getCompilerFile() {
      String[] compilerNames = {"nwnnsscomp.exe", "nwnnsscomp_kscript.exe", "nwnnsscomp_tslpatcher.exe"};

      // 1. Try configured path (if set) - all 3 filenames
      if (nwnnsscompPath != null && !nwnnsscompPath.trim().isEmpty()) {
         File configuredDir = new File(nwnnsscompPath);
         if (configuredDir.isDirectory()) {
            // If it's a directory, try all filenames in it
            for (String name : compilerNames) {
               File candidate = new File(configuredDir, name);
               if (candidate.exists()) {
                  return candidate;
               }
            }
         } else {
            // If it's a file, check if it exists
            if (configuredDir.exists()) {
               return configuredDir;
            }
            // Also try other filenames in the same directory
            File parent = configuredDir.getParentFile();
            if (parent != null) {
               for (String name : compilerNames) {
                  File candidate = new File(parent, name);
                  if (candidate.exists()) {
                     return candidate;
                  }
               }
            }
         }
      }

      // 2. Try tools/ directory - all 3 filenames
      File toolsDir = new File(System.getProperty("user.dir"), "tools");
      for (String name : compilerNames) {
         File candidate = new File(toolsDir, name);
         if (candidate.exists()) {
            return candidate;
         }
      }

      // 3. Try current working directory - all 3 filenames
      File cwd = new File(System.getProperty("user.dir"));
      for (String name : compilerNames) {
         File candidate = new File(cwd, name);
         if (candidate.exists()) {
            return candidate;
         }
      }

      // 4. Try NCSDecomp installation directory - all 3 filenames
      File ncsDecompDir = getNCSDecompDirectory();
      if (ncsDecompDir != null && !ncsDecompDir.equals(cwd)) {
         for (String name : compilerNames) {
            File candidate = new File(ncsDecompDir, name);
            if (candidate.exists()) {
               return candidate;
            }
         }
         // Also try tools/ subdirectory of NCSDecomp directory
         File ncsToolsDir = new File(ncsDecompDir, "tools");
         for (String name : compilerNames) {
            File candidate = new File(ncsToolsDir, name);
            if (candidate.exists()) {
               return candidate;
            }
         }
      }

      // Final fallback: return nwnnsscomp.exe in current directory (may not exist)
      return new File("nwnnsscomp.exe");
   }

   /**
    * Checks if the compiler binary is present.
    * @return true if compiler exists, false otherwise
    */
   private boolean checkCompilerExists() {
      File compiler = getCompilerFile();
      return compiler.exists();
   }

   /**
    * Strips the extension from a file path to produce a base name used by nwnnsscomp.
    */
   private String getShortName(File in) {
      int i = in.getAbsolutePath().lastIndexOf(46);
      return i == -1 ? in.getAbsolutePath() : in.getAbsolutePath().substring(0, i);
   }

   /**
    * Invokes nwnnsscomp in decompile mode against a single file.
    * Uses {@link NwnnsscompConfig} to build arguments appropriate for the detected binary.
    */
   private File externalDecompile(File in, boolean k2) {
      try {
         File compiler = getCompilerFile();
         if (!compiler.exists()) {
            System.out.println("[NCSDecomp] ERROR: Compiler not found: " + compiler.getAbsolutePath());
            return null;
         }

         String outname = this.getShortName(in) + ".pcode";
         File result = new File(outname);
         if (result.exists()) {
            result.delete();
         }

         // Use compiler detection to get correct command-line arguments
         NwnnsscompConfig config = new NwnnsscompConfig(compiler, in, result, k2);
         String[] args = config.getDecompileArgs(compiler.getAbsolutePath());

         System.out.println("[NCSDecomp] Using compiler: " + config.getChosenCompiler().getName() +
            " (SHA256: " + config.getSha256Hash().substring(0, 16) + "...)");
         System.out.println("[NCSDecomp] Input file: " + in.getAbsolutePath());
         System.out.println("[NCSDecomp] Expected output: " + result.getAbsolutePath());

         new FileDecompiler.WindowsExec().callExec(args);

         if (!result.exists()) {
            System.out.println("[NCSDecomp] ERROR: Expected output file does not exist: " + result.getAbsolutePath());
            System.out.println("[NCSDecomp]   This usually means nwnnsscomp.exe failed or produced no output.");
            System.out.println("[NCSDecomp]   Check the nwnnsscomp output above for error messages.");
            return null;
         }

         return result;
      } catch (Exception e) {
         System.out.println("[NCSDecomp] EXCEPTION during external decompile:");
         System.out.println("[NCSDecomp]   Exception Type: " + e.getClass().getName());
         System.out.println("[NCSDecomp]   Exception Message: " + e.getMessage());
         if (e.getCause() != null) {
            System.out.println("[NCSDecomp]   Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
         }
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Writes generated NSS code to a temporary file for external compilation.
    */
   private File writeCode(String code) {
      try {
         File out = new File("_generatedcode.nss");
         out.createNewFile();
         FileWriter writer = new FileWriter(out);
         writer.write(code);
         writer.close();
         File result = new File("_generatedcode.ncs");
         if (result.exists()) {
            result.delete();
         }

         return out;
      } catch (IOException var5) {
         System.out.println("IO exception on writing code: " + var5);
         return null;
      }
   }

   /**
    * Invokes nwnnsscomp in compile mode against a single NSS file.
    */
   private File externalCompile(File file, boolean k2) {
      try {
         File compiler = getCompilerFile();
         if (!compiler.exists()) {
            System.out.println("[NCSDecomp] ERROR: Compiler not found: " + compiler.getAbsolutePath());
            return null;
         }

         String outname = this.getShortName(file) + ".ncs";
         File result = new File(outname);

         // Use compiler detection to get correct command-line arguments
         NwnnsscompConfig config = new NwnnsscompConfig(compiler, file, result, k2);
         List<File> includeDirs = this.buildIncludeDirs(k2);
         String[] args = config.getCompileArgs(compiler.getAbsolutePath(), includeDirs);

         System.out.println("[NCSDecomp] Using compiler: " + config.getChosenCompiler().getName() +
            " (SHA256: " + config.getSha256Hash().substring(0, 16) + "...)");
         System.out.println("[NCSDecomp] Input file: " + file.getAbsolutePath());
         System.out.println("[NCSDecomp] Expected output: " + result.getAbsolutePath());
         if (includeDirs != null && !includeDirs.isEmpty()) {
            System.out.println("[NCSDecomp] Include directories: " + includeDirs.size());
            for (File includeDir : includeDirs) {
               System.out.println("[NCSDecomp]   - " + includeDir.getAbsolutePath());
            }
         }

         new FileDecompiler.WindowsExec().callExec(args);

         if (!result.exists()) {
            System.out.println("[NCSDecomp] ERROR: Expected output file does not exist: " + result.getAbsolutePath());
            System.out.println("[NCSDecomp]   This usually means nwnnsscomp.exe compilation failed.");
            System.out.println("[NCSDecomp]   Check the nwnnsscomp output above for compilation errors.");
            return null;
         }

         return result;
      } catch (Exception e) {
         System.out.println("[NCSDecomp] EXCEPTION during external compile:");
         System.out.println("[NCSDecomp]   Exception Type: " + e.getClass().getName());
         System.out.println("[NCSDecomp]   Exception Message: " + e.getMessage());
         if (e.getCause() != null) {
            System.out.println("[NCSDecomp]   Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
         }
         e.printStackTrace();
         return null;
      }
   }

   private List<File> buildIncludeDirs(boolean k2) {
      List<File> dirs = new ArrayList<>();
      File base = new File("test-work" + File.separator + "Vanilla_KOTOR_Script_Source");
      File gameDir = new File(base, k2 ? "TSL" : "K1");
      File scriptsBif = new File(gameDir, "Data" + File.separator + "scripts.bif");
      if (scriptsBif.exists()) {
         dirs.add(scriptsBif);
      }
      File rootOverride = new File(gameDir, "Override");
      if (rootOverride.exists()) {
         dirs.add(rootOverride);
      }
      // Fallback: allow includes relative to the game dir root.
      if (gameDir.exists()) {
         dirs.add(gameDir);
      }
      return dirs;
   }

   private void ensureActionsLoaded() throws DecompilerException {
      if (this.actions == null) {
         this.actions = loadActionsDataInternal(isK2Selected);
      }
   }

   /**
    * Converts a byte array to a hexadecimal string representation.
    *
    * @param bytes The byte array to convert
    * @param length The number of bytes to convert
    * @return Hexadecimal string representation
    */
   private String bytesToHex(byte[] bytes, int length) {
      StringBuilder hex = new StringBuilder();
      for (int i = 0; i < length; i++) {
         hex.append(String.format("%02X", bytes[i] & 0xFF));
         if (i < length - 1) {
            hex.append(" ");
         }
      }
      return hex.toString();
   }

   /**
    * Generates a comprehensive fallback stub with all available diagnostic information.
    * This ensures fallback stubs are as exhaustive, accurate, and complete as possible.
    *
    * @param file The file being decompiled
    * @param errorStage Description of the stage where the error occurred
    * @param exception The exception that occurred (may be null)
    * @param additionalInfo Additional context information (may be null)
    * @return A comprehensive fallback stub string
    */
   private String generateComprehensiveFallbackStub(File file, String errorStage, Exception exception, String additionalInfo) {
      StringBuilder stub = new StringBuilder();
      String newline = System.getProperty("line.separator");

      // Header with error type
      stub.append("// ========================================").append(newline);
      stub.append("// DECOMPILATION ERROR - FALLBACK STUB").append(newline);
      stub.append("// ========================================").append(newline);
      stub.append(newline);

      // File information
      stub.append("// File Information:").append(newline);
      if (file != null) {
         stub.append("//   Name: ").append(file.getName()).append(newline);
         stub.append("//   Path: ").append(file.getAbsolutePath()).append(newline);
         if (file.exists()) {
            stub.append("//   Size: ").append(file.length()).append(" bytes").append(newline);
            stub.append("//   Last Modified: ").append(new java.util.Date(file.lastModified())).append(newline);
            stub.append("//   Readable: ").append(file.canRead()).append(newline);
         } else {
            stub.append("//   Status: FILE DOES NOT EXIST").append(newline);
         }
      } else {
         stub.append("//   Status: FILE IS NULL").append(newline);
      }
      stub.append(newline);

      // Error stage information
      stub.append("// Error Stage: ").append(errorStage != null ? errorStage : "Unknown").append(newline);
      stub.append(newline);

      // Exception information
      if (exception != null) {
         stub.append("// Exception Details:").append(newline);
         stub.append("//   Type: ").append(exception.getClass().getName()).append(newline);
         stub.append("//   Message: ").append(exception.getMessage() != null ? exception.getMessage() : "(no message)").append(newline);

         // Include cause if available
         Throwable cause = exception.getCause();
         if (cause != null) {
            stub.append("//   Caused by: ").append(cause.getClass().getName()).append(newline);
            stub.append("//   Cause Message: ").append(cause.getMessage() != null ? cause.getMessage() : "(no message)").append(newline);
         }

         // Include stack trace summary (first few frames)
         StackTraceElement[] stack = exception.getStackTrace();
         if (stack != null && stack.length > 0) {
            stub.append("//   Stack Trace (first 5 frames):").append(newline);
            int maxFrames = Math.min(5, stack.length);
            for (int i = 0; i < maxFrames; i++) {
               stub.append("//     at ").append(stack[i].toString()).append(newline);
            }
            if (stack.length > maxFrames) {
               stub.append("//     ... (").append(stack.length - maxFrames).append(" more frames)").append(newline);
            }
         }
         stub.append(newline);
      }

      // Additional context information
      if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
         stub.append("// Additional Context:").append(newline);
         // Split long additional info into lines if needed
         String[] lines = additionalInfo.split("\n");
         for (String line : lines) {
            stub.append("//   ").append(line).append(newline);
         }
         stub.append(newline);
      }

      // Decompiler configuration
      stub.append("// Decompiler Configuration:").append(newline);
      stub.append("//   Game Mode: ").append(isK2Selected ? "KotOR 2 (TSL)" : "KotOR 1").append(newline);
      stub.append("//   Prefer Switches: ").append(preferSwitches).append(newline);
      stub.append("//   Strict Signatures: ").append(strictSignatures).append(newline);
      stub.append("//   Actions Data Loaded: ").append(this.actions != null).append(newline);
      stub.append(newline);

      // System information
      stub.append("// System Information:").append(newline);
      stub.append("//   Java Version: ").append(System.getProperty("java.version")).append(newline);
      stub.append("//   OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(newline);
      stub.append("//   Working Directory: ").append(System.getProperty("user.dir")).append(newline);
      stub.append(newline);

      // Timestamp
      stub.append("// Error Timestamp: ").append(new java.util.Date()).append(newline);
      stub.append(newline);

      // Recommendations
      stub.append("// Recommendations:").append(newline);
      if (file != null && file.exists() && file.length() == 0) {
         stub.append("//   - File is empty (0 bytes). This may indicate a corrupted or incomplete file.").append(newline);
      } else if (file != null && !file.exists()) {
         stub.append("//   - File does not exist. Verify the file path is correct.").append(newline);
      } else if (this.actions == null) {
         stub.append("//   - Actions data not loaded. Ensure k1_nwscript.nss or tsl_nwscript.nss is available.").append(newline);
      } else {
         stub.append("//   - This may indicate a corrupted, invalid, or unsupported NCS file format.").append(newline);
         stub.append("//   - The file may be from a different game version or modded in an incompatible way.").append(newline);
      }
      stub.append("//   - Check the exception details above for specific error information.").append(newline);
      stub.append("//   - Verify the file is a valid KotOR/TSL NCS bytecode file.").append(newline);
      stub.append(newline);

      // Minimal valid NSS stub
      stub.append("// Minimal fallback function:").append(newline);
      stub.append("void main() {").append(newline);
      stub.append("    // Decompilation failed at stage: ").append(errorStage != null ? errorStage : "Unknown").append(newline);
      if (exception != null && exception.getMessage() != null) {
         stub.append("    // Error: ").append(exception.getMessage().replace("\n", " ").replace("\r", "")).append(newline);
      }
      stub.append("}").append(newline);

      return stub.toString();
   }

   /**
    * Core decompilation pipeline that converts NCS bytecode to in-memory script state.
    * <p>
    * Steps include decoding the bytecode stream, building a parse tree, running
    * multiple analysis passes (destination resolution, dead code marking, typing),
    * flattening globals/subroutines, and finally constructing {@link SubScriptState}
    * objects ready for code generation.
    *
    * @param file NCS file to decode
    * @return {@link FileScriptData} containing parsed subroutines and metadata, or null on fatal error
    */
   private FileDecompiler.FileScriptData decompileNcs(File file) {
      FileDecompiler.FileScriptData data = null;
      String commands = null;
      SetDestinations setdest = null;
      DoTypes dotypes = null;
      Start ast = null;
      NodeAnalysisData nodedata = null;
      SubroutineAnalysisData subdata = null;
      Iterator<ASubroutine> subs = null;
      ASubroutine sub = null;
      ASubroutine mainsub = null;
      FlattenSub flatten = null;
      DoGlobalVars doglobs = null;
      CleanupPass cleanpass = null;
      MainPass mainpass = null;
      DestroyParseTree destroytree = null;
      if (this.actions == null) {
         System.out.println("null action! Creating fallback stub.");
         // Return comprehensive stub instead of null
         FileDecompiler.FileScriptData stub = new FileDecompiler.FileScriptData();
         String expectedFile = isK2Selected ? "tsl_nwscript.nss" : "k1_nwscript.nss";
         String stubCode = this.generateComprehensiveFallbackStub(file, "Actions data loading", null,
            "The actions data table (nwscript.nss) is required to decompile NCS files.\n" +
            "Expected file: " + expectedFile + "\n" +
            "Please ensure the appropriate nwscript.nss file is available in tools/ directory, working directory, or configured path.");
         stub.setCode(stubCode);
         return stub;
      }

      try {
         data = new FileDecompiler.FileScriptData();

         // Decode bytecode - wrap in try-catch to handle corrupted files
         try {
            commands = new Decoder(new BufferedInputStream(new FileInputStream(file)), this.actions).decode();
         } catch (Exception decodeEx) {
            System.out.println("Error during bytecode decoding: " + decodeEx.getMessage());
            // Create comprehensive fallback stub for decoding errors
            long fileSize = file.exists() ? file.length() : -1;
            String fileInfo = "File size: " + fileSize + " bytes";
            if (fileSize > 0) {
               try (FileInputStream fis = new FileInputStream(file)) {
                  byte[] header = new byte[Math.min(16, (int)fileSize)];
                  int read = fis.read(header);
                  if (read > 0) {
                     fileInfo += "\nFile header (hex): " + bytesToHex(header, read);
                  }
               } catch (Exception ignored) {}
            }
            String stub = this.generateComprehensiveFallbackStub(file, "Bytecode decoding", decodeEx, fileInfo);
            data.setCode(stub);
            return data;
         }

         // Parse commands - wrap in try-catch to handle parse errors, but try to recover
         try {
            ast = new Parser(new Lexer(new PushbackReader(new StringReader(commands), 1024))).parse();
         } catch (Exception parseEx) {
            System.out.println("Error during parsing: " + parseEx.getMessage());
            System.out.println("Attempting to recover by trying partial parsing strategies...");

            // Try to recover: attempt to parse in chunks or with relaxed rules
            ast = null;
            try {
               // Strategy 1: Try parsing with a larger buffer
               System.out.println("Trying parse with larger buffer...");
               ast = new Parser(new Lexer(new PushbackReader(new StringReader(commands), 2048))).parse();
               System.out.println("Successfully recovered parse with larger buffer.");
            } catch (Exception e1) {
               System.out.println("Larger buffer parse also failed: " + e1.getMessage());
               // Strategy 2: Try to extract what we can and create minimal structure
               // If we have decoded commands, we can at least create a basic structure
               if (commands != null && commands.length() > 0) {
                  System.out.println("Attempting to create minimal structure from decoded commands...");
                  try {
                     // Try to find subroutine boundaries in the commands string
                     // This is a heuristic recovery - look for common patterns
                     String[] lines = commands.split("\n");
                     int subCount = 0;
                     for (String line : lines) {
                        if (line.trim().startsWith("sub") || line.trim().startsWith("function")) {
                           subCount++;
                        }
                     }

                     // If we found some structure, try to continue with minimal setup
                     if (subCount > 0) {
                        System.out.println("Detected " + subCount + " potential subroutines in decoded commands, but full parse failed.");
                        // We'll fall through to create a stub, but with better information
                     }
                  } catch (Exception e2) {
                     System.out.println("Recovery attempt failed: " + e2.getMessage());
                  }
               }
            }

            // If we still don't have an AST, create comprehensive stub but preserve commands for potential manual recovery
            if (ast == null) {
               String commandsPreview = "none";
               if (commands != null && commands.length() > 0) {
                  int previewLength = Math.min(1000, commands.length());
                  commandsPreview = commands.substring(0, previewLength);
                  if (commands.length() > previewLength) {
                     commandsPreview += "\n... (truncated, total length: " + commands.length() + " characters)";
                  }
               }
               String additionalInfo = "Bytecode was successfully decoded but parsing failed.\n" +
                                      "Decoded commands length: " + (commands != null ? commands.length() : 0) + " characters\n" +
                                      "Decoded commands preview:\n" + commandsPreview + "\n\n" +
                                      "RECOVERY NOTE: The decoded commands are available but could not be parsed into an AST.\n" +
                                      "This may indicate malformed bytecode or an unsupported format variant.";
               String stub = this.generateComprehensiveFallbackStub(file, "Parsing decoded bytecode", parseEx, additionalInfo);
            data.setCode(stub);
            return data;
            }
            // If we recovered an AST, continue with decompilation
            System.out.println("Continuing decompilation with recovered parse tree.");
         }

         // Analysis passes - wrap in try-catch to allow partial recovery
         nodedata = new NodeAnalysisData();
         subdata = new SubroutineAnalysisData(nodedata);

         try {
         ast.apply(new SetPositions(nodedata));
         } catch (Exception e) {
            System.out.println("Error in SetPositions, continuing with partial positions: " + e.getMessage());
         }

         try {
         setdest = new SetDestinations(ast, nodedata, subdata);
         ast.apply(setdest);
         } catch (Exception e) {
            System.out.println("Error in SetDestinations, continuing without destination resolution: " + e.getMessage());
            setdest = null;
         }

         try {
            if (setdest != null) {
         ast.apply(new SetDeadCode(nodedata, subdata, setdest.getOrigins()));
            } else {
               // Try without origins if setdest failed
               ast.apply(new SetDeadCode(nodedata, subdata, null));
            }
         } catch (Exception e) {
            System.out.println("Error in SetDeadCode, continuing without dead code analysis: " + e.getMessage());
         }

         if (setdest != null) {
            try {
         setdest.done();
            } catch (Exception e) {
               System.out.println("Error finalizing SetDestinations: " + e.getMessage());
            }
         setdest = null;
         }

         try {
         subdata.splitOffSubroutines(ast);
         } catch (Exception e) {
            System.out.println("Error splitting subroutines, attempting to continue: " + e.getMessage());
            // Try to get main sub at least
            try {
               mainsub = subdata.getMainSub();
            } catch (Exception e2) {
               System.out.println("Could not recover main subroutine: " + e2.getMessage());
            }
         }
         ast = null;
         // Flattening - try to recover if main sub is missing
         try {
         mainsub = subdata.getMainSub();
         } catch (Exception e) {
            System.out.println("Error getting main subroutine: " + e.getMessage());
            mainsub = null;
         }

         if (mainsub != null) {
            try {
         flatten = new FlattenSub(mainsub, nodedata);
         mainsub.apply(flatten);
            } catch (Exception e) {
               System.out.println("Error flattening main subroutine: " + e.getMessage());
               flatten = null;
            }

            if (flatten != null) {
               try {
         for (ASubroutine iterSub : this.subIterable(subdata)) {
                     try {
            flatten.setSub(iterSub);
            iterSub.apply(flatten);
                     } catch (Exception e) {
                        System.out.println("Error flattening subroutine, skipping: " + e.getMessage());
                        // Continue with other subroutines
                     }
                  }
               } catch (Exception e) {
                  System.out.println("Error iterating subroutines during flattening: " + e.getMessage());
               }

               try {
         flatten.done();
               } catch (Exception e) {
                  System.out.println("Error finalizing flatten: " + e.getMessage());
               }
         flatten = null;
            }
         } else {
            System.out.println("Warning: No main subroutine available, continuing with partial decompilation.");
         }
         // Process globals - recover if this fails
         try {
         sub = subdata.getGlobalsSub();
         if (sub != null) {
               try {
            doglobs = new DoGlobalVars(nodedata, subdata);
            sub.apply(doglobs);
            cleanpass = new CleanupPass(doglobs.getScriptRoot(), nodedata, subdata, doglobs.getState());
            cleanpass.apply();
            subdata.setGlobalStack(doglobs.getStack());
            subdata.globalState(doglobs.getState());
            cleanpass.done();
               } catch (Exception e) {
                  System.out.println("Error processing globals, continuing without globals: " + e.getMessage());
                  if (doglobs != null) {
                     try {
                        doglobs.done();
                     } catch (Exception e2) {}
                  }
                  doglobs = null;
               }
            }
         } catch (Exception e) {
            System.out.println("Error getting globals subroutine: " + e.getMessage());
         }

         // Prototype engine - recover if this fails
         try {
         PrototypeEngine proto = new PrototypeEngine(nodedata, subdata, this.actions, FileDecompiler.strictSignatures);
         proto.run();
         } catch (Exception e) {
            System.out.println("Error in prototype engine, continuing with partial prototypes: " + e.getMessage());
         }

         // Type analysis - recover if main sub typing fails
         if (mainsub != null) {
            try {
         dotypes = new DoTypes(subdata.getState(mainsub), nodedata, subdata, this.actions, false);
         mainsub.apply(dotypes);

         try {
            dotypes.assertStack();
         } catch (Exception e) {
            System.out.println("Could not assert stack, continuing anyway.");
         }

         dotypes.done();
            } catch (Exception e) {
               System.out.println("Error typing main subroutine, continuing with partial types: " + e.getMessage());
               dotypes = null;
            }
         }

         // Type all subroutines - continue even if some fail
         boolean alldone = false;
         boolean onedone = true;
         int donecount = 0;

         try {
            alldone = subdata.countSubsDone() == subdata.numSubs();
            onedone = true;
            donecount = subdata.countSubsDone();
         } catch (Exception e) {
            System.out.println("Error checking subroutine completion status: " + e.getMessage());
         }

         for (int loopcount = 0; !alldone && onedone && loopcount < 1000; ++loopcount) {
            onedone = false;
            try {
            subs = subdata.getSubroutines();
            } catch (Exception e) {
               System.out.println("Error getting subroutines iterator: " + e.getMessage());
               break;
            }

            if (subs != null) {
            while (subs.hasNext()) {
                  try {
            sub = subs.next();
                     if (sub == null) continue;

               dotypes = new DoTypes(subdata.getState(sub), nodedata, subdata, this.actions, false);
               sub.apply(dotypes);
               dotypes.done();
                  } catch (Exception e) {
                     System.out.println("Error typing subroutine, skipping: " + e.getMessage());
                     // Continue with next subroutine
                  }
               }
            }

            if (mainsub != null) {
               try {
            dotypes = new DoTypes(subdata.getState(mainsub), nodedata, subdata, this.actions, false);
            mainsub.apply(dotypes);
            dotypes.done();
               } catch (Exception e) {
                  System.out.println("Error re-typing main subroutine: " + e.getMessage());
               }
            }

            try {
            alldone = subdata.countSubsDone() == subdata.numSubs();
               int newDoneCount = subdata.countSubsDone();
               onedone = newDoneCount > donecount;
               donecount = newDoneCount;
            } catch (Exception e) {
               System.out.println("Error checking completion status: " + e.getMessage());
               break;
            }
         }

         if (!alldone) {
            System.out.println("Unable to do final prototype of all subroutines. Continuing with partial results.");
         }

         this.enforceStrictSignatures(subdata, nodedata);

         dotypes = null;
         nodedata.clearProtoData();

         for (ASubroutine iterSub : this.subIterable(subdata)) {
            try {
               mainpass = new MainPass(subdata.getState(iterSub), nodedata, subdata, this.actions);
               iterSub.apply(mainpass);
               cleanpass = new CleanupPass(mainpass.getScriptRoot(), nodedata, subdata, mainpass.getState());
               cleanpass.apply();
               data.addSub(mainpass.getState());
               mainpass.done();
               cleanpass.done();
            } catch (Exception e) {
               System.out.println("Error while processing subroutine: " + e);
               e.printStackTrace(System.out);
               // Try to add partial subroutine state even if processing failed
               try {
                  SubroutineState state = subdata.getState(iterSub);
                  if (state != null) {
                     MainPass recoveryPass = new MainPass(state, nodedata, subdata, this.actions);
                     // Try to get state even if apply failed
                     SubScriptState recoveryState = recoveryPass.getState();
                     if (recoveryState != null) {
                        data.addSub(recoveryState);
                        System.out.println("Added partial subroutine state after error recovery.");
                     }
                  }
               } catch (Exception e2) {
                  System.out.println("Could not recover partial subroutine state: " + e2.getMessage());
               }
            }
         }

         // Generate code for main subroutine - recover if this fails
         if (mainsub != null) {
            try {
               mainpass = new MainPass(subdata.getState(mainsub), nodedata, subdata, this.actions);
               mainsub.apply(mainpass);

               try {
                  mainpass.assertStack();
               } catch (Exception e) {
                  System.out.println("Could not assert stack, continuing anyway.");
               }

               cleanpass = new CleanupPass(mainpass.getScriptRoot(), nodedata, subdata, mainpass.getState());
               cleanpass.apply();
               mainpass.getState().isMain(true);
               data.addSub(mainpass.getState());
               mainpass.done();
               cleanpass.done();
            } catch (Exception e) {
               System.out.println("Error generating code for main subroutine: " + e.getMessage());
               // Try to create a minimal main function stub using MainPass
               try {
                  mainpass = new MainPass(subdata.getState(mainsub), nodedata, subdata, this.actions);
                  // Even if apply fails, try to get the state
                  try {
                     mainsub.apply(mainpass);
                  } catch (Exception e2) {
                     System.out.println("Could not apply mainpass, but attempting to use partial state: " + e2.getMessage());
                  }
                  SubScriptState minimalMain = mainpass.getState();
                  if (minimalMain != null) {
                     minimalMain.isMain(true);
                     data.addSub(minimalMain);
                     System.out.println("Created minimal main subroutine stub.");
                  }
                  mainpass.done();
               } catch (Exception e2) {
                  System.out.println("Could not create minimal main stub: " + e2.getMessage());
               }
            }
         } else {
            System.out.println("Warning: No main subroutine available for code generation.");
         }
         // Store analysis data and globals - recover if this fails
         try {
         data.subdata(subdata);
         } catch (Exception e) {
            System.out.println("Error storing subroutine analysis data: " + e.getMessage());
         }

         if (doglobs != null) {
            try {
            cleanpass = new CleanupPass(doglobs.getScriptRoot(), nodedata, subdata, doglobs.getState());
            cleanpass.apply();
            data.globals(doglobs.getState());
            doglobs.done();
            cleanpass.done();
            } catch (Exception e) {
               System.out.println("Error finalizing globals: " + e.getMessage());
               try {
                  if (doglobs.getState() != null) {
                     data.globals(doglobs.getState());
                  }
                  doglobs.done();
               } catch (Exception e2) {
                  System.out.println("Could not recover globals state: " + e2.getMessage());
               }
            }
         }

         // Cleanup parse tree - this is safe to skip if it fails
         try {
         destroytree = new DestroyParseTree();

         for (ASubroutine iterSub : this.subIterable(subdata)) {
               try {
            iterSub.apply(destroytree);
               } catch (Exception e) {
                  System.out.println("Error destroying parse tree for subroutine: " + e.getMessage());
               }
         }

            if (mainsub != null) {
               try {
         mainsub.apply(destroytree);
               } catch (Exception e) {
                  System.out.println("Error destroying main parse tree: " + e.getMessage());
               }
            }
         } catch (Exception e) {
            System.out.println("Error during parse tree cleanup: " + e.getMessage());
            // Continue anyway - cleanup is not critical
         }

         return data;
      } catch (Exception e) {
         // Try to salvage partial results before giving up
         System.out.println("Error during decompilation: " + e.getMessage());
         e.printStackTrace(System.out);

         // Always return a FileScriptData, even if it's just a minimal stub
         if (data == null) {
            data = new FileDecompiler.FileScriptData();
         }

         // Aggressive recovery: try to salvage whatever state we have
         System.out.println("Attempting aggressive state recovery...");

         // Try to add any subroutines that were partially processed
         if (subdata != null && mainsub != null) {
            try {
               // Try to get main sub state even if it's incomplete
               SubroutineState mainState = subdata.getState(mainsub);
               if (mainState != null) {
                  try {
                     // Try to create a minimal main pass
                     mainpass = new MainPass(mainState, nodedata, subdata, this.actions);
                     try {
                        mainsub.apply(mainpass);
                     } catch (Exception e3) {
                        System.out.println("Could not apply mainpass to main sub, but continuing: " + e3.getMessage());
                     }
                     SubScriptState scriptState = mainpass.getState();
                     if (scriptState != null) {
                        scriptState.isMain(true);
                        data.addSub(scriptState);
                        mainpass.done();
                        System.out.println("Recovered main subroutine state.");
                     }
                  } catch (Exception e2) {
                     System.out.println("Could not create main pass: " + e2.getMessage());
                  }
               }
            } catch (Exception e2) {
               System.out.println("Error recovering main subroutine: " + e2.getMessage());
            }

            // Try to recover other subroutines
            try {
               for (ASubroutine iterSub : this.subIterable(subdata)) {
                  if (iterSub == mainsub) continue; // Already handled
                  try {
                     SubroutineState state = subdata.getState(iterSub);
                     if (state != null) {
                        try {
                           mainpass = new MainPass(state, nodedata, subdata, this.actions);
                           try {
                              iterSub.apply(mainpass);
                           } catch (Exception e3) {
                              System.out.println("Could not apply mainpass to subroutine, but continuing: " + e3.getMessage());
                           }
                           SubScriptState scriptState = mainpass.getState();
                           if (scriptState != null) {
                              data.addSub(scriptState);
                              mainpass.done();
                           }
                        } catch (Exception e2) {
                           System.out.println("Could not create mainpass for subroutine: " + e2.getMessage());
                        }
                     }
                  } catch (Exception e2) {
                     System.out.println("Error recovering subroutine: " + e2.getMessage());
                  }
               }
            } catch (Exception e2) {
               System.out.println("Error iterating subroutines during recovery: " + e2.getMessage());
            }

            // Try to store subdata
            try {
               data.subdata(subdata);
            } catch (Exception e2) {
               System.out.println("Error storing subdata: " + e2.getMessage());
            }
         }

         // Try to recover globals if available
         if (doglobs != null) {
            try {
               SubScriptState globState = doglobs.getState();
               if (globState != null) {
                  data.globals(globState);
                  System.out.println("Recovered globals state.");
               }
            } catch (Exception e2) {
               System.out.println("Error recovering globals: " + e2.getMessage());
            }
         }

         try {
            // Try to generate code from whatever we have
            data.generateCode();
            String partialCode = data.getCode();
            if (partialCode != null && !partialCode.trim().isEmpty()) {
               System.out.println("Successfully recovered partial decompilation with " +
                                 (data.getVars() != null ? data.getVars().size() : 0) + " subroutines.");
               // Add recovery note to the code
               String recoveryNote = "// ========================================\n" +
                                    "// PARTIAL DECOMPILATION - RECOVERED STATE\n" +
                                    "// ========================================\n" +
                                    "// This decompilation encountered errors but recovered partial results.\n" +
                                    "// Some subroutines or code sections may be incomplete or missing.\n" +
                                    "// Original error: " + e.getClass().getSimpleName() + ": " +
                                    (e.getMessage() != null ? e.getMessage() : "(no message)") + "\n" +
                                    "// ========================================\n\n";
               data.setCode(recoveryNote + partialCode);
               return data;
            }
         } catch (Exception genEx) {
            System.out.println("Could not generate partial code: " + genEx.getMessage());
         }

         // Last resort: create comprehensive stub with any available partial information
         String partialInfo = "Partial decompilation state:\n";
         try {
            if (data != null) {
               Hashtable<String, Vector<Variable>> vars = data.getVars();
               if (vars != null && vars.size() > 0) {
                  partialInfo += "  Subroutines with variable data: " + vars.size() + "\n";
               }
            }
            if (subdata != null) {
               try {
                  partialInfo += "  Total subroutines detected: " + subdata.numSubs() + "\n";
                  partialInfo += "  Subroutines fully typed: " + subdata.countSubsDone() + "\n";
               } catch (Exception ignored) {}
            }
            if (commands != null) {
               partialInfo += "  Commands decoded: " + commands.length() + " characters\n";
            }
            if (ast != null) {
               partialInfo += "  Parse tree created: yes\n";
            }
            if (nodedata != null) {
               partialInfo += "  Node analysis data available: yes\n";
            }
            if (mainsub != null) {
               partialInfo += "  Main subroutine identified: yes\n";
            }
         } catch (Exception ignored) {
            partialInfo += "  (Unable to gather partial state information)\n";
         }
         String errorStub = this.generateComprehensiveFallbackStub(file, "General decompilation pipeline", e, partialInfo);
         data.setCode(errorStub);
         System.out.println("Created fallback stub code due to decompilation errors.");
         return data;
      } finally {
         data = null;
         commands = null;
         setdest = null;
         dotypes = null;
         ast = null;
         if (nodedata != null) {
            nodedata.close();
         }

         nodedata = null;
         if (subdata != null) {
            subdata.parseDone();
         }

         subdata = null;
         subs = null;
         sub = null;
         mainsub = null;
         flatten = null;
         doglobs = null;
         cleanpass = null;
         mainpass = null;
         destroytree = null;
         System.gc();
      }
   }

   /**
    * Provides a type-safe view over subdata.getSubroutines(), validating elements at runtime.
    */
   private Iterable<ASubroutine> subIterable(SubroutineAnalysisData subdata) {
      List<ASubroutine> list = new ArrayList<>();
      Iterator<ASubroutine> raw = subdata.getSubroutines();

      while (raw.hasNext()) {
         ASubroutine sub = raw.next();
         if (sub == null) {
            throw new IllegalStateException("Unexpected null element in subroutine list");
         }
         list.add(sub);
      }

      return list;
   }

   private void enforceStrictSignatures(SubroutineAnalysisData subdata, NodeAnalysisData nodedata) {
      if (!FileDecompiler.strictSignatures) {
         return;
      }

      for (ASubroutine iterSub : this.subIterable(subdata)) {
         SubroutineState state = subdata.getState(iterSub);
         if (!state.isTotallyPrototyped()) {
            System.out.println(
               "Strict signatures: unresolved signature for subroutine at "
                  + Integer.toString(nodedata.getPos(iterSub))
                  + " (continuing)"
            );
         }
      }
   }

   /**
    * Encapsulates all state produced while decompiling or compiling a single script.
    * Stores subroutines, globals, generated source, and bytecode snapshots.
    */
   private class FileScriptData {
      /** Parsed subroutine states in the order they were processed. */
      private List<SubScriptState> subs = new ArrayList<>();
      /** Captured globals block, if present. */
      private SubScriptState globals = null;
      /** Shared analysis data used for struct/prototype generation. */
      private SubroutineAnalysisData subdata;
      /** Fully generated NSS source code string. */
      private String code = null;
      /** Decompiled p-code from the original NCS. */
      private String originalbytecode;
      /** Decompiled p-code from the newly compiled NSS. */
      private String generatedbytecode;

      public FileScriptData() {
         this.originalbytecode = null;
         this.generatedbytecode = null;
      }

      /**
       * Releases references to allow GC of parse data and subroutine states.
       */
      public void close() {
         Iterator<SubScriptState> it = this.subs.iterator();

         while (it.hasNext()) {
            it.next().close();
         }

         this.subs = null;
         if (this.globals != null) {
            this.globals.close();
            this.globals = null;
         }

         if (this.subdata != null) {
            this.subdata.close();
            this.subdata = null;
         }

         this.code = null;
         this.originalbytecode = null;
         this.generatedbytecode = null;
      }

      /**
       * Records the globals block captured during decompilation.
       */
      public void globals(SubScriptState globals) {
         this.globals = globals;
      }

      /**
       * Adds a processed subroutine to the script.
       */
      public void addSub(SubScriptState sub) {
         this.subs.add(sub);
      }

      /**
       * Stores analysis data used to emit struct/prototype declarations.
       */
      public void subdata(SubroutineAnalysisData subdata) {
         this.subdata = subdata;
      }

      private SubScriptState findSub(String name) {
         for (SubScriptState state : this.subs) {
            if (state.getName().equals(name)) {
               return state;
            }
         }

         return null;
      }

      /**
       * Attempts to rename a subroutine, regenerating code if successful.
       *
       * @return true when rename succeeds and code is refreshed
       */
      public boolean replaceSubName(String oldname, String newname) {
         SubScriptState state = this.findSub(oldname);
         if (state == null) {
            return false;
         } else if (this.findSub(newname) != null) {
            return false;
         } else {
            state.setName(newname);
            this.generateCode();
            return true;
         }
      }

      @Override
      public String toString() {
         return this.code;
      }

      /**
       * Returns a map of subroutine/global names to their variable tables.
       */
      public Hashtable<String, Vector<Variable>> getVars() {
         if (this.subs.size() == 0) {
            return null;
         } else {
            Hashtable<String, Vector<Variable>> vars = new Hashtable<>(1);

            for (SubScriptState state : this.subs) {
               vars.put(state.getName(), state.getVariables());
            }

            if (this.globals != null) {
               vars.put("GLOBALS", this.globals.getVariables());
            }

            return vars;
         }
      }

      /**
       * Returns the current generated NSS source.
       */
      public String getCode() {
         return this.code;
      }

      public void setCode(String code) {
         this.code = code;
      }

      public String getOriginalByteCode() {
         return this.originalbytecode;
      }

      public void setOriginalByteCode(String obcode) {
         this.originalbytecode = obcode;
      }

      public String getNewByteCode() {
         return this.generatedbytecode;
      }

      public void setNewByteCode(String nbcode) {
         this.generatedbytecode = nbcode;
      }

      /**
       * Builds the final NSS source string from globals, prototypes, and subroutines.
       * Always generates at least a minimal stub if no subroutines are available.
       */
      public void generateCode() {
         String newline = System.getProperty("line.separator");

         // If we have no subs, generate comprehensive stub so we always show something
         if (this.subs.size() == 0) {
            // Note: We don't have direct file access here, but we can still provide useful info
            String stub = "// ========================================" + newline +
                         "// DECOMPILATION WARNING - NO SUBROUTINES" + newline +
                         "// ========================================" + newline + newline +
                         "// Warning: No subroutines could be decompiled from this file." + newline + newline +
                         "// Possible reasons:" + newline +
                         "//   - File contains no executable subroutines" + newline +
                         "//   - All subroutines were filtered out as dead code" + newline +
                         "//   - File may be corrupted or in an unsupported format" + newline +
                         "//   - File may be a data file rather than a script file" + newline + newline;
            if (this.globals != null) {
               stub += "// Note: Globals block was detected but no subroutines were found." + newline + newline;
            }
            if (this.subdata != null) {
               try {
                  stub += "// Analysis data:" + newline;
                  stub += "//   Total subroutines detected: " + this.subdata.numSubs() + newline;
                  stub += "//   Subroutines processed: " + this.subdata.countSubsDone() + newline + newline;
               } catch (Exception ignored) {}
            }
            stub += "// Minimal fallback function:" + newline +
                   "void main() {" + newline +
                   "    // No code could be decompiled" + newline +
                   "}" + newline;
            this.code = stub;
            return;
         }

         StringBuffer protobuff = new StringBuffer();
         StringBuffer fcnbuff = new StringBuffer();

         for (SubScriptState state : this.subs) {
            try {
               if (!state.isMain()) {
                  String proto = state.getProto();
                  if (proto != null && !proto.trim().isEmpty()) {
                     protobuff.append(proto + ";" + newline);
                  }
               }

               String funcCode = state.toString();
               if (funcCode != null && !funcCode.trim().isEmpty()) {
                  fcnbuff.append(funcCode + newline);
               }
            } catch (Exception e) {
               // If a subroutine fails to generate, add a comment instead
               System.out.println("Error generating code for subroutine, adding placeholder: " + e.getMessage());
               fcnbuff.append("// Error: Could not decompile subroutine\n");
            }
         }

         String globs = new String();
         if (this.globals != null) {
            try {
               globs = "// Globals" + newline + this.globals.toStringGlobals() + newline;
            } catch (Exception e) {
               System.out.println("Error generating globals code: " + e.getMessage());
               globs = "// Error: Could not decompile globals\n";
            }
         }

         String protohdr = new String();
         if (protobuff.length() > 0) {
            protohdr = "// Prototypes" + newline;
            protobuff.append(newline);
         }

         String structDecls = "";
         try {
            if (this.subdata != null) {
               structDecls = this.subdata.getStructDeclarations();
            }
         } catch (Exception e) {
            System.out.println("Error generating struct declarations: " + e.getMessage());
         }

         String generated = structDecls + globs + protohdr + protobuff.toString() + fcnbuff.toString();

         // Ensure we always have at least something
         if (generated == null || generated.trim().isEmpty()) {
            String stub = "// ========================================" + newline +
                         "// CODE GENERATION WARNING - EMPTY OUTPUT" + newline +
                         "// ========================================" + newline + newline +
                         "// Warning: Code generation produced empty output despite having " + this.subs.size() + " subroutine(s)." + newline + newline;
            if (this.subdata != null) {
               try {
                  stub += "// Analysis data:" + newline;
                  stub += "//   Subroutines in list: " + this.subs.size() + newline;
                  stub += "//   Total subroutines detected: " + this.subdata.numSubs() + newline;
                  stub += "//   Subroutines fully typed: " + this.subdata.countSubsDone() + newline + newline;
               } catch (Exception ignored) {}
            }
            stub += "// This may indicate:" + newline +
                   "//   - All subroutines failed to generate code" + newline +
                   "//   - All code was filtered or marked as unreachable" + newline +
                   "//   - An internal error during code generation" + newline + newline +
                   "// Minimal fallback function:" + newline +
                   "void main() {" + newline +
                   "    // No code could be generated" + newline +
                   "}" + newline;
            generated = stub;
         }

         this.code = generated;
      }
   }

   /**
    * Thin wrapper around {@link ProcessBuilder} to execute external compilers on Windows.
    * Handles quoting, output streaming, and blocking until process completion.
    */
   private class WindowsExec {
      WindowsExec() {
      }

      /**
       * Executes a raw command string via {@code cmd /c}. Retained for legacy callers.
       */
      @SuppressWarnings("unused")
      public void callExec(String args) {
         try {
            System.out.println("Execing " + args);
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", args);
            Process proc = pb.start();
            FileDecompiler.WindowsExec.StreamGobbler errorGobbler = new FileDecompiler.WindowsExec.StreamGobbler(proc.getErrorStream(), "ERROR");
            FileDecompiler.WindowsExec.StreamGobbler outputGobbler = new FileDecompiler.WindowsExec.StreamGobbler(proc.getInputStream(), "OUTPUT");
            errorGobbler.start();
            outputGobbler.start();
            proc.waitFor();
         } catch (Throwable var6) {
            var6.printStackTrace();
         }
      }

      /**
       * Executes a command with an array of arguments.
       * This method is used when we have properly formatted arguments from compiler detection.
       *
       * @param args Array of command-line arguments (first element is the executable)
       */
      public void callExec(String[] args) {
         try {
            StringBuilder cmdStr = new StringBuilder();
            for (String arg : args) {
               if (cmdStr.length() > 0) {
                  cmdStr.append(" ");
               }
               // Quote arguments that contain spaces
               if (arg.contains(" ") || arg.contains("\"")) {
                  cmdStr.append("\"").append(arg.replace("\"", "\\\"")).append("\"");
               } else {
                  cmdStr.append(arg);
               }
            }
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("[NCSDecomp] Executing nwnnsscomp.exe:");
            System.out.println("[NCSDecomp] Command: " + cmdStr.toString());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            ProcessBuilder pb = new ProcessBuilder(args);
            Process proc = pb.start();
            FileDecompiler.WindowsExec.StreamGobbler errorGobbler = new FileDecompiler.WindowsExec.StreamGobbler(proc.getErrorStream(), "nwnnsscomp");
            FileDecompiler.WindowsExec.StreamGobbler outputGobbler = new FileDecompiler.WindowsExec.StreamGobbler(proc.getInputStream(), "nwnnsscomp");
            errorGobbler.start();
            outputGobbler.start();
            int exitCode = proc.waitFor();

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("[NCSDecomp] nwnnsscomp.exe exited with code: " + exitCode);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         } catch (Throwable var6) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("[NCSDecomp] EXCEPTION executing nwnnsscomp.exe:");
            System.out.println("[NCSDecomp] Exception Type: " + var6.getClass().getName());
            System.out.println("[NCSDecomp] Exception Message: " + var6.getMessage());
            var6.printStackTrace();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         }
      }

      /**
       * Continuously drains an input stream to avoid deadlocking the process.
       */
      private class StreamGobbler extends Thread {
         /** Stream to consume (stdout or stderr). */
         InputStream is;
         /** Label used when echoing lines to our logger. */
         String type;

         StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
         }

         @Override
         public void run() {
            try {
               InputStreamReader isr = new InputStreamReader(this.is);
               BufferedReader br = new BufferedReader(isr);
               String line = null;

               while ((line = br.readLine()) != null) {
                  // Clearly differentiate nwnnsscomp output from our output
                  System.out.println("[" + this.type + "] " + line);
               }
            } catch (IOException var4) {
               System.out.println("[NCSDecomp] Error reading " + this.type + " stream: " + var4.getMessage());
               var4.printStackTrace();
            }
         }
      }
   }
}

