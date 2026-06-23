ALTER TABLE patient ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE patient ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(80);
ALTER TABLE patient ADD COLUMN IF NOT EXISTS emergency_phone VARCHAR(30);
ALTER TABLE patient ADD COLUMN IF NOT EXISTS blood_type VARCHAR(10);
ALTER TABLE patient ADD COLUMN IF NOT EXISTS height_cm INT;
ALTER TABLE patient ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(5, 2);

CREATE TABLE IF NOT EXISTS patient_visitor (
  id BIGSERIAL PRIMARY KEY,
  owner_patient_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  relationship VARCHAR(40),
  phone VARCHAR(30),
  gender VARCHAR(10),
  age INT,
  address VARCHAR(255),
  emergency_contact VARCHAR(80),
  emergency_phone VARCHAR(30),
  blood_type VARCHAR(10),
  height_cm INT,
  weight_kg NUMERIC(5, 2),
  allergy_history VARCHAR(500),
  past_history VARCHAR(1000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_visitor_owner ON patient_visitor(owner_patient_id);
