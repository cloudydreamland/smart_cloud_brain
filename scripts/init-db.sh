#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-scb-mysql}"
MYSQL_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-root_password}"

for sql_file in \
  "$ROOT_DIR/sql/auth_db.sql" \
  "$ROOT_DIR/sql/patient_db.sql" \
  "$ROOT_DIR/sql/doctor_db.sql" \
  "$ROOT_DIR/sql/registration_db.sql" \
  "$ROOT_DIR/sql/triage_db.sql" \
  "$ROOT_DIR/sql/medical_record_db.sql" \
  "$ROOT_DIR/sql/prescription_db.sql" \
  "$ROOT_DIR/sql/ai_db.sql" \
  "$ROOT_DIR/sql/notification_db.sql" \
  "$ROOT_DIR/sql/admin_db.sql" \
  "$ROOT_DIR/sql/demo_seed_data.sql"
do
  docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < "$sql_file"
done
