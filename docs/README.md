# 文档总览

本项目当前口径：后端采用微服务架构，前端统一通过 `gateway-service` 访问真实接口；KingbaseES 是唯一事实数据库；RabbitMQ 负责可靠异步事件；知识库、药品、Prompt 模板搜索直接由 `admin-service` 查询 KingbaseES；`diagnosis-service` 已废弃并移除。

## 主要文档

- `01-project-overview.md`：项目概况
- `04-technical-solution.md`：技术方案
- `05-system-architecture.md`：系统架构
- `06-database-design.md`：数据库设计
- `07-api-design.md`：接口设计
- `09-development-plan.md`：开发计划
- `10-test-plan.md`：测试计划
- `11-deployment-ops.md`：部署和运维
- `14-microservice-architecture.md`：微服务架构
- `16-ai-knowledgebase-usage.md`：AI 和知识库使用说明
