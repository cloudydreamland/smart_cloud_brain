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
- `triage-service`、`medical-record-service`、`prescription-service` 通过后端链路调用 `ai-service`。
- 管理端排班建议当前保留现有规则生成与人工发布链路，AI 排班服务化接入作为后续交接项。
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

## 4. Dify 真实模型接入

项目已经预留 `DifyAiProvider`，可通过环境变量将 `ai-service` 从 Mock 切换为 Dify Workflow：

```text
AI_PROVIDER=dify
DIFY_BASE_URL=http://host.docker.internal/v1
DIFY_TRIAGE_API_KEY=app-xxxx
DIFY_MEDICAL_RECORD_API_KEY=app-xxxx
DIFY_PRESCRIPTION_CHECK_API_KEY=app-xxxx
AI_TIMEOUT_MS=8000
```

如果 `ai-service` 直接在本机运行，`DIFY_BASE_URL` 可以使用 `http://localhost/v1`；如果 `ai-service` 运行在 Docker 容器里，推荐使用 `http://host.docker.internal/v1` 访问 Mac 主机上的 Dify。

Dify 侧建议创建 3 个 Workflow 应用：

| Workflow | 输入变量 | 输出字段 |
| --- | --- | --- |
| 智慧云脑-智能分诊 | `patientId`, `chiefComplaint` | `recommendedDepartment`, `departmentCode`, `recommendedDoctorIds`, `reason`, `degraded` |
| 智慧云脑-病历草稿生成 | `registrationId`, `departmentCode`, `dialogueText` | `chiefComplaint`, `presentIllness`, `pastHistory`, `physicalExam`, `diagnosis`, `treatmentAdvice`, `degraded` |
| 智慧云脑-处方审核 | `patientId`, `doctorId`, `drugs` | `riskLevel`, `suggestions`, `interactions`, `degraded` |

`DifyAiProvider` 调用 Workflow 运行接口时发送 `inputs`、`response_mode=blocking` 和 `user`，并读取 `data.outputs`。如果 Dify 返回的是 `outputs.result` 字符串，后端也会尝试按 JSON 解析。

建议在 Dify 中创建“智慧云脑轻症知识库”，导入轻症分诊、药品禁忌、病历书写规范等内容，并在 3 个 Workflow 中使用知识库检索增强输出。

配置完成后可执行项目烟测脚本：

```bash
AI_BASE_URL=http://localhost:8081 ./scripts/verify-dify-ai.sh
```

真实 Dify 接入通过的判断标准：`/internal/ai/health` 返回 `provider=dify`、`difyConfigured=true`，并且三类业务响应中的 `degraded=false`。

## 5. 真实模型替换点

接入真实模型时，优先新增或替换 `AiProvider` 实现，保持 Controller、业务服务和前端调用不变。当前已经提供 `MockAiProvider` 和 `DifyAiProvider`，建议继续保留 Mock 作为演示和降级 Provider。

## 6. 答辩口径

可以这样说明：

> 项目不强调模型医学准确率，而强调 AI 服务化、Prompt 可配置、结构化输出、知识库约束、医生确认和失败降级。当前大模型 Provider 是 Mock，但调用链路、数据库落库、权限控制和业务闭环都是真实实现。

如果已经配置 Dify，可以补充：

> 项目通过 `DifyAiProvider` 接入 Dify Workflow，患者分诊、医生病历草稿和处方审核分别对应 3 个 Workflow。Dify 负责模型编排和知识库增强，`ai-service` 负责统一封装、结构化解析和降级，三端前端接口无需改动。
