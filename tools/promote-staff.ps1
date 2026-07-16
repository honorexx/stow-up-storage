param(
    [Parameter(Mandatory = $true)][string]$Email,
    [ValidateSet('ADMIN', 'SUPPORT')][string]$Role = 'SUPPORT'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$psql = Join-Path $root '.runtime\postgresql\pgsql\bin\psql.exe'
if (-not (Test-Path $psql)) { throw 'PostgreSQL local não encontrado.' }
$safeEmail = $Email.Trim().ToLowerInvariant().Replace("'", "''")
$passwordFile = Join-Path $root '.runtime\pg-password.txt'
$env:PGPASSWORD = if ($env:STOWUP_DB_PASSWORD) { $env:STOWUP_DB_PASSWORD } elseif (Test-Path $passwordFile) { (Get-Content $passwordFile -Raw).Trim() } else { 'stowup' }
$result = & $psql -h 127.0.0.1 -U stowup -d stowup -tAc "update users set role='$Role' where lower(email)='$safeEmail' returning email || ' -> ' || role;"
if ([string]::IsNullOrWhiteSpace($result)) { throw "Conta $Email não encontrada. Cadastre a identidade antes de promover." }
Write-Host $result.Trim() -ForegroundColor Green
