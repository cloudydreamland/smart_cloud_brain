# 部署与运维文档

## 本地依赖

| 组件 | 用途 |
|---|---|
| KingbaseES | 唯一事实数据库，执行 `sql/kingbase_schema.sql` |
| RabbitMQ | 异步事件、通知派发 |
| Gateway | 前端唯一 API 入口 |
| Web 三端 | 患者端、医生端、管理端 |

本项目以真实 KingbaseES 为准。Docker 环境默认使用团队提供的本地镜像：

```text
kingbase_v009r001c010b0004_single_x86:v1
```

后端通过 PostgreSQL 协议驱动访问 KingbaseES，因此容器启动时使用 `KINGBASE_DB_MODE=pg`。

## Docker 启动

```powershell
cd D:\smart_cloud_brain
docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build
```

如果本机 Docker CLI 不支持 `docker compose --env-file`，可使用旧版命令：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build
```

关键环境变量在 `deploy\env\.env`：

```text
DB_NAME=smart_cloud_brain
DB_USERNAME=scb
DB_PASSWORD=scb_password
DB_PORT=54321
DB_SERVICE_HOST=kingbase
DB_SERVICE_PORT=54321
KINGBASE_IMAGE=kingbase_v009r001c010b0004_single_x86:v1
KINGBASE_DB_MODE=pg
DB_INTERNAL_PORT=54321
DB_DATA_DIR=/home/kingbase/userdata/data
```

## 数据库初始化

KingbaseES 镜像不会自动执行 `/docker-entrypoint-initdb.d`。Compose 使用两个辅助服务保证初始化顺序：

- `kingbase-volume-init`：预创建并修正 `kingbase-data` 数据卷权限，避免 `initdb` 因数据目录归属错误失败。
- `db-init`：等待 KingbaseES 健康后创建 `smart_cloud_brain` 数据库，并执行 `sql/kingbase_schema.sql`。

所有后端业务服务会等待 `db-init` 成功完成后再启动。`sql/kingbase_schema.sql` 使用 `CREATE TABLE IF NOT EXISTS` 和 `ON CONFLICT`，可重复执行；默认登录账号会被刷新为验收密码：

- 患者：`13800000001 / 123456`
- 医生：`doctor1 / 123456`，也可使用手机号 `13900000001 / 123456`
- 管理员：`admin / 123456`

## 访问地址

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- RabbitMQ 管理台: `http://localhost:15672`

## 验收命令

查看服务状态：

```powershell
docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml ps
```

查看登录链路日志：

```powershell
docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml logs gateway-service auth-service kingbase db-init
```

验证默认账号：

```powershell
curl -X POST http://localhost:18080/api/patient/login -H "Content-Type: application/json" -d "{\"account\":\"13800000001\",\"password\":\"123456\"}"
curl -X POST http://localhost:18080/api/doctor/login -H "Content-Type: application/json" -d "{\"account\":\"doctor1\",\"password\":\"123456\"}"
curl -X POST http://localhost:18080/api/admin/login -H "Content-Type: application/json" -d "{\"account\":\"admin\",\"password\":\"123456\"}"
```

后端编译：

```powershell
cd D:\smart_cloud_brain\backend
mvn -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

## 常见问题

| 现象 | 排查方式 |
|---|---|
| `auth-service` 报 `Connection to kingbase:54321 refused` | 先看 `docker logs scb-kingbase`，确认 KingbaseES 是否完成 `initdb` 并通过健康检查。 |
| `initdb` 报数据目录权限错误 | 确认 `kingbase-volume-init` 已成功运行；必要时重建容器并保留 `kingbase-data` 卷。 |
| 登录提示账号不存在或密码错误 | 查看 `docker logs scb-db-init`，确认 `sql/kingbase_schema.sql` 已成功执行。 |
| 修改 SQL 后需要重新应用 | 执行 `docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --force-recreate db-init`。 |
| 端口冲突 | 修改 `deploy\env\.env` 中的 `GATEWAY_PORT`、`DB_PORT`、`RABBITMQ_PORT`、`RABBITMQ_MANAGEMENT_PORT`。 |
