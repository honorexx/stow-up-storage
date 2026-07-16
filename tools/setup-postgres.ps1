$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
$archive = Join-Path $runtime 'postgresql-17.10-2-windows-x64-binaries.zip'
$postgres = Join-Path $runtime 'postgresql'
$bin = Join-Path $postgres 'pgsql\bin'
$data = Join-Path $root 'postgres-data'
$password = $env:STOWUP_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) { $password = 'stowup' }

New-Item -ItemType Directory -Force $runtime | Out-Null
if (-not (Test-Path (Join-Path $bin 'initdb.exe'))) {
    if (-not (Test-Path $archive)) {
        Write-Host 'Baixando PostgreSQL 17 portátil...' -ForegroundColor Cyan
        & curl.exe -L --fail --output $archive 'https://get.enterprisedb.com/postgresql/postgresql-17.10-2-windows-x64-binaries.zip'
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao baixar PostgreSQL.' }
    }
    Write-Host 'Extraindo PostgreSQL...' -ForegroundColor Cyan
    Expand-Archive -LiteralPath $archive -DestinationPath $postgres -Force
}

if (-not (Test-Path (Join-Path $data 'PG_VERSION'))) {
    $pwFile = Join-Path $runtime 'pg-password.txt'
    Set-Content -LiteralPath $pwFile -Value $password -NoNewline
    $args = @('-D', $data, '--username=postgres', "--pwfile=$pwFile", '--auth=scram-sha-256', '--encoding=UTF8', '--locale=C')
    & (Join-Path $bin 'initdb.exe') @args
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao inicializar PostgreSQL.' }
}

& (Join-Path $PSScriptRoot 'start-postgres.ps1')
$env:PGPASSWORD = $password
$roleExists = & (Join-Path $bin 'psql.exe') -h 127.0.0.1 -U postgres -d postgres -tAc "select 1 from pg_roles where rolname='stowup'"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível acessar o PostgreSQL desta instalação.' }
if (($roleExists -join '').Trim() -ne '1') {
    & (Join-Path $bin 'psql.exe') -h 127.0.0.1 -U postgres -d postgres -v ON_ERROR_STOP=1 -c "create role stowup login password '$password';"
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o usuário local stowup.' }
}
$databaseExists = & (Join-Path $bin 'psql.exe') -h 127.0.0.1 -U postgres -d postgres -tAc "select 1 from pg_database where datname='stowup'"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar os bancos locais.' }
if (($databaseExists -join '').Trim() -ne '1') {
    & (Join-Path $bin 'createdb.exe') -h 127.0.0.1 -U postgres -O stowup stowup
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o banco local stowup.' }
}
Write-Host 'Banco stowup pronto.' -ForegroundColor Green
