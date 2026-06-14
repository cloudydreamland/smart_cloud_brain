CREATE DATABASE IF NOT EXISTS notification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE notification_db;

CREATE TABLE IF NOT EXISTS notification_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doctor_id BIGINT NOT NULL,
  patient_id BIGINT,
  prescription_id BIGINT,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  risk_level VARCHAR(20),
  read_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notification_doctor_status (doctor_id, read_status)
);
