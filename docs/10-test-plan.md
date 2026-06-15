# 10 测试计划

## 后端验证

- 运行 Maven 编译，确认所有服务可编译。
- 覆盖权限场景：
  - 患者只能创建、查看、取消自己的挂号。
  - 医生只能处理自己的挂号、病历和处方。
  - 管理员才能访问 `/api/admin/**`。
- 覆盖核心接口：
  - `/api/triage/consult`
  - `/api/registration/slots`
  - `/api/registration/create`
  - `/api/registration/cancel`
  - `/api/medical-record/generate`
  - `/api/medical-record/save`
  - `/api/prescription/check`
  - `/api/prescription/create`
  - `/api/admin/schedule/generate`
  - `/api/admin/schedule/publish`
  - `/api/admin/triage-desk/list`
  - `/api/admin/dict/list`

## 前端验证

- 运行三个 Web 端构建：
  - `patient-web`
  - `doctor-web`
  - `admin-web`
- 验证页面无乱码、无明显布局溢出，按钮禁用状态符合当前流程。
- 验证医生端 WebSocket 可接收处方风险通知，并能刷新通知列表。

## 数据库验证

- 使用 `sql/kingbase_schema.sql` 初始化空库。
- 确认基础数据存在：
  - 科室、医生、患者、管理员、药品、Prompt、知识库、系统字典。
  - `doctor_schedule` 有已发布排班。
  - `appointment_slot` 有可预约号源且 `remaining_capacity > 0`。

## 端到端验收

1. 患者登录后提交 AI 分诊。
2. 患者选择号源并创建挂号。
3. 患者取消一条挂号，状态变为 `CANCELLED`。
4. 患者重新挂号，医生端看到队列。
5. 医生生成并保存病历。
6. 医生审核处方并确认处方。
7. 患者端查看病历和处方。
8. 管理员生成 AI 排班建议并发布。
9. 患者端刷新后可看到新发布号源。
10. 管理员在分诊工作台分配医生并关闭记录。

## 默认账号

- 患者：`13800000001 / 123456`
- 医生：`doctor1 / 123456`
- 管理员：`admin / 123456`
