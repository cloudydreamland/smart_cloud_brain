<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, type Session } from "@smart-cloud-brain/shared-api";

const session = ref<Session | null>(JSON.parse(localStorage.getItem("patient-session") || "null"));
const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "13800000001", password: "123456" });
const registerForm = ref({ name: "测试患者", phone: "13800000001", password: "123456", gender: "FEMALE", age: 21 });
const chiefComplaint = ref("咽痛、低热、鼻塞两天，想预约轻症门诊。");
const departments = ref<Array<Record<string, unknown>>>([]);
const triage = ref<Record<string, unknown> | null>(null);
const slots = ref<Array<Record<string, unknown>>>([]);
const selectedSlotId = ref<number | null>(null);
const registrations = ref<Array<Record<string, unknown>>>([]);
const records = ref<Array<Record<string, unknown>>>([]);
const prescriptions = ref<Array<Record<string, unknown>>>([]);

const token = () => session.value?.token ?? "";
const availableSlots = computed(() => {
  const departmentName = String(triage.value?.recommendedDepartment || "");
  return slots.value.filter((slot) => !departmentName || String(slot.departmentName || "").includes(departmentName) || String(slot.departmentName || "") === departmentName);
});

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

async function register() {
  await run("注册", async () => {
    await api.registerPatient(registerForm.value);
    loginForm.value.account = registerForm.value.phone;
    loginForm.value.password = registerForm.value.password;
  });
}

async function login() {
  await run("登录", async () => {
    session.value = await api.loginPatient(loginForm.value.account, loginForm.value.password);
    localStorage.setItem("patient-session", JSON.stringify(session.value));
    await refresh();
  });
}

async function consult() {
  await run("智能分诊", async () => {
    triage.value = await api.triage(token(), chiefComplaint.value);
    await refreshSlots();
    selectedSlotId.value = Number(availableSlots.value[0]?.slotId || slots.value[0]?.slotId || 0) || null;
  });
}

async function refreshSlots() {
  if (session.value) {
    slots.value = await api.registrationSlots(token());
  }
}

async function createRegistration() {
  const slot = slots.value.find((item) => Number(item.slotId) === selectedSlotId.value);
  if (!slot) return;
  await run("预约挂号", async () => {
    await api.createRegistration(token(), {
      doctorId: Number(slot.doctorId),
      departmentId: Number(slot.departmentId),
      appointmentTime: String(slot.startTime),
      triageRecordId: triage.value?.triageRecordId ?? null,
      slotId: Number(slot.slotId),
    });
    await refresh();
  });
}

async function cancelRegistration(registrationId: unknown) {
  await run("取消挂号", async () => {
    await api.cancelRegistration(token(), Number(registrationId));
    await refresh();
  });
}

async function refresh() {
  departments.value = await api.departments();
  if (session.value) {
    await refreshSlots();
    registrations.value = await api.registrations(token());
    records.value = await api.medicalRecords(token());
    prescriptions.value = await api.prescriptions(token());
  }
}

function logout() {
  session.value = null;
  localStorage.removeItem("patient-session");
}

onMounted(refresh);
</script>

<template>
  <main class="app-shell">
    <aside class="rail">
      <p class="eyebrow">Patient Web</p>
      <h1>患者端</h1>
      <p>完成注册登录、AI 分诊、选择金仓号源、预约挂号、取消挂号、查看病历和处方。</p>
      <div class="session" v-if="session">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <button @click="logout">退出</button>
      </div>
    </aside>

    <section class="workspace">
      <div class="notice" v-if="message">{{ message }}</div>

      <section class="panel" v-if="!session">
        <h2>注册与登录</h2>
        <div class="grid two">
          <label>姓名<input v-model="registerForm.name" /></label>
          <label>手机号<input v-model="registerForm.phone" /></label>
          <label>密码<input v-model="registerForm.password" type="password" /></label>
          <label>年龄<input v-model.number="registerForm.age" type="number" /></label>
        </div>
        <button :disabled="busy" @click="register">注册患者</button>
        <div class="grid two compact">
          <label>账号<input v-model="loginForm.account" /></label>
          <label>密码<input v-model="loginForm.password" type="password" /></label>
        </div>
        <button class="primary" :disabled="busy" @click="login">登录患者端</button>
      </section>

      <template v-else>
        <section class="panel">
          <h2>AI 智能问诊与分诊</h2>
          <textarea v-model="chiefComplaint" rows="4" />
          <button class="primary" :disabled="busy" @click="consult">提交分诊</button>
          <div class="result" v-if="triage">
            <strong>{{ triage.recommendedDepartment }}</strong>
            <span>{{ triage.reason }}</span>
            <small>记录编号：{{ triage.triageRecordId }} · {{ triage.status }}</small>
          </div>
        </section>

        <section class="panel">
          <h2>选择可预约号源</h2>
          <div class="grid two">
            <label>推荐科室
              <select disabled>
                <option>{{ triage?.recommendedDepartment || departments[0]?.name || "请先提交分诊" }}</option>
              </select>
            </label>
            <label>号源
              <select v-model.number="selectedSlotId">
                <option v-for="item in (availableSlots.length ? availableSlots : slots)" :key="String(item.slotId)" :value="Number(item.slotId)">
                  {{ item.departmentName }} · {{ item.doctorName }} · {{ item.startTime }} · 余号 {{ item.remainingCapacity }}
                </option>
              </select>
            </label>
          </div>
          <button class="primary" :disabled="busy || !selectedSlotId" @click="createRegistration">确认预约</button>
        </section>

        <section class="panel">
          <h2>我的挂号</h2>
          <table>
            <tbody>
              <tr v-for="item in registrations" :key="String(item.registrationId)">
                <td>#{{ item.registrationId }}</td>
                <td>{{ item.departmentName }}</td>
                <td>{{ item.doctorName }}</td>
                <td>{{ item.appointmentTime }}</td>
                <td>{{ item.status }}</td>
                <td><button :disabled="busy || item.status === 'CANCELLED'" @click="cancelRegistration(item.registrationId)">取消</button></td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <h2>病历与处方记录</h2>
          <div class="split">
            <div>
              <h3>病历</h3>
              <p v-for="item in records" :key="String(item.medicalRecordId)">#{{ item.medicalRecordId }} · {{ item.diagnosis }} · {{ item.treatmentAdvice }}</p>
            </div>
            <div>
              <h3>处方</h3>
              <p v-for="item in prescriptions" :key="String(item.prescriptionId)">#{{ item.prescriptionId }} · 风险 {{ item.riskLevel }} · {{ item.status }}</p>
            </div>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
