# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# See LICENSE.txt file in the project root for full license information.

# Build script for NCSDecomp CLI
# Compiles all Java files and creates a self-contained JAR

$ErrorActionPreference = "Stop"

Write-Host "Building NCSDecomp CLI..." -ForegroundColor Green

# Clean build directory
if (Test-Path "build") {
    Write-Host "Cleaning build directory..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "build"
}
New-Item -ItemType Directory -Path "build" -Force | Out-Null

# Collect all Java files
Write-Host "Collecting Java source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Recurse -Filter "*.java" -Path "com" | ForEach-Object { $_.FullName }
$javaFiles = $javaFiles | Where-Object { $_ -notlike "*Test.java" }  # Exclude test files

if ($javaFiles.Count -eq 0) {
    Write-Host "Error: No Java files found!" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($javaFiles.Count) Java files to compile" -ForegroundColor Cyan

# Compile Java files
Write-Host "Compiling Java files..." -ForegroundColor Yellow
$sourceFiles = $javaFiles -join " "
$compileCommand = "javac -d build -encoding UTF-8 $sourceFiles"

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
$manifestDir = "build\META-INF"
New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null
@"
Manifest-Version: 1.0
Created-By: NCSDecomp Build Script
Main-Class: com.kotor.resource.formats.ncs.NCSDecompCLI

"@ | Out-File -FilePath "$manifestDir\MANIFEST.MF" -Encoding ASCII

# Create JAR file
Write-Host "Creating JAR file..." -ForegroundColor Yellow
if (Test-Path "NCSDecomp-CLI.jar") {
    Remove-Item -Force "NCSDecomp-CLI.jar"
}

Push-Location build
try {
    jar cfm ..\NCSDecomp-CLI.jar META-INF\MANIFEST.MF com
    if ($LASTEXITCODE -ne 0) {
        throw "JAR creation failed"
    }
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


