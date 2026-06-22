# AI 与轻症知识库使用说明

## 1. AI 定位

本项目 AI 定位为教学演示中的辅助能力，不替代医生诊断。所有 AI 输出只作为草稿或建议，最终病历、处方和排班必须由医生或管理员确认。

本地开发默认配置：

```text
AI_PROVIDER=mock
```

Mock 的是模型 Provider 输出，不是前后端链路。实际调用仍然是：

```text
前端 -> Gateway -> 业务服务 -> ai-service -> MockAiProvider
```

## 2. 已实现能力

- `ai-service` 提供智能分诊、病历生成、处方审核、AI 排班和 Prompt 解析能力。
- `triage-service`、`medical-record-service`、`prescription-service`、`admin-service` 通过后端链路调用 `ai-service`。
- 管理端展示“真实 AI 建议/规则降级”状态，建议通过后端强校验且经管理员确认后才能发布号源。
- 管理端可维护 Prompt 模板、药品和知识库条目。
- 知识库、药品、Prompt 模板搜索直接查询 KingbaseES 表，由 `admin-service` 暴露接口。
- `MockAiProvider` 提供确定性降级输出；Dify/OpenAI-compatible 调用失败时统一回退并标记 `degraded=true`。

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
DIFY_SCHEDULE_API_KEY=app-xxxx
AI_TIMEOUT_MS=8000
```

如果 `ai-service` 直接在本机运行，`DIFY_BASE_URL` 可以使用 `http://localhost/v1`；如果 `ai-service` 运行在 Docker 容器里，推荐使用 `http://host.docker.internal/v1` 访问 Mac 主机上的 Dify。

Dify 侧需要创建 4 个 Workflow 应用：

| Workflow | 输入变量 | 输出字段 |
| --- | --- | --- |
| 智慧云脑-智能分诊 | `patientId`, `chiefComplaint` | `recommendedDepartment`, `departmentCode`, `recommendedDoctorIds`, `reason`, `degraded` |
| 智慧云脑-病历草稿生成 | `registrationId`, `departmentCode`, `dialogueText` | `chiefComplaint`, `presentIllness`, `pastHistory`, `physicalExam`, `diagnosis`, `treatmentAdvice`, `degraded` |
| 智慧云脑-处方审核 | `patientId`, `doctorId`, `drugs` | `riskLevel`, `suggestions`, `interactions`, `degraded` |
| 智慧云脑-AI排班建议 | `startDate`, `days`, `doctors`, `departments`, `existingSchedules` | `suggestions`, `degraded` |

`DifyAiProvider` 调用 Workflow 运行接口时发送 `inputs`、`response_mode=blocking` 和 `user`，并读取 `data.outputs`。如果 Dify 返回的是 `outputs.result` 字符串，后端也会尝试按 JSON 解析。

建议在 Dify 中创建“智慧云脑轻症知识库”，导入轻症分诊、药品禁忌、病历书写规范等内容，并在相关 Workflow 中使用知识库检索增强输出。

配置完成后可执行项目烟测脚本：

```bash
AI_BASE_URL=http://localhost:8081 ./scripts/verify-dify-ai.sh
```

真实 Dify 接入通过的判断标准：`/internal/ai/health` 返回 `provider=dify`、`difyConfigured=true`，并且四类业务响应中的 `degraded=false`。主动停止或断开 Dify 后，四类接口仍应返回结构化结果并标记 `degraded=true`。

## 5. 真实模型替换点

接入真实模型时，优先新增或替换 `AiProvider` 实现，保持 Controller、业务服务和前端调用不变。当前已经提供 `MockAiProvider` 和 `DifyAiProvider`，建议继续保留 Mock 作为演示和降级 Provider。

## 6. 答辩口径

可以这样说明：

> 项目不强调模型医学准确率，而强调 AI 服务化、Prompt 可配置、结构化输出、知识库约束、人工确认和失败降级。演示环境使用 Dify + DeepSeek，Dify 异常时才回退确定性 Mock；调用链路、数据库落库、权限控制和业务闭环均为真实实现。

如果已经配置 Dify，可以补充：

> 项目通过 `DifyAiProvider` 接入四个 Dify Workflow，覆盖患者分诊、医生病历草稿、处方审核和管理员排班。Dify 负责模型编排，`ai-service` 负责统一封装、结构化解析、结果校验和降级，三端前端接口无需感知 Provider 变化。
