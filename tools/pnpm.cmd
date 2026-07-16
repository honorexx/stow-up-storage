@echo off
setlocal
set "ROOT=%~dp0.."
for /f "usebackq delims=" %%I in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". '%ROOT%\.runtime\toolchain.ps1'; Write-Output $STOWUP_NODE_DIR"`) do set "NODE_DIR=%%I"
for /f "usebackq delims=" %%I in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". '%ROOT%\.runtime\toolchain.ps1'; Write-Output $STOWUP_PNPM_CMD"`) do set "PNPM_CMD=%%I"
if not defined PNPM_CMD (
  echo Execute INSTALAR.bat primeiro.
  exit /b 1
)
set "PATH=%NODE_DIR%;%PATH%"
call "%PNPM_CMD%" %*
