#!/usr/bin/env bash
set -euo pipefail

AI_BASE_URL="${AI_BASE_URL:-http://localhost:8081}"

echo "[1/4] AI health"
curl -fsS "${AI_BASE_URL}/internal/ai/health"
echo

echo "[2/4] Dify triage workflow through ai-service"
curl -fsS \
  -H "Content-Type: application/json" \
  -d '{"patientId":1,"chiefComplaint":"胸痛、气短两天，活动后加重"}' \
  "${AI_BASE_URL}/internal/ai/triage"
echo

echo "[3/4] Dify medical record workflow through ai-service"
curl -fsS \
  -H "Content-Type: application/json" \
  -d '{"registrationId":1,"departmentCode":"CARDIOLOGY","dialogueText":"患者自述胸痛、气短两天，活动后加重，休息后稍缓解。"}' \
  "${AI_BASE_URL}/internal/ai/medical-record/generate"
echo

echo "[4/4] Dify prescription check workflow through ai-service"
curl -fsS \
  -H "Content-Type: application/json" \
  -d '{"patientId":1,"doctorId":1,"drugs":[{"drugName":"阿司匹林","dosage":"100mg","frequency":"每日一次","usageMethod":"口服"}]}' \
  "${AI_BASE_URL}/internal/ai/prescription/check"
echo

echo "Done. For a real Dify pass, provider must be dify, difyConfigured must be true, and business responses should have degraded=false."
