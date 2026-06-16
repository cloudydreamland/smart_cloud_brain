# 10 测试计划

## 1. 后端验证

统一编译命令：

```powershell
cd D:\smart_cloud_brain\backend
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=D:\DEVELOP\maven" -o -pl gateway-service,auth-service,patient-service,doctor-service,registration-service,triage-service,medical-record-service,prescription-service,notification-service,admin-service,ai-service -am compile -DskipTests
```

重点验证：

- 所有服务可编译。
- 患者只能访问自己的挂号、病历和处方。
- 医生只能访问自己的待接诊数据。
- 管理端接口必须 ADMIN 角色。
- AI 请求全部走 `ai-service`。
- RabbitMQ/WebSocket 通知链路可说明、可演示。

## 2. 前端验证

```powershell
cd D:\smart_cloud_brain\frontend
corepack pnpm test
corepack pnpm --filter patient-web build
corepack pnpm --filter doctor-web build
corepack pnpm --filter admin-web build
```

重点验证：

- 三端均能登录。
- 页面不展示硬编码业务假数据。
- 刷新页面后能重新拉取后端数据。
- 创建、取消、保存、发布后数据状态正确刷新。

## 3. 接口验证

使用 Postman 或页面操作验证：

- `/api/auth/login`
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

## 4. 数据库验证

演示前：

- `department`、`doctor`、`patient`、`admin_user` 有默认数据。
- `appointment_slot` 有可预约号源。

演示后：

- `triage_record` 有分诊记录。
- `registration` 有挂号记录。
- `medical_record` 有病历记录。
- `prescription` 有处方记录。
- `notification_message` 有通知记录。

## 5. 部署验证

```powershell
cd D:\smart_cloud_brain
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

检查：

- Gateway: `http://localhost:18080`
- 患者端: `http://localhost:5173`
- 医生端: `http://localhost:5174`
- 管理端: `http://localhost:5175`
- RabbitMQ 管理台: `http://localhost:15672`

