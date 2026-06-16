<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  medicalRecordStreamUrl,
  notificationWebSocketUrl,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";

type DataRow = Record<string, unknown>;

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { session } = storeToRefs(auth);
const { registrations, records, drugs, notifications, streamText, streamStatus } = storeToRefs(workflow);

const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "doctor1", password: "123456" });
const selectedRegistrationId = ref<number | null>(null);
const selectedPatientId = ref<number | null>(null);
const dialogueText = ref("患者自述咽痛、鼻塞、低热两天，夜间咳嗽明显。否认胸痛、呼吸困难，既往无哮喘史，未使用抗生素。");
const medicalForm = ref({
  registrationId: 0,
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  physicalExam: "生命体征平稳，咽部轻度充血。",
  diagnosis: "",
  treatmentAdvice: "",
  aiGenerated: true,
});
const prescription = ref({
  medicalRecordId: 0,
  riskLevel: "UNREVIEWED",
  drugs: [{ drugName: "对乙酰氨基酚片", dosage: "0.5g", frequency: "必要时", usageMethod: "口服" }],
});
const checkResult = ref<DataRow | null>(null);
let socket: WebSocket | null = null;
let recordStream: EventSource | null = null;

const selectedRegistration = computed(() => registrations.value.find((item) => Number(item.registrationId) === selectedRegistrationId.value) ?? null);
const serviceError = computed(() => message.value.includes("失败") || message.value.includes("不可用"));

function text(item: DataRow | null | undefined, key: string, fallback = "-") {
  const value = item?.[key];
  return value === undefined || value === null || value === "" ? fallback : String(value);
}

function tagClass(status: unknown) {
  const value = String(status || "");
  if (["CREATED", "CONFIRMED", "ARRIVED", "SAVED", "LOW", "APPROVED"].includes(value)) return "success";
  if (["HIGH", "FAILED", "REJECTED"].includes(value)) return "danger";
  if (["PENDING", "UNREVIEWED", "MEDIUM"].includes(value)) return "warning";
  return "";
}

async function run(label: string, task: () => Promise<void>) {
  busy.value = true;
  message.value = "";
  try {
    await task();
    message.value = `${label}成功`;
  } catch (error) {
    message.value = error instanceof Error ? error.message : `${label}失败`;
  } finally {
    busy.value = false;
  }
}

async function login() {
  await run("登录", async () => {
    const nextSession = await api.loginDoctor(loginForm.value.account, loginForm.value.password);
    auth.save("doctor-session", nextSession);
    connectSocket();
    await refresh();
  });
}

async function refresh() {
  if (session.value) {
    await workflow.refresh(auth.token());
  }
}

function connectSocket() {
  if (!session.value) return;
  socket?.close();
  socket = new WebSocket(notificationWebSocketUrl(session.value.userId, auth.token()));
  socket.onmessage = async () => {
    await refresh();
    message.value = "收到新的处方风险通知";
  };
}

function selectRegistration(item: DataRow) {
  selectedRegistrationId.value = Number(item.registrationId);
  selectedPatientId.value = Number(item.patientId);
  medicalForm.value.registrationId = Number(item.registrationId);
  prescription.value.medicalRecordId = 0;
  streamText.value = "";
  streamStatus.value = "DIALOGUE_READY";
}

function applyDraft(draft: DataRow) {
  medicalForm.value = {
    registrationId: selectedRegistrationId.value ?? 0,
    chiefComplaint: String(draft.chiefComplaint || ""),
    presentIllness: String(draft.presentIllness || ""),
    pastHistory: String(draft.pastHistory || ""),
    physicalExam: String(draft.physicalExam || medicalForm.value.physicalExam),
    diagnosis: String(draft.diagnosis || ""),
    treatmentAdvice: String(draft.treatmentAdvice || ""),
    aiGenerated: true,
  };
}

async function generateRecord() {
  if (!selectedRegistrationId.value) return;
  await run("AI 生成病历", async () => {
    streamText.value = "";
    streamStatus.value = "GENERATING";
    try {
      await generateRecordByStream(selectedRegistrationId.value);
    } catch {
      const draft = await api.generateMedicalRecord(auth.token(), {
        registrationId: selectedRegistrationId.value,
        departmentCode: "GENERAL",
        dialogueText: dialogueText.value,
      });
      applyDraft(draft);
      streamStatus.value = "DRAFT_READY";
      streamText.value = "流式生成不可用，已使用普通生成接口完成病历草稿。";
    }
  });
}

function generateRecordByStream(registrationId: number) {
  return new Promise<void>((resolve, reject) => {
    recordStream?.close();
    recordStream = new EventSource(medicalRecordStreamUrl(registrationId, auth.token()));
    recordStream.addEventListener("delta", (event) => {
      const data = parseEventData(event);
      streamText.value += `${String(data.text || event.data)}\n`;
    });
    recordStream.addEventListener("structured", (event) => {
      const data = parseEventData(event);
      applyDraft(data);
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
  await run("保存病历", async () => {
    const saved = await api.saveMedicalRecord(auth.token(), medicalForm.value);
    prescription.value.medicalRecordId = Number(saved.medicalRecordId);
    await refresh();
  });
}

async function checkPrescription() {
  if (!selectedPatientId.value) return;
  await run("AI 审核处方", async () => {
    checkResult.value = await api.checkPrescription(auth.token(), {
      patientId: selectedPatientId.value,
      doctorId: session.value?.userId,
      drugs: prescription.value.drugs,
    });
    prescription.value.riskLevel = String(checkResult.value.riskLevel || "UNREVIEWED");
    await refresh();
  });
}

async function createPrescription() {
  if (!selectedPatientId.value) return;
  await run("确认处方", async () => {
    await api.createPrescription(auth.token(), {
      patientId: selectedPatientId.value,
      medicalRecordId: prescription.value.medicalRecordId,
      riskLevel: prescription.value.riskLevel,
      drugs: prescription.value.drugs,
    });
    await refresh();
  });
}

function addDrug() {
  prescription.value.drugs.push({ drugName: "", dosage: "", frequency: "", usageMethod: "口服" });
}

function removeDrug(index: number) {
  prescription.value.drugs.splice(index, 1);
}

function logout() {
  socket?.close();
  socket = null;
  recordStream?.close();
  recordStream = null;
  auth.logout();
}

onMounted(async () => {
  auth.load("doctor-session");
  connectSocket();
  await refresh();
});
onBeforeUnmount(() => {
  socket?.close();
  recordStream?.close();
});
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>医</span>
        <div>
          <h1>医生诊疗工作台</h1>
          <p>接诊 · AI 病历 · 处方审核</p>
        </div>
      </div>
      <div v-if="session" class="user-card">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <div class="row-meta">
          <span class="tag success">在线接诊</span>
          <span class="tag warning">{{ notifications.length }} 条风险通知</span>
        </div>
      </div>
      <nav class="side-nav">
        <a class="active" href="#queue">待接诊 <b>{{ registrations.length }}</b></a>
        <a href="#patient">患者上下文</a>
        <a href="#record">病历草稿</a>
        <a href="#prescription">处方审核</a>
        <a href="#notifications">风险通知 <b>{{ notifications.length }}</b></a>
      </nav>
      <button v-if="session" type="button" @click="logout">退出登录</button>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Doctor Web</p>
          <h2>医生接诊与病历处方确认</h2>
          <p>把队列、患者信息、AI 草稿和处方风险放在同一工作区。</p>
        </div>
        <div class="toolbar">
          <button type="button" :disabled="busy" @click="refresh">同步队列</button>
          <button class="primary" type="button" :disabled="!selectedRegistrationId">开始接诊</button>
        </div>
      </header>

      <div v-if="message" class="notice" :class="{ error: serviceError, success: !serviceError }">{{ message }}</div>

      <section v-if="!session" class="panel login-panel">
        <h2>医生登录</h2>
        <div class="form-grid">
          <label>账号<input v-model="loginForm.account" /></label>
          <label>密码<input v-model="loginForm.password" type="password" /></label>
        </div>
        <button class="primary" type="button" :disabled="busy" @click="login">进入医生工作台</button>
      </section>

      <template v-else>
        <section class="metrics">
          <div class="metric"><span>待接诊</span><strong>{{ registrations.length }}</strong></div>
          <div class="metric"><span>AI 草稿状态</span><strong>{{ streamStatus }}</strong></div>
          <div class="metric"><span>处方风险</span><strong>{{ prescription.riskLevel }}</strong></div>
          <div class="metric"><span>今日已保存</span><strong>{{ records.length }}</strong></div>
        </section>

        <section class="main-grid">
          <div class="stack">
            <section id="queue" class="panel">
              <div class="panel-header">
                <div>
                  <h2>待接诊挂号列表</h2>
                  <p>点击队列患者后，右侧患者上下文和病历表单会同步更新。</p>
                </div>
                <div class="segmented">
                  <button class="active" type="button">全部</button>
                  <button type="button">待接诊</button>
                  <button type="button">复诊</button>
                </div>
              </div>
              <div v-if="registrations.length" class="table-scroll">
                <table>
                  <thead>
                    <tr><th>序号</th><th>患者</th><th>科室</th><th>预约时间</th><th>状态</th></tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="item in registrations"
                      :key="String(item.registrationId)"
                      :class="{ selected: Number(item.registrationId) === selectedRegistrationId }"
                      @click="selectRegistration(item)"
                    >
                      <td>#{{ text(item, "registrationId") }}</td>
                      <td>{{ text(item, "patientName", `患者 ${text(item, "patientId")}`) }}</td>
                      <td>{{ text(item, "departmentName") }}</td>
                      <td>{{ text(item, "appointmentTime") }}</td>
                      <td><span class="tag" :class="tagClass(item.status)">{{ text(item, "status") }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="empty-state">
                <strong>暂无待接诊患者</strong>
                <span>队列清空时显示当前医生、日期和刷新入口，避免误判为加载失败。</span>
                <button type="button" @click="refresh">刷新队列</button>
              </div>
            </section>

            <section id="record" class="panel">
              <div class="panel-header">
                <div>
                  <h2>AI 病历草稿生成区</h2>
                  <p>保留流式生成状态，医生必须确认后才能保存。</p>
                </div>
                <span class="tag info">{{ streamStatus }}</span>
              </div>
              <label>问诊对话摘要<textarea v-model="dialogueText" rows="4" /></label>
              <div class="toolbar">
                <button class="primary" type="button" :disabled="busy || !selectedRegistrationId" @click="generateRecord">流式生成病历</button>
                <button type="button">应用上次模板</button>
                <button type="button" disabled>生成中</button>
              </div>
              <div v-if="streamText || streamStatus !== 'IDLE'" class="clinical-note">
                <strong>生成状态：{{ streamStatus }}</strong>
                <p>{{ streamText || "等待 AI 输出结构化病历草稿。" }}</p>
              </div>
              <div class="form-grid">
                <label>主诉<input v-model="medicalForm.chiefComplaint" /></label>
                <label>诊断<input v-model="medicalForm.diagnosis" /></label>
                <label>现病史<textarea v-model="medicalForm.presentIllness" rows="3" /></label>
                <label>处理建议<textarea v-model="medicalForm.treatmentAdvice" rows="3" /></label>
              </div>
              <button class="primary" type="button" :disabled="busy || !medicalForm.registrationId" @click="saveRecord">医生确认保存</button>
            </section>
          </div>

          <aside class="stack">
            <section id="patient" class="panel">
              <div class="panel-header">
                <div>
                  <h2>当前患者上下文</h2>
                  <p>接诊过程中始终可见患者、挂号、分诊和过敏信息。</p>
                </div>
                <span class="tag success">当前接诊</span>
              </div>
              <div class="summary-strip">
                <div><span>患者</span><strong>{{ text(selectedRegistration, "patientName", "未选择") }}</strong></div>
                <div><span>挂号</span><strong>#{{ text(selectedRegistration, "registrationId") }}</strong></div>
                <div><span>科室</span><strong>{{ text(selectedRegistration, "departmentName") }}</strong></div>
              </div>
              <div class="timeline">
                <div><span>分诊</span><p>AI 推荐科室后，医生在此核对危险信号和主诉摘要。</p></div>
                <div><span>既往</span><p>无慢病记录；过敏史需在处方前再次确认。</p></div>
              </div>
            </section>

            <section id="prescription" class="panel">
              <div class="panel-header">
                <div>
                  <h2>处方录入和 AI 审核</h2>
                  <p>录入药品后先审核风险，再确认处方。</p>
                </div>
                <span class="tag" :class="tagClass(prescription.riskLevel)">{{ prescription.riskLevel }}</span>
              </div>
              <div v-for="(drug, index) in prescription.drugs" :key="index" class="drug-row">
                <label>药品<input v-model="drug.drugName" list="drug-options" /></label>
                <label>剂量<input v-model="drug.dosage" /></label>
                <label>频次<input v-model="drug.frequency" /></label>
                <label>用法<input v-model="drug.usageMethod" /></label>
                <button class="danger" type="button" @click="removeDrug(index)">移除</button>
              </div>
              <datalist id="drug-options">
                <option v-for="drug in drugs" :key="String(drug.id)" :value="String(drug.name)" />
              </datalist>
              <div class="toolbar">
                <button type="button" @click="addDrug">新增药品</button>
                <button class="primary" type="button" :disabled="busy || !selectedPatientId" @click="checkPrescription">AI 审核</button>
                <button class="primary" type="button" :disabled="busy || !prescription.medicalRecordId" @click="createPrescription">确认处方</button>
              </div>
              <div v-if="checkResult" class="notice warning">
                风险等级：{{ text(checkResult, "riskLevel", "未返回") }}。{{ text(checkResult, "suggestions", "请医生复核用药风险。") }}
              </div>
            </section>

            <section id="notifications" class="panel">
              <div class="panel-header">
                <div>
                  <h2>风险通知与已保存病历</h2>
                  <p>处方审核与 WebSocket 通知集中展示。</p>
                </div>
              </div>
              <div class="split">
                <div>
                  <h3>风险通知</h3>
                  <div v-if="notifications.length" class="mini-list">
                    <p v-for="item in notifications" :key="String(item.notificationId)">
                      {{ text(item, "title") }} · {{ text(item, "riskLevel") }} · {{ text(item, "readStatus") }}
                    </p>
                  </div>
                  <div v-else class="empty-state"><strong>暂无风险通知</strong><span>新的处方风险会实时推送。</span></div>
                </div>
                <div>
                  <h3>已保存病历</h3>
                  <div v-if="records.length" class="mini-list">
                    <p v-for="item in records" :key="String(item.medicalRecordId)">
                      #{{ text(item, "medicalRecordId") }} · {{ text(item, "patientName") }} · {{ text(item, "diagnosis") }}
                    </p>
                  </div>
                  <div v-else class="empty-state"><strong>暂无已保存病历</strong><span>保存病历后会出现在这里。</span></div>
                </div>
              </div>
            </section>
          </aside>
        </section>
      </template>
    </section>
  </main>
</template>
