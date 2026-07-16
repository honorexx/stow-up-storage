$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$toolchain = Join-Path $root '.runtime\toolchain.ps1'
if (-not (Test-Path $toolchain)) { throw 'Execute INSTALAR.bat primeiro.' }
. $toolchain
$env:PATH = "$STOWUP_NODE_DIR;$env:PATH"
Set-Location (Join-Path $root 'mobile')
$pidFile = Join-Path $root '.runtime\expo.pid'
$currentProcess = Get-Process -Id $PID
Set-Content -LiteralPath $pidFile -Value "$PID|$($currentProcess.StartTime.ToUniversalTime().Ticks)" -Encoding ascii
try {
    & $STOWUP_PNPM_CMD start --lan
    exit $LASTEXITCODE
} finally {
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}
