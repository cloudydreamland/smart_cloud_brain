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
1. 改代码 → dev server 自动热更新 → 用户在浏览器验收确认
2. 确认 OK → git commit + push
3. 需要部署时才执行 `corepack pnpm build:*` + 重建 Docker 容器（见踩坑记录 14）

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

**2026-06-29 审计更新**：以下接口在后端审查中确认已实现（AGENTS.md可能未及时更新）：
- ~~`GET /api/admin/dashboard/stats`~~ — ✅ AdminController 已实现
- ~~`GET /api/admin/doctor/list`~~ — ✅ AdminController 已实现
- ~~`GET /api/doctor/queue`~~ — ✅ DoctorController 已实现
- ~~`GET /api/doctor/dashboard`~~ — ✅ DoctorController 已实现

**仍需运行态验证**：
- `GET /api/patient/departments` — patient-service 状态待确认
- `GET /api/patient/doctors` — patient-service 状态待确认

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

### 14. 前端开发模式（推荐）

开发期间用 Vite dev server 而不是 Docker nginx，改代码后浏览器自动热更新，无需每次 build + recreate。

**启动方式：**
1. `docker stop scb-nginx`（释放 5173-5175 端口）
2. 三个终端分别启动：
   ```bash
   cd frontend/apps/patient-web  && VITE_GATEWAY_MODE=docker npx vite --port 5173
   cd frontend/apps/doctor-web   && VITE_GATEWAY_MODE=docker npx vite --port 5174
   cd frontend/apps/admin-web    && VITE_GATEWAY_MODE=docker npx vite --port 5175
   ```

**效果：** 改代码 → HMR 自动刷新，直接看效果。后端 API 仍走 Docker（localhost:18080）。

**验证完后：** `docker start scb-nginx` 恢复，或直接 commit + push。

**AI 改代码流程：** 改完源码即可，dev server 会自动热更新，不需要手动 build。

### 15. 容器手动 stop 后不会自动恢复

**问题**：所有后端微服务都设了 `restart: unless-stopped`，但 `unless-stopped` 只在 Docker daemon 重启时自动拉起之前运行的容器。被 `docker stop` 手动停掉的容器，**不会**自动重启。

**表现**：某个服务（如 triage-service）被手动 stop 后，前端报 "xxx-service unavailable"，但 `docker compose ps` 显示容器 Exited。

**恢复方法**：
```bash
docker start scb-triage-service          # 单个容器
docker start scb-patient-service scb-doctor-service  # 多个
```

**注意**：`docker compose up -d` 也不会重启被手动 stop 的容器（它只处理不存在/需要创建的容器）。要强制重建所有容器用 `--force-recreate`，但这会导致短暂服务中断。

**Mac M5 特殊问题**：`deploy/.env` 需要设置 `KINGBASE_IMAGE=kingbase_v009r001c010b0004_single_arm:v1`，否则 `docker compose up -d` 会试图 pull x86 镜像失败。队友 Windows 不需要此文件（已在 .gitignore 中）。

### 16. 号源查询用 startTime 而非 endTime（致命）

**问题**：`RegistrationService.slots()` 使用 `startTime >= now` 过滤号源，正确应该用 `endTime >= now`。

**表现**：管理端排班管理页面显示已发布的排班（如今天09:00-23:59），但患者端"可预约号源"页面显示号源=0。

**根因**：查询阈值 = now - 1min，09:00-23:59的排班在20:00查询时，startTime=09:00 < 20:00 被排除，虽然 endTime=23:59 > 20:00 实际仍可用。

**修复**：
1. `AppointmentSlotRepository` 新增：`findByStatusAndEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc()`
2. `RegistrationService.slots()` 替换查询方法
3. 检查 `DoctorScheduleService.slots()` 是否有同样问题

**关键文件**：
- `backend/registration-service/.../RegistrationService.java:142-151`
- `backend/registration-service/.../AppointmentSlotRepository.java`

### 17. 医生端硬编码假数据（验收违规）

**问题**：医生端多个页面存在硬编码的测试数据，违反验收标准"不写死业务演示数据"。

**表现**：
- `DoctorLoginPage.vue:10` — 硬编码测试凭据 `doctor_chen / 123456`（种子账号应为 `13900000001`）
- `DoctorLoginPage.vue:44-57` — 硬编码假数据：今日队列18、AI完成率92%、高风险2
- `ConsultationPage.vue:374-376` — 硬编码生命体征：37岁/女/38.1°C

**修复**：
1. 登录页改为占位符或正确种子账号
2. 信号面板改为动态数据或移除
3. 接诊页生命体征从患者信息/分诊记录动态读取

### 18. AuthService调试后门

**问题**：`AuthService.java:115-120` 的 `doctorByDemoAccount()` 允许通过 `doctor1`, `doctor2` 等格式绕过手机号查找直接登录。

**表现**：输入 `doctor1` 会查 doctor 表 id=1 的医生，输入 `doctor2` 查 id=2，以此类推。

**风险**：生产环境安全隐患。

**修复**：用环境变量控制是否启用，或直接移除。

### 19. 患者端 loading 状态复用导致列表消失

**问题**：`PrescriptionsPage.vue` 的 `openDetail()` 复用了列表的 `loading` ref，导致点"详情"按钮时整个列表被 `LoadingState` 覆盖，列表内容消失。

**修复**：列表刷新和详情加载必须用独立的 ref（`loading` / `detailLoading`）。

**教训**：一个页面里有多个异步操作时，每个操作的 loading 状态必须独立管理。这是患者端最常见的代码质量问题。

### 20. 患者端删除操作无确认直接执行

**问题**：`VisitPeoplePage.vue` 的 `deleteVisitor()` 直接调用 API 删除就诊人，没有 ConfirmDialog。用户误触即不可恢复。

**修复**：所有删除/取消/危险操作必须经过确认对话框。详见患者端页面规范"删除/危险操作确认"。

### 21. 患者端路由硬编码导致重定向依赖

**问题**：`TriageResultModal` 用 `router.push('/doctors')` 跳转，依赖旧路径的兼容重定向（`/doctors` → `/patient-services/doctors`）。如果移除重定向就会 404。

**修复**：统一用 `router.push({ name: 'patient-doctors' })` 命名路由，不要依赖重定向。

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

## 患者端页面规范（patient-web 专用）

以下规则基于 2026-06-30 全面审查总结，**违反任何一条都会被驳回**。

### 异步操作 loading 分离

- 每个独立的异步操作必须有自己的 `ref(false)` loading 状态
- **禁止多个异步操作共享同一个 loading**（如列表刷新 + 详情加载共用 `loading`）
- 命名约定：`loading`（列表/页面级刷新）、`detailLoading`（详情加载）、`saving`（提交/删除等写操作）
- 模板中 `LoadingState` 的 `v-if` 必须绑定正确的 loading ref，不能错绑

### 删除/危险操作确认

- 所有删除操作必须经过 `ConfirmDialog`（shared-ui）或等效确认对话框
- 确认框必须说明后果（如"取消后需要重新选择号源""就诊人数据将被删除"）
- 事件命名：`@confirm`（确认执行）、`@close`（取消/关闭）
- 确认框的 `tone` 属性：删除/危险操作用 `"danger"`，普通确认用默认

### 异步导航

- **禁止硬编码路径字符串**（如 `router.push('/doctors')`）
- 必须用命名路由：`router.push({ name: 'patient-doctors' })`
- 需要带参数时：`router.push({ name: 'xxx', query: { q: 'value' } })`
- 异步操作后的跳转用 `await router.push(...)` 确保导航完成

### 生命周期

- 异步数据加载统一用 `onMounted()` 调用，不要在 `<script setup>` 顶层直接调用 async 函数
- 延时操作（`setTimeout` 等）必须在 `onBeforeUnmount` 中清理，防止组件销毁后回调执行
- 顶层直接调用 async 函数的时序不如 `onMounted` 可预测，且不利于测试

### Toast

- 优先用 `inject<Ref<InstanceType<typeof Toast>>>("toast")` 注入 Layout 提供的 Toast
- inject 失败时必须 fallback 创建本地 Toast ref：`const toast = ref<InstanceType<typeof Toast>>()`
- Toast 调用一律用可选链：`toast?.value?.success(...)` / `toast?.value?.error(...)`
- **禁止假设 toast 一定存在**

### Drawer / 自定义弹层

- 必须支持 Escape 键关闭（`@keydown.escape` 或 `@keydown.escape.prevent`）
- 必须支持点击遮罩关闭（`@click.self`）
- 打开时自动 focus（`ref` + `nextTick(() => el.value?.focus())`），使 Escape 键可工作
- 角色标记：`role="dialog"` + `aria-modal="true"`

### 状态三件套

- 每个数据展示页面必须包含：`LoadingState` + `ErrorState` + `EmptyState`
- `LoadingState`：在 `!loaded && loading` 时显示（仅首次加载）
- `ErrorState`：在 `error` 非空时显示，message 必须包含具体原因
- `EmptyState`：在 `loaded` 且数据为空时显示
- 三者互斥，不能同时显示多个

### 假数据/占位数据

- 页面中使用的假数据/占位数据必须在页面顶部用 Notice/Banner 标注
- 标注文案格式："当前展示的是示例数据，正式数据接入后更新。"
- **假数据和真数据不能混在一起展示**（如用病历数据假装检查报告数据）
- 标注位置：页面标题下方、主内容区域上方

### 文件结构

- 单文件组件结构顺序：`<script setup>` → `<template>` → `<style scoped>`
- 禁止顺序：`<template>` → `<script>` → `<style>`
- 患者端页面单文件上限 **300 行**（比三端通用的 400 行更严格，因为患者端页面相对简单）
- 超过 300 行必须拆分：逻辑抽 composable（`useXxx.ts`），UI 拆子组件

### 错误恢复 UX

- API 请求失败后，页面必须提供重试入口（按钮或文字链）
- `ErrorState` 的 message 必须包含具体原因（"挂号记录加载失败"而不是"加载失败"）
- error 清除时机：用户点击重试时、用户切换筛选条件时
- 静默刷新（轮询/定时同步）失败时不要清除 error，避免闪烁

### Toast inject 兜底

- Layout（`PatientPortalLayout`）已 provide `toast` ref
- 页面优先 inject 使用
- inject 失败时必须 fallback 创建本地 Toast
- 所有 toast 调用用可选链，不要假设 toast 一定存在

### usePageAsync composable（推荐）

- 新增页面优先使用 `usePageAsync` composable 管理 loading/error/refresh
- 不要自己手动管理 `loading` / `loaded` / `error` 三件套
- `usePageAsync` 提供标准的异步操作模式：自动 loading 状态、错误处理、Toast 反馈
- 用法：`const { loading, loaded, error, refresh } = usePageAsync({ fetchFn: loadData, errorMessage: '数据加载失败' })`

## AI 助手注意事项

- shared-ui 组件依赖 Tailwind 类名，新增/修改组件时确保 Tailwind 可用
- 不要删除 `tailwind.config.js` 中的 `content` 配置里的 shared-ui 路径
- 遇到 CSS 视觉问题，先用 `browser_console` 的 `getBoundingClientRect()` 量实际渲染数据，不要只读代码推断
- 前端开发用 Vite dev server（见踩坑记录 14），改代码后浏览器自动热更新，不需要手动 build。只有提交部署时才需要 `corepack pnpm build:*`
- 全中文项目内容，保留英文专有名词
