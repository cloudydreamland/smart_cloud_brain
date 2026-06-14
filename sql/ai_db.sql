CREATE DATABASE IF NOT EXISTS ai_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_db;

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_type VARCHAR(50) NOT NULL,
  department_code VARCHAR(50),
  template_name VARCHAR(100) NOT NULL,
  template_content TEXT NOT NULL,
  output_schema JSON,
  version VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_prompt_task_department (task_type, department_code, enabled)
);

CREATE TABLE IF NOT EXISTS ai_generation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_type VARCHAR(50) NOT NULL,
  biz_id BIGINT,
  patient_id BIGINT,
  doctor_id BIGINT,
  prompt_template_id BIGINT,
  request_summary TEXT,
  response_summary TEXT,
  raw_result_json JSON,
  status VARCHAR(20) NOT NULL,
  error_message TEXT,
  duration_ms INT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ai_log_task_time (task_type, created_at),
  INDEX idx_ai_log_patient (patient_id)
);
