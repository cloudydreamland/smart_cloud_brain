CREATE DATABASE IF NOT EXISTS registration_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE registration_db;

CREATE TABLE IF NOT EXISTS registration (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  triage_record_id BIGINT,
  appointment_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_registration_patient (patient_id, status),
  INDEX idx_registration_doctor_time (doctor_id, appointment_time)
);
