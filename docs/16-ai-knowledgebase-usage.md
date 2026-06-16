# AI 与轻症知识库使用说明

## 1. AI 定位

本项目 AI 定位为教学演示中的辅助能力，不替代医生诊断。所有 AI 输出只作为草稿或建议，最终病历、处方和排班必须由医生或管理员确认。

当前默认配置：

```text
AI_PROVIDER=mock
```

Mock 的是模型 Provider 输出，不是前后端链路。实际调用仍然是：

```text
前端 -> Gateway -> 业务服务 -> ai-service -> MockAiProvider
```

## 2. 已实现能力

- `ai-service` 提供智能分诊、病历生成、处方审核、Prompt 解析等能力。
- `triage-service`、`medical-record-service`、`prescription-service` 和 `admin-service` 通过后端链路调用 `ai-service`。
- 管理端可维护 Prompt 模板、药品和知识库条目。
- 知识库、药品、Prompt 模板搜索直接查询 KingbaseES 表，由 `admin-service` 暴露接口。
- `MockAiProvider` 提供稳定演示输出，后续可替换为 Spring AI、Dify 或第三方大模型 Provider。

## 3. 轻症知识库范围

第一版覆盖校园常见轻症和风险提醒，例如普通感冒、咽痛发热、急性腹泻、皮肤过敏、咳嗽等。

核心字段：

- `title`：知识条目名称。
- `symptoms`：常见症状。
- `risk_signals`：危险信号。
- `advice`：初步建议。
- `department_code`：推荐科室。

## 4. 真实模型替换点

后续接入真实模型时，优先新增或替换 `AiProvider` 实现，保持 Controller、业务服务和前端调用不变。建议继续保留 `MockAiProvider` 作为演示和降级 Provider。

## 5. 答辩口径

可以这样说明：

> 项目不强调模型医学准确率，而强调 AI 服务化、Prompt 可配置、结构化输出、知识库约束、医生确认和失败降级。当前大模型 Provider 是 Mock，但调用链路、数据库落库、权限控制和业务闭环都是真实实现。

