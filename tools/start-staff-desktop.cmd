@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-staff-desktop.ps1"
if errorlevel 1 pause
