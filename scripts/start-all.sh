#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/deploy/env/.env.dev"
ENV_TEMPLATE="$ROOT_DIR/deploy/env/.env.dev.example"

command -v docker >/dev/null 2>&1 || {
  echo "Docker is not installed or not on PATH." >&2
  exit 1
}
docker info >/dev/null 2>&1 || {
  echo "Docker daemon is not running. Start Docker Desktop first." >&2
  exit 1
}

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_TEMPLATE" "$ENV_FILE"
  echo "Created local environment file: $ENV_FILE"
fi

case "$(uname -m)" in
  arm64|aarch64)
    KINGBASE_IMAGE="kingbase_v009r001c010b0004_single_arm:v1"
    ;;
  *)
    KINGBASE_IMAGE="kingbase_v009r001c010b0004_single_x86:v1"
    ;;
esac

echo "Using Kingbase image: $KINGBASE_IMAGE"

KINGBASE_IMAGE="$KINGBASE_IMAGE" "$ROOT_DIR/scripts/build-images.sh" "$ENV_FILE"
KINGBASE_IMAGE="$KINGBASE_IMAGE" docker compose \
  --env-file "$ENV_FILE" \
  -f "$ROOT_DIR/deploy/docker-compose.yml" \
  up -d --no-build

if grep -q '^AI_PROVIDER=dify$' "$ENV_FILE" 2>/dev/null; then
  DIFY_NETWORK="${DIFY_DOCKER_NETWORK:-docker_default}"
  docker network inspect "$DIFY_NETWORK" >/dev/null 2>&1 || {
    echo "Dify network $DIFY_NETWORK is missing. Start Dify first." >&2
    exit 1
  }
  if ! docker network inspect "$DIFY_NETWORK" --format '{{range .Containers}}{{println .Name}}{{end}}' | grep -qx 'scb-ai-service'; then
    docker network connect "$DIFY_NETWORK" scb-ai-service
  fi
  echo "Connected ai-service to Dify network: $DIFY_NETWORK"
fi
