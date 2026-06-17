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
import { useAdminWorkflowStore, useAuthStore } from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { session, permissionError } = storeToRefs(auth);
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

const loading = reactive({
  boot: true,
  auth: false,
  data: false,
  save: false,
  schedule: false,
  triage: false,
  search: false,
});
const error = ref("");
const notice = ref("");
const activeModule = ref<"dashboard" | "catalog" | "schedule" | "triage" | "search">("dashboard");
const activeCatalog = ref<"department" | "doctor" | "drug" | "knowledge" | "prompt" | "dict">("department");
const catalogKeyword = ref("");
const loginForm = reactive({ account: "", password: "" });
const departmentForm = reactive({ id: undefined as number | undefined, code: "", name: "", description: "" });
const doctorForm = reactive({ id: undefined as number | undefined, name: "", phone: "", password: "", departmentId: 0, title: "", specialty: "", status: "ENABLED" });
const drugForm = reactive({ id: undefined as number | undefined, name: "", specification: "", contraindication: "", interactionRule: "", status: "ENABLED" });
const knowledgeForm = reactive({ id: undefined as number | undefined, title: "", symptoms: "", riskSignals: "", advice: "", departmentCode: "", status: "ENABLED" });
const promptForm = reactive({ id: undefined as number | undefined, taskType: "MEDICAL_RECORD", departmentCode: "", templateName: "", templateContent: "", outputSchema: "{\"type\":\"object\"}", version: "v1", enabled: true });
const dictForm = reactive({ id: undefined as number | undefined, dictType: "", dictKey: "", dictValue: "", sort: 0, status: "ENABLED" });
const scheduleForm = reactive({ startDate: nextDate(), days: 3 });
const triageFilter = reactive({ risk: "", department: "", keyword: "" });
const assignForm = reactive({ triageRecordId: 0, doctorId: 0 });
const search = reactive({ q: "", departmentCode: "" });
const searchResults = reactive({
  knowledge: [] as DataRow[],
  drugs: [] as DataRow[],
  prompts: [] as DataRow[],
});
let unbindUnauthorized: (() => void) | null = null;

const moduleTitle = computed(() => ({
  dashboard: "工作台",
  catalog: "基础数据",
  schedule: "排班管理",
  triage: "分诊工作台",
  search: "检索维护",
}[activeModule.value]));
const enabledDepartments = computed(() => departments.value);
const keyword = computed(() => catalogKeyword.value.trim().toLowerCase());
const filteredDepartments = computed(() => departments.value.filter((item) => matchCatalog(item, ["code", "name", "description"])));
const filteredDoctors = computed(() => doctors.value.filter((item) => matchCatalog(item, ["name", "phone", "departmentName", "title", "specialty"])));
const filteredDrugs = computed(() => drugs.value.filter((item) => matchCatalog(item, ["name", "specification", "contraindication", "status"])));
const filteredKnowledge = computed(() => knowledge.value.filter((item) => matchCatalog(item, ["title", "symptoms", "riskSignals", "departmentCode", "status"])));
const filteredPrompts = computed(() => prompts.value.filter((item) => matchCatalog(item, ["taskType", "templateName", "departmentCode", "version"])));
const filteredDicts = computed(() => dicts.value.filter((item) => matchCatalog(item, ["dictType", "dictKey", "dictValue", "status"])));
const filteredTriage = computed(() => triageDesk.value.filter((item) => {
  const haystack = `${fieldText(item, "chiefComplaint", "")} ${fieldText(item, "reason", "")}`.toLowerCase();
  const filterKeyword = triageFilter.keyword.trim().toLowerCase();
  const departmentMatched = !triageFilter.department || fieldText(item, "recommendedDepartment", "").includes(triageFilter.department);
  const keywordMatched = !filterKeyword || haystack.includes(filterKeyword);
  const riskMatched = !triageFilter.risk || fieldText(item, "status", "").includes(triageFilter.risk);
  return departmentMatched && keywordMatched && riskMatched;
}));
const highRiskCount = computed(() =>
  triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(fieldText(item, "status", ""))).length
);
const searchCount = computed(() => searchResults.knowledge.length + searchResults.drugs.length + searchResults.prompts.length);

function nextDate() {
  return new Date(Date.now() + 86400000).toISOString().slice(0, 10);
}

function text(item: DataRow | null | undefined, key: string, fallback = "-") {
  return fieldText(item, key, fallback);
}

function matchCatalog(item: DataRow, keys: string[]) {
  if (!keyword.value) return true;
  return keys.some((key) => fieldText(item, key, "").toLowerCase().includes(keyword.value));
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
    setError("请输入管理员账号和密码。");
    return;
  }
  await run("auth", "登录失败", async () => {
    const nextSession = await api.loginAdmin(loginForm.account.trim(), loginForm.password);
    auth.save("admin-session", nextSession, "ADMIN");
    if (!auth.requireRole("ADMIN")) return;
    await refresh();
    setNotice("已进入管理端。");
  });
}

function logout() {
  auth.logout();
}

async function refresh() {
  if (!session.value || !auth.requireRole("ADMIN")) return;
  await run("data", "数据同步失败", async () => {
    await workflow.refresh(auth.token());
    if (!doctorForm.departmentId && departments.value[0]) {
      doctorForm.departmentId = toNumber(departments.value[0].id);
    }
    if (!assignForm.doctorId && doctors.value[0]) {
      assignForm.doctorId = toNumber(doctors.value[0].id);
    }
  });
}

function resetDepartment() {
  Object.assign(departmentForm, { id: undefined, code: "", name: "", description: "" });
}

function resetDoctor() {
  Object.assign(doctorForm, { id: undefined, name: "", phone: "", password: "", departmentId: toNumber(departments.value[0]?.id), title: "", specialty: "", status: "ENABLED" });
}

function resetDrug() {
  Object.assign(drugForm, { id: undefined, name: "", specification: "", contraindication: "", interactionRule: "", status: "ENABLED" });
}

function resetKnowledge() {
  Object.assign(knowledgeForm, { id: undefined, title: "", symptoms: "", riskSignals: "", advice: "", departmentCode: "", status: "ENABLED" });
}

function resetPrompt() {
  Object.assign(promptForm, { id: undefined, taskType: "MEDICAL_RECORD", departmentCode: "", templateName: "", templateContent: "", outputSchema: "{\"type\":\"object\"}", version: "v1", enabled: true });
}

function resetDict() {
  Object.assign(dictForm, { id: undefined, dictType: "", dictKey: "", dictValue: "", sort: 0, status: "ENABLED" });
}

function editDepartment(item: DataRow) {
  activeCatalog.value = "department";
  Object.assign(departmentForm, {
    id: toNumber(item.id) || undefined,
    code: text(item, "code", ""),
    name: text(item, "name", ""),
    description: text(item, "description", ""),
  });
}

function editDoctor(item: DataRow) {
  activeCatalog.value = "doctor";
  Object.assign(doctorForm, {
    id: toNumber(item.id) || undefined,
    name: text(item, "name", ""),
    phone: text(item, "phone", ""),
    password: "",
    departmentId: toNumber(item.departmentId),
    title: text(item, "title", ""),
    specialty: text(item, "specialty", ""),
    status: text(item, "status", "ENABLED"),
  });
}

function editDrug(item: DataRow) {
  activeCatalog.value = "drug";
  Object.assign(drugForm, {
    id: toNumber(item.id) || undefined,
    name: text(item, "name", ""),
    specification: text(item, "specification", ""),
    contraindication: text(item, "contraindication", ""),
    interactionRule: text(item, "interactionRule", ""),
    status: text(item, "status", "ENABLED"),
  });
}

function editKnowledge(item: DataRow) {
  activeCatalog.value = "knowledge";
  Object.assign(knowledgeForm, {
    id: toNumber(item.id) || undefined,
    title: text(item, "title", ""),
    symptoms: text(item, "symptoms", ""),
    riskSignals: text(item, "riskSignals", ""),
    advice: text(item, "advice", ""),
    departmentCode: text(item, "departmentCode", ""),
    status: text(item, "status", "ENABLED"),
  });
}

function editPrompt(item: DataRow) {
  activeCatalog.value = "prompt";
  Object.assign(promptForm, {
    id: toNumber(item.id) || undefined,
    taskType: text(item, "taskType", ""),
    departmentCode: text(item, "departmentCode", ""),
    templateName: text(item, "templateName", ""),
    templateContent: text(item, "templateContent", ""),
    outputSchema: text(item, "outputSchema", "{\"type\":\"object\"}"),
    version: text(item, "version", "v1"),
    enabled: Boolean(item.enabled),
  });
}

function editDict(item: DataRow) {
  activeCatalog.value = "dict";
  Object.assign(dictForm, {
    id: toNumber(item.id) || undefined,
    dictType: text(item, "dictType", ""),
    dictKey: text(item, "dictKey", ""),
    dictValue: text(item, "dictValue", ""),
    sort: toNumber(item.sort),
    status: text(item, "status", "ENABLED"),
  });
}

function validateRequired(values: Array<[string, string | number | undefined]>) {
  const missing = values.find(([, value]) => value === undefined || value === "" || value === 0);
  return missing ? `请填写${missing[0]}。` : "";
}

async function saveDepartment() {
  const invalid = validateRequired([["科室编码", departmentForm.code], ["科室名称", departmentForm.name]]);
  if (invalid) return setError(invalid);
  await run("save", "科室保存失败", async () => {
    await api.saveDepartment(auth.token(), { ...departmentForm });
    resetDepartment();
    await refresh();
    setNotice("科室已保存。");
  });
}

async function saveDoctor() {
  const invalid = validateRequired([["医生姓名", doctorForm.name], ["手机号", doctorForm.phone], ["科室", doctorForm.departmentId]]);
  if (invalid) return setError(invalid);
  await run("save", "医生保存失败", async () => {
    await api.saveDoctor(auth.token(), { ...doctorForm });
    doctorForm.password = "";
    await refresh();
    setNotice("医生已保存。");
  });
}

async function saveDrug() {
  const invalid = validateRequired([["药品名称", drugForm.name]]);
  if (invalid) return setError(invalid);
  await run("save", "药品保存失败", async () => {
    await api.saveDrug(auth.token(), { ...drugForm });
    await refresh();
    setNotice("药品已保存。");
  });
}

async function saveKnowledge() {
  const invalid = validateRequired([["知识标题", knowledgeForm.title], ["症状", knowledgeForm.symptoms], ["建议", knowledgeForm.advice]]);
  if (invalid) return setError(invalid);
  await run("save", "知识库保存失败", async () => {
    await api.saveKnowledgeEntry(auth.token(), { ...knowledgeForm });
    await refresh();
    setNotice("知识库条目已保存。");
  });
}

async function savePrompt() {
  const invalid = validateRequired([["任务类型", promptForm.taskType], ["模板名称", promptForm.templateName], ["模板内容", promptForm.templateContent]]);
  if (invalid) return setError(invalid);
  await run("save", "Prompt 保存失败", async () => {
    await api.savePrompt(auth.token(), { ...promptForm });
    await refresh();
    setNotice("Prompt 已保存。");
  });
}

async function saveDict() {
  const invalid = validateRequired([["字典类型", dictForm.dictType], ["字典键", dictForm.dictKey], ["字典值", dictForm.dictValue]]);
  if (invalid) return setError(invalid);
  await run("save", "字典保存失败", async () => {
    await api.saveDict(auth.token(), { ...dictForm });
    await refresh();
    setNotice("字典已保存。");
  });
}

async function disableDrug(item: DataRow) {
  if (!window.confirm("确认停用该药品？")) return;
  await run("save", "药品停用失败", async () => {
    await api.saveDrug(auth.token(), {
      id: toNumber(item.id),
      name: text(item, "name", ""),
      specification: text(item, "specification", ""),
      contraindication: text(item, "contraindication", ""),
      interactionRule: text(item, "interactionRule", ""),
      status: "DISABLED",
    });
    await refresh();
    setNotice("药品已停用。");
  });
}

async function generateSchedule() {
  await run("schedule", "排班建议生成失败", async () => {
    suggestions.value = await api.generateSchedule(auth.token(), { ...scheduleForm });
    setNotice("排班建议已生成。该能力由后端规则生成，发布前请人工审核。");
  });
}

async function publishSchedule() {
  const ids = suggestions.value.map((item) => toNumber(item.id)).filter(Boolean);
  if (!ids.length) {
    setError("请先生成或选择待发布建议。");
    return;
  }
  if (!window.confirm("确认发布当前待发布排班建议并生成号源？")) return;
  await run("schedule", "排班发布失败", async () => {
    schedules.value = await api.publishSchedule(auth.token(), { suggestionIds: ids });
    suggestions.value = [];
    await refresh();
    setNotice("排班已发布，号源容量和状态已同步。");
  });
}

async function showScheduleSuggestion(id: unknown) {
  await run("schedule", "排班建议详情加载失败", async () => {
    selectedScheduleSuggestion.value = await api.scheduleSuggestionDetail(auth.token(), toNumber(id));
  });
}

function useTriage(item: DataRow) {
  assignForm.triageRecordId = toNumber(item.triageRecordId);
  assignForm.doctorId = toNumber(item.assignedDoctorId, toNumber(doctors.value[0]?.id));
}

async function showTriage(item: DataRow) {
  await run("triage", "分诊详情加载失败", async () => {
    selectedTriage.value = await api.triageDetail(auth.token(), toNumber(item.triageRecordId));
    useTriage(item);
  });
}

async function assignTriage() {
  const invalid = validateRequired([["分诊记录", assignForm.triageRecordId], ["医生", assignForm.doctorId]]);
  if (invalid) return setError(invalid);
  await run("triage", "分诊分配失败", async () => {
    await api.assignTriage(auth.token(), { ...assignForm });
    await refresh();
    setNotice("分诊记录已分配。");
  });
}

async function closeTriage(item: DataRow) {
  if (!window.confirm("确认关闭该分诊记录？")) return;
  await run("triage", "关闭分诊失败", async () => {
    await api.closeTriage(auth.token(), toNumber(item.triageRecordId));
    await refresh();
    setNotice("分诊记录已关闭。");
  });
}

async function doSearch() {
  if (!search.q.trim()) {
    setError("请输入检索关键词。");
    return;
  }
  await run("search", "检索失败", async () => {
    const [knowledgeList, drugList, promptList] = await Promise.all([
      api.searchKnowledge(auth.token(), search.q.trim(), search.departmentCode.trim()),
      api.searchDrugs(auth.token(), search.q.trim()),
      api.searchPrompts(auth.token(), search.q.trim()),
    ]);
    searchResults.knowledge = knowledgeList;
    searchResults.drugs = drugList;
    searchResults.prompts = promptList;
  });
}

onMounted(async () => {
  unbindUnauthorized = auth.bindUnauthorized();
  auth.load("admin-session", "ADMIN");
  if (session.value && !permissionError.value) {
    await refresh();
  }
  loading.boot = false;
});

onBeforeUnmount(() => {
  unbindUnauthorized?.();
});
</script>

<template>
  <main class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>管</span>
        <div>
          <h1>医疗管理端</h1>
          <p>基础数据 · 排班 · 分诊</p>
        </div>
      </div>

      <nav v-if="session && !permissionError" class="side-nav" aria-label="管理端导航">
        <button type="button" :class="{ active: activeModule === 'dashboard' }" @click="activeModule = 'dashboard'">工作台</button>
        <button type="button" :class="{ active: activeModule === 'catalog' }" @click="activeModule = 'catalog'">基础数据</button>
        <button type="button" :class="{ active: activeModule === 'schedule' }" @click="activeModule = 'schedule'">排班管理</button>
        <button type="button" :class="{ active: activeModule === 'triage' }" @click="activeModule = 'triage'">分诊工作台</button>
        <button type="button" :class="{ active: activeModule === 'search' }" @click="activeModule = 'search'">检索维护</button>
      </nav>

      <div v-if="session" class="user-card">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <span class="tag warning">{{ highRiskCount }} 条需关注</span>
      </div>
      <button v-if="session" type="button" @click="logout">退出</button>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Admin Web</p>
          <h2>{{ moduleTitle }}</h2>
          <p>维护基础资料、排班号源、分诊分配和检索内容。</p>
        </div>
        <div v-if="session" class="toolbar">
          <span>{{ session.name }}</span>
          <button type="button" :disabled="loading.data" @click="refresh">刷新数据</button>
        </div>
      </header>

      <div v-if="error" class="notice error">{{ error }}</div>
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <div v-if="permissionError" class="notice error">
        <span>{{ permissionError }}</span>
        <button type="button" @click="logout">切换账号</button>
      </div>

      <section v-if="loading.boot" class="panel">
        <div class="loading-state">正在加载管理端配置...</div>
      </section>

      <form v-else-if="!session || permissionError" class="panel login-panel" @submit.prevent="login">
        <div class="panel-title">
          <p class="eyebrow">管理员登录</p>
          <h2>进入管理端</h2>
          <p>登录后可维护基础数据、排班建议和分诊分配。</p>
        </div>
        <div class="form-grid">
          <label>账号<input v-model.trim="loginForm.account" autocomplete="username" /></label>
          <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
        </div>
        <button class="primary" type="submit" :disabled="loading.auth">进入管理端</button>
      </form>

      <template v-else>
        <section v-show="activeModule === 'dashboard'" class="page-stack">
          <div class="metrics">
            <div class="metric"><span>科室</span><strong>{{ departments.length }}</strong></div>
            <div class="metric"><span>医生</span><strong>{{ doctors.length }}</strong></div>
            <div class="metric"><span>药品</span><strong>{{ drugs.length }}</strong></div>
            <div class="metric"><span>分诊记录</span><strong>{{ triageDesk.length }}</strong></div>
          </div>
          <div class="dashboard-grid">
            <section class="panel">
              <div class="panel-title">
                <h2>待处理分诊</h2>
                <p>高风险或需人工处理的记录应优先分配医生。</p>
              </div>
              <div v-if="highRiskCount" class="focus-list">
                <article v-for="item in triageDesk.filter((row) => ['MANUAL_REQUIRED', 'HIGH'].includes(text(row, 'status'))).slice(0, 5)" :key="String(item.triageRecordId)">
                  <span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span>
                  <strong>#{{ text(item, "triageRecordId") }} {{ text(item, "recommendedDepartment") }}</strong>
                  <button type="button" @click="activeModule = 'triage'; showTriage(item)">处理</button>
                </article>
              </div>
              <div v-else class="empty-state">暂无高风险或需人工处理的分诊记录。</div>
            </section>
            <section class="panel">
              <div class="panel-title">
                <h2>排班概况</h2>
                <p>查看待发布建议和已发布排班数量。</p>
              </div>
              <div class="summary-grid">
                <div><span>待发布建议</span><strong>{{ suggestions.length }}</strong></div>
                <div><span>已发布排班</span><strong>{{ schedules.length }}</strong></div>
              </div>
              <button type="button" @click="activeModule = 'schedule'">进入排班管理</button>
            </section>
          </div>
        </section>

        <section v-show="activeModule === 'catalog'" class="panel catalog-section">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">基础数据</p>
              <h2>批量维护</h2>
              <p>使用上方标签切换维护对象，右侧面板复用新增和编辑。</p>
            </div>
            <label class="search-field">搜索<input v-model.trim="catalogKeyword" placeholder="输入关键词筛选当前标签" /></label>
          </div>

          <div class="tabs">
            <button type="button" :class="{ active: activeCatalog === 'department' }" @click="activeCatalog = 'department'">科室</button>
            <button type="button" :class="{ active: activeCatalog === 'doctor' }" @click="activeCatalog = 'doctor'">医生</button>
            <button type="button" :class="{ active: activeCatalog === 'drug' }" @click="activeCatalog = 'drug'">药品</button>
            <button type="button" :class="{ active: activeCatalog === 'knowledge' }" @click="activeCatalog = 'knowledge'">知识库</button>
            <button type="button" :class="{ active: activeCatalog === 'prompt' }" @click="activeCatalog = 'prompt'">Prompt</button>
            <button type="button" :class="{ active: activeCatalog === 'dict' }" @click="activeCatalog = 'dict'">字典</button>
          </div>

          <div class="catalog-grid">
            <div class="table-scroll">
              <table v-show="activeCatalog === 'department'">
                <thead><tr><th>ID</th><th>编码</th><th>名称</th><th>说明</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredDepartments" :key="String(item.id)">
                    <td>{{ text(item, "id") }}</td><td>{{ text(item, "code") }}</td><td>{{ text(item, "name") }}</td><td>{{ text(item, "description") }}</td>
                    <td><button type="button" @click="editDepartment(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <table v-show="activeCatalog === 'doctor'">
                <thead><tr><th>医生</th><th>科室</th><th>职称</th><th>专长</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredDoctors" :key="String(item.id)">
                    <td>{{ text(item, "name") }}</td><td>{{ text(item, "departmentName") }}</td><td>{{ text(item, "title") }}</td><td>{{ text(item, "specialty") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editDoctor(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <table v-show="activeCatalog === 'drug'">
                <thead><tr><th>药品</th><th>规格</th><th>禁忌</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredDrugs" :key="String(item.id)">
                    <td>{{ text(item, "name") }}</td><td>{{ text(item, "specification") }}</td><td>{{ text(item, "contraindication") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td class="table-actions"><button type="button" @click="editDrug(item)">编辑</button><button class="danger subtle" type="button" @click="disableDrug(item)">停用</button></td>
                  </tr>
                </tbody>
              </table>
              <table v-show="activeCatalog === 'knowledge'">
                <thead><tr><th>标题</th><th>科室</th><th>症状</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredKnowledge" :key="String(item.id)">
                    <td>{{ text(item, "title") }}</td><td>{{ text(item, "departmentCode") }}</td><td>{{ text(item, "symptoms") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editKnowledge(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <table v-show="activeCatalog === 'prompt'">
                <thead><tr><th>任务</th><th>模板</th><th>科室</th><th>版本</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredPrompts" :key="String(item.id)">
                    <td>{{ text(item, "taskType") }}</td><td>{{ text(item, "templateName") }}</td><td>{{ text(item, "departmentCode") }}</td><td>{{ text(item, "version") }}</td>
                    <td><span class="tag" :class="item.enabled ? 'success' : 'danger'">{{ item.enabled ? "启用" : "停用" }}</span></td>
                    <td><button type="button" @click="editPrompt(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <table v-show="activeCatalog === 'dict'">
                <thead><tr><th>类型</th><th>键</th><th>值</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredDicts" :key="String(item.id)">
                    <td>{{ text(item, "dictType") }}</td><td>{{ text(item, "dictKey") }}</td><td>{{ text(item, "dictValue") }}</td><td>{{ text(item, "sort") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editDict(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <aside class="editor-panel">
              <form v-show="activeCatalog === 'department'" @submit.prevent="saveDepartment">
                <h3>{{ departmentForm.id ? "编辑科室" : "新增科室" }}</h3>
                <label>编码<input v-model.trim="departmentForm.code" /></label>
                <label>名称<input v-model.trim="departmentForm.name" /></label>
                <label>说明<textarea v-model.trim="departmentForm.description" rows="4" /></label>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetDepartment">新增</button></div>
              </form>

              <form v-show="activeCatalog === 'doctor'" @submit.prevent="saveDoctor">
                <h3>{{ doctorForm.id ? "编辑医生" : "新增医生" }}</h3>
                <div class="form-grid">
                  <label>姓名<input v-model.trim="doctorForm.name" /></label>
                  <label>手机号<input v-model.trim="doctorForm.phone" /></label>
                  <label>科室
                    <select v-model.number="doctorForm.departmentId">
                      <option :value="0">请选择</option>
                      <option v-for="item in enabledDepartments" :key="String(item.id)" :value="toNumber(item.id)">{{ item.name }}</option>
                    </select>
                  </label>
                  <label>职称<input v-model.trim="doctorForm.title" /></label>
                  <label>状态
                    <select v-model="doctorForm.status">
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  </label>
                  <label>新密码<input v-model="doctorForm.password" type="password" autocomplete="new-password" /></label>
                </div>
                <label>专长<textarea v-model.trim="doctorForm.specialty" rows="4" /></label>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetDoctor">新增</button></div>
              </form>

              <form v-show="activeCatalog === 'drug'" @submit.prevent="saveDrug">
                <h3>{{ drugForm.id ? "编辑药品" : "新增药品" }}</h3>
                <div class="form-grid">
                  <label>名称<input v-model.trim="drugForm.name" /></label>
                  <label>规格<input v-model.trim="drugForm.specification" /></label>
                  <label>状态
                    <select v-model="drugForm.status">
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  </label>
                </div>
                <label>禁忌<textarea v-model.trim="drugForm.contraindication" rows="4" /></label>
                <label>相互作用规则<textarea v-model.trim="drugForm.interactionRule" rows="4" /></label>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetDrug">新增</button></div>
              </form>

              <form v-show="activeCatalog === 'knowledge'" @submit.prevent="saveKnowledge">
                <h3>{{ knowledgeForm.id ? "编辑知识库" : "新增知识库" }}</h3>
                <div class="form-grid">
                  <label>标题<input v-model.trim="knowledgeForm.title" /></label>
                  <label>科室编码<input v-model.trim="knowledgeForm.departmentCode" /></label>
                  <label>状态
                    <select v-model="knowledgeForm.status">
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  </label>
                </div>
                <label>症状<textarea v-model.trim="knowledgeForm.symptoms" rows="3" /></label>
                <label>风险信号<textarea v-model.trim="knowledgeForm.riskSignals" rows="3" /></label>
                <label>建议<textarea v-model.trim="knowledgeForm.advice" rows="4" /></label>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetKnowledge">新增</button></div>
              </form>

              <form v-show="activeCatalog === 'prompt'" @submit.prevent="savePrompt">
                <h3>{{ promptForm.id ? "编辑 Prompt" : "新增 Prompt" }}</h3>
                <div class="form-grid">
                  <label>任务类型<input v-model.trim="promptForm.taskType" /></label>
                  <label>科室编码<input v-model.trim="promptForm.departmentCode" /></label>
                  <label>模板名称<input v-model.trim="promptForm.templateName" /></label>
                  <label>版本<input v-model.trim="promptForm.version" /></label>
                  <label>启用
                    <select v-model="promptForm.enabled">
                      <option :value="true">启用</option>
                      <option :value="false">停用</option>
                    </select>
                  </label>
                </div>
                <label>输出 Schema<textarea v-model.trim="promptForm.outputSchema" rows="3" /></label>
                <label>模板内容<textarea v-model.trim="promptForm.templateContent" rows="6" /></label>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetPrompt">新增</button></div>
              </form>

              <form v-show="activeCatalog === 'dict'" @submit.prevent="saveDict">
                <h3>{{ dictForm.id ? "编辑字典" : "新增字典" }}</h3>
                <div class="form-grid">
                  <label>类型<input v-model.trim="dictForm.dictType" /></label>
                  <label>键<input v-model.trim="dictForm.dictKey" /></label>
                  <label>值<input v-model.trim="dictForm.dictValue" /></label>
                  <label>排序<input v-model.number="dictForm.sort" type="number" /></label>
                  <label>状态
                    <select v-model="dictForm.status">
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  </label>
                </div>
                <div class="toolbar"><button class="primary" type="submit" :disabled="loading.save">保存</button><button type="button" @click="resetDict">新增</button></div>
              </form>
            </aside>
          </div>
        </section>

        <section v-show="activeModule === 'schedule'" class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <p class="eyebrow">排班管理</p>
              <h2>排班建议与发布</h2>
              <p>生成的是排班建议，发布前需要管理员人工审核。</p>
            </div>
            <span class="tag warning">排班建议</span>
          </div>
          <div class="schedule-toolbar">
            <label>开始日期<input v-model="scheduleForm.startDate" type="date" /></label>
            <label>天数<input v-model.number="scheduleForm.days" type="number" min="1" max="14" /></label>
            <div class="toolbar">
              <button type="button" :disabled="loading.schedule" @click="generateSchedule">生成建议</button>
              <button class="primary" type="button" :disabled="loading.schedule || !suggestions.length" @click="publishSchedule">发布号源</button>
            </div>
          </div>
          <div class="schedule-grid">
            <section>
              <h3>待发布建议</h3>
              <article v-for="item in suggestions" :key="String(item.id)" class="schedule-item" @click="showScheduleSuggestion(item.id)">
                <strong>{{ text(item, "workDate") }} {{ text(item, "timeRange") }}</strong>
                <span>{{ text(item, "doctorName") }} · 容量 {{ text(item, "capacity") }}</span>
              </article>
              <div v-if="!suggestions.length" class="empty-state">暂无待发布建议。</div>
            </section>
            <section>
              <h3>建议原因</h3>
              <div v-if="selectedScheduleSuggestion" class="detail-note">
                <strong>{{ text(selectedScheduleSuggestion, "doctorName") }} / {{ text(selectedScheduleSuggestion, "departmentName") }}</strong>
                <p>{{ text(selectedScheduleSuggestion, "reason", "暂无原因说明") }}</p>
              </div>
              <div v-else class="empty-state">请选择一条建议查看原因。</div>
            </section>
            <section>
              <h3>已发布排班</h3>
              <article v-for="item in schedules" :key="String(item.id)" class="schedule-item">
                <strong>{{ text(item, "workDate") }} {{ text(item, "timeRange") }}</strong>
                <span>{{ text(item, "doctorName") }} · 容量 {{ text(item, "capacity") }}</span>
              </article>
              <div v-if="!schedules.length" class="empty-state">暂无已发布排班。</div>
            </section>
          </div>
        </section>

        <section v-show="activeModule === 'triage'" class="triage-grid">
          <section class="panel">
            <div class="panel-title">
              <p class="eyebrow">分诊工作台</p>
              <h2>分诊列表</h2>
              <p>高风险或需人工处理记录会突出显示。</p>
            </div>
            <div class="filter-grid">
              <label>状态
                <select v-model="triageFilter.risk">
                  <option value="">全部</option>
                  <option value="MANUAL_REQUIRED">需人工处理</option>
                  <option value="AI_RECOMMENDED">AI已推荐</option>
                  <option value="CLOSED">已关闭</option>
                </select>
              </label>
              <label>推荐科室<input v-model.trim="triageFilter.department" /></label>
              <label>关键词<input v-model.trim="triageFilter.keyword" /></label>
            </div>
            <div v-if="filteredTriage.length" class="table-scroll">
              <table>
                <thead><tr><th>记录</th><th>主诉</th><th>推荐科室</th><th>医生</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredTriage" :key="String(item.triageRecordId)" :class="{ dangerRow: ['MANUAL_REQUIRED', 'HIGH'].includes(text(item, 'status')) }" @click="useTriage(item)">
                    <td>#{{ text(item, "triageRecordId") }}</td><td>{{ text(item, "chiefComplaint") }}</td><td>{{ text(item, "recommendedDepartment") }}</td><td>{{ text(item, "assignedDoctorName", "未分配") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td class="table-actions"><button type="button" @click.stop="showTriage(item)">详情</button><button class="danger subtle" type="button" @click.stop="closeTriage(item)">关闭</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">当前筛选条件下暂无分诊记录。</div>
          </section>

          <aside class="panel">
            <div class="panel-title">
              <h2>详情与分配</h2>
              <p>选择记录后分配给合适医生。</p>
            </div>
            <div v-if="selectedTriage" class="detail-note">
              <strong>#{{ text(selectedTriage, "triageRecordId") }} {{ text(selectedTriage, "recommendedDepartment") }}</strong>
              <p>{{ text(selectedTriage, "chiefComplaint") }}</p>
              <p>{{ text(selectedTriage, "reason") }}</p>
            </div>
            <div v-else class="empty-state">请选择一条分诊记录。</div>
            <div class="form-grid">
              <label>分诊记录 ID<input v-model.number="assignForm.triageRecordId" type="number" /></label>
              <label>分配医生
                <select v-model.number="assignForm.doctorId">
                  <option :value="0">请选择</option>
                  <option v-for="doctor in doctors" :key="String(doctor.id)" :value="toNumber(doctor.id)">{{ doctor.name }} / {{ doctor.departmentName }}</option>
                </select>
              </label>
            </div>
            <button class="primary" type="button" :disabled="loading.triage" @click="assignTriage">分配医生</button>
          </aside>
        </section>

        <section v-show="activeModule === 'search'" class="panel">
          <div class="panel-title">
            <p class="eyebrow">检索维护</p>
            <h2>知识、药品和 Prompt 检索</h2>
            <p>统一使用后端检索接口，便于维护基础内容一致性。</p>
          </div>
          <div class="filter-grid">
            <label>关键词<input v-model.trim="search.q" /></label>
            <label>科室编码<input v-model.trim="search.departmentCode" /></label>
            <div class="toolbar end"><button class="primary" type="button" :disabled="loading.search" @click="doSearch">检索</button></div>
          </div>
          <div v-if="searchCount" class="search-grid">
            <section>
              <h3>知识库</h3>
              <article v-for="item in searchResults.knowledge" :key="`k-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "title") }}</article>
            </section>
            <section>
              <h3>药品</h3>
              <article v-for="item in searchResults.drugs" :key="`d-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "name") }}</article>
            </section>
            <section>
              <h3>Prompt</h3>
              <article v-for="item in searchResults.prompts" :key="`p-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "templateName") }}</article>
            </section>
          </div>
          <div v-else class="empty-state">请输入关键词后检索。</div>
        </section>
      </template>
    </section>
  </main>
</template>
