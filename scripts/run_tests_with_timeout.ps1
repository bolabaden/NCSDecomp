# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

# PowerShell script to run tests with 2-minute timeout and profiling
# Cross-platform compatible (Windows, macOS, Linux)
param(
    [int]$TimeoutSeconds = 120
)

# Detect platform (PowerShell Core 6+ has automatic variables, fallback for older versions)
if ($PSVersionTable.PSVersion.Major -ge 6) {
    # Use automatic variables in PowerShell Core 6+
    # $IsWindows is automatically available
} else {
    # Fallback for Windows PowerShell 5.1
    if (-not (Test-Path variable:IsWindows)) { $script:IsWindows = $env:OS -eq "Windows_NT" }
}

$junitStandalone = Join-Path "." (Join-Path "lib" "junit-platform-console-standalone-1.10.0.jar")
if (-not (Test-Path $junitStandalone)) {
    Write-Host "Error: JUnit JAR not found at $junitStandalone" -ForegroundColor Red
    exit 1
}

# Ensure build directory exists and has compiled classes
$buildDir = Join-Path "." "build"
if (-not (Test-Path $buildDir)) {
    Write-Host "Error: Build directory not found. Run build.ps1 first." -ForegroundColor Red
    exit 1
}

# Find test file in Maven test directory
$testFile = Join-Path "." (Join-Path "src" (Join-Path "test" (Join-Path "java" (Join-Path "com" (Join-Path "kotor" (Join-Path "resource" (Join-Path "formats" (Join-Path "ncs" "NCSDecompCLIRoundTripTest.java"))))))))
if (-not (Test-Path $testFile)) {
    # Try alternative location
    $testFile = Join-Path "." (Join-Path "com" (Join-Path "kotor" (Join-Path "resource" (Join-Path "formats" (Join-Path "ncs" "NCSDecompCLIRoundTripTest.java")))))
    if (-not (Test-Path $testFile)) {
        Write-Host "Error: Test file not found" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Compiling test..."
$testSourceDir = Join-Path "." (Join-Path "src" (Join-Path "test" "java"))
$mainSourceDir = Join-Path "." (Join-Path "src" (Join-Path "main" "java"))
$pathSeparator = if ($IsWindows) { ";" } else { ":" }
$sourcepath = "$testSourceDir$pathSeparator$mainSourceDir"
$cp = "$buildDir$pathSeparator$junitStandalone$pathSeparator."
javac -cp $cp -d $buildDir -encoding UTF-8 -sourcepath $sourcepath $testFile 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!"
    exit 1
}

Write-Host "Running tests with $TimeoutSeconds second timeout and profiling..."
Write-Host "Profiling output will be in: test_profile.txt"

# Start the Java process with profiling
$job = Start-Job -ScriptBlock {
    param($cp, $junitStandalone)
    $env:JAVA_TOOL_OPTIONS = "-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=test_profile.txt -XX:+PrintCompilation"
    java -cp $cp -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=test_profile.txt org.junit.platform.console.ConsoleLauncher --class-path $cp --select-class com.kotor.resource.formats.ncs.NCSDecompCLIRoundTripTest 2>&1
} -ArgumentList $cp, $junitStandalone

# Wait for job with timeout
$result = Wait-Job -Job $job -Timeout $TimeoutSeconds

if ($null -eq $result) {
    Write-Host "`nTIMEOUT: Tests exceeded $TimeoutSeconds seconds. Killing process..."
    Stop-Job -Job $job
    Remove-Job -Job $job -Force
    exit 124
}

$output = Receive-Job -Job $job
Remove-Job -Job $job

# Output the results
$output | Write-Host

# Check exit code
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}


