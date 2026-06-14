# 任务覆盖与必做任务方案

## 0. 口径说明

本文档结合 `东软智慧云脑诊疗平台实践任务描述(1).docx` 和用户补充图片要求整理。Word 文档中的项目初始化、传统业务、智能分诊、AI 处方审核、AI 病历生成、前后端联调、成果提交、答辩和非功能要求均作为基础必做内容；图片中的六项任务全部作为本项目必做项。其中第六项统一按纯微服务架构落地理解，项目从初始化起即按业务域服务推进。

统一口径声明：本项目采用“基础诊疗闭环 + FY26 企业项目实训方案 + 补充必做任务”的统一口径。后端从项目初始化起即采用 Spring Boot 3 + Spring Cloud 纯微服务架构，前端统一通过 `gateway-service` 访问业务能力；患者端、医生端、管理端均为必做入口。WebSocket 实时通知、AI 流式响应、Pinia 状态机、Prompt 工程、双端分离部署、纯微服务架构六项任务全部作为 P0 必做交付，不再作为可选扩展项处理。管理端除基础数据维护外，还需要覆盖 AI 医生排班和 AI 分诊台的最小可演示能力。若其他文档与本口径不一致，以 README 和本文档为准。

## 1. 原始任务覆盖矩阵

| 原始要求 | 文档覆盖 | 开发交付 | 优先级 |
|---|---|---|---|
| 项目概述、背景、目标用户 | `01-project-overview.md`、`02-requirements-analysis.md` | 项目说明、演示场景、用户角色 | P0 |
| 技术栈说明 | `04-technical-solution.md` | Spring Boot 3、Vue 3、TypeScript、MySQL、Knife4j、JWT | P0 |
| 学生参与说明 | `09-development-plan.md` | 后端、前端、AI 集成、联调任务拆分 | P0 |
| 项目初始化与环境搭建 | `04-technical-solution.md`、`09-development-plan.md`、`11-deployment-ops.md` | 前后端脚手架、统一响应、全局异常、CORS、Axios | P0 |
| 患者管理、医生管理、在线挂号 | `03-prd.md`、`06-database-design.md`、`07-api-design.md`、`08-ui-interaction.md` | 注册登录、医生列表、挂号创建、列表、取消 | P0 |
| 智能分诊 | `03-prd.md`、`05-system-architecture.md`、`07-api-design.md` | `/api/triage/consult`、分诊记录、推荐医生跳转挂号 | P0 |
| AI 处方辅助审核 | `03-prd.md`、`06-database-design.md`、`07-api-design.md` | `/api/prescription/check`、审核记录、风险等级展示 | P0 |
| AI 病历自动生成 | `03-prd.md`、`06-database-design.md`、`07-api-design.md` | `/api/medical-record/generate`、结构化病历回填和保存 | P0 |
| 管理端必做入口 | `02-requirements-analysis.md`、`03-prd.md`、`07-api-design.md`、`08-ui-interaction.md` | 基础数据维护、AI 医生排班、AI 分诊台 | P0 |
| 前后端联调与综合演示 | `09-development-plan.md`、`10-test-plan.md` | 完整诊疗案例、Postman 记录、前端截图或录屏 | P0 |
| 成果要求、答辩要求 | `README.md`、`09-development-plan.md`、`10-test-plan.md` | 源码、数据库文档、PPT、演示视频、测试记录 | P0 |
| 非功能要求 | `02-requirements-analysis.md`、`10-test-plan.md`、`11-deployment-ops.md`、`12-risks.md` | 可运行、可演示、规范、友好、可扩展 | P0 |
| WebSocket 实时通知 | 本文档、`09-development-plan.md`、`10-test-plan.md` | 高风险处方实时告警 | P0 |
| AI 流式响应 | 本文档、`09-development-plan.md`、`10-test-plan.md` | 病历生成 SSE 或 WebSocket 流式输出 | P0 |
| 前端状态机优化 | 本文档、`09-development-plan.md`、`08-ui-interaction.md` | Pinia 模块化状态与流程状态枚举 | P0 |
| Prompt 工程优化 | 本文档、`09-development-plan.md`、`04-technical-solution.md` | 可配置 Prompt 模板、科室差异化模板 | P0 |
| 双端分离部署 | 本文档、`09-development-plan.md`、`11-deployment-ops.md` | Nginx 静态资源、各微服务 Spring Boot Jar、环境变量 | P0 |
| 纯微服务架构 | 本文档、`09-development-plan.md`、`04-technical-solution.md`、`05-system-architecture.md`、`14-microservice-architecture.md` | Gateway、注册发现、业务域服务划分、OpenFeign、熔断降级、数据边界说明 | P0 |

## 2. 六项必做任务

以下六项来自任务图片，全部作为必做交付项；若与其他文档存在冲突，以本节为准。

| 顺序 | 必做任务 | 优先级 | 必做原因 |
|---|---|---|---|
| 1 | WebSocket 实时通知 | P0 | AI 审核发现高风险用药时，医生端必须收到实时告警 |
| 2 | AI 流式响应 | P0 | 病历生成必须支持 SSE 或 WebSocket 流式输出，前端逐字展示 |
| 3 | 前端状态机优化 | P0 | 使用 Pinia 管理患者、医生、挂号、处方等状态 |
| 4 | Prompt 工程优化 | P0 | 后端 Service 设计可配置 Prompt 模板，按科室优化病历生成质量 |
| 5 | 双端分离部署 | P0 | 前端 Nginx 静态资源，各后端微服务独立 Jar 包 |
| 6 | 纯微服务架构 | P0 | 从项目初始化起按认证、患者、医生、挂号、分诊、病历、处方、AI、通知等业务域划分服务，使用 Gateway、注册发现、OpenFeign 和熔断降级实现服务治理 |

## 3. 必做一：WebSocket 实时通知

### 3.1 目标

当 AI 处方审核发现高风险用药时，后端主动通过 WebSocket 向医生端推送实时告警。

### 3.2 后端设计

推荐路径：

```text
/ws/notifications
```

消息结构：

```json
{
  "type": "PRESCRIPTION_HIGH_RISK",
  "doctorId": 1,
  "patientId": 1,
  "prescriptionId": 200,
  "riskLevel": "HIGH",
  "title": "高风险用药提醒",
  "content": "检测到潜在药物相互作用，请复核处方。",
  "createdAt": "2026-06-14 16:00:00"
}
```

### 3.3 验收标准

- 医生端无需刷新即可收到高风险提醒。
- 只有处方所属医生能收到对应提醒。
- WebSocket 断开后不影响处方保存主流程。
- 告警消息可在前端通知列表中保留。

## 4. 必做二：AI 流式响应

### 4.1 目标

病历生成接口支持 SSE 或 WebSocket 流式输出，前端逐字展示 AI 生成内容，提升用户体验。

### 4.2 接口设计

```http
GET /api/medical-record/generate/stream?registrationId=100
Authorization: Bearer <jwt-token>
Accept: text/event-stream
```

事件类型建议：

| event | data |
|---|---|
| `start` | 任务开始、taskId |
| `delta` | 本次新增文本片段 |
| `structured` | 已解析出的结构化字段 |
| `done` | 生成完成 |
| `error` | 生成失败原因 |

### 4.3 验收标准

- 生成过程中页面不白屏、不阻塞。
- 用户能看到持续输出的文本。
- 断流或超时时显示失败原因。
- 流式接口不影响原有非流式 `/api/medical-record/generate`。

## 5. 必做三：前端状态机优化

### 5.1 目标

将患者分诊挂号、医生问诊病历、处方审核三条复杂流程从零散页面状态升级为可追踪、可恢复的流程状态，减少重复请求、页面刷新丢失状态和按钮误操作。

### 5.2 设计方案

Pinia 建议拆分如下：

| Store | 职责 |
|---|---|
| `useAuthStore` | Token、角色、用户信息、登录状态 |
| `useTriageStore` | 主诉、分诊结果、推荐医生、分诊记录 ID |
| `useRegistrationStore` | 科室、医生、时间段、挂号记录 |
| `useConsultationStore` | 当前接诊患者、问诊文本、病历草稿、保存状态 |
| `usePrescriptionStore` | 药品明细、审核结果、风险等级、保存状态 |
| `useAiTaskStore` | AI 请求 loading、错误、重试次数、流式输出内容 |

核心流程状态建议使用枚举：

```ts
type TriageStage = 'IDLE' | 'INPUTTING' | 'AI_RUNNING' | 'RECOMMENDED' | 'REGISTERING' | 'DONE' | 'FAILED'
type RecordStage = 'IDLE' | 'DIALOGUE_READY' | 'GENERATING' | 'DRAFT_READY' | 'SAVING' | 'SAVED' | 'FAILED'
type PrescriptionStage = 'EDITING' | 'CHECKING' | 'CHECKED' | 'SAVING' | 'SAVED' | 'HIGH_RISK'
```

### 5.3 验收标准

- 刷新页面后，Token 和基础用户信息仍可恢复。
- AI 请求期间相关按钮禁用，避免重复提交。
- AI 失败后流程进入 `FAILED`，页面保留人工操作入口。
- 高风险处方进入 `HIGH_RISK` 状态，必须医生二次确认后才能保存。

## 6. 必做四：Prompt 工程优化

### 6.1 目标

将 Prompt 从硬编码字符串升级为可配置模板，支持按任务类型和科室差异化生成，提高病历生成、分诊推荐、处方审核结果的稳定性。

### 6.2 后端设计

新增 `PromptTemplateService`，按 `taskType + departmentCode` 获取模板。

建议任务类型：

| 类型 | 场景 |
|---|---|
| `TRIAGE` | 根据主诉推荐科室和医生 |
| `MEDICAL_RECORD` | 根据问诊对话生成结构化病历 |
| `PRESCRIPTION_CHECK` | 根据患者信息和药品列表生成风险提示 |

建议模板字段：

| 字段 | 说明 |
|---|---|
| `id` | 模板 ID |
| `task_type` | 任务类型 |
| `department_code` | 科室编码，通用模板可为空 |
| `template_content` | Prompt 模板内容 |
| `output_schema` | 期望 JSON 结构 |
| `enabled` | 是否启用 |
| `version` | 模板版本 |

### 6.3 Prompt 约束

- 明确 AI 只提供辅助建议，不能替代医生。
- 强制输出 JSON，字段缺失时后端返回降级结果。
- 病历生成必须包含主诉、现病史、既往史、体格检查、初步诊断、治疗意见。
- 处方审核必须输出风险等级、风险原因、相互作用、处理建议。

### 6.4 验收标准

- 至少支持通用模板和心内科模板。
- 后端可以在不修改业务代码的情况下切换模板。
- AI 输出非 JSON 时，后端能够记录失败并返回明确错误。
- 答辩时能够说明 Prompt 如何影响 AI 结果质量。

## 7. 必做五：双端分离部署

### 7.1 目标

将 Vue 前端和 Spring Boot 微服务按生产方式分离部署，前端由 Nginx 托管，各后端微服务以独立 Jar 包运行。

### 7.2 部署结构

```mermaid
flowchart LR
  Browser[浏览器] --> Nginx[Nginx 静态资源和反向代理]
  Nginx --> VueDist[Vue dist]
  Nginx --> Gateway[gateway-service Jar]
  Gateway --> Services[业务微服务集群]
  Services --> MySQL[(MySQL 8 多 schema)]
  Services --> AiService[ai-service Jar]
  Gateway --> Registry[Nacos/Eureka]
  AiService --> AI[外部 AI 服务或 Mock 服务]
```

### 7.3 验收标准

- 前端执行 `npm run build` 后生成 `dist`。
- 各后端微服务分别执行 `mvn clean package` 后生成 Jar，至少包含 `gateway-service`、`auth-service`、`patient-service`、`doctor-service`、`registration-service`、`triage-service`、`medical-record-service`、`prescription-service`、`ai-service`、`notification-service`。
- Nginx 能访问前端页面并代理 `/api`。
- 所有敏感配置通过环境变量注入，不写死在代码里。
- 可演示完整诊疗流程。

## 8. 必做六：纯微服务架构

### 8.1 目标

系统从项目初始化起采用按业务域划分的 Spring Cloud 纯微服务架构。前端只访问 `gateway-service`；认证、患者、医生、挂号、分诊、病历、处方、AI、通知等能力分别由独立服务承担；服务之间通过注册发现和 OpenFeign 调用，并配置超时、熔断、限流和降级。

### 8.2 服务边界

| 服务 | 职责 |
|---|---|
| `gateway-service` | 统一入口、路由、CORS、限流、Token 前置解析 |
| `auth-service` | 登录、JWT、角色和账号基础信息 |
| `patient-service` | 患者资料和患者个人中心 |
| `doctor-service` | 医生、科室、排班和号源 |
| `registration-service` | 创建挂号、取消挂号、挂号记录和状态流转 |
| `triage-service` | 分诊业务编排、AI 分诊调用、推荐结果和分诊记录 |
| `medical-record-service` | AI 病历生成编排、病历保存、病历列表和详情 |
| `prescription-service` | 处方开具、AI 审核编排、处方明细和审核记录 |
| `ai-service` | 模型调用、Prompt 模板、结构化输出校验和 AI Provider 适配 |
| `notification-service` | WebSocket 连接、高风险用药告警和通知历史 |
| `admin-service` | 管理端聚合入口，编排科室、医生、药品、Prompt、系统字典、AI 排班和 AI 分诊台 |

### 8.3 调用方式

前端调用统一经过网关：

```text
前端 -> gateway-service -> 业务微服务 -> 依赖微服务
```

关键内部接口建议：

| 接口 | 说明 |
|---|---|
| `POST /internal/auth/verify` | Token 校验和用户身份解析 |
| `GET /internal/doctors/{id}` | 医生详情和排班校验 |
| `GET /internal/patients/{id}` | 患者基础信息 |
| `GET /internal/registrations/{id}` | 挂号关系校验 |
| `POST /internal/ai/triage` | 智能分诊 |
| `POST /internal/ai/medical-record/generate` | 病历生成 |
| `GET /internal/ai/medical-record/generate/stream` | 流式病历生成 |
| `POST /internal/ai/prescription/check` | 处方审核 |
| `POST /internal/ai/schedule/generate` | AI 排班建议 |
| `GET /internal/triage-records` | 分诊台记录查询 |
| `POST /internal/triage-records/{id}/assign` | 分诊台人工改派 |
| `POST /internal/doctors/schedules/publish` | 发布排班和号源 |
| `POST /internal/notifications` | 创建并推送通知 |

### 8.4 验收标准

- 前端不直接访问任何业务服务或 `ai-service`，所有请求从 `gateway-service` 进入。
- 至少具备认证、患者、医生、挂号、分诊、病历、处方、AI、通知九个独立服务。
- 服务注册发现可用，服务名调用可通过 OpenFeign 完成。
- 每个服务拥有独立数据库或独立 schema，不跨服务直接 join。
- AI 服务、通知服务或医生服务不可用时，调用方有明确降级响应。
- 可以分别启动、停止、查看各服务日志，并能演示下游服务异常时的降级。

## 9. 必做任务交付清单

| 必做任务 | 后端交付 | 前端交付 | 文档交付 |
|---|---|---|---|
| 状态机优化 | 无强制后端改造 | Pinia 模块、状态枚举、按钮禁用和恢复逻辑 | 状态流转图 |
| Prompt 工程 | 模板表、模板服务、AI Service 接入 | 模板配置展示或模板管理页面 | Prompt 设计说明 |
| 流式响应 | SSE 接口、事件协议、超时处理 | EventSource 接入、逐字展示、错误处理 | 流式接口说明 |
| WebSocket 通知 | WebSocket 配置、通知服务、消息结构 | 医生端连接、通知展示、重连 | 通知链路说明 |
| 分离部署 | Jar 包、环境变量、Nginx 代理 | `dist` 构建产物 | 部署步骤和截图 |
| 纯微服务架构 | Gateway、注册发现、认证、患者、医生、挂号、分诊、病历、处方、AI、通知服务、OpenFeign、熔断降级 | 前端统一走网关 | 服务边界、调用链、数据边界和降级策略 |
| 管理端必做入口 | `admin-service` 编排归属服务，排班发布和分诊改派不跨库写数据 | 管理首页、基础数据维护、AI 排班、AI 分诊台 | 管理端范围和验收说明 |

## 10. 答辩讲解重点

1. 项目主线不是单点 AI 演示，而是诊前、诊中、诊后的业务闭环。
2. 系统是按业务域划分的 Spring Cloud 纯微服务；前端只访问网关。
3. 病历和处方必须由医生确认后保存，体现人机协作和医疗责任边界。
4. 纯微服务架构是基础架构要求：认证、患者、医生、挂号、分诊、病历、处方、AI、通知分别拥有清晰服务边界和数据边界。
5. 管理端是第三个必做入口，基础数据维护、AI 医生排班和 AI 分诊台是 P0 范围，不是可选后台。
6. 六项必做任务体现工程能力：WebSocket 支持实时告警，SSE/WebSocket 流式响应提升体验，Pinia 状态机保证复杂流程稳定，Prompt 保证 AI 输出质量，分离部署体现工程交付，纯微服务体现服务治理和边界设计。
7. AI 不可用时系统仍可完成手动挂号、手动病历、手动处方、手动排班和人工分诊改派，保证核心业务可用。
