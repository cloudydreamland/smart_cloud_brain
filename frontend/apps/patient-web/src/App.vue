<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  fieldText,
  formatApiError,
  statusClass,
  toNumber,
  type DataRow,
} from "@smart-cloud-brain/shared-api";
import { useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { session, permissionError } = storeToRefs(auth);
const { patient, departments, doctors, triage, triageHistory, slots, registrations, records, prescriptions } = storeToRefs(workflow);

const loading = reactive({
  boot: true,
  auth: false,
  data: false,
  triage: false,
  registration: false,
  detail: false,
});
const error = ref("");
const notice = ref("");
const active = ref("dashboard");
const selectedSlotId = ref<number | null>(null);
const selectedRecord = ref<DataRow | null>(null);
const selectedPrescription = ref<DataRow | null>(null);
let unbindUnauthorized: (() => void) | null = null;

const loginForm = reactive({ account: "", password: "" });
const registerForm = reactive({
  name: "",
  phone: "",
  password: "",
  gender: "FEMALE",
  age: 30,
  allergyHistory: "",
  pastHistory: "",
});
const triageForm = reactive({
  symptoms: "",
  duration: "",
  severity: "MEDIUM",
  extra: "",
});

const latestTriage = computed(() => triage.value ?? triageHistory.value[0] ?? null);
const recommendedDepartment = computed(() => fieldText(latestTriage.value, "recommendedDepartment", ""));
const visibleDoctors = computed(() => {
  const dept = recommendedDepartment.value;
  return doctors.value.filter((doctor) => !dept || fieldText(doctor, "departmentName", "").includes(dept));
});
const visibleSlots = computed(() => {
  const dept = recommendedDepartment.value;
  return slots.value.filter((slot) => !dept || fieldText(slot, "departmentName", "").includes(dept));
});
const selectedSlot = computed(() => {
  const list = visibleSlots.value.length ? visibleSlots.value : slots.value;
  return list.find((item) => toNumber(item.slotId) === selectedSlotId.value) ?? list[0] ?? null;
});
const activeRegistrations = computed(() => registrations.value.filter((item) => fieldText(item, "status") !== "CANCELLED"));
const canSubmitTriage = computed(() => triageForm.symptoms.trim().length >= 6 && triageForm.duration.trim().length > 0);

function text(item: DataRow | null | undefined, key: string, fallback = "-") {
  return fieldText(item, key, fallback);
}

function setError(message: string) {
  error.value = message;
  notice.value = "";
}

function setNotice(message: string) {
  notice.value = message;
  error.value = "";
}

async function run(key: keyof typeof loading, fallback: string, task: () => Promise<void>) {
  loading[key] = true;
  error.value = "";
  notice.value = "";
  try {
    await task();
  } catch (err) {
    setError(formatApiError(err, fallback));
  } finally {
    loading[key] = false;
  }
}

function validateLogin() {
  if (!loginForm.account.trim()) return "请输入手机号或账号。";
  if (!loginForm.password.trim()) return "请输入密码。";
  return "";
}

function validateRegister() {
  if (!registerForm.name.trim()) return "请输入姓名。";
  if (!/^1\d{10}$/.test(registerForm.phone.trim())) return "请输入 11 位手机号。";
  if (registerForm.password.length < 6) return "密码至少 6 位。";
  if (!registerForm.age || registerForm.age < 0 || registerForm.age > 120) return "请输入有效年龄。";
  return "";
}

async function register() {
  const invalid = validateRegister();
  if (invalid) {
    setError(invalid);
    return;
  }
  await run("auth", "注册失败", async () => {
    await api.registerPatient({ ...registerForm, phone: registerForm.phone.trim() });
    loginForm.account = registerForm.phone.trim();
    loginForm.password = registerForm.password;
    setNotice("注册成功，请登录后继续办理分诊和挂号。");
  });
}

async function login() {
  const invalid = validateLogin();
  if (invalid) {
    setError(invalid);
    return;
  }
  await run("auth", "登录失败", async () => {
    const nextSession = await api.loginPatient(loginForm.account.trim(), loginForm.password);
    auth.save("patient-session", nextSession, "PATIENT");
    if (!auth.requireRole("PATIENT")) return;
    await refreshAll();
    active.value = "dashboard";
    setNotice("已进入患者工作台。");
  });
}

function logout() {
  auth.logout();
  selectedRecord.value = null;
  selectedPrescription.value = null;
  triage.value = null;
}

async function refreshAll() {
  await run("data", "数据同步失败", async () => {
    await workflow.refreshPublicData();
    if (session.value && auth.requireRole("PATIENT")) {
      await workflow.refreshAuthenticated(auth.token());
      selectFirstSlot();
    }
  });
}

function selectFirstSlot() {
  if (!selectedSlotId.value) {
    selectedSlotId.value = toNumber((visibleSlots.value[0] ?? slots.value[0])?.slotId, 0) || null;
  }
}

function chiefComplaint() {
  return [
    `症状：${triageForm.symptoms.trim()}`,
    `持续时间：${triageForm.duration.trim()}`,
    `严重程度：${triageForm.severity}`,
    triageForm.extra.trim() ? `补充说明：${triageForm.extra.trim()}` : "",
  ].filter(Boolean).join("；");
}

async function submitTriage() {
  if (!auth.requireRole("PATIENT")) return;
  if (!canSubmitTriage.value) {
    setError("请至少填写症状和持续时间，便于分诊判断。");
    return;
  }
  await run("triage", "分诊提交失败", async () => {
    triage.value = await api.triage(auth.token(), { chiefComplaint: chiefComplaint() });
    await workflow.refreshAuthenticated(auth.token());
    selectFirstSlot();
    active.value = "registration";
    setNotice("分诊已完成，请根据推荐科室选择可预约号源。");
  });
}

async function createRegistration() {
  if (!auth.requireRole("PATIENT")) return;
  const slot = selectedSlot.value;
  if (!slot) {
    setError("当前没有可预约号源，请刷新后重试或选择其他科室。");
    return;
  }
  await run("registration", "挂号失败", async () => {
    await api.createRegistration(auth.token(), {
      doctorId: toNumber(slot.doctorId),
      departmentId: toNumber(slot.departmentId),
      appointmentTime: fieldText(slot, "startTime", ""),
      slotId: toNumber(slot.slotId),
      triageRecordId: toNumber(latestTriage.value?.triageRecordId, 0) || null,
    });
    await workflow.refreshAuthenticated(auth.token());
    setNotice("挂号已提交，请在挂号记录中查看状态。");
    active.value = "registrations";
  });
}

async function cancelRegistration(id: unknown) {
  if (!window.confirm("确认取消该挂号记录？")) return;
  await run("registration", "取消挂号失败", async () => {
    await api.cancelRegistration(auth.token(), toNumber(id));
    await workflow.refreshAuthenticated(auth.token());
    setNotice("挂号已取消。");
  });
}

async function openRecord(item: DataRow) {
  await run("detail", "病历详情加载失败", async () => {
    selectedRecord.value = await api.medicalRecordDetail(auth.token(), toNumber(item.medicalRecordId));
  });
}

function openPrescription(item: DataRow) {
  selectedPrescription.value = item;
}

onMounted(async () => {
  unbindUnauthorized = auth.bindUnauthorized();
  auth.load("patient-session", "PATIENT");
  await refreshAll();
  loading.boot = false;
});

onBeforeUnmount(() => {
  unbindUnauthorized?.();
});
</script>

<template>
  <main class="patient-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">患者端</p>
        <h1>就诊服务工作台</h1>
        <p>分诊、挂号、病历和处方记录集中处理。</p>
      </div>
      <div v-if="session" class="user-area">
        <span class="tag success">{{ session.name }}</span>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <button type="button" @click="logout">退出</button>
      </div>
    </header>

    <div v-if="error" class="notice error">{{ error }}</div>
    <div v-if="notice" class="notice success">{{ notice }}</div>
    <div v-if="permissionError" class="notice error">
      {{ permissionError }}
      <button type="button" @click="logout">切换账号</button>
    </div>

    <section v-if="loading.boot" class="panel">
      <div class="loading-state">正在加载患者端配置...</div>
    </section>

    <section v-else-if="!session || permissionError" class="auth-grid">
      <form class="panel" @submit.prevent="login">
        <div class="panel-header">
          <div>
            <h2>患者登录</h2>
            <p>登录后可提交分诊、预约号源并查看诊后资料。</p>
          </div>
        </div>
        <label>账号<input v-model.trim="loginForm.account" autocomplete="username" /></label>
        <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
        <button class="primary" type="submit" :disabled="loading.auth">登录</button>
      </form>

      <form class="panel" @submit.prevent="register">
        <div class="panel-header">
          <div>
            <h2>患者注册</h2>
            <p>注册字段与后端患者注册 DTO 对齐。</p>
          </div>
        </div>
        <div class="form-grid">
          <label>姓名<input v-model.trim="registerForm.name" /></label>
          <label>手机号<input v-model.trim="registerForm.phone" /></label>
          <label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" /></label>
          <label>年龄<input v-model.number="registerForm.age" type="number" min="0" max="120" /></label>
          <label>性别
            <select v-model="registerForm.gender">
              <option value="FEMALE">女</option>
              <option value="MALE">男</option>
              <option value="UNKNOWN">未说明</option>
            </select>
          </label>
          <label>过敏史<input v-model.trim="registerForm.allergyHistory" /></label>
        </div>
        <label>既往史<textarea v-model.trim="registerForm.pastHistory" rows="3" /></label>
        <button type="submit" :disabled="loading.auth">注册</button>
      </form>
    </section>

    <template v-else>
      <nav class="tabs" aria-label="患者端业务导航">
        <button type="button" :class="{ active: active === 'dashboard' }" @click="active = 'dashboard'">工作台</button>
        <button type="button" :class="{ active: active === 'triage' }" @click="active = 'triage'">AI 分诊</button>
        <button type="button" :class="{ active: active === 'registration' }" @click="active = 'registration'">挂号</button>
        <button type="button" :class="{ active: active === 'records' }" @click="active = 'records'">病历处方</button>
      </nav>

      <section class="metrics">
        <div class="metric"><span>推荐科室</span><strong>{{ recommendedDepartment || "待分诊" }}</strong></div>
        <div class="metric"><span>可预约号源</span><strong>{{ slots.length }}</strong></div>
        <div class="metric"><span>有效挂号</span><strong>{{ activeRegistrations.length }}</strong></div>
        <div class="metric"><span>诊后记录</span><strong>{{ records.length + prescriptions.length }}</strong></div>
      </section>

      <section v-show="active === 'dashboard'" class="main-grid">
        <div class="stack">
          <section class="panel">
            <div class="panel-header">
              <div>
                <h2>患者信息</h2>
                <p>来自 `/api/patient/info`。</p>
              </div>
              <button type="button" :disabled="loading.data" @click="refreshAll">刷新</button>
            </div>
            <div v-if="loading.data" class="loading-state">正在同步患者信息...</div>
            <div v-else-if="patient" class="summary-grid">
              <div><span>姓名</span><strong>{{ text(patient, "name") }}</strong></div>
              <div><span>手机号</span><strong>{{ text(patient, "phone") }}</strong></div>
              <div><span>年龄</span><strong>{{ text(patient, "age") }}</strong></div>
              <div><span>过敏史</span><strong>{{ text(patient, "allergyHistory", "未记录") }}</strong></div>
              <div class="wide"><span>既往史</span><strong>{{ text(patient, "pastHistory", "未记录") }}</strong></div>
            </div>
            <div v-else class="empty-state">暂无患者资料，请刷新或重新登录。</div>
          </section>

          <section class="panel">
            <div class="panel-header">
              <div>
                <h2>当前分诊结果</h2>
                <p>用于挂号推荐，不替代医生诊断。</p>
              </div>
              <button type="button" @click="active = 'triage'">提交新分诊</button>
            </div>
            <div v-if="latestTriage" class="clinical-note">
              <strong>{{ text(latestTriage, "recommendedDepartment", "待人工确认") }}</strong>
              <p>{{ text(latestTriage, "reason", "暂无建议说明") }}</p>
              <span class="tag" :class="statusClass(latestTriage.status)">{{ text(latestTriage, "status") }}</span>
            </div>
            <div v-else class="empty-state">暂无分诊记录，请先提交症状信息。</div>
          </section>
        </div>

        <aside class="stack">
          <section class="panel">
            <h2>挂号摘要</h2>
            <div v-if="registrations.length" class="list">
              <article v-for="item in registrations.slice(0, 4)" :key="String(item.registrationId)" class="list-row">
                <div>
                  <strong>{{ text(item, "departmentName") }} / {{ text(item, "doctorName") }}</strong>
                  <p>{{ text(item, "appointmentTime") }}</p>
                </div>
                <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
              </article>
            </div>
            <div v-else class="empty-state">暂无挂号记录。</div>
          </section>

          <section class="panel">
            <h2>诊后摘要</h2>
            <div v-if="records.length || prescriptions.length" class="mini-list">
              <p v-for="item in records.slice(0, 3)" :key="`r-${String(item.medicalRecordId)}`">
                病历 #{{ text(item, "medicalRecordId") }}：{{ text(item, "diagnosis") }}
              </p>
              <p v-for="item in prescriptions.slice(0, 3)" :key="`p-${String(item.prescriptionId)}`">
                处方 #{{ text(item, "prescriptionId") }}：{{ text(item, "riskLevel", "未审核") }}
              </p>
            </div>
            <div v-else class="empty-state">医生保存后会显示病历和处方。</div>
          </section>
        </aside>
      </section>

      <section v-show="active === 'triage'" class="main-grid">
        <form class="panel" @submit.prevent="submitTriage">
          <div class="panel-header">
            <div>
              <h2>AI 分诊</h2>
              <p>后端接口为 `/api/triage/consult`，实际请求字段为 `chiefComplaint`。</p>
            </div>
            <span class="tag info">需医生确认</span>
          </div>
          <label>主要症状<textarea v-model.trim="triageForm.symptoms" rows="4" /></label>
          <div class="form-grid">
            <label>持续时间<input v-model.trim="triageForm.duration" /></label>
            <label>严重程度
              <select v-model="triageForm.severity">
                <option value="LOW">轻度</option>
                <option value="MEDIUM">中度</option>
                <option value="HIGH">重度或明显加重</option>
              </select>
            </label>
          </div>
          <label>补充说明<textarea v-model.trim="triageForm.extra" rows="3" /></label>
          <button class="primary" type="submit" :disabled="loading.triage || !canSubmitTriage">提交分诊</button>
        </form>

        <aside class="panel">
          <h2>历史分诊</h2>
          <div v-if="loading.triage" class="loading-state">正在分析并保存分诊记录...</div>
          <div v-else-if="triageHistory.length" class="list">
            <article v-for="item in triageHistory" :key="String(item.triageRecordId)" class="list-row">
              <div>
                <strong>{{ text(item, "recommendedDepartment", "待确认") }}</strong>
                <p>{{ text(item, "chiefComplaint") }}</p>
                <p>{{ text(item, "reason") }}</p>
              </div>
              <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
            </article>
          </div>
          <div v-else class="empty-state">暂无历史分诊记录。</div>
        </aside>
      </section>

      <section v-show="active === 'registration'" class="main-grid">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>科室、医生和号源</h2>
              <p>号源来自 `/api/registration/slots`，仅展示可预约且剩余数量大于 0 的记录。</p>
            </div>
            <button type="button" :disabled="loading.data" @click="refreshAll">刷新号源</button>
          </div>
          <div class="summary-grid compact">
            <div><span>科室数</span><strong>{{ departments.length }}</strong></div>
            <div><span>推荐医生</span><strong>{{ visibleDoctors.length || doctors.length }}</strong></div>
            <div><span>可用号源</span><strong>{{ visibleSlots.length || slots.length }}</strong></div>
          </div>
          <div v-if="loading.data" class="loading-state">正在加载号源...</div>
          <div v-else-if="(visibleSlots.length || slots.length)" class="table-scroll">
            <table>
              <thead>
                <tr><th>时间</th><th>科室</th><th>医生</th><th>剩余</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="slot in (visibleSlots.length ? visibleSlots : slots)" :key="String(slot.slotId)" :class="{ selected: toNumber(slot.slotId) === selectedSlotId }">
                  <td>{{ text(slot, "startTime") }}</td>
                  <td>{{ text(slot, "departmentName") }}</td>
                  <td>{{ text(slot, "doctorName") }}</td>
                  <td>{{ text(slot, "remainingCapacity", "0") }}/{{ text(slot, "capacity", "0") }}</td>
                  <td><span class="tag" :class="statusClass(slot.status)">{{ text(slot, "status") }}</span></td>
                  <td><button type="button" @click="selectedSlotId = toNumber(slot.slotId)">选择</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">暂无可预约号源。请稍后刷新或联系导诊台。</div>
        </section>

        <aside class="stack">
          <section class="panel">
            <h2>预约确认</h2>
            <div class="summary-grid">
              <div><span>科室</span><strong>{{ text(selectedSlot, "departmentName", "未选择") }}</strong></div>
              <div><span>医生</span><strong>{{ text(selectedSlot, "doctorName", "未选择") }}</strong></div>
              <div><span>时间</span><strong>{{ text(selectedSlot, "startTime", "未选择") }}</strong></div>
            </div>
            <div class="notice warning">如号源已满或状态变化，提交时后端会返回冲突提示。</div>
            <button class="primary" type="button" :disabled="loading.registration || !selectedSlot" @click="createRegistration">确认挂号</button>
          </section>

          <section class="panel">
            <h2>挂号记录</h2>
            <div v-if="registrations.length" class="list">
              <article v-for="item in registrations" :key="String(item.registrationId)" class="list-row">
                <div>
                  <strong>#{{ text(item, "registrationId") }} {{ text(item, "departmentName") }}</strong>
                  <p>{{ text(item, "doctorName") }} / {{ text(item, "appointmentTime") }}</p>
                </div>
                <div class="row-actions">
                  <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
                  <button type="button" class="danger" :disabled="loading.registration || text(item, 'status') === 'CANCELLED'" @click="cancelRegistration(item.registrationId)">取消</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">暂无挂号记录。</div>
          </section>
        </aside>
      </section>

      <section v-show="active === 'records'" class="main-grid">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>病历列表</h2>
              <p>详情来自 `/api/medical-record/detail`。</p>
            </div>
            <button type="button" :disabled="loading.data" @click="refreshAll">刷新</button>
          </div>
          <div v-if="records.length" class="table-scroll">
            <table>
              <thead><tr><th>病历</th><th>主诉</th><th>诊断</th><th>生成方式</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="item in records" :key="String(item.medicalRecordId)">
                  <td>#{{ text(item, "medicalRecordId") }}</td>
                  <td>{{ text(item, "chiefComplaint") }}</td>
                  <td>{{ text(item, "diagnosis") }}</td>
                  <td>{{ item.aiGenerated ? "AI 草稿已确认" : "医生录入" }}</td>
                  <td><button type="button" @click="openRecord(item)">查看</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">暂无病历记录。医生保存病历后会显示在这里。</div>
        </section>

        <aside class="stack">
          <section class="panel">
            <h2>病历详情</h2>
            <div v-if="loading.detail" class="loading-state">正在加载病历详情...</div>
            <div v-else-if="selectedRecord" class="clinical-note">
              <strong>{{ text(selectedRecord, "diagnosis") }}</strong>
              <p>主诉：{{ text(selectedRecord, "chiefComplaint") }}</p>
              <p>现病史：{{ text(selectedRecord, "presentIllness", "未记录") }}</p>
              <p>体格检查：{{ text(selectedRecord, "physicalExam", "未记录") }}</p>
              <p>处理建议：{{ text(selectedRecord, "treatmentAdvice", "未记录") }}</p>
            </div>
            <div v-else class="empty-state">请选择一条病历查看详情。</div>
          </section>

          <section class="panel">
            <h2>处方</h2>
            <div v-if="prescriptions.length" class="list">
              <article v-for="item in prescriptions" :key="String(item.prescriptionId)" class="list-row">
                <div>
                  <strong>#{{ text(item, "prescriptionId") }} 风险 {{ text(item, "riskLevel", "未审核") }}</strong>
                  <p>{{ text(item, "createdAt") }} / {{ text(item, "status") }}</p>
                </div>
                <button type="button" @click="openPrescription(item)">查看</button>
              </article>
            </div>
            <div v-else class="empty-state">暂无处方记录。</div>
          </section>

          <section class="panel">
            <h2>处方详情</h2>
            <div v-if="selectedPrescription" class="clinical-note">
              <strong>风险等级：{{ text(selectedPrescription, "riskLevel", "未审核") }}</strong>
              <p>状态：{{ text(selectedPrescription, "status") }}</p>
              <ul>
                <li v-for="(item, index) in (selectedPrescription.items as DataRow[] || [])" :key="index">
                  {{ text(item, "drugName") }}，{{ text(item, "dosage") }}，{{ text(item, "frequency") }}，{{ text(item, "usageMethod") }}
                </li>
              </ul>
              <p>禁忌和建议由医生端处方审核后确认，患者端展示最终处方状态。</p>
            </div>
            <div v-else class="empty-state">请选择一张处方查看药品明细。</div>
          </section>
        </aside>
      </section>
    </template>
  </main>
</template>
