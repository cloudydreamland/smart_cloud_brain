-- ============================================================
-- 智慧云脑诊疗平台 — 种子数据扩充 V2
-- 目标：核心业务数据翻三倍
-- 执行：docker exec -i scb-kingbase ksql -U scb -d smart_cloud_brain < sql/V7__seed_expansion_v2.sql
-- ============================================================

-- ─── 1. 患者（13 → 40，新增 27 人）───
INSERT INTO patient (id, name, phone, password_hash, gender, age, allergy_history, past_history, address, blood_type, height_cm, weight_kg) VALUES
(14, '刘洋', '13800000014', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 55, '无', '高血压病史3年', '北京市朝阳区建国路88号', 'A', 172, 75.50),
(15, '张伟', '13800000015', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 42, '青霉素过敏', '无', '上海市浦东新区陆家嘴环路100号', 'O', 178, 82.00),
(16, '王秀英', '13800000016', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 68, '磺胺类过敏', '糖尿病8年，冠心病2年', '广州市天河区天河路385号', 'B', 158, 62.00),
(17, '陈明', '13800000017', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 35, '无', '无', '深圳市南山区科技园路1号', 'AB', 175, 70.00),
(18, '李娜', '13800000018', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 29, '花粉过敏', '哮喘病史', '杭州市西湖区文三路478号', 'A', 163, 52.00),
(19, '赵军', '13800000019', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 71, '无', '高血压10年，脑梗后遗症', '成都市武侯区人民南路四段1号', 'O', 168, 72.00),
(20, '孙丽华', '13800000020', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 48, '碘过敏', '甲状腺功能减退', '南京市鼓楼区中山路321号', 'A', 160, 58.00),
(21, '周强', '13800000021', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 38, '无', '无', '武汉市江汉区解放大道688号', 'B', 180, 85.00),
(22, '吴芳', '13800000022', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 56, '无', '高血压5年，高血脂', '西安市雁塔区高新路1号', 'O', 162, 65.00),
(23, '郑磊', '13800000023', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 27, '海鲜过敏', '无', '重庆市渝中区解放碑民权路27号', 'A', 176, 73.00),
(24, '黄丽', '13800000024', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 33, '无', '无', '苏州市姑苏区人民路1028号', 'B', 165, 55.00),
(25, '林志强', '13800000025', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 62, '无', '糖尿病12年，视网膜病变', '青岛市市南区香港中路76号', 'O', 170, 78.00),
(26, '何秀英', '13800000026', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 44, '花粉过敏', '过敏性鼻炎', '天津市和平区南京路189号', 'A', 161, 57.00),
(27, '马俊', '13800000027', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 50, '无', '高血压8年', '宁波市海曙区中山西路2号', 'B', 174, 80.00),
(28, '杨雪', '13800000028', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 25, '无', '无', '长沙市岳麓区麓山路932号', 'O', 168, 54.00),
(29, '许明辉', '13800000029', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 46, '酒精过敏', '脂肪肝', '郑州市金水区花园路114号', 'A', 177, 88.00),
(30, '朱红', '13800000030', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 39, '无', '无', '合肥市庐阳区长江中路232号', 'B', 164, 56.00),
(31, '胡建国', '13800000031', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 73, '无', '高血压15年，冠心病5年，房颤', '济南市历下区泉城路168号', 'O', 165, 70.00),
(32, '高敏', '13800000032', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 31, '青霉素过敏', '无', '福州市鼓楼区八一七路1号', 'A', 166, 53.00),
(33, '罗刚', '13800000033', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 58, '无', '慢性胃炎10年', '昆明市五华区东风西路12号', 'B', 171, 76.00),
(34, '梁静', '13800000034', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 41, '无', '无', '南宁市青秀区民族大道18号', 'O', 162, 55.00),
(35, '宋磊', '13800000035', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 36, '花粉过敏', '过敏性鼻炎', '大连市中山区人民路65号', 'A', 179, 81.00),
(36, '唐晓梅', '13800000036', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 63, '磺胺类过敏', '糖尿病5年，肾功能不全', '贵阳市云岩区中华中路98号', 'B', 157, 68.00),
(37, '韩冰', '13800000037', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 28, '无', '无', '太原市小店区长风街1号', 'O', 182, 78.00),
(38, '冯丽娟', '13800000038', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 52, '无', '高血压6年，更年期综合征', '哈尔滨市道里区中央大街1号', 'A', 160, 63.00),
(39, '蒋涛', '13800000039', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'MALE', 45, '酒精过敏', '痛风3年', '长春市朝阳区人民大街1号', 'B', 175, 83.00),
(40, '沈雅琴', '13800000040', '{sha256}8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'FEMALE', 34, '无', '无', '兰州市城关区东岗西路1号', 'O', 167, 58.00)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('patient', 'id'), COALESCE((SELECT MAX(id) FROM patient), 40));

-- ─── 2. 药品（10 → 30，新增 20 种）───
INSERT INTO drug (id, name, specification, contraindication, interaction_rule, status) VALUES
(11, '阿莫西林胶囊', '0.5g*24粒', '青霉素过敏者禁用', '与丙磺舒合用可延长半衰期', 'ENABLED'),
(12, '头孢克肟分散片', '0.1g*6片', '头孢菌素过敏者禁用', '与氨基糖苷类合用增加肾毒性', 'ENABLED'),
(13, '布洛芬缓释胶囊', '0.3g*20粒', '消化性溃疡活动期禁用', '与抗凝药合用增加出血风险', 'ENABLED'),
(14, '对乙酰氨基酚片', '0.5g*12片', '严重肝肾功能不全禁用', '与酒精合用增加肝毒性', 'ENABLED'),
(15, '硝苯地平控释片', '30mg*7片', '心源性休克禁用', '与β受体阻滞剂合用可致低血压', 'ENABLED'),
(16, '阿托伐他汀钙片', '20mg*7片', '活动性肝病禁用', '与红霉素合用增加横纹肌溶解风险', 'ENABLED'),
(17, '二甲双胍片', '0.5g*20片', '肾功能不全(eGFR<30)禁用', '与碘造影剂检查前后48h停用', 'ENABLED'),
(18, '奥美拉唑肠溶胶囊', '20mg*14粒', '无特殊禁忌', '与氯吡格雷合用降低抗血小板效果', 'ENABLED'),
(19, '氯雷他定片', '10mg*6片', '无特殊禁忌', '与酮康唑合用增加不良反应', 'ENABLED'),
(20, '蒙脱石散', '3g*10袋', '肠梗阻禁用', '与其他药物间隔2小时服用', 'ENABLED'),
(21, '盐酸氨溴索片', '30mg*20片', '无特殊禁忌', '与抗生素合用增加肺组织浓度', 'ENABLED'),
(22, '甲硝唑片', '0.2g*21片', '孕妇禁用', '与华法林合用增强抗凝效果', 'ENABLED'),
(23, '阿奇霉素片', '0.25g*6片', '对大环内酯类过敏禁用', '与含铝镁抗酸药间隔2h服用', 'ENABLED'),
(24, '诺氟沙星胶囊', '0.1g*12粒', '18岁以下禁用', '与茶碱合用升高茶碱浓度', 'ENABLED'),
(25, '格列美脲片', '2mg*12片', '1型糖尿病禁用', '与水杨酸盐合用增加低血糖风险', 'ENABLED'),
(26, '缬沙坦胶囊', '80mg*7粒', '孕妇禁用', '与保钾利尿剂合用注意高钾血症', 'ENABLED'),
(27, '美托洛尔缓释片', '47.5mg*7片', '严重心动过缓禁用', '与维拉帕米合用可致传导阻滞', 'ENABLED'),
(28, '华法林钠片', '2.5mg*100片', '活动性出血禁用', '与多种药物有相互作用，需监测INR', 'ENABLED'),
(29, '地高辛片', '0.25mg*100片', '室性心律失常禁用', '与胺碘酮合用升高地高辛浓度', 'ENABLED'),
(30, '螺内酯片', '20mg*100片', '高钾血症禁用', '与ACEI合用增加高钾风险', 'ENABLED')
ON CONFLICT (id) DO NOTHING;

-- ─── 3. 排班（48 → ~144，覆盖未来30天）───
INSERT INTO doctor_schedule (id, doctor_id, department_id, work_date, time_range, capacity, status) VALUES
-- 张医生（心内科）额外排班
(50, 1, 1, CURRENT_DATE + 8, '09:00-12:00', 20, 'PUBLISHED'),
(51, 1, 1, CURRENT_DATE + 8, '14:00-17:00', 15, 'PUBLISHED'),
(52, 1, 1, CURRENT_DATE + 10, '09:00-12:00', 20, 'PUBLISHED'),
(53, 1, 1, CURRENT_DATE + 12, '09:00-12:00', 20, 'PUBLISHED'),
(54, 1, 1, CURRENT_DATE + 14, '14:00-17:00', 15, 'PUBLISHED'),
(55, 1, 1, CURRENT_DATE + 16, '09:00-12:00', 20, 'PUBLISHED'),
(56, 1, 1, CURRENT_DATE + 18, '09:00-12:00', 20, 'PUBLISHED'),
(57, 1, 1, CURRENT_DATE + 20, '14:00-17:00', 15, 'PUBLISHED'),
(58, 1, 1, CURRENT_DATE + 22, '09:00-12:00', 20, 'PUBLISHED'),
(59, 1, 1, CURRENT_DATE + 25, '09:00-12:00', 20, 'PUBLISHED'),
(60, 1, 1, CURRENT_DATE + 28, '14:00-17:00', 15, 'PUBLISHED'),
-- 李医生（全科门诊）额外排班
(70, 2, 2, CURRENT_DATE + 8, '09:00-12:00', 25, 'PUBLISHED'),
(71, 2, 2, CURRENT_DATE + 9, '14:00-17:00', 20, 'PUBLISHED'),
(72, 2, 2, CURRENT_DATE + 11, '09:00-12:00', 25, 'PUBLISHED'),
(73, 2, 2, CURRENT_DATE + 13, '09:00-12:00', 25, 'PUBLISHED'),
(74, 2, 2, CURRENT_DATE + 15, '14:00-17:00', 20, 'PUBLISHED'),
(75, 2, 2, CURRENT_DATE + 17, '09:00-12:00', 25, 'PUBLISHED'),
(76, 2, 2, CURRENT_DATE + 19, '09:00-12:00', 25, 'PUBLISHED'),
(77, 2, 2, CURRENT_DATE + 21, '14:00-17:00', 20, 'PUBLISHED'),
(78, 2, 2, CURRENT_DATE + 23, '09:00-12:00', 25, 'PUBLISHED'),
(79, 2, 2, CURRENT_DATE + 26, '09:00-12:00', 25, 'PUBLISHED'),
(80, 2, 2, CURRENT_DATE + 29, '14:00-17:00', 20, 'PUBLISHED'),
-- 王医生（呼吸内科）额外排班
(90, 3, 3, CURRENT_DATE + 8, '14:00-17:00', 15, 'PUBLISHED'),
(91, 3, 3, CURRENT_DATE + 10, '09:00-12:00', 18, 'PUBLISHED'),
(92, 3, 3, CURRENT_DATE + 12, '14:00-17:00', 15, 'PUBLISHED'),
(93, 3, 3, CURRENT_DATE + 14, '09:00-12:00', 18, 'PUBLISHED'),
(94, 3, 3, CURRENT_DATE + 16, '14:00-17:00', 15, 'PUBLISHED'),
(95, 3, 3, CURRENT_DATE + 18, '09:00-12:00', 18, 'PUBLISHED'),
(96, 3, 3, CURRENT_DATE + 20, '09:00-12:00', 18, 'PUBLISHED'),
(97, 3, 3, CURRENT_DATE + 22, '14:00-17:00', 15, 'PUBLISHED'),
(98, 3, 3, CURRENT_DATE + 24, '09:00-12:00', 18, 'PUBLISHED'),
(99, 3, 3, CURRENT_DATE + 27, '14:00-17:00', 15, 'PUBLISHED'),
(100, 3, 3, CURRENT_DATE + 30, '09:00-12:00', 18, 'PUBLISHED'),
-- 刘医生（神经内科, dept=4）
(110, 4, 4, CURRENT_DATE + 1, '09:00-12:00', 15, 'PUBLISHED'),
(111, 4, 4, CURRENT_DATE + 3, '14:00-17:00', 12, 'PUBLISHED'),
(112, 4, 4, CURRENT_DATE + 5, '09:00-12:00', 15, 'PUBLISHED'),
(113, 4, 4, CURRENT_DATE + 8, '09:00-12:00', 15, 'PUBLISHED'),
(114, 4, 4, CURRENT_DATE + 10, '14:00-17:00', 12, 'PUBLISHED'),
(115, 4, 4, CURRENT_DATE + 13, '09:00-12:00', 15, 'PUBLISHED'),
(116, 4, 4, CURRENT_DATE + 16, '09:00-12:00', 15, 'PUBLISHED'),
(117, 4, 4, CURRENT_DATE + 19, '14:00-17:00', 12, 'PUBLISHED'),
(118, 4, 4, CURRENT_DATE + 22, '09:00-12:00', 15, 'PUBLISHED'),
(119, 4, 4, CURRENT_DATE + 25, '09:00-12:00', 15, 'PUBLISHED'),
(120, 4, 4, CURRENT_DATE + 28, '14:00-17:00', 12, 'PUBLISHED'),
-- 陈医生（消化内科, dept=5）
(130, 5, 5, CURRENT_DATE + 2, '09:00-12:00', 18, 'PUBLISHED'),
(131, 5, 5, CURRENT_DATE + 4, '14:00-17:00', 15, 'PUBLISHED'),
(132, 5, 5, CURRENT_DATE + 6, '09:00-12:00', 18, 'PUBLISHED'),
(133, 5, 5, CURRENT_DATE + 9, '09:00-12:00', 18, 'PUBLISHED'),
(134, 5, 5, CURRENT_DATE + 11, '14:00-17:00', 15, 'PUBLISHED'),
(135, 5, 5, CURRENT_DATE + 14, '09:00-12:00', 18, 'PUBLISHED'),
(136, 5, 5, CURRENT_DATE + 17, '09:00-12:00', 18, 'PUBLISHED'),
(137, 5, 5, CURRENT_DATE + 20, '14:00-17:00', 15, 'PUBLISHED'),
(138, 5, 5, CURRENT_DATE + 23, '09:00-12:00', 18, 'PUBLISHED'),
(139, 5, 5, CURRENT_DATE + 26, '09:00-12:00', 18, 'PUBLISHED'),
(140, 5, 5, CURRENT_DATE + 29, '14:00-17:00', 15, 'PUBLISHED'),
-- 赵医生（全科门诊, dept=2）
(150, 6, 2, CURRENT_DATE + 2, '14:00-17:00', 20, 'PUBLISHED'),
(151, 6, 2, CURRENT_DATE + 4, '09:00-12:00', 25, 'PUBLISHED'),
(152, 6, 2, CURRENT_DATE + 7, '09:00-12:00', 25, 'PUBLISHED'),
(153, 6, 2, CURRENT_DATE + 10, '14:00-17:00', 20, 'PUBLISHED'),
(154, 6, 2, CURRENT_DATE + 13, '09:00-12:00', 25, 'PUBLISHED'),
(155, 6, 2, CURRENT_DATE + 16, '14:00-17:00', 20, 'PUBLISHED'),
(156, 6, 2, CURRENT_DATE + 19, '09:00-12:00', 25, 'PUBLISHED'),
(157, 6, 2, CURRENT_DATE + 22, '14:00-17:00', 20, 'PUBLISHED'),
(158, 6, 2, CURRENT_DATE + 25, '09:00-12:00', 25, 'PUBLISHED'),
(159, 6, 2, CURRENT_DATE + 28, '09:00-12:00', 25, 'PUBLISHED'),
-- 周医生（心内科, dept=1）
(160, 7, 1, CURRENT_DATE + 2, '14:00-17:00', 15, 'PUBLISHED'),
(161, 7, 1, CURRENT_DATE + 4, '09:00-12:00', 20, 'PUBLISHED'),
(162, 7, 1, CURRENT_DATE + 7, '14:00-17:00', 15, 'PUBLISHED'),
(163, 7, 1, CURRENT_DATE + 10, '09:00-12:00', 20, 'PUBLISHED'),
(164, 7, 1, CURRENT_DATE + 13, '14:00-17:00', 15, 'PUBLISHED'),
(165, 7, 1, CURRENT_DATE + 16, '09:00-12:00', 20, 'PUBLISHED'),
(166, 7, 1, CURRENT_DATE + 19, '14:00-17:00', 15, 'PUBLISHED'),
(167, 7, 1, CURRENT_DATE + 22, '09:00-12:00', 20, 'PUBLISHED'),
(168, 7, 1, CURRENT_DATE + 25, '14:00-17:00', 15, 'PUBLISHED'),
(169, 7, 1, CURRENT_DATE + 28, '09:00-12:00', 20, 'PUBLISHED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('doctor_schedule', 'id'), COALESCE((SELECT MAX(id) FROM doctor_schedule), 170));

-- ─── 4. 挂号（17 → ~50）───
INSERT INTO registration (id, patient_id, doctor_id, department_id, appointment_time, status) VALUES
-- 张医生的挂号
(20, 14, 1, 1, CURRENT_DATE - 10 + INTERVAL '9 hours', 'COMPLETED'),
(21, 15, 1, 1, CURRENT_DATE - 8 + INTERVAL '14 hours', 'COMPLETED'),
(22, 16, 1, 1, CURRENT_DATE - 6 + INTERVAL '9 hours', 'COMPLETED'),
(23, 17, 1, 1, CURRENT_DATE - 4 + INTERVAL '9 hours', 'COMPLETED'),
(24, 18, 1, 1, CURRENT_DATE - 2 + INTERVAL '14 hours', 'CONFIRMED'),
(25, 19, 1, 1, CURRENT_DATE + 1 + INTERVAL '9 hours', 'CONFIRMED'),
-- 李医生的挂号
(30, 20, 2, 2, CURRENT_DATE - 9 + INTERVAL '9 hours', 'COMPLETED'),
(31, 21, 2, 2, CURRENT_DATE - 7 + INTERVAL '14 hours', 'COMPLETED'),
(32, 22, 2, 2, CURRENT_DATE - 5 + INTERVAL '9 hours', 'COMPLETED'),
(33, 23, 2, 2, CURRENT_DATE - 3 + INTERVAL '9 hours', 'COMPLETED'),
(34, 24, 2, 2, CURRENT_DATE - 1 + INTERVAL '14 hours', 'CONFIRMED'),
(35, 25, 2, 2, CURRENT_DATE + 1 + INTERVAL '9 hours', 'CONFIRMED'),
-- 王医生的挂号
(40, 26, 3, 3, CURRENT_DATE - 10 + INTERVAL '9 hours', 'COMPLETED'),
(41, 27, 3, 3, CURRENT_DATE - 7 + INTERVAL '14 hours', 'COMPLETED'),
(42, 28, 3, 3, CURRENT_DATE - 5 + INTERVAL '9 hours', 'COMPLETED'),
(43, 29, 3, 3, CURRENT_DATE - 3 + INTERVAL '14 hours', 'COMPLETED'),
(44, 30, 3, 3, CURRENT_DATE - 1 + INTERVAL '9 hours', 'CONFIRMED'),
(45, 31, 3, 3, CURRENT_DATE + 1 + INTERVAL '9 hours', 'CONFIRMED'),
-- 刘医生的挂号
(50, 32, 4, 4, CURRENT_DATE - 8 + INTERVAL '9 hours', 'COMPLETED'),
(51, 33, 4, 4, CURRENT_DATE - 5 + INTERVAL '14 hours', 'COMPLETED'),
(52, 34, 4, 4, CURRENT_DATE - 2 + INTERVAL '9 hours', 'CONFIRMED'),
(53, 35, 4, 4, CURRENT_DATE + 2 + INTERVAL '9 hours', 'CONFIRMED'),
-- 陈医生的挂号
(60, 36, 5, 5, CURRENT_DATE - 9 + INTERVAL '9 hours', 'COMPLETED'),
(61, 37, 5, 5, CURRENT_DATE - 6 + INTERVAL '14 hours', 'COMPLETED'),
(62, 38, 5, 5, CURRENT_DATE - 3 + INTERVAL '9 hours', 'CONFIRMED'),
(63, 39, 5, 5, CURRENT_DATE + 2 + INTERVAL '9 hours', 'CONFIRMED'),
-- 赵医生的挂号
(70, 40, 6, 2, CURRENT_DATE - 8 + INTERVAL '14 hours', 'COMPLETED'),
(71, 14, 6, 2, CURRENT_DATE - 5 + INTERVAL '9 hours', 'COMPLETED'),
(72, 15, 6, 2, CURRENT_DATE - 2 + INTERVAL '9 hours', 'CONFIRMED'),
-- 周医生的挂号
(80, 16, 7, 1, CURRENT_DATE - 7 + INTERVAL '14 hours', 'COMPLETED'),
(81, 17, 7, 1, CURRENT_DATE - 4 + INTERVAL '9 hours', 'COMPLETED'),
(82, 18, 7, 1, CURRENT_DATE - 1 + INTERVAL '14 hours', 'CONFIRMED'),
(83, 19, 7, 1, CURRENT_DATE + 3 + INTERVAL '9 hours', 'CONFIRMED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('registration', 'id'), COALESCE((SELECT MAX(id) FROM registration), 83));

-- ─── 5. 分诊记录（37 → ~110）───
INSERT INTO triage_record (id, patient_id, chief_complaint, recommended_department, recommended_doctor_ids, assigned_doctor_id, reason, status) VALUES
-- 挂号对应的分诊
(40, 14, '胸闷气短2天，活动后加重', '心内科', '1,7', 1, '症状提示心绞痛可能，建议心内科就诊', 'ASSIGNED'),
(41, 15, '头痛3天，伴有恶心', '心内科', '1,7', 1, '血压偏高，建议心内科评估', 'ASSIGNED'),
(42, 16, '心悸一周，偶有早搏', '心内科', '1,7', 1, '心悸症状需心内科排查', 'ASSIGNED'),
(43, 17, '胸痛1小时，持续不缓解', '心内科', '1,7', 1, '急性胸痛需紧急心内科处理', 'ASSIGNED'),
(44, 18, '咳嗽伴黄痰3天', '呼吸内科', '3', 3, '呼吸道感染症状', 'ASSIGNED'),
(45, 19, '发热38.5℃两天', '呼吸内科', '3', 3, '发热待查', 'ASSIGNED'),
(50, 20, '胃痛反复发作2年', '消化内科', '5', 5, '慢性胃痛需消化内科评估', 'ASSIGNED'),
(51, 21, '腹泻3天，水样便', '全科门诊', '2,6', 2, '急性胃肠炎可能', 'ASSIGNED'),
(52, 22, '头晕伴血压升高', '心内科', '1,7', 1, '高血压需心内科管理', 'ASSIGNED'),
(53, 23, '海鲜过敏后全身皮疹', '全科门诊', '2,6', 2, '过敏反应需抗过敏治疗', 'ASSIGNED'),
(54, 24, '体检发现血糖偏高', '全科门诊', '2,6', 2, '血糖异常需进一步评估', 'ASSIGNED'),
(55, 25, '糖尿病复查，调整用药', '全科门诊', '2,6', 2, '慢性病随访', 'ASSIGNED'),
(60, 26, '过敏性鼻炎发作', '呼吸内科', '3', 3, '过敏性鼻炎需呼吸科处理', 'ASSIGNED'),
(61, 27, '高血压复诊，血压控制不佳', '心内科', '1,7', 1, '血压控制不佳需调整方案', 'ASSIGNED'),
(62, 28, '体检发现甲状腺结节', '全科门诊', '2,6', 2, '甲状腺结节需评估', 'ASSIGNED'),
(63, 29, '痛风发作，左足关节红肿', '全科门诊', '2,6', 2, '急性痛风需抗炎治疗', 'ASSIGNED'),
(64, 30, '更年期症状，潮热出汗', '全科门诊', '2,6', 2, '更年期综合征', 'ASSIGNED'),
(65, 31, '高血压合并房颤', '心内科', '1,7', 1, '房颤需心内科管理', 'ASSIGNED'),
(70, 32, '青霉素过敏后呼吸困难', '呼吸内科', '3', 3, '严重过敏反应需紧急处理', 'ASSIGNED'),
(71, 33, '慢性胃炎复诊', '消化内科', '5', 5, '慢性胃炎随访', 'ASSIGNED'),
(72, 34, '体检发现脂肪肝', '全科门诊', '2,6', 2, '脂肪肝需生活方式指导', 'ASSIGNED'),
(73, 35, '过敏性鼻炎季节性发作', '呼吸内科', '3', 3, '季节性过敏', 'ASSIGNED'),
(80, 36, '糖尿病合并肾功能不全', '全科门诊', '2,6', 2, '糖尿病肾病需综合管理', 'ASSIGNED'),
(81, 37, '运动后胸闷', '心内科', '1,7', 1, '运动耐量下降需心内科评估', 'ASSIGNED'),
(82, 38, '高血压合并更年期', '心内科', '1,7', 1, '更年期高血压需综合管理', 'ASSIGNED'),
(83, 39, '痛风反复发作', '全科门诊', '2,6', 2, '痛风需长期管理', 'ASSIGNED'),
(84, 40, '体检一切正常，咨询保健', '全科门诊', '2,6', 2, '健康咨询', 'ASSIGNED'),
-- 其他散落分诊（AI_RECOMMENDED 未分配）
(90, 14, '失眠一周，入睡困难', '全科门诊', '2,6', NULL, '失眠需评估', 'AI_RECOMMENDED'),
(91, 15, '腰痛两周', '全科门诊', '2,6', NULL, '腰痛待查', 'AI_RECOMMENDED'),
(92, 16, '视力模糊加重', '全科门诊', '2,6', NULL, '视力下降需排查', 'AI_RECOMMENDED'),
(93, 17, '关节疼痛，晨起僵硬', '全科门诊', '2,6', NULL, '关节症状需排查风湿', 'AI_RECOMMENDED'),
(94, 18, '皮肤瘙痒反复', '全科门诊', '2,6', NULL, '皮肤瘙痒需评估', 'AI_RECOMMENDED'),
(95, 19, '便秘一周', '消化内科', '5', NULL, '便秘需消化科评估', 'AI_RECOMMENDED'),
(96, 20, '反酸烧心', '消化内科', '5', NULL, '胃食管反流可能', 'AI_RECOMMENDED'),
(97, 21, '尿频尿急', '全科门诊', '2,6', NULL, '泌尿系感染可能', 'AI_RECOMMENDED'),
(98, 22, '体重增加明显', '全科门诊', '2,6', NULL, '体重管理咨询', 'AI_RECOMMENDED'),
(99, 23, '反复口腔溃疡', '全科门诊', '2,6', NULL, '口腔溃疡需排查', 'AI_RECOMMENDED'),
(100, 24, '脱发增多', '全科门诊', '2,6', NULL, '脱发需评估', 'AI_RECOMMENDED'),
(101, 25, '颈椎不适', '全科门诊', '2,6', NULL, '颈椎病可能', 'AI_RECOMMENDED'),
(102, 26, '手脚麻木', '神经内科', '4', NULL, '周围神经病变可能', 'AI_RECOMMENDED'),
(103, 27, '记忆力下降', '神经内科', '4', NULL, '认知功能下降需评估', 'AI_RECOMMENDED'),
(104, 28, '心慌手抖', '心内科', '1,7', NULL, '甲亢可能', 'AI_RECOMMENDED'),
(105, 29, '腹胀纳差', '消化内科', '5', NULL, '消化功能不良', 'AI_RECOMMENDED'),
(106, 30, '盗汗低热', '呼吸内科', '3', NULL, '盗汗低热需排查', 'AI_RECOMMENDED'),
(107, 31, '下肢水肿', '心内科', '1,7', NULL, '水肿需排查心肾原因', 'AI_RECOMMENDED'),
(108, 32, '呼吸困难加重', '呼吸内科', '3', NULL, '呼吸困难需紧急评估', 'AI_RECOMMENDED'),
(109, 33, '黑便一次', '消化内科', '5', NULL, '消化道出血可能', 'AI_RECOMMENDED'),
(110, 34, '抽搐一次', '神经内科', '4', NULL, '抽搐需神经内科评估', 'AI_RECOMMENDED'),
(111, 35, '耳鸣持续', '全科门诊', '2,6', NULL, '耳鸣需评估', 'AI_RECOMMENDED'),
(112, 36, '胸痛放射至左肩', '心内科', '1,7', NULL, '典型心绞痛表现', 'AI_RECOMMENDED'),
(113, 37, '夜间打鼾', '呼吸内科', '3', NULL, '睡眠呼吸暂停可能', 'AI_RECOMMENDED'),
(114, 38, '潮热加重', '全科门诊', '2,6', NULL, '更年期症状加重', 'AI_RECOMMENDED'),
(115, 39, '血尿一次', '全科门诊', '2,6', NULL, '血尿需排查', 'AI_RECOMMENDED'),
(116, 40, '焦虑失眠', '全科门诊', '2,6', NULL, '焦虑状态需评估', 'AI_RECOMMENDED'),
(117, 14, '咳嗽加重伴发热', '呼吸内科', '3', NULL, '呼吸道感染', 'AI_RECOMMENDED'),
(118, 15, '胃痛加重', '消化内科', '5', NULL, '胃痛需消化科评估', 'AI_RECOMMENDED'),
(119, 16, '头晕加重', '神经内科', '4', NULL, '头晕需排查', 'AI_RECOMMENDED')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('triage_record', 'id'), COALESCE((SELECT MAX(id) FROM triage_record), 120));

-- ─── 6. 病历（11 → ~33）───
INSERT INTO medical_record (id, patient_id, doctor_id, registration_id, chief_complaint, present_illness, past_history, physical_exam, diagnosis, treatment_advice) VALUES
(20, 14, 1, 20, '胸闷气短2天', '患者2天前无明显诱因出现胸闷气短，活动后加重，休息后稍缓解', '高血压病史3年', 'BP 145/92mmHg, HR 88bpm, 心律齐', '冠心病 不稳定型心绞痛', '1.阿司匹林100mg qd 2.硝苯地平30mg qd 3.低盐低脂饮食 4.不适随诊'),
(21, 15, 1, 21, '头痛3天伴恶心', '3天前出现头痛，以枕部为主，伴恶心，未呕吐', '无', 'BP 162/98mmHg, 神经系统检查阴性', '高血压病2级', '1.硝苯地平30mg qd 2.低盐饮食 3.监测血压 4.1周后复诊'),
(22, 16, 1, 22, '心悸一周', '一周前开始出现心悸，偶有早搏感，持续数秒', '无', 'HR 82bpm, 偶发早搏', '心律失常 早搏', '1.完善24小时动态心电图 2.避免浓茶咖啡 3.观察'),
(23, 17, 1, 23, '胸痛1小时', '1小时前突发胸痛，位于胸骨后，压榨性，持续不缓解', '无', 'BP 138/86mmHg, HR 96bpm', '急性冠脉综合征待排', '1.立即心电图 2.心肌酶谱 3.阿司匹林300mg嚼服 4.收入院'),
(24, 20, 2, 30, '胃痛反复发作2年', '2年来反复出现上腹部隐痛，进食后加重，偶有反酸', '无', '上腹部压痛(+), 余(-)', '慢性胃炎', '1.奥美拉唑20mg bid 2.清淡饮食 3.忌辛辣 4.2周后复查'),
(25, 21, 2, 31, '腹泻3天', '3天前饮食不洁后出现腹泻，水样便，日6-8次', '无', '腹部软，脐周压痛(+), 肠鸣音活跃', '急性胃肠炎', '1.蒙脱石散3g tid 2.口服补液盐 3.清淡流食 4.发热就诊'),
(26, 22, 2, 32, '头晕伴血压升高', '反复头晕1周，自测血压150-160/95-100mmHg', '高血压病史5年', 'BP 158/96mmHg, HR 78bpm', '高血压病2级 高血脂', '1.阿托伐他汀20mg qn 2.继续降压药 3.低脂饮食 4.1月后复查血脂'),
(27, 26, 3, 40, '过敏性鼻炎发作', '进入春季后鼻塞、流清涕、打喷嚏加重', '过敏性鼻炎3年', '鼻黏膜苍白水肿, 双下甲肿大', '过敏性鼻炎', '1.氯雷他定10mg qd 2.布地奈德鼻喷剂 3.避免过敏原'),
(28, 27, 3, 41, '高血压复诊', '口服降压药后血压仍控制不佳', '高血压8年', 'BP 152/94mmHg, HR 72bpm', '高血压病2级 控制不佳', '1.调整降压方案 2.缬沙坦80mg qd 3.低盐饮食 4.2周后复诊'),
(29, 32, 4, 50, '青霉素过敏后呼吸困难', '注射青霉素后10分钟出现呼吸困难、皮疹', '青霉素过敏史', 'BP 128/82mmHg, HR 102bpm, 全身荨麻疹', '药物过敏反应', '1.立即停用青霉素 2.地塞米松10mg iv 3.氯雷他定10mg 4.观察2小时'),
(30, 33, 5, 60, '慢性胃炎复诊', '服药后胃痛明显缓解，偶有反酸', '慢性胃炎10年', '上腹部轻压痛(+)', '慢性胃炎 好转', '1.继续奥美拉唑 2.饮食规律 3.3月后复查胃镜'),
(31, 36, 5, 61, '糖尿病合并肾功能不全', '糖尿病12年，近期尿泡沫增多', '糖尿病12年，肾功能不全', 'BP 148/88mmHg, BMI 27.3', '2型糖尿病 糖尿病肾病IV期', '1.二甲双胍减量 2.加用SGLT2抑制剂 3.低蛋白饮食 4.监测肾功能'),
(32, 40, 6, 70, '体检一切正常', '年度体检各项指标正常', '无', 'BP 118/76mmHg, BMI 22.1', '健康体检 未见异常', '1.保持良好生活习惯 2.适当运动 3.均衡饮食 4.年度体检')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('medical_record', 'id'), COALESCE((SELECT MAX(id) FROM medical_record), 32));

-- ─── 7. 处方（11 → ~33）───
INSERT INTO prescription (id, patient_id, doctor_id, medical_record_id, risk_level, status, registration_id) VALUES
(20, 14, 1, 20, 'LOW', 'ACTIVE', 20),
(21, 15, 1, 21, 'LOW', 'ACTIVE', 21),
(22, 16, 1, 22, 'LOW', 'ACTIVE', 22),
(23, 17, 1, 23, 'HIGH', 'ACTIVE', 23),
(24, 20, 2, 24, 'LOW', 'ACTIVE', 30),
(25, 21, 2, 25, 'LOW', 'ACTIVE', 31),
(26, 22, 2, 26, 'MEDIUM', 'ACTIVE', 32),
(27, 26, 3, 27, 'LOW', 'ACTIVE', 40),
(28, 27, 3, 28, 'LOW', 'ACTIVE', 41),
(29, 32, 4, 29, 'MEDIUM', 'ACTIVE', 50),
(30, 33, 5, 30, 'LOW', 'ACTIVE', 60),
(31, 36, 5, 31, 'HIGH', 'ACTIVE', 61),
(32, 40, 6, 32, 'LOW', 'ACTIVE', 70)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('prescription', 'id'), COALESCE((SELECT MAX(id) FROM prescription), 32));

-- ─── 8. 处方明细（30 → ~90）───
INSERT INTO prescription_item (id, prescription_id, drug_name, dosage, frequency, usage_method, days) VALUES
-- 处方20：冠心病
(40, 20, '阿司匹林肠溶片', '100mg', '每日一次', '口服', 30),
(41, 20, '硝苯地平控释片', '30mg', '每日一次', '口服', 30),
(42, 20, '阿托伐他汀钙片', '20mg', '每晚一次', '口服', 30),
-- 处方21：高血压
(43, 21, '硝苯地平控释片', '30mg', '每日一次', '口服', 14),
-- 处方22：早搏
(44, 22, '美托洛尔缓释片', '47.5mg', '每日一次', '口服', 14),
-- 处方23：急性冠脉
(45, 23, '阿司匹林肠溶片', '300mg', '嚼服', '口服', 1),
(46, 23, '氯吡格雷片', '300mg', '负荷量', '口服', 1),
(47, 23, '硝酸甘油片', '0.5mg', '必要时', '舌下含服', 7),
-- 处方24：胃炎
(48, 24, '奥美拉唑肠溶胶囊', '20mg', '每日两次', '口服', 14),
-- 处方25：胃肠炎
(49, 25, '蒙脱石散', '3g', '每日三次', '冲服', 3),
(50, 25, '口服补液盐', '1袋', '每日多次', '冲服', 3),
-- 处方26：高血压+高血脂
(51, 26, '阿托伐他汀钙片', '20mg', '每晚一次', '口服', 30),
(52, 26, '缬沙坦胶囊', '80mg', '每日一次', '口服', 30),
-- 处方27：过敏性鼻炎
(53, 27, '氯雷他定片', '10mg', '每日一次', '口服', 7),
(54, 27, '布地奈德鼻喷剂', '1喷', '每日两次', '鼻喷', 14),
-- 处方28：高血压
(55, 28, '缬沙坦胶囊', '80mg', '每日一次', '口服', 30),
(56, 28, '硝苯地平控释片', '30mg', '每日一次', '口服', 30),
-- 处方29：过敏
(57, 29, '地塞米松注射液', '10mg', '静推', '静脉注射', 1),
(58, 29, '氯雷他定片', '10mg', '每日一次', '口服', 3),
-- 处方30：胃炎复诊
(59, 30, '奥美拉唑肠溶胶囊', '20mg', '每日一次', '口服', 28),
-- 处方31：糖尿病肾病
(60, 31, '二甲双胍片', '250mg', '每日两次', '口服', 30),
(61, 31, '缬沙坦胶囊', '80mg', '每日一次', '口服', 30),
(62, 31, '螺内酯片', '20mg', '每日一次', '口服', 30),
-- 处方32：健康
(63, 32, '无', '-', '-', '-', 0)
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('prescription_item', 'id'), COALESCE((SELECT MAX(id) FROM prescription_item), 63));

-- ─── 9. 通知（18 → ~54）───
INSERT INTO notification_message (id, doctor_id, patient_id, prescription_id, type, title, content, risk_level, read_status, created_at) VALUES
-- 处方审核通知
(30, 1, 14, 20, 'PRESCRIPTION_REVIEW', '处方审核通过', '张医生，您为患者刘洋开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ', CURRENT_TIMESTAMP - INTERVAL '10 days'),
(31, 1, 15, 21, 'PRESCRIPTION_REVIEW', '处方审核通过', '张医生，您为患者张伟开具的处方已通过审核，风险等级：低风险。', 'LOW', 'READ', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(32, 1, 17, 23, 'PRESCRIPTION_REVIEW', '处方审核-高风险', '张医生，您为患者赵强开具的处方存在药物相互作用风险，请复核。', 'HIGH', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(33, 2, 20, 24, 'PRESCRIPTION_REVIEW', '处方审核通过', '李医生，您为患者孙丽华开具的处方已通过审核。', 'LOW', 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(34, 2, 22, 26, 'PRESCRIPTION_REVIEW', '处方审核-中风险', '李医生，您为患者吴芳开具的处方存在中等风险，请注意。', 'MEDIUM', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(35, 3, 26, 27, 'PRESCRIPTION_REVIEW', '处方审核通过', '王医生，您为患者何秀英开具的处方已通过审核。', 'LOW', 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
-- 病历生成通知
(40, 1, 14, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者刘洋的病历已由AI辅助生成，请审核确认。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '10 days'),
(41, 1, 15, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者张伟的病历已由AI辅助生成，请审核确认。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(42, 2, 20, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者孙丽华的病历已由AI辅助生成，请审核确认。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(43, 3, 26, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者何秀英的病历已由AI辅助生成，请审核确认。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(44, 4, 32, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者杨雪的病历已由AI辅助生成，请审核确认。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(45, 5, 36, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者唐晓梅的病历已由AI辅助生成，请审核确认。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '6 days'),
-- 分诊通知
(50, 1, 14, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者刘洋因胸闷气短被分诊至心内科，请及时接诊。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '10 days'),
(51, 1, 15, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者张伟因头痛被分诊至心内科，请及时接诊。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(52, 2, 20, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者孙丽华因胃痛被分诊至消化内科，请及时接诊。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(53, 3, 26, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者何秀英因过敏性鼻炎被分诊至呼吸内科，请及时接诊。', NULL, 'READ', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(54, 4, 32, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者杨雪因过敏反应被分诊至呼吸内科，请及时接诊。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(55, 5, 36, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者唐晓梅因糖尿病肾病被分诊至消化内科，请及时接诊。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '6 days'),
(56, 1, 17, NULL, 'TRIAGE_ASSIGN', '紧急分诊', '患者赵强因急性胸痛被紧急分诊至心内科，请立即处理！', 'HIGH', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(57, 6, 40, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者沈雅琴因健康咨询被分诊至全科门诊。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(58, 7, 16, NULL, 'TRIAGE_ASSIGN', '新分诊分配', '患者王秀英因心悸被分诊至心内科，请及时接诊。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '6 days'),
-- 更多近期通知
(60, 1, 16, NULL, 'PRESCRIPTION_REVIEW', '处方审核通过', '张医生，您为患者王秀英开具的处方已通过审核。', 'LOW', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(61, 2, 21, NULL, 'PRESCRIPTION_REVIEW', '处方审核通过', '李医生，您为患者周强开具的处方已通过审核。', 'LOW', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(62, 3, 27, NULL, 'PRESCRIPTION_REVIEW', '处方审核通过', '王医生，您为患者马俊开具的处方已通过审核。', 'LOW', 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(63, 1, 14, NULL, 'MEDICAL_RECORD', '病历审核提醒', '患者刘洋的病历已超过24小时未审核，请尽快处理。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(64, 2, 21, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者周强的病历已由AI辅助生成，请审核确认。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(65, 3, 27, NULL, 'MEDICAL_RECORD', '病历生成完成', '患者马俊的病历已由AI辅助生成，请审核确认。', NULL, 'UNREAD', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('notification_message', 'id'), COALESCE((SELECT MAX(id) FROM notification_message), 65));

-- ─── 10. 知识库（10 → 30，新增 20 条）───
INSERT INTO knowledge_entry (title, symptoms, risk_signals, advice, department_code, status) VALUES
('急性心肌梗死', '胸骨后压榨性疼痛、大汗淋漓、放射至左臂、恶心呕吐', '持续胸痛>15分钟、大汗、濒死感', '立即120急救，嚼服阿司匹林300mg，绝对卧床', 'CARDIOLOGY', 'ENABLED'),
('高血压急症', '血压急剧升高>180/120mmHg、头痛、视物模糊、胸闷', '血压>180/120、靶器官损害表现', '立即降压处理，硝苯地平或乌拉地尔静脉用药', 'CARDIOLOGY', 'ENABLED'),
('心房颤动', '心悸、胸闷、脉搏不规则、乏力', '心室率>100bpm、血流动力学不稳定', '控制心室率，评估抗凝治疗需求', 'CARDIOLOGY', 'ENABLED'),
('慢性阻塞性肺疾病急性加重', '咳嗽加重、痰量增多、呼吸困难、发热', 'SpO2<90%、意识改变、严重呼吸困难', '支气管扩张剂、抗生素、必要时氧疗', 'PULMONOLOGY', 'ENABLED'),
('支气管哮喘急性发作', '喘息、呼吸困难、胸闷、咳嗽', '说话困难、端坐呼吸、SpO2<90%', '沙丁胺醇雾化吸入，严重者全身激素', 'PULMONOLOGY', 'ENABLED'),
('社区获得性肺炎', '发热、咳嗽、咳痰、胸痛、呼吸困难', '高热不退、血氧下降、意识改变', '抗生素治疗，支持治疗，必要时住院', 'PULMONOLOGY', 'ENABLED'),
('急性胃肠炎', '腹痛、腹泻、恶心呕吐、发热', '严重脱水、血便、高热不退', '补液、饮食调整、必要时抗生素', 'GASTRO', 'ENABLED'),
('消化性溃疡', '上腹痛、反酸、嗳气、餐后加重', '黑便、呕血、剧烈腹痛', '质子泵抑制剂，幽门螺杆菌检测', 'GASTRO', 'ENABLED'),
('急性阑尾炎', '转移性右下腹痛、恶心、低热', '反跳痛、肌紧张、高热', '外科会诊，可能需手术', 'GENERAL', 'ENABLED'),
('糖尿病酮症酸中毒', '多饮多尿、恶心呕吐、腹痛、意识模糊', '血糖>20mmol/L、代谢性酸中毒、Kussmaul呼吸', '紧急补液、胰岛素静脉泵入', 'GENERAL', 'ENABLED'),
('低血糖反应', '心悸、出汗、手抖、饥饿感、意识模糊', '血糖<3.9mmol/L、意识改变', '立即口服葡萄糖或静脉注射葡萄糖', 'GENERAL', 'ENABLED'),
('甲状腺功能亢进', '心悸、手抖、消瘦、多汗、易怒', '心率>100bpm、体重明显下降', '抗甲状腺药物治疗，避免碘摄入', 'ENDOCRINE', 'ENABLED'),
('急性脑梗死', '一侧肢体无力、言语不清、面瘫、意识障碍', '突发神经功能缺损、NIHSS评分', '4.5h内可溶栓，超过时间窗抗血小板', 'NEURO', 'ENABLED'),
('帕金森病', '静止性震颤、运动迟缓、肌强直、姿势不稳', '步态不稳、频繁跌倒', '左旋多巴替代治疗，康复训练', 'NEURO', 'ENABLED'),
('过敏性休克', '皮疹、呼吸困难、血压下降、意识丧失', '血压<90/60mmHg、喉头水肿', '立即肾上腺素0.5mg肌注，开放气道', 'EMERGENCY', 'ENABLED'),
('急性胰腺炎', '上腹剧痛、恶心呕吐、腹胀、发热', '血淀粉酶>3倍正常值、CT示胰腺水肿', '禁食、补液、镇痛、必要时ICU', 'GASTRO', 'ENABLED'),
('泌尿系感染', '尿频、尿急、尿痛、下腹不适', '高热、腰痛、血尿', '多饮水，抗生素治疗', 'GENERAL', 'ENABLED'),
('痛风急性发作', '关节红肿热痛、活动受限、血尿酸升高', '关节剧烈疼痛、发热', '秋水仙碱或NSAIDs，低嘌呤饮食', 'GENERAL', 'ENABLED'),
('贫血', '乏力、头晕、面色苍白、心悸', '血红蛋白<60g/L、活动后气促', '查明病因，必要时输血', 'GENERAL', 'ENABLED'),
('失眠症', '入睡困难、早醒、多梦、白天疲乏', '持续>1个月、影响日间功能', '睡眠卫生教育，必要时药物辅助', 'GENERAL', 'ENABLED')
ON CONFLICT DO NOTHING;

-- ─── 11. 提示词模板（4 → 12）───
INSERT INTO prompt_template (task_type, department_code, template_name, template_content, output_schema, version, enabled) VALUES
('TRIAGE', 'CARDIOLOGY', 'cardiology_triage_v1', '你是一位心内科分诊专家。根据患者主诉、症状和病史，判断是否需要心内科就诊，给出分诊建议。', '{"type":"object","required":["recommendedDepartment","urgencyLevel","confidence","reason"]}', 'v1', true),
('TRIAGE', 'PULMONOLOGY', 'pulmonology_triage_v1', '你是一位呼吸内科分诊专家。根据患者主诉和症状，判断是否需要呼吸内科就诊。', '{"type":"object","required":["recommendedDepartment","urgencyLevel","confidence","reason"]}', 'v1', true),
('TRIAGE', 'NEURO', 'neuro_triage_v1', '你是一位神经内科分诊专家。根据患者神经系统症状，判断是否需要神经内科就诊。', '{"type":"object","required":["recommendedDepartment","urgencyLevel","confidence","reason"]}', 'v1', true),
('MEDICAL_RECORD', 'CARDIOLOGY', 'cardiology_record_v1', '你是一位心内科专家医生。根据接诊信息生成SOAP格式病历，包含主诉、现病史、既往史、体格检查、诊断和治疗建议。', '{"type":"object","required":["chiefComplaint","presentIllness","pastHistory","physicalExam","diagnosis","treatmentAdvice","soapContent"]}', 'v1', true),
('MEDICAL_RECORD', 'PULMONOLOGY', 'pulmonology_record_v1', '你是一位呼吸内科专家医生。根据接诊信息生成SOAP格式病历。', '{"type":"object","required":["chiefComplaint","presentIllness","pastHistory","physicalExam","diagnosis","treatmentAdvice","soapContent"]}', 'v1', true),
('MEDICAL_RECORD', 'GASTRO', 'gastro_record_v1', '你是一位消化内科专家医生。根据接诊信息生成SOAP格式病历。', '{"type":"object","required":["chiefComplaint","presentIllness","pastHistory","physicalExam","diagnosis","treatmentAdvice","soapContent"]}', 'v1', true),
('MEDICAL_RECORD', 'NEURO', 'neuro_record_v1', '你是一位神经内科专家医生。根据接诊信息生成SOAP格式病历。', '{"type":"object","required":["chiefComplaint","presentIllness","pastHistory","physicalExam","diagnosis","treatmentAdvice","soapContent"]}', 'v1', true),
('PRESCRIPTION_CHECK', 'GENERAL', 'prescription_check_v1', '你是处方审核专家。检查处方中的药物相互作用、禁忌症、剂量合理性，给出风险评估。', '{"type":"object","required":["riskLevel","riskDescription","suggestions","interactions","contraindications","adjustmentSuggestions"]}', 'v1', true),
('PRESCRIPTION_CHECK', 'CARDIOLOGY', 'cardiology_prescription_check_v1', '你是心内科处方审核专家。重点检查心血管药物的相互作用和剂量。', '{"type":"object","required":["riskLevel","riskDescription","suggestions","interactions","contraindications","adjustmentSuggestions"]}', 'v1', true),
('TRIAGE', 'GENERAL', 'general_triage_v1', '你是一位全科分诊专家。根据患者主诉和症状，进行初步评估和分诊。', '{"type":"object","required":["recommendedDepartment","urgencyLevel","confidence","reason"]}', 'v1', true),
('MEDICAL_RECORD', 'GENERAL', 'general_record_v1', '你是一位全科医生。根据接诊信息生成SOAP格式病历。', '{"type":"object","required":["chiefComplaint","presentIllness","pastHistory","physicalExam","diagnosis","treatmentAdvice","soapContent"]}', 'v1', true),
('PRESCRIPTION_CHECK', 'NEURO', 'neuro_prescription_check_v1', '你是神经内科处方审核专家。重点检查神经系统药物的相互作用。', '{"type":"object","required":["riskLevel","riskDescription","suggestions","interactions","contraindications","adjustmentSuggestions"]}', 'v1', true)
ON CONFLICT DO NOTHING;

-- ─── 12. 医疗设备（6 → 18）───
INSERT INTO medical_device (device_code, name, category, department_id, location, status, purchase_date) VALUES
('DEV-XRAY-001', '数字化X光机', '影像设备', 1, '心内科影像室', 'AVAILABLE', '2023-03-15'),
('DEV-CT-001', '64排螺旋CT', '影像设备', 1, '影像中心CT室', 'IN_USE', '2022-08-20'),
('DEV-MRI-001', '1.5T核磁共振', '影像设备', 1, '影像中心MRI室', 'AVAILABLE', '2023-01-10'),
('DEV-ENDO-001', '电子胃镜', '内窥镜', 5, '消化内镜室', 'IN_USE', '2023-06-01'),
('DEV-ENDO-002', '电子肠镜', '内窥镜', 5, '消化内镜室', 'AVAILABLE', '2023-06-01'),
('DEV-LAB-001', '全自动生化分析仪', '检验设备', 2, '检验科', 'IN_USE', '2022-11-15'),
('DEV-LAB-002', '血球计数仪', '检验设备', 2, '检验科', 'AVAILABLE', '2023-02-20'),
('DEV-ECG-003', '动态心电图机', '诊断设备', 1, '心内科诊室', 'IN_USE', '2023-09-01'),
('DEV-BP-002', '动态血压监测仪', '监测设备', 1, '心内科诊室', 'AVAILABLE', '2023-09-01'),
('DEV-SPI-002', '肺功能检测仪', '诊断设备', 3, '呼吸内科诊室', 'AVAILABLE', '2023-07-15'),
('DEV-US-002', '彩超诊断仪', '影像设备', 3, '呼吸内科超声室', 'IN_USE', '2023-04-10'),
('DEV-EEG-002', '视频脑电图机', '诊断设备', 4, '神经内科诊室', 'AVAILABLE', '2023-08-20')
ON CONFLICT (device_code) DO NOTHING;

-- ─── 13. 设备使用记录（8 → 24）───
INSERT INTO device_usage_record (device_id, usage_type, used_by, patient_id, started_at, ended_at, result_status, remark) VALUES
(7, 'DIAGNOSIS', '张医生', 14, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '30 minutes', 'NORMAL', '心电图检查正常'),
(7, 'DIAGNOSIS', '张医生', 15, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '25 minutes', 'ABNORMAL', 'ST段轻度压低'),
(7, 'DIAGNOSIS', '张医生', 17, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '20 minutes', 'ABNORMAL', 'ST段抬高，急性心梗表现'),
(8, 'DIAGNOSIS', '李医生', 20, CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '45 minutes', 'NORMAL', '胃镜检查示浅表性胃炎'),
(8, 'DIAGNOSIS', '李医生', 22, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '40 minutes', 'NORMAL', '胃镜检查未见明显异常'),
(9, 'EXAMINATION', '检验科', NULL, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '2 hours', 'NORMAL', '生化全套检查'),
(9, 'EXAMINATION', '检验科', 16, CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '2 hours', 'ABNORMAL', '空腹血糖7.8mmol/L偏高'),
(10, 'EXAMINATION', '检验科', NULL, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '1 hour', 'NORMAL', '血常规检查'),
(11, 'DIAGNOSIS', '张医生', 16, CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '15 minutes', 'NORMAL', '动态心电图未见明显心律失常'),
(12, 'MONITORING', '张医生', 27, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '24 hours', 'ABNORMAL', '夜间血压偏高'),
(13, 'DIAGNOSIS', '王医生', 26, CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '20 minutes', 'ABNORMAL', 'FEV1/FVC 62%，肺功能减退'),
(14, 'DIAGNOSIS', '王医生', 29, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '25 minutes', 'ABNORMAL', 'FEV1/FVC 58%，中度阻塞'),
(15, 'IMAGING', '影像科', 32, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '30 minutes', 'ABNORMAL', '双肺纹理增多'),
(16, 'DIAGNOSIS', '刘医生', 32, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '40 minutes', 'ABNORMAL', '右侧颞叶异常放电'),
(17, 'DIAGNOSIS', '张医生', 31, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '15 minutes', 'ABNORMAL', '偶发室性早搏'),
(18, 'MONITORING', '张医生', 38, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '24 hours', 'ABNORMAL', '血压波动大'),
(19, 'DIAGNOSIS', '王医生', 37, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '20 minutes', 'NORMAL', '肺功能正常'),
(20, 'IMAGING', '影像科', 21, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '30 minutes', 'NORMAL', '胸部X光未见异常'),
(21, 'EXAMINATION', '检验科', 25, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hour', 'ABNORMAL', 'HbA1c 8.2%，血糖控制不佳'),
(22, 'EXAMINATION', '检验科', 36, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '1.5 hours', 'ABNORMAL', '肌酐升高，肾功能减退'),
(7, 'DIAGNOSIS', '周医生', 18, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '20 minutes', 'NORMAL', '心电图正常'),
(8, 'DIAGNOSIS', '陈医生', 33, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '35 minutes', 'NORMAL', '胃镜示慢性浅表性胃炎好转'),
(9, 'EXAMINATION', '检验科', 39, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '2 hours', 'ABNORMAL', '血尿酸520μmol/L偏高'),
(11, 'DIAGNOSIS', '刘医生', 35, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '15 minutes', 'ABNORMAL', '右侧大脑中动脉狭窄');
