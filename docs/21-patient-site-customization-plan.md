# Codex 执行任务：患者端站点配置化

## 0. 执行目标

在当前项目中实现“管理端配置患者端展示内容”的 MVP。

最终效果：

- 管理端可以配置患者端导航。
- 管理端可以配置患者端首页部分模块。
- 管理端可以配置患者端静态内容页。
- 管理端支持保存草稿和发布。
- 患者端读取已发布配置并渲染。
- 配置接口失败或配置无效时，患者端必须回退到本地默认配置。

该任务不是建设完整低代码平台。禁止实现拖拽建站、自定义 CSS、自定义 JS、任意页面生成器。

## 1. 当前项目事实

当前相关代码位置：

- 患者端导航：`frontend/apps/patient-web/src/components/PatientSiteHeader.vue`
- 患者端首页：`frontend/apps/patient-web/src/pages/HomePage.vue`
- 患者端静态页：`frontend/apps/patient-web/src/pages/StaticPage.vue`
- 患者端路由：`frontend/apps/patient-web/src/router/index.ts`
- 管理端路由：`frontend/apps/admin-web/src/router/index.ts`
- 管理端布局菜单：`frontend/apps/admin-web/src/layouts/AdminWorkspaceLayout.vue`
- shared API：`frontend/packages/shared-api/src/api.ts`
- shared API types：`frontend/packages/shared-api/src/types.ts`
- 管理端后端入口：`backend/admin-service/src/main/java/com/smartcloudbrain/admin/controller/AdminController.java`
- 管理端已有服务：`backend/admin-service/src/main/java/com/smartcloudbrain/admin/service/AdminCatalogService.java`
- Flyway 目录：`sql/flyway`

注意：

- 当前工作区里旧 `docs` 目录有大量已删除文件，这不是本任务造成的，不要恢复。
- 当前可能存在未跟踪文件，除非本任务需要，不要改动。

## 2. 实现边界

必须实现：

1. 新增患者端站点配置数据库表。
2. 新增后端配置保存、发布、读取接口。
3. 新增患者端公开读取已发布配置接口。
4. 患者端导航从配置读取。
5. 患者端静态页内容从配置读取。
6. 患者端首页至少支持 banner、notice、quick actions 的配置化。
7. 管理端新增“患者端配置”页面。
8. 管理端支持保存草稿、发布配置。
9. 配置校验和前端 fallback。

暂不实现：

- 配置历史回滚 UI。
- 图片上传。
- 多医院/多科室配置。
- 按患者标签配置。
- 完整文章 CMS。
- 拖拽式编辑器。

## 3. 数据库改造

新增 Flyway：

`sql/flyway/V6__patient_site_config.sql`

内容：

```sql
CREATE TABLE IF NOT EXISTS patient_site_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(80) NOT NULL,
  config_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  version INTEGER NOT NULL DEFAULT 1,
  remark VARCHAR(255),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_patient_site_config_key_status_version UNIQUE (config_key, status, version)
);

CREATE INDEX IF NOT EXISTS idx_patient_site_config_key_status
  ON patient_site_config(config_key, status);

INSERT INTO role_permission (role, permission_key, enabled) VALUES
  ('ADMIN', 'patient-site:manage', TRUE)
ON CONFLICT (role, permission_key) DO NOTHING;
```

如果当前 Flyway 方言或已有 SQL 风格需要调整，按项目现有 `sql/flyway` 文件风格处理。

## 4. 配置 Key

只允许以下配置：

```text
patient_nav
patient_home
patient_static_pages
```

后端保存和发布时必须校验 `config_key`，未知 key 返回 400。

## 5. 配置结构

### 5.1 patient_nav

```json
{
  "brand": {
    "name": "智慧云脑",
    "homeRoute": "patient-home"
  },
  "menus": [
    {
      "key": "care",
      "label": "医疗服务",
      "enabled": true,
      "sort": 10,
      "lead": "从需要帮助到获得照护",
      "description": "分诊、挂号、医生科室、检查预约等入口",
      "links": [
        {
          "label": "在线挂号",
          "routeName": "patient-doctors",
          "description": "查看可预约号源",
          "enabled": true,
          "sort": 10
        }
      ],
      "feature": {
        "label": "查看医疗服务",
        "routeName": "service-internet-clinic"
      }
    }
  ],
  "userLinks": [
    {
      "label": "我的预约",
      "routeName": "patient-appointments",
      "enabled": true,
      "sort": 10
    }
  ]
}
```

### 5.2 patient_home

```json
{
  "hero": {
    "eyebrow": "智慧云脑医疗服务",
    "title": "专业照护，从清晰入口开始",
    "primaryAction": {
      "label": "预约就诊",
      "routeName": "patient-doctors"
    },
    "secondaryAction": {
      "label": "AI 智能分诊",
      "routeName": "patient-triage"
    },
    "enabled": true
  },
  "modules": [
    {
      "type": "notice",
      "key": "emergency_notice",
      "enabled": true,
      "sort": 10,
      "content": {
        "level": "warning",
        "text": "如出现胸痛、呼吸困难、意识改变等情况，请立即前往急诊。"
      }
    },
    {
      "type": "quick_actions",
      "key": "quick_actions",
      "enabled": true,
      "sort": 20,
      "content": {
        "items": [
          {
            "label": "在线挂号",
            "routeName": "patient-doctors"
          },
          {
            "label": "AI 分诊",
            "routeName": "patient-triage"
          }
        ]
      }
    }
  ]
}
```

第一期首页模块白名单：

```text
notice
quick_actions
intro
locations
featured_departments
static_content
```

MVP 至少实现：

- `notice`
- `quick_actions`
- `hero`

其他模块可以继续使用当前静态实现，或仅保留默认渲染。

### 5.3 patient_static_pages

```json
{
  "pages": [
    {
      "routeName": "service-internet-clinic",
      "label": "医疗服务",
      "title": "互联网门诊",
      "intro": "面向复诊、病情咨询和诊后随访场景。",
      "enabled": true,
      "points": [
        {
          "title": "适用场景",
          "text": "复诊咨询、检查结果解读、慢病随访和用药问题整理。"
        }
      ],
      "primary": {
        "label": "先做 AI 智能分诊",
        "routeName": "patient-triage"
      }
    }
  ]
}
```

`StaticPage.vue` 应按 `route.name` 匹配页面配置。

## 6. 路由白名单

后端和前端都需要维护允许跳转的路由白名单。

第一期允许：

```text
patient-home
patient-dashboard
patient-triage
patient-doctors
patient-appointments
patient-records
patient-prescriptions
patient-reports
patient-invoices
patient-messages
patient-profile
patient-visitors
public-search
public-departments
public-conditions
public-guide
service-internet-clinic
service-exam-booking
service-inpatient
service-emergency
service-international
doctor-experts
doctor-centers
doctor-schedules
library-symptoms
library-drugs
library-tests
library-rehab
library-articles
ai-symptom
ai-record-summary
ai-medication
ai-assessment
about-hospital
about-news
about-careers
about-contact
```

如果配置中出现未知 `routeName`：

- 后端发布时拒绝。
- 前端渲染时过滤该项。

## 7. 后端实现

### 7.1 新增文件

新增：

```text
backend/admin-service/src/main/java/com/smartcloudbrain/admin/entity/PatientSiteConfig.java
backend/admin-service/src/main/java/com/smartcloudbrain/admin/repository/PatientSiteConfigRepository.java
backend/admin-service/src/main/java/com/smartcloudbrain/admin/dto/admin/PatientSiteConfigSaveRequest.java
backend/admin-service/src/main/java/com/smartcloudbrain/admin/dto/admin/PatientSiteConfigPublishRequest.java
backend/admin-service/src/main/java/com/smartcloudbrain/admin/service/PatientSiteConfigService.java
```

如项目习惯将公开接口放在独立 Controller，可新增：

```text
backend/admin-service/src/main/java/com/smartcloudbrain/admin/controller/PatientSiteConfigController.java
```

也可以先放入 `AdminController`，但公开读取接口建议独立，避免需要管理员权限。

### 7.2 Entity 字段

`PatientSiteConfig` 字段对应数据库表：

- `id`
- `configKey`
- `configJson`
- `status`
- `version`
- `remark`
- `createdBy`
- `updatedBy`
- `createdAt`
- `updatedAt`

继承项目现有 `BaseEntity` 时，避免重复声明 `id`、`createdAt`。

### 7.3 Repository 方法

至少需要：

```java
List<PatientSiteConfig> findByConfigKeyOrderByVersionDesc(String configKey);
Optional<PatientSiteConfig> findFirstByConfigKeyAndStatusOrderByVersionDesc(String configKey, String status);
List<PatientSiteConfig> findByStatus(String status);
```

可按实际实现调整。

### 7.4 管理端接口

新增：

```text
GET  /api/admin/patient-site/config?configKey=patient_nav
POST /api/admin/patient-site/save
POST /api/admin/patient-site/publish
GET  /api/admin/patient-site/history?configKey=patient_nav
```

权限：

- 必须要求 `RoleType.ADMIN`。
- 保存、发布、历史建议要求 `patient-site:manage`。

请求体：

```json
{
  "configKey": "patient_nav",
  "configJson": "{...}",
  "remark": "调整患者端导航"
}
```

发布请求：

```json
{
  "configKey": "patient_nav"
}
```

返回值使用 `Map<String, Object>`，保持当前 admin service 风格。

### 7.5 患者端公开接口

新增：

```text
GET /api/patient-site/config
```

返回：

```json
{
  "nav": {},
  "home": {},
  "staticPages": {}
}
```

公开接口不需要登录。只返回 `PUBLISHED` 状态中最新版本。

如果某个配置没有发布，返回空对象，不抛错：

```json
{
  "nav": {},
  "home": {},
  "staticPages": {}
}
```

### 7.6 后端校验

保存草稿时：

- `configKey` 必须合法。
- `configJson` 必须是合法 JSON。

发布时：

- `configKey` 必须合法。
- 对应草稿必须存在。
- JSON 结构必须符合对应配置类型。
- 所有 `routeName` 必须在白名单内。
- `patient_home.modules[].type` 必须在模块白名单内。
- 关键字段不能为空。

校验失败抛出 `BusinessException(400, "...")`。

### 7.7 发布逻辑

建议逻辑：

1. 找到指定 `configKey` 最新 `DRAFT`。
2. 校验配置。
3. 将同 `configKey` 的旧 `PUBLISHED` 改为 `ARCHIVED`。
4. 将该草稿复制为新的 `PUBLISHED` 记录，`version = maxVersion + 1`。
5. 返回发布后的记录。

如果为了 MVP 简化，也可以直接把草稿状态改为 `PUBLISHED`，但必须保证公开接口只读到一个最新发布版本。

## 8. 前端 shared-api

修改：

```text
frontend/packages/shared-api/src/types.ts
frontend/packages/shared-api/src/api.ts
```

新增类型：

```ts
export type PatientSiteConfigSaveRequest = {
  configKey: string;
  configJson: string;
  remark?: string;
};

export type PatientSiteConfigPublishRequest = {
  configKey: string;
};
```

新增 API：

```ts
patientSiteConfig: () => get<DataRow>("/patient-site/config")
```

admin API 新增：

```ts
patientSiteConfig: (token: string, configKey?: string) => get<DataRow>(...)
savePatientSiteConfig: (token: string, body: PatientSiteConfigSaveRequest) => post<DataRow>(...)
publishPatientSiteConfig: (token: string, body: PatientSiteConfigPublishRequest) => post<DataRow>(...)
patientSiteConfigHistory: (token: string, configKey: string) => get<DataRow[]>(...)
```

也要在最终导出的 `api` 对象里暴露。

## 9. 患者端实现

新增目录：

```text
frontend/apps/patient-web/src/site-config
```

新增文件：

```text
frontend/apps/patient-web/src/site-config/defaultConfig.ts
frontend/apps/patient-web/src/site-config/routeWhitelist.ts
frontend/apps/patient-web/src/site-config/usePatientSiteConfig.ts
frontend/apps/patient-web/src/site-config/types.ts
```

### 9.1 defaultConfig.ts

把当前 `PatientSiteHeader.vue` 中的导航默认值迁移到 `defaultConfig.ts`。

把当前 `StaticPage.vue` 中的静态页默认内容迁移到 `defaultConfig.ts`。

首页默认配置提供：

- hero
- notice
- quick actions

注意：迁移时保持页面现有行为，不要删除核心入口。

### 9.2 usePatientSiteConfig.ts

职责：

- 调用 `api.patientSiteConfig()`。
- 合并接口配置和本地默认配置。
- 过滤非法路由。
- 过滤 disabled 项。
- 按 `sort` 排序。
- 请求失败时返回默认配置。

接口形态建议：

```ts
export function usePatientSiteConfig() {
  const loading = ref(false);
  const config = ref(defaultPatientSiteConfig);
  const load = async () => {};
  return { config, loading, load };
}
```

如果多个组件会重复加载，可以用模块级缓存，避免每个页面重复请求。

### 9.3 PatientSiteHeader.vue

改造：

- 删除组件内写死的 `navMenus` 和 `userLinks`。
- 从 `usePatientSiteConfig()` 获取 `config.nav`。
- 保留当前菜单交互逻辑。
- `routeName` 转换为 `RouterLink` 的 `{ name: routeName }`。
- 配置为空时使用默认配置。

### 9.4 StaticPage.vue

改造：

- 从 `usePatientSiteConfig()` 获取 `config.staticPages.pages`。
- 用 `String(route.name)` 匹配 `routeName`。
- 未命中时使用现有 fallback。
- `primary.routeName` 转换为 `{ name: routeName }`。

### 9.5 HomePage.vue

MVP 改造：

- hero 文案和按钮从配置读取。
- notice 从配置读取。
- quick actions 从配置读取。

其他现有模块可以暂时保留静态渲染。

## 10. 管理端实现

新增页面：

```text
frontend/apps/admin-web/src/pages/PatientSiteConfigPage.vue
```

修改：

```text
frontend/apps/admin-web/src/router/index.ts
frontend/apps/admin-web/src/layouts/AdminWorkspaceLayout.vue
```

新增路由：

```ts
{ path: "patient-site", name: "admin-patient-site", component: PatientSiteConfigPage }
```

侧边栏“配置”分组新增：

```text
患者端配置 -> /patient-site
```

页面结构：

- Tabs：导航配置、首页配置、静态页配置。
- 每个 Tab 显示当前配置 JSON。
- 支持编辑 JSON。
- 支持保存草稿。
- 支持发布。
- 支持重新加载。

MVP 可以使用 JSON 编辑文本域，不强制做完整结构化表单。

但需要：

- 提交前检查 JSON.parse。
- 保存成功后提示。
- 发布成功后提示。
- 接口错误显示错误信息。

## 11. 样式要求

遵循当前管理端页面风格，不新增大型设计系统。

管理端页面应包含：

- 页面标题
- 配置类型 Tab
- 编辑区域
- 操作按钮：重新加载、保存草稿、发布
- 简短状态提示

不要做营销式 hero，不要增加复杂视觉装饰。

## 12. 测试要求

### 12.1 后端测试

新增或扩展测试，至少覆盖：

- 保存合法配置成功。
- 保存非法 JSON 失败。
- 保存未知 `configKey` 失败。
- 发布未知路由失败。
- 发布未知模块失败。
- 发布成功后公开接口可读取。
- 未发布配置不出现在公开接口。

测试位置按项目现有风格放在：

```text
backend/admin-service/src/test/java/com/smartcloudbrain/admin
```

### 12.2 前端测试

如果现有 Vitest 环境可用，补充：

- 配置 fallback。
- route 白名单过滤。
- sort 排序。
- disabled 过滤。

可放在：

```text
frontend/apps/__tests__
```

或靠近实现文件。

## 13. 验证命令

优先运行：

```powershell
cd frontend
pnpm test
pnpm build
```

后端优先运行：

```powershell
cd backend
mvn test -pl admin-service -am
```

如果项目现有命令不同，先查看 `package.json` 和 `pom.xml` 后选择等价命令。

如遇到 Windows PowerShell 编码显示乱码，不要因此重写业务文本；先确认文件实际编码是否为 UTF-8。

## 14. 验收流程

手动验收：

1. 启动后端和前端。
2. 管理员登录管理端。
3. 进入“患者端配置”。
4. 修改 `patient_nav`，新增或隐藏一个导航入口。
5. 保存草稿。
6. 发布。
7. 刷新患者端，确认导航变化生效。
8. 修改 `patient_static_pages` 中某个页面标题。
9. 发布。
10. 访问对应患者端页面，确认标题变化。
11. 模拟配置接口失败，确认患者端仍显示默认导航和默认页面。

## 15. 完成标准

任务完成时必须满足：

- 新增数据库迁移。
- 后端配置接口可用。
- 患者端公开配置接口可用。
- 管理端有可操作页面。
- 患者端导航可被管理端配置影响。
- 患者端静态页内容可被管理端配置影响。
- 首页 hero、notice、quick actions 至少部分配置化。
- 配置错误不会导致患者端白屏。
- 有必要测试，或明确说明未能运行的测试和原因。

## 16. 实施顺序

严格按以下顺序做：

1. 数据库迁移。
2. 后端 Entity、Repository、DTO、Service。
3. 后端管理接口和公开接口。
4. 后端校验和测试。
5. shared-api 类型和 API。
6. 患者端默认配置、白名单、加载 composable。
7. 改造患者端 Header。
8. 改造患者端 StaticPage。
9. 改造患者端 HomePage 的 MVP 模块。
10. 管理端新增页面和菜单。
11. 前端测试。
12. 构建和回归验证。

## 17. 禁止事项

- 不要恢复已删除的旧 docs 文件。
- 不要重构无关业务模块。
- 不要改医生端。
- 不要改患者端核心业务接口。
- 不要让管理员配置任意 URL、JS、CSS。
- 不要移除本地默认配置。
- 不要在配置接口失败时阻断患者登录、挂号、分诊、病历、处方等核心功能。
