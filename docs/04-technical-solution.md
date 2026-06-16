# 技术方案文档

## 1. 推荐技术栈

| 层级 | 技术 |
|---|---|
| 前端框架 | Vue 3、TypeScript、Vite、Pinia |
| Web 三端 | 患者端、医生端、管理端 |
| 移动端 | uni-app 患者移动端 |
| HTTP 客户端 | Axios，统一请求拦截和错误处理 |
| 后端框架 | JDK 17、Spring Boot 3、Spring MVC |
| 微服务 | Spring Cloud Gateway、OpenFeign、Nacos |
| 安全认证 | JWT、RBAC、网关鉴权、服务内数据归属校验 |
| 数据访问 | Spring Data JPA |
| 数据库 | KingbaseES/PostgreSQL 兼容数据库 |
| 异步事件 | RabbitMQ、Outbox 事件表 |
| 实时通信 | WebSocket，必要时使用 SSE |
| AI 能力 | 独立 `ai-service`，当前使用 Mock Provider |
| 部署 | Docker Compose、Nginx、Maven、JDK 17 |

## 2. 前端方案

前端采用 monorepo 管理患者端、医生端、管理端和共享包。三端统一通过 Gateway 访问后端，不在前端写死业务演示数据。

关键约束：

- 所有请求统一走共享 API 层，并携带 `Authorization: Bearer <token>`。
- 登录态、当前用户、关键流程状态由 Pinia 管理。
- 患者端展示分诊、号源、挂号、病历和处方数据，数据来自后端。
- 医生端展示本人待接诊队列、病历生成、处方审核和风险通知，数据来自后端。
- 管理端维护科室、医生、药品、Prompt、知识库、字典、排班和分诊工作台，数据写入数据库。
- AI 结果只展示后端 `ai-service` 返回内容，前端不伪造智能分诊、病历、处方审核或排班建议。

## 3. 后端方案

```text
backend/
  common-lib/              通用响应、异常、安全上下文、事件常量
  ai-api/                  AI 服务间契约 DTO
  gateway-service/         统一入口、JWT 校验、CORS、路由转发
  auth-service/            患者、医生、管理员登录
  patient-service/         患者资料与患者中心
  doctor-service/          科室、医生、排班和号源
  registration-service/    挂号创建、取消和状态流转
  triage-service/          分诊编排、分诊记录和推荐结果落库
  medical-record-service/  病历生成编排、医生确认保存、病历查询
  prescription-service/    处方开具、处方审核记录、Outbox 事件
  ai-service/              Mock AI、Prompt 渲染、结构化结果
  notification-service/    RabbitMQ 消费、WebSocket 推送、通知历史
  admin-service/           管理端聚合、基础数据和搜索入口
```

后端关键设计：

- 前端只访问 `gateway-service`，不直接访问业务服务或 `ai-service`。
- 服务之间通过 OpenFeign 或内部 HTTP 接口调用。
- 不允许业务服务跨库直接 join 其他服务表。
- 每个服务只维护自身领域数据，跨服务流程通过状态流转、补偿和幂等接口保障一致性。
- `ai-service` 不保存正式病历或处方，只保存 AI 调用日志、Prompt 模板和模型配置。
- `prescription-service` 先写业务数据和 `outbox_event`，再由发布器投递 RabbitMQ。
- `notification-service` 消费风险事件并通过 WebSocket 推送给医生端。

## 4. 数据库方案

验收数据库以 KingbaseES 为准，开发环境可使用 PostgreSQL 兼容模式。表结构集中在 `sql/kingbase_schema.sql`，按领域服务划分数据归属。

核心表包括：

- 用户与主数据：`patient`、`doctor`、`department`、`admin_user`。
- 诊疗流程：`triage_record`、`doctor_schedule`、`appointment_slot`、`registration`。
- 医疗记录：`medical_record`、`prescription`、`prescription_item`、`prescription_check_record`。
- AI 与管理：`prompt_template`、`ai_generation_log`、`knowledge_entry`、`drug`、`system_dict`。
- 异步通知：`outbox_event`、`notification_message`。

## 5. AI 与降级

AI 当前为 Mock Provider，但链路是真实后端链路：

```text
前端 -> gateway-service -> 业务服务 -> ai-service -> MockAiProvider
```

答辩时需要明确说明：

- Mock 的是模型输出，不是业务流程。
- 分诊记录、病历、处方、排班、通知等数据均按真实流程写入数据库。
- 后续接入真实模型时优先替换 `AiProvider` 实现，不改前端和业务主流程。

## 6. 关键技术难点

| 难点 | 解决方案 |
|---|---|
| AI 输出不稳定 | 后端约束结构化 DTO，失败时返回降级结果 |
| AI 不能阻塞主流程 | Mock Provider 保证演示稳定，真实 Provider 失败后可回退 |
| 跨服务一致性 | 单服务事务 + 状态流转 + Outbox 事件 |
| 权限越权 | JWT 角色校验 + 数据归属校验 |
| 高风险通知实时性 | RabbitMQ 事件 + WebSocket 推送 |
| 数据真实性 | 前端无业务假数据，核心数据来自 KingbaseES |
| 部署复杂 | Docker Compose 编排数据库、消息队列、微服务和三端 Web |

