# 部署与运维文档

## 本地依赖

| 组件 | 用途 |
|---|---|
| KingbaseES | 唯一事实数据库，执行 `sql/kingbase_schema.sql` |
| Nacos | 服务注册发现 |
| RabbitMQ | 异步事件、通知派发 |
| Gateway | 前端唯一 API 入口 |

知识库、药品、Prompt 模板搜索直接查询 KingbaseES，无需额外搜索中间件。

## Docker Compose 启动

```powershell
cd D:\smart_cloud_brain
copy deploy\env\.env.example deploy\env\.env
.\scripts\start-local.ps1
```

如果本机已经安装 KingbaseES，修改 `deploy\env\.env`：

```text
DB_SERVICE_HOST=host.docker.internal
DB_SERVICE_PORT=15432
DB_USERNAME=system
DB_PASSWORD=123456
```

如果没有本机 KingbaseES，可以启用内置兼容数据库：

```powershell
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

启动后访问：

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- Nacos: `http://localhost:8848`
- RabbitMQ: `http://localhost:15672`

## 后端单独编译

```powershell
cd backend
mvn -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am package -DskipTests
```

## 前端启动

```powershell
cd frontend
corepack pnpm install
corepack pnpm --filter patient-web dev
corepack pnpm --filter doctor-web dev
corepack pnpm --filter admin-web dev
```

如果只做验收演示，直接使用 Docker Compose 即可；pnpm 命令只用于本地前端热更新开发。

## 关键环境变量

| 变量 | 说明 |
|---|---|
| `DB_SERVICE_HOST` / `DB_SERVICE_PORT` / `DB_NAME` | KingbaseES 地址和库名 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 |
| `JWT_SECRET` | JWT 签名密钥 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 账号 |
| `AI_PROVIDER` | `mock` 或真实模型提供方 |

## 运维说明

- `outbox_event` 表记录待投递事件，RabbitMQ 不可用时主业务不失败，事件等待重试。
- `notification-service` 消费处方风险事件后写入 `notification_message` 并推送 `/ws/notifications`。
- 搜索接口直接读 KingbaseES 表，不需要索引重建。
