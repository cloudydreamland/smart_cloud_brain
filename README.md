# 智慧云脑诊疗平台

本项目是前后端分离的智慧诊疗微服务平台。后端使用 JDK 17、Spring Boot 3、Spring Cloud Gateway、OpenFeign、KingbaseES/PostgreSQL 兼容数据库和 RabbitMQ；前端使用 Vue 3 monorepo，包含患者端、医生端、管理端和患者移动端。

## 目录

```text
docs/       项目文档
backend/    Spring Boot 多模块微服务
frontend/   Vue 3 前端 monorepo
sql/        Kingbase 建表和演示种子数据
deploy/     Docker Compose、RabbitMQ、Nginx
postman/    接口调试集合
```

## 核心演示流程

1. 患者登录后提交 AI 分诊。
2. 患者从金仓号源中选择医生排班并预约挂号。
3. 医生查看自己的挂号队列，生成并保存 AI 病历草稿。
4. 医生执行处方审核并确认处方，风险通知通过 RabbitMQ/WebSocket 推送。
5. 患者查看自己的病历和处方。
6. 管理员维护基础数据、系统字典、分诊工作台，并生成/发布排班建议。

## 默认账号

- 患者：`13800000001 / 123456`
- 医生：`doctor1 / 123456`，也可以用手机号 `13900000001 / 123456`
- 管理员：`admin / 123456`

## 一键启动

本地工具版本：JDK 17、Node.js 24、Corepack 与 pnpm 9.15、Docker Desktop（含 Docker Compose）。

推荐使用脚本：

```powershell
cd D:\smart_cloud_brain
.\scripts\start-local.ps1
```

也可以直接使用 Docker Compose：

```powershell
docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build
```

默认配置为纯 Docker 运行：KingbaseES、RabbitMQ、后端微服务和三个前端都会由 Docker Compose 启动，不需要本机安装数据库服务。当前 Compose 不包含未实际接入的服务治理组件。

### 演示环境（含 AI）

演示时需要同时启动 Dify（AI 平台）。队友只需执行一条命令：

```bash
bash scripts/setup-demo.sh
```

首次运行会提示粘贴 Dify API Key（只需一次）。详细说明见 [docs/DEMO-SETUP.md](docs/DEMO-SETUP.md)。

常用地址：

- Gateway: `http://localhost:18080`
- Nginx 统一入口: `http://localhost:18000`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- RabbitMQ 管理台: `http://localhost:15672`

## 本地编译

后端：

```powershell
cd backend
mvn -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

前端：

```powershell
cd frontend
corepack pnpm install
corepack pnpm test
corepack pnpm --filter @smart-cloud-brain/patient-web build
corepack pnpm --filter @smart-cloud-brain/doctor-web build
corepack pnpm --filter @smart-cloud-brain/admin-web build
```

## 数据库

`sql/kingbase_schema.sql` 是演示环境主初始化脚本，包含：

- 患者、医生、管理员、科室、药品、Prompt、知识库。
- 系统字典。
- 已发布医生排班和可预约号源。

挂号、病历、处方、通知等交易数据通过实际演示流程生成。

## Real AI API configuration

`ai-service` defaults to the Dify provider. Put the real model API key, such as DeepSeek, in the Dify console model provider settings, then configure this project with Dify workflow API keys:

```env
AI_PROVIDER=dify
DIFY_BASE_URL=http://your-dify/v1
DIFY_TRIAGE_API_KEY=app-...
DIFY_MEDICAL_RECORD_API_KEY=app-...
DIFY_PRESCRIPTION_CHECK_API_KEY=app-...
```

The legacy `DIFY_API_KEY` remains as a deprecated shared-key fallback. If required variables are missing, `ai-service` fails during startup and reports the missing task key.

To bypass Dify and call an OpenAI-compatible provider directly, set:

```env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_MODEL=deepseek-v4-flash
```

Mock remains available only as an explicit local fallback:

```env
AI_PROVIDER=mock
```

Triage, medical-record generation, and prescription checks read enabled templates from `prompt_template`; admin changes affect later AI output. Successful and failed AI calls are written to `ai_generation_log` with summaries only, not full private medical text.

Note: AI-powered schedule suggestion is intentionally left as a handoff item. The current admin schedule workflow can generate suggestions and publish bookable slots, but it does not yet call `ai-service`.
