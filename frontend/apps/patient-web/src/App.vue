<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";

type DataRow = Record<string, unknown>;

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { session } = storeToRefs(auth);
const { departments, triage, slots, registrations, records, prescriptions } = storeToRefs(workflow);

const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "13800000001", password: "123456" });
const registerForm = ref({ name: "测试患者", phone: "13800000001", password: "123456", gender: "FEMALE", age: 32 });
const chiefComplaint = ref("咽痛、低热、鼻塞两天，夜间咳嗽明显，无胸痛和呼吸困难，希望预约轻症门诊。");
const selectedSlotId = ref<number | null>(null);

const availableSlots = computed(() => {
  const departmentName = String(triage.value?.recommendedDepartment || "");
  return slots.value.filter((slot) => !departmentName || String(slot.departmentName || "").includes(departmentName));
});

const visibleSlots = computed(() => (availableSlots.value.length ? availableSlots.value : slots.value));
const selectedSlot = computed(() => visibleSlots.value.find((item) => Number(item.slotId) === selectedSlotId.value) ?? visibleSlots.value[0] ?? null);
const serviceUnavailable = computed(() => message.value.includes("失败") || message.value.includes("不可用"));

function text(item: DataRow | null | undefined, key: string, fallback = "-") {
  const value = item?.[key];
  return value === undefined || value === null || value === "" ? fallback : String(value);
}

function tagClass(status: unknown) {
  const value = String(status || "");
  if (["CREATED", "CONFIRMED", "ENABLED", "APPROVED", "COMPLETED"].includes(value)) return "success";
  if (["CANCELLED", "FAILED", "DISABLED", "HIGH"].includes(value)) return "danger";
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

async function register() {
  await run("注册", async () => {
    await api.registerPatient(registerForm.value);
    loginForm.value.account = registerForm.value.phone;
    loginForm.value.password = registerForm.value.password;
  });
}

async function login() {
  await run("登录", async () => {
    const nextSession = await api.loginPatient(loginForm.value.account, loginForm.value.password);
    auth.save("patient-session", nextSession);
    await refresh();
  });
}

async function consult() {
  await run("智能分诊", async () => {
    triage.value = await api.triage(auth.token(), chiefComplaint.value);
    await workflow.refreshAuthenticated(auth.token());
    selectedSlotId.value = Number(visibleSlots.value[0]?.slotId || 0) || null;
    if (!selectedSlotId.value) {
      message.value = "暂无可预约号源，请稍后重试或联系导诊台。";
    }
  });
}

async function createRegistration() {
  const slot = selectedSlot.value;
  if (!slot) {
    message.value = "请选择可预约号源。";
    return;
  }
  await run("预约挂号", async () => {
    await api.createRegistration(auth.token(), {
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
    await api.cancelRegistration(auth.token(), Number(registrationId));
    await refresh();
  });
}

async function refresh() {
  await workflow.refreshPublicData();
  if (session.value) {
    await workflow.refreshAuthenticated(auth.token());
    if (!selectedSlotId.value) {
      selectedSlotId.value = Number(visibleSlots.value[0]?.slotId || 0) || null;
    }
  }
}

function logout() {
  auth.logout();
}

onMounted(async () => {
  auth.load("patient-session");
  await refresh();
});
</script>

<template>
  <main class="patient-page">
    <header class="mayo-header">
      <div class="mayo-utility">
        <a href="#care">患者服务</a>
        <a href="#symptoms">症状索引</a>
        <a href="#records">病历处方</a>
        <span v-if="session">{{ session.name }} · 已登录</span>
      </div>
      <div class="mayo-mainbar">
        <a class="mayo-brand" href="#top" aria-label="Smart Cloud Brain 患者端">
          <span class="mayo-symbol">SCB</span>
          <span>Smart Cloud Brain</span>
        </a>
        <nav class="mayo-nav" aria-label="患者服务导航">
          <a href="#triage">Request appointment</a>
          <a href="#slots">Find a doctor</a>
          <a href="#symptoms">Diseases & conditions</a>
          <a href="#records">Patient portal</a>
        </nav>
        <div class="mayo-actions">
          <button type="button">Search</button>
          <button type="button">Menu</button>
          <button class="primary" type="button" :disabled="busy" @click="session ? consult() : login()">开始预约</button>
        </div>
      </div>
    </header>

    <section id="top" class="mayo-hero">
      <div class="mayo-hero-content">
        <p class="eyebrow">PATIENT WEB</p>
        <h1>让就诊从正确分诊开始</h1>
        <p class="mayo-lead">描述症状，获取 AI 推荐科室，查看可预约号源，并在同一页面确认挂号与诊后记录。</p>
        <div class="mayo-hero-actions">
          <a class="button primary" href="#triage">Request appointment</a>
          <a class="button" href="#symptoms">查看症状索引</a>
        </div>
      </div>
      <div class="image-placeholder" aria-label="图片占位"></div>
    </section>

    <section class="account-strip">
      <template v-if="session">
        <span class="tag success">已登录</span>
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <button class="ghost" type="button" @click="logout">退出登录</button>
      </template>
      <template v-else>
        <span class="tag warning">未登录</span>
        <strong>患者登录后可提交分诊和预约</strong>
        <span>可使用演示账号 {{ loginForm.account }}</span>
      </template>
    </section>

    <div v-if="message" class="notice" :class="{ error: serviceUnavailable, success: !serviceUnavailable }">{{ message }}</div>

    <section v-if="!session" class="login-section panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">SIGN IN</p>
          <h2>患者登录与注册</h2>
        </div>
      </div>
      <div class="form-grid">
        <label>姓名<input v-model="registerForm.name" /></label>
        <label>手机号<input v-model="registerForm.phone" /></label>
        <label>密码<input v-model="registerForm.password" type="password" /></label>
        <label>年龄<input v-model.number="registerForm.age" type="number" /></label>
      </div>
      <div class="toolbar">
        <button type="button" :disabled="busy" @click="register">注册患者</button>
      </div>
      <div class="form-grid compact">
        <label>账号<input v-model="loginForm.account" /></label>
        <label>密码<input v-model="loginForm.password" type="password" /></label>
      </div>
      <button class="primary" type="button" :disabled="busy" @click="login">进入患者端</button>
    </section>

    <template v-else>
      <section class="task-strip" aria-label="快捷入口">
        <a href="#triage"><strong>Request appointment</strong><span>从 AI 分诊开始预约</span></a>
        <a href="#slots"><strong>Find a doctor</strong><span>按推荐科室选择医生号源</span></a>
        <a href="#registrations"><strong>Appointments</strong><span>查看我的挂号与候诊提醒</span></a>
        <a href="#records"><strong>Patient portal</strong><span>查看病历、处方和审核结果</span></a>
      </section>

      <section id="symptoms" class="alpha-section">
        <div>
          <h2>Find symptoms by first letter</h2>
          <p>用字母索引承载患者端症状入口，帮助患者快速定位常见主诉。</p>
        </div>
        <div class="alpha-list">
          <a v-for="letter in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ#'.split('')" :key="letter">{{ letter }}</a>
        </div>
      </section>

      <section class="feature-band">
        <div class="image-placeholder small" aria-label="图片占位"></div>
        <div>
          <h2>Healing starts here</h2>
          <div class="feature-items">
            <div>
              <strong>AI 分诊推荐结果</strong>
              <p>{{ text(triage, "recommendedDepartment", "提交分诊后展示推荐科室") }}</p>
              <p>{{ text(triage, "reason", "AI 会根据症状、持续时间和危险信号给出推荐理由。") }}</p>
            </div>
            <div>
              <strong>正确的下一步</strong>
              <p>系统优先推荐可预约号源，保留医生确认与人工复核提示，避免患者误把 AI 建议当作最终诊断。</p>
            </div>
          </div>
          <a class="button primary" href="#triage">开始智能问诊</a>
        </div>
      </section>

      <section class="metrics" aria-label="患者概览">
        <div class="metric"><span>今日可约号源</span><strong>{{ visibleSlots.length }}</strong></div>
        <div class="metric"><span>推荐科室</span><strong>{{ text(triage, "recommendedDepartment", departments[0]?.name ? String(departments[0].name) : "待分诊") }}</strong></div>
        <div class="metric"><span>进行中挂号</span><strong>{{ registrations.length }}</strong></div>
        <div class="metric"><span>诊后记录</span><strong>{{ records.length + prescriptions.length }}</strong></div>
      </section>

      <section id="care" class="content-grid">
        <div class="stack">
          <section id="triage" class="panel">
            <div class="panel-header">
              <div>
                <h2>AI 智能问诊与分诊输入区</h2>
                <p>患者输入主诉后展示推荐科室、理由和人工确认提示。</p>
              </div>
              <span class="tag info">建议需医生确认</span>
            </div>
            <div class="panel-body">
              <div class="form-grid">
                <label>
                  主诉与症状
                  <textarea v-model="chiefComplaint" rows="5" />
                </label>
                <div class="clinical-note">
                  <strong>分诊推荐结果</strong>
                  <p>推荐科室：{{ text(triage, "recommendedDepartment", "待提交分诊") }}</p>
                  <p>理由：{{ text(triage, "reason", "提交后展示 AI 推荐理由。") }}</p>
                  <div class="row-meta">
                    <span class="tag success">轻症优先</span>
                    <span class="tag warning">需人工确认</span>
                    <span class="tag">记录 #{{ text(triage, "triageRecordId", "-") }}</span>
                  </div>
                </div>
              </div>
              <div class="toolbar">
                <button class="primary" type="button" :disabled="busy" @click="consult">提交分诊</button>
                <button type="button">补充既往史</button>
                <button type="button" disabled>正在分析</button>
              </div>
            </div>
          </section>

          <section id="slots" class="panel">
            <div class="panel-header">
              <div>
                <h2>可预约号源列表</h2>
                <p>按推荐科室、时间和剩余号源组织，降低患者选择成本。</p>
              </div>
              <div class="segmented">
                <button class="active" type="button">推荐</button>
                <button type="button">今天</button>
                <button type="button">近三天</button>
              </div>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>时间</th>
                    <th>科室</th>
                    <th>医生</th>
                    <th>余号</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="slot in visibleSlots" :key="String(slot.slotId)" :class="{ selected: Number(slot.slotId) === selectedSlotId }">
                    <td>{{ text(slot, "startTime") }}</td>
                    <td>{{ text(slot, "departmentName") }}</td>
                    <td>{{ text(slot, "doctorName") }}</td>
                    <td><span class="tag" :class="Number(slot.remainingCapacity || 0) > 3 ? 'success' : 'warning'">{{ text(slot, "remainingCapacity", "0") }}</span></td>
                    <td><button type="button" @click="selectedSlotId = Number(slot.slotId)">选择</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="!visibleSlots.length" class="empty-state">
              <strong>暂无号源</strong>
              <span>推荐科室暂无可预约号源，请刷新或联系导诊台。</span>
              <button type="button" disabled>加入候补</button>
            </div>
          </section>

          <section id="registrations" class="panel">
            <div class="panel-header">
              <div>
                <h2>我的挂号</h2>
                <p>集中展示待就诊、已取消和已完成的挂号记录。</p>
              </div>
              <button type="button" @click="refresh">刷新</button>
            </div>
            <div v-if="registrations.length" class="list">
              <div v-for="item in registrations" :key="String(item.registrationId)" class="list-row">
                <div>
                  <strong>{{ text(item, "departmentName") }} · {{ text(item, "doctorName") }}</strong>
                  <p>{{ text(item, "appointmentTime") }} · 挂号 #{{ text(item, "registrationId") }}</p>
                  <span class="tag" :class="tagClass(item.status)">{{ text(item, "status") }}</span>
                </div>
                <button class="danger" type="button" :disabled="busy || item.status === 'CANCELLED'" @click="cancelRegistration(item.registrationId)">取消</button>
              </div>
            </div>
            <div v-else class="empty-state">
              <strong>暂无挂号</strong>
              <span>完成分诊并选择号源后，挂号记录会显示在这里。</span>
            </div>
          </section>
        </div>

        <aside class="stack">
          <section id="confirm" class="panel">
            <div class="panel-header">
              <div>
                <h2>预约确认区</h2>
                <p>确认患者、科室、医生和分诊记录。</p>
              </div>
              <span class="tag accent">待确认</span>
            </div>
            <div class="summary-strip">
              <div><span>科室</span><strong>{{ text(selectedSlot, "departmentName", "请选择号源") }}</strong></div>
              <div><span>医生</span><strong>{{ text(selectedSlot, "doctorName", "-") }}</strong></div>
              <div><span>时间</span><strong>{{ text(selectedSlot, "startTime", "-") }}</strong></div>
            </div>
            <div class="form-grid single">
              <label>就诊人<input :value="session.name" disabled /></label>
              <label>联系电话<input :value="loginForm.account" disabled /></label>
            </div>
            <div class="notice warning">分诊结果仅作为挂号推荐，最终诊断以医生接诊结论为准。</div>
            <div class="toolbar">
              <button class="primary" type="button" :disabled="busy || !selectedSlot" @click="createRegistration">确认预约</button>
              <a class="button" href="#slots">更换号源</a>
            </div>
          </section>

          <section id="records" class="panel">
            <div class="panel-header">
              <div>
                <h2>病历与处方记录</h2>
                <p>诊后信息按时间线组织，突出诊断、建议和处方审核状态。</p>
              </div>
            </div>
            <div v-if="records.length || prescriptions.length" class="timeline">
              <div v-for="item in records" :key="`record-${String(item.medicalRecordId)}`" class="timeline-item">
                <span>病历</span>
                <div>
                  <strong>{{ text(item, "diagnosis", "诊断待补充") }}</strong>
                  <p>{{ text(item, "treatmentAdvice", "暂无处理建议") }}</p>
                </div>
              </div>
              <div v-for="item in prescriptions" :key="`prescription-${String(item.prescriptionId)}`" class="timeline-item">
                <span>处方</span>
                <div>
                  <strong>#{{ text(item, "prescriptionId") }} · 风险 {{ text(item, "riskLevel", "未审核") }}</strong>
                  <p>{{ text(item, "status", "处方状态待同步") }}</p>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <strong>暂无病历</strong>
              <span>医生确认保存后，病历与处方会显示在这里。</span>
              <button type="button" @click="refresh">刷新记录</button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-header">
              <div>
                <h2>错误与服务状态</h2>
                <p>请求失败、服务不可用和禁用按钮状态统一呈现。</p>
              </div>
            </div>
            <div class="notice error">请求失败：号源服务暂不可用，请稍后重试或联系导诊台。</div>
            <button class="primary" type="button" :disabled="busy" @click="refresh">重试</button>
          </section>
        </aside>
      </section>
    </template>
  </main>
</template>
