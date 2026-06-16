# 17 阶段验收清单

## 1. 主业务链路

- 患者可使用 `13800000001 / 123456` 登录。
- 患者提交主诉后可获得 AI 分诊建议，建议来自后端 `ai-service`。
- 分诊记录写入数据库，管理端可查看。
- 患者可从数据库号源中选择医生排班并完成预约。
- 患者可取消自己的挂号。
- 医生可使用 `doctor1 / 123456` 登录。
- 医生登录后只能看到自己的待接诊挂号。
- 医生可生成 AI 病历草稿，并确认保存正式病历。
- 医生可执行 AI 处方审核并确认处方。
- 高风险处方可通过 RabbitMQ/WebSocket 形成医生端通知。
- 患者可查看自己的病历和处方记录。
- 管理员可使用 `admin / 123456` 登录。
- 管理员可维护科室、医生、药品、Prompt、知识库和系统字典。
- 管理员可生成 AI 排班建议，并发布为可预约号源。
- 管理员可查看分诊工作台，完成改派和关闭记录。

## 2. 真实流程检查

- 三个 Web 端不写死业务演示数据。
- 前端请求统一进入 Gateway。
- 核心业务数据来自 KingbaseES/PostgreSQL 兼容数据库。
- AI 当前为 Mock Provider，但必须走 `ai-service`。
- 病历和处方必须由医生确认后保存。
- 管理端发布排班后，患者端刷新可看到新增号源。

## 3. 必交材料

- 源代码：后端、Vue 3 Web 三端、uni-app 移动端、SQL、Postman。
- 文档：系统设计、数据库设计、接口设计、测试计划、部署说明、AI 使用说明、任务覆盖说明。
- 截图或录屏：三端页面、Postman 接口、数据库表、完整业务链路。
- 测试记录：Maven 编译、前端测试和构建、数据库初始化验证。

## 4. 验收命令

后端：

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

前端：

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm test
corepack pnpm --filter patient-web build
corepack pnpm --filter doctor-web build
corepack pnpm --filter admin-web build
```

## 5. 数据库验收

- 空库执行 `sql/kingbase_schema.sql` 成功。
- `department`、`doctor`、`patient`、`admin_user`、`drug`、`prompt_template`、`knowledge_entry` 有演示数据。
- `system_dict` 有状态字典。
- `doctor_schedule` 有已发布排班。
- `appointment_slot` 有 `remaining_capacity > 0` 的可预约号源。
- 演示后可查询到新增 `registration`、`medical_record`、`prescription`、`notification_message`。

## 6. 明确不做

- 不做住院业务。
- 不做真实支付、医保、药房库存和真实发药。
- 不把 AI 输出作为真实医疗诊断结论。

