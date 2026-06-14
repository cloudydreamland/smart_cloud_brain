# 部署与运维文档

## 1. 本地开发环境

| 工具 | 版本建议 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8+ |
| IDE | IntelliJ IDEA、VS Code |

注册中心启动：

```bash
cd deploy/nacos
docker compose up -d
```

网关启动：

```bash
cd backend/gateway-service
mvn spring-boot:run
```

核心微服务启动：

```bash
cd backend/auth-service
mvn spring-boot:run

cd backend/patient-service
mvn spring-boot:run

cd backend/doctor-service
mvn spring-boot:run

cd backend/registration-service
mvn spring-boot:run

cd backend/triage-service
mvn spring-boot:run

cd backend/medical-record-service
mvn spring-boot:run

cd backend/prescription-service
mvn spring-boot:run

cd backend/ai-service
mvn spring-boot:run

cd backend/notification-service
mvn spring-boot:run

cd backend/admin-service
mvn spring-boot:run
```

前端启动：

```bash
cd frontend
pnpm install
pnpm dev
```

接口文档：

```text
网关聚合文档：http://localhost:8080/doc.html
注册中心：http://localhost:8848
```

## 2. 测试环境

- 使用独立测试数据库。
- AI 服务可配置为 Mock 或测试 API Key。
- 前端使用测试 API 地址。
- 后端开启详细日志，方便联调。
- 准备固定演示数据 SQL。

## 3. 生产环境

推荐部署结构：

- Vue 前端构建为静态资源，部署到 Nginx。
- 各后端微服务分别打包为 Jar 独立运行。
- MySQL 独立部署。
- Nginx 将 `/api` 反向代理到 `gateway-service`。
- Nginx 需要为 `/ws/notifications` 配置 WebSocket 升级代理，为 SSE 接口关闭代理缓冲。
- 各业务服务通过注册中心发现彼此，并通过 OpenFeign 调用。
- `ai-service` 通过环境变量配置外部 AI 地址和密钥。
- 除 `gateway-service` 外，业务微服务只在内网暴露，不接受公网直接访问。

## 4. 部署流程

本地一键启动：

```bash
./scripts/start-all.sh
```

本地停止：

```bash
./scripts/stop-all.sh
```

### 4.1 后端微服务

```bash
cd backend
mvn clean package -DskipTests

java -jar gateway-service/target/gateway-service.jar
java -jar auth-service/target/auth-service.jar
java -jar patient-service/target/patient-service.jar
java -jar doctor-service/target/doctor-service.jar
java -jar registration-service/target/registration-service.jar
java -jar triage-service/target/triage-service.jar
java -jar medical-record-service/target/medical-record-service.jar
java -jar prescription-service/target/prescription-service.jar
java -jar ai-service/target/ai-service.jar
java -jar notification-service/target/notification-service.jar
java -jar admin-service/target/admin-service.jar
```

### 4.2 前端

```bash
cd frontend
pnpm install
pnpm build
```

将 `dist/` 目录部署到 Nginx 静态资源目录。

### 4.3 Nginx 代理示例

```nginx
server {
  listen 80;
  server_name localhost;

  location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_buffering off;
  }

  location /ws/ {
    proxy_pass http://127.0.0.1:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
  }
}
```

`ai-service` 和其他业务服务不建议直接暴露到公网。公网只暴露 Nginx 和 `gateway-service`，服务间调用通过注册中心和内网服务名完成。

## 5. 环境变量

| 变量 | 说明 |
|---|---|
| `DB_HOST` | 数据库地址 |
| `DB_PORT` | 数据库端口 |
| `DB_NAME` | 当前服务使用的数据库或 schema 名称 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `JWT_EXPIRE_HOURS` | JWT 有效期 |
| `AI_BASE_URL` | AI 服务地址 |
| `AI_API_KEY` | AI 服务密钥 |
| `AI_TIMEOUT_MS` | AI 调用超时时间 |
| `NACOS_SERVER_ADDR` | Nacos 注册中心地址 |
| `CONFIG_SERVER_ADDR` | 配置中心地址，可与 Nacos 地址一致 |
| `SERVICE_TIMEOUT_MS` | 服务间调用超时时间 |
| `FEIGN_CONNECT_TIMEOUT_MS` | OpenFeign 连接超时时间 |
| `FEIGN_READ_TIMEOUT_MS` | OpenFeign 读取超时时间 |
| `ADMIN_DEFAULT_PASSWORD` | 演示环境管理员初始密码 |
| `SCHEDULE_AI_ENABLED` | 是否启用 AI 排班，关闭时走手动维护 |

## 6. 日志方案

| 日志类型 | 内容 |
|---|---|
| 请求日志 | 接口路径、方法、耗时、状态码 |
| 异常日志 | 异常堆栈、请求参数摘要 |
| AI 调用日志 | 调用类型、耗时、状态、错误信息 |
| 微服务调用日志 | 服务间 OpenFeign 调用的接口、耗时、状态和降级原因 |
| 审计日志 | 登录、挂号、病历保存、处方保存、AI 审核、排班发布、分诊改派 |

注意：AI 日志和请求日志不应保存完整敏感病情信息，必要时进行脱敏。

## 7. 备份方案

- MySQL 每日备份一次。
- 版本发布前备份数据库结构和关键演示数据。
- 初始化 SQL 单独维护。
- AI 调用记录可按月归档。
- 重要配置文件不提交真实密钥，使用 `.env.example` 提供模板。
