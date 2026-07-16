$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'

function Stop-TrackedProcess([string]$FileName, [string[]]$AllowedNames, [switch]$Tree) {
    $path = Join-Path $runtime $FileName
    if (-not (Test-Path -LiteralPath $path)) { return }
    $parts = ((Get-Content -LiteralPath $path -ErrorAction SilentlyContinue | Select-Object -First 1) -split '\|')
    if ($parts.Count -lt 2) {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        return
    }
    $process = Get-Process -Id ([int]$parts[0]) -ErrorAction SilentlyContinue
    if ($process -and $AllowedNames -contains $process.ProcessName -and
        $process.StartTime.ToUniversalTime().Ticks -eq [long]$parts[1]) {
        if ($Tree) {
            & taskkill.exe /PID $process.Id /T /F *> $null
        } else {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
}

Stop-TrackedProcess 'backend.pid' @('java', 'javaw')
Stop-TrackedProcess 'expo.pid' @('powershell', 'pwsh') -Tree
Stop-TrackedProcess 'staff.pid' @('powershell', 'pwsh') -Tree

& (Join-Path $PSScriptRoot 'stop-postgres.ps1')
Write-Host 'StowUp encerrado.' -ForegroundColor Green
