@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Instalando o ambiente local do StowUp...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\bootstrap-windows.ps1"
if errorlevel 1 (
  echo.
  echo A instalação encontrou um erro. Leia a mensagem acima.
  pause
  exit /b 1
)
echo.
pause
