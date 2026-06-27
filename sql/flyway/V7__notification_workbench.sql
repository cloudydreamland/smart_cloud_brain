ALTER TABLE notification_message ADD COLUMN IF NOT EXISTS handle_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE notification_message ADD COLUMN IF NOT EXISTS handled_at TIMESTAMP;
ALTER TABLE notification_message ADD COLUMN IF NOT EXISTS triage_record_id BIGINT;
ALTER TABLE notification_message ADD COLUMN IF NOT EXISTS medical_record_id BIGINT;

UPDATE notification_message
SET handle_status = CASE
  WHEN UPPER(read_status) = 'READ' THEN 'HANDLED'
  ELSE 'PENDING'
END
WHERE handle_status IS NULL OR handle_status = 'PENDING';

UPDATE notification_message
SET handled_at = COALESCE(handled_at, created_at)
WHERE UPPER(handle_status) IN ('HANDLED', 'IGNORED') AND handled_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notification_doctor_created ON notification_message(doctor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_doctor_handle ON notification_message(doctor_id, handle_status);
CREATE INDEX IF NOT EXISTS idx_notification_doctor_type ON notification_message(doctor_id, type);
CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_triage_assign
  ON notification_message(doctor_id, triage_record_id, type)
  WHERE triage_record_id IS NOT NULL AND type = 'TRIAGE_ASSIGN';
