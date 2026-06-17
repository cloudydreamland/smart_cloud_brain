param(
  [ValidateSet("dev", "demo", "prod")]
  [string]$Profile = "dev",
  [switch]$NoBuild,
  [switch]$ExternalDb
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root "deploy\env\.env.$Profile"
$EnvExample = Join-Path $Root "deploy\env\.env.$Profile.example"
$ComposeFile = Join-Path $Root "deploy\docker-compose.yml"

if (-not (Test-Path $EnvFile)) {
  Copy-Item -LiteralPath $EnvExample -Destination $EnvFile
}

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
if (-not $ExternalDb) {
  $args += @("--profile", "embedded-db")
}
$args += @("up", "-d")
if (-not $NoBuild) {
  $args += "--build"
}

Write-Host "Starting Docker Compose services with profile '$Profile' ..."
& docker-compose @args
if ($LASTEXITCODE -ne 0) {
  throw "Docker Compose startup failed with exit code $LASTEXITCODE."
}

Write-Host ""
Write-Host "Done."
Write-Host "Patient: http://localhost:5173"
Write-Host "Doctor : http://localhost:5174"
Write-Host "Admin  : http://localhost:5175"
Write-Host "Gateway: http://localhost:18080"
