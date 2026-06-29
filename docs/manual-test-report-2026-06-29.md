# 三端模拟手工测试报告（2026-06-29）

## 结论摘要

- 前端自动化测试通过：10 个测试文件，57 个用例全部通过。
- 三端生产构建通过：patient-web、doctor-web、admin-web 均可打包，Tailwind/PostCSS 链路可用。
- 后端指定微服务离线编译通过。
- 后端单元测试通过：32 个 Surefire 报告，110 个用例全部通过。
- 运行态端到端测试未能完整执行：本机 Docker daemon 未运行，Gateway `127.0.0.1:18080`、患者端 `5173`、医生端 `5174` 均未监听。

本报告因此分为两类结果：

- 已验证：构建、单元测试、接口映射、Gateway 路由、代码层链路。
- 待运行环境恢复后复测：浏览器页面点击、真实登录、数据库写入、RabbitMQ/WebSocket 实时通知。

## 已执行验证

| 类别 | 命令/检查 | 结果 |
|---|---|---|
| 前端测试 | `corepack pnpm test` | 通过，57 passed |
| 患者端构建 | `corepack pnpm build:patient` | 通过 |
| 医生端构建 | `corepack pnpm build:doctor` | 通过 |
| 管理端构建 | `corepack pnpm build:admin` | 通过，有 ECharts chunk 超 500 kB 提示 |
| 后端编译 | 文档指定 Maven 离线 compile 命令 | 通过 |
| 后端单测 | 同模块 Maven 离线 test 命令 | 通过，110 tests |
| Docker 状态 | `docker ps` | 失败，Docker Desktop Linux engine 未运行 |
| 端口探测 | `127.0.0.1:18080/5173/5174` | 未监听 |

## 核心三端交互链路

### 链路 1：患者 AI 分诊 -> 管理端分诊工作台

| 步骤 | 页面/API | 预期 |
|---|---|---|
| 患者登录 | 患者端 `/login`，`13800000001 / 123456`；`POST /api/patient/login` | 返回 PATIENT token，进入患者工作台 |
| 提交主诉 | `/patient-services/triage`；`POST /api/triage/consult` | 返回推荐科室、医生、原因、provider/degraded |
| 后端链路 | Gateway -> triage-service -> ai-service | AI 结果必须由 ai-service 产生，Mock 也需标记 degraded/provider |
| 数据落库 | `triage_record` | 新增分诊记录 |
| 管理查看 | 管理端 `/triage-desk`；`GET /api/admin/triage-desk/list` | 能看到患者刚提交的分诊记录 |
| 管理处理 | `POST /api/admin/triage-desk/assign` / `close` | 可改派医生、关闭记录 |

代码对照：前端 API、后端 Controller、Gateway 路由均存在；AI 降级链路由 `ai-service` 的 orchestration/fallback 实现覆盖。运行态待复测。

### 链路 2：患者号源预约 -> 取消 -> 重新预约 -> 医生队列

| 步骤 | 页面/API | 预期 |
|---|---|---|
| 患者看号源 | `/patient-services/doctors`；`GET /api/registration/slots` | 号源来自 `appointment_slot`，只展示可预约余量 |
| 创建挂号 | `POST /api/registration/create` | 新增 `registration`，号源余量减少 |
| 查看我的挂号 | `/patient-services/appointments`；`GET /api/registration/list` | 只看到当前患者自己的挂号 |
| 取消挂号 | `POST /api/registration/cancel` | 挂号状态变更，号源余量回补 |
| 重新预约 | 再次调用 create | 产生一条有效挂号 |
| 医生查看 | 医生端 `/queue`；`GET /api/registration/list` 或 `/api/doctor/queue` | 医生只能看到自己的待接诊队列 |

代码对照：挂号权限测试存在并通过；医生队列相关接口已在 Controller 中实现。运行态待复测。

### 链路 3：医生接诊 -> AI 病历草稿 -> 医生确认保存 -> 患者查看

| 步骤 | 页面/API | 预期 |
|---|---|---|
| 医生登录 | 医生端 `/login`，文档脚本写 `doctor1 / 123456` | 需确认实际种子账号；SQL 当前医生手机号为 `13900000001` |
| 选择挂号 | `/consult/:registrationId` | 进入接诊页 |
| 生成草稿 | `POST /api/medical-record/generate` | medical-record-service 调 ai-service 返回草稿 |
| 保存病历 | `POST /api/medical-record/save` | 医生确认后才写入 `medical_record` |
| 患者查看 | 患者端 `/patient-services/records`；`GET /api/medical-record/list` | 患者只能看到自己的病历 |

代码对照：病历保存权限测试存在并通过；SSE 生成接口测试通过。运行态待复测。

### 链路 4：医生处方审核 -> 处方确认 -> 风险通知 -> 患者查看处方

| 步骤 | 页面/API | 预期 |
|---|---|---|
| 医生录入处方 | 医生端处方/接诊页 | 处方保存前先审核 |
| AI 审核 | `POST /api/prescription/check` | prescription-service 调 ai-service 返回风险等级和提示 |
| 确认处方 | `POST /api/prescription/create` | 写入 `prescription` / `prescription_item` |
| 高风险通知 | RabbitMQ -> notification-service -> WebSocket | 医生端通知列表/实时通知可见 |
| 患者查看 | `/patient-services/prescriptions`；`GET /api/prescription/list` | 患者只能看到自己的处方 |

代码对照：处方权限测试、通知服务测试通过；RabbitMQ/WebSocket 运行态待复测。

### 链路 5：管理端 AI 排班 -> 发布号源 -> 患者端刷新可见

| 步骤 | 页面/API | 预期 |
|---|---|---|
| 管理登录 | 管理端 `/login`，`admin / 123456` | 返回 ADMIN token |
| 生成建议 | `/schedule`；`POST /api/admin/schedule/generate` | 返回建议列表，包含 provider/degraded |
| 管理确认发布 | `POST /api/admin/schedule/publish` | 写入 `doctor_schedule` 和 `appointment_slot` |
| 患者刷新号源 | 患者端 `/patient-services/doctors` | 新发布号源可见 |

代码对照：管理端排班接口、内部医生排班发布接口、AI schedule validator 均存在；运行态待复测。

### 链路 6：管理基础数据维护 -> 患者/医生侧消费

| 维护对象 | 管理端 API | 消费侧预期 |
|---|---|---|
| 科室 | `/api/admin/department/list/save` | 患者号源、医生列表、分诊推荐展示最新科室 |
| 医生 | `/api/admin/doctor/list/save` | 患者可按科室筛选医生，挂号指向有效医生 |
| 药品 | `/api/admin/drug/list/save` | 医生处方检索使用后端药品数据 |
| Prompt | `/api/admin/prompt-template/list/save/test` | AI 任务提示词可维护、可测试 |
| 知识库 | `/api/admin/knowledge/list/save` | 搜索和 AI 上下文可读取 |
| 字典 | `/api/admin/dict/list/save` | 状态中文标签、选项来源保持一致 |

代码对照：API 封装和 Controller 映射均存在；页面 CRUD 运行态待复测。

### 链路 7：权限隔离负向链路

| 场景 | 预期 |
|---|---|
| 未登录访问患者个人数据 | 401 或跳登录 |
| 患者 token 调管理端接口 | 拒绝访问 |
| 患者查看他人挂号/病历/处方 | 后端过滤或拒绝 |
| 医生查看非本人待接诊 | 后端过滤或拒绝 |
| 非内部调用访问 `/internal/**` | 拒绝访问 |

代码对照：Gateway JWT、管理端权限、挂号/病历/处方权限单测均通过；运行态需用真实 token 复测。

## 发现的问题与风险

### P0：本次无法完成真实浏览器端到端测试

Docker Desktop Linux engine 未运行，Gateway 和三端端口未监听。当前无法验证真实登录、页面点击、数据库写入、RabbitMQ/WebSocket 通知。

复测前置：

1. 启动 Docker Desktop。
2. 按文档启动后端与依赖：`docker compose --env-file deploy\env\.env -f deploy\docker-compose.yml up -d --build`，若本机仅支持旧 CLI 则用 `docker-compose`。
3. 开发模式建议停止 `scb-nginx` 后分别启动三个 Vite dev server。

### P1：医生登录凭据文档存在冲突

验收清单/演示脚本写医生端使用 `doctor1 / 123456`，但 AGENTS.md 与 SQL 种子数据显示医生账号是手机号 `13900000001 / 123456`。如果登录接口按 `account` 查询手机号，演示脚本中的 `doctor1` 会失败。

建议复测时优先使用 `13900000001 / 123456`，并统一文档。

### P1：患者公网展示页仍存在硬编码 fallback 业务数据

源码中存在患者端公共科室/医生介绍 fallback 数据，例如：

- `frontend/apps/patient-web/src/pages/DepartmentsPage.vue`
- `frontend/apps/patient-web/src/pages/DoctorTeamPage.vue`
- `frontend/apps/patient-web/src/pages/PatientMessagesPage.vue`

如果后端数据为空或接口异常，页面可能展示静态业务内容。这与“页面不展示硬编码业务假数据 / 三个 Web 端不写死业务演示数据”的验收口径存在冲突。核心挂号页已使用后端号源 API，但公开信息页仍需确认是否允许 fallback。

### P2：管理端构建存在大 chunk 警告

admin-web 构建通过，但 ECharts chunk 约 536 kB，Vite 给出超过 500 kB 提示。不是功能阻断，但会影响首屏加载体感，可后续拆分图表 chunk。

### P2：PostCSS 配置有模块类型提示

三端构建均提示 `postcss.config.js` 未声明模块类型，被 Vite 重新按 ES module 解析。不是功能阻断，但可通过统一配置格式或 package type 消除噪声。

## 运行态复测清单

恢复 Docker/Gateway 后，按以下顺序执行最省时间：

1. `GET http://127.0.0.1:18080/actuator/health` 确认 Gateway 健康。
2. 患者登录，提交“胸痛、气短两天，活动后加重”，确认分诊结果和 `triage_record`。
3. 管理端登录，打开分诊工作台，确认能看到该分诊记录并改派/关闭。
4. 管理端生成并发布排班，患者端刷新号源，确认新增号源可预约。
5. 患者预约、取消、重新预约，确认 `registration` 状态和号源余量。
6. 医生登录，确认只看到自己队列。
7. 医生生成并保存病历，患者端确认病历可见。
8. 医生执行处方审核并确认处方，患者端确认处方可见。
9. 若处方高风险，确认 RabbitMQ 队列、通知表、医生端 WebSocket/通知列表。
10. 使用患者 token 访问管理接口、使用医生 token 访问他人数据，确认权限拒绝。
