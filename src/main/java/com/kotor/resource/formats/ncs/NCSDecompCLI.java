// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
         File cwd = new File(System.getProperty("user.dir"));
         
         // If no game flag was explicitly set, try nwscript.nss in cwd first
         if (!cfg.gameExplicitlySet) {
            File genericNwscript = new File(cwd, "nwscript.nss");
            if (genericNwscript.isFile()) {
               nwscriptFile = genericNwscript;
            } else {
               // If nwscript.nss not found in cwd and no game flag set, raise error
               System.err.println("Error: nwscript.nss not found in current directory: " + cwd.getAbsolutePath());
               System.err.println("");
               System.err.println("Please use one of the following:");
               System.err.println("  - Use --nwscript <path> to specify the nwscript.nss file location");
               System.err.println("  - Use -g k1, -g k2, --k1, or --k2 to select a game (will look for game-specific nwscript files)");
               System.err.println("  - Ensure nwscript.nss exists in the current directory");
               System.exit(1);
               return;
            }
         } else {
            // Game flag was explicitly set, try game-specific files
            String nssName = cfg.isK2 ? "tsl_nwscript.nss" : "k1_nwscript.nss";

            // Try tools/ directory first
            nwscriptFile = new File(new File(cwd, "tools"), nssName);
            
            // Fall back to current working directory (legacy support)
            if (!nwscriptFile.isFile()) {
               nwscriptFile = new File(cwd, nssName);
            }

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
      }

      FileDecompiler.isK2Selected = cfg.isK2;
      FileDecompiler.preferSwitches = cfg.preferSwitches;
      FileDecompiler.strictSignatures = cfg.strictSignatures;
      Charset charset = cfg.encoding;

      // Collect files with their base directories for hierarchy preservation
      List<InputFile> worklist = new ArrayList<>();
      List<File> inputFiles = new ArrayList<>();
      List<File> inputDirs = new ArrayList<>();
      
      for (String input : cfg.inputs) {
         File f = new File(input);
         if (!f.exists()) {
            System.err.println("Warning: input does not exist, skipping: " + f.getAbsolutePath());
            continue;
         }
         if (f.isFile()) {
            inputFiles.add(f);
         } else if (f.isDirectory()) {
            inputDirs.add(f);
         }
      }

      // Collect all .ncs files
      for (File inputFile : inputFiles) {
         if (inputFile.getName().toLowerCase().endsWith(".ncs")) {
            worklist.add(new InputFile(inputFile, inputFile.getParentFile()));
         }
      }
      
      for (File inputDir : inputDirs) {
         collect(inputDir, cfg.recursive, worklist, inputDir);
      }

      if (worklist.isEmpty()) {
         System.err.println("No .ncs files found to decompile.");
         System.exit(1);
         return;
      }

      // Validate output configuration
      File outputFileOrDir = null;
      if (cfg.output != null) {
         outputFileOrDir = new File(cfg.output);
         // If output exists and is a directory, or if we have multiple inputs, treat as directory
         if (outputFileOrDir.exists() && outputFileOrDir.isDirectory()) {
            // Valid: directory output
         } else if (worklist.size() == 1 && !outputFileOrDir.exists()) {
            // Single file output - check if parent directory exists
            File parent = outputFileOrDir.getParentFile();
            if (parent != null && !parent.exists()) {
               System.err.println("Error: output directory does not exist: " + parent.getAbsolutePath());
               System.exit(1);
               return;
            }
         } else if (worklist.size() > 1) {
            // Multiple inputs - output must be a directory
            if (!outputFileOrDir.exists()) {
               // Create directory if it doesn't exist
               if (!outputFileOrDir.mkdirs()) {
                  System.err.println("Error: failed to create output directory: " + outputFileOrDir.getAbsolutePath());
                  System.exit(1);
                  return;
               }
            } else if (!outputFileOrDir.isDirectory()) {
               System.err.println("Error: --output must be a directory when processing multiple files: " + outputFileOrDir.getAbsolutePath());
               System.exit(1);
               return;
            }
         }
      } else {
         // No output specified - default to current working directory
         outputFileOrDir = new File(System.getProperty("user.dir"));
      }

      try {
         // Use CLI-specific constructor with explicit nwscript path (no config files needed)
         FileDecompiler fd = new FileDecompiler(nwscriptFile);
         for (InputFile input : worklist) {
            try {
               if (cfg.stdout) {
                  String code = fd.decompileToString(input.file);
                  System.out.println("// " + input.file.getName());
                  System.out.println(code);
               } else {
                  File outFile = resolveOutput(input, outputFileOrDir, cfg);
                  outFile.getParentFile().mkdirs(); // Ensure parent directory exists
                  fd.decompileToFile(input.file, outFile, charset, cfg.overwrite);
                  if (!cfg.quiet) {
                     System.out.println("Decompiled " + input.file.getAbsolutePath() + " -> " + outFile.getAbsolutePath());
                  }
               }
            } catch (Exception ex) {
               System.err.println("Failed to decompile " + input.file.getAbsolutePath() + ": " + ex.getMessage());
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

   private static void collect(File f, boolean recursive, List<InputFile> out, File baseDir) {
      if (f.isFile() && f.getName().toLowerCase().endsWith(".ncs")) {
         out.add(new InputFile(f, baseDir));
      } else if (f.isDirectory()) {
         File[] kids = f.listFiles();
         if (kids == null) {
            return;
         }
         for (File kid : kids) {
            if (kid.isFile() && kid.getName().toLowerCase().endsWith(".ncs")) {
               out.add(new InputFile(kid, baseDir));
            } else if (recursive && kid.isDirectory()) {
               collect(kid, true, out, baseDir);
            }
         }
      }
   }

   private static File resolveOutput(InputFile input, File outputFileOrDir, CliConfig cfg) {
      File inputFile = input.file;
      File baseDir = input.baseDir;
      
      // If output is explicitly a single file (and we have only one input), use it directly
      if (cfg.output != null && !outputFileOrDir.isDirectory()) {
         return outputFileOrDir;
      }

      String base = stripExtension(inputFile.getName());
      String name = cfg.prefix + base + cfg.suffix + cfg.extension;
      
      // Determine output directory
      File outDir;
      if (cfg.output != null && outputFileOrDir.isDirectory()) {
         // Output is a directory - preserve hierarchy
         outDir = outputFileOrDir;
         
         // Compute relative path from base directory to input file
         Path basePath = baseDir.toPath().toAbsolutePath().normalize();
         Path inputPath = inputFile.toPath().toAbsolutePath().normalize();
         Path relativePath = basePath.relativize(inputPath);
         
         // If there's a relative path (file is in a subdirectory), preserve it
         if (relativePath.getParent() != null) {
            outDir = new File(outDir, relativePath.getParent().toString());
         }
      } else if (cfg.outDir != null) {
         // Explicit output directory specified via --out-dir
         outDir = new File(cfg.outDir);
      } else {
         // Default: use outputFileOrDir (which defaults to cwd if output not specified)
         outDir = outputFileOrDir;
         
         // If we're processing from a directory input, preserve hierarchy relative to cwd
         // by computing relative path from the original input directory
         if (baseDir != null && !baseDir.equals(inputFile.getParentFile())) {
            // This file came from a directory input, preserve its relative path
            Path basePath = baseDir.toPath().toAbsolutePath().normalize();
            Path inputPath = inputFile.toPath().toAbsolutePath().normalize();
            Path relativePath = basePath.relativize(inputPath);
            
            if (relativePath.getParent() != null) {
               outDir = new File(outDir, relativePath.getParent().toString());
            }
         }
      }
      
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
            case "-g":
               requireValue(args, i, a);
               cfg.isK2 = parseGame(args[++i]);
               cfg.gameExplicitlySet = true;
               break;
            case "--k1":
            case "--game=k1":
               cfg.isK2 = false;
               cfg.gameExplicitlySet = true;
               break;
            case "--k2":
            case "--tsl":
            case "--game=k2":
            case "--game=tsl":
               cfg.isK2 = true;
               cfg.gameExplicitlySet = true;
               break;
            case "--game":
               requireValue(args, i, a);
               cfg.isK2 = parseGame(args[++i]);
               cfg.gameExplicitlySet = true;
               break;
            case "--prefer-switches":
               cfg.preferSwitches = true;
               break;
            case "--strict-signatures":
               cfg.strictSignatures = true;
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
      if (v.equals("k1") || v.equals("1") || v.contains("kotor1")) {
         return false;
      }
      return v.equals("k2") || v.equals("tsl") || v.equals("2") || v.contains("kotor2");
   }

   private static void requireValue(String[] args, int i, String opt) {
      if (i + 1 >= args.length) {
         throw new IllegalArgumentException("Option requires a value: " + opt);
      }
   }

   private static String getExecutableName() {
      // Try to get jpackaged EXE name first
      String exePath = System.getProperty("java.launcher.path");
      if (exePath != null) {
         File exeFile = new File(exePath);
         return exeFile.getName();
      }
      
      // Otherwise, try to get JAR name
      try {
         String jarPath = NCSDecompCLI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
         File jarFile = new File(jarPath);
         return jarFile.getName();
      } catch (Exception e) {
         // Fallback to default name if we can't determine it
         return "NCSDecomp.jar";
      }
   }

   private static void printUsage() {
      String executableName = getExecutableName();
      String summary = "KotOR NCSDecomp headless decompiler (Beta 2, May 30 2006). Decompiles NCS -> NSS without external tools.";
      String author = "Original: JdNoa (decompiler), Dashus (GUI); further mods: th3w1zard1 | https://bolabaden.org | https://github.com/bolabaden";
      System.out.println(summary);
      System.out.println(author);
      System.out.println();
      
      // Format usage line based on whether it's a JAR or EXE
      String usageLine;
      if (executableName.endsWith(".jar")) {
         usageLine = "Usage: java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI [options] <files/dirs>";
      } else {
         usageLine = "Usage: " + executableName + " [options] <files/dirs>";
      }
      System.out.println(usageLine);
      
      System.out.println("Options:");
      System.out.println("  -h, --help                 Show help");
      System.out.println("  -v, --version              Show version info");
      System.out.println("  -i, --input <path>         Input .ncs file or directory (can repeat or pass positional)");
      System.out.println("  -o, --output <path>        Output file or directory (defaults to current directory)");
      System.out.println("                            If input is a directory, output must be a directory.");
      System.out.println("                            Directory hierarchy is preserved when both are directories.");
      System.out.println("  -O, --out-dir <dir>        Output directory (defaults to input directory or cwd)");
      System.out.println("      --prefix <text>        Prefix for generated filenames");
      System.out.println("      --suffix <text>        Suffix for generated filenames");
      System.out.println("      --ext <ext>            Output extension (default: .nss)");
      System.out.println("      --encoding <name>      Output charset (default: Windows-1252)");
      System.out.println("      --nwscript <path>      Path to nwscript.nss file (overrides --k1/--tsl)");
      System.out.println("      --stdout               Write decompiled source to stdout");
      System.out.println("      --overwrite            Overwrite existing files");
      System.out.println("  -r, --recursive            Recurse into directories when inputs are dirs");
      System.out.println("  -g, --game <value>         Select game: k1, k2, tsl, 1, or 2 (default: k1)");
      System.out.println("      --k1 | --k2 | --tsl    Select game (default k1). Looks for k1_nwscript.nss or");
      System.out.println("                            tsl_nwscript.nss in current directory. Use --nwscript");
      System.out.println("                            to specify a different location. If no game flag is");
      System.out.println("                            set and --nwscript is not provided, defaults to");
      System.out.println("                            nwscript.nss in current directory.");
      System.out.println("      --quiet                Suppress success logs");
      System.out.println("      --fail-fast            Stop on first decompile failure");
      System.out.println("      --prefer-switches      Prefer generating switch structures instead");
      System.out.println("                            of if-elseif chains when possible");
      System.out.println("      --strict-signatures    Fail if any subroutine signature remains unknown");
      System.out.println();
      System.out.println("Examples:");
      
      // Format examples based on whether it's a JAR or EXE
      if (executableName.endsWith(".jar")) {
         System.out.println("  Decompile single file to stdout:");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i foo.ncs --stdout");
         System.out.println("  Decompile single file to specific output:");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i foo.ncs -o bar.nss");
         System.out.println("  Decompile directory recursively preserving hierarchy:");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i scripts_dir -r -o out_dir");
         System.out.println("  Decompile directory recursively to out folder using TSL definitions:");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i scripts_dir -r -g k2 -O out_dir");
         System.out.println("  Decompile with game selection using -g flag:");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i file.ncs -g k1");
         System.out.println("    java -cp " + executableName + " com.kotor.resource.formats.ncs.NCSDecompCLI -i file.ncs -g 2");
      } else {
         System.out.println("  Decompile single file to stdout:");
         System.out.println("    " + executableName + " -i foo.ncs --stdout");
         System.out.println("  Decompile single file to specific output:");
         System.out.println("    " + executableName + " -i foo.ncs -o bar.nss");
         System.out.println("  Decompile directory recursively preserving hierarchy:");
         System.out.println("    " + executableName + " -i scripts_dir -r -o out_dir");
         System.out.println("  Decompile directory recursively to out folder using TSL definitions:");
         System.out.println("    " + executableName + " -i scripts_dir -r -g k2 -O out_dir");
         System.out.println("  Decompile with game selection using -g flag:");
         System.out.println("    " + executableName + " -i file.ncs -g k1");
         System.out.println("    " + executableName + " -i file.ncs -g 2");
      }
   }

   private static void printVersion() {
      System.out.println("NCSDecomp CLI headless decompiler (Beta 2, May 30 2006)");
      System.out.println("Modified by th3w1zard1 | https://bolabaden.org | https://github.com/bolabaden");
   }

   /**
    * Represents an input file with its base directory for hierarchy preservation.
    */
   private static final class InputFile {
      final File file;
      final File baseDir;  // The base directory this file was collected from
      
      InputFile(File file, File baseDir) {
         this.file = file;
         this.baseDir = baseDir;
      }
   }

   private static final class CliConfig {
      final List<String> inputs = new ArrayList<>();
      String output;  // Can be a file or directory
      String outDir;
      String prefix = "";
      String suffix = "";
      String extension = ".nss";
      // Default to Windows-1252 (standard for KotOR/TSL), fallback to UTF-8 if unavailable
      Charset encoding;
      {
         try {
            encoding = Charset.forName("Windows-1252");
         } catch (Exception e) {
            encoding = StandardCharsets.UTF_8;
         }
      }
      boolean stdout = false;
      boolean overwrite = false;
      boolean recursive = false;
      boolean help = false;
      boolean version = false;
      boolean quiet = false;
      boolean failFast = false;
      boolean isK2 = false;
      boolean gameExplicitlySet = false;  // Track if user explicitly set a game flag
      boolean preferSwitches = false;  // Prefer switch structures over if-elseif chains
      boolean strictSignatures = false;  // Abort if signatures stay partially inferred
      String nwscriptPath = null;  // Explicit nwscript file path (CLI-only)
   }
}


