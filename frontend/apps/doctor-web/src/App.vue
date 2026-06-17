<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  fieldText,
  formatApiError,
  medicalRecordStreamUrl,
  notificationWebSocketUrl,
  statusClass,
  toNumber,
  type DataRow,
  type DrugItem,
} from "@smart-cloud-brain/shared-api";
import { useAuthStore, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { session, permissionError } = storeToRefs(auth);
const { registrations, triageRecords, records, prescriptions, drugs, notifications, streamText, streamStatus } = storeToRefs(workflow);

const loading = reactive({
  boot: true,
  auth: false,
  data: false,
  record: false,
  prescription: false,
  notification: false,
});
const error = ref("");
const notice = ref("");
const selectedRegistrationId = ref<number | null>(null);
const socketStatus = ref("未连接");
const queueKeyword = ref("");
const queueStatus = ref("");
const loginForm = reactive({ account: "", password: "" });
const dialogueText = ref("");
const medicalForm = reactive({
  registrationId: 0,
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  physicalExam: "",
  diagnosis: "",
  treatmentAdvice: "",
  aiGenerated: true,
});
const prescription = reactive({
  medicalRecordId: 0,
  riskLevel: "UNREVIEWED",
  drugs: [{ drugName: "", dosage: "", frequency: "", usageMethod: "口服" }] as DrugItem[],
});
const checkResult = ref<DataRow | null>(null);
let socket: WebSocket | null = null;
let recordStream: EventSource | null = null;
let pollTimer: number | null = null;
let reconnectTimer: number | null = null;
let unbindUnauthorized: (() => void) | null = null;

const selectedRegistration = computed(() =>
  registrations.value.find((item) => toNumber(item.registrationId) === selectedRegistrationId.value) ?? null
);
const selectedTriage = computed(() => {
  const triageId = toNumber(selectedRegistration.value?.triageRecordId, 0);
  return triageRecords.value.find((item) => toNumber(item.triageRecordId) === triageId) ?? null;
});
const todayQueue = computed(() => registrations.value.filter((item) => fieldText(item, "status") !== "CANCELLED"));
const filteredQueue = computed(() => {
  const keyword = queueKeyword.value.trim().toLowerCase();
  return todayQueue.value.filter((item) => {
    const statusMatched = !queueStatus.value || fieldText(item, "status") === queueStatus.value;
    const haystack = `${fieldText(item, "patientName", "")} ${fieldText(item, "patientId", "")} ${fieldText(item, "departmentName", "")}`.toLowerCase();
    const keywordMatched = !keyword || haystack.includes(keyword);
    return statusMatched && keywordMatched;
  });
});
const unreadNotifications = computed(() => notifications.value.filter((item) => fieldText(item, "readStatus") !== "READ"));
const canSaveRecord = computed(() =>
  medicalForm.registrationId > 0 && medicalForm.chiefComplaint.trim().length > 0 && medicalForm.diagnosis.trim().length > 0
);
const canCheckPrescription = computed(() =>
  Boolean(selectedRegistration.value) && prescription.drugs.length > 0 && prescription.drugs.every((item) =>
    item.drugName.trim() && item.dosage.trim() && item.frequency.trim() && item.usageMethod.trim()
  )
);
const canCreatePrescription = computed(() => prescription.medicalRecordId > 0 && canCheckPrescription.value);
const checkInteractions = computed(() => (checkResult.value?.interactions as string[] | undefined) ?? []);

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

async function login() {
  if (!loginForm.account.trim() || !loginForm.password.trim()) {
    setError("请输入医生账号和密码。");
    return;
  }
  await run("auth", "登录失败", async () => {
    const nextSession = await api.loginDoctor(loginForm.account.trim(), loginForm.password);
    auth.save("doctor-session", nextSession, "DOCTOR");
    if (!auth.requireRole("DOCTOR")) return;
    await refresh();
    connectNotifications();
    setNotice("已进入医生接诊工作台。");
  });
}

function logout() {
  stopRealtime();
  auth.logout();
  selectedRegistrationId.value = null;
}

async function refresh() {
  if (!session.value || !auth.requireRole("DOCTOR")) return;
  await run("data", "数据同步失败", async () => {
    await workflow.refresh(auth.token());
    if (!selectedRegistrationId.value && todayQueue.value[0]) {
      selectRegistration(todayQueue.value[0]);
    }
  });
}

function selectRegistration(item: DataRow) {
  selectedRegistrationId.value = toNumber(item.registrationId);
  const chief = fieldText(selectedTriage.value, "chiefComplaint", fieldText(item, "patientName", ""));
  medicalForm.registrationId = selectedRegistrationId.value ?? 0;
  medicalForm.chiefComplaint = chief === "-" ? "" : chief;
  medicalForm.presentIllness = "";
  medicalForm.pastHistory = "";
  medicalForm.physicalExam = "";
  medicalForm.diagnosis = "";
  medicalForm.treatmentAdvice = "";
  medicalForm.aiGenerated = true;
  prescription.medicalRecordId = 0;
  prescription.riskLevel = "UNREVIEWED";
  checkResult.value = null;
  streamText.value = "";
  streamStatus.value = "READY";
}

function applyDraft(draft: DataRow) {
  medicalForm.chiefComplaint = fieldText(draft, "chiefComplaint", medicalForm.chiefComplaint);
  medicalForm.presentIllness = fieldText(draft, "presentIllness", medicalForm.presentIllness);
  medicalForm.pastHistory = fieldText(draft, "pastHistory", medicalForm.pastHistory);
  medicalForm.physicalExam = fieldText(draft, "physicalExam", medicalForm.physicalExam);
  medicalForm.diagnosis = fieldText(draft, "diagnosis", medicalForm.diagnosis);
  medicalForm.treatmentAdvice = fieldText(draft, "treatmentAdvice", medicalForm.treatmentAdvice);
  medicalForm.aiGenerated = true;
}

async function generateRecord() {
  if (!selectedRegistrationId.value) {
    setError("请先选择待接诊患者。");
    return;
  }
  if (!dialogueText.value.trim()) {
    setError("请填写问诊对话摘要后再生成病历草稿。");
    return;
  }
  await run("record", "病历生成失败", async () => {
    streamText.value = "";
    streamStatus.value = "GENERATING";
    try {
      await generateRecordByStream(selectedRegistrationId.value);
    } catch {
      const draft = await api.generateMedicalRecord(auth.token(), {
        registrationId: selectedRegistrationId.value,
        departmentCode: fieldText(selectedTriage.value, "departmentCode", ""),
        dialogueText: dialogueText.value,
      });
      applyDraft(draft);
      streamText.value = "流式生成不可用，已使用普通生成接口返回结构化草稿。";
      streamStatus.value = "DRAFT_READY";
    }
    setNotice("病历草稿已生成，请医生确认后保存。");
  });
}

function generateRecordByStream(registrationId: number) {
  return new Promise<void>((resolve, reject) => {
    recordStream?.close();
    recordStream = new EventSource(medicalRecordStreamUrl(registrationId, auth.token()));
    recordStream.addEventListener("delta", (event) => {
      const data = parseEventData(event);
      streamText.value += `${fieldText(data, "text", event.data)}\n`;
    });
    recordStream.addEventListener("structured", (event) => {
      applyDraft(parseEventData(event));
      streamStatus.value = "DRAFT_READY";
    });
    recordStream.addEventListener("done", () => {
      recordStream?.close();
      recordStream = null;
      streamStatus.value = "DRAFT_READY";
      resolve();
    });
    recordStream.addEventListener("error", () => {
      recordStream?.close();
      recordStream = null;
      streamStatus.value = "FAILED";
      reject(new Error("stream failed"));
    });
  });
}

function parseEventData(event: MessageEvent) {
  try {
    return JSON.parse(event.data) as DataRow;
  } catch {
    return { text: event.data };
  }
}

async function saveRecord() {
  if (!canSaveRecord.value) {
    setError("主诉和诊断为必填项，且必须选择患者。");
    return;
  }
  await run("record", "病历保存失败", async () => {
    const saved = await api.saveMedicalRecord(auth.token(), { ...medicalForm });
    prescription.medicalRecordId = toNumber(saved.medicalRecordId);
    await workflow.refresh(auth.token());
    setNotice("病历已保存，可以继续处方录入和审核。");
  });
}

function addDrug() {
  prescription.drugs.push({ drugName: "", dosage: "", frequency: "", usageMethod: "口服" });
}

function removeDrug(index: number) {
  if (prescription.drugs.length === 1) {
    prescription.drugs[0] = { drugName: "", dosage: "", frequency: "", usageMethod: "口服" };
    return;
  }
  prescription.drugs.splice(index, 1);
}

async function checkPrescription() {
  if (!canCheckPrescription.value) {
    setError("请先选择患者并完整填写药品、剂量、频次和用法。");
    return;
  }
  await run("prescription", "处方审核失败", async () => {
    checkResult.value = await api.checkPrescription(auth.token(), {
      patientId: toNumber(selectedRegistration.value?.patientId),
      doctorId: session.value?.userId,
      drugs: prescription.drugs,
    });
    prescription.riskLevel = fieldText(checkResult.value, "riskLevel", "UNREVIEWED");
    await workflow.refresh(auth.token());
    setNotice("处方审核完成，请查看风险结果。");
  });
}

async function createPrescription() {
  if (!canCreatePrescription.value) {
    setError("请先保存病历并完成处方药品录入。");
    return;
  }
  await run("prescription", "处方创建失败", async () => {
    await api.createPrescription(auth.token(), {
      patientId: toNumber(selectedRegistration.value?.patientId),
      medicalRecordId: prescription.medicalRecordId,
      riskLevel: prescription.riskLevel,
      drugs: prescription.drugs,
    });
    await workflow.refresh(auth.token());
    setNotice("处方已确认。当前后端未提供完成接诊接口，队列状态不会在前端伪造变更。");
  });
}

async function markRead(item: DataRow) {
  await run("notification", "标记已读失败", async () => {
    await api.markNotificationRead(auth.token(), toNumber(item.notificationId));
    await workflow.refresh(auth.token());
  });
}

function connectNotifications() {
  if (!session.value || !auth.requireRole("DOCTOR")) return;
  stopRealtime();
  socketStatus.value = "连接中";
  socket = new WebSocket(notificationWebSocketUrl(session.value.userId, auth.token()));
  socket.onopen = () => {
    socketStatus.value = "实时通知已连接";
    stopPolling();
  };
  socket.onmessage = async () => {
    await workflow.refresh(auth.token());
    setNotice("收到新的处方风险通知。");
  };
  socket.onerror = () => {
    socketStatus.value = "实时通知不可用，已启用轮询";
    startPolling();
  };
  socket.onclose = () => {
    if (session.value) {
      socketStatus.value = "实时通知断开，正在重连";
      startPolling();
      reconnectTimer = window.setTimeout(connectNotifications, 5000);
    }
  };
}

function startPolling() {
  if (pollTimer) return;
  pollTimer = window.setInterval(() => {
    if (session.value) workflow.refresh(auth.token()).catch(() => undefined);
  }, 15000);
}

function stopPolling() {
  if (pollTimer) window.clearInterval(pollTimer);
  pollTimer = null;
}

function stopRealtime() {
  socket?.close();
  socket = null;
  recordStream?.close();
  recordStream = null;
  stopPolling();
  if (reconnectTimer) window.clearTimeout(reconnectTimer);
  reconnectTimer = null;
  socketStatus.value = "未连接";
}

onMounted(async () => {
  unbindUnauthorized = auth.bindUnauthorized();
  auth.load("doctor-session", "DOCTOR");
  if (session.value && !permissionError.value) {
    await refresh();
    connectNotifications();
  }
  loading.boot = false;
});

onBeforeUnmount(() => {
  unbindUnauthorized?.();
  stopRealtime();
});
</script>

<template>
  <main class="doctor-shell">
    <header class="doctor-topbar">
      <div>
        <p class="eyebrow">医生工作台</p>
        <h1>接诊处理</h1>
        <p>今日队列、病历文书和处方审核集中处理。</p>
      </div>
      <div v-if="session" class="topbar-actions">
        <span class="tag info">{{ socketStatus }}</span>
        <span>{{ session.name }} · {{ session.role }} #{{ session.userId }}</span>
        <button type="button" :disabled="loading.data" @click="refresh">同步队列</button>
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
      <div class="loading-state">正在加载医生端配置...</div>
    </section>

    <form v-else-if="!session || permissionError" class="panel login-panel" @submit.prevent="login">
      <div class="panel-title">
        <p class="eyebrow">医生登录</p>
        <h2>进入接诊工作台</h2>
        <p>登录后可查看今日队列、生成病历并进行处方审核。</p>
      </div>
      <div class="form-grid">
        <label>账号<input v-model.trim="loginForm.account" autocomplete="username" /></label>
        <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
      </div>
      <button class="primary" type="submit" :disabled="loading.auth">进入工作台</button>
    </form>

    <section v-else class="doctor-workspace">
      <aside class="queue-column panel">
        <div class="panel-title">
          <p class="eyebrow">今日队列</p>
          <h2>待接诊患者</h2>
          <p>{{ filteredQueue.length }} / {{ todayQueue.length }} 条记录</p>
        </div>
        <div class="queue-filters">
          <label>搜索<input v-model.trim="queueKeyword" placeholder="姓名、患者ID、科室" /></label>
          <label>状态
            <select v-model="queueStatus">
              <option value="">全部</option>
              <option value="CREATED">已创建</option>
              <option value="CONFIRMED">已确认</option>
              <option value="PENDING">待处理</option>
            </select>
          </label>
        </div>
        <div v-if="loading.data" class="loading-state">正在同步队列...</div>
        <div v-else-if="filteredQueue.length" class="queue-list">
          <button
            v-for="item in filteredQueue"
            :key="String(item.registrationId)"
            type="button"
            class="queue-item"
            :class="{ active: toNumber(item.registrationId) === selectedRegistrationId }"
            @click="selectRegistration(item)"
          >
            <span>
              <strong>{{ text(item, "patientName", `患者${text(item, "patientId")}`) }}</strong>
              <small>#{{ text(item, "registrationId") }} · {{ text(item, "departmentName") }}</small>
              <small>{{ text(item, "appointmentTime") }}</small>
            </span>
            <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
          </button>
        </div>
        <div v-else class="empty-state">当前筛选条件下暂无待接诊患者。</div>
      </aside>

      <section class="encounter-column">
        <section class="panel patient-panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">当前患者</p>
              <h2>{{ selectedRegistration ? text(selectedRegistration, "patientName", `患者${text(selectedRegistration, "patientId")}`) : "未选择患者" }}</h2>
              <p>{{ selectedRegistration ? `${text(selectedRegistration, "departmentName")} · ${text(selectedRegistration, "appointmentTime")}` : "请从左侧队列选择患者开始接诊。" }}</p>
            </div>
            <span v-if="selectedRegistration" class="tag" :class="statusClass(selectedRegistration.status)">{{ text(selectedRegistration, "status") }}</span>
          </div>

          <div v-if="selectedRegistration" class="patient-summary">
            <div><span>挂号号</span><strong>#{{ text(selectedRegistration, "registrationId") }}</strong></div>
            <div><span>患者ID</span><strong>{{ text(selectedRegistration, "patientId") }}</strong></div>
            <div><span>医生</span><strong>{{ text(selectedRegistration, "doctorName", session.name) }}</strong></div>
          </div>
          <div v-else class="empty-state">未选择患者时不会生成病历或处方。</div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h2>分诊记录</h2>
            <p>接诊前快速查看主诉、推荐科室和处理建议。</p>
          </div>
          <div v-if="selectedTriage" class="clinical-note">
            <div class="note-head">
              <strong>{{ text(selectedTriage, "recommendedDepartment", "分诊科室待确认") }}</strong>
              <span class="tag" :class="statusClass(selectedTriage.status)">{{ text(selectedTriage, "status") }}</span>
            </div>
            <p>{{ text(selectedTriage, "chiefComplaint") }}</p>
            <p>{{ text(selectedTriage, "reason") }}</p>
          </div>
          <div v-else class="empty-state">当前挂号未关联分诊记录。</div>
        </section>

        <section class="panel record-editor">
          <div class="panel-header">
            <div class="panel-title">
              <h2>病历生成与编辑</h2>
              <p>先填写问诊摘要，可生成结构化草稿；医生确认后保存。</p>
            </div>
            <span class="tag info">{{ streamStatus }}</span>
          </div>
          <label>问诊摘要<textarea v-model.trim="dialogueText" rows="4" placeholder="记录患者主诉、现病史、查体要点和医嘱考虑" /></label>
          <div class="toolbar">
            <button class="primary" type="button" :disabled="loading.record || !selectedRegistrationId" @click="generateRecord">生成病历草稿</button>
            <button type="button" :disabled="loading.record || !canSaveRecord" @click="saveRecord">保存病历</button>
          </div>
          <div v-if="streamText" class="stream-box">
            <strong>生成状态</strong>
            <p>{{ streamText }}</p>
          </div>
          <div class="medical-form">
            <label class="wide">主诉<input v-model.trim="medicalForm.chiefComplaint" /></label>
            <label class="wide">诊断<input v-model.trim="medicalForm.diagnosis" /></label>
            <label>现病史<textarea v-model.trim="medicalForm.presentIllness" rows="4" /></label>
            <label>既往史<textarea v-model.trim="medicalForm.pastHistory" rows="4" /></label>
            <label>体格检查<textarea v-model.trim="medicalForm.physicalExam" rows="4" /></label>
            <label>处理建议<textarea v-model.trim="medicalForm.treatmentAdvice" rows="4" /></label>
          </div>
        </section>
      </section>

      <aside class="right-column">
        <section class="panel prescription-panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">处方审核</p>
              <h2>药品录入</h2>
              <p>保存病历后可确认处方；审核结果来自处方审核接口。</p>
            </div>
            <span class="tag" :class="statusClass(prescription.riskLevel)">{{ prescription.riskLevel }}</span>
          </div>
          <div class="drug-list">
            <div v-for="(drug, index) in prescription.drugs" :key="index" class="drug-row">
              <label>药品<input v-model.trim="drug.drugName" list="drug-options" /></label>
              <label>剂量<input v-model.trim="drug.dosage" /></label>
              <label>频次<input v-model.trim="drug.frequency" /></label>
              <label>用法<input v-model.trim="drug.usageMethod" /></label>
              <button class="danger subtle" type="button" @click="removeDrug(index)">删除</button>
            </div>
          </div>
          <datalist id="drug-options">
            <option v-for="drug in drugs" :key="String(drug.id)" :value="String(drug.name)" />
          </datalist>
          <div class="toolbar">
            <button type="button" @click="addDrug">新增药品</button>
            <button class="primary" type="button" :disabled="loading.prescription || !canCheckPrescription" @click="checkPrescription">审核处方</button>
            <button type="button" :disabled="loading.prescription || !canCreatePrescription" @click="createPrescription">确认处方</button>
          </div>
          <div v-if="checkResult" class="risk-panel" :class="statusClass(checkResult.riskLevel)">
            <strong>风险等级：{{ text(checkResult, "riskLevel", "未返回") }}</strong>
            <p>{{ text(checkResult, "suggestions", "请医生复核用药风险。") }}</p>
            <p v-if="checkInteractions.length">相互作用：{{ checkInteractions.join("、") }}</p>
          </div>
          <div v-else class="empty-state">处方审核结果会显示在这里。</div>
        </section>

        <section class="panel notification-panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">通知</p>
              <h2>风险与系统通知</h2>
              <p>{{ unreadNotifications.length }} 条未读</p>
            </div>
            <button type="button" :disabled="loading.notification" @click="refresh">刷新</button>
          </div>
          <div v-if="notifications.length" class="notification-list">
            <article v-for="item in notifications" :key="String(item.notificationId)" class="notification-row">
              <div>
                <strong>{{ text(item, "title") }}</strong>
                <p>{{ text(item, "content") }}</p>
                <div class="tag-row">
                  <span class="tag" :class="statusClass(item.riskLevel)">{{ text(item, "riskLevel", "INFO") }}</span>
                  <span class="tag" :class="statusClass(item.readStatus)">{{ text(item, "readStatus") }}</span>
                </div>
              </div>
              <button type="button" :disabled="text(item, 'readStatus') === 'READ'" @click="markRead(item)">已读</button>
            </article>
          </div>
          <div v-else class="empty-state">暂无通知。</div>
        </section>

        <section class="panel output-panel">
          <div class="panel-title">
            <h2>已保存输出</h2>
            <p>用于快速核对当前账号下近期病历和处方。</p>
          </div>
          <div class="mini-table">
            <div v-for="item in records.slice(0, 4)" :key="`r-${String(item.medicalRecordId)}`">
              <span>病历 #{{ text(item, "medicalRecordId") }}</span>
              <strong>{{ text(item, "diagnosis") }}</strong>
            </div>
            <div v-for="item in prescriptions.slice(0, 4)" :key="`p-${String(item.prescriptionId)}`">
              <span>处方 #{{ text(item, "prescriptionId") }}</span>
              <strong>{{ text(item, "riskLevel", "未审核") }}</strong>
            </div>
          </div>
          <div v-if="!records.length && !prescriptions.length" class="empty-state">暂无已保存病历或处方。</div>
        </section>
      </aside>
    </section>
  </main>
</template>
