# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI as self-contained executable
# Uses jpackage to create a standalone executable with bundled Java runtime
# Cross-platform compatible (Windows, macOS, Linux)

$ErrorActionPreference = "Stop"

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
Write-Host "Building NCSDecomp CLI as self-contained executable for $platformName..." -ForegroundColor Green
Write-Host ""

# Check prerequisites
$jpackageExe = if ($IsWindows) { "jpackage.exe" } else { "jpackage" }
$jpackagePath = Join-Path $env:JAVA_HOME (Join-Path "bin" $jpackageExe)
if (-not (Test-Path $jpackagePath)) {
    Write-Host "Error: jpackage not found at $jpackagePath" -ForegroundColor Red
    Write-Host "Please ensure JAVA_HOME is set and points to JDK 14 or later" -ForegroundColor Yellow
    exit 1
}

# Ensure JAR exists
$jarFile = Join-Path "." "NCSDecomp-CLI.jar"
if (-not (Test-Path $jarFile)) {
    Write-Host "NCSDecomp-CLI.jar not found. Running build.ps1 first..." -ForegroundColor Yellow
    $buildScript = Join-Path $PSScriptRoot "build.ps1"
    & $buildScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit 1
    }
}

# For CLI tools, always use app-image (portable executable that runs directly)
# This creates a folder with executable that can be run immediately without installation
$packageType = "app-image"
$exeOutputDir = Join-Path "." "dist-exe"

Write-Host "Creating portable executable (runs directly, no installation needed)..." -ForegroundColor Cyan
Write-Host ""

# Clean previous build (handle locked files gracefully)
if (Test-Path $exeOutputDir) {
    Write-Host "Cleaning previous build..." -ForegroundColor Yellow
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

