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

