# 部署与运维文档

## 本地依赖

| 组件 | 用途 |
|---|---|
| KingbaseES 或 PostgreSQL 兼容库 | 唯一事实数据库，执行 `sql/kingbase_schema.sql` |
| RabbitMQ | 异步事件、通知派发 |
| Gateway | 前端唯一 API 入口 |
| Web 三端 | 患者端、医生端、管理端 |

知识库、药品、Prompt 模板搜索直接查询数据库表，无需额外搜索中间件。AI 默认可以使用 `mock` Provider，但调用必须经过 `ai-service`。

## 数据库模式

### 模式一：内置 PostgreSQL/Kingbase 兼容演示库

这是新开发者默认推荐路径，命令会启动 Web 三端、Gateway、全部后端服务、RabbitMQ 和内置数据库。

```powershell
cd D:\smart_cloud_brain
copy deploy\env\.env.example deploy\env\.env
.\scripts\start-local.ps1
```

也可以直接使用 Docker Compose：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

当前默认 `KINGBASE_IMAGE=postgres:16-alpine`，它是 PostgreSQL/Kingbase 协议兼容演示库，不是真 KingbaseES 镜像。验收如果要求真实 KingbaseES，请使用模式二，或把 `KINGBASE_IMAGE` 改为团队提供的 KingbaseES 镜像并同步 `DB_INTERNAL_PORT`、`DB_DATA_DIR`。

### 模式二：外部 KingbaseES

外部 KingbaseES 需要先创建数据库，例如 `smart_cloud_brain`。然后修改 `deploy\env\.env`：

```text
DB_SERVICE_HOST=host.docker.internal
DB_SERVICE_PORT=15432
DB_NAME=smart_cloud_brain
DB_USERNAME=system
DB_PASSWORD=123456
```

启动时不要启用 `embedded-db` profile：

```powershell
cd D:\smart_cloud_brain
.\scripts\start-local.ps1 -ExternalDb
```

或直接运行：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build
```

## 数据库初始化

`docker-compose.yml` 包含一次性服务 `db-init`，它会等待 `DB_SERVICE_HOST:DB_SERVICE_PORT` 可连接后执行：

```text
sql/kingbase_schema.sql
```

内置演示库首次创建数据卷时，脚本也会挂载到 `/docker-entrypoint-initdb.d/001_schema.sql`，但真正控制后端启动顺序的是 `db-init`：各业务服务会等 `db-init` 成功完成后再启动。脚本使用 `CREATE TABLE IF NOT EXISTS` 与 `ON CONFLICT DO NOTHING`，重复执行不会覆盖已有演示数据。

如果修改了初始化 SQL 后需要重新执行：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --force-recreate db-init
```

如果想清空内置演示库重新初始化：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db down -v
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

## 启动后访问

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- RabbitMQ 管理台: `http://localhost:15672`

默认账号：

- 患者：`13800000001 / 123456`
- 医生：`doctor1 / 123456`，也可以用手机号 `13900000001 / 123456`
- 管理员：`admin / 123456`
- RabbitMQ：默认 `scb / scb_password`

## Docker 验收命令

查看服务健康状态：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db ps
```

查看数据库初始化日志：

```powershell
docker logs scb-db-init
```

查看 Gateway 健康检查：

```powershell
curl http://localhost:18080/actuator/health
```

验证默认账号可登录：

```powershell
curl -X POST http://localhost:18080/api/patient/login -H "Content-Type: application/json" -d "{\"account\":\"13800000001\",\"password\":\"123456\"}"
curl -X POST http://localhost:18080/api/doctor/login -H "Content-Type: application/json" -d "{\"account\":\"doctor1\",\"password\":\"123456\"}"
curl -X POST http://localhost:18080/api/admin/login -H "Content-Type: application/json" -d "{\"account\":\"admin\",\"password\":\"123456\"}"
```

查看后端日志：

```powershell
docker logs scb-auth-service
docker logs scb-gateway-service
docker logs scb-rabbitmq
```

## 常见失败排查

| 现象 | 排查方式 |
|---|---|
| `db-init` 一直等待数据库 | 检查 `DB_SERVICE_HOST` / `DB_SERVICE_PORT`。内置模式必须是 `kingbase / 5432` 并带 `--profile embedded-db`；外部模式确认宿主机端口可从容器访问。 |
| 后端服务未启动 | 先看 `docker logs scb-db-init`，确认 SQL 执行成功；再看 `docker logs scb-rabbitmq`，确认 RabbitMQ 健康。 |
| 登录提示账号不存在 | 确认 `sql/kingbase_schema.sql` 已执行；必要时重建 `db-init` 或清空内置库数据卷重新启动。 |
| RabbitMQ 管理台打不开 | 检查 `RABBITMQ_MANAGEMENT_PORT` 是否被占用，默认映射为 `15672`。 |
| 端口冲突 | 修改 `deploy\env\.env` 中的 `GATEWAY_PORT`、`DB_PORT`、`RABBITMQ_PORT`、`RABBITMQ_MANAGEMENT_PORT`，Web 三端端口在 `docker-compose.yml` 中分别是 `5173`、`5174`、`5175`。 |
| 使用真实 KingbaseES 镜像失败 | 确认镜像内部端口、数据目录和初始化机制，必要时设置 `KINGBASE_IMAGE`、`DB_INTERNAL_PORT`、`DB_DATA_DIR`，并优先通过 `db-init` 日志确认 SQL 是否兼容。 |

## 后端单独编译

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```
