# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI
# Compiles all Java files and creates a self-contained JAR
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

Write-Host "Building NCSDecomp CLI..." -ForegroundColor Green
if ($IsWindows) {
    Write-Host "Platform: Windows" -ForegroundColor Gray
} elseif ($IsMacOS) {
    Write-Host "Platform: macOS" -ForegroundColor Gray
} elseif ($IsLinux) {
    Write-Host "Platform: Linux" -ForegroundColor Gray
}

# Clean build directory and JAR file for idempotent builds
$buildDir = Join-Path "." "build"
$jarFile = Join-Path "." "NCSDecomp-CLI.jar"

Write-Host "Cleaning previous build artifacts..." -ForegroundColor Yellow
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
}
if (Test-Path $jarFile) {
    Remove-Item -Force $jarFile -ErrorAction SilentlyContinue
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
    if (Test-Path "META-INF\MANIFEST.MF") {
        $manifestContent = Get-Content "META-INF\MANIFEST.MF" -Raw
        if ($manifestContent -notmatch "Main-Class: com\.kotor\.resource\.formats\.ncs\.NCSDecompCLI") {
            Write-Host "WARNING: Manifest may have incorrect main class!" -ForegroundColor Yellow
            Write-Host "Manifest content:" -ForegroundColor Yellow
            Get-Content "META-INF\MANIFEST.MF" | Write-Host
        } else {
            Write-Host "Verified manifest has correct main class" -ForegroundColor Gray
        }
        Remove-Item -Recurse -Force "META-INF" -ErrorAction SilentlyContinue
    }

    Write-Host "JAR created successfully with all classes and resources" -ForegroundColor Gray
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


