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
const latestRegistration = computed(() => activeRegistrations.value[0] ?? registrations.value[0] ?? null);
const latestRecord = computed(() => records.value[0] ?? null);
const latestPrescription = computed(() => prescriptions.value[0] ?? null);
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
    setNotice("已进入患者服务工作台。");
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
    active.value = "dashboard";
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
    <header class="patient-header">
      <div class="brand-block">
        <span class="brand-mark">医</span>
        <div>
          <p class="eyebrow">患者服务</p>
          <h1>智慧云脑患者门户</h1>
        </div>
      </div>

      <nav v-if="session && !permissionError" class="primary-nav" aria-label="患者服务导航">
        <button type="button" :class="{ active: active === 'dashboard' }" @click="active = 'dashboard'">工作台</button>
        <button type="button" :class="{ active: active === 'triage' }" @click="active = 'triage'">AI分诊</button>
        <button type="button" :class="{ active: active === 'registration' }" @click="active = 'registration'">挂号</button>
        <button type="button" :class="{ active: active === 'records' }" @click="active = 'records'">病历</button>
        <button type="button" :class="{ active: active === 'prescriptions' }" @click="active = 'prescriptions'">处方</button>
      </nav>

      <div v-if="session" class="account-area">
        <span>{{ session.name }}</span>
        <button type="button" @click="logout">退出</button>
      </div>
    </header>

    <div v-if="error" class="notice error">{{ error }}</div>
    <div v-if="notice" class="notice success">{{ notice }}</div>
    <div v-if="permissionError" class="notice error">
      <span>{{ permissionError }}</span>
      <button type="button" @click="logout">切换账号</button>
    </div>

    <section v-if="loading.boot" class="panel">
      <div class="loading-state">正在加载患者服务配置...</div>
    </section>

    <section v-else-if="!session || permissionError" class="auth-layout">
      <div class="auth-intro">
        <p class="eyebrow">就诊前服务</p>
        <h2>先完成身份登录，再进行分诊、挂号和诊后资料查看。</h2>
        <div class="auth-steps">
          <span>1. 登录或注册</span>
          <span>2. 提交症状分诊</span>
          <span>3. 选择号源并确认</span>
        </div>
      </div>

      <div class="auth-forms">
        <form class="panel compact-panel" @submit.prevent="login">
          <div class="panel-title">
            <h2>患者登录</h2>
            <p>使用已注册手机号或账号进入患者服务。</p>
          </div>
          <label>账号<input v-model.trim="loginForm.account" autocomplete="username" /></label>
          <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
          <button class="primary" type="submit" :disabled="loading.auth">登录</button>
        </form>

        <form class="panel" @submit.prevent="register">
          <div class="panel-title">
            <h2>患者注册</h2>
            <p>信息用于挂号和医生接诊识别，请保持真实准确。</p>
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
            <label>过敏史<input v-model.trim="registerForm.allergyHistory" placeholder="如无可留空" /></label>
          </div>
          <label>既往史<textarea v-model.trim="registerForm.pastHistory" rows="3" placeholder="填写主要既往疾病或手术史" /></label>
          <button type="submit" :disabled="loading.auth">注册</button>
        </form>
      </div>
    </section>

    <template v-else>
      <section v-show="active === 'dashboard'" class="page-stack">
        <div class="hero-row">
          <section class="panel patient-summary">
            <div class="panel-title">
              <p class="eyebrow">当前患者</p>
              <h2>{{ text(patient, "name", session.name) }}</h2>
              <p>{{ text(patient, "phone", "未记录手机号") }} · {{ text(patient, "age", "年龄未记录") }} 岁</p>
            </div>
            <div class="summary-grid">
              <div><span>过敏史</span><strong>{{ text(patient, "allergyHistory", "未记录") }}</strong></div>
              <div><span>既往史</span><strong>{{ text(patient, "pastHistory", "未记录") }}</strong></div>
              <div><span>推荐科室</span><strong>{{ recommendedDepartment || "待分诊" }}</strong></div>
            </div>
          </section>

          <section class="panel next-step">
            <div class="panel-title">
              <p class="eyebrow">下一步建议</p>
              <h2>{{ latestTriage ? "确认挂号与就诊安排" : "先提交分诊信息" }}</h2>
              <p>{{ latestTriage ? "根据当前分诊结果选择合适号源，就诊后可查看病历与处方。" : "填写主要症状后，系统会给出推荐科室供挂号参考。" }}</p>
            </div>
            <div class="action-row">
              <button class="primary" type="button" @click="active = latestTriage ? 'registration' : 'triage'">
                {{ latestTriage ? "去挂号" : "提交分诊" }}
              </button>
              <button type="button" :disabled="loading.data" @click="refreshAll">刷新资料</button>
            </div>
          </section>
        </div>

        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-title">
              <h2>当前分诊结果</h2>
              <p>分诊结果用于挂号推荐，最终诊断以医生接诊为准。</p>
            </div>
            <div v-if="latestTriage" class="result-panel">
              <span class="tag" :class="statusClass(latestTriage.status)">{{ text(latestTriage, "status") }}</span>
              <h3>{{ text(latestTriage, "recommendedDepartment", "待人工确认") }}</h3>
              <p>{{ text(latestTriage, "reason", "暂无建议说明") }}</p>
            </div>
            <div v-else class="empty-state">暂无分诊记录，请先提交症状信息。</div>
          </section>

          <section class="panel">
            <div class="panel-title">
              <h2>最近挂号</h2>
              <p>已提交的预约会在这里显示处理状态。</p>
            </div>
            <div v-if="latestRegistration" class="record-card">
              <strong>{{ text(latestRegistration, "departmentName") }} · {{ text(latestRegistration, "doctorName") }}</strong>
              <span>{{ text(latestRegistration, "appointmentTime") }}</span>
              <span class="tag" :class="statusClass(latestRegistration.status)">{{ text(latestRegistration, "status") }}</span>
            </div>
            <div v-else class="empty-state">暂无挂号记录。</div>
          </section>

          <section class="panel">
            <div class="panel-title">
              <h2>最近病历</h2>
              <p>医生保存后，病历摘要会同步到患者端。</p>
            </div>
            <div v-if="latestRecord" class="record-card">
              <strong>{{ text(latestRecord, "diagnosis", "未记录诊断") }}</strong>
              <span>{{ text(latestRecord, "chiefComplaint", "未记录主诉") }}</span>
              <button type="button" @click="active = 'records'; openRecord(latestRecord)">查看详情</button>
            </div>
            <div v-else class="empty-state">暂无病历记录。</div>
          </section>

          <section class="panel">
            <div class="panel-title">
              <h2>最近处方</h2>
              <p>处方以医生确认后的内容为准。</p>
            </div>
            <div v-if="latestPrescription" class="record-card">
              <strong>处方 #{{ text(latestPrescription, "prescriptionId") }}</strong>
              <span>风险等级：{{ text(latestPrescription, "riskLevel", "未审核") }}</span>
              <button type="button" @click="active = 'prescriptions'; openPrescription(latestPrescription)">查看详情</button>
            </div>
            <div v-else class="empty-state">暂无处方记录。</div>
          </section>
        </div>
      </section>

      <section v-show="active === 'triage'" class="triage-layout">
        <form class="panel triage-form" @submit.prevent="submitTriage">
          <div class="panel-title">
            <p class="eyebrow">AI分诊</p>
            <h2>填写本次主要症状</h2>
            <p>请描述最困扰您的症状、持续时间和变化情况。</p>
          </div>
          <label>主要症状<textarea v-model.trim="triageForm.symptoms" rows="5" placeholder="例如：反复头痛三天，伴随恶心..." /></label>
          <div class="form-grid">
            <label>持续时间<input v-model.trim="triageForm.duration" placeholder="例如：3天、2周" /></label>
            <label>严重程度
              <select v-model="triageForm.severity">
                <option value="LOW">轻度</option>
                <option value="MEDIUM">中度</option>
                <option value="HIGH">重度或明显加重</option>
              </select>
            </label>
          </div>
          <label>补充说明<textarea v-model.trim="triageForm.extra" rows="3" placeholder="可填写诱因、用药、既往类似情况" /></label>
          <button class="primary" type="submit" :disabled="loading.triage || !canSubmitTriage">提交分诊</button>
        </form>

        <aside class="side-stack">
          <section class="panel">
            <div class="panel-title">
              <h2>分诊结果</h2>
              <p>推荐科室和原因会在提交后更新。</p>
            </div>
            <div v-if="loading.triage" class="loading-state">正在分析并保存分诊记录...</div>
            <div v-else-if="latestTriage" class="result-panel">
              <span class="tag" :class="statusClass(latestTriage.status)">{{ text(latestTriage, "status") }}</span>
              <h3>{{ text(latestTriage, "recommendedDepartment", "待确认") }}</h3>
              <p>{{ text(latestTriage, "reason", "暂无说明") }}</p>
            </div>
            <div v-else class="empty-state">提交后将在此显示分诊结果。</div>
          </section>

          <section class="panel">
            <div class="panel-title">
              <h2>历史分诊</h2>
              <p>用于回看最近症状描述和推荐记录。</p>
            </div>
            <div v-if="triageHistory.length" class="list">
              <article v-for="item in triageHistory" :key="String(item.triageRecordId)" class="list-row">
                <div>
                  <strong>{{ text(item, "recommendedDepartment", "待确认") }}</strong>
                  <p>{{ text(item, "chiefComplaint") }}</p>
                </div>
                <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
              </article>
            </div>
            <div v-else class="empty-state">暂无历史分诊记录。</div>
          </section>
        </aside>
      </section>

      <section v-show="active === 'registration'" class="registration-layout">
        <section class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">挂号</p>
              <h2>选择科室、医生和可用号源</h2>
              <p>优先展示与当前分诊推荐科室匹配的号源。</p>
            </div>
            <button type="button" :disabled="loading.data" @click="refreshAll">刷新号源</button>
          </div>
          <div class="filter-strip">
            <span>推荐科室：<strong>{{ recommendedDepartment || "暂无" }}</strong></span>
            <span>可用医生：<strong>{{ visibleDoctors.length || doctors.length }}</strong></span>
            <span>可预约号源：<strong>{{ visibleSlots.length || slots.length }}</strong></span>
          </div>
          <div v-if="loading.data" class="loading-state">正在加载号源...</div>
          <div v-else-if="(visibleSlots.length || slots.length)" class="slot-list">
            <article
              v-for="slot in (visibleSlots.length ? visibleSlots : slots)"
              :key="String(slot.slotId)"
              class="slot-row"
              :class="{ selected: toNumber(slot.slotId) === selectedSlotId }"
              @click="selectedSlotId = toNumber(slot.slotId)"
            >
              <div>
                <strong>{{ text(slot, "departmentName") }} · {{ text(slot, "doctorName") }}</strong>
                <span>{{ text(slot, "startTime") }}</span>
              </div>
              <div>
                <span>余号 {{ text(slot, "remainingCapacity", "0") }}/{{ text(slot, "capacity", "0") }}</span>
                <span class="tag" :class="statusClass(slot.status)">{{ text(slot, "status") }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">暂无可预约号源，请稍后刷新或联系导诊台。</div>
        </section>

        <aside class="side-stack">
          <section class="panel confirm-panel">
            <div class="panel-title">
              <h2>预约确认</h2>
              <p>提交前请核对科室、医生和时间。</p>
            </div>
            <div class="summary-grid single">
              <div><span>科室</span><strong>{{ text(selectedSlot, "departmentName", "未选择") }}</strong></div>
              <div><span>医生</span><strong>{{ text(selectedSlot, "doctorName", "未选择") }}</strong></div>
              <div><span>时间</span><strong>{{ text(selectedSlot, "startTime", "未选择") }}</strong></div>
            </div>
            <button class="primary" type="button" :disabled="loading.registration || !selectedSlot" @click="createRegistration">确认挂号</button>
          </section>

          <section class="panel">
            <div class="panel-title">
              <h2>挂号记录</h2>
              <p>可取消未完成的挂号记录。</p>
            </div>
            <div v-if="registrations.length" class="list">
              <article v-for="item in registrations" :key="String(item.registrationId)" class="list-row">
                <div>
                  <strong>#{{ text(item, "registrationId") }} {{ text(item, "departmentName") }}</strong>
                  <p>{{ text(item, "doctorName") }} · {{ text(item, "appointmentTime") }}</p>
                </div>
                <div class="row-actions">
                  <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
                  <button type="button" class="danger subtle" :disabled="loading.registration || text(item, 'status') === 'CANCELLED'" @click="cancelRegistration(item.registrationId)">取消</button>
                </div>
              </article>
            </div>
            <div v-else class="empty-state">暂无挂号记录。</div>
          </section>
        </aside>
      </section>

      <section v-show="active === 'records'" class="master-detail">
        <section class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">病历</p>
              <h2>病历列表</h2>
              <p>选择一条病历查看完整医疗记录。</p>
            </div>
            <button type="button" :disabled="loading.data" @click="refreshAll">刷新</button>
          </div>
          <div v-if="records.length" class="table-scroll">
            <table>
              <thead><tr><th>病历号</th><th>主诉</th><th>诊断</th><th>生成方式</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="item in records" :key="String(item.medicalRecordId)">
                  <td>#{{ text(item, "medicalRecordId") }}</td>
                  <td>{{ text(item, "chiefComplaint") }}</td>
                  <td>{{ text(item, "diagnosis") }}</td>
                  <td>{{ item.aiGenerated ? "AI草稿已确认" : "医生录入" }}</td>
                  <td><button type="button" @click="openRecord(item)">查看详情</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">暂无病历记录，医生保存病历后会显示在这里。</div>
        </section>

        <aside class="panel detail-panel">
          <div class="panel-title">
            <h2>病历详情</h2>
            <p>以医生最终保存内容为准。</p>
          </div>
          <div v-if="loading.detail" class="loading-state">正在加载病历详情...</div>
          <div v-else-if="selectedRecord" class="clinical-record">
            <h3>{{ text(selectedRecord, "diagnosis") }}</h3>
            <dl>
              <dt>主诉</dt><dd>{{ text(selectedRecord, "chiefComplaint") }}</dd>
              <dt>现病史</dt><dd>{{ text(selectedRecord, "presentIllness", "未记录") }}</dd>
              <dt>既往史</dt><dd>{{ text(selectedRecord, "pastHistory", "未记录") }}</dd>
              <dt>体格检查</dt><dd>{{ text(selectedRecord, "physicalExam", "未记录") }}</dd>
              <dt>处理建议</dt><dd>{{ text(selectedRecord, "treatmentAdvice", "未记录") }}</dd>
            </dl>
          </div>
          <div v-else class="empty-state">请选择一条病历查看详情。</div>
        </aside>
      </section>

      <section v-show="active === 'prescriptions'" class="master-detail">
        <section class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">处方</p>
              <h2>处方列表</h2>
              <p>查看医生确认后的处方和风险审核结果。</p>
            </div>
            <button type="button" :disabled="loading.data" @click="refreshAll">刷新</button>
          </div>
          <div v-if="prescriptions.length" class="list">
            <article v-for="item in prescriptions" :key="String(item.prescriptionId)" class="list-row prescription-row">
              <div>
                <strong>处方 #{{ text(item, "prescriptionId") }}</strong>
                <p>{{ text(item, "createdAt") }} · {{ text(item, "status") }}</p>
              </div>
              <div class="row-actions">
                <span class="tag" :class="statusClass(item.riskLevel)">{{ text(item, "riskLevel", "未审核") }}</span>
                <button type="button" @click="openPrescription(item)">查看详情</button>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">暂无处方记录。</div>
        </section>

        <aside class="panel detail-panel">
          <div class="panel-title">
            <h2>处方详情</h2>
            <p>如有用药疑问，请以医生说明为准。</p>
          </div>
          <div v-if="selectedPrescription" class="clinical-record">
            <h3>风险等级：{{ text(selectedPrescription, "riskLevel", "未审核") }}</h3>
            <dl>
              <dt>状态</dt><dd>{{ text(selectedPrescription, "status") }}</dd>
              <dt>药品明细</dt>
              <dd>
                <ul class="drug-list">
                  <li v-for="(item, index) in (selectedPrescription.items as DataRow[] || [])" :key="index">
                    {{ text(item, "drugName") }}，{{ text(item, "dosage") }}，{{ text(item, "frequency") }}，{{ text(item, "usageMethod") }}
                  </li>
                </ul>
              </dd>
            </dl>
          </div>
          <div v-else class="empty-state">请选择一张处方查看药品明细。</div>
        </aside>
      </section>
    </template>
  </main>
</template>
