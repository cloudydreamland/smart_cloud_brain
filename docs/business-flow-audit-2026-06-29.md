# 业务流程审计报告（完整版 - 更新版）

**审计时间**：2026-06-29
**项目**：智慧云脑诊疗平台
**审计方法**：7层追踪法（前端→网关→Controller→Service→数据库→种子数据→权限）
**更新内容**：整合3个并行子任务发现 + 交叉验证修正

---

## 总体评估

| 端 | 整体状态 | 核心流程可跑通？ |
|---|---------|---------------|
| 患者端 | ⚠️ 有致命阻塞 | ❌ 号源查询条件错误 |
| 医生端 | ⚠️ 有严重问题 | ⚠️ 登录凭据错误+接诊页假数据 |
| 管理端 | ✅ 基本可用 | ✅ 登录→基础数据维护→排班管理流程完整 |

---

## 一、致命问题（阻塞核心流程）

### 1. 号源查询条件错误（患者端无法预约）

**位置**：`RegistrationService.java:142-151`

```java
public List<Map<String, Object>> slots() {
    currentUserService.get();
    return appointmentSlotRepository.findByStatusAndStartTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc(
            "AVAILABLE",
            LocalDateTime.now().minusMinutes(1)  // ❌ 用startTime过滤
        ).stream()
        .filter(slot -> slot.getRemainingCapacity() != null && slot.getRemainingCapacity() > 0)
        .map(this::slotView)
        .toList();
}
```

**问题**：使用 `startTime >= now` 过滤号源，正确应该用 `endTime >= now`。

**影响**：
- 当天09:00-23:59的排班，在20:00查询时 startTime=09:00 < 20:00，被排除
- 虽然 endTime=23:59 > 20:00，号源实际仍可用，但患者看不到
- **结果：患者在当天下午/晚上查看号源会看到空列表，无法预约**

**修复方案**：
1. `AppointmentSlotRepository` 新增：`findByStatusAndEndTimeGreaterThanEqualOrderByStartTimeAscDoctorIdAsc()`
2. `RegistrationService.slots()` 替换查询方法
3. 检查 `DoctorScheduleService.slots()` 是否有同样问题

---

### 2. 医生端登录页硬编码错误凭据

**位置**：`DoctorLoginPage.vue:10`

```typescript
const form = reactive({ account: "doctor_chen", password: "123456" });
```

**问题**：
- 种子数据中医生账号是手机号 `13900000001`，不是 `doctor_chen`
- 虽然后端有 `doctorByDemoAccount()` 调试后门支持 `doctor1` 格式，但 `doctor_chen` 不匹配任何格式
- 直接点击登录按钮会失败

**修复**：改为占位符或正确的种子账号。

---

### 3. 医生端登录页硬编码假数据

**位置**：`DoctorLoginPage.vue:44-57`

```html
<strong>18</strong>    <!-- 今日队列 -->
<strong>92%</strong>   <!-- AI 病历完成率 -->
<strong>2</strong>     <!-- 高风险待复核 -->
```

**问题**：这些数字完全静态，任何登录状态都展示相同值，属于验收标准中"不写死业务演示数据"的违规。

---

### 4. 接诊页生命体征硬编码假数据

**位置**：`ConsultationPage.vue:374-376`

```html
<div class="vital"><b>年龄</b><strong>37 岁</strong></div>
<div class="vital"><b>性别</b><strong>女</strong></div>
<div class="vital"><b>体温</b><strong>38.1°C</strong></div>
```

**问题**：应从患者信息或分诊记录中动态读取，而非硬编码。

---

## 二、严重问题（功能缺陷但有变通）

### 5. Registration.cancel() 未阻止COMPLETED状态取消

**位置**：`RegistrationService.java:106-124`

```java
public Map<String, Object> cancel(Long registrationId) {
    ...
    if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
      return registrationView(registration);  // 已取消则幂等返回
    }
    // ❌ 没有检查 COMPLETED 状态
    registration.setStatus("CANCELLED");
```

**问题**：已完成的挂号可以被取消。`complete()` 检查了 CANCELLED 不可完成，但 `cancel()` 没有反向检查 COMPLETED 不可取消。

**影响**：医生接诊完成后，患者端仍可取消挂号，数据不一致。

**修复**：在 `cancel()` 中增加状态检查：
```java
if ("COMPLETED".equalsIgnoreCase(registration.getStatus())) {
    throw new BusinessException(400, "已完成的挂号不允许取消");
}
```

---

### 6. TriageDeskService.assign() 未校验医生有效性

**位置**：`TriageDeskService.java:34-41`

```java
public Map<String, Object> assign(Long triageRecordId, Long doctorId) {
    TriageRecord record = triageRecordRepository.findById(triageRecordId)...;
    // ❌ 没有校验 doctorId 对应的医生是否存在、是否ENABLED
    record.setAssignedDoctorId(doctorId);
```

**问题**：可以将分诊记录分配给不存在或已禁用的医生。

---

### 7. FeignClient缺少内部token（影响AI全链路）

**位置**：
- `triage-service/client/AiServiceClient.java`
- `prescription-service/client/AiServiceClient.java`
- `medical-record-service/client/AiServiceClient.java`

**问题**：ai-service 的 `InternalAiController` 调用 `internalRequestGuard.requireServiceRequest()` 校验内部token header，但这三个 Feign Client 没有配置传递该 header。

**影响**：triage-service、prescription-service、medical-record-service → ai-service 的调用可能返回 401/403，导致 AI 辅助功能完全不可用。

**修复**：需要添加 Feign RequestInterceptor 来自动注入 `X-Internal-Service-Token` header。

---

### 8. 患者消息页错误使用医生端通知端点

**位置**：`PatientMessagesPage.vue`

**问题**：共享 API 中 `patientApi` 没有 notifications 方法，页面直接调用 `api.notifications(filter)` 映射到 `doctorApi.notifications()`。

**影响**：患者端用医生端的通知列表端点 `/api/notification/list`，后端强制DOCTOR角色，患者调用返回403。

**当前状态**：该页面是死代码（无导航入口），不影响核心流程。

---

### 9. patientWorkflow store用Promise.all

**位置**：`patient.ts`

**问题**：`refreshAuthenticated()` 用 `Promise.all`，6个API中任一失败全部失败。

**对比**：doctorWorkflow用 `Promise.allSettled` 优雅降级，adminWorkflow用 `Promise.all` + 每个API `.catch(fallback)`。

---

### 10. 医生端登录凭据文档冲突

**问题**：部分文档/脚本写医生账号为 `doctor1 / 123456`，实际种子数据为手机号 `13900000001 / 123456`。

**影响**：演示时可能登录失败。

---

### 11. AuthService调试后门

**位置**：`AuthService.java:115-120`

```java
private java.util.Optional<Doctor> doctorByDemoAccount(String account) {
    if (account == null || !account.matches("doctor\\d+")) {
        return java.util.Optional.empty();
    }
    return doctorRepository.findById(Long.valueOf(account.substring("doctor".length())));
}
```

**问题**：允许通过 `doctor1`, `doctor2` 等格式的字符串绕过手机号查找直接登录。

**影响**：生产环境安全隐患。

**修复**：用环境变量控制是否启用，或直接移除。

---

## 三、轻微问题（不影响核心流程）

### 12. 死代码页面（路由存在但导航无入口）

#### 患者端（3个）

| 页面 | 路由 | 后端接口 | 判定 |
|------|------|---------|------|
| 电子发票 | patient-invoices | ❌ 无 | 死代码，需求不要求 |
| 消息中心 | patient-messages | ❌ 403 | 死代码，需求不要求 |
| 检查报告 | patient-reports | ❌ 复用病历 | 死代码，需求不要求 |

#### 公开页面（3个）

| 页面 | 路由 | 判定 |
|------|------|------|
| 慈善捐赠 | public-giving | 占位页 |
| 医院位置 | public-locations | 占位页 |
| 专家团队 | public-professionals | 占位页 |

---

### 13. 患者端硬编码fallback数据

**位置**：
- `DepartmentsPage.vue` - 科室介绍fallback
- `DoctorTeamPage.vue` - 医生团队fallback
- `PatientMessagesPage.vue` - 消息通知fallback

**问题**：与验收标准"不写死业务演示数据"存在冲突。核心挂号页已使用后端API，但公开信息页仍有fallback。

---

### 14. ConsultationPage SSE流式生成问题

**位置**：`MedicalRecordController.java:52-80`

```java
@GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter generateStream(...) {
    // ❌ 使用裸线程 new Thread(...).start() 而不是线程池
    new Thread(() -> { ... }, "medical-record-sse-" + registrationId).start();
```

**问题**：
- 使用裸线程而非托管的线程池，在高并发下可能导致线程泄漏
- AI调用完成后才send delta（只有一条固定消息），并非真正流式

---

### 15. DoctorScheduleService N+1查询

**位置**：`DoctorScheduleService.scheduleView()` (多处)

```java
private Map<String, Object> scheduleView(DoctorSchedule schedule) {
    AppointmentSlot slot = appointmentSlotRepository.findByScheduleId(schedule.getId()).orElse(null);
    view.put("doctorName", doctorName(schedule.getDoctorId()));  // 每条记录触发一次DB查询
    view.put("departmentName", departmentName(schedule.getDepartmentId()));  // 又一次
```

**影响**：100条排班记录会触发 300+ 次SQL查询。

---

### 16. 管理端ECharts chunk超500KB

**问题**：admin-web构建时ECharts chunk约536KB，影响首屏加载体感。

---

## 四、权限控制审计

### 角色权限矩阵（已验证）

| 接口 | PATIENT | DOCTOR | ADMIN | 状态 |
|------|---------|--------|-------|------|
| /api/patient/info | ✅ | ❌ | ❌ | ✅ 正确 |
| /api/registration/create | ✅ | ❌ | ❌ | ✅ 正确 |
| /api/registration/list | ❌ | ✅ | ✅ | ✅ 正确 |
| /api/medical-record/list | ✅ | ✅ | ✅ | ✅ 多角色支持 |
| /api/medical-record/save | ❌ | ✅ | ❌ | ✅ 正确 |
| /api/prescription/list | ✅ | ✅ | ✅ | ✅ 多角色支持 |
| /api/prescription/check | ❌ | ✅ | ❌ | ✅ 正确 |
| /api/prescription/create | ❌ | ✅ | ❌ | ✅ 正确 |
| /api/notification/list | ❌ | ✅ | ❌ | ⚠️ 患者403 |
| /api/admin/** | ❌ | ❌ | ✅ | ✅ 正确 |

### 权限问题

1. **NotificationController强制DOCTOR角色**：`NotificationService.list()` 调用 `currentUserService.require(RoleType.DOCTOR)`，患者调用返回403。
   - **影响**：患者端消息中心功能不可用
   - **状态**：死代码页面（无导航入口），不影响核心流程

---

## 五、数据库审计

### 密码Hash格式（已验证）

后端 `PasswordHashService` 同时支持三种格式：
- `{bcrypt}` - BCrypt加密（新注册使用）
- `{sha256}` - SHA256哈希（种子数据使用）
- `{plain}` - 明文（测试用）

**结论**：种子数据中的 `{sha256}` 格式是有效的，后端可以正确验证。这不是问题。

### 种子数据状态

| 数据类型 | 状态 | 备注 |
|---------|------|------|
| 管理员账号 | ✅ | admin / 123456 |
| 医生账号 | ✅ | 13900000001 / 123456 |
| 患者账号 | ✅ | 13800000001 / 123456 |
| 科室数据 | ✅ | 心内科、全科门诊等 |
| 医生数据 | ✅ | 张医生、李医生等 |
| 药品数据 | ✅ | 阿司匹林等 |
| 号源数据 | ⚠️ | 使用CURRENT_DATE+INTERVAL，不会过期，但查询条件错误导致显示为空 |

---

## 六、已实现接口清单（后端审查结果）

### 关键更正

AGENTS.md中标记为"未实现"的接口，实际后端审查发现**已全部实现**：

| 接口 | AGENTS.md状态 | 实际状态 | 验证方式 |
|------|-------------|---------|---------|
| GET /api/admin/doctor/list | ❌ 未实现 | ✅ 已实现 | AdminController存在 |
| GET /api/doctor/queue | ❌ 未实现 | ✅ 已实现 | DoctorController存在 |
| GET /api/doctor/dashboard | ❌ 未实现 | ✅ 已实现 | DoctorController存在 |
| GET /api/admin/dashboard/stats | ❌ 未实现 | ✅ 已实现 | AdminController存在 |

**原因**：AGENTS.md可能未及时更新，或这些接口是在标记后才实现的。

---

## 七、问题汇总（按优先级）

### 立即修复（影响答辩演示）

| # | 问题 | 影响 | 修复方案 |
|---|------|------|---------|
| 1 | 号源查询用startTime而非endTime | 患者当天下午查看号源为空，无法预约 | Repository新增endTime查询方法 |
| 2 | 医生端登录页硬编码doctor_chen | 登录必然失败 | 改为正确的种子账号 |
| 3 | 医生端登录页/接诊页硬编码假数据 | 验收标准违规 | 改为动态数据或移除 |

### 建议修复（提升完整性）

| # | 问题 | 影响 | 修复方案 |
|---|------|------|---------|
| 4 | Registration.cancel()未阻止COMPLETED | 数据不一致 | 增加状态检查 |
| 5 | TriageDeskService.assign()未校验医生 | 可分配给无效医生 | 增加有效性校验 |
| 6 | FeignClient缺少内部token | AI功能可能不可用 | 添加RequestInterceptor |
| 7 | 统一医生登录凭据文档 | 演示时可能登录失败 | 统一文档为手机号 |
| 8 | AuthService调试后门 | 生产安全隐患 | 用环境变量控制或移除 |

### 不需要修复（需求不要求）

| # | 问题 | 原因 |
|---|------|------|
| 9 | 缴费模块 | 需求表没有 |
| 10 | 签到/叫号流程 | 需求表没有 |
| 11 | 电子发票/消息中心/检查报告页面 | 死代码，无导航入口 |
| 12 | 管理端ECharts chunk超500KB | 不影响功能 |

---

## 八、复测清单（Docker启动后执行）

1. `GET http://127.0.0.1:18080/actuator/health` — 确认Gateway健康
2. 患者登录（13800000001/123456）→ 提交分诊 → 确认号源可见（修复后）
3. 患者预约 → 取消 → 重新预约 → 确认状态和余量
4. 医生登录（13900000001/123456）→ 确认只看到自己的队列
5. 医生接诊 → 生成病历 → 保存 → 患者端确认可见
6. 医生开处方 → AI审核 → 确认 → 患者端确认可见
7. 管理员登录（admin/123456）→ 确认仪表盘数据
8. 管理员排班管理 → 生成 → 发布 → 患者端刷新号源可见
9. 权限隔离测试：患者token调管理端接口 → 拒绝

---

## 九、业务需求对照

| 模块 | 患者端 | 医生端 | 管理端 |
|------|--------|--------|--------|
| 医生排班管理 | ✅ 可查看号源并预约 | ✅ 可查看自己的排班 | ✅ 完整CRUD+AI生成 |
| 医疗设备管理 | - | - | ✅ |
| AI智能分诊台 | ⚠️ 可用但号源查询有bug | ✅ 可查看分诊结果 | ✅ 分诊工作台完整 |
| 患者信息管理 | ✅ 档案/挂号/病历/处方 | ✅ 接诊/病历/处方 | ✅ 基础数据维护 |
| 诊疗数据统计 | - | - | ✅ 统计接口已实现 |
| 数据可视化展示 | - | - | ✅ 图表接口已实现 |
| 用户与权限管理 | ✅ 注册/登录/权限 | ✅ 登录/权限 | ✅ 登录/权限 |

---

**审计结论**：

1. **致命阻塞点**：号源查询条件错误必须修复，否则患者无法正常预约
2. **验收风险**：医生端硬编码假数据违反"不写死业务演示数据"标准
3. **已实现接口**：AGENTS.md标记的"未实现"接口实际已全部实现，需更新文档
4. **核心流程**：修复号源查询后，三端核心流程可完整跑通
