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
  $Pattern = "^\s*$([regex]::Escape($Name))\s*="
  $Line = Get-Content -LiteralPath $Path | Where-Object {
    $_ -match $Pattern -and $_ -notmatch "^\s*#"
  } | Select-Object -First 1
  if (-not $Line) {
    return $Default
  }
  $Value = ($Line -replace $Pattern, "").Trim()
  if (($Value.StartsWith('"') -and $Value.EndsWith('"')) -or ($Value.StartsWith("'") -and $Value.EndsWith("'"))) {
    $Value = $Value.Substring(1, $Value.Length - 2)
  }
  if ($Value) { return $Value }
  return $Default
}

function Get-EffectiveEnvValue {
  param(
    [string]$Path,
    [string]$Name,
    [string]$Default
  )
  $ProcessValue = [Environment]::GetEnvironmentVariable($Name, "Process")
  if (-not [string]::IsNullOrWhiteSpace($ProcessValue)) {
    return $ProcessValue.Trim()
  }
  return Get-EnvValue -Path $Path -Name $Name -Default $Default
}

function Invoke-DockerBuild {
  param([string[]]$DockerArgs)
  & docker build @DockerArgs
}

function Invoke-BackendPackage {
  param([string[]]$Services)
  $ServiceList = $Services -join ","
  $BackendPom = Join-Path $Root "backend\pom.xml"

  Write-Host "Building backend Maven artifacts once: $ServiceList"
  if (Get-Command mvn -ErrorAction SilentlyContinue) {
    & mvn -f $BackendPom -pl $ServiceList -am clean package -DskipTests
  } else {
    Write-Host "Local Maven is unavailable; using maven:3.9.10-eclipse-temurin-17 in Docker."
    & docker run --rm `
      -v "$($Root):/workspace" `
      -v "smart-cloud-brain-m2:/root/.m2" `
      -w /workspace `
      maven:3.9.10-eclipse-temurin-17 `
      mvn -f backend/pom.xml -pl $ServiceList -am clean package -DskipTests
  }
  if ($LASTEXITCODE -ne 0) {
    throw "Backend Maven package failed."
  }
}

function Assert-BackendArtifacts {
  param([string[]]$Services)
  foreach ($Service in $Services) {
    $JarPath = Join-Path $Root "backend\$Service\target\$Service.jar"
    if (-not (Test-Path -LiteralPath $JarPath)) {
      throw "Backend artifact is missing: $JarPath. Re-run without -NoBuild so start-local.ps1 can package backend services first."
    }
  }
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

& docker image inspect $KingbaseImage *> $null
if ($LASTEXITCODE -ne 0) {
  throw @"
Kingbase image is missing.
Current architecture: $Architecture
Required image: $KingbaseImage

Check local Docker images:
  docker image ls
  docker image ls $($KingbaseImage.Split(":")[0])

If you have a Kingbase image tar package, import it with:
  docker load -i <your-kingbase-image>.tar

If you do not have the tar package, contact the project maintainer to obtain the Kingbase image.
"@
}

$args = @(
  "--env-file", $EnvFile,
  "-f", $ComposeFile
)
$AiProvider = (Get-EffectiveEnvValue -Path $EnvFile -Name "AI_PROVIDER" -Default "dify").ToLowerInvariant()
$DifyNetwork = $null
$DifyComposeOverride = $null
if ($AiProvider -eq "dify") {
  $DifyNetwork = Get-EffectiveEnvValue -Path $EnvFile -Name "DIFY_DOCKER_NETWORK" -Default "docker_default"
  & docker network inspect $DifyNetwork *> $null
  if ($LASTEXITCODE -ne 0) {
    throw "Dify network '$DifyNetwork' is missing. Start Dify first or set AI_PROVIDER=openai/mock for local startup without Dify."
  }

  $YamlDifyNetwork = $DifyNetwork.Replace("'", "''")
  $DifyComposeOverride = Join-Path ([System.IO.Path]::GetTempPath()) "smart-cloud-brain-dify-network-$PID.yml"
  Set-Content -LiteralPath $DifyComposeOverride -Encoding UTF8 -Value @"
services:
  ai-service:
    networks:
      - default
      - dify

networks:
  dify:
    external: true
    name: '$YamlDifyNetwork'
"@
  $args += @("-f", $DifyComposeOverride)
  Write-Host "Dify provider enabled; ai-service will join external network: $DifyNetwork"
}
if (-not $NoBuild) {
  $BuildServices = @(
    "gateway-service", "auth-service", "patient-service", "doctor-service",
    "registration-service", "triage-service", "medical-record-service",
    "prescription-service", "notification-service", "admin-service", "ai-service"
  )
  Invoke-BackendPackage -Services $BuildServices
  Assert-BackendArtifacts -Services $BuildServices

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

$AiProviderLine = $AiProvider -eq "dify"
if ($AiProviderLine) {
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
