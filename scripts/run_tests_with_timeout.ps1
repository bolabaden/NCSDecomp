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

# Ensure lib directory exists
$libDir = Join-Path "." "lib"
if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

# Try to find JUnit JAR (prefer exact version, fall back to any version)
$junitVersion = "1.10.0"
$junitJarName = "junit-platform-console-standalone-$junitVersion.jar"
$junitStandalone = Join-Path $libDir $junitJarName

if (-not (Test-Path $junitStandalone)) {
    # Try to find any version of junit-platform-console-standalone in lib/
    $existingJars = Get-ChildItem -Path $libDir -Filter "junit-platform-console-standalone-*.jar" -ErrorAction SilentlyContinue
    if ($existingJars) {
        # Use the latest version found (sort by name, which should sort versions correctly)
        $latestJar = $existingJars | Sort-Object Name -Descending | Select-Object -First 1
        $junitStandalone = $latestJar.FullName
        Write-Host "Found existing JUnit JAR: $($latestJar.Name)" -ForegroundColor Green
    } else {
        # Download from Maven Central
        Write-Host "JUnit JAR not found. Downloading from Maven Central..." -ForegroundColor Yellow
        $mavenUrl = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$junitVersion/$junitJarName"

        try {
            Write-Host "Downloading from: $mavenUrl" -ForegroundColor Gray
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $mavenUrl -OutFile $junitStandalone -UseBasicParsing
            $ProgressPreference = 'Continue'

            if (Test-Path $junitStandalone) {
                $fileSize = (Get-Item $junitStandalone).Length / 1MB
                Write-Host "Successfully downloaded JUnit JAR ($([math]::Round($fileSize, 2)) MB)" -ForegroundColor Green
            } else {
                Write-Host "Error: Download failed - file not found after download" -ForegroundColor Red
                exit 1
            }
        } catch {
            Write-Host "Error: Failed to download JUnit JAR from Maven Central" -ForegroundColor Red
            Write-Host "  URL: $mavenUrl" -ForegroundColor Gray
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Gray
            exit 1
        }
    }
}

# Ensure build directory exists and has compiled classes (Java/Maven idiomatic: target/classes)
$buildDir = Join-Path "." (Join-Path "target" "classes")
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

# Use -source 8 -target 8 for Java 8 compatibility regardless of installed JDK version
$compileOutput = javac -cp $cp -d $buildDir -encoding UTF-8 -source 8 -target 8 -sourcepath $sourcepath $testFile 2>&1
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Write-Host ""
    if ($compileOutput) {
        $compileOutput | Write-Host
    }
    exit 1
}

# Ensure test-work directory exists
$testWorkDir = "test-work"
if (-not (Test-Path $testWorkDir)) {
    New-Item -ItemType Directory -Path $testWorkDir -Force | Out-Null
}

$profileFile = Join-Path $testWorkDir "test_profile.txt"
Write-Host "Running tests with $TimeoutSeconds second timeout and profiling..." -ForegroundColor Cyan
Write-Host "Profiling output will be in: $profileFile" -ForegroundColor Gray
Write-Host ""

# Start the Java process with profiling
# Note: -XX:+PrintCompilation is removed to avoid stdout spam
# The compilation log is written to file instead
$job = Start-Job -ScriptBlock {
    param($cp, $profilePath)
    # Redirect stderr to null to suppress warnings, but keep stdout for test output
    $env:JAVA_TOOL_OPTIONS = "-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=$profilePath"
    java -cp $cp -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=$profilePath com.kotor.resource.formats.ncs.NCSDecompCLIRoundTripTest 2>$null
} -ArgumentList $cp, $profileFile

# Wait for job with timeout
$result = Wait-Job -Job $job -Timeout $TimeoutSeconds

if ($null -eq $result) {
    Write-Host "`nTIMEOUT: Tests exceeded $TimeoutSeconds seconds. Killing process..." -ForegroundColor Red
    Stop-Job -Job $job
    Remove-Job -Job $job -Force
    exit 124
}

$output = Receive-Job -Job $job
Remove-Job -Job $job

# Output the results
$output | Write-Host

# Check exit code
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    exit $exitCode
}

# Analyze profile if it exists
if (Test-Path $profileFile) {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "PROFILE ANALYSIS - Performance Bottlenecks" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan

    $profileContent = Get-Content $profileFile -ErrorAction SilentlyContinue
    if ($profileContent) {
        # Parse compilation log - extract methods with compilation info
        $methodData = @{}
        $totalCompilations = 0

        foreach ($line in $profileContent) {
            # Match compilation lines: <timestamp> <compile_id> <level> [flags] <size> <method>
            if ($line -match '^\s*(\d+)\s+(\d+)\s+(\d+)\s+([!%s]*)\s+(\d+)\s+(.+)') {
                $totalCompilations++
                $compileId = $matches[2]
                $level = [int]$matches[3]
                $flags = $matches[4]
                $size = [int]$matches[5]
                $method = $matches[6].Trim()

                # Only track C1/C2 compiled methods (level 1-4)
                if ($level -ge 1 -and $level -le 4) {
                    if (-not $methodData.ContainsKey($method)) {
                        $methodData[$method] = @{
                            Method = $method
                            MaxSize = $size
                            MaxLevel = $level
                            Compilations = 0
                            Flags = @()
                        }
                    }
                    $methodData[$method].Compilations++
                    if ($size -gt $methodData[$method].MaxSize) {
                        $methodData[$method].MaxSize = $size
                        $methodData[$method].MaxLevel = $level
                    }
                    if ($flags -and -not ($methodData[$method].Flags -contains $flags)) {
                        $methodData[$method].Flags += $flags
                    }
                }
            }
        }

        # Sort by size and compilation count to find bottlenecks
        $bottlenecks = $methodData.Values |
            Where-Object { $_.MaxSize -gt 100 -or $_.Compilations -gt 1 } |
            Sort-Object -Property @{Expression = {$_.MaxSize}; Descending = $true}, @{Expression = {$_.Compilations}; Descending = $true} |
            Select-Object -First 15

        if ($bottlenecks) {
            Write-Host ""
            Write-Host "Top Performance Bottlenecks:" -ForegroundColor Yellow
            Write-Host ""
            Write-Host ("{0,-60} {1,-8} {2,-6} {3,-10}" -f "Method", "Size", "Level", "Compiles") -ForegroundColor Gray
            Write-Host ("{0}" -f ("-" * 90)) -ForegroundColor Gray

            foreach ($method in $bottlenecks) {
                $levelStr = "C$($method.MaxLevel)"
                $flagStr = ""
                if ($method.Flags -contains '!') { $flagStr += "[OSR] " }
                if ($method.Flags -contains '%') { $flagStr += "[ON-STACK] " }
                if ($method.Flags -contains 's') { $flagStr += "[SYNC] " }

                $methodName = if ($method.Method.Length -gt 55) {
                    $method.Method.Substring(0, 52) + "..."
                } else {
                    $method.Method
                }

                Write-Host ("{0,-60} {1,6} B {2,-6} {3,2}x" -f $methodName, $method.MaxSize, $levelStr, $method.Compilations)
                if ($flagStr) {
                    Write-Host ("  {0}" -f $flagStr.Trim()) -ForegroundColor DarkGray
                }
            }
        }

        Write-Host ""
        Write-Host ("Total JIT compilations: {0}" -f $totalCompilations) -ForegroundColor Gray
        Write-Host ("Methods compiled: {0}" -f $methodData.Count) -ForegroundColor Gray

        # Find frequently recompiled methods (deoptimization issues)
        $recompiled = $methodData.Values | Where-Object { $_.Compilations -gt 2 } | Sort-Object -Property Compilations -Descending | Select-Object -First 5
        if ($recompiled) {
            Write-Host ""
            Write-Host "Methods with multiple compilations (possible deoptimization):" -ForegroundColor Yellow
            foreach ($method in $recompiled) {
                Write-Host ("  {0} - compiled {1} times" -f $method.Method, $method.Compilations) -ForegroundColor DarkYellow
            }
        }
    } else {
        Write-Host "Profile file is empty or could not be read." -ForegroundColor Yellow
    }

    Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Full compilation log: $profileFile" -ForegroundColor DarkGray
}


