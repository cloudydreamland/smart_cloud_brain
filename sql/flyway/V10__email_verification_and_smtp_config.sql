ALTER TABLE patient ADD COLUMN IF NOT EXISTS email VARCHAR(120);
CREATE UNIQUE INDEX IF NOT EXISTS uk_patient_email ON patient(email) WHERE email IS NOT NULL;

CREATE TABLE IF NOT EXISTS patient_email_verification_code (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(120) NOT NULL,
  phone VARCHAR(20),
  purpose VARCHAR(40) NOT NULL DEFAULT 'REGISTER',
  code_hash VARCHAR(128) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP,
  failed_attempts INT NOT NULL DEFAULT 0,
  last_sent_ip VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_email_code_email_purpose ON patient_email_verification_code(email, purpose, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_patient_email_code_expires ON patient_email_verification_code(expires_at);

CREATE TABLE IF NOT EXISTS system_email_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(40) NOT NULL DEFAULT 'SMTP',
  host VARCHAR(255) NOT NULL DEFAULT '',
  port INT NOT NULL DEFAULT 465,
  username VARCHAR(255) NOT NULL DEFAULT '',
  password_cipher TEXT,
  from_address VARCHAR(255) NOT NULL DEFAULT '',
  from_name VARCHAR(120) NOT NULL DEFAULT '',
  ssl_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  starttls_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_system_email_config_key UNIQUE (config_key)
);

INSERT INTO role_permission (role, permission_key, enabled) VALUES
  ('ADMIN', 'system-config:manage', TRUE)
ON CONFLICT (role, permission_key) DO NOTHING;
