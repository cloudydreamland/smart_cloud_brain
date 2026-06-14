# 技术方案文档

## 1. 推荐技术栈

| 层级 | 技术 |
|---|---|
| 前端框架 | Vue 3、TypeScript、Vite |
| UI 组件 | Element Plus |
| 路由与状态 | Vue Router、Pinia |
| HTTP 客户端 | Axios |
| 后端框架 | Spring Boot 3、Spring MVC、Spring Cloud OpenFeign |
| 数据访问 | Spring Data JPA，或 SQL-Toy |
| 参数校验 | Spring Validation |
| 数据库 | MySQL 8，PostgreSQL 可选 |
| 认证 | JWT |
| API 文档 | Knife4j / Swagger3 |
| AI 集成 | 独立 `ai-service`、HTTP API、大语言模型服务、知识图谱服务可选 |
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
```

### 2.1 前端关键设计

- 使用 `request.ts` 统一封装 Axios。
- 请求拦截器自动读取 Token 并设置 `Authorization: Bearer <token>`。
- 响应拦截器统一处理 `401`、`403`、业务错误和 AI 错误。
- Pinia 拆分为 `userStore`、`registrationStore`、`doctorStore` 等模块。
- 路由守卫根据登录状态和角色控制访问。
- AI 请求统一展示 Loading，失败后保留手动操作入口。

## 3. 后端微服务架构

```text
backend/
  diagnosis-service/       业务服务，对前端提供 REST API
    controller/            认证、患者、医生、挂号、病历、处方 API
    service/               业务编排、事务、权限校验
    client/                调用 ai-service 的 Spring Cloud Feign 客户端
    repository/            业务数据访问
    entity/                患者、医生、挂号、病历、处方等实体
    common/                Result、错误码、工具类
    exception/             全局异常处理
  ai-service/              AI 能力服务，对 diagnosis-service 提供内部 API
    controller/            分诊、病历生成、处方审核内部接口
    service/               AI 编排、Prompt、Provider 适配、降级
    provider/              外部大模型、知识图谱、Mock 实现
    dto/                   AI 请求和响应 DTO
    common/                AI 错误码、结构化输出校验
```

### 3.1 后端关键设计

- Controller 只处理请求接收、参数校验和响应封装。
- `diagnosis-service` 的 Service 负责业务编排、事务和权限校验。
- Repository 负责数据访问。
- AI 能力必须放入独立 `ai-service`，避免业务服务直接依赖具体大模型 Provider。
- `diagnosis-service` 通过 Spring Cloud Feign 调用 `ai-service`，并设置超时、错误解码和降级。
- WebSocket 用于高风险处方实时告警。
- SSE 或 WebSocket 用于病历生成流式输出。
- 使用 `@Transactional` 保证挂号、病历、处方等写操作一致性。
- 使用 `@RestControllerAdvice` 统一处理异常。

## 4. 数据库设计思路

- 传统业务表与 AI 记录表分开，便于追踪 AI 调用。
- 挂号表连接患者、医生、科室。
- 病历与挂号关联，处方与病历关联。
- AI 分诊记录保留主诉和推荐结果。
- AI 处方审核记录保留风险等级、建议、原始 JSON。
- AI 病历生成可在病历表中通过 `ai_generated` 标记，也可后续扩展独立生成记录表。

## 5. 第三方服务或接口

| 服务 | 用途 | MVP 策略 |
|---|---|---|
| `ai-service` | 分诊、病历生成、处方审核统一入口 | 必选 |
| 大语言模型 API | 分诊、病历生成、处方审核 | 可先使用 Mock 实现 |
| 知识图谱服务 | 药品相互作用、疾病科室映射 | 可选 |
| Knife4j | API 文档和联调 | 必选 |
| Nginx | 前端部署和反向代理 | 生产环境使用 |

## 6. 系统模块划分

| 模块 | 后端服务 | 前端页面 |
|---|---|---|
| 认证 | `AuthService` | 登录页、注册页 |
| 患者 | `PatientService` | 患者首页、个人中心 |
| 医生 | `DoctorService` | 医生列表、医生工作台 |
| 挂号 | `RegistrationService` | 在线挂号、我的挂号 |
| 分诊 AI | `diagnosis-service` 调用 `ai-service` 的 `TriageAiController` | 智能分诊页 |
| 病历 | `MedicalRecordService` 调用 `ai-service` 的 `MedicalRecordAiController` | 问诊病历页、病历详情 |
| 处方 | `PrescriptionService` 调用 `ai-service` 的 `PrescriptionCheckAiController` | 处方开具页、处方详情 |

## 7. 关键技术难点与解决方案

| 难点 | 解决方案 |
|---|---|
| AI 返回不稳定 | 要求 JSON 输出，后端校验字段，不合法则返回降级提示 |
| AI 请求耗时 | 前端 Loading，后端设置超时，必要时异步处理 |
| AI 服务不可用 | 使用 Mock 或手动流程兜底 |
| 微服务调用失败 | Spring Cloud Feign 设置超时、错误解码和降级，业务服务返回可操作提示 |
| 高风险告警实时性 | 使用 WebSocket 推送给医生端，断线后前端重连 |
| 病历生成等待时间长 | 使用 SSE 或 WebSocket 流式输出，前端逐字展示 |
| 复杂前端流程状态混乱 | 使用 Pinia 模块化 Store 和流程状态枚举 |
| Prompt 难维护 | 使用可配置 Prompt 模板，按任务类型和科室选择 |
| 权限隔离 | JWT + 角色校验 + 数据归属校验 |
| 前后端跨域 | Vite 代理 + 后端 CORS 配置 |
| 重复挂号 | 数据库唯一约束或业务校验 |
| 处方安全 | AI 仅辅助，医生确认后才能保存 |
| 演示稳定性 | 准备固定演示数据和 Mock AI 返回 |
