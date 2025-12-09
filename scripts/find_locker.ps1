<#
.SYNOPSIS
    Unlocks and moves/renames a folder by terminating blocking processes.

.DESCRIPTION
    Attempts to move or rename a folder. If the operation fails due to file locks,
    identifies and terminates processes that may be locking the folder, trying
    one process at a time until the operation succeeds.

.PARAMETER Source
    Path to the source folder to move/rename. Required.

.PARAMETER Destination
    Path to the destination folder. Required for move operations.

.PARAMETER Delete
    If specified, deletes the source folder instead of moving it. Cannot be used with -Destination.

.PARAMETER DryRun
    Shows what would be done without actually performing the operation.

.PARAMETER Verbose
    Provides detailed output about the process.

.PARAMETER ExcludeProcess
    Process names to exclude from termination (can be specified multiple times).

.PARAMETER IncludeProcess
    Process names to specifically include in termination attempts (can be specified multiple times).

.PARAMETER WaitMs
    Milliseconds to wait after terminating a process before retrying the operation. Default: 500.

.EXAMPLE
    .\find_locker.ps1 -Source "C:\MyFolder" -Destination "C:\NewFolder"
    Moves C:\MyFolder to C:\NewFolder, terminating blocking processes if needed.

.EXAMPLE
    .\find_locker.ps1 -Source "C:\MyFolder" -Delete
    Deletes C:\MyFolder, terminating blocking processes if needed.

.EXAMPLE
    .\find_locker.ps1 -Source "C:\MyFolder" -Destination "C:\NewFolder" -DryRun
    Shows what would be done without actually performing the operation.

.EXAMPLE
    .\find_locker.ps1 -Source "C:\MyFolder" -Destination "C:\NewFolder" -ExcludeProcess "explorer","svchost"
    Moves the folder but excludes explorer and svchost from termination.
#>

[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateScript({Test-Path $_ -PathType Container})]
    [string]$Source,

    [Parameter(Mandatory=$false)]
    [string]$Destination,

    [Parameter(Mandatory=$false)]
    [switch]$Delete,

    [Parameter(Mandatory=$false)]
    [switch]$DryRun,

    [Parameter(Mandatory=$false)]
    [string[]]$ExcludeProcess = @(),

    [Parameter(Mandatory=$false)]
    [string[]]$IncludeProcess = @(),

    [Parameter(Mandatory=$false)]
    [int]$WaitMs = 500
)

# Validate parameters
if ($Delete -and $Destination) {
    Write-Error "Cannot specify both -Delete and -Destination. Use -Delete to remove the folder, or -Destination to move it."
    exit 1
}

if (-not $Delete -and -not $Destination) {
    Write-Error "Must specify either -Destination (to move) or -Delete (to remove)."
    exit 1
}

# Resolve paths
$Source = Resolve-Path $Source -ErrorAction Stop
if ($Destination) {
    $Destination = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Destination)
}

function Test-FolderOperation {
    param(
        [string]$SourcePath,
        [string]$DestPath = $null,
        [bool]$IsDelete = $false,
        [bool]$WhatIf = $false
    )

    try {
        if ($IsDelete) {
            if (-not $WhatIf) {
                Remove-Item -Path $SourcePath -Force -Recurse -ErrorAction Stop
            }
            return $true
        } else {
            if (-not $WhatIf) {
                # If destination exists and has content, check if source is empty
                if (Test-Path $DestPath) {
                    $destItems = Get-ChildItem $DestPath -Force -ErrorAction SilentlyContinue
                    if ($destItems.Count -gt 0) {
                        $sourceItems = Get-ChildItem $SourcePath -Force -ErrorAction SilentlyContinue
                        if ($sourceItems.Count -eq 0) {
                            # Source is empty, just remove it
                            Remove-Item -Path $SourcePath -Force -ErrorAction Stop
                            return $true
                        } else {
                            Write-Verbose "Both folders have content. Cannot move."
                            return $false
                        }
                    }
                }
                Move-Item -Path $SourcePath -Destination $DestPath -Force -ErrorAction Stop
            }
            return $true
        }
    } catch {
        Write-Verbose "Operation failed: $($_.Exception.Message)"
        return $false
    }
}

function Get-CandidateProcesses {
    param(
        [string]$FolderPath,
        [string[]]$Exclude = @(),
        [string[]]$Include = @()
    )

    $candidates = @()
    $normalizedFolder = $FolderPath.TrimEnd('\', '/')
    $folderName = Split-Path $FolderPath -Leaf
    
    Write-Verbose "Searching for processes that might have '$normalizedFolder' open..."

    # Get all processes with their command lines using CIM
    try {
        $allProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | 
            Where-Object { $null -ne $_ }
        
        foreach ($proc in $allProcesses) {
            try {
                $procName = $proc.Name -replace '\.exe$', ''
                $commandLine = $proc.CommandLine
                $executablePath = $proc.ExecutablePath
                
                # Skip if excluded
                $isExcluded = $false
                if ($Exclude.Count -gt 0) {
                    foreach ($excludeName in $Exclude) {
                        if ($procName -like $excludeName) {
                            $isExcluded = $true
                            break
                        }
                    }
                }
                if ($isExcluded) { continue }
                
                # Check if folder path appears in command line
                $matchesFolder = $false
                if ($commandLine -and $commandLine -like "*$normalizedFolder*") {
                    $matchesFolder = $true
                    Write-Verbose "Found process '$procName' (PID: $($proc.ProcessId)) with folder in command line"
                }
                
                # Check if process executable is in the folder
                if ($executablePath -and $executablePath -like "*$normalizedFolder*") {
                    $matchesFolder = $true
                    Write-Verbose "Found process '$procName' (PID: $($proc.ProcessId)) running from folder"
                }
                
                # Check if folder name appears in command line (for IDEs/editors that might have workspace open)
                if ($commandLine -and $commandLine -like "*$folderName*") {
                    $matchesFolder = $true
                    Write-Verbose "Found process '$procName' (PID: $($proc.ProcessId)) with folder name in command line"
                }
                
                if ($matchesFolder) {
                    # Get the process object
                    $procObj = Get-Process -Id $proc.ProcessId -ErrorAction SilentlyContinue
                    if ($procObj) {
                        $candidates += $procObj
                    }
                }
            } catch {
                Write-Verbose "Error processing process $($proc.ProcessId): $($_.Exception.Message)"
            }
        }
    } catch {
        Write-Verbose "Could not query WMI processes: $($_.Exception.Message)"
    }
    
    # Also check processes by name (for common IDEs/editors that might have folders open)
    # This is a fallback if WMI doesn't show the folder in command line
    $commonProcesses = @('Cursor', 'Code', 'devenv', 'idea64', 'idea', 'pycharm64', 'pycharm', 
                         'WebStorm', 'studio64', 'studio', 'explorer', 'java', 'javaw')
    
    foreach ($procName in $commonProcesses) {
        try {
            $procs = Get-Process -Name $procName -ErrorAction SilentlyContinue
            if ($procs) {
                foreach ($proc in $procs) {
                    # Skip if already in candidates
                    if ($candidates | Where-Object { $_.Id -eq $proc.Id }) {
                        continue
                    }
                    
                    # Skip if excluded
                    $isExcluded = $false
                    if ($Exclude.Count -gt 0) {
                        foreach ($excludeName in $Exclude) {
                            if ($proc.Name -like $excludeName) {
                                $isExcluded = $true
                                break
                            }
                        }
                    }
                    if (-not $isExcluded) {
                        $candidates += $proc
                        Write-Verbose "Added common process '$($proc.Name)' (PID: $($proc.Id)) as candidate"
                    }
                }
            }
        } catch {
            Write-Verbose "Error checking process $procName : $($_.Exception.Message)"
        }
    }

    # If specific processes are included, add them
    if ($Include.Count -gt 0) {
        foreach ($procName in $Include) {
            try {
                $procs = Get-Process -Name $procName -ErrorAction SilentlyContinue
                if ($procs) {
                    foreach ($proc in $procs) {
                        # Skip if already in candidates
                        if (-not ($candidates | Where-Object { $_.Id -eq $proc.Id })) {
                            $candidates += $proc
                            Write-Verbose "Added included process '$($proc.Name)' (PID: $($proc.Id))"
                        }
                    }
                }
            } catch {
                Write-Verbose "Error including process $procName : $($_.Exception.Message)"
            }
        }
    }

    # Filter out excluded processes (final pass)
    if ($Exclude.Count -gt 0) {
        $candidates = $candidates | Where-Object {
            $excluded = $false
            foreach ($excludeName in $Exclude) {
                if ($_.Name -like $excludeName) {
                    $excluded = $true
                    break
                }
            }
            -not $excluded
        }
    }

    # Remove duplicates and return unique processes, sorted by ID
    return $candidates | Sort-Object Id -Unique
}

# Main execution
$operation = if ($Delete) { "delete" } else { "move" }

Write-Host "Attempting to $operation folder..." -ForegroundColor Cyan
Write-Host "  Source: $Source" -ForegroundColor Gray
if (-not $Delete) {
    Write-Host "  Destination: $Destination" -ForegroundColor Gray
}
if ($DryRun) {
    Write-Host "  Mode: DRY RUN (no changes will be made)" -ForegroundColor Yellow
}
Write-Host ""

# First, try the operation without terminating anything
Write-Host "[1] Testing $operation without terminating any processes..." -ForegroundColor Cyan
$testResult = Test-FolderOperation -SourcePath $Source -DestPath $Destination -IsDelete $Delete -WhatIf:$DryRun

if ($testResult) {
    if (-not $DryRun) {
        Write-Host "SUCCESS: Folder $operation completed without terminating any processes!" -ForegroundColor Green
        exit 0
    } else {
        Write-Host "SUCCESS: Operation would succeed without terminating any processes!" -ForegroundColor Green
        exit 0
    }
}

Write-Host "Operation failed. Identifying candidate processes to terminate..." -ForegroundColor Yellow
Write-Host ""

# Get candidate processes
$candidates = Get-CandidateProcesses -FolderPath $Source -Exclude $ExcludeProcess -Include $IncludeProcess

if ($candidates.Count -eq 0) {
    Write-Host "No candidate processes found. The folder may be locked by:" -ForegroundColor Red
    Write-Host "  - A system process (may require administrator privileges)" -ForegroundColor Red
    Write-Host "  - A process not accessible to this user" -ForegroundColor Red
    Write-Host "  - A file handle that cannot be identified" -ForegroundColor Red
    Write-Host ""
    Write-Host "You may need to:" -ForegroundColor Yellow
    Write-Host "  - Close applications manually" -ForegroundColor Yellow
    Write-Host "  - Run this script as administrator" -ForegroundColor Yellow
    Write-Host "  - Restart your computer" -ForegroundColor Yellow
    exit 1
}

Write-Host "Found $($candidates.Count) candidate process(es) to try terminating:" -ForegroundColor Cyan
$candidates | Format-Table Id, Name, @{Label="Path";Expression={if($_.Path){$_.Path}else{"N/A"}}} -AutoSize
Write-Host ""

if ($DryRun) {
    Write-Host "DRY RUN: Would terminate processes one by one until operation succeeds." -ForegroundColor Yellow
    exit 0
}

# Try terminating processes one by one
$terminatedCount = 0
foreach ($proc in $candidates) {
    $terminatedCount++
    Write-Host "[$($terminatedCount + 1)] Terminating process: $($proc.Name) (PID: $($proc.Id))" -ForegroundColor Yellow

    try {
        if ($PSCmdlet.ShouldProcess("Process $($proc.Name) (PID: $($proc.Id))", "Terminate")) {
            if (-not $DryRun) {
                Stop-Process -Id $proc.Id -Force -ErrorAction Stop
                Write-Verbose "Process terminated."
            } else {
                Write-Verbose "Would terminate process (dry run)."
            }
        }

        # Wait for handles to be released
        Start-Sleep -Milliseconds $WaitMs

        # Try the operation again
        Write-Verbose "Testing $operation after termination..."
        $testResult = Test-FolderOperation -SourcePath $Source -DestPath $Destination -IsDelete $Delete -WhatIf:$DryRun

        if ($testResult) {
            Write-Host "SUCCESS: Folder $operation completed after terminating $terminatedCount process(es)!" -ForegroundColor Green
            $terminatedProcs = $candidates[0..($terminatedCount-1)] | ForEach-Object { "$($_.Name) (PID: $($_.Id))" }
            Write-Host "Terminated process(es): $($terminatedProcs -join ', ')" -ForegroundColor Gray
            exit 0
        } else {
            Write-Verbose "Operation still failing, trying next process..."
        }
    } catch {
        Write-Host "    Failed to terminate process: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

# If we get here, all processes were terminated but operation still fails
Write-Host ""
Write-Host "FAILED: Terminated $terminatedCount process(es) but folder is still locked." -ForegroundColor Red
Write-Host "The folder may be locked by:" -ForegroundColor Red
Write-Host "  - A system process that cannot be terminated" -ForegroundColor Red
Write-Host "  - A kernel-mode driver or service" -ForegroundColor Red
Write-Host "  - A file handle that requires a system restart to release" -ForegroundColor Red
exit 1
