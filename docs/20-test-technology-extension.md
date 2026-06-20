# 测试技术扩展说明

## 运行命令

后端单元测试与 JaCoCo 覆盖率报告：

```powershell
cd D:\smart_cloud_brain\backend
mvn test
```

前端 Vitest 单元测试：

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm test
```

前端 Vitest 覆盖率报告：

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm test:coverage
```

## 后端测试内容

- 使用 JUnit 5、Mockito 和 Spring Boot Test。
- 使用 JaCoCo 自动生成覆盖率报告。
- `backend/pom.xml` 统一配置 JaCoCo，排除 `Application`、`entity`、`dto`、`repository`、`config` 等样板代码，重点统计服务、控制器、安全和异常处理逻辑。
- 本轮补充测试覆盖：
  - `common-lib`：JWT 签发/验签、密码哈希兼容、统一异常处理。
  - `auth-service`：患者注册、重复手机号、三类角色登录、旧密码升级、演示医生账号。
  - `patient-service`：当前患者资料、内部患者摘要、空字段默认值、患者不存在。
  - `triage-service`：AI 分诊落库、降级状态、越权患者 ID、角色范围列表、分诊台详情/指派/关闭。
  - `registration-service`：号源挂号扣减、满号状态、重复挂号冲突、取消恢复号源、医生完成接诊、角色范围列表。
- 现有测试继续覆盖医生排班、病历权限、处方权限、AI provider、通知、管理端缓存和目录能力。

## 前端测试内容

- 使用 Vitest、V8 Coverage、jsdom、Vue Test Utils 和 Pinia。
- `frontend/package.json` 新增：
  - `test`: `vitest run`
  - `test:coverage`: `vitest run --coverage`
- `frontend/vitest.config.ts` 统一配置测试环境、覆盖率报告和阈值。
- 本轮补充测试覆盖：
  - API 请求封装：GET/POST、query 参数、Authorization、业务错误、HTTP 错误、网络错误、401 事件、空响应、非法 JSON。
  - API 路由矩阵：患者端、医生端、管理端主要接口 wrapper。
  - 工具函数：数字转换、字段展示、状态样式、状态文本、错误格式化、WebSocket/SSE URL。
  - Pinia store：登录态保存/加载/退出、角色校验、未授权事件、患者/医生/管理员工作流刷新。
  - 共享 UI：状态标签、空/错/加载状态、数据表、弹窗、抽屉、确认对话框。

## 报告路径

后端 JaCoCo HTML 报告：

```text
backend\common-lib\target\site\jacoco\index.html
backend\auth-service\target\site\jacoco\index.html
backend\patient-service\target\site\jacoco\index.html
backend\doctor-service\target\site\jacoco\index.html
backend\registration-service\target\site\jacoco\index.html
backend\triage-service\target\site\jacoco\index.html
backend\medical-record-service\target\site\jacoco\index.html
backend\prescription-service\target\site\jacoco\index.html
backend\ai-service\target\site\jacoco\index.html
backend\notification-service\target\site\jacoco\index.html
backend\admin-service\target\site\jacoco\index.html
```

前端 Vitest Coverage HTML 报告：

```text
frontend\coverage\index.html
```

## 本轮验证结果

- 后端：`mvn test` 通过，66 个 JUnit 测试用例，0 failures，0 errors，0 skipped。
- 前端：`corepack pnpm test` 通过，3 个测试文件，16 个 Vitest 测试用例。
- 前端覆盖率：`corepack pnpm test:coverage` 通过；覆盖率统计范围为共享 API 与已测试共享 UI 组件，总体约 95% 行覆盖、95% 语句覆盖、94% 函数覆盖。

## 截图建议

- 截取 `mvn test` 最后的 `BUILD SUCCESS` 和测试总数。
- 打开任一后端 JaCoCo HTML 报告并截图，例如 `backend\triage-service\target\site\jacoco\index.html`。
- 截取 `corepack pnpm test:coverage` 的覆盖率表格。
- 打开 `frontend\coverage\index.html` 并截图。
