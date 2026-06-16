<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, useAdminWorkflowStore, useAuthStore } from "@smart-cloud-brain/shared-api";

type DataRow = Record<string, unknown>;

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { session } = storeToRefs(auth);
const {
  departments,
  doctors,
  drugs,
  prompts,
  knowledge,
  dicts,
  suggestions,
  schedules,
  triageDesk,
  selectedScheduleSuggestion,
  selectedTriage,
} = storeToRefs(workflow);

const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "admin", password: "123456" });
const departmentForm = ref({ code: "GENERAL", name: "全科门诊", description: "常见轻症初诊与复诊" });
const doctorForm = ref({ name: "李医生", phone: "13900000002", password: "", departmentId: 2, title: "主治医师", specialty: "发热、咽痛、腹泻、皮肤过敏", status: "ENABLED" });
const drugForm = ref({ name: "对乙酰氨基酚片", specification: "0.5g", contraindication: "严重肝功能不全慎用", interactionRule: "避免与其他含同成分复方药重复使用", status: "ENABLED" });
const promptForm = ref({ taskType: "MEDICAL_RECORD", departmentCode: "GENERAL", templateName: "GENERAL_MEDICAL_RECORD_v1", templateContent: "根据轻症问诊文本生成结构化病历 JSON，必须提示医生确认。", outputSchema: "{\"type\":\"object\"}", version: "v1", enabled: true });
const knowledgeForm = ref({ title: "普通感冒", symptoms: "鼻塞、流涕、咽痛、低热、轻微咳嗽", riskSignals: "持续高热、呼吸困难、胸痛、意识异常", advice: "注意休息和补液，症状加重或超过三天建议线下就诊。", departmentCode: "GENERAL", status: "ENABLED" });
const dictForm = ref({ dictType: "REGISTRATION_STATUS", dictKey: "CREATED", dictValue: "已预约", sort: 1, status: "ENABLED" });
const scheduleForm = ref({ startDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10), days: 3 });
const assignForm = ref({ triageRecordId: 0, doctorId: 2 });
const search = ref({ q: "感冒", departmentCode: "" });
const searchResults = ref({
  knowledge: [] as Array<DataRow>,
  drugs: [] as Array<DataRow>,
  prompts: [] as Array<DataRow>,
});

const serviceError = computed(() => message.value.includes("失败") || message.value.includes("超时"));
const searchCount = computed(() => searchResults.value.knowledge.length + searchResults.value.drugs.length + searchResults.value.prompts.length);

function text(item: DataRow | null | undefined, key: string, fallback = "-") {
  const value = item?.[key];
  return value === undefined || value === null || value === "" ? fallback : String(value);
}

function tagClass(status: unknown) {
  const value = String(status || "");
  if (["ENABLED", "PUBLISHED", "ASSIGNED", "CREATED"].includes(value)) return "success";
  if (["DISABLED", "CLOSED", "FAILED"].includes(value)) return "danger";
  if (["PENDING", "DRAFT", "UNPUBLISHED"].includes(value)) return "warning";
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
    const nextSession = await api.loginAdmin(loginForm.value.account, loginForm.value.password);
    auth.save("admin-session", nextSession);
    await refresh();
  });
}

async function refresh() {
  if (!session.value) return;
  await workflow.refresh(auth.token());
}

async function saveDepartment() {
  await run("保存科室", async () => {
    await api.saveDepartment(auth.token(), departmentForm.value);
    await refresh();
  });
}

async function saveDoctor() {
  await run("保存医生", async () => {
    await api.saveDoctor(auth.token(), doctorForm.value);
    doctorForm.value.password = "";
    await refresh();
  });
}

async function saveDrug() {
  await run("保存药品", async () => {
    await api.saveDrug(auth.token(), drugForm.value);
    await refresh();
  });
}

async function savePrompt() {
  await run("保存 Prompt", async () => {
    await api.savePrompt(auth.token(), promptForm.value);
    await refresh();
  });
}

async function saveKnowledge() {
  await run("保存知识库", async () => {
    await api.saveKnowledgeEntry(auth.token(), knowledgeForm.value);
    await refresh();
  });
}

async function saveDict() {
  await run("保存字典", async () => {
    await api.saveDict(auth.token(), dictForm.value);
    await refresh();
  });
}

async function generateSchedule() {
  await run("生成 AI 排班建议", async () => {
    suggestions.value = await api.generateSchedule(auth.token(), scheduleForm.value);
    await refresh();
  });
}

async function publishSchedule() {
  await run("发布排班", async () => {
    const suggestionIds = suggestions.value.map((item) => Number(item.id)).filter(Boolean);
    schedules.value = await api.publishSchedule(auth.token(), { suggestionIds });
    suggestions.value = [];
    await refresh();
  });
}

async function showScheduleSuggestion(id: unknown) {
  selectedScheduleSuggestion.value = await api.scheduleSuggestionDetail(auth.token(), Number(id));
}

async function assignTriage() {
  await run("分配分诊", async () => {
    await api.assignTriage(auth.token(), assignForm.value);
    await refresh();
  });
}

async function showTriage(id: unknown) {
  selectedTriage.value = await api.triageDetail(auth.token(), Number(id));
}

async function closeTriage(id: unknown) {
  await run("关闭分诊", async () => {
    await api.closeTriage(auth.token(), Number(id));
    await refresh();
  });
}

async function doSearch() {
  await run("搜索", async () => {
    const [knowledgeList, drugList, promptList] = await Promise.all([
      api.searchKnowledge(auth.token(), search.value.q, search.value.departmentCode),
      api.searchDrugs(auth.token(), search.value.q),
      api.searchPrompts(auth.token(), search.value.q),
    ]);
    searchResults.value = { knowledge: knowledgeList, drugs: drugList, prompts: promptList };
  });
}

function useTriage(item: DataRow) {
  assignForm.value.triageRecordId = Number(item.triageRecordId);
  assignForm.value.doctorId = Number(item.assignedDoctorId || doctors.value[0]?.id || 0);
}

function logout() {
  auth.logout();
}

onMounted(async () => {
  auth.load("admin-session");
  await refresh();
});
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>管</span>
        <div>
          <h1>运营管理工作台</h1>
          <p>基础数据 · 号源 · 知识库</p>
        </div>
      </div>
      <div v-if="session" class="user-card">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <div class="row-meta">
          <span class="tag success">已登录</span>
          <span class="tag">{{ triageDesk.length }} 条分诊</span>
        </div>
      </div>
      <nav class="side-nav">
        <a class="active" href="#catalog">科室医生 <b>{{ doctors.length }}</b></a>
        <a href="#schedule">AI 排班 <b>{{ suggestions.length || "待生成" }}</b></a>
        <a href="#triage">分诊工作台 <b>{{ triageDesk.length }}</b></a>
        <a href="#maintenance">配置维护</a>
        <a href="#search">金仓搜索</a>
        <a href="#table">批量编辑</a>
      </nav>
      <button v-if="session" type="button" @click="logout">退出登录</button>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Admin Web</p>
          <h2>基础数据、号源与 AI 配置统一维护</h2>
          <p>管理端强调批量浏览、快速编辑、分诊改派和数据发布状态。</p>
        </div>
        <div class="toolbar">
          <button type="button">导出本页</button>
          <button class="primary" type="button" :disabled="busy" @click="refresh">刷新数据</button>
        </div>
      </header>

      <div v-if="message" class="notice" :class="{ error: serviceError, success: !serviceError }">{{ message }}</div>

      <section v-if="!session" class="panel login-panel">
        <h2>管理员登录</h2>
        <div class="form-grid">
          <label>账号<input v-model="loginForm.account" /></label>
          <label>密码<input v-model="loginForm.password" type="password" /></label>
        </div>
        <button class="primary" type="button" :disabled="busy" @click="login">进入管理端</button>
      </section>

      <template v-else>
        <section class="metrics">
          <div class="metric"><span>启用科室</span><strong>{{ departments.length }}</strong></div>
          <div class="metric"><span>在岗医生</span><strong>{{ doctors.length }}</strong></div>
          <div class="metric"><span>待发布建议</span><strong>{{ suggestions.length }}</strong></div>
          <div class="metric"><span>待处理分诊</span><strong>{{ triageDesk.length }}</strong></div>
        </section>

        <section class="main-grid">
          <div class="stack">
            <section id="catalog" class="panel">
              <div class="panel-header">
                <div>
                  <h2>科室与医生维护</h2>
                  <p>基础资料表单编辑，右侧表格批量浏览和状态管理。</p>
                </div>
                <span class="tag success">自动保存草稿</span>
              </div>
              <div class="form-grid three">
                <label>科室编码<input v-model="departmentForm.code" /></label>
                <label>科室名称<input v-model="departmentForm.name" /></label>
                <label>说明<input v-model="departmentForm.description" /></label>
                <label>医生姓名<input v-model="doctorForm.name" /></label>
                <label>手机号<input v-model="doctorForm.phone" /></label>
                <label>科室 ID<input v-model.number="doctorForm.departmentId" type="number" /></label>
                <label>职称<input v-model="doctorForm.title" /></label>
                <label>专长<input v-model="doctorForm.specialty" /></label>
                <label>新密码<input v-model="doctorForm.password" placeholder="留空则不修改" /></label>
              </div>
              <div class="toolbar">
                <button type="button" :disabled="busy" @click="saveDepartment">保存科室</button>
                <button class="primary" type="button" :disabled="busy" @click="saveDoctor">保存医生</button>
              </div>
              <div class="table-scroll">
                <table>
                  <thead><tr><th>医生</th><th>科室</th><th>职称</th><th>专长</th><th>状态</th></tr></thead>
                  <tbody>
                    <tr v-for="doctor in doctors" :key="String(doctor.id)">
                      <td>{{ text(doctor, "name") }}</td>
                      <td>{{ text(doctor, "departmentName") }}</td>
                      <td>{{ text(doctor, "title") }}</td>
                      <td>{{ text(doctor, "specialty") }}</td>
                      <td><span class="tag" :class="tagClass(doctor.status)">{{ text(doctor, "status", "ENABLED") }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="!doctors.length" class="empty-state"><strong>暂无医生</strong><span>保存医生后将在这里显示。</span></div>
            </section>

            <section id="schedule" class="panel">
              <div class="panel-header">
                <div>
                  <h2>AI 排班与号源发布</h2>
                  <p>生成建议、审阅原因、批量发布号源，发布前保留可回退状态。</p>
                </div>
                <span class="tag warning">建议待审 {{ suggestions.length }}</span>
              </div>
              <div class="form-grid three">
                <label>开始日期<input v-model="scheduleForm.startDate" type="date" /></label>
                <label>生成天数<input v-model.number="scheduleForm.days" type="number" /></label>
                <label>发布策略<select><option>仅发布已勾选建议</option><option>全部发布</option></select></label>
              </div>
              <div class="toolbar">
                <button type="button" :disabled="busy" @click="generateSchedule">生成 AI 建议</button>
                <button class="primary" type="button" :disabled="busy || !suggestions.length" @click="publishSchedule">发布号源</button>
                <button class="warning" type="button">撤回草稿</button>
              </div>
              <div class="schedule-grid">
                <div>
                  <h3>待发布建议</h3>
                  <p v-for="item in suggestions" :key="String(item.id)" @click="showScheduleSuggestion(item.id)">
                    #{{ text(item, "id") }} {{ text(item, "workDate") }} {{ text(item, "timeRange") }} {{ text(item, "doctorName") }}
                  </p>
                  <div v-if="!suggestions.length" class="empty-state"><strong>暂无建议</strong><span>点击生成 AI 建议。</span></div>
                </div>
                <div>
                  <h3>建议详情</h3>
                  <p v-if="selectedScheduleSuggestion">#{{ selectedScheduleSuggestion.id }} {{ selectedScheduleSuggestion.doctorName }} · {{ selectedScheduleSuggestion.reason }}</p>
                  <p v-else>选择左侧建议后查看详情。</p>
                </div>
                <div>
                  <h3>已发布排班</h3>
                  <p v-for="item in schedules" :key="String(item.id)">#{{ text(item, "id") }} {{ text(item, "workDate") }} {{ text(item, "timeRange") }} {{ text(item, "doctorName") }}</p>
                </div>
              </div>
            </section>

            <section id="triage" class="panel">
              <div class="panel-header">
                <div>
                  <h2>分诊工作台</h2>
                  <p>查看分诊详情、人工改派医生、关闭异常分诊记录。</p>
                </div>
              </div>
              <div class="form-grid three">
                <label>分诊记录 ID<input v-model.number="assignForm.triageRecordId" type="number" /></label>
                <label>分配医生
                  <select v-model.number="assignForm.doctorId">
                    <option v-for="doctor in doctors" :key="String(doctor.id)" :value="Number(doctor.id)">{{ doctor.name }} · {{ doctor.departmentName }}</option>
                  </select>
                </label>
                <div class="actions"><button class="primary" type="button" :disabled="busy" @click="assignTriage">分配医生</button></div>
              </div>
              <div class="table-scroll">
                <table>
                  <thead><tr><th>记录</th><th>主诉</th><th>推荐科室</th><th>当前医生</th><th>状态</th><th>操作</th></tr></thead>
                  <tbody>
                    <tr v-for="item in triageDesk" :key="String(item.triageRecordId)" @click="useTriage(item)">
                      <td>#{{ text(item, "triageRecordId") }}</td>
                      <td>{{ text(item, "chiefComplaint") }}</td>
                      <td>{{ text(item, "recommendedDepartment") }}</td>
                      <td>{{ text(item, "assignedDoctorName") }}</td>
                      <td><span class="tag" :class="tagClass(item.status)">{{ text(item, "status") }}</span></td>
                      <td class="table-actions">
                        <button type="button" @click.stop="showTriage(item.triageRecordId)">详情</button>
                        <button class="danger" type="button" @click.stop="closeTriage(item.triageRecordId)">关闭</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="selectedTriage" class="notice">
                <strong>分诊详情 #{{ selectedTriage.triageRecordId }}</strong>
                <span>{{ selectedTriage.chiefComplaint }} · {{ selectedTriage.reason }}</span>
              </div>
              <div v-if="!triageDesk.length" class="empty-state"><strong>暂无分诊记录</strong><span>患者提交分诊后会进入工作台。</span></div>
            </section>
          </div>

          <aside class="stack">
            <section id="maintenance" class="panel">
              <div class="panel-header">
                <div>
                  <h2>字典、药品、Prompt、知识库维护</h2>
                  <p>用标签页收敛配置项，保留表单和发布状态。</p>
                </div>
              </div>
              <div class="tabs">
                <button class="active" type="button">字典</button>
                <button type="button">药品</button>
                <button type="button">Prompt</button>
                <button type="button">知识库</button>
              </div>
              <div class="form-grid">
                <label>字典类型<input v-model="dictForm.dictType" /></label>
                <label>字典键<input v-model="dictForm.dictKey" /></label>
                <label>字典值<input v-model="dictForm.dictValue" /></label>
                <label>药品名称<input v-model="drugForm.name" /></label>
                <label>规格<input v-model="drugForm.specification" /></label>
                <label>Prompt 模板<input v-model="promptForm.templateName" /></label>
                <label>禁忌<textarea v-model="drugForm.contraindication" rows="2" /></label>
                <label>相互作用<textarea v-model="drugForm.interactionRule" rows="2" /></label>
                <label>知识标题<input v-model="knowledgeForm.title" /></label>
                <label>科室编码<input v-model="knowledgeForm.departmentCode" /></label>
                <label>症状<textarea v-model="knowledgeForm.symptoms" rows="2" /></label>
                <label>危险信号<textarea v-model="knowledgeForm.riskSignals" rows="2" /></label>
              </div>
              <label>Prompt 内容<textarea v-model="promptForm.templateContent" rows="3" /></label>
              <label>知识库建议<textarea v-model="knowledgeForm.advice" rows="3" /></label>
              <div class="toolbar">
                <button type="button" :disabled="busy" @click="saveDict">保存字典</button>
                <button type="button" :disabled="busy" @click="saveDrug">保存药品</button>
                <button type="button" :disabled="busy" @click="savePrompt">保存 Prompt</button>
                <button class="primary" type="button" :disabled="busy" @click="saveKnowledge">保存知识库</button>
              </div>
            </section>

            <section id="search" class="panel">
              <div class="panel-header">
                <div>
                  <h2>金仓搜索与当前数据概览</h2>
                  <p>用于核对知识库、药品、Prompt 和基础数据是否一致。</p>
                </div>
              </div>
              <div class="form-grid">
                <label>关键词<input v-model="search.q" /></label>
                <label>科室编码<input v-model="search.departmentCode" /></label>
              </div>
              <div class="toolbar">
                <button class="primary" type="button" :disabled="busy" @click="doSearch">搜索</button>
                <button type="button" @click="search.q = ''">重置</button>
              </div>
              <div class="summary-strip">
                <div><span>知识库</span><strong>{{ knowledge.length }} 条</strong></div>
                <div><span>药品</span><strong>{{ drugs.length }} 条</strong></div>
                <div><span>Prompt</span><strong>{{ prompts.length }} 个</strong></div>
              </div>
              <div class="mini-list">
                <p v-for="item in searchResults.knowledge" :key="`k-${String(item.id)}`">知识 #{{ text(item, "id") }} {{ text(item, "title") }}</p>
                <p v-for="item in searchResults.drugs" :key="`d-${String(item.id)}`">药品 #{{ text(item, "id") }} {{ text(item, "name") }}</p>
                <p v-for="item in searchResults.prompts" :key="`p-${String(item.id)}`">Prompt #{{ text(item, "id") }} {{ text(item, "templateName") }}</p>
              </div>
              <div v-if="!searchCount" class="empty-state"><strong>暂无搜索结果</strong><span>输入关键词后搜索金仓数据。</span></div>
              <div class="notice error">错误状态：金仓搜索连接超时时，当前区域显示缓存结果和重试入口。</div>
            </section>

            <section id="table" class="panel">
              <div class="panel-header">
                <div>
                  <h2>表格批量浏览和编辑状态</h2>
                  <p>支持选中、编辑中、待发布、禁用等状态识别。</p>
                </div>
                <button type="button">批量保存</button>
              </div>
              <div class="table-scroll">
                <table>
                  <thead><tr><th>选择</th><th>对象</th><th>类型</th><th>状态</th><th>编辑状态</th></tr></thead>
                  <tbody>
                    <tr>
                      <td><input type="checkbox" checked /></td>
                      <td>普通感冒知识条目</td>
                      <td>知识库</td>
                      <td><span class="tag success">启用</span></td>
                      <td><span class="tag warning">待发布</span></td>
                    </tr>
                    <tr>
                      <td><input type="checkbox" /></td>
                      <td>GENERAL_MEDICAL_RECORD_v1</td>
                      <td>Prompt</td>
                      <td><span class="tag success">启用</span></td>
                      <td><span class="tag info">编辑中</span></td>
                    </tr>
                    <tr>
                      <td><input type="checkbox" disabled /></td>
                      <td>旧版感冒分诊规则</td>
                      <td>字典</td>
                      <td><span class="tag">停用</span></td>
                      <td><span class="tag">只读</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="empty-state"><strong>当前筛选无数据</strong><span>批量表格无结果时，提示清空筛选或新建数据。</span></div>
            </section>
          </aside>
        </section>
      </template>
    </section>
  </main>
</template>
