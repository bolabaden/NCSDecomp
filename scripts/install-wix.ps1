# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# Automatic WiX Installation Script
# Installs WiX Toolset via winget (requires admin privileges)

Write-Host "Installing WiX Toolset for jpackage .exe creation..." -ForegroundColor Green
Write-Host ""

# Check if WiX is already installed
$wixInstalled = Get-Command "light.exe" -ErrorAction SilentlyContinue
if ($wixInstalled) {
    Write-Host "WiX is already installed at: $($wixInstalled.Source)" -ForegroundColor Green
    exit 0
}

# Check for winget
$wingetPath = Get-Command "winget.exe" -ErrorAction SilentlyContinue
if (-not $wingetPath) {
    Write-Host "Error: winget not found. Please install WiX manually from:" -ForegroundColor Red
    Write-Host "https://wixtoolset.org" -ForegroundColor Cyan
    exit 1
}

Write-Host "Installing WiX Toolset via winget..." -ForegroundColor Yellow
Write-Host "Note: Administrator privileges required. You will be prompted for elevation." -ForegroundColor Cyan
Write-Host ""

# Install WiX
try {
    $process = Start-Process -FilePath "winget" -ArgumentList "install", "--id", "WiXToolset.WiXToolset", "--accept-package-agreements", "--accept-source-agreements" -Verb RunAs -Wait -PassThru

    if ($process.ExitCode -eq 0) {
        Write-Host ""
        Write-Host "WiX installation completed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Please restart your PowerShell terminal for PATH changes to take effect." -ForegroundColor Yellow
        Write-Host "Then run: .\scripts\build.ps1 -BuildExe" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "WiX installation failed or was cancelled (exit code: $($process.ExitCode))" -ForegroundColor Red
        Write-Host "You can install WiX manually from: https://wixtoolset.org" -ForegroundColor Cyan
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "Error installing WiX: $_" -ForegroundColor Red
    Write-Host "You can install WiX manually from: https://wixtoolset.org" -ForegroundColor Cyan
    exit 1
}


