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

$UseComposePlugin = $true
& docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
  if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    throw "Neither 'docker compose' nor 'docker-compose' is available."
  }
  $UseComposePlugin = $false
  Write-Host "Docker Compose plugin is unavailable; using docker-compose fallback."
}

function Invoke-Compose {
  param([string[]]$ComposeArgs)
  if ($UseComposePlugin) {
    & docker compose @ComposeArgs
  } else {
    & docker-compose @ComposeArgs
  }
}

function Get-EnvValue {
  param(
    [string]$Path,
    [string]$Name,
    [string]$Default
  )
  if (-not (Test-Path $Path)) {
    return $Default
  }
  $Line = Get-Content -LiteralPath $Path | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -First 1
  if (-not $Line) {
    return $Default
  }
  $Value = $Line.Substring($Name.Length + 1).Trim()
  if ($Value) { return $Value }
  return $Default
}

function Invoke-DockerBuild {
  param([string[]]$DockerArgs)
  & docker build @DockerArgs
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

$args = @(
  "--env-file", $EnvFile,
  "-f", $ComposeFile
)
if (-not $NoBuild) {
  $BuildServices = @(
    "gateway-service", "auth-service", "patient-service", "doctor-service",
    "registration-service", "triage-service", "medical-record-service",
    "prescription-service", "notification-service", "admin-service", "ai-service"
  )
  foreach ($Service in $BuildServices) {
    Invoke-DockerBuild @(
      "-f", (Join-Path $Root "backend\Dockerfile.service"),
      "--build-arg", "SERVICE=$Service",
      "-t", "deploy-$Service",
      $Root
    )
    if ($LASTEXITCODE -ne 0) {
      throw "Docker image build failed for '$Service'."
    }
  }

  $PnpmVersion = Get-EnvValue -Path $EnvFile -Name "PNPM_VERSION" -Default "9.15.0"
  $NpmRegistry = Get-EnvValue -Path $EnvFile -Name "NPM_REGISTRY" -Default "https://registry.npmmirror.com"
  Invoke-DockerBuild @(
    "-f", (Join-Path $Root "deploy\nginx\Dockerfile"),
    "--build-arg", "PNPM_VERSION=$PnpmVersion",
    "--build-arg", "NPM_REGISTRY=$NpmRegistry",
    "-t", "deploy-nginx",
    $Root
  )
  if ($LASTEXITCODE -ne 0) {
    throw "Docker image build failed for 'nginx'."
  }
}
$args += @("up", "-d", "--no-build")

Write-Host "Starting Docker Compose services with environment '$Profile' ..."
Invoke-Compose $args
if ($LASTEXITCODE -ne 0) {
  throw "Docker Compose startup failed with exit code $LASTEXITCODE."
}

$AiProviderLine = Get-Content -LiteralPath $EnvFile | Where-Object { $_ -eq "AI_PROVIDER=dify" }
if ($AiProviderLine) {
  $DifyNetwork = if ($env:DIFY_DOCKER_NETWORK) { $env:DIFY_DOCKER_NETWORK } else { "docker_default" }
  & docker network inspect $DifyNetwork *> $null
  if ($LASTEXITCODE -ne 0) {
    throw "Dify network '$DifyNetwork' is missing. Start Dify first."
  }
  $ConnectedNames = & docker network inspect $DifyNetwork --format '{{range .Containers}}{{println .Name}}{{end}}'
  if ($ConnectedNames -notcontains "scb-ai-service") {
    & docker network connect $DifyNetwork scb-ai-service
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to connect ai-service to Dify network '$DifyNetwork'."
    }
  }
  Write-Host "Connected ai-service to Dify network: $DifyNetwork"
}

Write-Host ""
Write-Host "Done."
Write-Host "Patient: http://localhost:5173"
Write-Host "Doctor : http://localhost:5174"
Write-Host "Admin  : http://localhost:5175"
Write-Host "Gateway: http://localhost:18080"
