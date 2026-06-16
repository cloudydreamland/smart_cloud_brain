<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  medicalRecordStreamUrl,
  notificationWebSocketUrl,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { session } = storeToRefs(auth);
const { registrations, records, drugs, notifications, streamText, streamStatus } = storeToRefs(workflow);

const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "doctor1", password: "123456" });
const selectedRegistrationId = ref<number | null>(null);
const selectedPatientId = ref<number | null>(null);
const dialogueText = ref("患者自述咽痛、鼻塞、低热两天，无明显胸痛和呼吸困难。");
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
  drugs: [{ drugName: "对乙酰氨基酚", dosage: "0.5g", frequency: "必要时", usageMethod: "口服" }],
});
const checkResult = ref<Record<string, unknown> | null>(null);
let socket: WebSocket | null = null;
let recordStream: EventSource | null = null;

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

function selectRegistration(item: Record<string, unknown>) {
  selectedRegistrationId.value = Number(item.registrationId);
  selectedPatientId.value = Number(item.patientId);
  medicalForm.value.registrationId = Number(item.registrationId);
  prescription.value.medicalRecordId = 0;
  streamText.value = "";
  streamStatus.value = "DIALOGUE_READY";
}

function applyDraft(draft: Record<string, unknown>) {
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
    return JSON.parse(event.data) as Record<string, unknown>;
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
    <aside class="rail">
      <p class="eyebrow">Doctor Web</p>
      <h1>医生端</h1>
      <p>接诊患者，流式生成并确认 AI 病历草稿，完成处方审核，并接收实时风险通知。</p>
      <div class="session" v-if="session">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <button @click="logout">退出</button>
      </div>
    </aside>

    <section class="workspace">
      <div class="notice" v-if="message">{{ message }}</div>

      <section class="panel" v-if="!session">
        <h2>医生登录</h2>
        <div class="grid two">
          <label>账号<input v-model="loginForm.account" /></label>
          <label>密码<input v-model="loginForm.password" type="password" /></label>
        </div>
        <button class="primary" :disabled="busy" @click="login">进入医生工作台</button>
      </section>

      <template v-else>
        <section class="panel">
          <h2>待接诊挂号</h2>
          <table>
            <tbody>
              <tr v-for="item in registrations" :key="String(item.registrationId)" @click="selectRegistration(item)">
                <td>#{{ item.registrationId }}</td>
                <td>{{ item.patientName }}</td>
                <td>{{ item.departmentName }}</td>
                <td>{{ item.appointmentTime }}</td>
                <td>{{ item.status }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <h2>AI 病历草稿</h2>
          <textarea v-model="dialogueText" rows="3" />
          <button :disabled="busy || !selectedRegistrationId" @click="generateRecord">流式生成病历</button>
          <div class="result" v-if="streamText || streamStatus !== 'IDLE'">
            <strong>生成状态：{{ streamStatus }}</strong>
            <span style="white-space: pre-line">{{ streamText }}</span>
          </div>
          <div class="grid two">
            <label>主诉<input v-model="medicalForm.chiefComplaint" /></label>
            <label>诊断<input v-model="medicalForm.diagnosis" /></label>
            <label>现病史<textarea v-model="medicalForm.presentIllness" rows="3" /></label>
            <label>处理建议<textarea v-model="medicalForm.treatmentAdvice" rows="3" /></label>
          </div>
          <button class="primary" :disabled="busy || !medicalForm.registrationId" @click="saveRecord">医生确认保存</button>
        </section>

        <section class="panel">
          <h2>处方审核</h2>
          <div class="grid four" v-for="(drug, index) in prescription.drugs" :key="index">
            <label>药品<input v-model="drug.drugName" list="drug-options" /></label>
            <label>剂量<input v-model="drug.dosage" /></label>
            <label>频次<input v-model="drug.frequency" /></label>
            <label>用法<input v-model="drug.usageMethod" /></label>
          </div>
          <datalist id="drug-options">
            <option v-for="drug in drugs" :key="String(drug.id)" :value="String(drug.name)" />
          </datalist>
          <div class="actions">
            <button :disabled="busy || !selectedPatientId" @click="checkPrescription">AI 审核</button>
            <button class="primary" :disabled="busy || !prescription.medicalRecordId" @click="createPrescription">确认处方</button>
          </div>
          <div class="result" v-if="checkResult">
            <strong>风险等级：{{ checkResult.riskLevel }}</strong>
            <span>{{ checkResult.suggestions }}</span>
          </div>
        </section>

        <section class="panel">
          <h2>通知与历史病历</h2>
          <div class="split">
            <div>
              <h3>风险通知</h3>
              <p v-for="item in notifications" :key="String(item.notificationId)">{{ item.title }} · {{ item.riskLevel }} · {{ item.readStatus }}</p>
            </div>
            <div>
              <h3>已保存病历</h3>
              <p v-for="item in records" :key="String(item.medicalRecordId)">#{{ item.medicalRecordId }} · {{ item.patientName }} · {{ item.diagnosis }}</p>
            </div>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
