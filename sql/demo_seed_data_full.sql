-- ============================================================
-- 智慧云脑诊疗平台 — 仿真业务数据种子
-- 覆盖：排班、号源、分诊、挂号、病历、处方、通知、AI日志
-- 执行前确认基础表（department/doctor/patient/drug）已有数据
-- ============================================================

-- 0. 额外患者（让数据更丰富）
INSERT INTO patient (id, name, phone, password_hash, gender, age, allergy_history, past_history) VALUES
  (2, '李明', '13800000002', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 45, '青霉素过敏', '高血压病史5年'),
  (3, '王芳', '13800000003', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 32, '无', '无特殊'),
  (4, '赵强', '13800000004', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 67, '磺胺类过敏', '糖尿病10年，冠心病3年'),
  (5, '陈丽', '13800000005', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 28, '花粉过敏', '哮喘病史')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('patient', 'id'), COALESCE((SELECT MAX(id) FROM patient), 5));

-- 1. 排班（未来两周，三位医生都有排班）
INSERT INTO doctor_schedule (id, doctor_id, department_id, work_date, time_range, capacity, status) VALUES
  -- 张医生（心内科）
  (10, 1, 1, CURRENT_DATE + 1, '09:00-12:00', 20, 'PUBLISHED'),
  (11, 1, 1, CURRENT_DATE + 1, '14:00-17:00', 15, 'PUBLISHED'),
  (12, 1, 1, CURRENT_DATE + 2, '09:00-12:00', 20, 'PUBLISHED'),
  (13, 1, 1, CURRENT_DATE + 3, '09:00-12:00', 20, 'PUBLISHED'),
  (14, 1, 1, CURRENT_DATE + 3, '14:00-17:00', 15, 'PUBLISHED'),
  (15, 1, 1, CURRENT_DATE + 5, '09:00-12:00', 20, 'PUBLISHED'),
  (16, 1, 1, CURRENT_DATE + 7, '09:00-12:00', 20, 'PUBLISHED'),
  -- 李医生（全科门诊）
  (20, 2, 2, CURRENT_DATE + 1, '09:00-12:00', 25, 'PUBLISHED'),
  (21, 2, 2, CURRENT_DATE + 1, '14:00-17:00', 20, 'PUBLISHED'),
  (22, 2, 2, CURRENT_DATE + 2, '09:00-12:00', 25, 'PUBLISHED'),
  (23, 2, 2, CURRENT_DATE + 4, '09:00-12:00', 25, 'PUBLISHED'),
  (24, 2, 2, CURRENT_DATE + 6, '14:00-17:00', 20, 'PUBLISHED'),
  -- 王医生（呼吸内科）
  (30, 3, 3, CURRENT_DATE + 1, '09:00-12:00', 18, 'PUBLISHED'),
  (31, 3, 3, CURRENT_DATE + 2, '14:00-17:00', 15, 'PUBLISHED'),
  (32, 3, 3, CURRENT_DATE + 3, '09:00-12:00', 18, 'PUBLISHED'),
  (33, 3, 3, CURRENT_DATE + 5, '14:00-17:00', 15, 'PUBLISHED'),
  (34, 3, 3, CURRENT_DATE + 7, '09:00-12:00', 18, 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('doctor_schedule', 'id'), COALESCE((SELECT MAX(id) FROM doctor_schedule), 40));

-- 2. 号源（与排班对应）
INSERT INTO appointment_slot (id, schedule_id, doctor_id, department_id, start_time, end_time, capacity, remaining_capacity, status) VALUES
  (10, 10, 1, 1, CURRENT_DATE + INTERVAL '1 day 9 hours', CURRENT_DATE + INTERVAL '1 day 12 hours', 20, 14, 'AVAILABLE'),
  (11, 11, 1, 1, CURRENT_DATE + INTERVAL '1 day 14 hours', CURRENT_DATE + INTERVAL '1 day 17 hours', 15, 15, 'AVAILABLE'),
  (12, 12, 1, 1, CURRENT_DATE + INTERVAL '2 day 9 hours', CURRENT_DATE + INTERVAL '2 day 12 hours', 20, 18, 'AVAILABLE'),
  (13, 13, 1, 1, CURRENT_DATE + INTERVAL '3 day 9 hours', CURRENT_DATE + INTERVAL '3 day 12 hours', 20, 20, 'AVAILABLE'),
  (20, 20, 2, 2, CURRENT_DATE + INTERVAL '1 day 9 hours', CURRENT_DATE + INTERVAL '1 day 12 hours', 25, 20, 'AVAILABLE'),
  (21, 21, 2, 2, CURRENT_DATE + INTERVAL '1 day 14 hours', CURRENT_DATE + INTERVAL '1 day 17 hours', 20, 17, 'AVAILABLE'),
  (22, 22, 2, 2, CURRENT_DATE + INTERVAL '2 day 9 hours', CURRENT_DATE + INTERVAL '2 day 12 hours', 25, 25, 'AVAILABLE'),
  (30, 30, 3, 3, CURRENT_DATE + INTERVAL '1 day 9 hours', CURRENT_DATE + INTERVAL '1 day 12 hours', 18, 12, 'AVAILABLE'),
  (31, 31, 3, 3, CURRENT_DATE + INTERVAL '2 day 14 hours', CURRENT_DATE + INTERVAL '2 day 17 hours', 15, 15, 'AVAILABLE'),
  (32, 32, 3, 3, CURRENT_DATE + INTERVAL '3 day 9 hours', CURRENT_DATE + INTERVAL '3 day 12 hours', 18, 18, 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('appointment_slot', 'id'), COALESCE((SELECT MAX(id) FROM appointment_slot), 40));

-- 3. 分诊记录
INSERT INTO triage_record (id, patient_id, chief_complaint, recommended_department, recommended_doctor_ids, assigned_doctor_id, reason, ai_result_json, status) VALUES
  (1, 1, '胸痛三天，活动后加重，伴有轻微气短', '心内科', '1', 1, '主诉提示心内科评估，建议完善心电图和心肌酶检查', '{"department":"CARDIOLOGY","confidence":0.92,"urgency":"HIGH"}', 'ASSIGNED'),
  (2, 2, '反复头晕一周，血压偏高', '心内科', '1', 1, '头晕伴血压升高，需排除高血压急症和脑血管问题', '{"department":"CARDIOLOGY","confidence":0.85,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (3, 3, '咳嗽咳痰一周，黄痰，低热', '呼吸内科', '3', 3, '呼吸道感染表现，建议呼吸内科评估', '{"department":"RESPIRATORY","confidence":0.88,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (4, 4, '胸闷心悸两天，活动后加重', '心内科', '1', NULL, '心悸伴胸闷，需心内科排查心律失常', '{"department":"CARDIOLOGY","confidence":0.90,"urgency":"HIGH"}', 'AI_RECOMMENDED'),
  (5, 5, '反复咳嗽，喘息，有过敏性哮喘病史', '呼吸内科', '3', 3, '哮喘急性发作可能，建议呼吸内科处理', '{"department":"RESPIRATORY","confidence":0.91,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (6, 1, '胃痛、反酸一周，空腹时明显', '全科门诊', '2', 2, '消化道症状，建议全科门诊初步评估', '{"department":"GENERAL","confidence":0.80,"urgency":"LOW"}', 'ASSIGNED'),
  (7, 4, '血糖控制不佳，近期口渴多尿加重', '全科门诊', '2', NULL, '糖尿病复诊，需调整用药方案', '{"department":"GENERAL","confidence":0.87,"urgency":"MEDIUM"}', 'AI_RECOMMENDED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('triage_record', 'id'), COALESCE((SELECT MAX(id) FROM triage_record), 10));

-- 4. 挂号记录
INSERT INTO registration (id, patient_id, doctor_id, department_id, triage_record_id, slot_id, appointment_time, status) VALUES
  -- 已完成的挂号（今天之前）
  (1, 1, 1, 1, 1, 10, CURRENT_TIMESTAMP - INTERVAL '2 day', 'COMPLETED'),
  (2, 3, 3, 3, 3, 30, CURRENT_TIMESTAMP - INTERVAL '1 day', 'COMPLETED'),
  -- 今天的待接诊
  (3, 2, 1, 1, 2, 10, CURRENT_TIMESTAMP + INTERVAL '2 hour', 'CONFIRMED'),
  (4, 5, 3, 3, 5, 30, CURRENT_TIMESTAMP + INTERVAL '3 hour', 'CONFIRMED'),
  -- 明天的预约
  (5, 4, 1, 1, 4, 12, CURRENT_DATE + INTERVAL '1 day 9 hours', 'CONFIRMED'),
  (6, 1, 2, 2, 6, 20, CURRENT_DATE + INTERVAL '1 day 14 hours', 'CONFIRMED'),
  (7, 4, 2, 2, 7, 21, CURRENT_DATE + INTERVAL '1 day 14 hours', 'CREATED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('registration', 'id'), COALESCE((SELECT MAX(id) FROM registration), 10));

-- 5. 病历（已完成的挂号对应的病历）
INSERT INTO medical_record (id, patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, diagnosis, treatment_advice, ai_generated) VALUES
  (1, 1, 1, 1,
   '胸痛三天，活动后加重',
   '患者3天前无明显诱因出现胸骨后疼痛，呈闷痛，持续约10分钟，活动后加重，休息后缓解。伴有轻微气短，无恶心呕吐，无放射痛。',
   '高血压病史2年，规律服用氨氯地平。否认糖尿病、冠心病史。',
   'BP 145/92mmHg, HR 78bpm, 心肺听诊无明显异常，双下肢无水肿。',
   '冠心病 待排；高血压2级',
   '1. 完善心电图、心肌酶谱、心脏彩超\n2. 低盐低脂饮食\n3. 继续服用氨氯地平控制血压\n4. 如胸痛加重立即就诊',
   TRUE),
  (2, 3, 3, 2,
   '咳嗽咳痰一周，伴低热',
   '1周前受凉后出现咳嗽，咳黄色粘痰，量中等，体温波动在37.5-38.2°C，无胸痛，无气促。',
   '否认慢性呼吸系统疾病史。',
   'T 37.8°C, 双肺呼吸音粗，右下肺可闻及少量湿啰音，余无明显异常。',
   '急性支气管炎',
   '1. 阿莫西林胶囊 0.5g tid x 7天\n2. 氨溴索口服液 10ml tid\n3. 多饮水，注意休息\n4. 1周后复诊',
   TRUE)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('medical_record', 'id'), COALESCE((SELECT MAX(id) FROM medical_record), 5));

-- 6. 处方
INSERT INTO prescription (id, patient_id, doctor_id, medical_record_id, registration_id, risk_level, status) VALUES
  (1, 1, 1, 1, 1, 'LOW', 'ACTIVE'),
  (2, 3, 3, 2, 2, 'LOW', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO prescription_item (id, prescription_id, drug_name, dosage, frequency, usage_method, days, remark) VALUES
  (1, 1, '阿司匹林', '100mg', '每日一次', '口服', 30, '饭后服用，注意出血风险'),
  (2, 1, '氨氯地平', '5mg', '每日一次', '口服', 30, '继续当前降压方案'),
  (3, 2, '阿莫西林', '0.5g', '每日三次', '口服', 7, '青霉素不过敏方可使用'),
  (4, 2, '氨溴索', '10ml', '每日三次', '口服', 7, '化痰')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('prescription', 'id'), COALESCE((SELECT MAX(id) FROM prescription), 5));
SELECT setval(pg_get_serial_sequence('prescription_item', 'id'), COALESCE((SELECT MAX(id) FROM prescription_item), 10));

-- 7. 处方审核记录
INSERT INTO prescription_check_record (id, prescription_id, patient_id, doctor_id, risk_level, suggestions, interactions, ai_result_json, created_at) VALUES
  (1, 1, 1, 1, 'LOW', '处方审核通过，无明显风险', '[]', '{"riskLevel":"LOW","suggestions":[],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (2, 2, 3, 3, 'LOW', '处方审核通过', '[]', '{"riskLevel":"LOW","suggestions":[],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('prescription_check_record', 'id'), COALESCE((SELECT MAX(id) FROM prescription_check_record), 5));

-- 8. 通知消息（医生端通知）
INSERT INTO notification_message (id, doctor_id, patient_id, prescription_id, type, title, content, risk_level, read_status) VALUES
  (1, 1, 1, 1, 'PRESCRIPTION_LOW_RISK', '处方审核通过', '张医生为患者测试患者开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ'),
  (2, 1, 2, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者李明已预约明天上午心内科门诊，请及时查看。', NULL, 'UNREAD'),
  (3, 3, 3, 2, 'PRESCRIPTION_LOW_RISK', '处方审核通过', '王医生为患者王芳开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ'),
  (4, 3, 5, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者陈丽已预约今天上午呼吸内科门诊。', NULL, 'UNREAD'),
  (5, 1, 4, NULL, 'TRIAGE_ASSIGNED', '分诊分配', '患者赵强的分诊已分配给您，主诉：胸闷心悸两天，活动后加重。', NULL, 'UNREAD'),
  (6, 2, 1, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者测试患者已预约明天下午全科门诊。', NULL, 'UNREAD')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('notification_message', 'id'), COALESCE((SELECT MAX(id) FROM notification_message), 10));

-- 9. AI 生成日志
INSERT INTO ai_generation_log (id, task_type, provider, model, request_id, biz_id, patient_id, doctor_id, input_summary, output_summary, success, latency_ms, status, duration_ms, created_at) VALUES
  (1, 'TRIAGE', 'dify', 'mock', 'req-001', 1, 1, NULL, '胸痛三天，活动后加重', '推荐心内科，置信度0.92', TRUE, 850, 'SUCCESS', 850, CURRENT_TIMESTAMP - INTERVAL '3 day'),
  (2, 'TRIAGE', 'dify', 'mock', 'req-002', 2, 2, NULL, '反复头晕一周，血压偏高', '推荐心内科，置信度0.85', TRUE, 720, 'SUCCESS', 720, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (3, 'TRIAGE', 'dify', 'mock', 'req-003', 3, 3, NULL, '咳嗽咳痰一周，黄痰，低热', '推荐呼吸内科，置信度0.88', TRUE, 680, 'SUCCESS', 680, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (4, 'MEDICAL_RECORD', 'dify', 'mock', 'req-004', 1, 1, 1, '胸痛三天活动后加重', '生成结构化病历', TRUE, 1200, 'SUCCESS', 1200, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (5, 'PRESCRIPTION_CHECK', 'dify', 'mock', 'req-005', 1, 1, 1, '阿司匹林+氨氯地平', '风险等级LOW', TRUE, 560, 'SUCCESS', 560, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (6, 'TRIAGE', 'mock-provider', 'mock', 'req-006', 4, 4, NULL, '胸闷心悸两天，活动后加重', '确定性降级：主诉提示心内科评估。', TRUE, 15, 'DEGRADED', 15, CURRENT_TIMESTAMP - INTERVAL '12 hour'),
  (7, 'PRESCRIPTION_CHECK', 'mock-provider', 'mock', 'req-007', 2, 3, 3, '阿莫西林+氨溴索', '确定性降级：未发现明显高风险药物相互作用。', TRUE, 12, 'DEGRADED', 12, CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('ai_generation_log', 'id'), COALESCE((SELECT MAX(id) FROM ai_generation_log), 10));

-- 10. AI 排班建议（管理端 AI 排班建议区）
INSERT INTO ai_schedule_suggestion (id, doctor_id, department_id, work_date, time_range, capacity, reason, status, source, degraded) VALUES
  (1, 1, 1, CURRENT_DATE + 8, '09:00-12:00', 20, '周一上午心内科就诊量较高，建议增加号源', 'PENDING', 'dify', FALSE),
  (2, 2, 2, CURRENT_DATE + 8, '14:00-17:00', 25, '全科门诊下午复诊患者较多', 'PENDING', 'mock', TRUE),
  (3, 3, 3, CURRENT_DATE + 9, '09:00-12:00', 18, '呼吸内科季节性疾病高发期', 'PENDING', 'dify', FALSE)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('ai_schedule_suggestion', 'id'), COALESCE((SELECT MAX(id) FROM ai_schedule_suggestion), 5));
