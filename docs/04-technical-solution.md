# 技术方案文档

## 1. 推荐技术栈

| 层级 | 技术 |
|---|---|
| 前端框架 | Vue 3、TypeScript、Vite |
| UI 组件 | Element Plus |
| 路由与状态 | Vue Router、Pinia |
| HTTP 客户端 | Axios |
| 后端框架 | Spring Boot 3、Spring MVC、Spring Cloud Gateway、Spring Cloud OpenFeign、Spring Security |
| 数据访问 | Spring Data JPA，或 SQL-Toy |
| 参数校验 | Spring Validation |
| 数据库 | MySQL 8，PostgreSQL 可选 |
| 认证 | Spring Security、JWT、RBAC |
| API 文档 | Knife4j / Swagger3 |
| 服务治理 | Nacos 或 Eureka、Nacos Config 或 Spring Cloud Config、Resilience4j 或 Sentinel |
| AI 集成 | 独立 `ai-service`、HTTP API、大语言模型服务、Spring AI、ChatClient、结构化输出、RAG 和 Function Calling 可作为推荐实现 |
| 实时通信 | WebSocket、SSE |
| 部署 | Nginx、JDK 17、Maven、Docker 可选 |

## 2. 前端架构

```text
frontend/
  src/
    api/                 接口封装
    assets/              静态资源
    components/          通用组件
    router/              路由配置
    stores/              Pinia 状态模块
    types/               TypeScript 类型
    utils/
      request.ts         Axios 实例和拦截器
    views/
      auth/              登录注册
      patient/           患者端页面
      doctor/            医生端页面
      admin/             管理端页面、AI 排班、AI 分诊台
```

### 2.1 前端关键设计

- 使用 `request.ts` 统一封装 Axios。
- 请求拦截器自动读取 Token 并设置 `Authorization: Bearer <token>`。
- 响应拦截器统一处理 `401`、`403`、业务错误和 AI 错误。
- Pinia 拆分为 `userStore`、`registrationStore`、`doctorStore` 等模块。
- 路由守卫根据登录状态和角色控制访问，管理端路由只允许 ADMIN 角色访问。
- AI 请求统一展示 Loading，失败后保留手动操作入口。

## 3. 后端纯微服务架构

```text
backend/
  common-lib/              Result、错误码、通用 DTO、工具类
  gateway-service/         统一入口、路由、CORS、限流、鉴权前置
  auth-service/            登录、JWT、角色、Token 刷新
  patient-service/         患者注册资料、患者个人中心
  doctor-service/          医生、科室、排班、号源
  registration-service/    创建挂号、取消挂号、挂号记录
  triage-service/          分诊业务编排、分诊记录、推荐结果
  medical-record-service/  病历生成编排、病历保存、病历查询
  prescription-service/    处方开具、处方明细、处方审核记录
  ai-service/              智能分诊、病历生成、处方审核、Prompt、模型适配
  notification-service/    WebSocket 连接、实时通知、通知历史
  admin-service/           管理端聚合入口、系统配置、后台操作编排
```

### 3.1 后端关键设计

- Controller 只处理请求接收、参数校验和响应封装。
- 前端只访问 `gateway-service`，不直接访问业务服务或 `ai-service`。
- 各业务服务按领域划分，分别负责自己的 Controller、Service、Repository 和数据库 schema。
- 服务之间通过 OpenFeign 调用，不允许跨服务直接访问数据库。
- 每个 Feign Client 必须设置超时、错误解码、熔断和降级。
- `ai-service` 只负责模型调用、Prompt 渲染、结构化输出校验和 AI 降级，不保存挂号、病历、处方等业务主数据。
- `notification-service` 负责 WebSocket 连接和高风险处方实时告警。
- `admin-service` 只作为管理端聚合和后台操作编排入口；科室、医生、药品、Prompt 等业务主数据仍写入各自归属服务。AI 排班由 `admin-service` 编排 `ai-service` 和 `doctor-service`，AI 分诊台由 `admin-service` 编排 `triage-service`、`doctor-service` 和 `ai-service`。
- SSE 或 WebSocket 用于病历生成流式输出，可由 `medical-record-service` 透传 `ai-service` 的流式结果。
- 使用 `@Transactional` 保证单个服务内部写操作一致性；跨服务流程使用状态流转、补偿或幂等接口处理。
- 使用 `@RestControllerAdvice` 统一处理异常。

## 4. 数据库设计思路

- 教学环境可以共用一个 MySQL 实例，但必须按服务划分独立数据库或 schema。
- `patient-service` 只维护患者资料，`doctor-service` 只维护医生、科室和排班。
- `registration-service` 维护挂号记录，不直接 join 患者表或医生表，需要数据时通过服务接口查询。
- `medical-record-service` 维护病历记录，`prescription-service` 维护处方和处方审核记录。
- `ai-service` 维护 AI 调用日志、Prompt 模板和模型配置，不保存正式病历或处方。
- 禁止跨 schema 直接 join，跨服务聚合由接口编排或查询模型完成。

## 5. 第三方服务或接口

| 服务 | 用途 | MVP 策略 |
|---|---|---|
| Nacos 或 Eureka | 服务注册发现 | 必选 |
| Spring Cloud Gateway | 统一 API 入口和路由 | 必选 |
| OpenFeign | 服务间同步调用 | 必选 |
| Resilience4j 或 Sentinel | 熔断、限流、降级 | 推荐 |
| `ai-service` | 分诊、病历生成、处方审核统一入口 | 必选 |
| 大语言模型 API | 分诊、病历生成、处方审核 | 可先使用 Mock 实现 |
| Spring AI / ChatClient | 模型适配、结构化输出、工具调用 | 推荐 |
| 知识图谱服务 | 药品相互作用、疾病科室映射 | 可选 |
| Knife4j | API 文档和联调 | 必选 |
| Nginx | 前端部署和反向代理 | 生产环境使用 |

## 6. 系统模块划分

| 模块 | 后端服务 | 前端页面 |
|---|---|---|
| 网关 | `gateway-service` | 所有页面统一入口 |
| 认证 | `auth-service` | 登录页、注册页 |
| 患者 | `patient-service` | 患者首页、个人中心 |
| 医生 | `doctor-service` | 医生列表、医生工作台 |
| 挂号 | `registration-service` | 在线挂号、我的挂号 |
| 智能分诊 | `triage-service` 调用 `ai-service` 和 `doctor-service` | 智能分诊页 |
| 病历 | `medical-record-service` 调用 `ai-service` | 问诊病历页、病历详情 |
| 处方 | `prescription-service` 调用 `ai-service` 和 `notification-service` | 处方开具页、处方详情 |
| 通知 | `notification-service` | 医生端通知抽屉、实时告警 |
| 管理 | `admin-service` 编排各归属服务 | 管理端基础数据维护、AI 排班管理、AI 分诊台 |

## 7. 关键技术难点与解决方案

| 难点 | 解决方案 |
|---|---|
| AI 返回不稳定 | 要求 JSON 输出，后端校验字段，不合法则返回降级提示 |
| AI 请求耗时 | 前端 Loading，后端设置超时，必要时异步处理 |
| AI 服务不可用 | 使用 Mock 或手动流程兜底 |
| 微服务调用失败 | OpenFeign 设置超时、错误解码和降级，Resilience4j/Sentinel 进行熔断限流 |
| 高风险告警实时性 | 使用 WebSocket 推送给医生端，断线后前端重连 |
| 病历生成等待时间长 | 使用 SSE 或 WebSocket 流式输出，前端逐字展示 |
| 复杂前端流程状态混乱 | 使用 Pinia 模块化 Store 和流程状态枚举 |
| Prompt 难维护 | 使用可配置 Prompt 模板，按任务类型和科室选择 |
| 管理端范围失控 | 将 P0 限定为基础数据维护、AI 排班建议 + 人工确认发布、AI 分诊台查看/改派 |
| 权限隔离 | JWT + 角色校验 + 数据归属校验 |
| 前后端跨域 | Vite 代理 + 后端 CORS 配置 |
| 重复挂号 | 数据库唯一约束或业务校验 |
| 处方安全 | AI 仅辅助，医生确认后才能保存 |
| 演示稳定性 | 准备固定演示数据和 Mock AI 返回 |
