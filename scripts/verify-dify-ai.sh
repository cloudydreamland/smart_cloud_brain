#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
AI_BASE_URL="${AI_BASE_URL:-http://localhost:8081}"
ENV_FILE="${PROJECT_ENV_FILE:-$ROOT_DIR/deploy/env/.env}"

if [ -z "${INTERNAL_SERVICE_TOKEN:-}" ] && [ -f "$ENV_FILE" ]; then
  INTERNAL_SERVICE_TOKEN="$(sed -n 's/^INTERNAL_SERVICE_TOKEN=//p' "$ENV_FILE" | head -1)"
fi
INTERNAL_SERVICE_TOKEN="${INTERNAL_SERVICE_TOKEN:-smart-cloud-brain-internal-local-token-change}"
AUTH_HEADER="X-Internal-Token: $INTERNAL_SERVICE_TOKEN"

if date -v+1d +%F >/dev/null 2>&1; then
  START_DATE="$(date -v+1d +%F)"
  END_DATE="$(date -v+2d +%F)"
else
  START_DATE="$(date -d tomorrow +%F)"
  END_DATE="$(date -d '+2 days' +%F)"
fi

call_ai() {
  local path="$1"
  local body="$2"
  curl -fsS -H "$AUTH_HEADER" -H "Content-Type: application/json" \
    -d "$body" "${AI_BASE_URL}${path}"
}

echo "[1/5] AI health"
health="$(curl -fsS "${AI_BASE_URL}/internal/ai/health")"
echo "$health" | jq -e '.code == 0 and .data.provider == "dify" and .data.status == "UP" and .data.difyConfigured == true' >/dev/null

echo "[2/5] Dify triage workflow"
triage="$(call_ai /internal/ai/triage '{"patientId":1,"chiefComplaint":"胸痛、气短两天，活动后加重"}')"
echo "$triage" | jq -e '.code == 0 and .data.degraded == false and (.data.departmentCode | length > 0)' >/dev/null

echo "[3/5] Dify medical record workflow"
record="$(call_ai /internal/ai/medical-record/generate '{"registrationId":1,"departmentCode":"CARDIOLOGY","dialogueText":"患者自述胸痛、气短两天，活动后加重，休息后稍缓解。"}')"
echo "$record" | jq -e '.code == 0 and .data.degraded == false and (.data.diagnosis | length > 0)' >/dev/null

echo "[4/5] Dify prescription workflow"
prescription="$(call_ai /internal/ai/prescription/check '{"patientId":1,"doctorId":1,"drugs":[{"drugName":"阿司匹林","dosage":"100mg","frequency":"每日一次","usageMethod":"口服"}]}')"
echo "$prescription" | jq -e '.code == 0 and .data.degraded == false and (.data.riskLevel | length > 0)' >/dev/null

echo "[5/5] Dify schedule workflow"
schedule_body="$(jq -nc --arg start "$START_DATE" '{startDate:$start,days:2,doctors:[{doctorId:1,doctorName:"王医生",departmentId:1,departmentCode:"CARDIOLOGY",specialty:"胸痛",enabled:true}],departments:[{departmentId:1,departmentCode:"CARDIOLOGY",departmentName:"心内科"}],existingSchedules:[]}')"
schedule="$(call_ai /internal/ai/schedule/suggest "$schedule_body")"
echo "$schedule" | jq -e --arg start "$START_DATE" --arg end "$END_DATE" '
  .code == 0 and .data.provider == "dify" and .data.degraded == false
  and (.data.suggestions | length > 0)
  and all(.data.suggestions[];
    .doctorId == 1 and .departmentId == 1
    and .workDate >= $start and .workDate <= $end
    and (.capacity >= 1 and .capacity <= 100)
    and (.timeRange | test("^([01][0-9]|2[0-3]):[0-5][0-9]-([01][0-9]|2[0-3]):[0-5][0-9]$")))
' >/dev/null

echo "All four Dify workflows passed with degraded=false."
