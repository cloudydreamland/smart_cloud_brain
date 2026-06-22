#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.yml"
ENV_FILE="${PROJECT_ENV_FILE:-$ROOT_DIR/deploy/env/.env}"
FAILED=0

compose=(docker compose)
if [ -f "$ENV_FILE" ]; then
  compose+=(--env-file "$ENV_FILE")
fi
compose+=(-f "$COMPOSE_FILE")

required_services="kingbase rabbitmq redis gateway-service auth-service patient-service doctor-service registration-service triage-service medical-record-service prescription-service notification-service admin-service ai-service patient-web doctor-web admin-web nginx"

for service in $required_services; do
  container_id=$("${compose[@]}" ps -q "$service")
  if [ -z "$container_id" ]; then
    echo "[FAIL] $service: container is missing"
    FAILED=1
    continue
  fi
  status=$(docker inspect -f '{{.State.Status}}' "$container_id")
  health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")
  if [ "$status" != "running" ] || [ "$health" = "unhealthy" ]; then
    echo "[FAIL] $service: status=$status health=$health"
    FAILED=1
  else
    echo "[PASS] $service: status=$status health=$health"
  fi
done

if [ "$FAILED" -ne 0 ]; then
  echo "Stack verification failed."
  exit 1
fi

curl -fsS http://localhost:18080/actuator/health >/dev/null
curl -fsS http://localhost:8081/internal/ai/health >/dev/null
"$ROOT_DIR/scripts/verify-dify-ai.sh"
echo "Stack verification passed."
