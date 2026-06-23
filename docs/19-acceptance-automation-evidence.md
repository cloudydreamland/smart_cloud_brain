# Acceptance Automation Evidence

Date: 2026-06-19

## Current Scope

- AI 排班服务化链路暂不纳入本轮完善范围，由后续任务接手。
- 本轮补齐当前用户接口、处方详情接口、权限测试、文档命令与验收路径对齐。

## Backend Maven Verification

Command:

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl auth-service,prescription-service -am test
```

Result:

- PASS. Build success.
- Covered tests added in this pass: `/api/auth/me` current-user response, prescription detail ownership denial, doctor-owned prescription detail with items.

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

## Frontend Verification

Command:

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm test
corepack pnpm --filter @smart-cloud-brain/patient-web build
corepack pnpm --filter @smart-cloud-brain/doctor-web build
corepack pnpm --filter @smart-cloud-brain/admin-web build
```

Result:

- PASS. `shared-api` tests: 4 passed.
- PASS. patient-web, doctor-web, and admin-web production builds completed.

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
- 2026-06-19 update: collection now covers `/api/auth/me`, doctor prescription detail, and patient prescription detail.

Runtime command after local services are up:

```powershell
cd D:\smart_cloud_brain
newman run postman\smart-cloud-brain.postman_collection.json -e postman\local.postman_environment.json
```

Covered workflows:

- Admin schedule workflow: login, generate schedule suggestions, read detail, publish, read dictionaries.
- Patient closed loop: login, `/api/auth/me`, profile, AI triage through backend, list bookable slots, create registration, create/cancel another registration, list registrations.
- Doctor closed loop: login, list registrations, generate medical record, SSE generation, save record, AI prescription check, create prescription, read prescription detail, read notifications.
- Patient follow-up views: read saved medical records, prescriptions and prescription detail.
- Admin triage desk workflow: list, detail, assign, close.
- AI fallback workflow: non-cardiology triage uses the backend AI chain and stores manual/degraded status.

Runtime result:

- Not completed in this pass.
- `docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build` failed because this Docker CLI does not support `--env-file` on the `docker compose` subcommand.
- `docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build` failed because Docker daemon was not running: `//./pipe/docker_engine` was not found.
- `newman run ...` failed because `newman` is not installed on this machine.

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

Checks:

- All expected tables exist.
- Required seed data exists for department, doctor, patient, admin user, drug, prompt template, knowledge entry and system dictionary.
- Published schedules exist.
- Appointment slots are bookable with `status = 'AVAILABLE'` and `remaining_capacity > 0`.
- SQL initialization changes should be applied through Flyway/init scripts before running this verifier.

Runtime result:

- Not completed in this pass.
- `psql` was not installed, so the script attempted to use `ksql` inside the `scb-kingbase` Docker container.
- Docker daemon was not running, so the fallback database client could not connect to `//./pipe/docker_engine`.

## Screenshots

Screenshot directory:

- `D:\smart_cloud_brain\docs\evidence\screenshots`

Current status:

- Pending capture for a future full-stack run.
- Capture after Docker Desktop and Newman are available: Postman/Newman summary, database verification, Docker Compose status, RabbitMQ status, and three Web screenshots.
