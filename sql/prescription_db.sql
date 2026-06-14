CREATE DATABASE IF NOT EXISTS prescription_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE prescription_db;

CREATE TABLE IF NOT EXISTS prescription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  medical_record_id BIGINT,
  risk_level VARCHAR(20),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_prescription_patient (patient_id),
  INDEX idx_prescription_doctor (doctor_id)
);

CREATE TABLE IF NOT EXISTS prescription_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prescription_id BIGINT NOT NULL,
  drug_name VARCHAR(100) NOT NULL,
  dosage VARCHAR(100) NOT NULL,
  frequency VARCHAR(100) NOT NULL,
  usage_method VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS prescription_check_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prescription_id BIGINT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  risk_level VARCHAR(20) NOT NULL,
  suggestions TEXT,
  interactions TEXT,
  ai_result_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
