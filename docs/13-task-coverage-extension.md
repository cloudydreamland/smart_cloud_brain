# 任务覆盖与必做任务方案

## 1. 统一交付目标

项目交付目标为“基础诊疗闭环 + Spring Cloud 微服务 + AI 辅助能力 + Web 三端演示”。患者端、医生端、管理端均为必做入口，前端统一通过 Gateway 调后端服务，核心数据来自 KingbaseES/PostgreSQL 兼容数据库。

## 2. 必做任务覆盖

| 必做任务 | 当前方案 | 验收重点 |
|---|---|---|
| 患者登录 | `auth-service` 签发 JWT | `13800000001 / 123456` 可登录 |
| AI 智能分诊 | `triage-service` 调 `ai-service` | 分诊记录写入数据库，前端展示后端结果 |
| 在线挂号 | `registration-service` 使用号源 | 号源来自 `appointment_slot`，预约扣减余号 |
| 医生工作台 | 医生查看本人挂号队列 | 医生不能处理其他医生数据 |
| AI 病历生成 | `medical-record-service` 调 `ai-service` | 医生确认后保存正式病历 |
| AI 处方审核 | `prescription-service` 调 `ai-service` | 审核记录和处方写入数据库 |
| 风险通知 | RabbitMQ + WebSocket | 医生端可收到处方风险通知 |
| 管理端基础数据 | `admin-service` 聚合管理 | 科室、医生、药品、Prompt、知识库、字典可维护 |
| AI 排班 | 管理员生成建议并发布 | 发布后患者端能看到新增号源 |
| 分诊工作台 | 管理员查看、改派、关闭 | 分诊记录状态可流转 |
| Docker 部署 | Compose 编排 | 三端 Web、Gateway、业务服务可启动 |

## 3. 不做范围

- 不做真实支付、医保、发药、药房库存。
- 不接入真实医院生产数据。
- AI 输出仅用于教学演示和辅助建议，不作为真实医疗诊断结论。
- 真实大模型接入保留接口位置，默认使用 Mock Provider。

## 4. 答辩讲解重点

1. 本项目不是单点 AI 页面演示，而是诊前、诊中、诊后的业务闭环。
2. 前端只访问 Gateway，后端按认证、患者、医生、挂号、分诊、病历、处方、AI、通知、管理拆分服务。
3. KingbaseES 是唯一事实库，基础数据、号源和交易数据均可查询验证。
4. AI 当前是 Mock Provider，但请求链路走 `ai-service`，前端没有伪造结果。
5. RabbitMQ 和 WebSocket 用于高风险处方通知，体现异步事件和实时通信能力。

