CREATE DATABASE IF NOT EXISTS triage_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE triage_db;

CREATE TABLE IF NOT EXISTS triage_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  chief_complaint TEXT NOT NULL,
  recommended_department VARCHAR(100),
  recommended_doctor_ids VARCHAR(255),
  reason TEXT,
  ai_result_json JSON,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_triage_patient_time (patient_id, created_at)
);
