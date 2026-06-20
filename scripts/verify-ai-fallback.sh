#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
AI_BASE_URL="${AI_BASE_URL:-http://localhost:8081}"
DIFY_GATEWAY_CONTAINER="${DIFY_GATEWAY_CONTAINER:-docker-nginx-1}"
ENV_FILE="${PROJECT_ENV_FILE:-$ROOT_DIR/deploy/env/.env}"

if [ -z "${INTERNAL_SERVICE_TOKEN:-}" ] && [ -f "$ENV_FILE" ]; then
  INTERNAL_SERVICE_TOKEN="$(sed -n 's/^INTERNAL_SERVICE_TOKEN=//p' "$ENV_FILE" | head -1)"
fi
INTERNAL_SERVICE_TOKEN="${INTERNAL_SERVICE_TOKEN:-smart-cloud-brain-internal-local-token-change}"
AUTH_HEADER="X-Internal-Token: $INTERNAL_SERVICE_TOKEN"

if date -v+1d +%F >/dev/null 2>&1; then
  START_DATE="$(date -v+1d +%F)"
else
  START_DATE="$(date -d tomorrow +%F)"
fi

restore_dify() {
  if [ "$(docker inspect -f '{{.State.Running}}' "$DIFY_GATEWAY_CONTAINER")" != "true" ]; then
    docker start "$DIFY_GATEWAY_CONTAINER" >/dev/null
  fi
}
trap restore_dify EXIT

call_ai() {
  local label="$1"
  local path="$2"
  local body="$3"
  local response
  response="$(curl -fsS -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$body" "${AI_BASE_URL}${path}")"
  echo "$response" | jq -e '.code == 0 and .data.degraded == true' >/dev/null
  echo "$label: degraded=true"
}

echo "Stopping $DIFY_GATEWAY_CONTAINER to simulate a Dify outage."
docker stop "$DIFY_GATEWAY_CONTAINER" >/dev/null
sleep 1

call_ai "triage" /internal/ai/triage \
  '{"patientId":1,"chiefComplaint":"胸痛、气短两天，活动后加重"}'
call_ai "medical-record" /internal/ai/medical-record/generate \
  '{"registrationId":1,"departmentCode":"CARDIOLOGY","dialogueText":"患者自述胸痛、气短两天，活动后加重。"}'
call_ai "prescription" /internal/ai/prescription/check \
  '{"patientId":1,"doctorId":1,"drugs":[{"drugName":"阿司匹林","dosage":"100mg","frequency":"每日一次","usageMethod":"口服"}]}'
schedule_body="$(jq -nc --arg start "$START_DATE" '{startDate:$start,days:1,doctors:[{doctorId:1,doctorName:"王医生",departmentId:1,departmentCode:"CARDIOLOGY",specialty:"胸痛",enabled:true}],departments:[{departmentId:1,departmentCode:"CARDIOLOGY",departmentName:"心内科"}],existingSchedules:[]}')"
call_ai "schedule" /internal/ai/schedule/suggest "$schedule_body"

restore_dify
trap - EXIT
echo "Dify gateway restored; verifying real workflows again."
for attempt in 1 2 3 4 5; do
  sleep 4
  if "$ROOT_DIR/scripts/verify-dify-ai.sh"; then
    exit 0
  fi
  echo "Real Dify verification attempt $attempt failed after network restore."
done
exit 1
