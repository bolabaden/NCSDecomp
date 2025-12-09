// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless CLI entrypoint for NCSDecomp.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse command-line options (input files/dirs, output, encoding, game variant).</li>
 *   <li>Locate the appropriate {@code nwscript.nss} (K1/TSL) and initialize {@link FileDecompiler}.</li>
 *   <li>Decompile NCS to NSS (stdout or files) without invoking external compilers.</li>
 * </ul>
 */
public final class NCSDecompCLI {
   private NCSDecompCLI() {
   }

   public static void main(String[] args) {
      CliConfig cfg;
      try {
         cfg = parseArgs(args);
      } catch (IllegalArgumentException ex) {
         System.err.println("Error: " + ex.getMessage());
         printUsage();
         System.exit(1);
         return;
      }

      if (cfg.help) {
         printUsage();
         return;
      }

      if (cfg.version) {
         printVersion();
         return;
      }

      if (cfg.inputs.isEmpty()) {
         System.err.println("Error: at least one input .ncs file or directory is required.");
         printUsage();
         System.exit(1);
         return;
      }

      // Determine nwscript file path
      File nwscriptFile = null;
      if (cfg.nwscriptPath != null) {
         // Explicit path provided via --nwscript
         nwscriptFile = new File(cfg.nwscriptPath);
         if (!nwscriptFile.isFile()) {
            System.err.println("Error: nwscript file does not exist: " + nwscriptFile.getAbsolutePath());
            System.exit(1);
            return;
         }
      } else {
         // Try to find nwscript file in multiple locations:
         // 1. Current working directory
         // 2. Executable directory (for jpackage app-images)
         // 3. JAR directory
         String nssName = cfg.isK2 ? "tsl_nwscript.nss" : "k1_nwscript.nss";

         // Try current working directory first
         File cwd = new File(System.getProperty("user.dir"));
         nwscriptFile = new File(cwd, nssName);

         if (!nwscriptFile.isFile()) {
            // Try executable directory (jpackage puts nss files in app directory)
            String exePath = System.getProperty("java.launcher.path");
            if (exePath != null) {
               File exeDir = new File(exePath).getParentFile();
               if (exeDir != null) {
                  File appDir = new File(exeDir, "app");
                  if (appDir.exists()) {
                     nwscriptFile = new File(appDir, nssName);
                  }
               }
            }
         }

         if (!nwscriptFile.isFile()) {
            // Try JAR directory
            try {
               String jarPath = NCSDecompCLI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
               File jarDir = new File(jarPath).getParentFile();
               if (jarDir != null) {
                  nwscriptFile = new File(jarDir, nssName);
               }
            } catch (Exception e) {
               // Ignore
            }
         }

         if (!nwscriptFile.isFile()) {
            System.err.println("Error: nwscript file not found: " + nssName);
            System.err.println("Searched in:");
            System.err.println("  - Current directory: " + cwd.getAbsolutePath());
            System.err.println("  - Executable app directory");
            System.err.println("  - JAR directory");
            System.err.println("");
            System.err.println("Please use --nwscript <path> to specify the nwscript.nss file location,");
            System.err.println("or ensure " + nssName + " is in one of the above directories.");
            System.exit(1);
            return;
         }
      }

      FileDecompiler.isK2Selected = cfg.isK2;
      Charset charset = cfg.encoding;

      List<File> worklist = new ArrayList<>();
      for (String input : cfg.inputs) {
         File f = new File(input);
         if (!f.exists()) {
            System.err.println("Warning: input does not exist, skipping: " + f.getAbsolutePath());
            continue;
         }
         collect(f, cfg.recursive, worklist);
      }

      if (worklist.isEmpty()) {
         System.err.println("No .ncs files found to decompile.");
         System.exit(1);
         return;
      }

      if (cfg.output != null && worklist.size() > 1) {
         System.err.println("Error: --output may only be used with a single input file.");
         System.exit(1);
         return;
      }

      try {
         // Use CLI-specific constructor with explicit nwscript path (no config files needed)
         FileDecompiler fd = new FileDecompiler(nwscriptFile);
         for (File in : worklist) {
            try {
               if (cfg.stdout) {
                  String code = fd.decompileToString(in);
                  System.out.println("// " + in.getName());
                  System.out.println(code);
               } else {
                  File outFile = resolveOutput(in, cfg);
                  fd.decompileToFile(in, outFile, charset, cfg.overwrite);
                  if (!cfg.quiet) {
                     System.out.println("Decompiled " + in.getAbsolutePath() + " -> " + outFile.getAbsolutePath());
                  }
               }
            } catch (Exception ex) {
               System.err.println("Failed to decompile " + in.getAbsolutePath() + ": " + ex.getMessage());
               if (cfg.failFast) {
                  System.exit(1);
               }
            }
         }
      } catch (Exception ex) {
         System.err.println("Fatal: " + ex.getMessage());
         System.exit(1);
      }
   }

   private static void collect(File f, boolean recursive, List<File> out) {
      if (f.isFile() && f.getName().toLowerCase().endsWith(".ncs")) {
         out.add(f);
      } else if (f.isDirectory()) {
         File[] kids = f.listFiles();
         if (kids == null) {
            return;
         }
         for (File kid : kids) {
            if (kid.isFile() && kid.getName().toLowerCase().endsWith(".ncs")) {
               out.add(kid);
            } else if (recursive && kid.isDirectory()) {
               collect(kid, true, out);
            }
         }
      }
   }

   private static File resolveOutput(File input, CliConfig cfg) {
      if (cfg.output != null) {
         return new File(cfg.output);
      }

      String base = stripExtension(input.getName());
      String name = cfg.prefix + base + cfg.suffix + cfg.extension;
      File outDir = cfg.outDir != null ? new File(cfg.outDir) : input.getParentFile();
      return new File(outDir, name);
   }

   private static String stripExtension(String name) {
      int dot = name.lastIndexOf('.');
      return dot == -1 ? name : name.substring(0, dot);
   }

   private static CliConfig parseArgs(String[] args) {
      CliConfig cfg = new CliConfig();
      for (int i = 0; i < args.length; i++) {
         String a = args[i];
         switch (a) {
            case "-h":
            case "--help":
               cfg.help = true;
               break;
            case "-v":
            case "--version":
               cfg.version = true;
               break;
            case "-i":
            case "--input":
               requireValue(args, i, a);
               cfg.inputs.add(args[++i]);
               break;
            case "-o":
            case "--output":
               requireValue(args, i, a);
               cfg.output = args[++i];
               break;
            case "-O":
            case "--out-dir":
               requireValue(args, i, a);
               cfg.outDir = args[++i];
               break;
            case "--suffix":
               requireValue(args, i, a);
               cfg.suffix = args[++i];
               break;
            case "--prefix":
               requireValue(args, i, a);
               cfg.prefix = args[++i];
               break;
            case "--ext":
               requireValue(args, i, a);
               String ext = args[++i];
               cfg.extension = ext.startsWith(".") ? ext : "." + ext;
               break;
            case "--encoding":
               requireValue(args, i, a);
               cfg.encoding = Charset.forName(args[++i]);
               break;
            case "--nwscript":
               requireValue(args, i, a);
               cfg.nwscriptPath = args[++i];
               break;
            case "--stdout":
               cfg.stdout = true;
               break;
            case "--overwrite":
               cfg.overwrite = true;
               break;
            case "-r":
            case "--recursive":
               cfg.recursive = true;
               break;
            case "--quiet":
               cfg.quiet = true;
               break;
            case "--fail-fast":
               cfg.failFast = true;
               break;
            case "--k1":
            case "--game=k1":
               cfg.isK2 = false;
               break;
            case "--k2":
            case "--tsl":
            case "--game=k2":
            case "--game=tsl":
               cfg.isK2 = true;
               break;
            case "--game":
               requireValue(args, i, a);
               cfg.isK2 = parseGame(args[++i]);
               break;
            default:
               if (a.startsWith("-")) {
                  throw new IllegalArgumentException("Unknown option: " + a);
               }
               cfg.inputs.add(a);
               break;
         }
      }
      return cfg;
   }

   private static boolean parseGame(String value) {
      String v = value.toLowerCase();
      return v.equals("k2") || v.equals("tsl") || v.equals("2") || v.contains("kotor2");
   }

   private static void requireValue(String[] args, int i, String opt) {
      if (i + 1 >= args.length) {
         throw new IllegalArgumentException("Option requires a value: " + opt);
      }
   }

   private static void printUsage() {
      String summary = "KotOR NCSDecomp headless decompiler (Beta 2, May 30 2006). Decompiles NCS -> NSS without external tools.";
      String author = "Original: JdNoa (decompiler), Dashus (GUI); further mods: th3w1zard1 | https://bolabaden.org | https://github.com/bolabaden";
      System.out.println(summary);
      System.out.println(author);
      System.out.println();
      System.out.println("Usage: java -cp NCSDecomp.jar com.kotor.resource.formats.ncs.NCSDecompCLI [options] <files/dirs>");
      System.out.println("Options:");
      System.out.println("  -h, --help                 Show help");
      System.out.println("  -v, --version              Show version info");
      System.out.println("  -i, --input <path>         Input .ncs file or directory (can repeat or pass positional)");
      System.out.println("  -o, --output <file>        Output .nss file (only when a single input is provided)");
      System.out.println("  -O, --out-dir <dir>        Output directory (defaults to input directory)");
      System.out.println("      --prefix <text>        Prefix for generated filenames");
      System.out.println("      --suffix <text>        Suffix for generated filenames");
      System.out.println("      --ext <ext>            Output extension (default: .nss)");
      System.out.println("      --encoding <name>      Output charset (default: UTF-8)");
      System.out.println("      --nwscript <path>      Path to nwscript.nss file (overrides --k1/--tsl)");
      System.out.println("      --stdout               Write decompiled source to stdout");
      System.out.println("      --overwrite            Overwrite existing files");
      System.out.println("  -r, --recursive            Recurse into directories when inputs are dirs");
      System.out.println("      --k1 | --k2 | --tsl    Select game (default k1). Looks for k1_nwscript.nss or");
      System.out.println("                            tsl_nwscript.nss in current directory. Use --nwscript");
      System.out.println("                            to specify a different location.");
      System.out.println("      --quiet                Suppress success logs");
      System.out.println("      --fail-fast            Stop on first decompile failure");
      System.out.println();
      System.out.println("Examples:");
      System.out.println("  Decompile single file to stdout:");
      System.out.println("    java -cp NCSDecomp.jar ...NCSDecompCLI -i foo.ncs --stdout");
      System.out.println("  Decompile directory recursively to out folder using TSL definitions:");
      System.out.println("    java -cp NCSDecomp.jar ...NCSDecompCLI -i scripts_dir -r --k2 -O out_dir");
   }

   private static void printVersion() {
      System.out.println("NCSDecomp CLI headless decompiler (Beta 2, May 30 2006)");
      System.out.println("Modified by th3w1zard1 | https://bolabaden.org | https://github.com/bolabaden");
   }

   private static final class CliConfig {
      final List<String> inputs = new ArrayList<>();
      String output;
      String outDir;
      String prefix = "";
      String suffix = "";
      String extension = ".nss";
      Charset encoding = StandardCharsets.UTF_8;
      boolean stdout = false;
      boolean overwrite = false;
      boolean recursive = false;
      boolean help = false;
      boolean version = false;
      boolean quiet = false;
      boolean failFast = false;
      boolean isK2 = false;
      String nwscriptPath = null;  // Explicit nwscript file path (CLI-only)
   }
}


