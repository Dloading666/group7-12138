param(
  [string]$DbHost = 'localhost',
  [int]$DbPort = 3306,
  [string]$DbName = 'rpa',
  [string]$DbUser = 'root',
  [string]$DbPassword = 'root',
  [string]$FrontendHost = '127.0.0.1',
  [int]$FrontendPort = 5173,
  [switch]$UseMySql,
  [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $rootDir 'backend'
$frontendDir = Join-Path $rootDir 'frontend'
$jdbcUrl = "jdbc:mysql://$($DbHost):$($DbPort)/$($DbName)?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"

if ($UseMySql) {
  $backendCommand = @"
`$env:SPRING_PROFILES_ACTIVE = 'mysql'
`$env:SPRING_DATASOURCE_URL = '$jdbcUrl'
`$env:SPRING_DATASOURCE_USERNAME = '$DbUser'
`$env:SPRING_DATASOURCE_PASSWORD = '$DbPassword'
Set-Location '$backendDir'
mvn spring-boot:run
"@
  $backendMode = "MySQL target: $DbHost`:$DbPort / $DbName"
} else {
  $backendCommand = @"
`$env:SPRING_PROFILES_ACTIVE = 'dev'
Set-Location '$backendDir'
mvn spring-boot:run
"@
  $backendMode = 'Backend profile: dev (embedded H2)'
}

$frontendCommand = @"
Set-Location '$frontendDir'
if (-not (Test-Path 'node_modules')) {
  npm install
}
npm run dev -- --host $FrontendHost --port $FrontendPort
"@

if ($DryRun) {
  Write-Host 'Backend command:'
  Write-Host $backendCommand
  Write-Host ''
  Write-Host 'Frontend command:'
  Write-Host $frontendCommand
  exit 0
}

Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $backendCommand | Out-Null
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand | Out-Null

Write-Host 'RPA dev environment is starting.'
Write-Host "Backend: http://localhost:8080/api"
Write-Host "Frontend: http://$FrontendHost`:$FrontendPort"
Write-Host ''
Write-Host $backendMode
