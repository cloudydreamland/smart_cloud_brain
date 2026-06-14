# 数据库设计文档

## 1. 数据库选型

MVP 推荐使用 MySQL 8。字段命名采用小写下划线，时间字段统一使用 `DATETIME`，主键统一使用 `BIGINT AUTO_INCREMENT`。

数据库按纯微服务数据归属设计：每个服务拥有独立数据库或独立 schema，同一服务内部可以建立数据库外键；跨服务只保存业务 ID 作为逻辑引用，禁止跨 schema 直接建外键或连表查询。需要其他服务数据时，通过内部接口或 OpenFeign 查询。

## 2. 表设计

### 2.1 `account_credential` 账号凭证表（`auth_db`）

由 `auth-service` 维护，统一承载患者、医生、管理员的登录凭证和角色信息。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 账号凭证 ID |
| login_name | VARCHAR(50) | NOT NULL, UNIQUE | 登录名，可为手机号或管理员用户名 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希 |
| role | VARCHAR(20) | NOT NULL | PATIENT/DOCTOR/ADMIN |
| subject_id | BIGINT | NOT NULL | 对应业务主体 ID，按角色指向患者、医生或管理员资料 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.2 `patient` 患者表（`patient_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 患者 ID |
| auth_account_id | BIGINT | 逻辑引用 | 对应 `auth_db.account_credential.id` |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| phone | VARCHAR(20) | NOT NULL, UNIQUE | 手机号 |
| gender | VARCHAR(10) | NULL | 性别 |
| age | INT | NULL | 年龄 |
| allergy_history | VARCHAR(500) | NULL | 过敏史 |
| past_history | VARCHAR(1000) | NULL | 既往史 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.3 `department` 科室表（`doctor_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 科室 ID |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 科室名称 |
| description | VARCHAR(500) | NULL | 科室描述 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.4 `doctor` 医生表（`doctor_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 医生 ID |
| auth_account_id | BIGINT | 逻辑引用 | 对应 `auth_db.account_credential.id` |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| phone | VARCHAR(20) | UNIQUE | 联系手机号 |
| department_id | BIGINT | FK（服务内） | 科室 ID |
| title | VARCHAR(50) | NULL | 职称 |
| specialty | VARCHAR(500) | NULL | 擅长方向 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.5 `registration` 挂号表（`registration_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 挂号 ID |
| patient_id | BIGINT | 逻辑引用, NOT NULL | 患者 ID |
| doctor_id | BIGINT | 逻辑引用, NOT NULL | 医生 ID |
| department_id | BIGINT | 逻辑引用, NOT NULL | 科室 ID |
| triage_record_id | BIGINT | 逻辑引用, NULL | 分诊记录 ID |
| appointment_time | DATETIME | NOT NULL | 预约时间 |
| status | VARCHAR(20) | NOT NULL | CREATED/CANCELLED/FINISHED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.6 `doctor_schedule` 医生排班表（`doctor_db`）

由 `doctor-service` 维护，用于记录医生在指定日期和班次的出诊安排。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 排班 ID |
| doctor_id | BIGINT | FK（服务内）, NOT NULL | 医生 ID |
| department_id | BIGINT | FK（服务内）, NOT NULL | 科室 ID |
| schedule_date | DATE | NOT NULL | 排班日期 |
| shift_type | VARCHAR(20) | NOT NULL | AM/PM/NIGHT |
| status | VARCHAR(20) | NOT NULL | DRAFT/PUBLISHED/CANCELLED |
| source_type | VARCHAR(20) | NOT NULL | MANUAL/AI |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.7 `appointment_slot` 号源表（`doctor_db`）

由 `doctor-service` 维护，用于支撑患者按可预约时间段挂号。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 号源 ID |
| schedule_id | BIGINT | FK（服务内）, NOT NULL | 排班 ID |
| doctor_id | BIGINT | FK（服务内）, NOT NULL | 医生 ID |
| department_id | BIGINT | FK（服务内）, NOT NULL | 科室 ID |
| start_time | DATETIME | NOT NULL | 开始时间 |
| end_time | DATETIME | NOT NULL | 结束时间 |
| capacity | INT | NOT NULL | 号源容量 |
| remaining | INT | NOT NULL | 剩余号源 |
| status | VARCHAR(20) | NOT NULL | AVAILABLE/FULL/CLOSED |

### 2.8 `triage_record` 分诊记录表（`triage_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 分诊记录 ID |
| patient_id | BIGINT | 逻辑引用, NOT NULL | 患者 ID |
| chief_complaint | TEXT | NOT NULL | 主诉 |
| recommended_department | VARCHAR(100) | NULL | 推荐科室 |
| recommended_doctor_ids | VARCHAR(255) | NULL | 推荐医生 ID 列表 |
| reason | TEXT | NULL | 推荐原因 |
| assigned_department_id | BIGINT | 逻辑引用, NULL | 管理端人工改派科室 ID |
| assigned_doctor_id | BIGINT | 逻辑引用, NULL | 管理端人工改派医生 ID |
| ai_result_json | JSON | NULL | AI 原始结果 |
| status | VARCHAR(20) | NOT NULL | SUCCESS/FAILED/ASSIGNED/CLOSED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.9 `triage_desk_record` 分诊台处理记录表（`triage_db`）

用于记录管理端对 AI 分诊结果的查看、改派和关闭操作。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 分诊台记录 ID |
| triage_record_id | BIGINT | FK（服务内）, NOT NULL | 分诊记录 ID |
| operator_admin_id | BIGINT | 逻辑引用, NOT NULL | 操作管理员 ID |
| action_type | VARCHAR(30) | NOT NULL | VIEW/ASSIGN/CLOSE |
| from_department_id | BIGINT | 逻辑引用, NULL | 原推荐科室 ID |
| to_department_id | BIGINT | 逻辑引用, NULL | 改派科室 ID |
| from_doctor_id | BIGINT | 逻辑引用, NULL | 原推荐医生 ID |
| to_doctor_id | BIGINT | 逻辑引用, NULL | 改派医生 ID |
| note | VARCHAR(500) | NULL | 处理说明 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.10 `medical_record` 病历表（`medical_record_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 病历 ID |
| patient_id | BIGINT | 逻辑引用, NOT NULL | 患者 ID |
| doctor_id | BIGINT | 逻辑引用, NOT NULL | 医生 ID |
| registration_id | BIGINT | 逻辑引用, NOT NULL | 挂号 ID |
| chief_complaint | TEXT | NOT NULL | 主诉 |
| present_illness | TEXT | NULL | 现病史 |
| past_history | TEXT | NULL | 既往史 |
| physical_exam | TEXT | NULL | 体格检查 |
| diagnosis | TEXT | NOT NULL | 初步诊断 |
| treatment_advice | TEXT | NULL | 治疗意见 |
| ai_generated | BOOLEAN | NOT NULL | 是否由 AI 生成 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.11 `prescription` 处方表（`prescription_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 处方 ID |
| patient_id | BIGINT | 逻辑引用, NOT NULL | 患者 ID |
| doctor_id | BIGINT | 逻辑引用, NOT NULL | 医生 ID |
| medical_record_id | BIGINT | 逻辑引用, NULL | 病历 ID |
| risk_level | VARCHAR(20) | NULL | LOW/MEDIUM/HIGH |
| status | VARCHAR(20) | NOT NULL | DRAFT/SAVED |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.12 `prescription_item` 处方明细表（`prescription_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 明细 ID |
| prescription_id | BIGINT | FK（服务内）, NOT NULL | 处方 ID |
| drug_name | VARCHAR(100) | NOT NULL | 药品名称 |
| dosage | VARCHAR(100) | NOT NULL | 剂量 |
| frequency | VARCHAR(100) | NOT NULL | 频次 |
| usage_method | VARCHAR(200) | NOT NULL | 用法 |

### 2.13 `prescription_check_record` 处方审核记录表（`prescription_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 审核记录 ID |
| prescription_id | BIGINT | FK（服务内）, NULL | 处方 ID |
| patient_id | BIGINT | 逻辑引用, NOT NULL | 患者 ID |
| doctor_id | BIGINT | 逻辑引用, NOT NULL | 医生 ID |
| risk_level | VARCHAR(20) | NOT NULL | 风险等级 |
| suggestions | TEXT | NULL | 用药建议 |
| interactions | TEXT | NULL | 药物相互作用 |
| ai_result_json | JSON | NULL | AI 原始结果 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.14 `notification_message` 实时通知表（`notification_db`）

用于记录 WebSocket 高风险用药告警，保证医生端刷新后仍可查看历史通知。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 通知 ID |
| doctor_id | BIGINT | 逻辑引用, NOT NULL | 接收医生 ID |
| patient_id | BIGINT | 逻辑引用, NULL | 关联患者 ID |
| prescription_id | BIGINT | 逻辑引用, NULL | 关联处方 ID |
| type | VARCHAR(50) | NOT NULL | 通知类型，如 PRESCRIPTION_HIGH_RISK |
| title | VARCHAR(100) | NOT NULL | 通知标题 |
| content | TEXT | NOT NULL | 通知内容 |
| risk_level | VARCHAR(20) | NULL | 风险等级 |
| read_status | VARCHAR(20) | NOT NULL | UNREAD/READ |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.15 `prompt_template` Prompt 模板表（`ai_db`）

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

### 2.16 `ai_schedule_suggestion` AI 排班建议表（`ai_db`）

用于记录 AI 生成的排班建议、原始返回和人工确认状态。发布时由 `admin-service` 调用 `doctor-service` 写入正式排班和号源。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 排班建议 ID |
| department_id | BIGINT | 逻辑引用, NOT NULL | 科室 ID |
| date_range_start | DATE | NOT NULL | 起始日期 |
| date_range_end | DATE | NOT NULL | 结束日期 |
| request_rule_json | JSON | NULL | 号源规则和约束 |
| suggestion_json | JSON | NOT NULL | AI 排班建议 |
| raw_result_json | JSON | NULL | AI 原始返回 |
| status | VARCHAR(20) | NOT NULL | GENERATED/CONFIRMED/PUBLISHED/REJECTED |
| confirmed_by | BIGINT | 逻辑引用, NULL | 确认管理员 ID |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.17 `ai_generation_log` AI 生成日志表（`ai_db`）

用于记录分诊、病历生成、处方审核等 AI 调用，便于联调、追踪和答辩说明。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 日志 ID |
| task_type | VARCHAR(50) | NOT NULL | AI 任务类型 |
| biz_id | BIGINT | NULL | 关联业务 ID |
| patient_id | BIGINT | NULL | 患者 ID |
| doctor_id | BIGINT | NULL | 医生 ID |
| prompt_template_id | BIGINT | FK（服务内）, NULL | 使用的 Prompt 模板 ID |
| request_summary | TEXT | NULL | 请求摘要，避免保存完整敏感信息 |
| response_summary | TEXT | NULL | 响应摘要 |
| raw_result_json | JSON | NULL | AI 原始结果 |
| status | VARCHAR(20) | NOT NULL | SUCCESS/FAILED/DEGRADED |
| error_message | TEXT | NULL | 错误信息 |
| duration_ms | INT | NULL | 调用耗时 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.18 `admin_user` 管理员表（`admin_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 管理员 ID |
| auth_account_id | BIGINT | 逻辑引用 | 对应 `auth_db.account_credential.id` |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录账号 |
| name | VARCHAR(50) | NOT NULL | 管理员姓名 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.19 `admin_operation_log` 管理端操作日志表（`admin_db`）

用于记录管理员新增、修改、启停、AI 排班发布、分诊改派等操作。

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 操作日志 ID |
| admin_user_id | BIGINT | FK（服务内）, NOT NULL | 管理员 ID |
| operation_type | VARCHAR(50) | NOT NULL | 操作类型 |
| target_type | VARCHAR(50) | NOT NULL | 操作对象类型 |
| target_id | BIGINT | NULL | 操作对象 ID |
| detail | TEXT | NULL | 操作详情摘要 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 2.20 `system_dict` 系统字典表（`admin_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 字典 ID |
| dict_type | VARCHAR(50) | NOT NULL | 字典类型 |
| dict_code | VARCHAR(50) | NOT NULL | 字典编码 |
| dict_label | VARCHAR(100) | NOT NULL | 显示名称 |
| sort_order | INT | NOT NULL | 排序 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2.21 `drug` 药品基础表（`prescription_db`）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 药品 ID |
| name | VARCHAR(100) | NOT NULL | 药品名称 |
| specification | VARCHAR(100) | NULL | 规格 |
| contraindication | TEXT | NULL | 禁忌说明 |
| interaction_rule | TEXT | NULL | 相互作用说明 |
| status | VARCHAR(20) | NOT NULL | ENABLED/DISABLED |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

## 3. 主键 / 关联字段

纯微服务模式下，数据库外键只允许出现在同一服务 schema 内。下表中的“逻辑引用”只保存 ID，不建立数据库外键；调用方需要详情时通过归属服务接口查询。

| 表 | 主键 | 服务内外键 | 跨服务逻辑引用 |
|---|---|---|---|
| account_credential | id | 无 | subject_id |
| patient | id | 无 | auth_account_id |
| department | id | 无 | 无 |
| doctor | id | department_id -> department.id | auth_account_id |
| registration | id | 无 | patient_id、doctor_id、department_id、triage_record_id |
| doctor_schedule | id | doctor_id -> doctor.id、department_id -> department.id | 无 |
| appointment_slot | id | schedule_id -> doctor_schedule.id、doctor_id -> doctor.id、department_id -> department.id | 无 |
| triage_record | id | 无 | patient_id、assigned_department_id、assigned_doctor_id |
| triage_desk_record | id | triage_record_id -> triage_record.id | operator_admin_id、to_department_id、to_doctor_id |
| medical_record | id | 无 | patient_id、doctor_id、registration_id |
| prescription | id | 无 | patient_id、doctor_id、medical_record_id |
| prescription_item | id | prescription_id -> prescription.id | 无 |
| prescription_check_record | id | prescription_id -> prescription.id | patient_id、doctor_id |
| notification_message | id | 无 | doctor_id、patient_id、prescription_id |
| prompt_template | id | 无 | 无 |
| ai_schedule_suggestion | id | 无 | department_id、confirmed_by |
| ai_generation_log | id | prompt_template_id -> prompt_template.id | patient_id、doctor_id、biz_id |
| admin_user | id | 无 | auth_account_id |
| admin_operation_log | id | admin_user_id -> admin_user.id | 无 |
| system_dict | id | 无 | 无 |
| drug | id | 无 | 无 |

## 4. 索引建议

| 表 | 索引 |
|---|---|
| account_credential | `uk_auth_login_name(login_name)`、`idx_auth_role_subject(role, subject_id)` |
| patient | `uk_patient_phone(phone)` |
| doctor | `idx_doctor_department(department_id)`、`uk_doctor_phone(phone)` |
| doctor_schedule | `idx_schedule_doctor_date(doctor_id, schedule_date)`、`idx_schedule_department_date(department_id, schedule_date)` |
| appointment_slot | `idx_slot_doctor_time(doctor_id, start_time)`、`idx_slot_status(status)` |
| registration | `idx_registration_patient(patient_id, status)`、`idx_registration_doctor_time(doctor_id, appointment_time)` |
| triage_record | `idx_triage_patient_time(patient_id, created_at)`、`idx_triage_status(status)` |
| triage_desk_record | `idx_triage_desk_record(triage_record_id)`、`idx_triage_desk_operator(operator_admin_id)` |
| medical_record | `idx_record_patient(patient_id)`、`idx_record_doctor(doctor_id)` |
| prescription | `idx_prescription_patient(patient_id)`、`idx_prescription_doctor(doctor_id)` |
| notification_message | `idx_notification_doctor_status(doctor_id, read_status)` |
| prompt_template | `idx_prompt_task_department(task_type, department_code, enabled)` |
| ai_schedule_suggestion | `idx_ai_schedule_department_date(department_id, date_range_start, date_range_end)`、`idx_ai_schedule_status(status)` |
| ai_generation_log | `idx_ai_log_task_time(task_type, created_at)`、`idx_ai_log_patient(patient_id)` |
| admin_user | `uk_admin_username(username)` |
| admin_operation_log | `idx_admin_log_user_time(admin_user_id, created_at)` |
| system_dict | `idx_dict_type_code(dict_type, dict_code)` |
| drug | `idx_drug_name(name)`、`idx_drug_status(status)` |

## 5. 表关系说明

- 一个科室包含多个医生。
- 一个医生可以拥有多条排班，一条排班可以生成多个号源。
- 一个患者可以通过逻辑 ID 关联多条挂号记录。
- 一个医生可以通过逻辑 ID 关联多条挂号记录。
- 一条挂号记录可以通过逻辑 ID 关联一个分诊记录。
- 一条挂号记录可以通过逻辑 ID 生成一份病历。
- 一份病历可以通过逻辑 ID 关联多张处方。
- 一张处方包含多条药品明细。
- 一张处方可以有一条或多条 AI 审核记录。
- 高风险处方审核可以通过逻辑 ID 生成一条或多条医生端通知消息。
- 一个 AI 调用日志可以在服务内关联 Prompt 模板，并通过逻辑 ID 关联患者、医生和具体业务记录。
- 一个 AI 排班建议在人工确认后，由 `doctor-service` 转化为正式排班和号源。
- 一个分诊记录可以关联多条分诊台处理记录，用于记录管理员查看、改派和关闭动作。

## 5.1 管理端数据边界说明

`admin-service` 只维护管理员资料、系统字典、操作日志等管理端配置数据。科室、医生、排班和号源归属 `doctor-service`；分诊记录和分诊台处理归属 `triage-service`；Prompt、AI 调用日志和 AI 排班建议归属 `ai-service`。管理端不得跨 schema 直接 join 或直接写入其他服务数据库，必须通过内部接口或 OpenFeign 编排归属服务。

## 6. 示例数据

```sql
INSERT INTO department (id, name, description, created_at)
VALUES (1, '心内科', '心血管疾病诊疗科室', NOW());

INSERT INTO account_credential (id, login_name, password_hash, role, subject_id, status, created_at, updated_at)
VALUES (1, '13900000001', '$2a$mock', 'DOCTOR', 1, 'ENABLED', NOW(), NOW());

INSERT INTO doctor (id, auth_account_id, name, phone, department_id, title, specialty, status, created_at)
VALUES (1, 1, '张医生', '13900000001', 1, '主任医师', '胸痛、心悸、高血压', 'ENABLED', NOW());

INSERT INTO account_credential (id, login_name, password_hash, role, subject_id, status, created_at, updated_at)
VALUES (2, '13800000000', '$2a$mock', 'PATIENT', 1, 'ENABLED', NOW(), NOW());

INSERT INTO patient (id, auth_account_id, name, phone, gender, age, allergy_history, past_history, created_at, updated_at)
VALUES (1, 2, '王小明', '13800000000', '男', 35, '无', '无明显既往史', NOW(), NOW());
```
