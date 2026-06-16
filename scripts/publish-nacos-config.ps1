param(
  [string]$NacosUrl = "http://localhost:8848",
  [string]$ConfigDir = "$PSScriptRoot\..\deploy\nacos\configs",
  [string]$Group = "DEFAULT_GROUP",
  [int]$WaitSeconds = 60
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ConfigDir)) {
  throw "Nacos config directory not found: $ConfigDir"
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
$ready = $false
while ((Get-Date) -lt $deadline) {
  try {
    $health = Invoke-RestMethod -Method Get -Uri "$NacosUrl/nacos/v1/console/health/readiness" -TimeoutSec 3
    if ("$health" -match "^(OK|UP)$") {
      $ready = $true
      break
    }
  } catch {
    Start-Sleep -Seconds 2
  }
}

if (-not $ready) {
  throw "Nacos is not reachable at $NacosUrl. Start Nacos first, then rerun this script. For Docker: docker compose -f deploy\nacos\docker-compose.yml up -d"
}

Get-ChildItem -LiteralPath $ConfigDir -Filter *.yml | ForEach-Object {
  $content = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
  $body = @{
    dataId = $_.Name
    group = $Group
    type = "yaml"
    content = $content
  }
  Invoke-RestMethod -Method Post -Uri "$NacosUrl/nacos/v1/cs/configs" -Body $body | Out-Null
  Write-Host "published $($_.Name)"
}
