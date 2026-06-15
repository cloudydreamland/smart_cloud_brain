INSERT INTO department (id, code, name, description) VALUES
  (1, 'CARDIOLOGY', 'Cardiology', 'Chest pain, palpitation and hypertension clinic'),
  (2, 'GENERAL', 'General Clinic', 'Common mild symptoms and follow-up clinic'),
  (3, 'RESPIRATORY', 'Respiratory', 'Cough, asthma and respiratory infection clinic')
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status) VALUES
  (1, 'Dr Zhang', '13900000001', '{plain}123456', 1, 'Chief Physician', 'Chest pain, palpitation, hypertension', 'ENABLED'),
  (2, 'Dr Li', '13900000002', '{plain}123456', 2, 'Attending Physician', 'Fever, sore throat, diarrhea, allergy', 'ENABLED'),
  (3, 'Dr Wang', '13900000003', '{plain}123456', 3, 'Associate Chief Physician', 'Cough, asthma, lung infection', 'ENABLED')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  phone = EXCLUDED.phone,
  password_hash = EXCLUDED.password_hash,
  department_id = EXCLUDED.department_id,
  title = EXCLUDED.title,
  specialty = EXCLUDED.specialty,
  status = EXCLUDED.status;

INSERT INTO patient (id, name, phone, password_hash, gender, age, allergy_history, past_history) VALUES
  (1, 'Test Patient', '13800000001', '{plain}123456', 'FEMALE', 21, 'No known drug allergy', 'No special medical history')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  phone = EXCLUDED.phone,
  password_hash = EXCLUDED.password_hash,
  gender = EXCLUDED.gender,
  age = EXCLUDED.age,
  allergy_history = EXCLUDED.allergy_history,
  past_history = EXCLUDED.past_history;

INSERT INTO admin_user (id, username, password_hash, name, status) VALUES
  (1, 'admin', '{plain}123456', 'System Admin', 'ENABLED')
ON CONFLICT (id) DO UPDATE SET
  username = EXCLUDED.username,
  password_hash = EXCLUDED.password_hash,
  name = EXCLUDED.name,
  status = EXCLUDED.status;

INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status) VALUES
  (1, 'Aspirin', '100mg', 'Contraindicated in active bleeding', 'Higher bleeding risk with anticoagulants', 'ENABLED'),
  (2, 'Acetaminophen', '0.5g', 'Use with caution in severe liver dysfunction', 'Avoid duplicate use with compound cold medicine', 'ENABLED'),
  (3, 'Loratadine', '10mg', 'Contraindicated if allergic to this drug', 'Use carefully with strong CYP3A4 inhibitors', 'ENABLED'),
  (4, 'Amoxicillin', '0.25g', 'Contraindicated in penicillin allergy', 'Rash risk increases with allopurinol', 'ENABLED')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  specification = EXCLUDED.specification,
  contraindication = EXCLUDED.contraindication,
  interaction_rule = EXCLUDED.interaction_rule,
  status = EXCLUDED.status;

INSERT INTO prompt_template (id, task_type, department_code, template_name, template_content, output_schema, version, enabled) VALUES
  (1, 'TRIAGE', 'GENERAL', 'GENERAL_TRIAGE_v1', 'Recommend department and priority from chief complaint. Return reason.', '{"type":"object"}', 'v1', TRUE),
  (2, 'MEDICAL_RECORD', 'GENERAL', 'GENERAL_MEDICAL_RECORD_v1', 'Generate structured medical record JSON from consultation text. Doctor must confirm final result.', '{"type":"object"}', 'v1', TRUE),
  (3, 'PRESCRIPTION_CHECK', 'GENERAL', 'GENERAL_PRESCRIPTION_CHECK_v1', 'Check contraindications, interactions and medication risk from patient and drug list.', '{"type":"object"}', 'v1', TRUE)
ON CONFLICT (id) DO UPDATE SET
  task_type = EXCLUDED.task_type,
  department_code = EXCLUDED.department_code,
  template_name = EXCLUDED.template_name,
  template_content = EXCLUDED.template_content,
  output_schema = EXCLUDED.output_schema,
  version = EXCLUDED.version,
  enabled = EXCLUDED.enabled;

INSERT INTO knowledge_entry (id, title, symptoms, risk_signals, advice, department_code, status) VALUES
  (1, 'Common Cold', 'Stuffy nose, runny nose, sore throat, low fever, mild cough', 'Persistent high fever, dyspnea, chest pain, abnormal consciousness', 'Rest and drink fluids. Seek offline care if symptoms worsen or last more than three days.', 'GENERAL', 'ENABLED'),
  (2, 'Acute Diarrhea', 'Diarrhea, abdominal pain, nausea, mild fatigue', 'Bloody stool, severe dehydration, persistent high fever, severe abdominal pain', 'Drink fluids and eat light food. Seek medical care if risk signals appear.', 'GENERAL', 'ENABLED'),
  (3, 'Chest Pain', 'Chest pain, chest tightness, palpitation, shortness of breath after activity', 'Crushing chest pain, cold sweat, syncope, persistent pain', 'Go to emergency care for risk signals. Otherwise visit cardiology for evaluation.', 'CARDIOLOGY', 'ENABLED'),
  (4, 'Acute Cough', 'Cough, itchy throat, small amount of white sputum', 'Hemoptysis, dyspnea, persistent high fever, low oxygen saturation', 'Drink water and rest. Visit respiratory clinic if cough lasts over a week or risk signals appear.', 'RESPIRATORY', 'ENABLED')
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  symptoms = EXCLUDED.symptoms,
  risk_signals = EXCLUDED.risk_signals,
  advice = EXCLUDED.advice,
  department_code = EXCLUDED.department_code,
  status = EXCLUDED.status;

SELECT setval(pg_get_serial_sequence('department', 'id'), COALESCE((SELECT MAX(id) FROM department), 1));
SELECT setval(pg_get_serial_sequence('doctor', 'id'), COALESCE((SELECT MAX(id) FROM doctor), 1));
SELECT setval(pg_get_serial_sequence('patient', 'id'), COALESCE((SELECT MAX(id) FROM patient), 1));
SELECT setval(pg_get_serial_sequence('admin_user', 'id'), COALESCE((SELECT MAX(id) FROM admin_user), 1));
SELECT setval(pg_get_serial_sequence('drug', 'id'), COALESCE((SELECT MAX(id) FROM drug), 1));
SELECT setval(pg_get_serial_sequence('prompt_template', 'id'), COALESCE((SELECT MAX(id) FROM prompt_template), 1));
SELECT setval(pg_get_serial_sequence('knowledge_entry', 'id'), COALESCE((SELECT MAX(id) FROM knowledge_entry), 1));
