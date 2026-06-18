param(
  [ValidateSet("dev", "demo", "prod")]
  [string]$Profile = "dev",
  [switch]$NoBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root "deploy\env\.env.$Profile"
$EnvExample = Join-Path $Root "deploy\env\.env.$Profile.example"
$ComposeFile = Join-Path $Root "deploy\docker-compose.yml"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker is not installed or not available on PATH."
}
& docker info *> $null
if ($LASTEXITCODE -ne 0) {
  throw "Docker daemon is not running. Start Docker Desktop first."
}

if (-not (Test-Path $EnvFile)) {
  Copy-Item -LiteralPath $EnvExample -Destination $EnvFile
  Write-Host "Created local environment file: $EnvFile"
}

$Architecture = $null
try {
  $Architecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
} catch {
  $Architecture = $null
}
if ($null -ne $Architecture) {
  $Architecture = $Architecture.ToString()
} elseif ($env:PROCESSOR_ARCHITEW6432) {
  $Architecture = $env:PROCESSOR_ARCHITEW6432
} elseif ($env:PROCESSOR_ARCHITECTURE) {
  $Architecture = $env:PROCESSOR_ARCHITECTURE
} else {
  $Architecture = "AMD64"
}
$KingbaseImage = if ($Architecture -match "^(Arm64|ARM64)$") {
  "kingbase_v009r001c010b0004_single_arm:v1"
} else {
  "kingbase_v009r001c010b0004_single_x86:v1"
}
$env:KINGBASE_IMAGE = $KingbaseImage
Write-Host "Using Kingbase image: $KingbaseImage"

if (-not $NoBuild) {
  Write-Host "Packaging backend jars ..."
  $BackendPom = Join-Path $Root "backend\pom.xml"
  & mvn -f $BackendPom clean package -DskipTests
  if ($LASTEXITCODE -ne 0) {
    throw "Backend packaging failed with exit code $LASTEXITCODE."
  }
}

$args = @(
  "--env-file", $EnvFile,
  "-f", $ComposeFile
)
$args += @("up", "-d")
if (-not $NoBuild) {
  $args += "--build"
}

Write-Host "Starting Docker Compose services with environment '$Profile' ..."
& docker compose @args
if ($LASTEXITCODE -ne 0) {
  throw "Docker Compose startup failed with exit code $LASTEXITCODE."
}

Write-Host ""
Write-Host "Done."
Write-Host "Patient: http://localhost:5173"
Write-Host "Doctor : http://localhost:5174"
Write-Host "Admin  : http://localhost:5175"
Write-Host "Gateway: http://localhost:18080"
