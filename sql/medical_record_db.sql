CREATE DATABASE IF NOT EXISTS medical_record_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medical_record_db;

CREATE TABLE IF NOT EXISTS medical_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  chief_complaint TEXT NOT NULL,
  present_illness TEXT,
  past_history TEXT,
  physical_exam TEXT,
  diagnosis TEXT NOT NULL,
  treatment_advice TEXT,
  ai_generated BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_record_patient (patient_id),
  INDEX idx_record_doctor (doctor_id)
);
