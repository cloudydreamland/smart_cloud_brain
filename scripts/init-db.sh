#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
FLYWAY_IMAGE="${FLYWAY_IMAGE:-flyway/flyway:10-alpine}"
DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-54321}"
DB_NAME="${DB_NAME:-smart_cloud_brain}"
DB_USERNAME="${DB_USERNAME:-scb}"
DB_PASSWORD="${DB_PASSWORD:-scb_password}"

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "$ROOT_DIR/sql/flyway:/flyway/sql:ro" \
  "$FLYWAY_IMAGE" \
  -url="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
  -user="$DB_USERNAME" \
  -password="$DB_PASSWORD" \
  -connectRetries=60 \
  -locations=filesystem:/flyway/sql \
  -baselineOnMigrate=true \
  -baselineVersion=0 \
  migrate
