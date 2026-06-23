CREATE DATABASE IF NOT EXISTS patient_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE patient_db;

CREATE TABLE IF NOT EXISTS patient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  gender VARCHAR(10),
  age INT,
  allergy_history VARCHAR(500),
  past_history VARCHAR(1000),
  address VARCHAR(255),
  emergency_contact VARCHAR(80),
  emergency_phone VARCHAR(30),
  blood_type VARCHAR(10),
  height_cm INT,
  weight_kg DECIMAL(5,2),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS patient_visitor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
  weight_kg DECIMAL(5,2),
  allergy_history VARCHAR(500),
  past_history VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_patient_visitor_owner (owner_patient_id)
);
