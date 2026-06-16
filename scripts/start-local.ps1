param(
  [switch]$NoBuild,
  [switch]$ExternalDb
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root "deploy\env\.env"
$EnvExample = Join-Path $Root "deploy\env\.env.example"
$ComposeFile = Join-Path $Root "deploy\docker-compose.yml"

if (-not (Test-Path $EnvFile)) {
  Copy-Item -LiteralPath $EnvExample -Destination $EnvFile
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

Write-Host "Starting Docker Compose services ..."
& docker-compose @args

Write-Host ""
Write-Host "Done."
Write-Host "Patient: http://localhost:5173"
Write-Host "Doctor : http://localhost:5174"
Write-Host "Admin  : http://localhost:5175"
Write-Host "Gateway: http://localhost:18080"
