# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI as self-contained Windows .exe
# Uses jpackage to create a standalone executable with bundled Java runtime

$ErrorActionPreference = "Stop"

Write-Host "Building NCSDecomp CLI as self-contained .exe..." -ForegroundColor Green
Write-Host ""

# Check prerequisites
$jpackagePath = "$env:JAVA_HOME\bin\jpackage.exe"
if (-not (Test-Path $jpackagePath)) {
    Write-Host "Error: jpackage not found at $jpackagePath" -ForegroundColor Red
    Write-Host "Please ensure JAVA_HOME is set and points to JDK 14 or later" -ForegroundColor Yellow
    exit 1
}

# Ensure JAR exists
if (-not (Test-Path "NCSDecomp-CLI.jar")) {
    Write-Host "NCSDecomp-CLI.jar not found. Running build.ps1 first..." -ForegroundColor Yellow
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit 1
    }
}

# For CLI tools, always use app-image (portable executable that runs directly)
# This creates a folder with NCSDecomp.exe that can be run immediately without installation
$packageType = "app-image"
$exeOutputDir = "dist-exe"

Write-Host "Creating portable executable (runs directly, no installation needed)..." -ForegroundColor Cyan
Write-Host ""

# Clean previous build (handle locked files gracefully)
if (Test-Path $exeOutputDir) {
    Write-Host "Cleaning previous build..." -ForegroundColor Yellow
    try {
        Get-Process | Where-Object { $_.Path -like "*$exeOutputDir*" } | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
        Remove-Item -Recurse -Force $exeOutputDir -ErrorAction Stop
    } catch {
        Write-Host "Warning: Could not delete $exeOutputDir (files may be locked)" -ForegroundColor Yellow
        Write-Host "Please close any running NCSDecomp processes and try again." -ForegroundColor Yellow
        Write-Host "Or manually delete the dist-exe folder." -ForegroundColor Yellow
        exit 1
    }
}

# Create temporary input directory for jpackage
$jpackageInput = "jpackage-input"
if (Test-Path $jpackageInput) {
    Remove-Item -Recurse -Force $jpackageInput
}
New-Item -ItemType Directory -Path $jpackageInput -Force | Out-Null

# Copy JAR to input directory
Copy-Item "NCSDecomp-CLI.jar" "$jpackageInput\NCSDecomp-CLI.jar"

# Copy nwscript files as resources (they'll be in the app directory)
Copy-Item "k1_nwscript.nss" "$jpackageInput\" -ErrorAction SilentlyContinue
Copy-Item "tsl_nwscript.nss" "$jpackageInput\" -ErrorAction SilentlyContinue
Copy-Item "..\k1_nwscript.nss" "$jpackageInput\" -ErrorAction SilentlyContinue
Copy-Item "..\tsl_nwscript.nss" "$jpackageInput\" -ErrorAction SilentlyContinue

Write-Host "Building portable executable with jpackage..." -ForegroundColor Yellow
Write-Host "This may take several minutes (first time can take 5-10 minutes)..." -ForegroundColor Cyan
Write-Host ""

try {
    # Build CLI executable
    $cliAppName = "NCSDecompCLI"
    $cliMainClass = "com.kotor.resource.formats.ncs.NCSDecompCLI"
    $appVersion = "2.0"

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
        "--win-console",
        "--java-options", "-Xmx2G",
        "--java-options", "-Djava.awt.headless=true"
    )

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
        Copy-Item $guiJarPath "$guiInputDir\NCSDecomp.jar"

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
    $cliExePath = Join-Path $cliAppImagePath "$cliAppName.exe"
    if (-not (Test-Path $cliExePath)) {
        throw "CLI executable not found in app image: $cliExePath"
    }

    # Copy nwscript files to CLI app directory
    $cliAppDir = Join-Path $cliAppImagePath "app"
    if (Test-Path $cliAppDir) {
        $nssFiles = @("k1_nwscript.nss", "tsl_nwscript.nss")
        foreach ($nssFile in $nssFiles) {
            $sourcePath = $nssFile
            if (-not (Test-Path $sourcePath)) {
                $sourcePath = Join-Path ".." $nssFile
            }
            if (Test-Path $sourcePath) {
                Copy-Item $sourcePath (Join-Path $cliAppDir $nssFile) -Force
                Write-Host "Copied $nssFile to CLI app directory" -ForegroundColor Gray
            }
        }
    }

    # Note: Don't create launcher copies in root - jpackage app-images must be run from their directory
    # The launchers would look for cfg files in the wrong location

    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Created portable executables:" -ForegroundColor Cyan

    # CLI executable info
    $cliSizeMB = [math]::Round((Get-ChildItem $cliAppImagePath -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
    Write-Host "  CLI: $cliExePath ($cliSizeMB MB)" -ForegroundColor White

    # GUI executable info (if exists)
    $guiAppImagePath = Join-Path $exeOutputDir $guiAppName
    if (Test-Path $guiAppImagePath) {
        $guiExePath = Join-Path $guiAppImagePath "$guiAppName.exe"
        if (Test-Path $guiExePath) {
            $guiSizeMB = [math]::Round((Get-ChildItem $guiAppImagePath -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
            Write-Host "  GUI: $guiExePath ($guiSizeMB MB)" -ForegroundColor White
        }
    }

    Write-Host ""
    Write-Host "Portable executables ready! Run directly without installation." -ForegroundColor Green
    Write-Host ""
    Write-Host "CLI Usage examples:" -ForegroundColor Cyan
    Write-Host "  .\dist-exe\NCSDecompCLI\NCSDecompCLI.exe --help" -ForegroundColor White
    Write-Host "  .\dist-exe\NCSDecompCLI\NCSDecompCLI.exe --version" -ForegroundColor White
    Write-Host "  .\dist-exe\NCSDecompCLI\NCSDecompCLI.exe -i script.ncs -o script.nss --k2" -ForegroundColor White
    Write-Host ""
    Write-Host "No Java installation required - everything is included!" -ForegroundColor Green

    # Cleanup temp directory
    if (Test-Path $jpackageInput) {
        Remove-Item -Recurse -Force $jpackageInput
    }
} catch {
    Write-Host "Build failed: $_" -ForegroundColor Red
    # Cleanup on error
    if (Test-Path $jpackageInput) {
        Remove-Item -Recurse -Force $jpackageInput -ErrorAction SilentlyContinue
    }
    if (Test-Path "jpackage-input-gui") {
        Remove-Item -Recurse -Force "jpackage-input-gui" -ErrorAction SilentlyContinue
    }
    exit 1
}

