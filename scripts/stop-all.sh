#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

docker compose -f "$ROOT_DIR/deploy/docker-compose.yml" down
