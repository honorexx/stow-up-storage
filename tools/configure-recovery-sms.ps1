$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null

$sid = Read-Host 'Twilio Account SID'
$from = Read-Host 'Número remetente Twilio no formato +5511...'
$token = Read-Host 'Twilio Auth Token' -AsSecureString
if ([string]::IsNullOrWhiteSpace($sid) -or [string]::IsNullOrWhiteSpace($from)) {
    throw 'Informe o Account SID e o número remetente.'
}

Set-Content -LiteralPath (Join-Path $runtime 'twilio-account-sid.txt') -Value $sid.Trim() -Encoding utf8
Set-Content -LiteralPath (Join-Path $runtime 'twilio-from-number.txt') -Value $from.Trim() -Encoding utf8
$token | ConvertFrom-SecureString | Set-Content -LiteralPath (Join-Path $runtime 'twilio-auth-token.dpapi') -Encoding utf8

Write-Host 'Credenciais de SMS salvas com proteção do Windows.' -ForegroundColor Green
Write-Host 'Reiniciando o backend...'
& (Join-Path $PSScriptRoot 'start-backend.ps1')
