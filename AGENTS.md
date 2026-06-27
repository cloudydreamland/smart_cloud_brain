# AGENTS.md — 智慧云脑诊疗平台

## 项目概况

三端诊疗平台（患者端 / 医生端 / 管理端），pnpm monorepo。

```
frontend/
├── apps/
│   ├── patient-web/    # 患者端，端口 5173（dev）
│   ├── doctor-web/     # 医生端，端口 5174（dev）/ Docker
│   └── admin-web/      # 管理端，端口 5175（dev）/ Docker
├── packages/
│   ├── shared-ui/      # 共享组件库（Modal、Toast、Sidebar 等）
│   ├── shared-api/     # 共享 API 封装
│   └── shared-types/   # 共享类型定义
```

## 构建命令

```bash
cd frontend
corepack pnpm build:doctor    # 构建医生端
corepack pnpm build:admin     # 构建管理端
corepack pnpm build:patient   # 构建患者端
```

## ⚠️ 踩坑记录（重要）

### 1. Tailwind CSS 依赖链

**事实**：`shared-ui` 的 11 个组件（Modal、Toast、Popover、Input、Button 等）使用了 Tailwind CSS 类名（`flex`、`fixed`、`inset-0`、`z-[101]` 等）。

**后果**：所有消费 shared-ui 的 app 都必须配置 Tailwind 构建，否则这些组件样式完全失效（不可见）。

**当前配置**：doctor-web 和 admin-web 已配置 Tailwind v3 构建（`postcss.config.js` + `tailwind.config.js`）。不要再用 CDN 方式引入 Tailwind。

**不要做的事**：
- 不要删除 `vite.config.ts` 中的 postcss 配置
- 不要删除 `postcss.config.js` 和 `tailwind.config.js`
- 不要在 `index.html` 里加回 `<script src="https://cdn.tailwindcss.com">`

### 2. CSS 优先级（Specificity）

Tailwind 的 `preflight`（CSS reset）会覆盖自定义样式。当 Tailwind 类名和自定义 CSS 冲突时：

- 用更具体的选择器提高优先级：`.doctor-app button` 优于 `button`
- `@import` 语句必须在 `@tailwind` 指令之前
- 不要滥用 `!important`

### 3. 侧边栏 group-label 文字折行

**问题**：`CollapsibleSidebar.vue` 的 `.c-group-label` 在折叠态（56px 宽）下文字折行，导致高度从 28px 膨胀到 76px，展开时导航项整体上跳 48px。

**修复**：`.c-group-label` 必须有 `white-space: nowrap; overflow: hidden; text-overflow: ellipsis;`

**教训**：设计稿里没有的元素 = CSS 对比覆盖不到的盲区。遇到视觉 bug 不要只比代码，要用 JS 量 DOM 实际渲染数据（`getBoundingClientRect`、`scrollWidth/clientWidth`）。

### 4. Docker 重建后端

```bash
# 重建单个服务
DOCKER_BUILDKIT=1 docker build -f backend/Dockerfile.service --build-arg SERVICE=xxx -t scb-xxx .

# ⚠️ 重建容器时必须 source env/.env（cat 读出来密码是 ***，source 才能正确加载）
source env/.env && docker run -d --name xxx ...

# ⚠️ --name 必须和服务名一致（无 scb- 前缀），否则网关 DNS 解析失败
```

### 5. 登录页 CSS

登录页的 CSS 在 `style.css` 末尾定义。Tailwind preflight 会覆盖未定义的样式——登录页之前靠 preflight 兜底，现在已有完整 CSS。

### 6. 设计稿还原标准

Vue 页面必须与 `UI测试/设计稿/` 下的 HTML 逐页一致（颜色保持项目主题色不动）。先出计划确认范围，再逐页执行 + 截图验证。

### 7. 代码改动流程

**改完代码后不要直接 commit**。流程：
1. 改代码 → build → 重建 Docker → 重启容器
2. 用户在浏览器验收确认
3. 确认 OK → git commit + push

### 8. Docker Desktop 代理导致网关 502

**问题**：Docker Desktop 自动给容器注入 `http_proxy=http://host.docker.internal:7897`，网关容器的所有内部服务请求都走了本地代理，代理不认识 Docker 内网主机名，返回 502。

**表现**：gateway → auth-service 返回 502 Bad Gateway，所有认证和数据接口全挂。

**修复**：在 docker-compose.yml 的 gateway-service 加 `NO_PROXY` 环境变量，列出所有内部服务名。用 `${NO_PROXY:-...}` 写法让没代理的队友不受影响。

```yaml
NO_PROXY: ${NO_PROXY:-localhost,127.0.0.1,auth-service,patient-service,doctor-service,...}
no_proxy: ${NO_PROXY:-localhost,127.0.0.1,auth-service,patient-service,doctor-service,...}
```

**排查方法**：`docker exec <container> curl -v http://<service>:<port>/actuator/health` — 看是否走了代理。

### 9. JWT_SECRET 不一致导致全端 401

**问题**：auth-service 容器残留了旧的 `JWT_SECRET=replacethiscret`，gateway 用的是 `smart-cloud-brain-local-secret-please-change`。auth 签的 token gateway 验不了。

**表现**：三端登录全部成功（200），但所有需要认证的接口全部返回 401。

**排查方法**：
```bash
docker exec scb-gateway-service env | grep JWT_SECRET
docker exec scb-auth-service env | grep JWT_SECRET
# 两边必须一致
```

**修复**：`docker compose up -d --no-deps --force-recreate auth-service`

**教训**：force-recreate 才会重新读 docker-compose.yml 的环境变量。只 restart 不会。

### 10. 前端调用后端未实现的接口返回 500

**表现**：接口返回 `{"code":500,"message":"internal server error"}`，gateway 日志显示 `NoResourceFoundException: No static resource api/xxx`。

**原因**：前端提前写了 API 调用，但后端 controller 还没实现对应路由。

**当前未实现的接口**：
- `GET /api/patient/departments` — patient-service 没有
- `GET /api/patient/doctors` — patient-service 没有
- `GET /api/admin/dashboard/stats` — admin-service 没有
- `GET /api/admin/doctor/list` — admin-service 没有
- `GET /api/doctor/queue` — doctor-service 没有
- `GET /api/doctor/dashboard` — doctor-service 没有

### 11. 种子数据登录凭据

所有种子账号密码都是 `123456`，哈希为 sha256。

| 角色 | 账号（account 字段） | 密码 |
|------|---------------------|------|
| 患者 | 13800000001 | 123456 |
| 医生 | 13900000001 | 123456 |
| 管理员 | admin | 123456 |

**注意**：医生端用手机号登录，不是 username。管理端也是 `admin` 不是别的。

### 12. 后端服务间调用也需要 NO_PROXY

**问题**：Docker Desktop 代理不仅影响 gateway，也影响所有后端微服务。admin-service 调 doctor-service 内部接口时走了代理，导致查询参数（如 `departmentId`）被代理吞掉或请求被转发到外网。

**表现**：排班搜索传了 `departmentId=2` 但返回全部数据（代理返回了缓存/默认响应），force-recreate 服务后数据变空。

**修复**：在 `&domain-env` YAML anchor 中加入 `NO_PROXY`，所有继承该 anchor 的后端服务都会自动获得正确的 NO_PROXY 配置。不要只给 gateway 加——**所有会发起 HTTP 调用的后端服务都需要**。

**教训**：docker-compose 的 YAML anchor 可以一劳永逸地解决批量配置问题。不要逐个服务加环境变量，找到共享的 anchor 一起改。

### 13. force-recreate 后服务间调用链断裂

**问题**：force-recreate 多个有依赖关系的服务时，如果启动顺序不对，服务间调用会失败。admin-service 依赖 doctor-service 和 triage-service，必须等它们 healthy 后再启动。

**表现**：recreate 后排班数据从 6 条变 0 条，admin-service 无法从 doctor-service 拉到数据。

**排查方法**：检查服务启动顺序和健康状态，`docker compose ps` 查看各服务状态。

## 前端代码规范（Agent 必读）

以下规则基于历史屎山代码审查总结，**违反任何一条都会被驳回**。

### 组件体积

- **单文件上限 400 行**（`<script>` + `<template>` + `<style>` 合计）。超过必须拆分。
- 拆分方向：按功能拆子组件（`XxxEditor.vue`、`XxxList.vue`），或按逻辑抽 composable（`useXxx.ts`）。
- 反面教材：`PatientSiteConfigPage.vue` 1705 行 / 75KB，一个文件塞了 3 种配置的全部 CRUD + 校验 + 6 种编辑弹窗。

### 类型安全

- **禁止用 `Record<string, unknown>` 做业务数据类型**。`DataRow` 仅用于后端返回值的临时兼容层。
- 新增/修改 API 返回值时，优先使用 `types.ts` 里已定义的具体接口（`Department`、`Doctor`、`Registration` 等）。
- 模板中访问字段：用 `item.fieldName` 而不是 `fieldText(item, "fieldName")`（后者仅在类型不确定时使用）。

### API 层

- **不要手动传 token**。`request()` 已通过 `setTokenProvider` 自动注入 token，新 API 函数签名不需要 `token` 参数。
- 新增 API 函数放在对应的 namespace（`adminApi` / `doctorApi` / `patientApi`），不要堆到末尾的扁平 `api` re-export 对象里。

### 状态映射

- **状态码→中文标签**：使用 `shared-api/formatters.ts` 的 `statusText()`，不要在组件里重新维护映射表。
- **状态码→CSS class**：管理端用 `statusClass()`，医生端用 `doctorPresentation.ts` 的 `statusTone()`（它返回细粒度的 `low`/`pending`/`active` 等 class）。
- 新增状态值时，**同时更新** `statusText` 和对应的 tone 函数。

### 控制流

- **禁止散落的 `if (entity === "X")` 链**处理多实体逻辑。用 `Record<Entity, Handler>` map 查表。
- 反面教材：`AdminCatalogPage` 原来 6 个 if 分支做 save，现在用 `saveHandlers[entity]()` 一行搞定。

### CSS

- **禁止内联 `style=""`**。所有样式写在 `<style>` 块或 CSS 文件里。
- 颜色用 CSS 变量（`var(--primary)` 等），不要硬编码 hex 值。
- 组件 scoped 样式的类名用 BEM 或语义前缀（`.consult-layout`、`.metric-card`），避免 `.title` `.content` 这种泛化命名。

### 模板

- 模板里的复杂逻辑（条件判断、数据转换、格式化）抽到 `<script setup>` 的 computed 或函数里，不要内联在 HTML 中。
- `v-if` / `v-else-if` 链超过 3 个分支时，考虑拆成子组件或用 `<component :is>` 动态渲染。

### Store

- Pinia store 只管状态和数据获取，不管 UI 逻辑（格式化、DOM 操作）。
- `refresh()` 方法里多个并行请求用 `Promise.all`，单个失败用 `.catch(fallback)` 隔离，不要让一个接口挂掉拖垮全部数据。

## AI 助手注意事项

- shared-ui 组件依赖 Tailwind 类名，新增/修改组件时确保 Tailwind 可用
- 不要删除 `tailwind.config.js` 中的 `content` 配置里的 shared-ui 路径
- 遇到 CSS 视觉问题，先用 `browser_console` 的 `getBoundingClientRect()` 量实际渲染数据，不要只读代码推断
- Windows 环境下如需通过网页验证功能或视觉效果，不要手动启动前端开发服务器；应在项目根目录执行 `.\scripts\start-local.ps1`，等待 Docker 构建和容器启动完成后，再连接浏览器进行测试
- 全中文项目内容，保留英文专有名词
