$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$toolchain = Join-Path $root '.runtime\toolchain.ps1'
if (-not (Test-Path $toolchain)) { throw 'Execute INSTALAR.bat primeiro.' }
. $toolchain
$env:JAVA_HOME = Split-Path (Split-Path $STOWUP_JAVA_EXE -Parent) -Parent
$env:PATH = "$env:JAVA_HOME\bin;$(Split-Path $STOWUP_MVN_CMD -Parent);$env:PATH"
$env:STOWUP_API_URL = 'http://localhost:8080/api'
Set-Location (Join-Path $root 'staff-desktop')
$pidFile = Join-Path $root '.runtime\staff.pid'
$currentProcess = Get-Process -Id $PID
Set-Content -LiteralPath $pidFile -Value "$PID|$($currentProcess.StartTime.ToUniversalTime().Ticks)" -Encoding ascii
try {
    & $STOWUP_MVN_CMD javafx:run
    exit $LASTEXITCODE
} finally {
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}
