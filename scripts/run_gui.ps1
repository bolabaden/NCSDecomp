#!/usr/bin/env pwsh
# Script to run NCSDecomp GUI
# Usage: ./scripts/run_gui.ps1

$ErrorActionPreference = "Stop"

# Compile main classes
Write-Host "Compiling..." -ForegroundColor Cyan
mvn compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

# Run GUI
Write-Host "Starting NCSDecomp GUI..." -ForegroundColor Cyan
Write-Host ""

java -cp "target/classes;lib/*" `
    com.kotor.resource.formats.ncs.Decompiler

exit $LASTEXITCODE

