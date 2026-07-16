$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null

Write-Host 'Use somente as credenciais de TESTE nesta etapa.' -ForegroundColor Yellow
$publicKey = Read-Host 'Public Key de teste do Mercado Pago'
$accessToken = Read-Host 'Access Token de teste do Mercado Pago' -AsSecureString

if ([string]::IsNullOrWhiteSpace($publicKey)) {
    throw 'Informe a Public Key de teste.'
}

$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($accessToken)
try {
    $plainToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    if ([string]::IsNullOrWhiteSpace($plainToken) -or -not $plainToken.StartsWith('APP_USR-')) {
        throw 'O Access Token informado não possui o formato esperado do Mercado Pago (APP_USR-...).'
    }
} finally {
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    $plainToken = $null
}

Set-Content -LiteralPath (Join-Path $runtime 'mercado-pago-public-key.txt') -Value $publicKey.Trim() -Encoding utf8
$accessToken | ConvertFrom-SecureString | Set-Content -LiteralPath (Join-Path $runtime 'mercado-pago-access-token.dpapi') -Encoding utf8
Set-Content -LiteralPath (Join-Path $runtime 'mercado-pago-test-mode.txt') -Value 'true' -Encoding ascii

Write-Host 'Credenciais de teste salvas com proteção do Windows.' -ForegroundColor Green
Write-Host 'Reiniciando o backend...'
& (Join-Path $PSScriptRoot 'start-backend.ps1')
