# 18 答辩演示脚本与截图清单

## 1. 开场说明

推荐开场：

> 我们的项目是智慧云脑诊疗平台，重点不是做一个静态 AI 页面，而是做患者、医生、管理员三端贯通的真实诊疗流程。前端所有核心数据都通过 Gateway 调后端服务，数据来自 KingbaseES 兼容数据库。AI 当前使用 Mock Provider，但请求链路走 `ai-service`，所以 Mock 的只是模型输出，不是业务流程。

## 2. 患者端演示

1. 打开患者端 `http://localhost:5173`。
2. 使用 `13800000001 / 123456` 登录。
3. 输入“咽痛、低热、鼻塞两天”，提交 AI 分诊。
4. 说明链路：患者端 -> Gateway -> `triage-service` -> `ai-service` -> Mock Provider。
5. 展示推荐科室、医生和分诊原因。
6. 进入号源列表，选择数据库中可预约号源完成挂号。
7. 取消一条挂号，再重新预约一条有效挂号。

## 3. 医生端演示

1. 打开医生端 `http://localhost:5174`。
2. 使用 `doctor1 / 123456` 登录。
3. 展示医生只能看到自己的待接诊队列。
4. 选中患者挂号，点击生成 AI 病历草稿。
5. 说明病历草稿来自 `medical-record-service` 调 `ai-service`。
6. 医生确认或修改后保存正式病历。
7. 执行 AI 处方审核并确认处方。
8. 展示风险提示和医生端通知。

## 4. 管理端演示

1. 打开管理端 `http://localhost:5175`。
2. 使用 `admin / 123456` 登录。
3. 展示科室、医生、药品、Prompt、知识库和系统字典维护。
4. 生成 AI 排班建议。
5. 说明当前排班建议由 Mock AI 返回，但发布动作会写入医生排班和可预约号源。
6. 发布排班后回到患者端刷新号源，确认新增号源可见。
7. 打开分诊工作台，查看分诊记录详情，演示分配医生和关闭记录。

## 5. 技术亮点讲解

- Gateway 统一入口：三端前端不直接访问业务服务。
- 纯微服务拆分：认证、患者、医生、挂号、分诊、病历、处方、AI、通知、管理职责清晰。
- KingbaseES 事实库：演示数据、流程数据和管理数据均可查库验证。
- RabbitMQ + Outbox：处方风险事件异步投递，避免主事务成功但消息丢失。
- WebSocket：医生端实时接收风险通知。
- AI 服务化：当前 Mock Provider 可稳定演示，后续可替换真实模型。
- 人机协作边界：病历、处方和排班建议都需要医生或管理员确认。

## 6. Docker 部署说明

答辩可展示命令：

```powershell
cd D:\smart_cloud_brain
copy deploy\env\.env.example deploy\env\.env
docker-compose --env-file deploy\env\.env -f deploy\docker-compose.yml --profile embedded-db up -d --build
```

说明：

- Compose 启动 Gateway、各业务微服务、三端 Web、Nacos、RabbitMQ、Nginx 和兼容数据库。
- `sql/kingbase_schema.sql` 初始化基础表和演示数据。
- Maven 本地仓库固定为 `D:\DEVELOP\maven`，后端离线编译命令已写入 README 和验收清单。

## 7. 截图清单

- 患者端登录成功。
- 患者端 AI 分诊结果。
- 患者端号源列表和预约成功。
- 医生端待接诊队列。
- 医生端 AI 病历生成和保存。
- 医生端处方审核结果。
- 医生端风险通知列表。
- 患者端病历与处方记录。
- 管理端基础数据维护。
- 管理端 AI 排班建议、详情和发布结果。
- 管理端分诊工作台详情、改派和关闭结果。
- 数据库中 `appointment_slot`、`doctor_schedule`、`registration`、`medical_record`、`prescription`、`notification_message` 查询结果。
- RabbitMQ 管理台队列或连接状态。
- Docker Compose 服务运行状态。

