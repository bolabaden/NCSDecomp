# NCSDecomp

KotOR Script Decompiler - A decompiler for Knights of the Old Republic NCS script files.

## Project Structure

This project follows standard Maven directory layout conventions:

```
.
├── pom.xml                          # Maven project configuration
├── src/
│   ├── main/
│   │   ├── java/                    # Main Java source code
│   │   │   └── com/
│   │   │       └── kotor/
│   │   │           └── resource/
│   │   │               └── formats/
│   │   │                   └── ncs/
│   │   └── resources/               # Main resources (config files, data files)
│   │       ├── com/kotor/resource/formats/ncs/
│   │       │   ├── lexer/lexer.dat
│   │       │   └── parser/parser.dat
│   │       ├── k1_nwscript.nss
│   │       ├── tsl_nwscript.nss
│   │       └── nwscript.sablecc
│   └── test/
│       ├── java/                    # Test Java source code
│       │   └── com/kotor/resource/formats/ncs/
│       └── resources/               # Test resources
├── scripts/                         # Build and utility scripts
├── docs/                            # Documentation files
└── lib/                             # External JAR dependencies (legacy, now managed by Maven)
```

## Building

### Prerequisites

- Java Development Kit (JDK) 8 or later
- Apache Maven 3.6.0 or later (optional, for Maven builds)
- PowerShell (for Windows build scripts)

### Build Options

#### Option 1: Using PowerShell Scripts (Recommended for Windows)

```powershell
# Build JAR file
.\scripts\build.ps1

# Build self-contained .exe (requires JDK 14+ with jpackage)
.\scripts\build-exe.ps1

# Build and publish distribution package
.\scripts\publish.ps1
```

The PowerShell build script will:
1. Compile all Java source files from `src/main/java`
2. Copy resources from `src/main/resources` to the build directory
3. Create a JAR file named `NCSDecomp-CLI.jar` in the project root

#### Option 2: Using Maven

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Package the application (creates JAR file)
mvn package

# Clean build artifacts
mvn clean

# Full clean and build
mvn clean package
```

The Maven build process will:

1. Compile all Java source files from `src/main/java`
2. Copy resources from `src/main/resources` to the output directory
3. Run all tests from `src/test/java`
4. Create a JAR file in the `target/` directory
5. Create a fat JAR (with dependencies) named `ncsdecomp-CLI-1.0.0-SNAPSHOT.jar`

## Running

### Using Maven

```bash
# Run the CLI application
mvn exec:java -Dexec.mainClass="com.kotor.resource.formats.ncs.NCSDecompCLI" -Dexec.args="[arguments]"
```

### Using the JAR file

```bash
# After building with PowerShell script
java -jar NCSDecomp-CLI.jar [arguments]

# Or after building with Maven
java -jar target/ncsdecomp-CLI-1.0.0-SNAPSHOT.jar [arguments]
```

**Note:** When using the JAR file, ensure `k1_nwscript.nss` or `tsl_nwscript.nss` is in your current working directory, or use the `--nwscript <path>` option to specify the location.

## Usage

See `docs/README-CLI.md` and `docs/README-USER.md` for detailed usage instructions.

## Development

### Code Organization

- **Source Code**: All production Java code is in `src/main/java/`
- **Test Code**: All test Java code is in `src/test/java/`
- **Resources**: Configuration files, data files, and other resources are in `src/main/resources/`
- **Test Resources**: Test-specific resources are in `src/test/resources/`

### Package Structure

The main package is `com.kotor.resource.formats.ncs`, which contains:

- Core decompiler logic
- Lexer and parser components
- Script node representations
- Stack and variable management
- Utility classes

### Adding Dependencies

Dependencies are managed through Maven. Add new dependencies to `pom.xml` in the `<dependencies>` section.

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=KNCSDecompCLIRoundTripTest
```

## License

Copyright 2021-2025 NCSDecomp
Licensed under the Business Source License 1.1 (BSL 1.1).
See LICENSE.txt file in the project root for full license information.

## Credits

Original DeNCS Developers:

- JdNoa - Script Decompiler
- Dashus - GUI

Current Maintainer:

- th3w1zard1

Website: <https://bolabaden.org>
Source Code: <https://github.com/bolabaden>
