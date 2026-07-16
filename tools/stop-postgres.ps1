$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$bin = Join-Path $root '.runtime\postgresql\pgsql\bin'
$data = Join-Path $root 'postgres-data'

if (-not (Test-Path (Join-Path $data 'PG_VERSION'))) { Write-Host 'Cluster PostgreSQL não encontrado.'; exit 0 }
$pgCtl = Join-Path $bin 'pg_ctl.exe'
& $pgCtl -D $data status *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'PostgreSQL desta instalação já está parado.'
    exit 0
}
& $pgCtl -D $data stop -m fast
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível parar o PostgreSQL.' }
Write-Host 'PostgreSQL StowUp encerrado.' -ForegroundColor Green
