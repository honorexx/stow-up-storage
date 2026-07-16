$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
$logs = Join-Path $runtime 'logs'
$jar = Join-Path $root 'target\stowup-1.0.0-SNAPSHOT.jar'
$emailFile = Join-Path $runtime 'smtp-email.txt'
$passwordFile = Join-Path $runtime 'smtp-password.dpapi'
$twilioSidFile = Join-Path $runtime 'twilio-account-sid.txt'
$twilioTokenFile = Join-Path $runtime 'twilio-auth-token.dpapi'
$twilioFromFile = Join-Path $runtime 'twilio-from-number.txt'
$mercadoPagoPublicKeyFile = Join-Path $runtime 'mercado-pago-public-key.txt'
$mercadoPagoTokenFile = Join-Path $runtime 'mercado-pago-access-token.dpapi'
$toolchainFile = Join-Path $runtime 'toolchain.ps1'
$backendEnvironmentFile = Join-Path $runtime 'backend.env.ps1'
$backendPidFile = Join-Path $runtime 'backend.pid'

if (Test-Path -LiteralPath $toolchainFile) { . $toolchainFile }
if (Test-Path -LiteralPath $backendEnvironmentFile) { . $backendEnvironmentFile }

if (-not (Test-Path -LiteralPath $jar)) {
    throw 'O backend ainda não foi empacotado. Execute o Maven package pelo IntelliJ.'
}
& (Join-Path $PSScriptRoot 'start-postgres.ps1')

$email = $null
$encryptedPassword = $null
if ((Test-Path -LiteralPath $emailFile) -and (Test-Path -LiteralPath $passwordFile)) {
    try {
        $email = (Get-Content -LiteralPath $emailFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $encryptedPassword = (Get-Content -LiteralPath $passwordFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $securePassword = $encryptedPassword | ConvertTo-SecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $env:SMTP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
        $env:PASSWORD_RECOVERY_DELIVERY_MODE = 'live'
        $env:PASSWORD_RECOVERY_EMAIL_FROM = $email
        $env:SMTP_HOST = 'smtp.gmail.com'
        $env:SMTP_PORT = '587'
        $env:SMTP_USERNAME = $email
        $env:SMTP_AUTH = 'true'
        $env:SMTP_STARTTLS = 'true'
    } catch {
        $email = $null
        $encryptedPassword = $null
        $env:PASSWORD_RECOVERY_DELIVERY_MODE = 'log'
        Write-Warning 'A senha de e-mail pertence a outra sessão do Windows. O backend seguirá em modo local; execute tools\configure-recovery-email.ps1 para reconfigurar.'
    }
} elseif ([string]::IsNullOrWhiteSpace($env:PASSWORD_RECOVERY_DELIVERY_MODE)) {
    $env:PASSWORD_RECOVERY_DELIVERY_MODE = 'log'
}

if ((Test-Path -LiteralPath $twilioSidFile) -and (Test-Path -LiteralPath $twilioTokenFile) -and (Test-Path -LiteralPath $twilioFromFile)) {
    try {
        $env:TWILIO_ACCOUNT_SID = (Get-Content -LiteralPath $twilioSidFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $encryptedTwilioToken = (Get-Content -LiteralPath $twilioTokenFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $secureTwilioToken = $encryptedTwilioToken | ConvertTo-SecureString
        $twilioPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureTwilioToken)
        try {
            $env:TWILIO_AUTH_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($twilioPointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($twilioPointer)
        }
        $env:TWILIO_FROM_NUMBER = (Get-Content -LiteralPath $twilioFromFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $env:PASSWORD_RECOVERY_DELIVERY_MODE = 'live'
    } catch {
        Write-Warning 'As credenciais da Twilio não puderam ser lidas. Execute tools\configure-recovery-sms.ps1 novamente.'
    }
}

if ((Test-Path -LiteralPath $mercadoPagoPublicKeyFile) -and (Test-Path -LiteralPath $mercadoPagoTokenFile)) {
    try {
        $env:MERCADO_PAGO_PUBLIC_KEY = (Get-Content -LiteralPath $mercadoPagoPublicKeyFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $encryptedMercadoPagoToken = (Get-Content -LiteralPath $mercadoPagoTokenFile -Raw -Encoding utf8).Trim().TrimStart([char]0xFEFF)
        $secureMercadoPagoToken = $encryptedMercadoPagoToken | ConvertTo-SecureString
        $mercadoPagoPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureMercadoPagoToken)
        try {
            $env:MERCADO_PAGO_ACCESS_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($mercadoPagoPointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($mercadoPagoPointer)
        }
    } catch {
        Write-Warning 'As credenciais do Mercado Pago não puderam ser lidas. Execute tools\configure-mercado-pago.ps1 novamente.'
    }
}

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $knownProcess = if (Test-Path -LiteralPath $backendPidFile) {
        (Get-Content -LiteralPath $backendPidFile -ErrorAction SilentlyContinue | Select-Object -First 1) -split '\|'
    }
    if ($knownProcess -and [int]$knownProcess[0] -eq $listener.OwningProcess) {
        Stop-Process -Id $listener.OwningProcess -Force
        Wait-Process -Id $listener.OwningProcess -Timeout 10 -ErrorAction SilentlyContinue
    } else {
        throw 'A porta 8080 está sendo usada por outro programa ou por outra instalação do StowUp. Encerre-o antes de iniciar esta cópia.'
    }
}

$java = if ($STOWUP_JAVA_EXE -and (Test-Path -LiteralPath $STOWUP_JAVA_EXE)) {
    $STOWUP_JAVA_EXE
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java.exe -ErrorAction Stop).Source
}

New-Item -ItemType Directory -Force $logs | Out-Null
# Alguns terminais criam as variáveis Path e PATH ao mesmo tempo. O
# Start-Process do Windows trata isso como chave duplicada, então normalizamos.
$processPath = $env:Path
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
$process = Start-Process -WindowStyle Hidden -FilePath $java `
    -ArgumentList @('-jar', 'target\stowup-1.0.0-SNAPSHOT.jar') `
    -WorkingDirectory $root `
    -RedirectStandardOutput (Join-Path $logs 'backend-email.out.log') `
    -RedirectStandardError (Join-Path $logs 'backend-email.err.log') `
    -PassThru
Set-Content -LiteralPath $backendPidFile -Value "$($process.Id)|$($process.StartTime.ToUniversalTime().Ticks)" -Encoding ascii

$env:SMTP_PASSWORD = $null
$env:TWILIO_AUTH_TOKEN = $null
$env:MERCADO_PAGO_ACCESS_TOKEN = $null
$encryptedPassword = $null
$deadline = (Get-Date).AddSeconds(30)
do {
    Start-Sleep -Milliseconds 500
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/spaces' -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            $delivery = if ($email) { "envio real por $email" } else { 'recuperação em modo local' }
            Write-Host "StowUp iniciado com $delivery." -ForegroundColor Green
            Write-Host "Backend: http://localhost:8080 (PID $($process.Id))"
            exit 0
        }
    } catch {
        if ($process.HasExited) { break }
    }
} while ((Get-Date) -lt $deadline)

Get-Content -LiteralPath (Join-Path $logs 'backend-email.out.log') -Tail 50 -ErrorAction SilentlyContinue
Get-Content -LiteralPath (Join-Path $logs 'backend-email.err.log') -Tail 50 -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $backendPidFile -Force -ErrorAction SilentlyContinue
throw 'O Spring Boot não iniciou. Veja os últimos erros acima.'
