#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/deploy/env/.env.dev}"
BACKEND_SERVICES="gateway-service auth-service patient-service doctor-service registration-service triage-service medical-record-service prescription-service notification-service admin-service ai-service"
WEB_SERVICES="patient-web doctor-web admin-web"

# Compose v5 delegates builds to Bake, whose session header cannot encode this
# repository's non-ASCII path. Native builds keep the same Compose image names.
for service in $BACKEND_SERVICES; do
  docker build \
    -f "$ROOT_DIR/backend/Dockerfile.service" \
    --build-arg "SERVICE=$service" \
    -t "deploy-$service" \
    "$ROOT_DIR"
done

PNPM_VERSION="$(sed -n 's/^PNPM_VERSION=//p' "$ENV_FILE" 2>/dev/null | head -1)"
NPM_REGISTRY="$(sed -n 's/^NPM_REGISTRY=//p' "$ENV_FILE" 2>/dev/null | head -1)"
PNPM_VERSION="${PNPM_VERSION:-9.15.0}"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"

for service in $WEB_SERVICES; do
  docker build \
    -f "$ROOT_DIR/frontend/Dockerfile.web" \
    --build-arg "APP_NAME=$service" \
    --build-arg "PNPM_VERSION=$PNPM_VERSION" \
    --build-arg "NPM_REGISTRY=$NPM_REGISTRY" \
    -t "deploy-$service" \
    "$ROOT_DIR"
done
