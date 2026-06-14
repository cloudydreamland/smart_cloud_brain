USE auth_db;

INSERT INTO admin_user (id, username, password_hash, name, status)
VALUES (1, 'admin', '$2a$mock', '系统管理员', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

USE doctor_db;

INSERT INTO department (id, code, name, description)
VALUES (1, 'CARDIOLOGY', '心内科', '心血管疾病诊疗科室')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status)
VALUES (1, '张医生', '13900000001', '$2a$mock', 1, '主任医师', '胸痛、心悸、高血压', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status)
VALUES (1, '阿司匹林', '100mg', '活动性出血禁用', '与抗凝药同用出血风险升高', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

USE ai_db;

INSERT INTO prompt_template (
  id,
  task_type,
  department_code,
  template_name,
  template_content,
  output_schema,
  version,
  enabled
)
VALUES (
  1,
  'MEDICAL_RECORD_GENERATE',
  'CARDIOLOGY',
  '心内科病历生成模板',
  '请基于患者主诉、病史和检查信息生成结构化门诊病历，输出诊断建议和治疗建议。',
  JSON_OBJECT('fields', JSON_ARRAY('chiefComplaint', 'diagnosis', 'treatmentAdvice')),
  'v1',
  TRUE
)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);
