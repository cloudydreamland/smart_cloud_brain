CREATE TABLE IF NOT EXISTS medical_device (
  id BIGSERIAL PRIMARY KEY,
  device_code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(80),
  department_id BIGINT,
  location VARCHAR(160),
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  purchase_date DATE,
  last_maintenance_at TIMESTAMP,
  remark TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_usage_record (
  id BIGSERIAL PRIMARY KEY,
  device_id BIGINT NOT NULL,
  usage_type VARCHAR(32) NOT NULL,
  used_by VARCHAR(120),
  patient_id BIGINT,
  started_at TIMESTAMP,
  ended_at TIMESTAMP,
  result_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  remark TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permission (
  id BIGSERIAL PRIMARY KEY,
  role VARCHAR(32) NOT NULL,
  permission_key VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_role_permission UNIQUE (role, permission_key)
);

ALTER TABLE doctor_schedule ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE appointment_slot ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE patient ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_device_department_status ON medical_device(department_id, status);
CREATE INDEX IF NOT EXISTS idx_device_usage_device_time ON device_usage_record(device_id, started_at);
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON role_permission(role, enabled);
CREATE INDEX IF NOT EXISTS idx_patient_name_phone ON patient(name, phone);
CREATE INDEX IF NOT EXISTS idx_registration_status_time ON registration(status, appointment_time);
CREATE INDEX IF NOT EXISTS idx_registration_department_time ON registration(department_id, appointment_time);

INSERT INTO role_permission (role, permission_key, enabled) VALUES
  ('ADMIN', 'dashboard:view', TRUE),
  ('ADMIN', 'department:manage', TRUE),
  ('ADMIN', 'doctor:manage', TRUE),
  ('ADMIN', 'drug:manage', TRUE),
  ('ADMIN', 'schedule:manage', TRUE),
  ('ADMIN', 'triage:manage', TRUE),
  ('ADMIN', 'device:manage', TRUE),
  ('ADMIN', 'patient:manage', TRUE),
  ('ADMIN', 'statistics:view', TRUE),
  ('ADMIN', 'statistics:export', TRUE),
  ('ADMIN', 'account:manage', TRUE),
  ('ADMIN', 'permission:manage', TRUE),
  ('ADMIN', 'knowledge:manage', TRUE),
  ('ADMIN', 'prompt:manage', TRUE),
  ('ADMIN', 'dict:manage', TRUE),
  ('ADMIN', 'search:view', TRUE),
  ('DOCTOR', 'doctor-dashboard:view', TRUE),
  ('DOCTOR', 'doctor-schedule:view', TRUE),
  ('DOCTOR', 'doctor-consult:manage', TRUE),
  ('PATIENT', 'patient-portal:view', TRUE)
ON CONFLICT (role, permission_key) DO NOTHING;
