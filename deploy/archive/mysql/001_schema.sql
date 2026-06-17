-- Archived legacy MySQL schema. Current database migration source is sql/flyway.
CREATE TABLE IF NOT EXISTS patient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  gender VARCHAR(10),
  age INT,
  allergy_history VARCHAR(500),
  past_history VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  department_id BIGINT NOT NULL,
  title VARCHAR(50),
  specialty VARCHAR(500),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doctor_department (department_id),
  CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department(id)
);

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

CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drug (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  specification VARCHAR(100),
  contraindication TEXT,
  interaction_rule TEXT,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_drug_name (name),
  INDEX idx_drug_status (status)
);

CREATE TABLE IF NOT EXISTS knowledge_entry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  symptoms TEXT NOT NULL,
  risk_signals TEXT,
  advice TEXT NOT NULL,
  department_code VARCHAR(50),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_knowledge_status (status),
  INDEX idx_knowledge_department (department_code)
);

INSERT INTO department (id, code, name, description)
VALUES (1, 'CARDIOLOGY', '心内科', '心血管疾病诊疗科室')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO department (id, code, name, description)
VALUES (2, 'GENERAL', '全科/轻症门诊', '校园常见轻症初诊与复诊')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status)
VALUES (1, '张医生', '13900000001', '$2a$mock', 1, '主任医师', '胸痛、心悸、高血压', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status)
VALUES (2, '李医生', '13900000002', '{bcrypt}$2a$12$u7TPtMrkgSKTxjIj7cACBOW1CdobARmBR0Hr8CwCMDxYMwsypQiiu', 2, '主治医师', '感冒发热、咽痛、腹泻、皮肤过敏', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO admin_user (id, username, password_hash, name, status)
VALUES (1, 'admin', '{bcrypt}$2a$12$u7TPtMrkgSKTxjIj7cACBOW1CdobARmBR0Hr8CwCMDxYMwsypQiiu', '系统管理员', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status)
VALUES (1, '阿司匹林', '100mg', '活动性出血禁用', '与抗凝药同用出血风险升高', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status)
VALUES (2, '对乙酰氨基酚', '0.5g', '严重肝功能不全慎用', '避免与其他含对乙酰氨基酚复方药重复使用', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO knowledge_entry (id, title, symptoms, risk_signals, advice, department_code, status)
VALUES
  (1, '普通感冒', '鼻塞、流涕、咽痛、低热、轻微咳嗽', '持续高热、呼吸困难、胸痛、意识异常', '注意休息和补液，症状加重或超过三天建议线下就诊。', 'GENERAL', 'ENABLED'),
  (2, '急性腹泻', '腹泻、腹痛、恶心、轻度乏力', '便血、严重脱水、持续高热、剧烈腹痛', '补液和清淡饮食，出现危险信号时及时就医。', 'GENERAL', 'ENABLED')
ON DUPLICATE KEY UPDATE title = VALUES(title);
