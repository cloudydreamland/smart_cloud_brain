<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { api, notificationWebSocketUrl, type Session } from "@smart-cloud-brain/shared-api";

const session = ref<Session | null>(JSON.parse(localStorage.getItem("doctor-session") || "null"));
const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "doctor1", password: "123456" });
const registrations = ref<Array<Record<string, unknown>>>([]);
const records = ref<Array<Record<string, unknown>>>([]);
const drugs = ref<Array<Record<string, unknown>>>([]);
const notifications = ref<Array<Record<string, unknown>>>([]);
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

const token = () => session.value?.token ?? "";

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
    session.value = await api.loginDoctor(loginForm.value.account, loginForm.value.password);
    localStorage.setItem("doctor-session", JSON.stringify(session.value));
    connectSocket();
    await refresh();
  });
}

async function refresh() {
  if (session.value) {
    drugs.value = await api.searchDrugs(token(), "");
    registrations.value = await api.registrations(token());
    records.value = await api.medicalRecords(token());
    notifications.value = await api.notifications(token());
  }
}

function connectSocket() {
  if (!session.value) return;
  socket?.close();
  socket = new WebSocket(notificationWebSocketUrl(session.value.userId));
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
}

async function generateRecord() {
  if (!selectedRegistrationId.value) return;
  await run("AI 生成病历", async () => {
    const draft = await api.generateMedicalRecord(token(), {
      registrationId: selectedRegistrationId.value,
      departmentCode: "GENERAL",
      dialogueText: dialogueText.value,
    });
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
  });
}

async function saveRecord() {
  await run("保存病历", async () => {
    const saved = await api.saveMedicalRecord(token(), medicalForm.value);
    prescription.value.medicalRecordId = Number(saved.medicalRecordId);
    await refresh();
  });
}

async function checkPrescription() {
  if (!selectedPatientId.value) return;
  await run("AI 审核处方", async () => {
    checkResult.value = await api.checkPrescription(token(), {
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
    await api.createPrescription(token(), {
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
  session.value = null;
  localStorage.removeItem("doctor-session");
}

onMounted(async () => {
  connectSocket();
  await refresh();
});
onBeforeUnmount(() => socket?.close());
</script>

<template>
  <main class="app-shell">
    <aside class="rail">
      <p class="eyebrow">Doctor Web</p>
      <h1>医生端</h1>
      <p>接诊患者，确认 AI 病历草稿，完成处方审核，并接收实时风险通知。</p>
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
          <button :disabled="busy || !selectedRegistrationId" @click="generateRecord">生成病历</button>
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
