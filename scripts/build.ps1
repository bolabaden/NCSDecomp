# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI
# Compiles all Java files and creates a self-contained JAR
# Optionally creates a self-contained executable using jpackage
# Cross-platform compatible (Windows, macOS, Linux)

param(
    [switch]$BuildExe = $false,
    [switch]$Help = $false
)

$ErrorActionPreference = "Stop"

# Show help if requested
if ($Help) {
    Write-Host "NCSDecomp Build Script" -ForegroundColor Green
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Cyan
    Write-Host "  .\scripts\build.ps1              # Build JAR file only"
    Write-Host "  .\scripts\build.ps1 -BuildExe    # Build JAR and executable"
    Write-Host "  .\scripts\build.ps1 -Help         # Show this help"
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Cyan
    Write-Host "  -BuildExe    Build self-contained executable (requires JDK 14+ with jpackage)"
    Write-Host "  -Help        Show this help message"
    exit 0
}

# Detect platform (PowerShell Core 6+ has automatic variables, fallback for older versions)
if ($PSVersionTable.PSVersion.Major -ge 6) {
    # Use automatic variables in PowerShell Core 6+
    # $IsWindows, $IsLinux, $IsMacOS are automatically available
} else {
    # Fallback for Windows PowerShell 5.1
    if (-not (Test-Path variable:IsWindows)) { $script:IsWindows = $env:OS -eq "Windows_NT" }
    if (-not (Test-Path variable:IsLinux)) { $script:IsLinux = $false }
    if (-not (Test-Path variable:IsMacOS)) { $script:IsMacOS = $false }
}

$platformName = if ($IsWindows) { "Windows" } elseif ($IsMacOS) { "macOS" } elseif ($IsLinux) { "Linux" } else { "Unknown" }
Write-Host "Building NCSDecomp CLI..." -ForegroundColor Green
Write-Host "Platform: $platformName" -ForegroundColor Gray
if ($BuildExe) {
    Write-Host "Mode: JAR + Executable" -ForegroundColor Cyan
} else {
    Write-Host "Mode: JAR only" -ForegroundColor Cyan
}
Write-Host ""

# Function to clean up .class files
function Remove-ClassFiles {
    param(
        [string]$Path
    )
    
    Write-Host "Cleaning .class files in $Path..." -ForegroundColor Yellow
    $classFiles = Get-ChildItem -Path $Path -Recurse -Filter "*.class" -ErrorAction SilentlyContinue
    if ($classFiles.Count -gt 0) {
        $classFiles | Remove-Item -Force -ErrorAction SilentlyContinue
        Write-Host "Removed $($classFiles.Count) .class file(s)" -ForegroundColor Gray
    }
}

# Clean build directory, JAR file, and any stray .class files for idempotent builds
$buildDir = Join-Path "." "build"
$jarFile = Join-Path "." "NCSDecomp-CLI.jar"
$javaSourceDir = Join-Path "." (Join-Path "src" (Join-Path "main" "java"))

Write-Host "Cleaning previous build artifacts..." -ForegroundColor Yellow

# Clean up .class files in source directory (stray files)
if (Test-Path $javaSourceDir) {
    Remove-ClassFiles -Path $javaSourceDir
}

# Clean build directory
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
}

# Clean JAR file
if (Test-Path $jarFile) {
    Remove-Item -Force $jarFile -ErrorAction SilentlyContinue
}

# Clean executable output directory if building executable
if ($BuildExe) {
    $exeOutputDir = Join-Path "." "dist-exe"
    if (Test-Path $exeOutputDir) {
        Write-Host "Cleaning previous executable build..." -ForegroundColor Yellow
        try {
            # Only try to stop processes on Windows (Unix systems handle this differently)
            if ($IsWindows) {
                Get-Process | Where-Object { $_.Path -like "*$exeOutputDir*" } | Stop-Process -Force -ErrorAction SilentlyContinue
                Start-Sleep -Milliseconds 500
            }
            Remove-Item -Recurse -Force $exeOutputDir -ErrorAction Stop
        } catch {
            Write-Host "Warning: Could not delete $exeOutputDir (files may be locked)" -ForegroundColor Yellow
            Write-Host "Please close any running NCSDecomp processes and try again." -ForegroundColor Yellow
            Write-Host "Or manually delete the dist-exe folder." -ForegroundColor Yellow
            exit 1
        }
    }
}

New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

# Collect all Java files from Maven source directory
Write-Host "Collecting Java source files..." -ForegroundColor Yellow
if (-not (Test-Path $javaSourceDir)) {
    Write-Host "Error: Source directory not found: $javaSourceDir" -ForegroundColor Red
    exit 1
}

$javaFiles = Get-ChildItem -Recurse -Filter "*.java" -Path $javaSourceDir | ForEach-Object { $_.FullName }
$javaFiles = $javaFiles | Where-Object { $_ -notlike "*Test.java" }  # Exclude test files

if ($javaFiles.Count -eq 0) {
    Write-Host "Error: No Java files found in $javaSourceDir!" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($javaFiles.Count) Java files to compile" -ForegroundColor Cyan

# Copy resources to build directory
Write-Host "Copying resources..." -ForegroundColor Yellow
$resourcesDir = Join-Path "." (Join-Path "src" (Join-Path "main" "resources"))
if (Test-Path $resourcesDir) {
    $resourceDest = $buildDir
    $resourcesFullPath = (Resolve-Path $resourcesDir).Path
    Get-ChildItem -Path $resourcesDir -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($resourcesFullPath.Length + 1)
        # Normalize path separators for cross-platform compatibility
        if ($IsWindows) {
            $relativePath = $relativePath -replace '/', '\'
        } else {
            $relativePath = $relativePath -replace '\\', '/'
        }
        $destPath = Join-Path $resourceDest $relativePath
        $destDir = Split-Path $destPath -Parent
        if ($destDir -and -not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        Copy-Item $_.FullName $destPath -Force
    }
    Write-Host "Resources copied successfully" -ForegroundColor Gray
} else {
    Write-Host "Warning: Resources directory not found: $resourcesDir" -ForegroundColor Yellow
}

# Compile Java files
Write-Host "Compiling Java files..." -ForegroundColor Yellow

# Build javac arguments array for proper handling
$javacArgs = @(
    "-d", $buildDir,
    "-encoding", "UTF-8",
    "-sourcepath", $javaSourceDir
) + $javaFiles

try {
    # Capture both stdout and stderr properly
    $compileOutput = & javac $javacArgs 2>&1
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "COMPILATION ERRORS:" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host ""

        # Display all error output
        if ($compileOutput) {
            $compileOutput | ForEach-Object {
                if ($_ -is [System.Management.Automation.ErrorRecord]) {
                    Write-Host $_.Exception.Message -ForegroundColor Red
                    if ($_.ScriptStackTrace) {
                        Write-Host $_.ScriptStackTrace -ForegroundColor DarkRed
                    }
                } else {
                    Write-Host $_ -ForegroundColor Red
                }
            }
        } else {
            Write-Host "No error details captured. Exit code: $exitCode" -ForegroundColor Yellow
        }

        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host ""
        throw "Compilation failed with exit code $exitCode"
    }
    Write-Host "Compilation successful!" -ForegroundColor Green

    # Verify main class was compiled
    $mainClassFile = Join-Path $buildDir (Join-Path "com" (Join-Path "kotor" (Join-Path "resource" (Join-Path "formats" (Join-Path "ncs" "NCSDecompCLI.class")))))
    if (-not (Test-Path $mainClassFile)) {
        Write-Host "Error: Main class not found after compilation: $mainClassFile" -ForegroundColor Red
        Write-Host "Checking for compiled classes..." -ForegroundColor Yellow
        $classFiles = Get-ChildItem -Path $buildDir -Recurse -Filter "*.class"
        if ($classFiles.Count -eq 0) {
            Write-Host "No .class files found in build directory!" -ForegroundColor Red
        } else {
            Write-Host "Found $($classFiles.Count) class files, but main class is missing." -ForegroundColor Yellow
            $classFiles | Select-Object -First 5 | ForEach-Object { Write-Host "  $($_.FullName)" -ForegroundColor Gray }
        }
        exit 1
    }
    Write-Host "Verified main class compiled: NCSDecompCLI.class" -ForegroundColor Gray
} catch {
    Write-Host "Compilation failed: $_" -ForegroundColor Red
    exit 1
}

# Create manifest for CLI
Write-Host "Creating manifest..." -ForegroundColor Yellow
$manifestDir = Join-Path $buildDir "META-INF"
New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null
$manifestFile = Join-Path $manifestDir "MANIFEST.MF"
$manifestContent = @"
Manifest-Version: 1.0
Created-By: NCSDecomp Build Script
Main-Class: com.kotor.resource.formats.ncs.NCSDecompCLI

"@
if ($IsWindows) {
    $manifestContent | Out-File -FilePath $manifestFile -Encoding ASCII
} else {
    $manifestContent | Out-File -FilePath $manifestFile -Encoding utf8NoBOM
}

# Create JAR file
Write-Host "Creating JAR file..." -ForegroundColor Yellow

Push-Location $buildDir
try {
    # Create JAR with all compiled classes, resources, and META-INF
    # This includes everything in the build directory with proper structure
    $manifestPath = Join-Path "META-INF" "MANIFEST.MF"
    $parentJar = if ($IsWindows) { "..\NCSDecomp-CLI.jar" } else { "../NCSDecomp-CLI.jar" }

    # Ensure we're creating a fresh JAR (remove if exists)
    if (Test-Path $parentJar) {
        Remove-Item -Force $parentJar
    }

    # Create JAR with explicit manifest
    jar cfm $parentJar $manifestPath com META-INF
    if ($LASTEXITCODE -ne 0) {
        throw "JAR creation failed"
    }

    # Add resources (nwscript files, etc.) to JAR root if they exist
    $resourceFiles = Get-ChildItem -Path "." -Filter "*.nss" -File -ErrorAction SilentlyContinue
    if ($resourceFiles.Count -gt 0) {
        jar uf $parentJar *.nss
        Write-Host "Added $($resourceFiles.Count) resource file(s) to JAR" -ForegroundColor Gray
    }

    # Verify JAR contents
    Write-Host "Verifying JAR contents..." -ForegroundColor Gray
    $jarList = jar tf $parentJar
    $mainClassInJar = $jarList | Where-Object { $_ -like "com/kotor/resource/formats/ncs/NCSDecompCLI.class" }
    if (-not $mainClassInJar) {
        throw "Main class not found in JAR: com.kotor.resource.formats.ncs.NCSDecompCLI"
    }

    # Verify manifest
    jar xf $parentJar META-INF/MANIFEST.MF
    $manifestPathCheck = if ($IsWindows) { "META-INF\MANIFEST.MF" } else { "META-INF/MANIFEST.MF" }
    if (Test-Path $manifestPathCheck) {
        $manifestContent = Get-Content $manifestPathCheck -Raw
        if ($manifestContent -notmatch "Main-Class: com\.kotor\.resource\.formats\.ncs\.NCSDecompCLI") {
            Write-Host "WARNING: Manifest may have incorrect main class!" -ForegroundColor Yellow
            Write-Host "Manifest content:" -ForegroundColor Yellow
            Get-Content $manifestPathCheck | Write-Host
        } else {
            Write-Host "Verified manifest has correct main class" -ForegroundColor Gray
        }
        Remove-Item -Recurse -Force "META-INF" -ErrorAction SilentlyContinue
    }

    Write-Host "JAR created successfully with all classes and resources" -ForegroundColor Gray
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "JAR build complete! Created NCSDecomp-CLI.jar" -ForegroundColor Green

# Build executable if requested
if ($BuildExe) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building self-contained executable..." -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""

    # Check prerequisites
    $jpackageExe = if ($IsWindows) { "jpackage.exe" } else { "jpackage" }
    $jpackagePath = Join-Path $env:JAVA_HOME (Join-Path "bin" $jpackageExe)
    if (-not (Test-Path $jpackagePath)) {
        Write-Host "Error: jpackage not found at $jpackagePath" -ForegroundColor Red
        Write-Host "Please ensure JAVA_HOME is set and points to JDK 14 or later" -ForegroundColor Yellow
        exit 1
    }

    # Verify JAR contains the correct main class
    Write-Host "Verifying JAR file..." -ForegroundColor Gray
    $jarList = jar tf $jarFile 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: JAR file appears to be corrupted or invalid" -ForegroundColor Red
        exit 1
    }

    $mainClassInJar = $jarList | Where-Object { $_ -like "com/kotor/resource/formats/ncs/NCSDecompCLI.class" }
    if (-not $mainClassInJar) {
        Write-Host "Error: JAR file does not contain main class com.kotor.resource.formats.ncs.NCSDecompCLI" -ForegroundColor Red
        exit 1
    }

    # Verify manifest has correct main class
    $tempBase = if ($env:TEMP) { $env:TEMP } elseif ($env:TMPDIR) { $env:TMPDIR } else { "/tmp" }
    $tempDir = Join-Path $tempBase "jpackage-verify-$(Get-Random)"
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    $currentLocation = Get-Location
    $jarFileAbsolute = (Resolve-Path $jarFile).Path
    try {
        Push-Location $tempDir
        jar xf $jarFileAbsolute META-INF/MANIFEST.MF 2>&1 | Out-Null
        $manifestPath = if ($IsWindows) { "META-INF\MANIFEST.MF" } else { "META-INF/MANIFEST.MF" }
        if (Test-Path $manifestPath) {
            $manifestContent = Get-Content $manifestPath -Raw
            if ($manifestContent -notmatch "Main-Class: com\.kotor\.resource\.formats\.ncs\.NCSDecompCLI") {
                Write-Host "Error: JAR manifest has incorrect main class!" -ForegroundColor Red
                Write-Host "Manifest content:" -ForegroundColor Yellow
                Get-Content $manifestPath | Write-Host
                exit 1
            } else {
                Write-Host "JAR verified: Contains correct main class and manifest" -ForegroundColor Gray
            }
        }
    } finally {
        if ((Get-Location).Path -ne $currentLocation.Path) {
            Pop-Location
        }
        Remove-Item -Recurse -Force $tempDir -ErrorAction SilentlyContinue
    }

    # For CLI tools, always use app-image (portable executable that runs directly)
    # This creates a folder with executable that can be run immediately without installation
    $packageType = "app-image"
    $exeOutputDir = Join-Path "." "dist-exe"

    Write-Host "Creating portable executable (runs directly, no installation needed)..." -ForegroundColor Cyan
    Write-Host "This may take several minutes (first time can take 5-10 minutes)..." -ForegroundColor Cyan
    Write-Host ""

    # Create temporary input directory for jpackage
    $jpackageInput = "jpackage-input"
    if (Test-Path $jpackageInput) {
        Remove-Item -Recurse -Force $jpackageInput
    }
    New-Item -ItemType Directory -Path $jpackageInput -Force | Out-Null

    # Copy JAR to input directory
    $jarDest = Join-Path $jpackageInput "NCSDecomp-CLI.jar"
    Copy-Item $jarFile $jarDest

    # Copy nwscript files as resources (they'll be in the app directory)
    # Try multiple locations: root, src/main/resources, and publish directory
    $nwscriptLocations = @(
        (Join-Path "." "k1_nwscript.nss"),
        (Join-Path "." "tsl_nwscript.nss"),
        (Join-Path "." (Join-Path "src" (Join-Path "main" (Join-Path "resources" "k1_nwscript.nss")))),
        (Join-Path "." (Join-Path "src" (Join-Path "main" (Join-Path "resources" "tsl_nwscript.nss")))),
        (Join-Path "." (Join-Path "publish" "k1_nwscript.nss")),
        (Join-Path "." (Join-Path "publish" "tsl_nwscript.nss"))
    )

    foreach ($nssFile in $nwscriptLocations) {
        if (Test-Path $nssFile) {
            $fileName = Split-Path $nssFile -Leaf
            $destFile = Join-Path $jpackageInput $fileName
            Copy-Item $nssFile $destFile -Force -ErrorAction SilentlyContinue
            Write-Host "Copied $fileName to jpackage input" -ForegroundColor Gray
        }
    }

    Write-Host "Building portable executable with jpackage..." -ForegroundColor Yellow
    Write-Host ""

    try {
        # Build CLI executable
        $cliAppName = "NCSDecompCLI"
        $cliMainClass = "com.kotor.resource.formats.ncs.NCSDecompCLI"
        $appVersion = "1.0.0"

        Write-Host "Building CLI executable ($cliAppName)..." -ForegroundColor Yellow

        $cliJpackageArgs = @(
            "--type", $packageType,
            "--input", $jpackageInput,
            "--name", $cliAppName,
            "--main-jar", "NCSDecomp-CLI.jar",
            "--main-class", $cliMainClass,
            "--app-version", $appVersion,
            "--description", "KotOR NCSDecomp Script Decompiler - CLI Version",
            "--vendor", "bolabaden.org",
            "--copyright", "Original: JdNoa, Dashus | Modified by: th3w1zard1",
            "--dest", $exeOutputDir,
            "--java-options", "-Xmx2G",
            "--java-options", "-Djava.awt.headless=true"
        )

        # Add Windows-specific options only on Windows
        if ($IsWindows) {
            $cliJpackageArgs += "--win-console"
        }

        & $jpackagePath $cliJpackageArgs
        if ($LASTEXITCODE -ne 0) {
            throw "jpackage CLI build failed with exit code $LASTEXITCODE"
        }

        # Build GUI executable (if NCSDecomp.jar exists)
        $guiAppName = "NCSDecomp"
        $guiMainClass = "com.kotor.resource.formats.ncs.Decompiler"
        $guiJarPath = "NCSDecomp.jar"

        if (Test-Path $guiJarPath) {
            Write-Host "Building GUI executable ($guiAppName)..." -ForegroundColor Yellow

            # Create GUI input directory
            $guiInputDir = "jpackage-input-gui"
            if (Test-Path $guiInputDir) {
                Remove-Item -Recurse -Force $guiInputDir
            }
            New-Item -ItemType Directory -Path $guiInputDir -Force | Out-Null
            $guiJarDest = if ($IsWindows) { "$guiInputDir\NCSDecomp.jar" } else { "$guiInputDir/NCSDecomp.jar" }
            Copy-Item $guiJarPath $guiJarDest

            $guiJpackageArgs = @(
                "--type", $packageType,
                "--input", $guiInputDir,
                "--name", $guiAppName,
                "--main-jar", "NCSDecomp.jar",
                "--main-class", $guiMainClass,
                "--app-version", $appVersion,
                "--description", "KotOR NCSDecomp Script Decompiler - GUI Version",
                "--vendor", "bolabaden.org",
                "--copyright", "Original: JdNoa, Dashus | Modified by: th3w1zard1",
                "--dest", $exeOutputDir,
                "--java-options", "-Xmx2G"
            )

            & $jpackagePath $guiJpackageArgs
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Warning: GUI executable build failed" -ForegroundColor Yellow
            } else {
                Write-Host "GUI executable built successfully!" -ForegroundColor Green
            }

            # Cleanup GUI input
            if (Test-Path $guiInputDir) {
                Remove-Item -Recurse -Force $guiInputDir
            }
        } else {
            Write-Host "Note: NCSDecomp.jar not found, skipping GUI executable build" -ForegroundColor Yellow
            Write-Host "      (Only CLI executable will be created)" -ForegroundColor Yellow
        }

        # Process CLI executable
        $cliAppImagePath = Join-Path $exeOutputDir $cliAppName
        if (-not (Test-Path $cliAppImagePath)) {
            throw "CLI app image not found after build: $cliAppImagePath"
        }

        # Executable name differs by platform
        $cliExeName = if ($IsWindows) { "$cliAppName.exe" } else { $cliAppName }
        $cliExePath = Join-Path $cliAppImagePath $cliExeName
        if (-not (Test-Path $cliExePath)) {
            throw "CLI executable not found in app image: $cliExePath"
        }

        # Copy nwscript files to CLI app directory
        $cliAppDir = Join-Path $cliAppImagePath "app"
        if (Test-Path $cliAppDir) {
            $nssFiles = @("k1_nwscript.nss", "tsl_nwscript.nss")
            foreach ($nssFile in $nssFiles) {
                # Try multiple source locations
                $sourcePaths = @(
                    (Join-Path "." $nssFile),
                    (Join-Path ".." $nssFile),
                    (Join-Path "." (Join-Path "src" (Join-Path "main" (Join-Path "resources" $nssFile)))),
                    (Join-Path "." (Join-Path "publish" $nssFile))
                )
                $copied = $false
                foreach ($sourcePath in $sourcePaths) {
                    if (Test-Path $sourcePath) {
                        $destPath = Join-Path $cliAppDir $nssFile
                        Copy-Item $sourcePath $destPath -Force
                        Write-Host "Copied $nssFile to CLI app directory" -ForegroundColor Gray
                        $copied = $true
                        break
                    }
                }
                if (-not $copied) {
                    Write-Host "Warning: Could not find $nssFile to copy to CLI app directory" -ForegroundColor Yellow
                }
            }
        }

        Write-Host ""
        Write-Host "Executable build successful!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Created portable executables:" -ForegroundColor Cyan

        # CLI executable info
        $cliSizeMB = [math]::Round((Get-ChildItem $cliAppImagePath -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
        Write-Host "  CLI: $cliExePath ($cliSizeMB MB)" -ForegroundColor White

        # GUI executable info (if exists)
        $guiAppImagePath = Join-Path $exeOutputDir $guiAppName
        if (Test-Path $guiAppImagePath) {
            $guiExeName = if ($IsWindows) { "$guiAppName.exe" } else { $guiAppName }
            $guiExePath = Join-Path $guiAppImagePath $guiExeName
            if (Test-Path $guiExePath) {
                $guiSizeMB = [math]::Round((Get-ChildItem $guiAppImagePath -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
                Write-Host "  GUI: $guiExePath ($guiSizeMB MB)" -ForegroundColor White
            }
        }

        Write-Host ""
        Write-Host "Portable executables ready! Run directly without installation." -ForegroundColor Green
        Write-Host ""
        Write-Host "CLI Usage examples:" -ForegroundColor Cyan
        $exePathExample = if ($IsWindows) {
            ".\dist-exe\NCSDecompCLI\NCSDecompCLI.exe"
        } else {
            "./dist-exe/NCSDecompCLI/NCSDecompCLI"
        }
        Write-Host "  $exePathExample --help" -ForegroundColor White
        Write-Host "  $exePathExample --version" -ForegroundColor White
        Write-Host "  $exePathExample -i script.ncs -o script.nss --k2" -ForegroundColor White
        Write-Host ""
        Write-Host "No Java installation required - everything is included!" -ForegroundColor Green

        # Cleanup temp directory
        if (Test-Path $jpackageInput) {
            Remove-Item -Recurse -Force $jpackageInput
        }
    } catch {
        Write-Host "Executable build failed: $_" -ForegroundColor Red
        # Cleanup on error
        if (Test-Path $jpackageInput) {
            Remove-Item -Recurse -Force $jpackageInput -ErrorAction SilentlyContinue
        }
        if (Test-Path "jpackage-input-gui") {
            Remove-Item -Recurse -Force "jpackage-input-gui" -ErrorAction SilentlyContinue
        }
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "Usage examples:" -ForegroundColor Cyan
    Write-Host "  java -jar NCSDecomp-CLI.jar -i input.ncs" -ForegroundColor White
    Write-Host "  java -jar NCSDecomp-CLI.jar -i input.ncs --stdout" -ForegroundColor White
    Write-Host "  java -jar NCSDecomp-CLI.jar -i scripts_dir -r --k2 -O output_dir" -ForegroundColor White
    Write-Host "  java -jar NCSDecomp-CLI.jar --help" -ForegroundColor White
    Write-Host ""
    Write-Host "To build executable, run: .\scripts\build.ps1 -BuildExe" -ForegroundColor Cyan
}
