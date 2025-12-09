# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Publish script for NCSDecomp CLI
# Packages everything needed for end-user distribution
# Cross-platform compatible (Windows, macOS, Linux)

$ErrorActionPreference = "Stop"

# Detect platform (PowerShell Core 6+)
$IsWindows = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsWindows } else { $env:OS -eq "Windows_NT" }
$IsLinux = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsLinux } else { $false }
$IsMacOS = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsMacOS } else { $false }

Write-Host "NCSDecomp CLI - Publishing Package" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
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
$buildExeScript = Join-Path $PSScriptRoot "build-exe.ps1"
& $buildExeScript
if ($LASTEXITCODE -ne 0) {
    Write-Host "EXE build failed!" -ForegroundColor Red
    exit 1
}

# Create publish directory
$publishDir = Join-Path "." "publish"
if (Test-Path $publishDir) {
    Write-Host ""
    Write-Host "Cleaning previous publish directory..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $publishDir
}
New-Item -ItemType Directory -Path $publishDir -Force | Out-Null

Write-Host ""
Write-Host "Step 3: Packaging distribution files..." -ForegroundColor Yellow

# Copy CLI executable folder (NCSDecompCLI)
$cliAppImageDir = Join-Path "." (Join-Path "dist-exe" "NCSDecompCLI")
$cliAppDest = Join-Path $publishDir "NCSDecompCLI"

if (Test-Path $cliAppImageDir) {
    Copy-Item $cliAppImageDir $cliAppDest -Recurse
    Write-Host "  - Copied NCSDecompCLI folder (portable app)" -ForegroundColor Cyan
    $exeName = if ($IsWindows) { "NCSDecompCLI.exe" } else { "NCSDecompCLI" }
    $runPath = Join-Path "NCSDecompCLI" $exeName
    Write-Host "    Run: $runPath" -ForegroundColor Gray
} else {
    Write-Host "  Warning: NCSDecompCLI folder not found, skipping..." -ForegroundColor Yellow
}

# Copy GUI executable folder (NCSDecomp) if it exists
$guiAppImageDir = Join-Path "." (Join-Path "dist-exe" "NCSDecomp")
$guiAppDest = Join-Path $publishDir "NCSDecomp"

if (Test-Path $guiAppImageDir) {
    Copy-Item $guiAppImageDir $guiAppDest -Recurse
    Write-Host "  - Copied NCSDecomp folder (portable app)" -ForegroundColor Cyan
    $guiExeName = if ($IsWindows) { "NCSDecomp.exe" } else { "NCSDecomp" }
    $guiRunPath = Join-Path "NCSDecomp" $guiExeName
    Write-Host "    Run: $guiRunPath" -ForegroundColor Gray
} else {
    Write-Host "  Note: NCSDecomp folder (GUI) not found, skipping..." -ForegroundColor Gray
}

# Copy JAR as alternative
Copy-Item "NCSDecomp-CLI.jar" "$publishDir\NCSDecomp-CLI.jar"
Write-Host "  - Copied NCSDecomp-CLI.jar" -ForegroundColor Cyan

# Copy required nwscript files from src/main/resources
$nwscriptSource = Join-Path "." (Join-Path "src" (Join-Path "main" "resources"))
$k1Source = Join-Path $nwscriptSource "k1_nwscript.nss"
$tslSource = Join-Path $nwscriptSource "tsl_nwscript.nss"
$k1Root = Join-Path "." "k1_nwscript.nss"
$tslRoot = Join-Path "." "tsl_nwscript.nss"

if (Test-Path $k1Source) {
    Copy-Item $k1Source $publishDir
    Write-Host "  - Copied k1_nwscript.nss" -ForegroundColor Cyan
} elseif (Test-Path $k1Root) {
    Copy-Item $k1Root $publishDir
    Write-Host "  - Copied k1_nwscript.nss" -ForegroundColor Cyan
} else {
    Write-Host "  Warning: k1_nwscript.nss not found" -ForegroundColor Yellow
}

if (Test-Path $tslSource) {
    Copy-Item $tslSource $publishDir
    Write-Host "  - Copied tsl_nwscript.nss" -ForegroundColor Cyan
} elseif (Test-Path $tslRoot) {
    Copy-Item $tslRoot $publishDir
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
NCSDecomp CLI Distribution Package
Version: 2.0
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
Write-Host "================================" -ForegroundColor Green
Write-Host "Publishing complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Distribution package ready in: $publishDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Package contents:" -ForegroundColor Yellow
$publishDirResolved = (Resolve-Path $publishDir).Path
$pathSeparator = if ($IsWindows) { "\" } else { "/" }
Get-ChildItem $publishDir -Recurse | ForEach-Object {
    $relativePath = $_.FullName.Replace($publishDirResolved + $pathSeparator, "")
    $size = if ($_.PSIsContainer) { "<DIR>" } else { "$([math]::Round($_.Length / 1KB, 2)) KB" }
    Write-Host "  $relativePath ($size)" -ForegroundColor Gray
}

