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
Write-Host "Step 1: Building JAR..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "build.ps1"
& $buildScript
if ($LASTEXITCODE -ne 0) {
    Write-Host "JAR build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
$exeType = if ($IsWindows) { ".exe" } elseif ($IsMacOS) { ".app" } else { "executable" }
Write-Host "Step 2: Building self-contained $exeType..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "build.ps1"
& $buildScript -BuildExecutable
if ($LASTEXITCODE -ne 0) {
    Write-Host "EXE build failed!" -ForegroundColor Red
    exit 1
}

# Create assembly directory (Java/Maven idiomatic: target/assembly)
$targetDir = Join-Path "." "target"
$publishDir = Join-Path $targetDir "assembly"
if (Test-Path $publishDir) {
    Write-Host ""
    Write-Host "Cleaning previous assembly directory..." -ForegroundColor Yellow
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
            Write-Host "Error: Could not clean assembly directory. Please close any applications using files in $publishDir" -ForegroundColor Red
            Write-Host "Or manually delete the target/assembly folder and try again." -ForegroundColor Yellow
            exit 1
        }
    }
}
New-Item -ItemType Directory -Path $publishDir -Force | Out-Null

Write-Host ""
Write-Host "Step 3: Packaging distribution files..." -ForegroundColor Yellow

# Copy CLI executable folder (NCSDecompCLI)
$cliAppImageDir = Join-Path $targetDir (Join-Path "dist" "NCSDecompCLI")
$cliAppDest = Join-Path $publishDir "NCSDecompCLI"

if (Test-Path $cliAppImageDir) {
    Copy-Item $cliAppImageDir $cliAppDest -Recurse
    Write-Host "  - Copied NCSDecompCLI folder (portable app)" -ForegroundColor Cyan
    $exeName = if ($IsWindows) { "NCSDecompCLI.exe" } else { "NCSDecompCLI" }
    $runPath = Join-Path "NCSDecompCLI" $exeName
    Write-Host "    Run: $runPath" -ForegroundColor Gray
    
    # Copy JAR next to executable for alternative usage
    if (Test-Path $cliJarSource) {
        Copy-Item $cliJarSource (Join-Path $cliAppDest "NCSDecomp-CLI.jar")
        Write-Host "  - Copied NCSDecomp-CLI.jar to NCSDecompCLI folder" -ForegroundColor Cyan
    }
} else {
    Write-Host "  Warning: NCSDecompCLI folder not found, skipping..." -ForegroundColor Yellow
}

# Copy GUI executable folder (NCSDecomp)
$guiAppImageDir = Join-Path $targetDir (Join-Path "dist" "NCSDecomp")
$guiAppDest = Join-Path $publishDir "NCSDecomp"

if (Test-Path $guiAppImageDir) {
    Copy-Item $guiAppImageDir $guiAppDest -Recurse
    Write-Host "  - Copied NCSDecomp folder (portable app)" -ForegroundColor Cyan
    $guiExeName = if ($IsWindows) { "NCSDecomp.exe" } else { "NCSDecomp" }
    $guiRunPath = Join-Path "NCSDecomp" $guiExeName
    Write-Host "    Run: $guiRunPath" -ForegroundColor Gray
    
    # Copy JAR next to executable for alternative usage
    if (Test-Path $guiJarSource) {
        Copy-Item $guiJarSource (Join-Path $guiAppDest "NCSDecomp.jar")
        Write-Host "  - Copied NCSDecomp.jar to NCSDecomp folder" -ForegroundColor Cyan
    }
} else {
    Write-Host "  Note: NCSDecomp folder (GUI) not found, skipping..." -ForegroundColor Gray
}

# Copy JAR as alternative (from target/jar)
$jarDir = Join-Path $targetDir "jar"
$cliJarSource = Join-Path $jarDir "NCSDecomp-CLI.jar"
$guiJarSource = Join-Path $jarDir "NCSDecomp.jar"

if (Test-Path $cliJarSource) {
    Copy-Item $cliJarSource (Join-Path $publishDir "NCSDecomp-CLI.jar")
    Write-Host "  - Copied NCSDecomp-CLI.jar" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: NCSDecomp-CLI.jar not found at $cliJarSource" -ForegroundColor Yellow
}

# Copy GUI JAR if present
if (Test-Path $guiJarSource) {
    Copy-Item $guiJarSource (Join-Path $publishDir "NCSDecomp.jar")
    Write-Host "  - Copied NCSDecomp.jar (GUI)" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: NCSDecomp.jar not found at $guiJarSource" -ForegroundColor Yellow
}

# Copy required nwscript files from tools/ or src/main/resources
$nwscriptSource = Join-Path "." (Join-Path "src" (Join-Path "main" "resources"))
$toolsDir = Join-Path "." "tools"
$k1Source = Join-Path $nwscriptSource "k1_nwscript.nss"
$tslSource = Join-Path $nwscriptSource "tsl_nwscript.nss"
$k1Tools = Join-Path $toolsDir "k1_nwscript.nss"
$tslTools = Join-Path $toolsDir "tsl_nwscript.nss"

# Copy compiler tools from tools/ directory
# Priority order based on KnownExternalCompilers.java:
# 1. nwnnsscomp.exe (primary - generic name)
# 2. nwnnsscomp_kscript.exe (secondary - KOTOR Scripting Tool)
# 3. nwnnsscomp_tslpatcher.exe (TSLPatcher variant)
# 4. nwnnsscomp_v1.exe (v1.3 first public release)
$toolsPublishDir = Join-Path $publishDir "tools"
New-Item -ItemType Directory -Path $toolsPublishDir -Force | Out-Null

# Compiler tools in priority order (primary first, then secondary, then others)
$compilerTools = @(
    "nwnnsscomp.exe",              # Primary - generic name (highest priority)
    "nwnnsscomp_kscript.exe",      # Secondary - KOTOR Scripting Tool
    "nwnnsscomp_tslpatcher.exe",   # TSLPatcher variant
    "nwnnsscomp_v1.exe"            # v1.3 first public release
)

$copiedCount = 0
foreach ($tool in $compilerTools) {
    $toolPath = Join-Path $toolsDir $tool
    if (Test-Path $toolPath) {
        Copy-Item $toolPath (Join-Path $toolsPublishDir $tool)
        $priority = if ($copiedCount -eq 0) { " (primary)" } elseif ($copiedCount -eq 1) { " (secondary)" } else { "" }
        Write-Host "  - Copied $tool to tools/$priority" -ForegroundColor Cyan
        $copiedCount++
    } else {
        if ($tool -eq "nwnnsscomp.exe" -or $tool -eq "nwnnsscomp_v1.exe") {
            # These are optional - only warn if they're missing
            Write-Host "  Note: $tool not found (optional)" -ForegroundColor Gray
        } else {
            Write-Host "  Warning: $tool not found at $toolPath" -ForegroundColor Yellow
        }
    }
}

if (Test-Path $k1Source) {
    Copy-Item $k1Source $publishDir
    Write-Host "  - Copied k1_nwscript.nss" -ForegroundColor Cyan
} elseif (Test-Path $k1Tools) {
    Copy-Item $k1Tools $publishDir
    Write-Host "  - Copied k1_nwscript.nss" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: k1_nwscript.nss not found" -ForegroundColor Yellow
}

if (Test-Path $tslSource) {
    Copy-Item $tslSource $publishDir
    Write-Host "  - Copied tsl_nwscript.nss" -ForegroundColor Cyan
} elseif (Test-Path $tslTools) {
    Copy-Item $tslTools $publishDir
    Write-Host "  - Copied tsl_nwscript.nss" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: tsl_nwscript.nss not found" -ForegroundColor Yellow
}

# Copy user-friendly README
$docsReadmeUser = Join-Path "." (Join-Path "docs" "README-USER.md")
$rootReadmeUser = Join-Path "." "README-USER.md"
if (Test-Path $docsReadmeUser) {
    Copy-Item $docsReadmeUser (Join-Path $publishDir "README.txt")
    Write-Host "  - Copied README.txt" -ForegroundColor Cyan
} elseif (Test-Path $rootReadmeUser) {
    Copy-Item $rootReadmeUser (Join-Path $publishDir "README.txt")
    Write-Host "  - Copied README.txt" -ForegroundColor Cyan
}

# Copy technical documentation
$docsReadmeCli = Join-Path "." (Join-Path "docs" "README-CLI.md")
$rootReadmeCli = Join-Path "." "README-CLI.md"
if (Test-Path $docsReadmeCli) {
    Copy-Item $docsReadmeCli (Join-Path $publishDir "README-TECHNICAL.md")
    Write-Host "  - Copied README-TECHNICAL.md" -ForegroundColor Cyan
} elseif (Test-Path $rootReadmeCli) {
    Copy-Item $rootReadmeCli (Join-Path $publishDir "README-TECHNICAL.md")
    Write-Host "  - Copied README-TECHNICAL.md" -ForegroundColor Cyan
}

# Copy main README if it exists
$rootReadme = Join-Path "." "README.md"
if (Test-Path $rootReadme) {
    Copy-Item $rootReadme $publishDir
    Write-Host "  - Copied README.md" -ForegroundColor Cyan
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
NCSDecompCLI\NCSDecompCLI.exe -i "script.ncs" -o "script.nss" --k2
pause
"@
    $example1 | Out-File (Join-Path $examplesDir "example1-decompile-single.bat") -Encoding ASCII

    $example2 = @"
@echo off
REM Example: Decompile entire directory recursively (KotOR 1)
NCSDecompCLI\NCSDecompCLI.exe -i "scripts_folder" -r --k1 -O "output_folder"
pause
"@
    $example2 | Out-File (Join-Path $examplesDir "example2-decompile-folder.bat") -Encoding ASCII

    $example3 = @"
@echo off
REM Example: View decompiled code in console
NCSDecompCLI\NCSDecompCLI.exe -i "script.ncs" --stdout --k2
pause
"@
    $example3 | Out-File (Join-Path $examplesDir "example3-view-in-console.bat") -Encoding ASCII
    Write-Host "  - Created example batch files" -ForegroundColor Cyan
} else {
    # Unix shell scripts
    $example1 = @"
#!/bin/bash
# Example: Decompile a single file (KotOR 2/TSL)
./NCSDecompCLI/NCSDecompCLI -i "script.ncs" -o "script.nss" --k2
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
./NCSDecompCLI/NCSDecompCLI -i "scripts_folder" -r --k1 -O "output_folder"
"@
    $example2File = Join-Path $examplesDir "example2-decompile-folder.sh"
    $example2 | Out-File $example2File -Encoding utf8NoBOM
    if (-not $IsWindows) {
        chmod +x $example2File
    }

    $example3 = @"
#!/bin/bash
# Example: View decompiled code in console
./NCSDecompCLI/NCSDecompCLI -i "script.ncs" --stdout --k2
"@
    $example3File = Join-Path $examplesDir "example3-view-in-console.sh"
    $example3 | Out-File $example3File -Encoding utf8NoBOM
    if (-not $IsWindows) {
        chmod +x $example3File
    }
    Write-Host "  - Created example shell scripts" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Step 4: Creating version info..." -ForegroundColor Yellow
$versionInfo = @"
NCSDecomp Distribution Package (CLI + GUI)
Version: 1.0.0
Build Date: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Platform: $(if ($IsWindows) { "Windows" } elseif ($IsMacOS) { "macOS" } elseif ($IsLinux) { "Linux" } else { "Unknown" })

For more information, visit: https://bolabaden.org
"@
$versionFile = Join-Path $publishDir "VERSION.txt"
if ($IsWindows) {
    $versionInfo | Out-File $versionFile -Encoding ASCII
} else {
    $versionInfo | Out-File $versionFile -Encoding utf8NoBOM
}
Write-Host "  - Created VERSION.txt" -ForegroundColor Cyan

Write-Host ""
Write-Host "Step 5: Creating ZIP archive..." -ForegroundColor Yellow

# Create ZIP archive name with version and platform (Java/Maven idiomatic: target/)
$platformSuffix = if ($IsWindows) { "Windows" } elseif ($IsMacOS) { "macOS" } else { "Linux" }
$zipFileName = "NCSDecomp-v1.0.0-$platformSuffix.zip"
$zipPath = Join-Path $targetDir $zipFileName

# Remove existing ZIP if it exists
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
    Write-Host "  - Removed existing ZIP archive" -ForegroundColor Gray
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
Write-Host "Distribution package ready in: target/assembly" -ForegroundColor Cyan
if (Test-Path $zipPath) {
    Write-Host "ZIP archive created: target/$zipFileName" -ForegroundColor Green
}

