$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
$downloads = Join-Path $runtime 'downloads'
New-Item -ItemType Directory -Force $runtime, $downloads | Out-Null

function Download-File([string]$Url, [string]$Destination) {
    Write-Host "Baixando $Url" -ForegroundColor Cyan
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination
}

function Find-Java21 {
    $candidates = @()
    if ($env:STOWUP_JAVA_EXE) { $candidates += $env:STOWUP_JAVA_EXE }
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $candidates += (Get-ChildItem 'C:\Program Files\Java','C:\Program Files\Eclipse Adoptium',$runtime -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (-not (Test-Path -LiteralPath $candidate)) { continue }
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try { $version = (& $candidate -version 2>&1 | Select-Object -First 1) -join '' } finally { $ErrorActionPreference = $previousPreference }
        if ($version -match '"21(\.|\")') { return $candidate }
    }
    return $null
}

function Install-Java21 {
    $java = Find-Java21
    if ($java) { return $java }
    $archive = Join-Path $downloads 'jdk21.zip'
    $extract = Join-Path $runtime 'java-extract'
    if (Test-Path $extract) { Remove-Item -LiteralPath $extract -Recurse -Force }
    Download-File 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $extract -Force
    $java = Get-ChildItem $extract -Filter java.exe -Recurse | Where-Object { $_.FullName -match '\\bin\\java\.exe$' } | Select-Object -First 1 -ExpandProperty FullName
    if (-not $java) { throw 'O JDK 21 não foi encontrado após a extração.' }
    return $java
}

function Find-Maven {
    if ($env:STOWUP_MVN_CMD -and (Test-Path $env:STOWUP_MVN_CMD)) { return $env:STOWUP_MVN_CMD }
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $found = Get-ChildItem 'C:\Program Files\JetBrains',$runtime -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    return $found
}

function Install-Maven {
    $maven = Find-Maven
    if ($maven) { return $maven }
    $archive = Join-Path $downloads 'apache-maven-3.9.11-bin.zip'
    $destination = Join-Path $runtime 'maven'
    Download-File 'https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip' $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $destination -Force
    $maven = Get-ChildItem $destination -Filter mvn.cmd -Recurse | Select-Object -First 1 -ExpandProperty FullName
    if (-not $maven) { throw 'O Maven não foi encontrado após a extração.' }
    return $maven
}

function Find-CompatibleNode {
    if ($env:STOWUP_NODE_EXE -and (Test-Path $env:STOWUP_NODE_EXE)) { return $env:STOWUP_NODE_EXE }
    $command = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($command) {
        $major = [int]((& $command.Source --version).TrimStart('v').Split('.')[0])
        if ($major -ge 20 -and $major -le 24) { return $command.Source }
    }
    $found = Get-ChildItem $runtime -Filter node.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    return $found
}

function Install-Node {
    $node = Find-CompatibleNode
    if ($node) { return $node }
    $releases = Invoke-RestMethod -Uri 'https://nodejs.org/dist/index.json'
    $release = $releases | Where-Object { $_.version -like 'v22.*' -and $_.files -contains 'win-x64-zip' } | Select-Object -First 1
    if (-not $release) { throw 'Não foi possível localizar uma versão compatível do Node.js.' }
    $archiveName = "node-$($release.version)-win-x64.zip"
    $archive = Join-Path $downloads $archiveName
    $destination = Join-Path $runtime 'node'
    Download-File "https://nodejs.org/dist/$($release.version)/$archiveName" $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $destination -Force
    $node = Get-ChildItem $destination -Filter node.exe -Recurse | Select-Object -First 1 -ExpandProperty FullName
    if (-not $node) { throw 'O Node.js não foi encontrado após a extração.' }
    return $node
}

function New-RandomSecret([int]$Bytes = 32) {
    $buffer = New-Object byte[] $Bytes
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($buffer) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($buffer)
}

function Get-LanAddress {
    try {
        $socket = New-Object Net.Sockets.UdpClient
        $socket.Connect('8.8.8.8', 53)
        $address = ([Net.IPEndPoint]$socket.Client.LocalEndPoint).Address.ToString()
        $socket.Dispose()
        if ($address -and $address -ne '127.0.0.1') { return $address }
    } catch {}
    return (Get-NetIPAddress -AddressFamily IPv4 -AddressState Preferred -ErrorAction SilentlyContinue | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1 -ExpandProperty IPAddress)
}

Write-Host 'Preparando ferramentas do StowUp...' -ForegroundColor Cyan
$java = Install-Java21
$maven = Install-Maven
$node = Install-Node
$nodeDirectory = Split-Path $node -Parent
$npm = Join-Path $nodeDirectory 'npm.cmd'
$pnpmHome = Join-Path $runtime 'pnpm'
$pnpm = if ($env:STOWUP_PNPM_CMD -and (Test-Path $env:STOWUP_PNPM_CMD)) {
    $env:STOWUP_PNPM_CMD
} else {
    Join-Path $pnpmHome 'node_modules\.bin\pnpm.cmd'
}

$env:JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent
$env:PATH = "$nodeDirectory;$env:JAVA_HOME\bin;$(Split-Path $maven -Parent);$env:PATH"
if (-not (Test-Path $pnpm)) {
    if (-not (Test-Path $npm)) { throw 'npm.cmd não foi encontrado junto ao Node.js.' }
    & $npm install --prefix $pnpmHome pnpm@11.7.0
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível instalar o pnpm.' }
}

$quote = { param($value) "'" + ($value -replace "'", "''") + "'" }
$toolchainLines = New-Object 'System.Collections.Generic.List[string]'
$toolchainLines.Add('$STOWUP_JAVA_EXE = ' + (& $quote $java))
$toolchainLines.Add('$STOWUP_MVN_CMD = ' + (& $quote $maven))
$toolchainLines.Add('$STOWUP_NODE_DIR = ' + (& $quote $nodeDirectory))
$toolchainLines.Add('$STOWUP_PNPM_CMD = ' + (& $quote $pnpm))
Set-Content -LiteralPath (Join-Path $runtime 'toolchain.ps1') -Value $toolchainLines -Encoding utf8

$backendEnvironment = Join-Path $runtime 'backend.env.ps1'
$accessFile = Join-Path $runtime 'ACESSO_LOCAL.txt'
if (-not (Test-Path $backendEnvironment)) {
    $masterPassword = 'SU-' + ([Guid]::NewGuid().ToString('N').Substring(0, 14))
    $settingLines = New-Object 'System.Collections.Generic.List[string]'
    $settingLines.Add('$env:JWT_SECRET = ' + (& $quote (New-RandomSecret 48)))
    $settingLines.Add('$env:DATA_ENCRYPTION_KEY = ' + (& $quote (New-RandomSecret 48)))
    $settingLines.Add('$env:STOWUP_MASTER_ENABLED = ''true''')
    $settingLines.Add('$env:STOWUP_MASTER_NAME = ''Usuário Mestre''')
    $settingLines.Add('$env:STOWUP_MASTER_EMAIL = ''master@stowup.local''')
    $settingLines.Add('$env:STOWUP_MASTER_PASSWORD = ' + (& $quote $masterPassword))
    Set-Content -LiteralPath $backendEnvironment -Value $settingLines -Encoding utf8
    @("STOWUP - ACESSO LOCAL", "", "Usuário mestre: master@stowup.local", "Senha: $masterPassword", "", "Este arquivo é local e não entra no Git.") | Set-Content -LiteralPath $accessFile -Encoding utf8
}

$ip = Get-LanAddress
if (-not $ip) { throw 'Não foi possível detectar o IP local deste computador.' }
Set-Content -LiteralPath (Join-Path $root 'mobile\.env.local') -Value "EXPO_PUBLIC_API_URL=http://${ip}:8080/api" -Encoding ascii

Write-Host 'Preparando PostgreSQL...' -ForegroundColor Cyan
& (Join-Path $PSScriptRoot 'setup-postgres.ps1')

Write-Host 'Instalando dependências do aplicativo...' -ForegroundColor Cyan
Push-Location (Join-Path $root 'mobile')
try { & $pnpm install --frozen-lockfile; if ($LASTEXITCODE -ne 0) { throw 'Falha no pnpm install.' } } finally { Pop-Location }

Write-Host 'Compilando Spring Boot...' -ForegroundColor Cyan
& $maven -f (Join-Path $root 'pom.xml') package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o Spring Boot.' }

Write-Host 'Compilando central JavaFX...' -ForegroundColor Cyan
& $maven -f (Join-Path $root 'staff-desktop\pom.xml') package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar a central JavaFX.' }

Write-Host ''
Write-Host 'INSTALAÇÃO CONCLUÍDA' -ForegroundColor Green
Write-Host "API mobile configurada para http://${ip}:8080/api"
Write-Host "Credenciais locais: $accessFile"
Write-Host 'Agora execute INICIAR.bat.' -ForegroundColor Green
