# 东软智慧云脑诊疗平台

本仓库用于整理和实现“东软智慧云脑诊疗平台”课程实践项目。项目基于 JDK 17、Spring Boot 3、Vue 3、uni-app/HBuilder、Node.js 24 和 KingbaseES 兼容方案，包含 Vue Web 患者端、医生端、管理端，以及 uni-app 安卓患者端，围绕患者注册登录、AI 智能问诊/分诊、在线挂号、医生接诊、AI 病历草稿、医生确认病历、医生开具处方、AI 处方审核、患者查看记录形成轻症诊疗业务闭环。

文档口径：核心业务、技术栈、成果要求和答辩要求以 `东软智慧云脑诊疗平台实践任务描述(1).docx` 为基础；图片中的六项任务按用户要求作为本项目必做项处理。

统一口径声明：本项目采用“基础诊疗闭环 + FY26 企业项目实训方案 + 补充必做任务”的统一口径。第一阶段以后端 `diagnosis-service + ai-service` 承载可运行主链路，其他服务目录保留为后续领域拆分和长期维护规划；前端通过 `/api` 代理访问业务能力。患者端、医生端、管理端均为必做入口。WebSocket 实时通知、AI 流式响应、Prompt 工程、双端分离部署、纯微服务演进六项任务全部作为 P0 必做交付，不再作为可选扩展项处理。管理端第一版聚焦科室、医生、药品、Prompt 模板和轻症知识库维护。

## 文档入口

- [文档总览](./docs/README.md)
- [开发计划文档](./docs/09-development-plan.md)
- [任务覆盖与拓展方案](./docs/13-task-coverage-extension.md)
- [微服务架构说明](./docs/14-microservice-architecture.md)

## 工程目录

```text
smart-cloud-brain/
  docs/       项目文档
  backend/    Spring Boot / Spring Cloud 后端多模块工程
  frontend/   pnpm 前端 monorepo，包含患者端、医生端和管理端
  sql/        KingbaseES 建表脚本、兼容 SQL 和演示数据
  deploy/     Docker Compose、Nacos、MySQL、Nginx 和环境变量配置
  scripts/    本地构建、启动、停止和初始化脚本
  postman/    本地接口调试集合
```

## 本地运行

```bash
cd backend
mvn test
mvn -pl diagnosis-service spring-boot:run

cd frontend
corepack pnpm install
corepack pnpm test
corepack pnpm run build
corepack pnpm --filter patient-web dev   # http://localhost:5173
corepack pnpm --filter doctor-web dev    # http://localhost:5174
corepack pnpm --filter admin-web dev     # http://localhost:5175
```

默认演示账号：

- 患者端：可先用患者注册功能创建账号。
- 医生端：`13900000002 / 123456`
- 管理端：`admin / 123456`

KingbaseES 建库脚本位于 `sql/kingbase_schema.sql`；若本地先用 MySQL 演示，可继续使用 `deploy/mysql/init/001_schema.sql`。

## 推荐开发主线

```text
项目初始化 -> 登录认证 -> 管理端基础数据 -> AI 排班与号源 -> 智能分诊与挂号 -> AI 分诊台 -> 医生工作台 -> AI 病历生成 -> AI 处方审核 -> 六项必做任务 -> 联调演示
```

## 管理端必做范围

- 基础管理：管理员登录、科室管理、医生管理、药品管理、Prompt 模板管理、系统字典管理。
- AI 医生排班：管理员选择科室、日期范围、医生列表和号源规则，生成 AI 排班建议，人工确认后发布排班与号源。
- AI 分诊台：管理员查看患者分诊记录、AI 推荐科室和医生、推荐理由与处理状态，并支持人工改派后同步到挂号流程。

## 六项必做任务

1. WebSocket 实时通知：AI 审核发现高风险用药时，后端通过 WebSocket 向医生端推送实时告警。
2. AI 流式响应：病历生成接口支持 SSE 或 WebSocket 流式输出，前端逐字展示 AI 生成内容。
3. 前端状态机优化：使用 Pinia 模块化管理患者信息、医生信息、挂号记录、处方记录等状态。
4. AI 提示词工程优化：后端 Service 设计可配置 Prompt 模板，针对儿科、心内科等科室优化病历生成质量。
5. 双端分离部署：前端打包为 Nginx 静态资源，各后端微服务分别打包为可独立运行 Jar 包。
6. 纯微服务架构：从项目初始化起按认证、患者、医生、挂号、分诊、病历、处方、AI、通知等业务域划分服务，使用 Gateway、注册发现、OpenFeign 和熔断降级实现服务治理。

项目定位为教学实践和答辩演示系统，不用于真实医疗诊疗决策。AI 输出仅作为辅助建议，正式病历和处方结果必须由医生确认。
