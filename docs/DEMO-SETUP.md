# 演示环境部署 — 队友速查

## 你需要什么

- **Docker Desktop**（必须，[下载](https://www.docker.com/products/docker-desktop/)）
- 项目代码（已拉取到本地）

## 前提条件

Dify 需要提前配置好模型和 Workflow。如果你是首次部署：

1. 登录 Dify 控制台（http://localhost）
2. 在 **设置 → 模型供应商** 中添加 DeepSeek（或其他 LLM）
3. 导入 3 个 Workflow（分诊 / 病历生成 / 处方审核）

> 这些配置由负责 AI 的同学统一完成，队友只需粘贴 API Key 即可。

## 一键部署

```bash
cd 项目根目录
bash scripts/setup-demo.sh
```

脚本会自动完成：
1. 启动 Dify（AI 平台）
2. 打开浏览器让你导入 3 个 Workflow
3. 提示你粘贴 3 个 API Key
4. 启动数据库、后端、前端所有服务
5. 仅将 AI 服务接入 Dify 的 Docker 网络

## 首次需要手动操作

脚本运行到"步骤 2/5"时会打开浏览器：

### 导入 Workflow

1. 浏览器自动打开 **http://localhost/apps**
2. 注册/登录 Dify
3. 点击「创建应用」→「导入 DSL 文件」
4. 依次导入 `deploy/dify-workflows/` 下的 3 个文件：
   - `triage.yml`（分诊）
   - `medical-record.yml`（病历生成）
   - `prescription.yml`（处方审核）

### 复制 API Key

导入完成后，对每个应用：
1. 进入应用 →「API 访问」→ 复制 API Key
2. 粘贴到终端对应位置

> API Key 只需粘贴一次，之后会保存到本地 .env 文件。

## 部署完成后

| 服务 | 地址 |
|------|------|
| 患者端 | http://localhost:5173 |
| 医生端 | http://localhost:5174 |
| 管理端 | http://localhost:5175 |
| Nginx 统一入口 | http://localhost:18000 |
| Dify 控制台 | http://localhost |

## 常用命令

```bash
# 验证 AI 是否正常
bash scripts/verify-dify-ai.sh

# 停止所有服务
docker compose -f deploy/docker-compose.yml down

# 查看 AI 服务日志
docker compose -f deploy/docker-compose.yml logs -f ai-service

# 重新启动（不重建镜像）
docker compose -f deploy/docker-compose.yml up -d
```

## 常见问题

**Q: Docker 启动报错 "Cannot connect to the Docker daemon"**
A: 打开 Docker Desktop，等左下角图标变绿再运行脚本。

**Q: 脚本卡在"等待 Dify 就绪"**
A: 首次启动 Dify 需要下载镜像，可能要 5-10 分钟。超过 10 分钟按 Ctrl+C 后重新运行。

**Q: AI 功能返回 degraded=true**
A: 检查 3 个 Workflow 是否都已导入，且对应的 API Key 是否正确粘贴。

**Q: 端口被占用 (5173/5174/18080)**
A: 检查端口占用：`lsof -i :5173`，找到 PID 后 kill 掉，或修改 `deploy/env/.env` 中的端口。

**Q: 内存不够 (Mac 16G)**
A: 同时跑 Dify + 全栈服务吃内存。演示前关掉不用的应用，或在 Docker Desktop 里把内存限制调到 8-10GB。

**Q: Buildx 提示 `header ... contains value with non-printable ASCII characters`**
A: Docker Desktop 的 Buildx 在部分中文路径下存在兼容问题。请将项目放到纯英文路径（例如 `~/Projects/smart-cloud-brain`）后重试。
