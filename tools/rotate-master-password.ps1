param(
    [string]$Email = 'master@stowup.local',
    [string]$NewPassword
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
$psql = Join-Path $runtime 'postgresql\pgsql\bin\psql.exe'
$passwordFile = Join-Path $runtime 'pg-password.txt'
$backendEnvironment = Join-Path $runtime 'backend.env.ps1'
$accessFile = Join-Path $runtime 'ACESSO_LOCAL.txt'

if (-not (Test-Path -LiteralPath $psql)) { throw 'PostgreSQL local não encontrado. Execute INSTALAR.bat primeiro.' }
if (-not (Test-Path -LiteralPath $backendEnvironment)) { throw 'Configuração local não encontrada. Execute INSTALAR.bat primeiro.' }

& (Join-Path $PSScriptRoot 'start-postgres.ps1')

if ([string]::IsNullOrWhiteSpace($NewPassword)) {
    $buffer = New-Object byte[] 18
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($buffer) } finally { $generator.Dispose() }
    $newPassword = 'SU-' + [Convert]::ToBase64String($buffer).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')
} else {
    if ($NewPassword.Length -lt 12) { throw 'A senha mestre deve possuir pelo menos 12 caracteres.' }
    $newPassword = $NewPassword
}
$safePassword = $newPassword.Replace("'", "''")
$safeEmail = $Email.Trim().ToLowerInvariant().Replace("'", "''")
$env:PGPASSWORD = if ($env:STOWUP_DB_PASSWORD) {
    $env:STOWUP_DB_PASSWORD
} elseif (Test-Path -LiteralPath $passwordFile) {
    (Get-Content -LiteralPath $passwordFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
} else {
    'stowup'
}

$sql = @"
create extension if not exists pgcrypto;
update users
set password_hash = crypt('$safePassword', gen_salt('bf', 12))
where lower(email) = '$safeEmail'
returning id;
"@
$result = $sql | & $psql -X -q -v ON_ERROR_STOP=1 -h 127.0.0.1 -U stowup -d stowup -tA
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível atualizar a senha mestre no PostgreSQL.' }
if ([string]::IsNullOrWhiteSpace(($result | Out-String))) { throw "Conta mestre $Email não encontrada." }

$quotedPassword = "'" + $newPassword.Replace("'", "''") + "'"
$environmentLines = Get-Content -LiteralPath $backendEnvironment -Encoding utf8
$passwordSettingFound = $false
$environmentLines = $environmentLines | ForEach-Object {
    if ($_ -match '^\$env:STOWUP_MASTER_PASSWORD\s*=') {
        $passwordSettingFound = $true
        '$env:STOWUP_MASTER_PASSWORD = ' + $quotedPassword
    } else {
        $_
    }
}
if (-not $passwordSettingFound) { $environmentLines += '$env:STOWUP_MASTER_PASSWORD = ' + $quotedPassword }
$environmentLines | Set-Content -LiteralPath $backendEnvironment -Encoding utf8

$accessLines = if (Test-Path -LiteralPath $accessFile) {
    Get-Content -LiteralPath $accessFile -Encoding utf8
} else {
    @('STOWUP - ACESSO LOCAL', '', "Usuário mestre: $Email")
}
$accessPasswordFound = $false
$accessLines = $accessLines | ForEach-Object {
    if ($_ -match '^Senha:') {
        $accessPasswordFound = $true
        "Senha: $newPassword"
    } else {
        $_
    }
}
if (-not $accessPasswordFound) { $accessLines += "Senha: $newPassword" }
$accessLines | Set-Content -LiteralPath $accessFile -Encoding utf8

$env:PGPASSWORD = $null
$newPassword = $null
$safePassword = $null
Write-Host 'Senha mestre alterada com sucesso.' -ForegroundColor Green
Write-Host "Consulte a nova senha em: $accessFile"
