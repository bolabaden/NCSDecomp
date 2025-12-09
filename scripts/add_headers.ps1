# Script to add Business Source License 1.1 headers to all source files

$javaHeader = @"
// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

"@

$ps1Header = @"
# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
# See LICENSE.txt file in the project root for full license information.

"@

$batHeader = @"
@REM Copyright 2021-2025 NCSDecomp
@REM Licensed under the Business Source License 1.1 (BSL 1.1).
@REM See LICENSE.txt file in the project root for full license information.

"@

$mdHeader = @"
<!-- Copyright 2021-2025 NCSDecomp -->
<!-- Licensed under the Business Source License 1.1 (BSL 1.1). -->
<!-- See LICENSE.txt file in the project root for full license information. -->

"@

$txtHeader = @"
Copyright 2021-2025 NCSDecomp
Licensed under the Business Source License 1.1 (BSL 1.1).
See LICENSE.txt file in the project root for full license information.

"@

$cfgHeader = @"
# Copyright 2021-2025 NCSDecomp
# Licensed under the Business Source License 1.1 (BSL 1.1).
# See LICENSE.txt file in the project root for full license information.

"@

function Add-HeadersToFiles {
    param(
        [string]$fileExtension,
        [string]$header,
        [string]$fileType
    )

    Write-Host "Finding $fileType files missing copyright headers..."

    $files = Get-ChildItem -Path . -Filter *.$fileExtension -Recurse -File |
        Where-Object {
            $_.FullName -notlike '*\obj\*' -and
            $_.FullName -notlike '*\bin\*' -and
            $_.FullName -notlike '*\build\*' -and
            $_.FullName -notlike '*\dist-exe\*' -and
            $_.FullName -notlike '*\publish\*' -and
            $_.FullName -notlike '*\runtime\*' -and
            $_.FullName -notlike '*\.history\*' -and
            $_.FullName -notlike '*\vendor\*' -and
            $_.FullName -notlike '*\grammar\*' -and
            $_.FullName -notlike '*\META-INF\*'
        } |
        ForEach-Object {
            $content = Get-Content $_.FullName -First 10 -ErrorAction SilentlyContinue | Out-String
            if ($content -and $content -notmatch 'Copyright 2021-2025 NCSDecomp') {
                $_
            }
        }

    $fileCount = @($files).Count

    if ($fileCount -eq 0) {
        Write-Host "No $fileType files found missing headers."
        return 0
    }

    Write-Host "Found $fileCount $fileType file(s) missing headers. Adding headers now...`n"

    foreach ($file in $files) {
        if ($file) {
            $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
            Write-Host "Adding header to: $relativePath"
            $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
            if ($content) {
                Set-Content -Path $file.FullName -Value ($header + $content) -NoNewline
            }
        }
    }

    Write-Host "Done! Added headers to $fileCount $fileType file(s).`n"
    return $fileCount
}

# Process Java files
$javaCount = Add-HeadersToFiles -fileExtension "java" -header $javaHeader -fileType "Java"

# Process PowerShell files
$ps1Count = Add-HeadersToFiles -fileExtension "ps1" -header $ps1Header -fileType "PowerShell"

# Process Batch files
$batCount = Add-HeadersToFiles -fileExtension "bat" -header $batHeader -fileType "Batch"

# Process Markdown files
$mdCount = Add-HeadersToFiles -fileExtension "md" -header $mdHeader -fileType "Markdown"

# Process Text files
$txtCount = Add-HeadersToFiles -fileExtension "txt" -header $txtHeader -fileType "Text"

# Process Config files
$cfgCount = Add-HeadersToFiles -fileExtension "cfg" -header $cfgHeader -fileType "Config"

$totalCount = $javaCount + $ps1Count + $batCount + $mdCount + $txtCount + $cfgCount

if ($totalCount -eq 0) {
    Write-Host "`nAll files have headers. Nothing to do!"
} else {
    Write-Host "`nTotal: Added headers to $totalCount file(s)."
}


