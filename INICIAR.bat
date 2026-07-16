@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist ".runtime\toolchain.ps1" (
  echo Execute INSTALAR.bat primeiro.
  pause
  exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\start-backend.ps1"
if errorlevel 1 (
  pause
  exit /b 1
)
start "StowUp - Expo" powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start-expo.ps1"
start "StowUp - Central JavaFX" powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start-staff-desktop.ps1"
echo StowUp iniciado. Abra o Expo Go no celular.
pause
