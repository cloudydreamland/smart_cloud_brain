# 智慧云脑诊疗平台

智慧云脑诊疗平台是一个面向课程实训和答辩演示的前后端分离微服务项目。后端采用 JDK 17、Spring Boot 3、Spring Cloud Gateway、OpenFeign、KingbaseES/PostgreSQL 兼容数据库和 RabbitMQ；前端采用 Vue 3 monorepo，包含患者端、医生端、管理端和患者移动端。

## 项目结构

```text
docs/       项目文档、验收清单、答辩脚本
backend/    Spring Boot 多模块微服务
frontend/   Vue 3 前端 monorepo
sql/        KingbaseES 兼容建表和演示种子数据
deploy/     Docker Compose、Nacos、RabbitMQ、Nginx
postman/    接口调试集合和本地环境变量
scripts/    本地启动、构建和初始化脚本
```

## 真实流程驱动说明

- Web 三端不写死业务演示数据，患者端、医生端和管理端均通过 Gateway 调后端接口。
- 核心数据来自 KingbaseES/PostgreSQL 兼容数据库，包括患者、医生、科室、排班、号源、挂号、病历、处方、通知、Prompt、知识库和字典。
- AI 能力必须走后端 `ai-service` 链路，前端不伪造 AI 结果。
- 当前 AI Provider 默认为 Mock Provider，用于稳定演示智能分诊、病历生成、处方审核和排班建议；后续可替换真实模型 Provider。
- RabbitMQ 承载处方风险等异步事件，WebSocket 负责医生端实时通知。

## 默认账号

- 患者：`13800000001 / 123456`
- 医生：`doctor1 / 123456`，也可使用手机号 `13900000001 / 123456`
- 管理员：`admin / 123456`

## 一键启动

推荐在 Windows PowerShell 中执行：

```powershell
cd D:\smart_cloud_brain
.\scripts\start-local.ps1
```

脚本会在缺少 `deploy\env\.env` 时从 `deploy\env\.env.example` 复制一份配置，然后默认启用 `embedded-db` profile，通过 Docker Compose 启动网关、业务服务、三端 Web、Nacos、RabbitMQ 和内置兼容数据库。

也可以直接执行：

```powershell
cd D:\smart_cloud_brain
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

当前默认 `KINGBASE_IMAGE=postgres:16-alpine`，这是 PostgreSQL/Kingbase 协议兼容演示库，不是真 KingbaseES 镜像。连接外部 KingbaseES 时，修改 `deploy\env\.env` 中的 `DB_SERVICE_HOST`、`DB_SERVICE_PORT`、`DB_USERNAME`、`DB_PASSWORD`，并运行 `.\scripts\start-local.ps1 -ExternalDb`。

常用地址：

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- Nacos: `http://localhost:8848`
- RabbitMQ 管理台: `http://localhost:15672`

## 数据库初始化

主初始化脚本为 `sql/kingbase_schema.sql`，包含表结构、基础演示数据和可预约号源。Docker Compose 会通过 `db-init` 服务等待数据库可用并执行该脚本；内置演示库首次创建数据卷时也会挂载到初始化目录。

脚本包含：

- 患者、医生、管理员、科室、药品、Prompt、知识库、系统字典。
- 已发布医生排班和可预约号源。
- 挂号、病历、处方、通知等交易数据由演示流程实时生成。

如果使用外部 KingbaseES，请先创建兼容数据库；启动 Compose 后由 `db-init` 执行初始化 SQL。详细验收命令和排查说明见 `docs/11-deployment-ops.md`。

## 本地构建

后端验证统一使用指定 Maven 本地仓库：

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

如果新增 Maven 依赖，需要先去掉 `-o` 联网下载依赖，再回到离线命令验证。

前端验证：

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm install
corepack pnpm test
corepack pnpm --filter patient-web build
corepack pnpm --filter doctor-web build
corepack pnpm --filter admin-web build
```

## Docker 部署

```powershell
cd D:\smart_cloud_brain
copy deploy\env\.env.example deploy\env\.env
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

如需连接外部 KingbaseES，修改 `deploy\env\.env` 中的数据库地址、端口、用户名和密码，并去掉 `--profile embedded-db`，或执行 `.\scripts\start-local.ps1 -ExternalDb`。
