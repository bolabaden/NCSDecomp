# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI
# Compiles all Java files and creates a self-contained JAR
# Cross-platform compatible (Windows, macOS, Linux)

$ErrorActionPreference = "Stop"

# Detect platform (PowerShell Core 6+)
$IsWindows = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsWindows } else { $env:OS -eq "Windows_NT" }
$IsLinux = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsLinux } else { $false }
$IsMacOS = if ($PSVersionTable.PSVersion.Major -ge 6) { $IsMacOS } else { $false }

Write-Host "Building NCSDecomp CLI..." -ForegroundColor Green
if ($IsWindows) {
    Write-Host "Platform: Windows" -ForegroundColor Gray
} elseif ($IsMacOS) {
    Write-Host "Platform: macOS" -ForegroundColor Gray
} elseif ($IsLinux) {
    Write-Host "Platform: Linux" -ForegroundColor Gray
}

# Clean build directory
$buildDir = Join-Path "." "build"
if (Test-Path $buildDir) {
    Write-Host "Cleaning build directory..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $buildDir
}
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

# Collect all Java files from Maven source directory
Write-Host "Collecting Java source files..." -ForegroundColor Yellow
$javaSourceDir = Join-Path "." (Join-Path "src" (Join-Path "main" "java"))
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
$sourceFiles = $javaFiles -join " "
$compileCommand = "javac -d $buildDir -encoding UTF-8 -sourcepath `"$javaSourceDir`" $sourceFiles"

try {
    Invoke-Expression $compileCommand
    if ($LASTEXITCODE -ne 0) {
        throw "Compilation failed with exit code $LASTEXITCODE"
    }
    Write-Host "Compilation successful!" -ForegroundColor Green
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
$jarFile = Join-Path "." "NCSDecomp-CLI.jar"
if (Test-Path $jarFile) {
    Remove-Item -Force $jarFile
}

Push-Location $buildDir
try {
    # Create JAR with all compiled classes, resources, and META-INF
    # This includes everything in the build directory with proper structure
    $manifestPath = Join-Path "META-INF" "MANIFEST.MF"
    $parentJar = if ($IsWindows) { "..\NCSDecomp-CLI.jar" } else { "../NCSDecomp-CLI.jar" }
    jar cfm $parentJar $manifestPath *
    if ($LASTEXITCODE -ne 0) {
        throw "JAR creation failed"
    }
    Write-Host "JAR created with all classes and resources" -ForegroundColor Gray
} finally {
    Pop-Location
}

Write-Host "Build complete! Created NCSDecomp-CLI.jar" -ForegroundColor Green
Write-Host ""
Write-Host "Usage examples:" -ForegroundColor Cyan
Write-Host "  java -jar NCSDecomp-CLI.jar -i input.ncs"
Write-Host "  java -jar NCSDecomp-CLI.jar -i input.ncs --stdout"
Write-Host "  java -jar NCSDecomp-CLI.jar -i scripts_dir -r --k2 -O output_dir"
Write-Host "  java -jar NCSDecomp-CLI.jar --help"


