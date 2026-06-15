# 智慧云脑诊疗平台

本项目是前后端分离的智慧诊疗微服务平台。后端采用 JDK 17、Spring Boot 3、Spring Cloud Gateway、OpenFeign、KingbaseES、RabbitMQ；前端采用 Vue 3 monorepo，包含患者端、医生端和管理端。系统主链路覆盖患者注册登录、AI 分诊、挂号、医生接诊、AI 病历生成、处方开具、AI 处方审核、实时通知和患者记录查看。

## 目录

```text
docs/       项目文档
backend/    Spring Boot 多模块微服务
frontend/   Vue 3 前端 monorepo
sql/        KingbaseES 主建表和种子数据
deploy/     Docker Compose、Nacos、RabbitMQ、Nginx
postman/    接口调试集合
```

## 后端服务

前端只访问 `gateway-service`。`diagnosis-service` 已移除，不再作为入口或兼容服务。

```text
gateway-service, auth-service, patient-service, doctor-service,
registration-service, triage-service, medical-record-service,
prescription-service, notification-service, admin-service, ai-service
```

KingbaseES 是唯一事实数据库。知识库、药品和 Prompt 的搜索直接查询金仓表；RabbitMQ 负责处方风险通知等异步事件。

## 一键启动

先复制环境变量模板：

```powershell
cd D:\smart_cloud_brain
copy deploy\env\.env.example deploy\env\.env
```

如果你本机已经安装 KingbaseES，编辑 `deploy\env\.env`：

```text
DB_SERVICE_HOST=host.docker.internal
DB_SERVICE_PORT=15432
DB_USERNAME=system
DB_PASSWORD=123456
```

推荐启动方式：

```powershell
.\scripts\start-local.ps1
```

这个脚本会启动 Docker Compose 里的 Nacos、RabbitMQ、Gateway、后端微服务和三个前端容器，搜索直接使用金仓。

如果不使用脚本，也可以直接执行：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build
```

如果没有本机 KingbaseES，可以启用内置兼容数据库：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

常用地址：

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- Nacos: `http://localhost:8848`
- RabbitMQ 管理台: `http://localhost:15672`

## 本地编译

```powershell
cd backend
mvn -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

## 前端启动

```powershell
cd frontend
corepack pnpm install
corepack pnpm --filter patient-web dev
corepack pnpm --filter doctor-web dev
corepack pnpm --filter admin-web dev
```

日常如果只用 Docker 一键启动，不需要单独运行 pnpm。

默认测试账号：

- 患者端：`13800000001 / 123456`
- 医生端：`13900000002 / 123456`
- 管理端：`admin / 123456`
