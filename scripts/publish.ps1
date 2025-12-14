# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Publish script for NCSDecomp CLI and GUI
# Packages everything needed for end-user distribution
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

Write-Host "NCSDecomp CLI + GUI - Publishing Package" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green
Write-Host ""

# Build everything first
# Try to build executable if jpackage is available (requires JDK 14+), otherwise just build JARs
Write-Host "Step 1: Building JAR files (Java 8 compatible)..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "build.ps1"

# Check if jpackage is available (for executable builds)
$javaHome = $env:JAVA_HOME
$jpackageExe = if ($IsWindows) { "jpackage.exe" } else { "jpackage" }
$jpackageAvailable = $false
if ($javaHome -and (Test-Path $javaHome)) {
    $jpackagePath = Join-Path $javaHome (Join-Path "bin" $jpackageExe)
    $jpackageAvailable = (Test-Path $jpackagePath)
}

if ($jpackageAvailable) {
    Write-Host "  jpackage found - will build executable..." -ForegroundColor Gray
    & $buildScript -BuildExecutable
    if ($LASTEXITCODE -ne 0) {
        Write-Host "EXE build failed, falling back to JAR-only build..." -ForegroundColor Yellow
        # Fall back to JAR-only build
        & $buildScript
        if ($LASTEXITCODE -ne 0) {
            Write-Host "JAR build failed!" -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "  jpackage not found (requires JDK 14+) - building JARs only..." -ForegroundColor Gray
    Write-Host "  (JARs are Java 8 compatible and will run on any JRE 8+)" -ForegroundColor Gray
    & $buildScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "JAR build failed!" -ForegroundColor Red
        exit 1
    }
}

# Create archive staging directory (this becomes the ZIP root)
$targetDir = Join-Path "." "target"
$publishDir = Join-Path $targetDir "archive-top-level"
if (Test-Path $publishDir) {
    Write-Host ""
    Write-Host "Cleaning previous archive staging directory..." -ForegroundColor Yellow
    try {
        # Try to stop any processes that might be using files in assembly directory
        if ($IsWindows) {
            Get-Process | Where-Object { $_.Path -like "*$publishDir*" } | Stop-Process -Force -ErrorAction SilentlyContinue
            Start-Sleep -Milliseconds 500
        }
        Remove-Item -Recurse -Force $publishDir -ErrorAction Stop
    } catch {
        Write-Host "Warning: Could not delete $publishDir (files may be locked)" -ForegroundColor Yellow
        Write-Host "Attempting to delete individual files..." -ForegroundColor Yellow
        try {
            Get-ChildItem -Path $publishDir -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue
            Remove-Item -Force $publishDir -ErrorAction SilentlyContinue
        } catch {
            Write-Host "Error: Could not clean archive staging directory. Please close any applications using files in $publishDir" -ForegroundColor Red
            Write-Host "Or manually delete the target/archive-top-level folder and try again." -ForegroundColor Yellow
            exit 1
        }
    }
}
New-Item -ItemType Directory -Path $publishDir -Force | Out-Null

Write-Host ""
Write-Host "Step 2: Packaging distribution files..." -ForegroundColor Yellow

# Get JAR file paths first (needed for copying next to executables)
$jarDir = Join-Path $targetDir "jar"
$cliJarSource = Join-Path $jarDir "NCSDecompCLI.jar"
$guiJarSource = Join-Path $jarDir "NCSDecomp.jar"

# Copy complete jpackage app-image directories (not just the .exe files!)
# jpackage creates app-images that REQUIRE their sibling app/ + runtime/ folders to run.
# Without these folders, the .exe launcher fails with "Error opening NCSDecomp.cfg".
$distDir = Join-Path $targetDir "dist"

# Helper function to copy jpackage app-image and add config/tools
function Copy-JPackageApp {
    param(
        [string]$AppName,
        [string]$SourceDir,
        [string]$DestDir
    )

    $appImageDir = Join-Path $SourceDir $AppName
    if (-not (Test-Path $appImageDir)) {
        return $false
    }

    $destAppDir = Join-Path $DestDir $AppName
    Write-Host "  - Copying $AppName app-image (complete jpackage output)..." -ForegroundColor Cyan

    # Copy entire app-image directory
    Copy-Item -Path $appImageDir -Destination $destAppDir -Recurse -Force

    # Create config/ inside the app-image so the EXE can find it
    $appConfigDir = Join-Path $destAppDir "config"
    New-Item -ItemType Directory -Path $appConfigDir -Force | Out-Null

    # Create tools/ inside the app-image so the EXE can find compilers
    $appToolsDir = Join-Path $destAppDir "tools"
    New-Item -ItemType Directory -Path $appToolsDir -Force | Out-Null

    return $true
}

# Copy CLI app-image
$cliCopied = Copy-JPackageApp -AppName "NCSDecompCLI" -SourceDir $distDir -DestDir $publishDir
if ($cliCopied) {
    Write-Host "    - Copied complete NCSDecompCLI app-image" -ForegroundColor Gray
} else {
    Write-Host "  Note: NCSDecompCLI app-image not found (executable build skipped - requires JDK 14+ with jpackage)" -ForegroundColor Gray
    Write-Host "         JAR files are provided instead (Java 8+ compatible)" -ForegroundColor Gray
}

# Copy GUI app-image
$guiCopied = Copy-JPackageApp -AppName "NCSDecomp" -SourceDir $distDir -DestDir $publishDir
if ($guiCopied) {
    Write-Host "    - Copied complete NCSDecomp app-image" -ForegroundColor Gray
} else {
    Write-Host "  Note: NCSDecomp app-image not found (GUI executable build skipped)" -ForegroundColor Gray
}

# Copy JAR files to root as well (for standalone usage)

if (Test-Path $cliJarSource) {
    Copy-Item $cliJarSource (Join-Path $publishDir "NCSDecompCLI.jar")
    Write-Host "  - Copied NCSDecompCLI.jar" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: NCSDecompCLI.jar not found at $cliJarSource" -ForegroundColor Yellow
}

# Copy GUI JAR if present
if (Test-Path $guiJarSource) {
    Copy-Item $guiJarSource (Join-Path $publishDir "NCSDecomp.jar")
    Write-Host "  - Copied NCSDecomp.jar (GUI)" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: NCSDecomp.jar not found at $guiJarSource" -ForegroundColor Yellow
}

# Copy tools/ payload (compiler + nwscript) into tools/ folder in the archive
# Also copy into each jpackage app-image directory for EXE users
$toolsDir = Join-Path "." "tools"
$toolsPublishDir = Join-Path $publishDir "tools"
New-Item -ItemType Directory -Path $toolsPublishDir -Force | Out-Null

$toolPayload = @(
    "k1_nwscript.nss",
    "tsl_nwscript.nss",
    "nwnnsscomp_kscript.exe",
    "nwnnsscomp_ktool.exe",
    "ncsdis.exe",
    "icudt63.dll",
    "icuin63.dll",
    "icuuc63.dll",
    "libboost_filesystem.dll",
    "libboost_locale.dll",
    "libboost_thread.dll",
    "libgcc_s_seh-1.dll",
    "libiconv-2.dll",
    "libstdc++-6.dll",
    "libwinpthread-1.dll"
)

# Collect destinations: root tools/ plus each app-image tools/
$toolDestinations = @($toolsPublishDir)
$cliToolsDir = Join-Path (Join-Path $publishDir "NCSDecompCLI") "tools"
$guiToolsDir = Join-Path (Join-Path $publishDir "NCSDecomp") "tools"
if (Test-Path $cliToolsDir) { $toolDestinations += $cliToolsDir }
if (Test-Path $guiToolsDir) { $toolDestinations += $guiToolsDir }

foreach ($tool in $toolPayload) {
    $toolPath = Join-Path $toolsDir $tool
    if (Test-Path $toolPath) {
        foreach ($dest in $toolDestinations) {
            Copy-Item $toolPath (Join-Path $dest $tool) -Force
        }
        Write-Host "  - Copied tools/$tool (to $($toolDestinations.Count) locations)" -ForegroundColor Cyan
    } else {
        Write-Host "  Warning: tools/$tool not found" -ForegroundColor Yellow
    }
}

# Create default config file into config/
# Also copy into each jpackage app-image directory for EXE users
$configPublishDir = Join-Path $publishDir "config"
New-Item -ItemType Directory -Path $configPublishDir -Force | Out-Null
$defaultConfig = @"
# NCSDecomp Configuration
# key=value (Java .properties format)
#
# The GUI reads this file from: config/ncsdecomp.conf
# (legacy name also supported: config/dencs.conf)
#
# NOTE: Leave path settings empty to use automatic detection.
# The app will find tools/compilers relative to the EXE location.
# Only set paths if you want to override the default locations.

Game Variant=k1
Prefer Switches=false
Strict Signatures=false
Overwrite Files=false
Encoding=Windows-1252
File Extension=.nss
Filename Prefix=
Filename Suffix=
Link Scroll Bars=false

# Compiler settings - leave empty for automatic detection from app's tools/ directory
nwnnsscomp Folder Path=
nwnnsscomp Filename=
nwnnsscomp Path=

# nwscript.nss paths - leave empty for automatic detection from app's tools/ directory
K1 nwscript Path=
K2 nwscript Path=
"@

# Collect destinations: root config/ plus each app-image config/
$configDestinations = @($configPublishDir)
$cliConfigDir = Join-Path (Join-Path $publishDir "NCSDecompCLI") "config"
$guiConfigDir = Join-Path (Join-Path $publishDir "NCSDecomp") "config"
if (Test-Path $cliConfigDir) { $configDestinations += $cliConfigDir }
if (Test-Path $guiConfigDir) { $configDestinations += $guiConfigDir }

foreach ($dest in $configDestinations) {
    $configOutPath = Join-Path $dest "ncsdecomp.conf"
    if ($IsWindows) {
        $defaultConfig | Out-File $configOutPath -Encoding ASCII
    } else {
        $defaultConfig | Out-File $configOutPath -Encoding utf8NoBOM
    }
}
Write-Host "  - Wrote config/ncsdecomp.conf (to $($configDestinations.Count) locations)" -ForegroundColor Cyan

# Copy docs into archive root (verbatim names)
$docsDir = Join-Path "." "docs"
$docPayload = @(
    "CHANGELOG.txt",
    "README-CLI.md",
    "README-TECHNICAL.md",
    "README-USER.md",
    "README.txt",
    "VERSION.txt"
)

foreach ($doc in $docPayload) {
    $docPath = Join-Path $docsDir $doc
    if (Test-Path $docPath) {
        Copy-Item $docPath (Join-Path $publishDir $doc) -Force
        Write-Host "  - Copied $doc" -ForegroundColor Cyan
    } else {
        Write-Host "  Warning: docs/$doc not found" -ForegroundColor Yellow
    }
}

# Copy LICENSE file if it exists (check common locations and names)
$licenseFiles = @(
    (Join-Path "." "LICENSE"),
    (Join-Path "." "LICENSE.txt"),
    (Join-Path "." "LICENSE.md"),
    (Join-Path "." "LICENSE.TXT")
)
$licenseCopied = $false
foreach ($licenseFile in $licenseFiles) {
    if (Test-Path $licenseFile) {
        $licenseName = Split-Path $licenseFile -Leaf
        Copy-Item $licenseFile (Join-Path $publishDir $licenseName)
        Write-Host "  - Copied $licenseName" -ForegroundColor Cyan
        $licenseCopied = $true
        break
    }
}
if (-not $licenseCopied) {
    Write-Host "  Warning: LICENSE file not found" -ForegroundColor Yellow
}

# Create examples directory
$examplesDir = Join-Path $publishDir "examples"
New-Item -ItemType Directory -Path $examplesDir -Force | Out-Null

# Create example scripts (batch files for Windows, shell scripts for Unix)
if ($IsWindows) {
    # Windows batch files
    $example1 = @"
@echo off
REM Example: Decompile a single file (KotOR 2/TSL)
..\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2
pause
"@
    $example1 | Out-File (Join-Path $examplesDir "example1-decompile-single.bat") -Encoding ASCII

    $example2 = @"
@echo off
REM Example: Decompile entire directory recursively (KotOR 1)
..\NCSDecompCLI.exe -i "scripts_folder" -r --k1 -O "output_folder"
pause
"@
    $example2 | Out-File (Join-Path $examplesDir "example2-decompile-folder.bat") -Encoding ASCII

    $example3 = @"
@echo off
REM Example: View decompiled code in console
..\NCSDecompCLI.exe -i "script.ncs" --stdout --k2
pause
"@
    $example3 | Out-File (Join-Path $examplesDir "example3-view-in-console.bat") -Encoding ASCII
    Write-Host "  - Created example batch files" -ForegroundColor Cyan
} else {
    # Unix shell scripts
    $example1 = @"
#!/bin/bash
# Example: Decompile a single file (KotOR 2/TSL)
../NCSDecompCLI -i "script.ncs" -o "script.nss" --k2
"@
    $example1File = Join-Path $examplesDir "example1-decompile-single.sh"
    $example1 | Out-File $example1File -Encoding utf8NoBOM
    if (-not $IsWindows) {
        # Make executable on Unix
        chmod +x $example1File
    }

    $example2 = @"
#!/bin/bash
# Example: Decompile entire directory recursively (KotOR 1)
../NCSDecompCLI -i "scripts_folder" -r --k1 -O "output_folder"
"@
    $example2File = Join-Path $examplesDir "example2-decompile-folder.sh"
    $example2 | Out-File $example2File -Encoding utf8NoBOM
    if (-not $IsWindows) {
        chmod +x $example2File
    }

    $example3 = @"
#!/bin/bash
# Example: View decompiled code in console
../NCSDecompCLI -i "script.ncs" --stdout --k2
"@
    $example3File = Join-Path $examplesDir "example3-view-in-console.sh"
    $example3 | Out-File $example3File -Encoding utf8NoBOM
    if (-not $IsWindows) {
        chmod +x $example3File
    }
    Write-Host "  - Created example shell scripts" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Step 3: Creating ZIP archive..." -ForegroundColor Yellow

# Create ZIP archive name with version and platform (Java/Maven idiomatic: target/)
$platformSuffix = if ($IsWindows) { "Windows" } elseif ($IsMacOS) { "macOS" } else { "Linux" }
$version = "0.0.0"
$docsVersionPath = Join-Path "." (Join-Path "docs" "VERSION.txt")
if (Test-Path $docsVersionPath) {
    try {
        $versionLine = Get-Content $docsVersionPath | Where-Object { $_ -match '^Version:\s*' } | Select-Object -First 1
        if ($versionLine) {
            $parsed = ($versionLine -replace '^Version:\s*', '').Trim()
            if ($parsed) { $version = $parsed }
        }
    } catch {
        # ignore and keep default
    }
}
$zipFileName = "NCSDecomp-v$version-$platformSuffix.zip"
$zipPath = Join-Path $targetDir $zipFileName

# Remove existing ZIP if it exists
if (Test-Path $zipPath) {
    try {
        Remove-Item -Force $zipPath -ErrorAction Stop
        Write-Host "  - Removed existing ZIP archive" -ForegroundColor Gray
    } catch {
        Write-Host "  Warning: Could not delete existing ZIP (file may be locked)" -ForegroundColor Yellow
        Write-Host "  Attempting to close any processes using the file..." -ForegroundColor Yellow
        if ($IsWindows) {
            # Try to find and close processes that might be using the file
            Get-Process | Where-Object { $_.Path -like "*$zipPath*" } | Stop-Process -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
            try {
                Remove-Item -Force $zipPath -ErrorAction Stop
                Write-Host "  - Removed existing ZIP archive after closing processes" -ForegroundColor Gray
            } catch {
                Write-Host "  Error: Still cannot delete ZIP. Please close any applications using it and try again." -ForegroundColor Red
                exit 1
            }
        } else {
            Write-Host "  Error: Cannot delete ZIP. Please close any applications using it and try again." -ForegroundColor Red
            exit 1
        }
    }
}

# Create ZIP archive using .NET Compression
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $compressionLevel = [System.IO.Compression.CompressionLevel]::Optimal

    $publishDirResolved = (Resolve-Path $publishDir).Path
    $zipPathResolved = (Resolve-Path $targetDir).Path
    $zipPathResolved = Join-Path $zipPathResolved $zipFileName

    # Create ZIP from publish directory
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $publishDirResolved,
        $zipPathResolved,
        $compressionLevel,
        $false  # includeBaseDirectory = false (we want contents, not the publish folder itself)
    )

    $zipSize = [math]::Round((Get-Item $zipPathResolved).Length / 1MB, 2)
    Write-Host "  - Created ZIP archive: $zipFileName ($zipSize MB)" -ForegroundColor Cyan
} catch {
    Write-Host "  Error creating ZIP archive: $_" -ForegroundColor Red
    Write-Host "  Falling back to Compress-Archive..." -ForegroundColor Yellow

    # Fallback: Use Compress-Archive (PowerShell 5.0+)
    try {
        $publishDirResolved = (Resolve-Path $publishDir).Path
        Compress-Archive -Path "$publishDirResolved\*" -DestinationPath $zipPath -Force
        $zipSize = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)
        Write-Host "  - Created ZIP archive: $zipFileName ($zipSize MB)" -ForegroundColor Cyan
    } catch {
        Write-Host "  Error: Could not create ZIP archive. Manual packaging required." -ForegroundColor Red
        Write-Host "  Error details: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Green
Write-Host "Publishing complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Distribution package ready in: target/archive-top-level" -ForegroundColor Cyan
if (Test-Path $zipPath) {
    Write-Host "ZIP archive created: target/$zipFileName" -ForegroundColor Green
}

