<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api, type Session } from "@smart-cloud-brain/shared-api";

const session = ref<Session | null>(JSON.parse(localStorage.getItem("patient-session") || "null"));
const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "13800000001", password: "123456" });
const registerForm = ref({ name: "测试患者", phone: "13800000001", password: "123456", gender: "FEMALE", age: 21 });
const chiefComplaint = ref("咽痛、低热、鼻塞两天，想预约轻症门诊。");
const departments = ref<Array<Record<string, unknown>>>([]);
const doctors = ref<Array<Record<string, unknown>>>([]);
const triage = ref<Record<string, unknown> | null>(null);
const appointment = ref({ doctorId: 2, departmentId: 2, appointmentTime: new Date(Date.now() + 86400000).toISOString().slice(0, 16) });
const registrations = ref<Array<Record<string, unknown>>>([]);
const records = ref<Array<Record<string, unknown>>>([]);
const prescriptions = ref<Array<Record<string, unknown>>>([]);

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
    const recommended = String(triage.value.departmentCode || "");
    const department = departments.value.find((item) => item.code === recommended);
    if (department?.id) appointment.value.departmentId = Number(department.id);
    if (Array.isArray(triage.value.recommendedDoctorIds) && triage.value.recommendedDoctorIds[0]) {
      appointment.value.doctorId = Number(triage.value.recommendedDoctorIds[0]);
    }
  });
}

async function createRegistration() {
  await run("预约挂号", async () => {
    await api.createRegistration(token(), {
      ...appointment.value,
      appointmentTime: `${appointment.value.appointmentTime}:00`,
      triageRecordId: triage.value?.triageRecordId ?? null,
    });
    await refresh();
  });
}

async function refresh() {
  departments.value = await api.departments();
  doctors.value = await api.doctors();
  if (session.value) {
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
      <p>完成轻症预问诊、AI 分诊、预约挂号、诊疗记录查看。</p>
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
          <h2>预约挂号</h2>
          <div class="grid three">
            <label>科室
              <select v-model.number="appointment.departmentId">
                <option v-for="item in departments" :key="String(item.id)" :value="Number(item.id)">{{ item.name }}</option>
              </select>
            </label>
            <label>医生
              <select v-model.number="appointment.doctorId">
                <option v-for="item in doctors" :key="String(item.id)" :value="Number(item.id)">{{ item.name }} · {{ item.departmentName }}</option>
              </select>
            </label>
            <label>预约时间<input v-model="appointment.appointmentTime" type="datetime-local" /></label>
          </div>
          <button class="primary" :disabled="busy" @click="createRegistration">确认预约</button>
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
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <h2>病历与处方记录</h2>
          <div class="split">
            <div>
              <h3>病历</h3>
              <p v-for="item in records" :key="String(item.medicalRecordId)">{{ item.diagnosis }} · {{ item.treatmentAdvice }}</p>
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
