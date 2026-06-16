# 接口设计文档 API

## 1. 通用约定

基础路径：

```text
/api
```

认证方式：

```http
Authorization: Bearer <jwt-token>
```

统一响应建议：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

## 2. 认证接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 患者、医生、管理员登录 |
| POST | `/api/auth/register` | 患者注册 |
| GET | `/api/auth/me` | 当前登录用户信息 |

## 3. 患者与医生接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/patients/me` | 患者本人信息 |
| GET | `/api/doctors` | 医生列表 |
| GET | `/api/doctors/{id}` | 医生详情 |
| GET | `/api/departments` | 科室列表 |

## 4. 分诊接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/triage/consult` | 提交主诉并获取 AI 分诊建议 |
| GET | `/api/triage/records` | 患者分诊记录 |
| GET | `/api/admin/triage-desk/list` | 管理端分诊工作台 |
| POST | `/api/admin/triage-desk/assign` | 管理端分配医生 |

## 5. 挂号接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/registration/slots` | 可预约号源 |
| POST | `/api/registration/create` | 创建挂号 |
| POST | `/api/registration/cancel` | 取消挂号 |
| GET | `/api/registration/my` | 患者挂号列表 |
| GET | `/api/registration/doctor/queue` | 医生待接诊队列 |

## 6. 病历接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/medical-record/generate` | AI 生成病历草稿 |
| POST | `/api/medical-record/save` | 医生保存正式病历 |
| GET | `/api/medical-record/list` | 病历列表 |
| GET | `/api/medical-record/{id}` | 病历详情 |

## 7. 处方接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/prescription/check` | AI 处方审核 |
| POST | `/api/prescription/create` | 保存处方 |
| GET | `/api/prescription/list` | 处方列表 |
| GET | `/api/prescription/{id}` | 处方详情 |

## 8. 管理端接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/admin/departments/**` | 科室维护 |
| GET/POST | `/api/admin/doctors/**` | 医生维护 |
| GET/POST | `/api/admin/drugs/**` | 药品维护 |
| GET/POST | `/api/admin/prompts/**` | Prompt 维护 |
| GET/POST | `/api/admin/knowledge/**` | 知识库维护 |
| GET/POST | `/api/admin/dict/**` | 字典维护 |
| POST | `/api/admin/schedule/generate` | AI 排班建议 |
| POST | `/api/admin/schedule/publish` | 发布排班和号源 |

## 9. 通知接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notifications` | 通知列表 |
| POST | `/api/notifications/{id}/read` | 标记已读 |
| WS | `/ws/notifications` | 医生端实时通知 |

## 10. AI 内部接口

AI 内部接口只允许后端服务调用，不由前端直接访问。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/internal/ai/triage` | 智能分诊 |
| POST | `/internal/ai/medical-record/generate` | 病历生成 |
| POST | `/internal/ai/prescription/check` | 处方审核 |
| POST | `/internal/ai/schedule/generate` | 排班建议 |

