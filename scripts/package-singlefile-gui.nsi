; NSIS Script for creating self-extracting single-file executable (GUI version)
; Extracts to temp directory and runs the application
; No installer UI - silent extraction and execution

; Application information
!define APP_NAME "NCSDecomp"
!define APP_VERSION "1.0.0"

; Compression
SetCompressor /SOLID lzma
SetCompressorDictSize 32

; Request execution level (no admin required)
RequestExecutionLevel user

; Silent mode (no UI)
SilentInstall silent
AutoCloseWindow true

; Output file
OutFile "${APP_NAME}-${APP_VERSION}-Windows.exe"

; Installer sections
Section "MainSection" SEC01
    ; Create unique temp directory for this execution
    GetTempFileName $0
    Delete $0
    CreateDirectory "$0"

    ; Set output path
    SetOutPath "$0"

    ; Extract all files from the app directory
    File /r "${APP_NAME}\*.*"

    ; Find and execute the main executable
    ; Try common locations
    IfFileExists "$0\${APP_NAME}\${APP_NAME}.exe" 0 +3
    Exec '"$0\${APP_NAME}\${APP_NAME}.exe"'
    Goto end

    IfFileExists "$0\${APP_NAME}.exe" 0 +3
    Exec '"$0\${APP_NAME}.exe"'
    Goto end

    ; Note: For GUI apps, we don't cleanup immediately
    ; The temp directory will be cleaned by Windows temp cleanup
    end:
SectionEnd
