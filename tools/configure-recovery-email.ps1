param([string]$Email)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = Read-Host 'E-mail remetente da StowUp'
}
if ($Email -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') {
    throw 'Informe um e-mail remetente válido.'
}
$email = $Email.Trim().ToLowerInvariant()

Write-Host "Configurando o remetente $email" -ForegroundColor Cyan
$entered = Read-Host 'Cole o código de 16 caracteres criado pelo Google' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($entered)
try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $plain = $plain -replace '\s', ''
    if ($plain.Length -ne 16) {
        throw 'O código deve possuir 16 caracteres. Execute novamente e confira o código do Google.'
    }
    $normalized = ConvertTo-SecureString $plain -AsPlainText -Force
    New-Item -ItemType Directory -Force $runtime | Out-Null
    Set-Content -LiteralPath (Join-Path $runtime 'smtp-email.txt') -Value $email -Encoding ascii
    Set-Content -LiteralPath (Join-Path $runtime 'smtp-password.dpapi') `
        -Value ($normalized | ConvertFrom-SecureString) -Encoding ascii
} finally {
    if ($pointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    $plain = $null
}

Write-Host 'Código salvo de forma criptografada para este usuário do Windows.' -ForegroundColor Green
& (Join-Path $PSScriptRoot 'start-backend.ps1')
