# 接口设计文档 API

## 1. 通用约定

### 1.1 基础路径

```text
/api
```

### 1.2 认证方式

登录成功后，前端在请求头中携带：

```http
Authorization: Bearer <jwt-token>
```

### 1.3 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 2. 错误码

| code | 含义 |
|---|---|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 Token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 数据冲突 |
| 500 | 系统异常 |
| 600 | AI 服务不可用 |

## 3. 接口列表

### 3.1 对前端开放的业务接口

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/patient/register` | POST | 游客 | 患者注册 |
| `/api/patient/login` | POST | 游客 | 患者登录 |
| `/api/doctor/login` | POST | 游客 | 医生登录 |
| `/api/patient/info` | GET | 患者 | 获取患者信息 |
| `/api/doctor/list` | GET | 登录用户 | 医生列表 |
| `/api/doctor/detail` | GET | 登录用户 | 医生详情 |
| `/api/triage/consult` | POST | 患者 | 智能分诊 |
| `/api/registration/create` | POST | 患者 | 创建挂号 |
| `/api/registration/list` | GET | 患者/医生 | 挂号列表 |
| `/api/registration/cancel` | POST | 患者 | 取消挂号 |
| `/api/medical-record/generate` | POST | 医生 | AI 生成病历 |
| `/api/medical-record/save` | POST | 医生 | 保存病历 |
| `/api/medical-record/list` | GET | 患者/医生 | 病历列表 |
| `/api/medical-record/detail` | GET | 患者/医生 | 病历详情 |
| `/api/prescription/check` | POST | 医生 | AI 处方审核 |
| `/api/prescription/create` | POST | 医生 | 保存处方 |
| `/api/prescription/list` | GET | 患者/医生 | 处方列表 |
| `/api/notification/list` | GET | 医生 | 医生通知列表 |
| `/api/notification/read` | POST | 医生 | 标记通知已读 |
| `/api/prompt-template/list` | GET | 医生/管理员 | Prompt 模板列表或配置展示 |
| `/ws/notifications` | WS | 医生 | WebSocket 实时通知连接 |
| `/api/admin/login` | POST | 游客 | 管理员登录 |
| `/api/admin/department/list` | GET | 管理员 | 科室列表 |
| `/api/admin/department/save` | POST | 管理员 | 新增或编辑科室 |
| `/api/admin/doctor/list` | GET | 管理员 | 医生列表 |
| `/api/admin/doctor/save` | POST | 管理员 | 新增或编辑医生 |
| `/api/admin/drug/list` | GET | 管理员 | 药品列表 |
| `/api/admin/drug/save` | POST | 管理员 | 新增或编辑药品 |
| `/api/admin/prompt-template/list` | GET | 管理员 | Prompt 模板列表 |
| `/api/admin/prompt-template/save` | POST | 管理员 | 新增或编辑 Prompt 模板 |
| `/api/admin/dict/list` | GET | 管理员 | 系统字典列表 |
| `/api/admin/dict/save` | POST | 管理员 | 新增或编辑系统字典 |
| `/api/admin/schedule/generate` | POST | 管理员 | AI 生成排班建议 |
| `/api/admin/schedule/publish` | POST | 管理员 | 人工确认后发布排班和号源 |
| `/api/admin/schedule/list` | GET | 管理员 | 排班和号源列表 |
| `/api/admin/schedule/suggestion/detail` | GET | 管理员 | AI 排班建议详情 |
| `/api/admin/triage-desk/list` | GET | 管理员 | AI 分诊台列表 |
| `/api/admin/triage-desk/detail` | GET | 管理员 | AI 分诊台详情 |
| `/api/admin/triage-desk/assign` | POST | 管理员 | 人工改派分诊科室或医生 |
| `/api/admin/triage-desk/close` | POST | 管理员 | 关闭分诊台处理记录 |

### 3.2 服务间内部 AI 接口

内部接口只允许微服务之间通过内网和 OpenFeign 调用，不直接暴露给前端。前端统一访问 `gateway-service`，由网关转发到对应业务服务。

| 接口 | 方法 | 调用方 | 说明 |
|---|---|---|---|
| `/internal/auth/verify` | POST | `gateway-service`、业务服务 | Token 校验和身份解析 |
| `/internal/patients/{id}` | GET | 挂号、病历、处方服务 | 患者基础信息 |
| `/internal/doctors/{id}` | GET | 挂号、分诊服务 | 医生详情、科室、排班 |
| `/internal/registrations/{id}` | GET | 病历、处方服务 | 挂号关系校验 |
| `/internal/ai/triage` | POST | `triage-service` | 智能分诊 |
| `/internal/ai/medical-record/generate` | POST | `medical-record-service` | AI 生成病历 |
| `/internal/ai/medical-record/generate/stream` | GET | `medical-record-service` | AI 流式生成病历 |
| `/internal/ai/prescription/check` | POST | `prescription-service` | AI 处方审核 |
| `/internal/ai/schedule/generate` | POST | `admin-service` | AI 生成排班建议 |
| `/internal/ai/prompt-template/resolve` | POST | `ai-service` 内部 | 按任务类型和科室解析 Prompt 模板 |
| `/internal/notifications` | POST | `prescription-service` | 创建并推送通知 |
| `/internal/triage-records` | GET | `admin-service` | 查询分诊记录 |
| `/internal/triage-records/{id}/assign` | POST | `admin-service` | 更新分诊人工改派结果 |
| `/internal/doctors/schedules/publish` | POST | `admin-service` | 发布医生排班和号源 |

## 4. 接口详情

### 4.1 患者注册

```http
POST /api/patient/register
Content-Type: application/json
```

请求：

```json
{
  "name": "王小明",
  "phone": "13800000000",
  "password": "123456",
  "gender": "男",
  "age": 35,
  "allergyHistory": "无",
  "pastHistory": "无明显既往史"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "patientId": 1
  }
}
```

### 4.2 患者登录

```http
POST /api/patient/login
Content-Type: application/json
```

请求：

```json
{
  "phone": "13800000000",
  "password": "123456"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "role": "PATIENT",
    "name": "王小明"
  }
}
```

### 4.3 医生列表

```http
GET /api/doctor/list?departmentId=1
Authorization: Bearer jwt-token
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "张医生",
      "departmentName": "心内科",
      "title": "主任医师",
      "specialty": "胸痛、心悸、高血压"
    }
  ]
}
```

### 4.4 智能分诊

```http
POST /api/triage/consult
Authorization: Bearer jwt-token
Content-Type: application/json
```

请求：

```json
{
  "chiefComplaint": "胸痛伴气短，活动后加重"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "triageRecordId": 10,
    "recommendedDepartment": "心内科",
    "recommendedDoctors": [
      {
        "id": 1,
        "name": "张医生",
        "title": "主任医师",
        "specialty": "胸痛、心悸、高血压"
      }
    ],
    "reason": "症状与心血管疾病相关，建议优先心内科就诊"
  }
}
```

业务接口处理链路：

```text
前端 -> gateway-service -> triage-service /api/triage/consult -> ai-service /internal/ai/triage
```

### 4.5 创建挂号

```http
POST /api/registration/create
Authorization: Bearer jwt-token
Content-Type: application/json
```

请求：

```json
{
  "doctorId": 1,
  "departmentId": 1,
  "appointmentTime": "2026-06-15 09:30:00",
  "triageRecordId": 10
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "registrationId": 100,
    "status": "CREATED"
  }
}
```

### 4.6 AI 生成病历

```http
POST /api/medical-record/generate
Authorization: Bearer jwt-token
Content-Type: application/json
```

请求：

```json
{
  "registrationId": 100,
  "dialogueText": "患者：胸痛伴气短两天。医生：是否活动后加重？患者：是。"
}
```

业务接口处理链路：

```text
前端 -> gateway-service -> medical-record-service /api/medical-record/generate -> ai-service /internal/ai/medical-record/generate
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chiefComplaint": "胸痛伴气短两天",
    "presentIllness": "患者两天前出现胸痛伴气短，活动后加重。",
    "pastHistory": "未提供明确既往史",
    "physicalExam": "待补充",
    "diagnosis": "胸痛待查",
    "treatmentAdvice": "建议完善心电图、心肌酶等检查"
  }
}
```

### 4.6.1 AI 流式生成病历

```http
GET /api/medical-record/generate/stream?registrationId=100
Authorization: Bearer jwt-token
Accept: text/event-stream
```

SSE 事件：

```text
event: start
data: {"taskId":"mr-100"}

event: delta
data: {"text":"主诉：胸痛伴气短两天"}

event: structured
data: {"chiefComplaint":"胸痛伴气短两天","diagnosis":"胸痛待查"}

event: done
data: {"taskId":"mr-100"}
```

异常事件：

```text
event: error
data: {"message":"AI 生成中断，可重试或手动填写"}
```

说明：该接口 `Accept` 和响应类型均为 `text/event-stream`，只返回 SSE 事件协议；普通 JSON 病历草稿响应属于非流式 `/api/medical-record/generate`。

### 4.7 AI 处方审核

```http
POST /api/prescription/check
Authorization: Bearer jwt-token
Content-Type: application/json
```

请求：

```json
{
  "patientId": 1,
  "drugs": [
    {
      "drugName": "阿司匹林",
      "dosage": "100mg",
      "frequency": "每日一次",
      "usageMethod": "口服"
    }
  ]
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "riskLevel": "LOW",
    "suggestions": "注意胃肠道不良反应，建议饭后服用。",
    "interactions": []
  }
}
```

业务接口处理链路：

```text
前端 -> gateway-service -> prescription-service /api/prescription/check -> ai-service /internal/ai/prescription/check
```

### 4.8 管理端 AI 排班

```http
POST /api/admin/schedule/generate
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

请求：

```json
{
  "departmentId": 1,
  "dateRangeStart": "2026-06-15",
  "dateRangeEnd": "2026-06-21",
  "doctorIds": [1, 2, 3],
  "slotRule": {
    "shiftTypes": ["AM", "PM"],
    "capacityPerSlot": 20
  }
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "suggestionId": 9001,
    "status": "GENERATED",
    "items": [
      {
        "doctorId": 1,
        "scheduleDate": "2026-06-15",
        "shiftType": "AM",
        "capacity": 20
      }
    ]
  }
}
```

发布：

```http
POST /api/admin/schedule/publish
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

```json
{
  "suggestionId": 9001,
  "items": [
    {
      "doctorId": 1,
      "scheduleDate": "2026-06-15",
      "shiftType": "AM",
      "capacity": 20
    }
  ]
}
```

业务接口处理链路：

```text
前端 -> gateway-service -> admin-service /api/admin/schedule/generate -> ai-service /internal/ai/schedule/generate
前端 -> gateway-service -> admin-service /api/admin/schedule/publish -> doctor-service /internal/doctors/schedules/publish
```

### 4.9 管理端 AI 分诊台

列表：

```http
GET /api/admin/triage-desk/list?status=SUCCESS&page=1&pageSize=10
Authorization: Bearer admin-jwt-token
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1,
    "records": [
      {
        "triageRecordId": 10,
        "patientId": 1,
        "chiefComplaint": "胸痛伴气短",
        "recommendedDepartment": "心内科",
        "recommendedDoctorIds": [1],
        "reason": "症状与心血管疾病相关",
        "status": "SUCCESS"
      }
    ]
  }
}
```

人工改派：

```http
POST /api/admin/triage-desk/assign
Authorization: Bearer admin-jwt-token
Content-Type: application/json
```

```json
{
  "triageRecordId": 10,
  "departmentId": 2,
  "doctorId": 8,
  "note": "管理员根据问诊描述改派到呼吸内科"
}
```

业务接口处理链路：

```text
前端 -> gateway-service -> admin-service /api/admin/triage-desk/list -> triage-service /internal/triage-records
前端 -> gateway-service -> admin-service /api/admin/triage-desk/assign -> triage-service /internal/triage-records/{id}/assign
```

### 4.10 WebSocket 实时通知

```text
ws://localhost:8080/ws/notifications?token=<jwt-token>
```

服务端推送消息：

```json
{
  "type": "PRESCRIPTION_HIGH_RISK",
  "notificationId": 300,
  "doctorId": 1,
  "patientId": 1,
  "prescriptionId": 200,
  "riskLevel": "HIGH",
  "title": "高风险用药提醒",
  "content": "检测到潜在药物相互作用，请复核处方。",
  "createdAt": "2026-06-14 16:00:00"
}
```

触发规则：

1. 医生调用 `/api/prescription/check`。
2. AI 审核结果为 `HIGH`。
3. 后端保存 `notification_message`。
4. 后端通过 WebSocket 推送给处方所属医生。

### 4.11 医生通知列表

```http
GET /api/notification/list?readStatus=UNREAD
Authorization: Bearer jwt-token
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 300,
      "type": "PRESCRIPTION_HIGH_RISK",
      "title": "高风险用药提醒",
      "content": "检测到潜在药物相互作用，请复核处方。",
      "riskLevel": "HIGH",
      "readStatus": "UNREAD",
      "createdAt": "2026-06-14 16:00:00"
    }
  ]
}
```

### 4.12 Prompt 模板列表

```http
GET /api/prompt-template/list?taskType=MEDICAL_RECORD&departmentCode=CARDIOLOGY
Authorization: Bearer jwt-token
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "taskType": "MEDICAL_RECORD",
      "departmentCode": "CARDIOLOGY",
      "templateName": "心内科病历生成模板",
      "version": "v1",
      "enabled": true
    }
  ]
}
```

## 5. KingbaseES 搜索接口补充

| API | Method | Role | Description |
|---|---|---|---|
| `/api/search/knowledge?q=&departmentCode=` | GET | 登录用户 | 从 KingbaseES 搜索知识库 |
| `/api/search/drugs?q=` | GET | 登录用户 | 从 KingbaseES 搜索药品 |
| `/api/admin/search/prompts?q=` | GET | 管理员 | 从 KingbaseES 搜索 Prompt 模板 |
