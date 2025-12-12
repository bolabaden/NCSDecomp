#!/usr/bin/env pwsh
# Script to run NCSDecomp CLI
# Usage: ./scripts/run_cli.ps1 [options] <files/dirs>
# Example: ./scripts/run_cli.ps1 -i input.ncs -o output.nss -g k1

$ErrorActionPreference = "Stop"

# Compile main classes
Write-Host "Compiling..." -ForegroundColor Cyan
mvn compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

# Run CLI with all passed arguments
Write-Host "Running NCSDecomp CLI..." -ForegroundColor Cyan
Write-Host ""

java -cp "target/classes;lib/*" `
    com.kotor.resource.formats.ncs.NCSDecompCLI `
    $args

exit $LASTEXITCODE

