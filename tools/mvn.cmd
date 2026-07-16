@echo off
setlocal
set "ROOT=%~dp0.."
for /f "usebackq delims=" %%I in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". '%ROOT%\.runtime\toolchain.ps1'; Write-Output $STOWUP_MVN_CMD"`) do set "MVN_CMD=%%I"
for /f "usebackq delims=" %%I in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". '%ROOT%\.runtime\toolchain.ps1'; Write-Output $STOWUP_JAVA_EXE"`) do set "JAVA_EXE=%%I"
if not defined MVN_CMD (
  echo Execute INSTALAR.bat primeiro.
  exit /b 1
)
for %%I in ("%JAVA_EXE%") do set "JAVA_BIN=%%~dpI"
for %%I in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fI"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%MVN_CMD%" %*
