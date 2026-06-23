CREATE TABLE IF NOT EXISTS patient_site_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(80) NOT NULL,
  config_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  version INTEGER NOT NULL DEFAULT 1,
  remark VARCHAR(255),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_patient_site_config_key_status_version UNIQUE (config_key, status, version)
);

CREATE INDEX IF NOT EXISTS idx_patient_site_config_key_status
  ON patient_site_config(config_key, status);

INSERT INTO role_permission (role, permission_key, enabled) VALUES
  ('ADMIN', 'patient-site:manage', TRUE)
ON CONFLICT (role, permission_key) DO NOTHING;
