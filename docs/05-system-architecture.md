# 系统架构设计文档

## 1. 整体架构图

```mermaid
flowchart TB
  P[患者端 Vue 3] --> DS[diagnosis-service 业务服务]
  D[医生端 Vue 3] --> DS
  DS --> AUTH[JWT 认证与权限校验]
  DS --> BIZ[业务服务层]
  BIZ --> DB[(业务 MySQL 数据库)]
  DS --> AIC[AI Service Client Spring Cloud Feign]
  AIC --> AIS[ai-service AI 能力服务]
  AIS --> LLM[大语言模型 API 或 Mock]
  AIS --> KG[知识图谱服务 可选]
  DS --> DOC1[业务 Knife4j 文档]
  AIS --> DOC2[AI 服务 Knife4j 文档]
```

## 2. 模块之间的关系

| 模块 | 依赖模块 | 说明 |
|---|---|---|
| 认证模块 | 患者、医生 | 登录后生成 JWT 和角色信息 |
| 患者模块 | 认证模块 | 维护患者个人信息 |
| 医生模块 | 科室模块 | 医生归属于科室 |
| 挂号模块 | 患者、医生、科室、分诊记录 | 创建患者与医生之间的预约关系 |
| 智能分诊模块 | 患者、医生、`ai-service` | 根据主诉推荐科室和医生 |
| 病历模块 | 患者、医生、挂号、`ai-service` | 生成和保存结构化病历 |
| 处方模块 | 患者、医生、病历、`ai-service` | 开方并进行 AI 审核 |

## 3. 数据流转流程

1. 前端页面提交请求。
2. Axios 自动携带 JWT。
3. 后端网关层或拦截器完成身份识别。
4. Controller 校验请求参数。
5. Service 执行业务逻辑。
6. 如涉及 AI，`diagnosis-service` 通过 Spring Cloud Feign 调用 `ai-service`。
7. Repository 持久化业务数据和 AI 记录。
8. 后端返回统一响应。
9. 前端更新页面状态。

## 4. 服务调用流程

### 4.1 智能分诊调用链

```text
TriageController
  -> TriageService
    -> AiTriageClient
      -> ai-service /internal/ai/triage
        -> AI HTTP API / Mock AI
    -> DoctorService 查询推荐医生
    -> TriageRecordRepository 保存分诊记录
  -> 返回推荐结果
```

### 4.2 病历生成调用链

```text
MedicalRecordController
  -> MedicalRecordService
    -> AiMedicalRecordClient
      -> ai-service /internal/ai/medical-record/generate
        -> LLM API 生成结构化病历
  -> 返回草稿病历
```

保存时：

```text
MedicalRecordController
  -> MedicalRecordService
    -> RegistrationRepository 校验挂号关系
    -> MedicalRecordRepository 保存病历
```

### 4.3 处方审核调用链

```text
PrescriptionController
  -> PrescriptionService
    -> AiPrescriptionCheckClient
      -> ai-service /internal/ai/prescription/check
        -> AI API / 知识图谱服务
    -> PrescriptionCheckRecordRepository 保存审核记录
  -> 返回审核结果
```

## 5. 部署结构说明

```mermaid
flowchart LR
  Browser[浏览器] --> Nginx[Nginx]
  Nginx --> Static[Vue 静态资源]
  Nginx --> Diagnosis[diagnosis-service Jar]
  Diagnosis --> MySQL[(MySQL)]
  Diagnosis --> AiService[ai-service Jar]
  AiService --> AI[外部 AI 服务或 Mock]
```

## 6. 推荐端口

| 服务 | 本地端口 |
|---|---|
| Vue 开发服务 | `5173` |
| diagnosis-service | `8080` |
| ai-service | `8081` |
| MySQL | `3306` |
| Knife4j | `http://localhost:8080/doc.html` |
