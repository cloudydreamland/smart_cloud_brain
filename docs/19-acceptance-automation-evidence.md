# Acceptance Automation Evidence

Date: 2026-06-16

## Backend Maven Verification

Command:

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl doctor-service,admin-service,registration-service,medical-record-service,prescription-service,notification-service,ai-service -am test
```

Result:

- PASS. Build success.
- Covered tests: doctor schedule publish creates bookable slots, admin permission, admin schedule delegation, admin triage assign/close, registration permission, medical-record permission, medical-record SSE generation, prescription permission, notification creation, AI mock provider, prompt template resolution.

Command:

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

Result:

- PASS. Build success for the required backend module list.

## Postman Acceptance Collection

Files:

- `D:\smart_cloud_brain\postman\smart-cloud-brain.postman_collection.json`
- `D:\smart_cloud_brain\postman\local.postman_environment.json`

Static validation command:

```powershell
cd D:\smart_cloud_brain
node -e "JSON.parse(require('fs').readFileSync('postman/smart-cloud-brain.postman_collection.json','utf8')); JSON.parse(require('fs').readFileSync('postman/local.postman_environment.json','utf8')); console.log('postman json ok')"
```

Result:

- PASS. Postman collection and local environment JSON parse successfully.

Runtime command after local services are up:

```powershell
cd D:\smart_cloud_brain
newman run postman\smart-cloud-brain.postman_collection.json -e postman\local.postman_environment.json
```

Covered workflows:

- Admin schedule workflow: login, generate suggestions, read detail, publish, read dictionaries.
- Patient closed loop: login, profile, AI triage through backend, list bookable slots, create registration, create/cancel another registration, list registrations.
- Doctor closed loop: login, list registrations, generate medical record, SSE generation, save record, AI prescription check, create prescription, read notifications.
- Patient follow-up views: read saved medical records and prescriptions.
- Admin triage desk workflow: list, detail, assign, close.
- AI fallback workflow: non-cardiology triage uses the backend AI chain and stores manual/degraded status.

Runtime result:

- Not executed in this pass because the full service stack was not started in this workspace session.

## Kingbase-Compatible Database Verification

Script:

- `D:\smart_cloud_brain\scripts\verify-kingbase.ps1`

Syntax check command:

```powershell
cd D:\smart_cloud_brain
$null = [scriptblock]::Create((Get-Content scripts\verify-kingbase.ps1 -Raw)); Write-Output 'verify-kingbase.ps1 syntax ok'
```

Result:

- PASS. Script parses successfully.

Runtime command for existing initialized database:

```powershell
cd D:\smart_cloud_brain
$env:KINGBASE_HOST = "127.0.0.1"
$env:KINGBASE_PORT = "54321"
$env:KINGBASE_DATABASE = "smart_cloud_brain"
$env:KINGBASE_USER = "system"
$env:KINGBASE_PASSWORD = "<password>"
.\scripts\verify-kingbase.ps1
```

Runtime command to verify repeatable initialization:

```powershell
cd D:\smart_cloud_brain
.\scripts\verify-kingbase.ps1 -ApplySql
```

Checks:

- All expected tables exist.
- Required seed data exists for department, doctor, patient, admin user, drug, prompt template, knowledge entry and system dictionary.
- Published schedules exist.
- Appointment slots are bookable with `status = 'AVAILABLE'` and `remaining_capacity > 0`.
- With `-ApplySql`, schema and seed SQL are executed twice and key table counts are compared for idempotency.

Runtime result:

- Not executed in this pass because no Kingbase connection credentials were provided.

## Screenshots

Screenshot directory:

- `D:\smart_cloud_brain\docs\evidence\screenshots`

Current status:

- No browser/Postman/database screenshots were captured in this backend-only automation pass.
- Put future Postman runner, database client and three-terminal E2E screenshots in this directory and reference them from this document.
