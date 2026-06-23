# 文档总览

本目录用于项目设计、开发、部署、测试、验收和答辩说明。当前统一口径如下：

- 后端采用 Spring Cloud 纯微服务架构，前端只通过 `gateway-service` 调用后端。
- KingbaseES/PostgreSQL 兼容数据库是唯一事实库。
- Web 三端不依赖前端假数据，核心业务数据均来自数据库。
- AI 当前使用 `ai-service` 中的 Mock Provider，但前端必须走 Gateway 到后端 AI 链路。
- RabbitMQ 用于异步处方风险事件，WebSocket 用于医生端实时通知。

## 主要文档

- `01-project-overview.md`：项目概述。
- `02-requirements-analysis.md`：需求分析。
- `03-prd.md`：产品需求说明。
- `04-technical-solution.md`：技术方案。
- `05-system-architecture.md`：系统架构。
- `06-database-design.md`：数据库设计。
- `07-api-design.md`：接口设计。
- `08-ui-interaction.md`：页面与交互。
- `09-development-plan.md`：开发计划。
- `10-test-plan.md`：测试计划。
- `11-deployment-ops.md`：启动、部署和运维。
- `12-risks.md`：风险与问题清单。
- `13-task-coverage-extension.md`：必做任务覆盖。
- `14-microservice-architecture.md`：微服务架构说明。
- `15-team-division.md`：团队分工。
- `16-ai-knowledgebase-usage.md`：AI 与知识库使用说明。
- `17-acceptance-checklist.md`：阶段验收清单。
- `18-demo-script.md`：答辩演示脚本。

