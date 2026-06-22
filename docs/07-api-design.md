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
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 2. 认证接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/patient/login` | 患者登录 |
| POST | `/api/doctor/login` | 医生登录 |
| POST | `/api/admin/login` | 管理员登录 |
| POST | `/api/patient/register` | 患者注册 |
| GET | `/api/auth/me` | 当前登录用户信息 |

## 3. 患者与医生接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/patient/info` | 患者本人信息 |
| GET | `/api/doctor/list` | 医生列表 |
| GET | `/api/doctor/detail?id={id}` | 医生详情 |
| GET | `/api/doctor/department/list` | 科室列表 |

## 4. 分诊接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/triage/consult` | 提交主诉并获取 AI 分诊建议 |
| GET | `/api/triage/list` | 当前用户可见分诊记录 |
| GET | `/api/admin/triage-desk/list` | 管理端分诊工作台 |
| POST | `/api/admin/triage-desk/assign` | 管理端分配医生 |
| POST | `/api/admin/triage-desk/close` | 管理端关闭分诊记录 |

## 5. 挂号接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/registration/slots` | 可预约号源 |
| POST | `/api/registration/create` | 创建挂号 |
| POST | `/api/registration/cancel` | 取消挂号 |
| POST | `/api/registration/complete` | 医生完成接诊 |
| GET | `/api/registration/list` | 当前用户可见挂号列表；患者看本人，医生看本人队列，管理员看全部 |

## 6. 病历接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/medical-record/generate` | AI 生成病历草稿 |
| POST | `/api/medical-record/save` | 医生保存正式病历 |
| GET | `/api/medical-record/list` | 病历列表 |
| GET | `/api/medical-record/detail?id={id}` | 病历详情 |
| GET | `/api/medical-record/generate/stream` | SSE 生成病历草稿 |

## 7. 处方接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/prescription/check` | AI 处方审核 |
| POST | `/api/prescription/create` | 保存处方 |
| GET | `/api/prescription/list` | 处方列表 |
| GET | `/api/prescription/detail?id={id}` | 处方详情 |
| GET | `/api/prescription/{id}` | 处方详情兼容路径 |

## 8. 管理端接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/admin/department/**` | 科室维护 |
| POST | `/api/admin/doctor/save` | 医生维护 |
| GET/POST | `/api/admin/drug/**` | 药品维护 |
| GET/POST | `/api/admin/prompt-template/**` | Prompt 维护 |
| GET/POST | `/api/admin/knowledge/**` | 知识库维护 |
| GET/POST | `/api/admin/dict/**` | 字典维护 |
| POST | `/api/admin/schedule/generate` | 生成 AI 排班建议；响应包含 `source`、`degraded`，管理员确认后再发布 |
| POST | `/api/admin/schedule/publish` | 发布排班和号源 |
| GET | `/api/admin/schedule/list` | 已发布排班列表 |
| GET | `/api/admin/schedule/suggestion/detail?id={id}` | 排班建议详情 |
| GET | `/api/search/knowledge` | 知识库搜索 |
| GET | `/api/search/drugs` | 药品搜索 |
| GET | `/api/admin/search/prompts` | Prompt 搜索 |

## 9. 通知接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notification/list` | 医生通知列表 |
| POST | `/api/notification/read` | 标记已读，body 传 `notificationId` |
| WS | `/ws/notifications` | 医生端实时通知 |

## 10. AI 内部接口

AI 内部接口只允许后端服务调用，不由前端直接访问。所有 `/internal/**` 请求必须携带 `X-Internal-Token`，值与各服务的 `INTERNAL_SERVICE_TOKEN` 一致；缺失或错误时返回 401。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/internal/ai/triage` | 智能分诊 |
| POST | `/internal/ai/medical-record/generate` | 病历生成 |
| GET | `/internal/ai/medical-record/generate/stream` | 病历生成 SSE |
| POST | `/internal/ai/prescription/check` | 处方审核 |
| POST | `/internal/ai/schedule/suggest` | AI 排班建议，输入医生、科室、日期范围和已有排班 |
| POST | `/internal/ai/prompt-template/resolve` | Prompt 模板解析 |

排班结果会在 `admin-service` 中进行二次强校验：医生必须存在且启用、科室匹配、日期处于 1-14 天请求范围内、时间格式与先后顺序合法、容量为 1-100，且同一医生时段不得重复。Dify/OpenAI-compatible 调用异常时四类任务统一回退确定性 Mock，并返回 `degraded=true`。
