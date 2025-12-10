<!-- Copyright 2021-2025 NCSDecomp -->
<!-- Licensed under the Business Source License 1.1 (BSL 1.1). -->
<!-- See LICENSE.txt file in the project root for full license information. -->

# NCSDecomp - KotOR Script Decompiler

## Welcome

**NCSDecomp** is a tool that converts compiled KotOR game scripts (`.ncs` files) back into readable source code (`.nss` files). This version includes both a simple-to-use Windows program and command-line tools.

---

## 🚀 Quick Start Guide

### Option 1: Using the Self-Contained Executable (Easiest!)

1. **Download** the `NCSDecompCLI` folder (for command-line) or `NCSDecomp` folder (for GUI)
2. **For CLI:**
   - **Windows:** Navigate to the `NCSDecompCLI` folder and run `NCSDecompCLI.exe` from Command Prompt
   - **macOS/Linux:** Navigate to the `NCSDecompCLI` folder and run `./NCSDecompCLI` from Terminal
3. **For GUI:**
   - **Windows:** Navigate to the `NCSDecomp` folder and double-click `NCSDecomp.exe`
   - **macOS:** Navigate to the `NCSDecomp` folder and double-click `NCSDecomp.app`
   - **Linux:** Navigate to the `NCSDecomp` folder and run `./NCSDecomp`

That's it! No Java installation needed - everything is included in the folders.

### Option 2: Using the JAR File

If you prefer the JAR version, you'll need Java installed on your computer. Then you can run:

```bash
java -jar NCSDecomp-CLI.jar [options]
```

---

## 📖 How to Use

### Opening Terminal/Command Prompt

**Windows:**
1. Press `Windows Key + R`
2. Type `cmd` and press Enter
3. Navigate to the folder where NCSDecomp is located:

   ```powershell
   cd C:\path\to\NCSDecomp
   ```

**macOS/Linux:**
1. Open Terminal
2. Navigate to the folder where NCSDecomp is located:

   ```bash
   cd /path/to/NCSDecomp
   ```

### Basic Examples

**Note:** You can run the executable from any directory. If you're in the `NCSDecompCLI` folder, use `NCSDecompCLI.exe` (Windows) or `./NCSDecompCLI` (macOS/Linux). If you're in the parent directory, use `NCSDecompCLI\NCSDecompCLI.exe` (Windows) or `./NCSDecompCLI/NCSDecompCLI` (macOS/Linux).

#### Decompile a Single File (KotOR 2 / TSL)

**Windows:**
```powershell
# From within NCSDecompCLI folder:
.\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2

# Or from parent directory:
.\NCSDecompCLI\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2
```

**macOS/Linux:**
```bash
# From within NCSDecompCLI folder:
./NCSDecompCLI -i "script.ncs" -o "script.nss" --k2

# Or from parent directory:
./NCSDecompCLI/NCSDecompCLI -i "script.ncs" -o "script.nss" --k2
```

This will:

- Read `script.ncs`
- Create `script.nss` with the decompiled code
- Use KotOR 2 definitions (TSL)

#### Decompile a Single File (KotOR 1)

**Windows:**
```powershell
.\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k1
```

**macOS/Linux:**
```bash
./NCSDecompCLI -i "script.ncs" -o "script.nss" --k1
```

#### Decompile an Entire Folder

**Windows:**
```powershell
.\NCSDecompCLI.exe -i "scripts_folder" -r --k2 -O "output_folder"
```

**macOS/Linux:**
```bash
./NCSDecompCLI -i "scripts_folder" -r --k2 -O "output_folder"
```

This will:

- Process all `.ncs` files in `scripts_folder`
- Include all subfolders (`-r` means recursive)
- Save results to `output_folder`
- Use KotOR 2 definitions (`--k2`)

#### View Decompiled Code in Console

**Windows:**
```powershell
.\NCSDecompCLI.exe -i "script.ncs" --stdout --k2
```

**macOS/Linux:**
```bash
./NCSDecompCLI -i "script.ncs" --stdout --k2
```

This displays the code directly in the terminal/command window instead of saving to a file.

---

## 🎮 Game Mode Selection

NCSDecomp needs to know which game you're working with:

- **`--k1`** or **`--game=k1`** - For Knights of the Old Republic (KotOR 1)
- **`--k2`** or **`--tsl`** or **`--game=k2`** - For Knights of the Old Republic II: The Sith Lords (TSL)

If you don't specify, it defaults to KotOR 1 mode.

---

## 📁 Required Files

For the **CLI version** (`NCSDecompCLI.exe`), the required files are automatically included:

- `k1_nwscript.nss` - Required for KotOR 1 scripts (automatically in `app` folder)
- `tsl_nwscript.nss` - Required for KotOR 2/TSL scripts (automatically in `app` folder)

**Note:** The self-contained `NCSDecompCLI.exe` includes these files automatically in the `app` subdirectory. The executable will find them automatically - no manual setup needed!

If using the JAR version, you need to have them in your current working directory.

---

## 🔧 Common Options

| Option | Description | Example |
|--------|-------------|---------|
| `-i`, `--input` | Input file or folder | `-i "script.ncs"` |
| `-o`, `--output` | Output file name | `-o "output.nss"` |
| `-O`, `--out-dir` | Output folder | `-O "results"` |
| `-r`, `--recursive` | Process subfolders | `-r` |
| `--k1` | Use KotOR 1 mode | `--k1` |
| `--k2`, `--tsl` | Use KotOR 2/TSL mode | `--k2` |
| `--stdout` | Show output in console | `--stdout` |
| `--overwrite` | Overwrite existing files | `--overwrite` |
| `--quiet` | Less verbose output | `--quiet` |
| `--help` | Show help message | `--help` |

---

## 📝 More Examples

### Example 1: Batch Decompile

**Windows:** Create a text file named `decompile.bat` with this content:

```batch
@echo off
cd /d "%~dp0NCSDecompCLI"
NCSDecompCLI.exe -i "C:\KotOR\scripts" -r --k2 -O "C:\KotOR\decompiled"
pause
```

**macOS/Linux:** Create a shell script named `decompile.sh` with this content:

```bash
#!/bin/bash
cd "$(dirname "$0")/NCSDecompCLI"
./NCSDecompCLI -i "/path/to/scripts" -r --k2 -O "/path/to/decompiled"
```

Make it executable: `chmod +x decompile.sh`

This script changes to the NCSDecompCLI directory first, then runs the executable.

### Example 2: Process Multiple Files

**Windows:**
```powershell
.\NCSDecompCLI.exe -i file1.ncs -i file2.ncs -i file3.ncs --k2 -O output
```

**macOS/Linux:**
```bash
./NCSDecompCLI -i file1.ncs -i file2.ncs -i file3.ncs --k2 -O output
```

### Example 3: Add Custom Suffix

**Windows:**
```powershell
.\NCSDecompCLI.exe -i script.ncs --suffix "_decompiled" --k2
```

**macOS/Linux:**
```bash
./NCSDecompCLI -i script.ncs --suffix "_decompiled" --k2
```

This creates `script_decompiled.nss` instead of `script.nss`.

---

## ❓ Troubleshooting

### "Error: nwscript file not found"

**Problem**: Missing `nwscript.nss` file

**Solution**:

- For `NCSDecompCLI.exe`: The files should be automatically in the `app` subdirectory. If you get this error, ensure the executable folder structure is intact and the `app` folder contains the nwscript files.
- For JAR version: Make sure `k1_nwscript.nss` or `tsl_nwscript.nss` is in your current working directory.
- You can also use `--nwscript <path>` to specify the exact location of the nwscript file.

### "No .ncs files found"

**Problem**: No `.ncs` files in the specified location

**Solution**: Check that your input path is correct and contains `.ncs` files

### Program won't start

**Windows:** Windows security might be blocking it. Right-click `NCSDecomp.exe` → Properties → Check "Unblock" → Apply

**macOS:** You may need to allow the app in System Preferences → Security & Privacy

**Linux:** Ensure the executable has execute permissions: `chmod +x NCSDecompCLI/NCSDecompCLI`

---

## 📚 Getting Help

**Windows:**
- Run `.\NCSDecompCLI.exe --help` for a full list of CLI options (from within the NCSDecompCLI folder)
- Run `.\NCSDecompCLI.exe --version` for version information

**macOS/Linux:**
- Run `./NCSDecompCLI --help` for a full list of CLI options (from within the NCSDecompCLI folder)
- Run `./NCSDecompCLI --version` for version information

- Visit [https://bolabaden.org](https://bolabaden.org) for more resources

---

## 🎯 Advanced Usage

For detailed technical documentation, see `README-TECHNICAL.md` included in this package.

---

## 🙏 Credits

**Original Developers:**

- JdNoa - Script Decompiler
- Dashus - GUI

**Current Maintainer:**

- th3w1zard1

**Website:** [https://bolabaden.org](https://bolabaden.org)

**Source Code:** [https://github.com/bolabaden](https://github.com/bolabaden)

---

## 📄 License

This software is provided "as is" with no warranty. See the original NCSDecomp documentation for license details.

---

**Enjoy decompiling!** 🎮✨
