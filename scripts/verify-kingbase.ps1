param(
  [string]$Client = $(if ($env:KINGBASE_CLIENT) { $env:KINGBASE_CLIENT } else { "psql" }),
  [string]$Url = $env:KINGBASE_URL,
  [string]$HostName = $(if ($env:KINGBASE_HOST) { $env:KINGBASE_HOST } else { "127.0.0.1" }),
  [int]$Port = $(if ($env:KINGBASE_PORT) { [int]$env:KINGBASE_PORT } else { 54321 }),
  [string]$Database = $(if ($env:KINGBASE_DATABASE) { $env:KINGBASE_DATABASE } else { "smart_cloud_brain" }),
  [string]$User = $(if ($env:KINGBASE_USER) { $env:KINGBASE_USER } else { "system" }),
  [string]$Password = $env:KINGBASE_PASSWORD,
  [switch]$ApplySql,
  [string]$SchemaPath = "",
  [string]$SeedPath = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if ([string]::IsNullOrWhiteSpace($SchemaPath)) {
  $SchemaPath = Join-Path $Root "sql\kingbase_schema.sql"
}
if ([string]::IsNullOrWhiteSpace($SeedPath)) {
  $SeedPath = Join-Path $Root "sql\kingbase_seed_ascii.sql"
}

if (-not [string]::IsNullOrWhiteSpace($Password)) {
  $env:PGPASSWORD = $Password
}

function Get-ClientArgs {
  if (-not [string]::IsNullOrWhiteSpace($Url)) {
    return @($Url)
  }
  return @("-h", $HostName, "-p", "$Port", "-U", $User, "-d", $Database)
}

function Invoke-Db {
  param([string[]]$ExtraArgs)
  $args = @(Get-ClientArgs) + @("-v", "ON_ERROR_STOP=1") + $ExtraArgs
  $output = & $Client @args
  if ($LASTEXITCODE -ne 0) {
    throw "Database client failed with exit code $LASTEXITCODE"
  }
  return $output
}

function Invoke-Scalar {
  param([string]$Sql)
  $result = Invoke-Db @("-At", "-c", $Sql)
  if ($result -is [array]) {
    return ($result | Select-Object -Last 1).Trim()
  }
  return "$result".Trim()
}

function Invoke-SqlFile {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) {
    throw "SQL file not found: $Path"
  }
  Invoke-Db @("-f", $Path) | Out-Null
}

function Assert-PositiveCount {
  param([string]$Name, [string]$Sql)
  $count = [int](Invoke-Scalar $Sql)
  if ($count -le 0) {
    throw "$Name check failed. Count was $count."
  }
  Write-Host "[PASS] $Name count = $count"
}

function Get-Snapshot {
  $tables = @(
    "patient", "department", "doctor", "triage_record", "doctor_schedule",
    "appointment_slot", "ai_schedule_suggestion", "registration", "medical_record",
    "prescription", "prescription_item", "prescription_check_record",
    "notification_message", "prompt_template", "ai_generation_log", "outbox_event",
    "admin_user", "drug", "knowledge_entry", "system_dict"
  )
  $snapshot = [ordered]@{}
  foreach ($table in $tables) {
    $snapshot[$table] = [int](Invoke-Scalar "SELECT COUNT(*) FROM $table;")
  }
  $snapshot["bookable_slots"] = [int](Invoke-Scalar "SELECT COUNT(*) FROM appointment_slot WHERE status = 'AVAILABLE' AND remaining_capacity > 0 AND start_time >= CURRENT_TIMESTAMP - INTERVAL '1 day';")
  return $snapshot
}

function Assert-SameSnapshot {
  param($Expected, $Actual)
  foreach ($key in $Expected.Keys) {
    if ($Expected[$key] -ne $Actual[$key]) {
      throw "Idempotency check failed for $key. First run=$($Expected[$key]), second run=$($Actual[$key])."
    }
  }
  Write-Host "[PASS] repeated schema/seed execution is idempotent for key counts"
}

if ($ApplySql) {
  Write-Host "[INFO] Applying schema and seed for the first pass"
  Invoke-SqlFile $SchemaPath
  Invoke-SqlFile $SeedPath
  $first = Get-Snapshot

  Write-Host "[INFO] Applying schema and seed for the second pass"
  Invoke-SqlFile $SchemaPath
  Invoke-SqlFile $SeedPath
  $second = Get-Snapshot
  Assert-SameSnapshot $first $second
}

$expectedTables = @(
  "patient", "department", "doctor", "triage_record", "doctor_schedule",
  "appointment_slot", "ai_schedule_suggestion", "registration", "medical_record",
  "prescription", "prescription_item", "prescription_check_record",
  "notification_message", "prompt_template", "ai_generation_log", "outbox_event",
  "admin_user", "drug", "knowledge_entry", "system_dict"
)
$tableList = ($expectedTables | ForEach-Object { "'$_'" }) -join ","
$existingTableCount = [int](Invoke-Scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ($tableList);")
if ($existingTableCount -ne $expectedTables.Count) {
  throw "Table existence check failed. Expected $($expectedTables.Count), actual $existingTableCount."
}
Write-Host "[PASS] all expected tables exist"

Assert-PositiveCount "department seed" "SELECT COUNT(*) FROM department;"
Assert-PositiveCount "doctor seed" "SELECT COUNT(*) FROM doctor;"
Assert-PositiveCount "patient seed" "SELECT COUNT(*) FROM patient;"
Assert-PositiveCount "admin seed" "SELECT COUNT(*) FROM admin_user;"
Assert-PositiveCount "drug seed" "SELECT COUNT(*) FROM drug;"
Assert-PositiveCount "prompt template seed" "SELECT COUNT(*) FROM prompt_template;"
Assert-PositiveCount "knowledge entry seed" "SELECT COUNT(*) FROM knowledge_entry;"
Assert-PositiveCount "system dict seed" "SELECT COUNT(*) FROM system_dict;"
Assert-PositiveCount "published schedule seed" "SELECT COUNT(*) FROM doctor_schedule WHERE status = 'PUBLISHED';"
Assert-PositiveCount "bookable appointment slot" "SELECT COUNT(*) FROM appointment_slot WHERE status = 'AVAILABLE' AND remaining_capacity > 0 AND start_time >= CURRENT_TIMESTAMP - INTERVAL '1 day';"

Write-Host "[DONE] Kingbase-compatible database verification passed"
