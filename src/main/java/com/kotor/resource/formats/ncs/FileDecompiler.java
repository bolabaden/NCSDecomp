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
    * Attempts to load the action table from the working directory.
    * <p>
    * The lookup matches legacy behavior: {@code tsl_nwscript.nss} for TSL,
    * otherwise {@code k1_nwscript.nss}. This method isolates the IO and error
    * handling so callers receive a single {@link DecompilerException}.
    */
   private static ActionsData loadActionsDataInternal(boolean isK2Selected) throws DecompilerException {
      try {
         File dir = new File(System.getProperty("user.dir"));
         File actionfile = isK2Selected ? new File(dir, "tsl_nwscript.nss") : new File(dir, "k1_nwscript.nss");
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
    *
    * @param file NCS file to decompile
    * @return One of {@link #SUCCESS}, {@link #PARTIAL_COMPILE}, {@link #PARTIAL_COMPARE}, or {@link #FAILURE}
    * @throws DecompilerException when parsing or external compilation fails
    */
   public int decompile(File file) throws DecompilerException {
      this.ensureActionsLoaded();
      FileDecompiler.FileScriptData data = this.filedata.get(file);
      if (data == null) {
         System.out.println("\n---> starting decompilation: " + file.getName() + " <---");
         data = this.decompileNcs(file);
         if (data == null) {
            return 0;
         }

         this.filedata.put(file, data);
      }

      data.generateCode();
      return this.compileAndCompare(file, data.getCode(), data);
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
      File newcompiled = null;
      File newdecompiled = null;
      File olddecompiled = null;
      this.checkCompilerExists();

      try {
         olddecompiled = this.externalDecompile(file, isK2Selected);
         if (olddecompiled == null) {
            System.out.println("nwnnsscomp decompile of old compiled file failed.  Check code.");
            return 2;
         }

         data.setOriginalByteCode(this.readFile(olddecompiled));
         newcompiled = this.externalCompile(newfile, isK2Selected);
         if (newcompiled == null) {
            return 2;
         }

         newdecompiled = this.externalDecompile(newcompiled, isK2Selected);
         if (newdecompiled == null) {
            System.out.println("nwnnsscomp decompile of new compiled file failed.  Check code.");
            return 2;
         }

         data.setNewByteCode(this.readFile(newdecompiled));
         if (this.compareBinaryFiles(file, newcompiled)) {
            return 1;
         }

         // Fall back to textual pcode comparison to aid debugging.
         String diff = this.comparePcodeFiles(olddecompiled, newdecompiled);
         if (diff != null) {
            System.out.println("P-code difference: " + diff);
         }
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

      return 3;
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
      File newcompiled = null;
      File newdecompiled = null;
      this.checkCompilerExists();

      try {
         newcompiled = this.externalCompile(nssFile, isK2Selected);
         if (newcompiled == null) {
            return 0;
         }

         newdecompiled = this.externalDecompile(newcompiled, isK2Selected);
         if (newdecompiled != null) {
            data.setNewByteCode(this.readFile(newdecompiled));
            return 1;
         }

         System.out.println("nwnnsscomp decompile of new compiled file failed.  Check code.");
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

      return 0;
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
    * Returns the expected nwnnsscomp executable location (current working directory).
    */
   private File getCompilerFile() {
      return new File("nwnnsscomp.exe");
   }

   /**
    * Guard that ensures the compiler binary is present before external invocation.
    */
   private void checkCompilerExists() throws DecompilerException {
      File compiler = getCompilerFile();
      if (!compiler.exists()) {
         throw new DecompilerException("The compiler " + compiler.getAbsolutePath() + " could not be opened.");
      }
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
            System.out.println("Compiler not found: " + compiler.getAbsolutePath());
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

         System.out.println("Using compiler: " + config.getChosenCompiler().getName() +
            " (SHA256: " + config.getSha256Hash().substring(0, 16) + "...)");

         new FileDecompiler.WindowsExec().callExec(args);
         return !result.exists() ? null : result;
      } catch (Exception e) {
         System.out.println("Error during external decompile: " + e.getMessage());
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
            System.out.println("Compiler not found: " + compiler.getAbsolutePath());
            return null;
         }

         String outname = this.getShortName(file) + ".ncs";
         File result = new File(outname);

         // Use compiler detection to get correct command-line arguments
         NwnnsscompConfig config = new NwnnsscompConfig(compiler, file, result, k2);
         List<File> includeDirs = this.buildIncludeDirs(k2);
         String[] args = config.getCompileArgs(compiler.getAbsolutePath(), includeDirs);

         System.out.println("Using compiler: " + config.getChosenCompiler().getName() +
            " (SHA256: " + config.getSha256Hash().substring(0, 16) + "...)");

         new FileDecompiler.WindowsExec().callExec(args);
         return !result.exists() ? null : result;
      } catch (Exception e) {
         System.out.println("Error during external compile: " + e.getMessage());
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
   @SuppressWarnings("unchecked")
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
         System.out.println("null action!");
         return null;
      }

      try {
         data = new FileDecompiler.FileScriptData();
         commands = new Decoder(new BufferedInputStream(new FileInputStream(file)), this.actions).decode();
         ast = new Parser(new Lexer(new PushbackReader(new StringReader(commands), 1024))).parse();
         nodedata = new NodeAnalysisData();
         subdata = new SubroutineAnalysisData(nodedata);
         ast.apply(new SetPositions(nodedata));
         setdest = new SetDestinations(ast, nodedata, subdata);
         ast.apply(setdest);
         ast.apply(new SetDeadCode(nodedata, subdata, setdest.getOrigins()));
         setdest.done();
         setdest = null;
         subdata.splitOffSubroutines(ast);
         ast = null;
         mainsub = subdata.getMainSub();
         flatten = new FlattenSub(mainsub, nodedata);
         mainsub.apply(flatten);
         for (ASubroutine iterSub : this.subIterable(subdata)) {
            flatten.setSub(iterSub);
            iterSub.apply(flatten);
         }

         flatten.done();
         flatten = null;
         sub = subdata.getGlobalsSub();
         if (sub != null) {
            doglobs = new DoGlobalVars(nodedata, subdata);
            sub.apply(doglobs);
            cleanpass = new CleanupPass(doglobs.getScriptRoot(), nodedata, subdata, doglobs.getState());
            cleanpass.apply();
            subdata.setGlobalStack(doglobs.getStack());
            subdata.globalState(doglobs.getState());
            cleanpass.done();
         }

         PrototypeEngine proto = new PrototypeEngine(nodedata, subdata, this.actions, FileDecompiler.strictSignatures);
         proto.run();

         dotypes = new DoTypes(subdata.getState(mainsub), nodedata, subdata, this.actions, false);
         mainsub.apply(dotypes);

         try {
            dotypes.assertStack();
         } catch (Exception e) {
            System.out.println("Could not assert stack, continuing anyway.");
         }

         dotypes.done();
         boolean alldone = subdata.countSubsDone() == subdata.numSubs();
         boolean onedone = true;
         int donecount = subdata.countSubsDone();

         for (int loopcount = 0; !alldone && onedone && loopcount < 1000; ++loopcount) {
            onedone = false;
            subs = subdata.getSubroutines();

            while (subs.hasNext()) {
            sub = subs.next();
               dotypes = new DoTypes(subdata.getState(sub), nodedata, subdata, this.actions, false);
               sub.apply(dotypes);
               dotypes.done();
            }

            dotypes = new DoTypes(subdata.getState(mainsub), nodedata, subdata, this.actions, false);
            mainsub.apply(dotypes);
            dotypes.done();
            alldone = subdata.countSubsDone() == subdata.numSubs();
            onedone = onedone || subdata.countSubsDone() > donecount;
            donecount = subdata.countSubsDone();
         }

         if (!alldone) {
            System.out.println("Unable to do final prototype of all subroutines.");
            //FIXME: causes crashes that prevent any output from showing up.
            //FileScriptData fileScriptData = null;
            //return fileScriptData;
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
            }
         }

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
         data.subdata(subdata);
         if (doglobs != null) {
            cleanpass = new CleanupPass(doglobs.getScriptRoot(), nodedata, subdata, doglobs.getState());
            cleanpass.apply();
            data.globals(doglobs.getState());
            doglobs.done();
            cleanpass.done();
         }

         destroytree = new DestroyParseTree();

         for (ASubroutine iterSub : this.subIterable(subdata)) {
            iterSub.apply(destroytree);
         }

         mainsub.apply(destroytree);
         return data;
      } catch (Exception e) {
         e.printStackTrace(System.out);
         return null;
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

      @SuppressWarnings("unused")
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
       */
      public void generateCode() {
         if (this.subs.size() == 0) {
            return;
         }

         String newline = System.getProperty("line.separator");
         StringBuffer protobuff = new StringBuffer();
         StringBuffer fcnbuff = new StringBuffer();

         for (SubScriptState state : this.subs) {
            if (!state.isMain()) {
               protobuff.append(state.getProto() + ";" + newline);
            }

            fcnbuff.append(state.toString() + newline);
         }

         String globs = new String();
         if (this.globals != null) {
            globs = "// Globals" + newline + this.globals.toStringGlobals() + newline;
         }

         String protohdr = new String();
         if (protobuff.length() > 0) {
            protohdr = "// Prototypes" + newline;
            protobuff.append(newline);
         }

         this.code = this.subdata.getStructDeclarations() + globs + protohdr + protobuff.toString() + fcnbuff.toString();
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
            System.out.println("Execing " + cmdStr.toString());

            ProcessBuilder pb = new ProcessBuilder(args);
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
                  System.out.println(this.type + ">" + line);
               }
            } catch (IOException var4) {
               var4.printStackTrace();
            }
         }
      }
   }
}

