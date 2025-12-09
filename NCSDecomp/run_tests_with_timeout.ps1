# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# See LICENSE.txt file in the project root for full license information.

# PowerShell script to run tests with 2-minute timeout and profiling
param(
    [int]$TimeoutSeconds = 120
)

$junitStandalone = "..\lib\junit-platform-console-standalone-1.10.0.jar"
$cp = "build;$junitStandalone;."

Write-Host "Compiling test..."
javac -cp $cp -d build -encoding UTF-8 com.kotor.resource.formats.ncs/NCSDecompCLIRoundTripTest.java 2>&1 | Out-Null

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
    java -cp $cp -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=test_profile.txt com.kotor.resource.formats.ncs.NCSDecompCLIRoundTripTest 2>&1
} -ArgumentList $cp, $junitStandalone

# Wait for job with timeout
$result = Wait-Job -Job $job -Timeout $TimeoutSeconds

if ($result -eq $null) {
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


