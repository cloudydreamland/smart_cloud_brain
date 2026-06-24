-- ============================================================
-- 智慧云脑诊疗平台 — 种子数据扩充（三端）
-- 执行前确保基础表已有初始数据
-- ============================================================

-- 0. 新增科室（消化内科、神经内科）
INSERT INTO department (id, code, name, description) VALUES
  (4, 'GASTRO', '消化内科', '胃痛、腹泻、肝胆疾病等消化系统疾病诊疗'),
  (5, 'NEURO', '神经内科', '头痛、头晕、脑血管疾病等神经系统疾病诊疗')
ON CONFLICT (id) DO NOTHING;

-- 1. 新增医生（4位）
INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status) VALUES
  (4, '刘医生', '13900000004', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 4, '主任医师', '胃炎、消化性溃疡、肝硬化', 'ENABLED'),
  (5, '陈医生', '13900000005', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 4, '主治医师', '胃食管反流、胆囊炎、胰腺炎', 'ENABLED'),
  (6, '孙医生', '13900000006', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 5, '副主任医师', '头痛、眩晕、脑梗塞', 'ENABLED'),
  (7, '周医生', '13900000007', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 5, '主治医师', '帕金森、癫痫、周围神经病', 'ENABLED')
ON CONFLICT (phone) DO UPDATE SET
  name = EXCLUDED.name, password_hash = EXCLUDED.password_hash,
  department_id = EXCLUDED.department_id, title = EXCLUDED.title,
  specialty = EXCLUDED.specialty, status = EXCLUDED.status;

-- 2. 新增患者（8位，覆盖不同年龄性别病史）
INSERT INTO patient (id, name, phone, password_hash, gender, age, allergy_history, past_history, address, emergency_contact, emergency_phone, blood_type, height_cm, weight_kg) VALUES
  (6, '周小明', '13800000006', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 8, '无', '无特殊', '北京市朝阳区建国路88号', '周建国', '13700000006', 'A', 132, 28.5),
  (7, '吴芳', '13800000007', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 22, '花粉过敏', '无特殊', '北京市海淀区中关村大街1号', '吴强', '13700000007', 'O', 165, 52.0),
  (8, '郑伟', '13800000008', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 35, '磺胺类过敏', '高血压病史3年', '北京市西城区金融街10号', '郑丽', '13700000008', 'B', 175, 78.0),
  (9, '黄丽华', '13800000009', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 45, '青霉素过敏', '糖尿病5年，高血脂', '北京市东城区王府井大街5号', '黄伟', '13700000009', 'AB', 160, 65.5),
  (10, '林志强', '13800000010', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 52, '无', '冠心病2年，支架术后', '北京市丰台区南三环西路16号', '林芳', '13700000010', 'O', 170, 72.0),
  (11, '何秀英', '13800000011', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 68, '头孢类过敏', '高血压15年，糖尿病8年', '北京市朝阳区望京西路8号', '何强', '13700000011', 'A', 155, 60.0),
  (12, '马俊', '13800000012', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 75, '无', '脑梗塞后遗症，帕金森病3年', '北京市石景山区古城大街12号', '马丽', '13700000012', 'B', 168, 65.0),
  (13, '杨雪', '13800000013', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 29, '海鲜过敏', '偏头痛病史5年', '北京市大兴区亦庄经济开发区', '杨磊', '13700000013', 'O', 162, 50.0)
ON CONFLICT (id) DO NOTHING;

-- 3. 患者陪诊人
INSERT INTO patient_visitor (owner_patient_id, name, relationship, phone, gender, age, blood_type, height_cm, weight_kg, allergy_history, past_history) VALUES
  (6, '周建国', '父亲', '13700000006', 'MALE', 38, 'A', 178, 80.0, '无', '无特殊'),
  (8, '郑丽', '妻子', '13700000008', 'FEMALE', 32, 'O', 163, 55.0, '无', '无特殊'),
  (11, '何强', '儿子', '13700000011', 'MALE', 42, 'A', 175, 75.0, '无', '无特殊'),
  (12, '马丽', '女儿', '13700000012', 'FEMALE', 48, 'B', 160, 58.0, '无', '甲状腺功能减退');

-- 4. 新增药品
INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status) VALUES
  (5, '奥美拉唑', '20mg', '对本品过敏禁用', '与氯吡格雷合用可能降低后者疗效', 'ENABLED'),
  (6, '甲钴胺', '0.5mg', '对本品过敏禁用', '避免与同一时间段输注其他药物', 'ENABLED'),
  (7, '阿托伐他汀', '20mg', '活动性肝病禁用', '与红霉素合用增加横纹肌溶解风险', 'ENABLED'),
  (8, '二甲双胍', '0.5g', '严重肾功能不全禁用', '与碘造影剂检查前后48小时停用', 'ENABLED'),
  (9, '布洛芬', '0.3g', '活动性消化性溃疡禁用', '与阿司匹林合用增加胃肠道出血风险', 'ENABLED'),
  (10, '氯吡格雷', '75mg', '活动性出血禁用', '与奥美拉唑合用可能降低抗血小板效果', 'ENABLED')
ON CONFLICT (id) DO NOTHING;

-- 5. 知识库扩充
INSERT INTO knowledge_entry (id, title, symptoms, risk_signals, advice, department_code, status) VALUES
  (5, '消化性溃疡', '上腹痛、反酸、嗳气、餐后腹胀', '呕血、黑便、剧烈腹痛、消瘦', '规律饮食，避免辛辣刺激。出现呕血或黑便立即就医。', 'GASTRO', 'ENABLED'),
  (6, '偏头痛', '单侧搏动性头痛、恶心、畏光、视觉先兆', '突发剧烈头痛、意识障碍、肢体无力', '记录头痛日记寻找诱因，发作时安静休息。出现危险信号立即急诊。', 'NEURO', 'ENABLED'),
  (7, '高血压', '头晕、头痛、心悸、视物模糊', '血压>180/120mmHg、胸痛、呼吸困难、意识改变', '低盐饮食，规律服药，每日监测血压。出现高血压急症立即就医。', 'CARDIOLOGY', 'ENABLED'),
  (8, '2型糖尿病', '多饮、多尿、多食、体重下降', '低血糖（出汗、心悸、意识模糊）、足部溃疡', '控制饮食，规律运动，遵医嘱用药，定期监测血糖。', 'GENERAL', 'ENABLED'),
  (9, '急性胃炎', '上腹不适、恶心、呕吐、食欲不振', '剧烈腹痛、呕血、脱水征象', '清淡饮食，避免刺激性食物。症状持续或加重需消化内科就诊。', 'GASTRO', 'ENABLED'),
  (10, '脑血管病后遗症', '肢体活动障碍、言语不利、认知下降', '突发意识障碍、肢体瘫痪加重、头痛呕吐', '坚持康复训练，控制基础病，定期复查。出现新发症状立即就医。', 'NEURO', 'ENABLED')
ON CONFLICT (id) DO NOTHING;

-- 6. 补充字典条目
INSERT INTO system_dict (id, dict_type, dict_key, dict_value, sort, status) VALUES
  (6, 'REGISTRATION_STATUS', 'CONFIRMED', '已确认', 3, 'ENABLED'),
  (7, 'REGISTRATION_STATUS', 'COMPLETED', '已完成', 4, 'ENABLED'),
  (8, 'REGISTRATION_STATUS', 'WAITING', '候诊中', 5, 'ENABLED'),
  (9, 'TRIAGE_STATUS', 'MANUAL_REQUIRED', '需人工复核', 4, 'ENABLED'),
  (10, 'PRESCRIPTION_STATUS', 'ACTIVE', '有效', 1, 'ENABLED'),
  (11, 'PRESCRIPTION_STATUS', 'COMPLETED', '已完成', 2, 'ENABLED'),
  (12, 'PRESCRIPTION_STATUS', 'CANCELLED', '已取消', 3, 'ENABLED'),
  (13, 'RISK_LEVEL', 'LOW', '低风险', 1, 'ENABLED'),
  (14, 'RISK_LEVEL', 'MEDIUM', '中风险', 2, 'ENABLED'),
  (15, 'RISK_LEVEL', 'HIGH', '高风险', 3, 'ENABLED')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Phase 2: 排班 + 号源
-- ============================================================

-- 7. 新增排班（7位医生，未来两周）
INSERT INTO doctor_schedule (id, doctor_id, department_id, work_date, time_range, capacity, status) VALUES
  -- 张医生（心内科）补充
  (40, 1, 1, CURRENT_DATE + 8, '09:00-12:00', 20, 'PUBLISHED'),
  (41, 1, 1, CURRENT_DATE + 10, '14:00-17:00', 15, 'PUBLISHED'),
  -- 李医生（全科）补充
  (42, 2, 2, CURRENT_DATE + 8, '14:00-17:00', 25, 'PUBLISHED'),
  (43, 2, 2, CURRENT_DATE + 10, '09:00-12:00', 25, 'PUBLISHED'),
  -- 王医生（呼吸）补充
  (44, 3, 3, CURRENT_DATE + 8, '14:00-17:00', 18, 'PUBLISHED'),
  (45, 3, 3, CURRENT_DATE + 11, '09:00-12:00', 18, 'PUBLISHED'),
  -- 刘医生（消化内科）
  (50, 4, 4, CURRENT_DATE + 1, '09:00-12:00', 22, 'PUBLISHED'),
  (51, 4, 4, CURRENT_DATE + 2, '14:00-17:00', 18, 'PUBLISHED'),
  (52, 4, 4, CURRENT_DATE + 4, '09:00-12:00', 22, 'PUBLISHED'),
  (53, 4, 4, CURRENT_DATE + 8, '09:00-12:00', 22, 'PUBLISHED'),
  -- 陈医生（消化内科）
  (55, 5, 4, CURRENT_DATE + 1, '14:00-17:00', 20, 'PUBLISHED'),
  (56, 5, 4, CURRENT_DATE + 3, '09:00-12:00', 20, 'PUBLISHED'),
  (57, 5, 4, CURRENT_DATE + 5, '14:00-17:00', 18, 'PUBLISHED'),
  -- 孙医生（神经内科）
  (60, 6, 5, CURRENT_DATE + 1, '09:00-12:00', 15, 'PUBLISHED'),
  (61, 6, 5, CURRENT_DATE + 3, '14:00-17:00', 15, 'PUBLISHED'),
  (62, 6, 5, CURRENT_DATE + 6, '09:00-12:00', 15, 'PUBLISHED'),
  (63, 6, 5, CURRENT_DATE + 9, '09:00-12:00', 15, 'PUBLISHED'),
  -- 周医生（神经内科）
  (65, 7, 5, CURRENT_DATE + 2, '09:00-12:00', 18, 'PUBLISHED'),
  (66, 7, 5, CURRENT_DATE + 4, '14:00-17:00', 18, 'PUBLISHED'),
  (67, 7, 5, CURRENT_DATE + 7, '09:00-12:00', 18, 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;

-- 8. 新增号源（与排班对应）
INSERT INTO appointment_slot (id, schedule_id, doctor_id, department_id, start_time, end_time, capacity, remaining_capacity, status) VALUES
  -- 张医生补充号源
  (40, 40, 1, 1, CURRENT_DATE + INTERVAL '8 day 9 hours', CURRENT_DATE + INTERVAL '8 day 12 hours', 20, 16, 'AVAILABLE'),
  (41, 41, 1, 1, CURRENT_DATE + INTERVAL '10 day 14 hours', CURRENT_DATE + INTERVAL '10 day 17 hours', 15, 15, 'AVAILABLE'),
  -- 李医生补充号源
  (42, 42, 2, 2, CURRENT_DATE + INTERVAL '8 day 14 hours', CURRENT_DATE + INTERVAL '8 day 17 hours', 25, 22, 'AVAILABLE'),
  (43, 43, 2, 2, CURRENT_DATE + INTERVAL '10 day 9 hours', CURRENT_DATE + INTERVAL '10 day 12 hours', 25, 25, 'AVAILABLE'),
  -- 王医生补充号源
  (44, 44, 3, 3, CURRENT_DATE + INTERVAL '8 day 14 hours', CURRENT_DATE + INTERVAL '8 day 17 hours', 18, 14, 'AVAILABLE'),
  (45, 45, 3, 3, CURRENT_DATE + INTERVAL '11 day 9 hours', CURRENT_DATE + INTERVAL '11 day 12 hours', 18, 18, 'AVAILABLE'),
  -- 刘医生号源
  (50, 50, 4, 4, CURRENT_DATE + INTERVAL '1 day 9 hours', CURRENT_DATE + INTERVAL '1 day 12 hours', 22, 18, 'AVAILABLE'),
  (51, 51, 4, 4, CURRENT_DATE + INTERVAL '2 day 14 hours', CURRENT_DATE + INTERVAL '2 day 17 hours', 18, 18, 'AVAILABLE'),
  (52, 52, 4, 4, CURRENT_DATE + INTERVAL '4 day 9 hours', CURRENT_DATE + INTERVAL '4 day 12 hours', 22, 20, 'AVAILABLE'),
  (53, 53, 4, 4, CURRENT_DATE + INTERVAL '8 day 9 hours', CURRENT_DATE + INTERVAL '8 day 12 hours', 22, 22, 'AVAILABLE'),
  -- 陈医生号源
  (55, 55, 5, 4, CURRENT_DATE + INTERVAL '1 day 14 hours', CURRENT_DATE + INTERVAL '1 day 17 hours', 20, 17, 'AVAILABLE'),
  (56, 56, 5, 4, CURRENT_DATE + INTERVAL '3 day 9 hours', CURRENT_DATE + INTERVAL '3 day 12 hours', 20, 20, 'AVAILABLE'),
  (57, 57, 5, 4, CURRENT_DATE + INTERVAL '5 day 14 hours', CURRENT_DATE + INTERVAL '5 day 17 hours', 18, 18, 'AVAILABLE'),
  -- 孙医生号源
  (60, 60, 6, 5, CURRENT_DATE + INTERVAL '1 day 9 hours', CURRENT_DATE + INTERVAL '1 day 12 hours', 15, 10, 'AVAILABLE'),
  (61, 61, 6, 5, CURRENT_DATE + INTERVAL '3 day 14 hours', CURRENT_DATE + INTERVAL '3 day 17 hours', 15, 15, 'AVAILABLE'),
  (62, 62, 6, 5, CURRENT_DATE + INTERVAL '6 day 9 hours', CURRENT_DATE + INTERVAL '6 day 12 hours', 15, 12, 'AVAILABLE'),
  (63, 63, 6, 5, CURRENT_DATE + INTERVAL '9 day 9 hours', CURRENT_DATE + INTERVAL '9 day 12 hours', 15, 15, 'AVAILABLE'),
  -- 周医生号源
  (65, 65, 7, 5, CURRENT_DATE + INTERVAL '2 day 9 hours', CURRENT_DATE + INTERVAL '2 day 12 hours', 18, 15, 'AVAILABLE'),
  (66, 66, 7, 5, CURRENT_DATE + INTERVAL '4 day 14 hours', CURRENT_DATE + INTERVAL '4 day 17 hours', 18, 18, 'AVAILABLE'),
  (67, 67, 7, 5, CURRENT_DATE + INTERVAL '7 day 9 hours', CURRENT_DATE + INTERVAL '7 day 12 hours', 18, 18, 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Phase 3: 业务流水数据
-- ============================================================

-- 9. 新增分诊记录（10条）
INSERT INTO triage_record (id, patient_id, chief_complaint, recommended_department, recommended_doctor_ids, assigned_doctor_id, reason, ai_result_json, status) VALUES
  (28, 6, '腹痛、腹泻2天，伴恶心呕吐', '消化内科', '4,5', 4, '急性胃肠炎表现，建议消化内科评估', '{"department":"GASTRO","confidence":0.89,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (29, 7, '反复头痛3天，以额部为主，伴恶心', '神经内科', '6,7', 6, '头痛待查，需排除颅内病变', '{"department":"NEURO","confidence":0.87,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (30, 8, '头晕、视物模糊一周，血压偏高', '心内科', '1', 1, '高血压控制不佳，需调整用药', '{"department":"CARDIOLOGY","confidence":0.91,"urgency":"HIGH"}', 'ASSIGNED'),
  (31, 9, '口渴多尿加重，血糖控制不佳', '全科门诊', '2', 2, '糖尿病复诊，需调整降糖方案', '{"department":"GENERAL","confidence":0.93,"urgency":"MEDIUM"}', 'ASSIGNED'),
  (32, 10, '胸闷、心悸，活动后气短加重', '心内科', '1', 1, '冠心病术后复查，需评估心功能', '{"department":"CARDIOLOGY","confidence":0.95,"urgency":"HIGH"}', 'ASSIGNED'),
  (33, 11, '头晕、恶心，血压180/100mmHg', '心内科', '1', 1, '高血压急症，需紧急处理', '{"department":"CARDIOLOGY","confidence":0.96,"urgency":"CRITICAL"}', 'ASSIGNED'),
  (34, 12, '右侧肢体活动不利加重，言语不清', '神经内科', '6,7', 6, '脑血管病后遗症加重，需神经内科评估', '{"department":"NEURO","confidence":0.94,"urgency":"HIGH"}', 'ASSIGNED'),
  (35, 13, '反复搏动性头痛，伴畏光、恶心', '神经内科', '6,7', 6, '偏头痛典型表现，建议神经内科诊治', '{"department":"NEURO","confidence":0.92,"urgency":"LOW"}', 'ASSIGNED'),
  (36, 1, '反酸、烧心一周，餐后加重', '消化内科', '4,5', 4, '胃食管反流可能，建议消化内科评估', '{"department":"GASTRO","confidence":0.88,"urgency":"LOW"}', 'ASSIGNED'),
  (37, 4, '双下肢水肿加重，活动后气促', '心内科', '1', 1, '心功能不全表现，需心内科紧急评估', '{"department":"CARDIOLOGY","confidence":0.93,"urgency":"HIGH"}', 'ASSIGNED')
ON CONFLICT (id) DO NOTHING;

-- 10. 新增挂号记录（12条）
INSERT INTO registration (id, patient_id, doctor_id, department_id, triage_record_id, slot_id, appointment_time, status) VALUES
  -- 已完成
  (8, 6, 4, 4, 28, 50, CURRENT_TIMESTAMP - INTERVAL '3 day', 'COMPLETED'),
  (9, 7, 6, 5, 29, 60, CURRENT_TIMESTAMP - INTERVAL '2 day', 'COMPLETED'),
  (10, 8, 1, 1, 30, 40, CURRENT_TIMESTAMP - INTERVAL '1 day', 'COMPLETED'),
  (11, 9, 2, 2, 31, 42, CURRENT_TIMESTAMP - INTERVAL '1 day', 'COMPLETED'),
  (12, 10, 1, 1, 32, 41, CURRENT_TIMESTAMP - INTERVAL '12 hour', 'COMPLETED'),
  -- 已确认（今天/明天）
  (13, 11, 1, 1, 33, 41, CURRENT_TIMESTAMP + INTERVAL '2 hour', 'CONFIRMED'),
  (14, 12, 6, 5, 34, 61, CURRENT_DATE + INTERVAL '1 day 9 hours', 'CONFIRMED'),
  (15, 13, 6, 5, 35, 62, CURRENT_DATE + INTERVAL '2 day 9 hours', 'CONFIRMED'),
  (16, 1, 4, 4, 36, 52, CURRENT_DATE + INTERVAL '1 day 14 hours', 'CONFIRMED'),
  (17, 4, 1, 1, 37, 40, CURRENT_DATE + INTERVAL '3 day 9 hours', 'CONFIRMED'),
  -- 已取消
  (18, 5, 3, 3, 5, 31, CURRENT_TIMESTAMP - INTERVAL '2 day', 'CANCELLED'),
  (19, 2, 2, 2, 2, 21, CURRENT_TIMESTAMP - INTERVAL '3 day', 'CANCELLED')
ON CONFLICT (id) DO NOTHING;

-- 11. 新增病历（8条，关联已完成的挂号）
INSERT INTO medical_record (id, patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, diagnosis, treatment_advice, ai_generated) VALUES
  (3, 6, 4, 8,
   '腹痛、腹泻2天，伴恶心呕吐',
   '患者2天前进食不洁食物后出现上腹部疼痛，呈阵发性绞痛，伴腹泻，每日4-5次，为黄色稀水样便，无脓血。伴恶心，呕吐2次，为胃内容物。无发热，无里急后重。',
   '既往体健，否认肝炎、结核等传染病史。否认食物药物过敏史。',
   'T 37.2°C, 腹软，脐周及上腹部压痛(+)，无反跳痛，肠鸣音活跃，肝脾肋下未触及。',
   '急性胃肠炎',
   '1. 蒙脱石散 3g tid po（空腹）\n2. 补液盐 III 适量口服\n3. 流质饮食，避免油腻辛辣\n4. 注意观察大便次数及性状，如出现发热或血便及时复诊',
   TRUE),
  (4, 7, 6, 9,
   '反复头痛3天，以额部为主',
   '患者3天前无明显诱因出现头痛，以额部为主，呈搏动性，程度中等，每次持续2-4小时，伴恶心，无呕吐。畏光，喜安静环境。既往有类似发作史。',
   '既往有偏头痛病史5年，未规律服药。否认高血压、糖尿病史。',
   'BP 118/75mmHg, 神清语利，双侧瞳孔等大等圆，对光反射灵敏，颈软，四肢肌力肌张力正常，病理征未引出。',
   '偏头痛',
   '1. 布洛芬缓释胶囊 0.3g prn po（头痛时服用，每日不超过2次）\n2. 避免强光、噪音等诱因\n3. 规律作息，保证睡眠\n4. 记录头痛日记，如发作频繁可考虑预防用药\n5. 如出现突发剧烈头痛、肢体无力等立即急诊',
   TRUE),
  (5, 8, 1, 10,
   '头晕、视物模糊一周',
   '患者1周前出现头晕，呈持续性，伴视物模糊，测血压165/100mmHg，自行服用氨氯地平5mg qd，血压控制不佳。伴头胀，无恶心呕吐，无肢体麻木。',
   '高血压病史3年，规律服用氨氯地平5mg qd。否认糖尿病、冠心病史。',
   'BP 158/96mmHg, HR 76bpm, 心律齐，各瓣膜区未闻及杂音。双肺呼吸音清。四肢肌力正常。',
   '高血压2级（高危）',
   '1. 氨氯地平 5mg qd po（继续）\n2. 加用缬沙坦 80mg qd po\n3. 低盐低脂饮食，每日食盐<6g\n4. 每日监测血压并记录\n5. 2周后复诊评估血压控制情况',
   TRUE),
  (6, 9, 2, 11,
   '口渴多尿加重，血糖控制不佳',
   '患者近2周口渴、多尿症状加重，伴乏力，体重下降约2kg。自测空腹血糖波动在10-14mmol/L，餐后血糖16-20mmol/L。无恶心呕吐，无视物模糊。',
   '2型糖尿病5年，口服二甲双胍0.5g tid。高血脂2年，口服阿托伐他汀20mg qn。否认高血压。',
   'BP 132/82mmHg, BMI 25.6, 空腹血糖12.8mmol/L, HbA1c 8.5%。心肺腹未见明显异常。',
   '2型糖尿病 血糖控制不佳；高脂血症',
   '1. 二甲双胍 0.5g tid po（继续，餐中服用）\n2. 加用格列美脲 1mg qd po（早餐前）\n3. 阿托伐他汀 20mg qn po（继续）\n4. 糖尿病饮食，控制总热量\n5. 适量运动，每周≥150分钟\n6. 1周后复查空腹血糖，3个月后复查HbA1c',
   TRUE),
  (7, 10, 1, 12,
   '胸闷、心悸，活动后气短加重',
   '患者冠脉支架术后2年，近1周胸闷、心悸加重，活动后气短，爬2层楼即感呼吸困难。无胸痛，无夜间阵发性呼吸困难。双下肢轻度水肿。',
   '冠心病2年，支架植入术后。长期服用阿司匹林100mg qd、氯吡格雷75mg qd、阿托伐他汀20mg qn。高血压1年，服用缬沙坦80mg qd。',
   'BP 135/85mmHg, HR 82bpm, 心律齐，心尖区可闻及2/6级收缩期杂音。双肺底可闻及少量湿啰音。双下肢凹陷性水肿(+)。',
   '冠心病 心功能II级（NYHA）；高血压2级',
   '1. 继续双联抗血小板：阿司匹林100mg qd + 氯吡格雷75mg qd\n2. 阿托伐他汀 20mg qn\n3. 缬沙坦 80mg qd\n4. 加用呋塞米 20mg qd po（利尿消肿）\n5. 低盐饮食，限制液体入量<1500ml/日\n6. 适当限制活动，避免重体力劳动\n7. 1周后复诊评估心功能',
   TRUE),
  (8, 11, 1, 13,
   '头晕、恶心，血压180/100mmHg',
   '患者今日晨起头晕加重，伴恶心，测血压180/100mmHg，自行加服氨氯地平5mg后血压降至165/95mmHg。伴头胀、视物模糊，无胸痛，无肢体活动障碍。',
   '高血压15年，糖尿病8年。服用氨氯地平5mg qd、二甲双胍0.5g tid。未规律监测血压。',
   'BP 168/98mmHg, HR 78bpm, 心律齐。双肺呼吸音清。腹部膨隆，肝脾肋下未触及。双下肢无水肿。随机血糖11.2mmol/L。',
   '高血压3级（极高危）；2型糖尿病',
   '1. 氨氯地平 5mg → 10mg qd po\n2. 加用氯沙坦 50mg qd po\n3. 二甲双胍 0.5g tid po（继续）\n4. 立即卧床休息，避免情绪激动\n5. 每日监测血压3次（晨起、午后、睡前）\n6. 低盐低脂糖尿病饮食\n7. 3天后复诊，如血压仍>160/100mmHg需住院调整',
   TRUE),
  (9, 12, 6, 14,
   '右侧肢体活动不利加重，言语不清',
   '患者脑梗塞后遗症2年，右侧肢体活动不利。近1周右侧上肢肌力明显下降，原来可抬举过肩，现只能平举。伴言语含糊，构音障碍加重。无头痛，无意识障碍。',
   '脑梗塞后遗症2年，帕金森病3年。服用美多芭0.25g tid、阿司匹林100mg qd、阿托伐他汀20mg qn。',
   'BP 142/88mmHg, 神清，构音障碍，右侧鼻唇沟浅。右上肢肌力3级，右下肢肌力4级。左侧肢体肌力5级。右侧肢体肌张力增高（铅管样）。双侧Babinski征(-)。',
   '脑梗塞后遗症 加重；帕金森病',
   '1. 继续美多芭 0.25g tid po（餐前1小时服用）\n2. 阿司匹林 100mg qd + 阿托伐他汀 20mg qn\n3. 加用甲钴胺 0.5mg tid po（营养神经）\n4. 康复科会诊，制定康复训练方案\n5. 头颅MRI复查评估脑部情况\n6. 2周后复诊评估肌力恢复情况',
   TRUE),
  (10, 13, 6, 15,
   '反复搏动性头痛，伴畏光、恶心',
   '患者近5年反复出现搏动性头痛，以右侧颞部为主，每次持续4-72小时，伴恶心、畏光、畏声。每月发作2-3次。本次发作持续2天，口服布洛芬可部分缓解。',
   '偏头痛病史5年，未规律服药。否认高血压、糖尿病、颅脑外伤史。',
   'BP 115/70mmHg, 神清语利，双侧瞳孔等大等圆，颈软，神经系统查体未见明显阳性体征。',
   '偏头痛 无先兆',
   '1. 布洛芬缓释胶囊 0.3g prn po（发作时服用）\n2. 预防用药：氟桂利嗪 5mg qn po（每晚睡前）\n3. 避免诱因：规律作息、避免过度疲劳、减少咖啡因摄入\n4. 记录头痛日记（发作时间、持续时间、诱因、用药情况）\n5. 如每月发作>4次或常规止痛药无效，复诊调整方案',
   TRUE),
  (11, 1, 4, 16,
   '反酸、烧心一周，餐后加重',
   '患者1周前出现反酸、烧心，以餐后1小时明显，平卧时加重。伴上腹胀满，嗳气。无腹痛，无呕血黑便。近3天加重。',
   '既往体健，否认消化系统疾病史。无药物过敏史。',
   'BP 120/78mmHg, 腹软，剑突下轻压痛，无反跳痛。肝脾肋下未触及。肠鸣音正常。',
   '胃食管反流病',
   '1. 奥美拉唑 20mg qd po（早餐前30分钟）\n2. 莫沙必利 5mg tid po（餐前）\n3. 饮食宜清淡，避免辛辣、咖啡、浓茶\n4. 餐后不要立即平卧，睡前3小时不进食\n5. 睡觉时抬高床头15-20cm\n6. 4周后复诊评估症状改善情况',
   TRUE),
  (12, 4, 1, 17,
   '双下肢水肿加重，活动后气促',
   '患者糖尿病10年、冠心病3年，近2周双下肢水肿逐渐加重，由脚踝蔓延至小腿。活动后气促明显，平卧时好转。夜间偶有憋醒。伴食欲下降、腹胀。',
   '糖尿病10年（胰岛素治疗中）、冠心病3年（未行介入治疗）、高血压5年。服用二甲双胍0.5g tid、氨氯地平5mg qd、阿司匹林100mg qd。',
   'BP 148/95mmHg, HR 88bpm, 心律齐，心尖区可闻及3/6级收缩期杂音。双肺底可闻及湿啰音。腹部膨隆，移动性浊音(+)。双下肢凹陷性水肿(++)。',
   '冠心病 心功能III级（NYHA）；2型糖尿病；高血压2级；腹水待查',
   '1. 住院进一步诊治\n2. 呋塞米 20mg iv st（利尿）\n3. 螺内酯 20mg qd po\n4. 氨氯地平 5mg → 10mg qd po\n5. 二甲双胍 0.5g tid po（继续）\n6. 低盐低脂糖尿病饮食，限水<1500ml/日\n7. 完善心脏彩超、BNP、肝肾功能、腹部B超检查',
   TRUE)
ON CONFLICT (id) DO NOTHING;

-- 12. 新增处方（8张）
INSERT INTO prescription (id, patient_id, doctor_id, medical_record_id, registration_id, risk_level, status) VALUES
  (9, 6, 4, 3, 8, 'LOW', 'ACTIVE'),
  (10, 7, 6, 4, 9, 'LOW', 'ACTIVE'),
  (11, 8, 1, 5, 10, 'MEDIUM', 'ACTIVE'),
  (12, 9, 2, 6, 11, 'MEDIUM', 'ACTIVE'),
  (13, 10, 1, 7, 12, 'MEDIUM', 'ACTIVE'),
  (14, 11, 1, 8, 13, 'HIGH', 'ACTIVE'),
  (15, 12, 6, 9, 14, 'LOW', 'ACTIVE'),
  (16, 13, 6, 10, 15, 'LOW', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO prescription_item (id, prescription_id, drug_name, dosage, frequency, usage_method, days, remark) VALUES
  -- 处方9：急性胃肠炎
  (9, 9, '蒙脱石散', '3g', '每日三次', '口服', 5, '空腹服用，与餐间隔2小时'),
  (10, 9, '补液盐III', '1袋', '需要时', '口服', 5, '腹泻时服用，防止脱水'),
  -- 处方10：偏头痛
  (11, 10, '布洛芬缓释胶囊', '0.3g', '需要时', '口服', 30, '头痛时服用，每日不超过2次'),
  (12, 10, '氟桂利嗪', '5mg', '每晚一次', '口服', 30, '睡前服用，预防发作'),
  -- 处方11：高血压
  (13, 11, '氨氯地平', '5mg', '每日一次', '口服', 30, '继续服用'),
  (14, 11, '缬沙坦', '80mg', '每日一次', '口服', 30, '新增降压药，与氨氯地平联用'),
  -- 处方12：糖尿病
  (15, 12, '二甲双胍', '0.5g', '每日三次', '口服', 30, '餐中服用，减少胃肠道反应'),
  (16, 12, '格列美脲', '1mg', '每日一次', '口服', 30, '早餐前服用'),
  (17, 12, '阿托伐他汀', '20mg', '每晚一次', '口服', 30, '继续服用'),
  -- 处方13：冠心病
  (18, 13, '阿司匹林', '100mg', '每日一次', '口服', 30, '继续双联抗血小板'),
  (19, 13, '氯吡格雷', '75mg', '每日一次', '口服', 30, '继续双联抗血小板'),
  (20, 13, '阿托伐他汀', '20mg', '每晚一次', '口服', 30, '继续服用'),
  (21, 13, '缬沙坦', '80mg', '每日一次', '口服', 30, '继续服用'),
  (22, 13, '呋塞米', '20mg', '每日一次', '口服', 7, '利尿消肿，注意监测电解质'),
  -- 处方14：高血压急症
  (23, 14, '氨氯地平', '10mg', '每日一次', '口服', 30, '加量至10mg'),
  (24, 14, '氯沙坦', '50mg', '每日一次', '口服', 30, '新增ARB类降压药'),
  (25, 14, '二甲双胍', '0.5g', '每日三次', '口服', 30, '继续服用'),
  -- 处方15：帕金森+脑梗
  (26, 15, '美多芭', '0.25g', '每日三次', '口服', 30, '餐前1小时服用'),
  (27, 15, '阿司匹林', '100mg', '每日一次', '口服', 30, '继续服用'),
  (28, 15, '阿托伐他汀', '20mg', '每晚一次', '口服', 30, '继续服用'),
  (29, 15, '甲钴胺', '0.5mg', '每日三次', '口服', 30, '营养神经'),
  -- 处方16：偏头痛预防
  (30, 16, '布洛芬缓释胶囊', '0.3g', '需要时', '口服', 30, '发作时服用'),
  (31, 16, '氟桂利嗪', '5mg', '每晚一次', '口服', 30, '预防用药')
ON CONFLICT (id) DO NOTHING;

-- 13. 新增处方审核记录
INSERT INTO prescription_check_record (id, prescription_id, patient_id, doctor_id, risk_level, suggestions, interactions, ai_result_json, created_at) VALUES
  (3, 9, 6, 4, 'LOW', '处方审核通过，蒙脱石散与补液盐可同时使用', '[]', '{"riskLevel":"LOW","suggestions":[],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '3 day'),
  (4, 10, 7, 6, 'LOW', '处方审核通过，注意布洛芬不宜长期大量使用', '[]', '{"riskLevel":"LOW","suggestions":["布洛芬使用超过3天需复诊"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (5, 11, 8, 1, 'MEDIUM', '氨氯地平与缬沙坦联用需监测血压，防止低血压', '[{"drug1":"氨氯地平","drug2":"缬沙坦","risk":"低血压风险增加","severity":"MODERATE"}]', '{"riskLevel":"MEDIUM","suggestions":["联用降压药需密切监测血压"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (6, 12, 9, 2, 'MEDIUM', '格列美脲与二甲双胍联用需注意低血糖风险', '[{"drug1":"格列美脲","drug2":"二甲双胍","risk":"低血糖风险增加","severity":"MODERATE"}]', '{"riskLevel":"MEDIUM","suggestions":["磺脲类+双胍类联用需监测血糖","建议患者随身携带糖块"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (7, 13, 10, 1, 'MEDIUM', '双联抗血小板+利尿剂，注意出血风险和电解质', '[{"drug1":"阿司匹林","drug2":"氯吡格雷","risk":"出血风险增加","severity":"MODERATE"}]', '{"riskLevel":"MEDIUM","suggestions":["双抗治疗期间注意出血征象","呋塞米需监测血钾"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '12 hour'),
  (8, 14, 11, 1, 'HIGH', '氨氯地平加量+新增ARB，老年患者低血压风险高', '[{"drug1":"氨氯地平","drug2":"氯沙坦","risk":"严重低血压风险","severity":"HIGH"}]', '{"riskLevel":"HIGH","suggestions":["老年患者联用降压药需逐步加量","建议每日监测血压3次","如出现头晕、乏力及时就诊"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '2 hour'),
  (9, 15, 12, 6, 'LOW', '处方审核通过，美多芭需空腹服用', '[]', '{"riskLevel":"LOW","suggestions":["美多芭需餐前1小时服用以保证吸收"],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (10, 16, 13, 6, 'LOW', '处方审核通过，氟桂利嗪预防偏头痛', '[]', '{"riskLevel":"LOW","suggestions":[],"degraded":false}', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Phase 4: 通知 + AI日志
-- ============================================================

-- 14. 新增通知消息（12条）
INSERT INTO notification_message (id, doctor_id, patient_id, prescription_id, type, title, content, risk_level, read_status) VALUES
  (7, 4, 6, 9, 'PRESCRIPTION_LOW_RISK', '处方审核通过', '刘医生为患者周小明开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ'),
  (8, 6, 7, 10, 'PRESCRIPTION_LOW_RISK', '处方审核通过', '孙医生为患者吴芳开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ'),
  (9, 1, 8, 11, 'PRESCRIPTION_MEDIUM_RISK', '处方审核提醒', '张医生为患者郑伟开具的处方存在中度风险：氨氯地平与缬沙坦联用需监测血压。', 'MEDIUM', 'UNREAD'),
  (10, 2, 9, 12, 'PRESCRIPTION_MEDIUM_RISK', '处方审核提醒', '李医生为患者黄丽华开具的处方存在中度风险：格列美脲与二甲双胍联用需注意低血糖。', 'MEDIUM', 'UNREAD'),
  (11, 1, 10, 13, 'PRESCRIPTION_MEDIUM_RISK', '处方审核提醒', '张医生为患者林志强开具的处方存在中度风险：双联抗血小板+利尿剂需注意出血和电解质。', 'MEDIUM', 'UNREAD'),
  (12, 1, 11, 14, 'PRESCRIPTION_HIGH_RISK', '处方高风险警告', '张医生为患者何秀英开具的处方存在高风险：老年患者联用降压药需逐步加量，密切监测。', 'HIGH', 'UNREAD'),
  (13, 1, 11, NULL, 'TRIAGE_ASSIGNED', '分诊分配', '患者何秀英的分诊已分配给您，主诉：头晕、恶心，血压180/100mmHg。紧急程度：高。', NULL, 'READ'),
  (14, 4, 6, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者周小明已预约消化内科门诊，请及时查看。', NULL, 'READ'),
  (15, 6, 7, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者吴芳已预约神经内科门诊，请及时查看。', NULL, 'READ'),
  (16, 6, 12, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者马俊已预约神经内科门诊（复诊），请提前查看病历。', NULL, 'UNREAD'),
  (17, 4, 1, NULL, 'REGISTRATION_NEW', '新挂号通知', '患者测试患者已预约消化内科门诊。', NULL, 'UNREAD'),
  (18, 1, 4, NULL, 'TRIAGE_ASSIGNED', '分诊分配', '患者赵强的分诊已分配给您，主诉：双下肢水肿加重，活动后气促。紧急程度：高。', NULL, 'UNREAD')
ON CONFLICT (id) DO NOTHING;

-- 15. 新增AI生成日志（15条）
INSERT INTO ai_generation_log (id, task_type, provider, model, request_id, biz_id, patient_id, doctor_id, input_summary, output_summary, success, latency_ms, status, duration_ms, created_at) VALUES
  (20, 'TRIAGE', 'dify', 'mock', 'req-020', 28, 6, NULL, '腹痛、腹泻2天，伴恶心呕吐', '推荐消化内科，置信度0.89', TRUE, 780, 'SUCCESS', 780, CURRENT_TIMESTAMP - INTERVAL '4 day'),
  (21, 'TRIAGE', 'dify', 'mock', 'req-021', 29, 7, NULL, '反复头痛3天，以额部为主', '推荐神经内科，置信度0.87', TRUE, 820, 'SUCCESS', 820, CURRENT_TIMESTAMP - INTERVAL '3 day'),
  (22, 'TRIAGE', 'dify', 'mock', 'req-022', 30, 8, NULL, '头晕、视物模糊一周，血压偏高', '推荐心内科，置信度0.91', TRUE, 650, 'SUCCESS', 650, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (23, 'TRIAGE', 'dify', 'mock', 'req-023', 31, 9, NULL, '口渴多尿加重，血糖控制不佳', '推荐全科门诊，置信度0.93', TRUE, 710, 'SUCCESS', 710, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (24, 'TRIAGE', 'dify', 'mock', 'req-024', 32, 10, NULL, '胸闷、心悸，活动后气短加重', '推荐心内科，置信度0.95', TRUE, 690, 'SUCCESS', 690, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (25, 'TRIAGE', 'dify', 'mock', 'req-025', 33, 11, NULL, '头晕、恶心，血压180/100mmHg', '推荐心内科紧急，置信度0.96', TRUE, 620, 'SUCCESS', 620, CURRENT_TIMESTAMP - INTERVAL '12 hour'),
  (26, 'TRIAGE', 'dify', 'mock', 'req-026', 34, 12, NULL, '右侧肢体活动不利加重', '推荐神经内科，置信度0.94', TRUE, 750, 'SUCCESS', 750, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (27, 'TRIAGE', 'dify', 'mock', 'req-027', 35, 13, NULL, '反复搏动性头痛，伴畏光', '推荐神经内科，置信度0.92', TRUE, 680, 'SUCCESS', 680, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (28, 'MEDICAL_RECORD', 'dify', 'mock', 'req-028', 3, 6, 4, '腹痛腹泻2天伴恶心呕吐', '生成结构化病历', TRUE, 1350, 'SUCCESS', 1350, CURRENT_TIMESTAMP - INTERVAL '3 day'),
  (29, 'MEDICAL_RECORD', 'dify', 'mock', 'req-029', 4, 7, 6, '反复头痛3天额部为主', '生成结构化病历', TRUE, 1280, 'SUCCESS', 1280, CURRENT_TIMESTAMP - INTERVAL '2 day'),
  (30, 'MEDICAL_RECORD', 'dify', 'mock', 'req-030', 5, 8, 1, '头晕视物模糊血压偏高', '生成结构化病历', TRUE, 1150, 'SUCCESS', 1150, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (31, 'PRESCRIPTION_CHECK', 'dify', 'mock', 'req-031', 11, 8, 1, '氨氯地平+缬沙坦', '风险等级MEDIUM', TRUE, 620, 'SUCCESS', 620, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (32, 'PRESCRIPTION_CHECK', 'dify', 'mock', 'req-032', 12, 9, 2, '格列美脲+二甲双胍', '风险等级MEDIUM', TRUE, 580, 'SUCCESS', 580, CURRENT_TIMESTAMP - INTERVAL '1 day'),
  (33, 'PRESCRIPTION_CHECK', 'dify', 'mock', 'req-033', 14, 11, 1, '氨氯地平+氯沙坦', '风险等级HIGH', TRUE, 650, 'SUCCESS', 650, CURRENT_TIMESTAMP - INTERVAL '2 hour'),
  (34, 'PRESCRIPTION_CHECK', 'dify', 'mock', 'req-034', 13, 10, 1, '阿司匹林+氯吡格雷+呋塞米', '风险等级MEDIUM', TRUE, 590, 'SUCCESS', 590, CURRENT_TIMESTAMP - INTERVAL '12 hour')
ON CONFLICT (id) DO NOTHING;

-- 16. AI排班建议
INSERT INTO ai_schedule_suggestion (id, doctor_id, department_id, work_date, time_range, capacity, reason, status, source, degraded) VALUES
  (10, 4, 4, CURRENT_DATE + 12, '09:00-12:00', 22, '消化内科周末就诊量较高，建议增开上午号源', 'PENDING', 'dify', FALSE),
  (11, 6, 5, CURRENT_DATE + 12, '09:00-12:00', 15, '神经内科季节性头痛患者增多', 'PENDING', 'dify', FALSE),
  (12, 1, 1, CURRENT_DATE + 14, '14:00-17:00', 15, '心内科下午复诊患者较多，建议增加号源', 'PENDING', 'mock', TRUE),
  (13, 2, 2, CURRENT_DATE + 14, '09:00-12:00', 25, '全科门诊周一上午就诊高峰', 'PENDING', 'dify', FALSE),
  (14, 3, 3, CURRENT_DATE + 15, '09:00-12:00', 18, '呼吸内科流感季节就诊量上升', 'PENDING', 'dify', FALSE)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Phase 5: 管理端数据
-- ============================================================

-- 17. 医疗设备
INSERT INTO medical_device (device_code, name, category, department_id, location, status, purchase_date, last_maintenance_at, remark) VALUES
  ('DEV-ECG-001', '12导联心电图机', '诊断设备', 1, '心内科诊室1', 'IN_USE', '2023-06-15', CURRENT_TIMESTAMP - INTERVAL '1 month', '运行正常'),
  ('DEV-ECG-002', '12导联心电图机', '诊断设备', 1, '心内科诊室2', 'AVAILABLE', '2023-06-15', CURRENT_TIMESTAMP - INTERVAL '2 month', '备用设备'),
  ('DEV-BP-001', '动态血压监测仪', '监测设备', 1, '心内科门诊', 'AVAILABLE', '2024-01-10', CURRENT_TIMESTAMP - INTERVAL '15 day', '新购设备'),
  ('DEV-US-001', '腹部超声诊断仪', '诊断设备', 4, '消化内科B超室', 'IN_USE', '2022-08-20', CURRENT_TIMESTAMP - INTERVAL '2 month', '运行良好'),
  ('DEV-EEG-001', '脑电图机', '诊断设备', 5, '神经内科检查室', 'MAINTENANCE', '2021-12-01', CURRENT_TIMESTAMP - INTERVAL '3 day', '主板故障，维修中'),
  ('DEV-SPI-001', '肺功能检测仪', '诊断设备', 3, '呼吸内科功能室', 'AVAILABLE', '2023-03-15', CURRENT_TIMESTAMP - INTERVAL '1 month', '定期校准');

-- 18. 设备使用记录
INSERT INTO device_usage_record (device_id, usage_type, used_by, patient_id, started_at, ended_at, result_status, remark) VALUES
  (1, '心电图检查', '张医生', 8, CURRENT_TIMESTAMP - INTERVAL '1 day 3 hour', CURRENT_TIMESTAMP - INTERVAL '1 day 2.5 hour', 'NORMAL', '窦性心律，左室高电压'),
  (1, '心电图检查', '张医生', 10, CURRENT_TIMESTAMP - INTERVAL '12 hour 2 hour', CURRENT_TIMESTAMP - INTERVAL '12 hour 1.5 hour', 'ABNORMAL', 'ST段改变，建议进一步检查'),
  (2, '心电图检查', '张医生', 11, CURRENT_TIMESTAMP - INTERVAL '2 hour', CURRENT_TIMESTAMP - INTERVAL '1.5 hour', 'ABNORMAL', '左室肥厚伴劳损'),
  (4, '腹部B超', '刘医生', 6, CURRENT_TIMESTAMP - INTERVAL '3 day 1 hour', CURRENT_TIMESTAMP - INTERVAL '3 day 0.5 hour', 'NORMAL', '肝胆胰脾未见明显异常'),
  (4, '腹部B超', '刘医生', 1, CURRENT_TIMESTAMP - INTERVAL '1 day 2 hour', CURRENT_TIMESTAMP - INTERVAL '1 day 1.5 hour', 'NORMAL', '胃部未见占位性病变'),
  (5, '脑电图检查', '孙医生', 12, CURRENT_TIMESTAMP - INTERVAL '5 day 2 hour', CURRENT_TIMESTAMP - INTERVAL '5 day 1 hour', 'ABNORMAL', '左侧颞叶慢波增多'),
  (5, '脑电图检查', '孙医生', 13, CURRENT_TIMESTAMP - INTERVAL '2 day 3 hour', CURRENT_TIMESTAMP - INTERVAL '2 day 2 hour', 'NORMAL', '正常脑电图'),
  (6, '肺功能检测', '王医生', 5, CURRENT_TIMESTAMP - INTERVAL '4 day 2 hour', CURRENT_TIMESTAMP - INTERVAL '4 day 1.5 hour', 'ABNORMAL', '轻度通气功能障碍');

-- 19. 角色权限
INSERT INTO role_permission (role, permission_key, enabled) VALUES
  ('ADMIN', 'dashboard:view', TRUE),
  ('ADMIN', 'department:manage', TRUE),
  ('ADMIN', 'doctor:manage', TRUE),
  ('ADMIN', 'drug:manage', TRUE),
  ('ADMIN', 'schedule:manage', TRUE),
  ('ADMIN', 'triage:manage', TRUE),
  ('ADMIN', 'device:manage', TRUE),
  ('ADMIN', 'patient:manage', TRUE),
  ('ADMIN', 'statistics:view', TRUE),
  ('ADMIN', 'statistics:export', TRUE),
  ('ADMIN', 'account:manage', TRUE),
  ('ADMIN', 'permission:manage', TRUE),
  ('ADMIN', 'knowledge:manage', TRUE),
  ('ADMIN', 'prompt:manage', TRUE),
  ('ADMIN', 'dict:manage', TRUE),
  ('ADMIN', 'patient-site:manage', TRUE),
  ('DOCTOR', 'schedule:view', TRUE),
  ('DOCTOR', 'registration:view', TRUE),
  ('DOCTOR', 'registration:confirm', TRUE),
  ('DOCTOR', 'triage:view', TRUE),
  ('DOCTOR', 'triage:assign', TRUE),
  ('DOCTOR', 'medical_record:create', TRUE),
  ('DOCTOR', 'medical_record:view', TRUE),
  ('DOCTOR', 'prescription:create', TRUE),
  ('DOCTOR', 'prescription:view', TRUE),
  ('DOCTOR', 'notification:view', TRUE),
  ('PATIENT', 'profile:view', TRUE),
  ('PATIENT', 'profile:edit', TRUE),
  ('PATIENT', 'registration:create', TRUE),
  ('PATIENT', 'registration:cancel', TRUE),
  ('PATIENT', 'medical_record:view', TRUE),
  ('PATIENT', 'prescription:view', TRUE)
ON CONFLICT (role, permission_key) DO NOTHING;

-- ============================================================
-- 更新序列值
-- ============================================================
SELECT setval(pg_get_serial_sequence('department', 'id'), COALESCE((SELECT MAX(id) FROM department), 1));
SELECT setval(pg_get_serial_sequence('doctor', 'id'), COALESCE((SELECT MAX(id) FROM doctor), 1));
SELECT setval(pg_get_serial_sequence('patient', 'id'), COALESCE((SELECT MAX(id) FROM patient), 1));
SELECT setval(pg_get_serial_sequence('drug', 'id'), COALESCE((SELECT MAX(id) FROM drug), 1));
SELECT setval(pg_get_serial_sequence('doctor_schedule', 'id'), COALESCE((SELECT MAX(id) FROM doctor_schedule), 1));
SELECT setval(pg_get_serial_sequence('appointment_slot', 'id'), COALESCE((SELECT MAX(id) FROM appointment_slot), 1));
SELECT setval(pg_get_serial_sequence('triage_record', 'id'), COALESCE((SELECT MAX(id) FROM triage_record), 1));
SELECT setval(pg_get_serial_sequence('registration', 'id'), COALESCE((SELECT MAX(id) FROM registration), 1));
SELECT setval(pg_get_serial_sequence('medical_record', 'id'), COALESCE((SELECT MAX(id) FROM medical_record), 1));
SELECT setval(pg_get_serial_sequence('prescription', 'id'), COALESCE((SELECT MAX(id) FROM prescription), 1));
SELECT setval(pg_get_serial_sequence('prescription_item', 'id'), COALESCE((SELECT MAX(id) FROM prescription_item), 1));
SELECT setval(pg_get_serial_sequence('prescription_check_record', 'id'), COALESCE((SELECT MAX(id) FROM prescription_check_record), 1));
SELECT setval(pg_get_serial_sequence('notification_message', 'id'), COALESCE((SELECT MAX(id) FROM notification_message), 1));
SELECT setval(pg_get_serial_sequence('ai_generation_log', 'id'), COALESCE((SELECT MAX(id) FROM ai_generation_log), 1));
SELECT setval(pg_get_serial_sequence('ai_schedule_suggestion', 'id'), COALESCE((SELECT MAX(id) FROM ai_schedule_suggestion), 1));
SELECT setval(pg_get_serial_sequence('knowledge_entry', 'id'), COALESCE((SELECT MAX(id) FROM knowledge_entry), 1));
SELECT setval(pg_get_serial_sequence('system_dict', 'id'), COALESCE((SELECT MAX(id) FROM system_dict), 1));
SELECT setval(pg_get_serial_sequence('medical_device', 'id'), COALESCE((SELECT MAX(id) FROM medical_device), 1));
SELECT setval(pg_get_serial_sequence('device_usage_record', 'id'), COALESCE((SELECT MAX(id) FROM device_usage_record), 1));
SELECT setval(pg_get_serial_sequence('role_permission', 'id'), COALESCE((SELECT MAX(id) FROM role_permission), 1));
