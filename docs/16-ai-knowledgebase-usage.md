# AI 与轻症知识库使用说明

本项目的 AI 定位是教学实践中的辅助能力，不代替医生诊断。所有 AI 输出只作为草稿或建议，最终病历和处方必须由医生确认。

## 已实现能力

- `ai-service` 独立提供智能分诊、病历生成、处方审核和 Prompt 解析接口。
- `diagnosis-service` 通过 OpenFeign 调用 `ai-service`，AI 不可用时返回可操作的降级结果。
- `MockAiProvider` 提供稳定演示数据，后续可替换为 Spring AI、Dify 或第三方大模型。
- 管理端可维护 Prompt 模板和轻症知识库条目。

## 轻症知识库范围

第一版覆盖校园常见轻症，例如普通感冒、急性腹泻、咽痛发热、皮肤过敏等。知识库字段包括：

- `title`：轻症条目名称。
- `symptoms`：常见症状。
- `riskSignals`：危险信号，例如持续高热、胸痛、呼吸困难。
- `advice`：初步建议。
- `departmentCode`：推荐科室。

## 替换真实模型的接口点

后续接入 Spring AI 时，优先替换 `AiProvider` 实现，不改业务 Controller。推荐保留 `MockAiProvider` 作为演示和降级 Provider。

## 答辩口径

本项目不强调模型医学准确率，强调 AI 服务化、Prompt 可配置、结构化输出、知识库约束、医生确认和失败降级。
