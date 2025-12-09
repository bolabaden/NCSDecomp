<!-- Copyright 2021-2025 NCSDecomp -->
<!-- Licensed under the Business Source License 1.1 (BSL 1.1). -->
<!-- See LICENSE.txt file in the project root for full license information. -->

# NCSDecomp CLI - Command-Line Interface Guide

## Overview

NCSDecomp CLI is a headless command-line decompiler for KotOR game scripts. It converts compiled NCS bytecode back into readable NSS source code without requiring external compilers.

**Website:** [https://bolabaden.org](https://bolabaden.org)  
**Source:** [https://github.com/bolabaden](https://github.com/bolabaden)

## Requirements

### For Self-Contained Executable (.exe)

- Windows 10 or later
- No Java installation needed (Java runtime is included)

### For JAR Version

- Java Runtime Environment (JRE) 8 or later
- `k1_nwscript.nss` or `tsl_nwscript.nss` file in the working directory (depending on game mode)

## Usage

### Using the Self-Contained Executable

The easiest way to use NCSDecomp CLI is with the self-contained `.exe` file:

```powershell
# Show help
.\NCSDecompCLI\NCSDecompCLI.exe --help

# Show version
.\NCSDecompCLI\NCSDecompCLI.exe --version

# Decompile single file to stdout
.\NCSDecompCLI\NCSDecompCLI.exe -i input.ncs --stdout --k2

# Decompile single file to output file
.\NCSDecompCLI\NCSDecompCLI.exe -i input.ncs -o output.nss --k2

# Decompile directory recursively (KotOR 1)
.\NCSDecompCLI\NCSDecompCLI.exe -i scripts_dir -r --k1 -O output_dir

# Decompile directory recursively (KotOR 2 / TSL)
.\NCSDecompCLI\NCSDecompCLI.exe -i scripts_dir -r --k2 -O output_dir

# Decompile with custom suffix
.\NCSDecompCLI\NCSDecompCLI.exe -i input.ncs --suffix "_decompiled" --k2
```

**Note:** The executable must be run from within the `NCSDecompCLI` folder, or use the full path to the executable.

### Using the JAR File

If you're using the JAR version:

```bash
# Show help
java -jar NCSDecomp-CLI.jar --help

# Show version
java -jar NCSDecomp-CLI.jar --version

# Decompile single file to stdout
java -jar NCSDecomp-CLI.jar -i input.ncs --stdout --k2

# Decompile single file to output file
java -jar NCSDecomp-CLI.jar -i input.ncs -o output.nss --k2

# Decompile directory recursively (KotOR 1)
java -jar NCSDecomp-CLI.jar -i scripts_dir -r --k1 -O output_dir

# Decompile directory recursively (KotOR 2 / TSL)
java -jar NCSDecomp-CLI.jar -i scripts_dir -r --k2 -O output_dir

# Decompile with custom suffix
java -jar NCSDecomp-CLI.jar -i input.ncs --suffix "_decompiled" --k2
```

## Command-Line Options

| Option | Description |
|-------|-------------|
| `-h, --help` | Show help message |
| `-v, --version` | Show version information |
| `-i, --input <path>` | Input .ncs file or directory (can repeat or pass positional) |
| `-o, --output <file>` | Output .nss file (only when a single input is provided) |
| `-O, --out-dir <dir>` | Output directory (defaults to input directory) |
| `--prefix <text>` | Prefix for generated filenames |
| `--suffix <text>` | Suffix for generated filenames |
| `--ext <ext>` | Output extension (default: .nss) |
| `--encoding <name>` | Output charset (default: UTF-8) |
| `--nwscript <path>` | Path to nwscript.nss file (overrides --k1/--tsl) |
| `--stdout` | Write decompiled source to stdout |
| `--overwrite` | Overwrite existing files |
| `-r, --recursive` | Recurse into directories when inputs are dirs |
| `--k1` | Select KotOR 1 mode (default) |
| `--k2, --tsl` | Select KotOR 2 / TSL mode |
| `--quiet` | Suppress success logs |
| `--fail-fast` | Stop on first decompile failure |

## Game Mode Selection

NCSDecomp needs to know which game you're working with:

- **`--k1`** or **`--game=k1`** - For Knights of the Old Republic (KotOR 1)
- **`--k2`** or **`--tsl`** or **`--game=k2`** - For Knights of the Old Republic II: The Sith Lords (TSL)

If you don't specify, it defaults to KotOR 1 mode.

## Required Files

### Self-Contained Executable (.exe)

The `nwscript.nss` files are automatically included in the `app` subdirectory. The executable will automatically find them - no manual setup needed!

### JAR Version

You need to have either `k1_nwscript.nss` or `tsl_nwscript.nss` in your current working directory when you run the JAR. You can also use the `--nwscript <path>` option to specify the exact location.

## Examples

### Example 1: Decompile Single File

```powershell
.\NCSDecompCLI\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2
```

### Example 2: Decompile Entire Folder

```powershell
.\NCSDecompCLI\NCSDecompCLI.exe -i "C:\KotOR\scripts" -r --k2 -O "C:\KotOR\decompiled"
```

### Example 3: View Decompiled Code in Console

```powershell
.\NCSDecompCLI\NCSDecompCLI.exe -i "script.ncs" --stdout --k2
```

### Example 4: Process Multiple Files

```powershell
.\NCSDecompCLI\NCSDecompCLI.exe -i file1.ncs -i file2.ncs -i file3.ncs --k2 -O output
```

### Example 5: Add Custom Suffix

```powershell
.\NCSDecompCLI\NCSDecompCLI.exe -i script.ncs --suffix "_decompiled" --k2
```

This creates `script_decompiled.nss` instead of `script.nss`.

## Troubleshooting

### "Error: nwscript file not found"

**For .exe version:** The nwscript files should be automatically in the `app` subdirectory. If you get this error, ensure the executable folder structure is intact.

**For JAR version:** Make sure `k1_nwscript.nss` or `tsl_nwscript.nss` is in your current working directory, or use `--nwscript <path>` to specify the exact location.

### "No .ncs files found"

Check that your input path is correct and contains `.ncs` files.

### Executable won't start

Windows security might be blocking it. Right-click the executable → Properties → Check "Unblock" → Apply.

## Getting Help

- Run `.\NCSDecompCLI\NCSDecompCLI.exe --help` for a full list of options
- Run `.\NCSDecompCLI\NCSDecompCLI.exe --version` for version information
- Visit [https://bolabaden.org](https://bolabaden.org) for more resources

## Credits

**Original Developers:**

- JdNoa - Script Decompiler
- Dashus - GUI

**Current Maintainer:**

- th3w1zard1

**Website:** [https://bolabaden.org](https://bolabaden.org)  
**Source Code:** [https://github.com/bolabaden](https://github.com/bolabaden)
