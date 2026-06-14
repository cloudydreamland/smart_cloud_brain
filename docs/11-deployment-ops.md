# 部署与运维文档

## 1. 本地开发环境

| 工具 | 版本建议 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8+ |
| IDE | IntelliJ IDEA、VS Code |

后端启动：

```bash
mvn spring-boot:run
```

前端启动：

```bash
npm install
npm run dev
```

接口文档：

```text
http://localhost:8080/doc.html
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
- Spring Boot 后端打包为 Jar 独立运行。
- MySQL 独立部署。
- Nginx 将 `/api` 反向代理到后端。
- AI 服务通过环境变量配置地址和密钥。

## 4. 部署流程

### 4.1 后端

```bash
mvn clean package -DskipTests
java -jar target/smart-diagnosis.jar
```

### 4.2 前端

```bash
npm install
npm run build
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
  }
}
```

## 5. 环境变量

| 变量 | 说明 |
|---|---|
| `DB_HOST` | 数据库地址 |
| `DB_PORT` | 数据库端口 |
| `DB_NAME` | 数据库名称 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `JWT_EXPIRE_HOURS` | JWT 有效期 |
| `AI_BASE_URL` | AI 服务地址 |
| `AI_API_KEY` | AI 服务密钥 |
| `AI_TIMEOUT_MS` | AI 调用超时时间 |

## 6. 日志方案

| 日志类型 | 内容 |
|---|---|
| 请求日志 | 接口路径、方法、耗时、状态码 |
| 异常日志 | 异常堆栈、请求参数摘要 |
| AI 调用日志 | 调用类型、耗时、状态、错误信息 |
| 审计日志 | 登录、挂号、病历保存、处方保存、AI 审核 |

注意：AI 日志和请求日志不应保存完整敏感病情信息，必要时进行脱敏。

## 7. 备份方案

- MySQL 每日备份一次。
- 版本发布前备份数据库结构和关键演示数据。
- 初始化 SQL 单独维护。
- AI 调用记录可按月归档。
- 重要配置文件不提交真实密钥，使用 `.env.example` 提供模板。

