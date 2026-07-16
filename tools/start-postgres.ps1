$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$bin = Join-Path $root '.runtime\postgresql\pgsql\bin'
$data = Join-Path $root 'postgres-data'
$logs = Join-Path $root 'postgres-logs'

if (-not (Test-Path (Join-Path $bin 'pg_ctl.exe'))) {
    throw 'PostgreSQL portátil não encontrado. Execute tools\setup-postgres.ps1 primeiro.'
}
if (-not (Test-Path (Join-Path $data 'PG_VERSION'))) {
    throw 'Cluster PostgreSQL não inicializado. Execute tools\setup-postgres.ps1 primeiro.'
}

$pgCtl = Join-Path $bin 'pg_ctl.exe'
& $pgCtl -D $data status *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host 'PostgreSQL desta instalação já está ativo em 127.0.0.1:5432.' -ForegroundColor Green
    exit 0
}

$listener = Get-NetTCPConnection -LocalPort 5432 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    throw 'A porta 5432 está sendo usada por outro PostgreSQL. Encerre a outra instalação do StowUp antes de continuar.'
}

New-Item -ItemType Directory -Force $logs | Out-Null
& $pgCtl -D $data -l (Join-Path $logs 'postgres.log') -o '-p 5432' start
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o PostgreSQL.' }
Write-Host 'PostgreSQL StowUp iniciado em 127.0.0.1:5432.' -ForegroundColor Green
