NCSDecomp - KotOR Script Decompiler

Copyright 2021-2025 NCSDecomp
Licensed under the Business Source License 1.1 (BSL 1.1).
See LICENSE.txt file in the project root for full license information.

---------------
About
---------------
NCSDecomp is a KotOR script decompiler based on the original DeNCS project.

Original Authors:
	JdNoa - Script Decompiler - jdnoa@hotmail.com
	Dashus - GUI - humanitas@gmail.com

This project is loosely based on their work. The original DeNCS was an internal tool
used by TSLRP (The Sith Lords Restoration Project) and was released to the public
"as is" with no warranty.

---------------
What's New
---------------
This version includes:
    - Near complete rewrite for modern java projects
	- Self-contained Windows executables (no Java installation required)
	- Command-line interface (CLI) for headless operation
	- Support for both KotOR 1 and KotOR 2/TSL
	- Improved nwscript.nss file discovery
	- Modern build system using jpackage

---------------
Requirements
---------------
For Self-Contained Executable (.exe):
	- Windows 10 or later
	- No Java installation needed (Java runtime is included)

For JAR Version:
	- Java Runtime Environment (JRE) 8 or later
	- k1_nwscript.nss or tsl_nwscript.nss file (included in distribution)

---------------
Quick Start
---------------
Option 1: Using the Windows Executable (Recommended)
	1. Navigate to the NCSDecompCLI folder
	2. Run NCSDecompCLI.exe from Command Prompt
	3. See examples below

Option 2: Using the JAR File
	java -jar NCSDecomp-CLI.jar [options]

---------------
Usage Examples
---------------
Show help:
	.\NCSDecompCLI.exe --help

Decompile single file (KotOR 2/TSL):
	.\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2

Decompile entire folder recursively:
	.\NCSDecompCLI.exe -i "scripts_folder" -r --k2 -O "output_folder"

View decompiled code in console:
	.\NCSDecompCLI.exe -i "script.ncs" --stdout --k2

For more detailed usage information, see README-CLI.md or README-USER.md in the
distribution package.

---------------
Game Support
---------------
NCSDecomp supports both:
	- Knights of the Old Republic (KotOR 1) - use --k1 flag
	- Knights of the Old Republic II: The Sith Lords (TSL) - use --k2 or --tsl flag

---------------
Credits
---------------
Original DeNCS Developers:
	- JdNoa - Script Decompiler
	- Dashus - GUI

Current Maintainer:
	- th3w1zard1

Website: https://bolabaden.org
Source Code: https://github.com/bolabaden

---------------
License
---------------
This software is provided under the Business Source License 1.1 (BSL 1.1).
See LICENSE.txt file in the project root for full license information.

---------------
Acknowledgments
---------------
Thanks to the original DeNCS authors for their foundational work.

Special thanks to:
	- Torlack for documenting the NCS file format
	- Étienne Gagnon for developing SableCC (used in the original lexer/parser)
	- Fred Tetra for the K1/2 version of nwnnsscomp
