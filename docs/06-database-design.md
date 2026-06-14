# 数据库设计文档

## 1. 数据库选型

MVP 推荐使用 MySQL 8。字段命名采用小写下划线，时间字段统一使用 `DATETIME`，主键统一使用 `BIGINT AUTO_INCREMENT`。

## 2. 表设计

### 2.1 `patient` 患者表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 患者 ID |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| phone | VARCHAR(20) | NOT NULL, UNIQUE | 手机号 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希 |
| gender | VARCHAR(10) | NULL | 性别 |
| age | INT | NULL | 年龄 |
| allergy_history | VARCHAR(500) | NULL | 过敏史 |
| past_history | VARCHAR(1000) | NULL | 既往史 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.2 `department` 科室表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 科室 ID |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 科室名称 |
| description | VARCHAR(500) | NULL | 科室描述 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.3 `doctor` 医生表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 医生 ID |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| phone | VARCHAR(20) | UNIQUE | 登录手机号 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希 |
| department_id | BIGINT | FK | 科室 ID |
| title | VARCHAR(50) | NULL | 职称 |
| specialty | VARCHAR(500) | NULL | 擅长方向 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.4 `registration` 挂号表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 挂号 ID |
| patient_id | BIGINT | FK, NOT NULL | 患者 ID |
| doctor_id | BIGINT | FK, NOT NULL | 医生 ID |
| department_id | BIGINT | FK, NOT NULL | 科室 ID |
| triage_record_id | BIGINT | FK, NULL | 分诊记录 ID |
| appointment_time | DATETIME | NOT NULL | 预约时间 |
| status | VARCHAR(20) | NOT NULL | CREATED/CANCELLED/FINISHED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.5 `triage_record` 分诊记录表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 分诊记录 ID |
| patient_id | BIGINT | FK, NOT NULL | 患者 ID |
| chief_complaint | TEXT | NOT NULL | 主诉 |
| recommended_department | VARCHAR(100) | NULL | 推荐科室 |
| recommended_doctor_ids | VARCHAR(255) | NULL | 推荐医生 ID 列表 |
| reason | TEXT | NULL | 推荐原因 |
| ai_result_json | JSON | NULL | AI 原始结果 |
| status | VARCHAR(20) | NOT NULL | SUCCESS/FAILED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.6 `medical_record` 病历表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 病历 ID |
| patient_id | BIGINT | FK, NOT NULL | 患者 ID |
| doctor_id | BIGINT | FK, NOT NULL | 医生 ID |
| registration_id | BIGINT | FK, NOT NULL | 挂号 ID |
| chief_complaint | TEXT | NOT NULL | 主诉 |
| present_illness | TEXT | NULL | 现病史 |
| past_history | TEXT | NULL | 既往史 |
| physical_exam | TEXT | NULL | 体格检查 |
| diagnosis | TEXT | NOT NULL | 初步诊断 |
| treatment_advice | TEXT | NULL | 治疗意见 |
| ai_generated | BOOLEAN | NOT NULL | 是否由 AI 生成 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.7 `prescription` 处方表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 处方 ID |
| patient_id | BIGINT | FK, NOT NULL | 患者 ID |
| doctor_id | BIGINT | FK, NOT NULL | 医生 ID |
| medical_record_id | BIGINT | FK, NULL | 病历 ID |
| risk_level | VARCHAR(20) | NULL | LOW/MEDIUM/HIGH |
| status | VARCHAR(20) | NOT NULL | DRAFT/SAVED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.8 `prescription_item` 处方明细表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 明细 ID |
| prescription_id | BIGINT | FK, NOT NULL | 处方 ID |
| drug_name | VARCHAR(100) | NOT NULL | 药品名称 |
| dosage | VARCHAR(100) | NOT NULL | 剂量 |
| frequency | VARCHAR(100) | NOT NULL | 频次 |
| usage_method | VARCHAR(200) | NOT NULL | 用法 |

### 2.9 `prescription_check_record` 处方审核记录表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 审核记录 ID |
| prescription_id | BIGINT | FK, NULL | 处方 ID |
| patient_id | BIGINT | FK, NOT NULL | 患者 ID |
| doctor_id | BIGINT | FK, NOT NULL | 医生 ID |
| risk_level | VARCHAR(20) | NOT NULL | 风险等级 |
| suggestions | TEXT | NULL | 用药建议 |
| interactions | TEXT | NULL | 药物相互作用 |
| ai_result_json | JSON | NULL | AI 原始结果 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.10 `notification_message` 实时通知表

用于记录 WebSocket 高风险用药告警，保证医生端刷新后仍可查看历史通知。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 通知 ID |
| doctor_id | BIGINT | FK, NOT NULL | 接收医生 ID |
| patient_id | BIGINT | FK, NULL | 关联患者 ID |
| prescription_id | BIGINT | FK, NULL | 关联处方 ID |
| type | VARCHAR(50) | NOT NULL | 通知类型，如 PRESCRIPTION_HIGH_RISK |
| title | VARCHAR(100) | NOT NULL | 通知标题 |
| content | TEXT | NOT NULL | 通知内容 |
| risk_level | VARCHAR(20) | NULL | 风险等级 |
| read_status | VARCHAR(20) | NOT NULL | UNREAD/READ |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.11 `prompt_template` Prompt 模板表

用于支持后端 Service 中的可配置 Prompt 模板，按任务类型和科室优化 AI 输出。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 模板 ID |
| task_type | VARCHAR(50) | NOT NULL | TRIAGE/MEDICAL_RECORD/PRESCRIPTION_CHECK |
| department_code | VARCHAR(50) | NULL | 科室编码，通用模板可为空 |
| template_name | VARCHAR(100) | NOT NULL | 模板名称 |
| template_content | TEXT | NOT NULL | Prompt 模板内容 |
| output_schema | JSON | NULL | 期望输出 JSON 结构 |
| version | VARCHAR(20) | NOT NULL | 模板版本 |
| enabled | BOOLEAN | NOT NULL | 是否启用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.12 `ai_generation_log` AI 生成日志表

用于记录分诊、病历生成、处方审核等 AI 调用，便于联调、追踪和答辩说明。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 日志 ID |
| task_type | VARCHAR(50) | NOT NULL | AI 任务类型 |
| biz_id | BIGINT | NULL | 关联业务 ID |
| patient_id | BIGINT | NULL | 患者 ID |
| doctor_id | BIGINT | NULL | 医生 ID |
| prompt_template_id | BIGINT | FK, NULL | 使用的 Prompt 模板 ID |
| request_summary | TEXT | NULL | 请求摘要，避免保存完整敏感信息 |
| response_summary | TEXT | NULL | 响应摘要 |
| raw_result_json | JSON | NULL | AI 原始结果 |
| status | VARCHAR(20) | NOT NULL | SUCCESS/FAILED/DEGRADED |
| error_message | TEXT | NULL | 错误信息 |
| duration_ms | INT | NULL | 调用耗时 |
| created_at | DATETIME | NOT NULL | 创建时间 |

## 3. 主键 / 外键

| 表 | 主键 | 外键 |
|---|---|---|
| patient | id | 无 |
| department | id | 无 |
| doctor | id | department_id -> department.id |
| registration | id | patient_id、doctor_id、department_id、triage_record_id |
| triage_record | id | patient_id -> patient.id |
| medical_record | id | patient_id、doctor_id、registration_id |
| prescription | id | patient_id、doctor_id、medical_record_id |
| prescription_item | id | prescription_id -> prescription.id |
| prescription_check_record | id | prescription_id、patient_id、doctor_id |
| notification_message | id | doctor_id、patient_id、prescription_id |
| prompt_template | id | 无 |
| ai_generation_log | id | patient_id、doctor_id、prompt_template_id |

## 4. 索引建议

| 表 | 索引 |
|---|---|
| patient | `uk_patient_phone(phone)` |
| doctor | `idx_doctor_department(department_id)`、`uk_doctor_phone(phone)` |
| registration | `idx_registration_patient(patient_id, status)`、`idx_registration_doctor_time(doctor_id, appointment_time)` |
| triage_record | `idx_triage_patient_time(patient_id, created_at)` |
| medical_record | `idx_record_patient(patient_id)`、`idx_record_doctor(doctor_id)` |
| prescription | `idx_prescription_patient(patient_id)`、`idx_prescription_doctor(doctor_id)` |
| notification_message | `idx_notification_doctor_status(doctor_id, read_status)` |
| prompt_template | `idx_prompt_task_department(task_type, department_code, enabled)` |
| ai_generation_log | `idx_ai_log_task_time(task_type, created_at)`、`idx_ai_log_patient(patient_id)` |

## 5. 表关系说明

- 一个科室包含多个医生。
- 一个患者可以有多条挂号记录。
- 一个医生可以有多条挂号记录。
- 一条挂号记录可以关联一个分诊记录。
- 一条挂号记录可以生成一份病历。
- 一份病历可以关联多张处方。
- 一张处方包含多条药品明细。
- 一张处方可以有一条或多条 AI 审核记录。
- 高风险处方审核可以生成一条或多条医生端通知消息。
- 一个 AI 调用日志可以关联 Prompt 模板、患者、医生和具体业务记录。

## 6. 示例数据

```sql
INSERT INTO department (id, name, description, created_at)
VALUES (1, '心内科', '心血管疾病诊疗科室', NOW());

INSERT INTO doctor (id, name, phone, password_hash, department_id, title, specialty, status, created_at)
VALUES (1, '张医生', '13900000001', '$2a$mock', 1, '主任医师', '胸痛、心悸、高血压', 'ENABLED', NOW());

INSERT INTO patient (id, name, phone, password_hash, gender, age, allergy_history, past_history, created_at, updated_at)
VALUES (1, '王小明', '13800000000', '$2a$mock', '男', 35, '无', '无明显既往史', NOW(), NOW());
```
