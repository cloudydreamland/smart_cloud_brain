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
const activeCatalog = ref<"department" | "doctor" | "drug" | "knowledge" | "prompt" | "dict">("department");
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

const enabledDepartments = computed(() => departments.value);
const filteredTriage = computed(() => triageDesk.value.filter((item) => {
  const haystack = `${fieldText(item, "chiefComplaint", "")} ${fieldText(item, "reason", "")}`.toLowerCase();
  const keyword = triageFilter.keyword.trim().toLowerCase();
  const departmentMatched = !triageFilter.department || fieldText(item, "recommendedDepartment", "").includes(triageFilter.department);
  const keywordMatched = !keyword || haystack.includes(keyword);
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
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>管</span>
        <div>
          <h1>管理工作台</h1>
          <p>基础数据 / 排班 / 分诊</p>
        </div>
      </div>
      <div v-if="session" class="user-card">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <div class="row-meta">
          <span class="tag success">已登录</span>
          <span class="tag warning">{{ highRiskCount }} 需关注</span>
        </div>
      </div>
      <nav class="side-nav">
        <a class="active" href="#catalog">基础数据 <b>{{ departments.length + doctors.length }}</b></a>
        <a href="#schedule">排班 <b>{{ suggestions.length || schedules.length }}</b></a>
        <a href="#triage">分诊 <b>{{ filteredTriage.length }}</b></a>
        <a href="#search">检索</a>
      </nav>
      <button v-if="session" type="button" @click="logout">退出</button>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Admin Web</p>
          <h2>运营管理</h2>
          <p>维护基础资料、排班号源和分诊分配。</p>
        </div>
        <div class="toolbar">
          <button type="button" :disabled="loading.data" @click="refresh">刷新数据</button>
        </div>
      </header>

      <div v-if="error" class="notice error">{{ error }}</div>
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <div v-if="permissionError" class="notice error">
        {{ permissionError }}
        <button type="button" @click="logout">切换账号</button>
      </div>

      <section v-if="loading.boot" class="panel">
        <div class="loading-state">正在加载管理端配置...</div>
      </section>

      <form v-else-if="!session || permissionError" class="panel login-panel" @submit.prevent="login">
        <h2>管理员登录</h2>
        <div class="form-grid">
          <label>账号<input v-model.trim="loginForm.account" autocomplete="username" /></label>
          <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
        </div>
        <button class="primary" type="submit" :disabled="loading.auth">进入管理端</button>
      </form>

      <template v-else>
        <section class="metrics">
          <div class="metric"><span>科室</span><strong>{{ departments.length }}</strong></div>
          <div class="metric"><span>医生</span><strong>{{ doctors.length }}</strong></div>
          <div class="metric"><span>药品</span><strong>{{ drugs.length }}</strong></div>
          <div class="metric"><span>分诊记录</span><strong>{{ triageDesk.length }}</strong></div>
        </section>

        <section id="catalog" class="panel">
          <div class="panel-header">
            <div>
              <h2>基础数据管理</h2>
              <p>字段与后端保存 DTO 对齐；后端未提供删除接口，危险操作以停用实现。</p>
            </div>
          </div>
          <div class="tabs">
            <button type="button" :class="{ active: activeCatalog === 'department' }" @click="activeCatalog = 'department'">科室</button>
            <button type="button" :class="{ active: activeCatalog === 'doctor' }" @click="activeCatalog = 'doctor'">医生</button>
            <button type="button" :class="{ active: activeCatalog === 'drug' }" @click="activeCatalog = 'drug'">药品</button>
            <button type="button" :class="{ active: activeCatalog === 'knowledge' }" @click="activeCatalog = 'knowledge'">知识库</button>
            <button type="button" :class="{ active: activeCatalog === 'prompt' }" @click="activeCatalog = 'prompt'">Prompt</button>
            <button type="button" :class="{ active: activeCatalog === 'dict' }" @click="activeCatalog = 'dict'">字典</button>
          </div>

          <div v-show="activeCatalog === 'department'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="saveDepartment">
              <h3>{{ departmentForm.id ? "编辑科室" : "新增科室" }}</h3>
              <div class="form-grid">
                <label>编码<input v-model.trim="departmentForm.code" /></label>
                <label>名称<input v-model.trim="departmentForm.name" /></label>
              </div>
              <label>说明<textarea v-model.trim="departmentForm.description" rows="3" /></label>
              <div class="toolbar">
                <button class="primary" type="submit" :disabled="loading.save">保存科室</button>
                <button type="button" @click="resetDepartment">清空</button>
              </div>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>ID</th><th>编码</th><th>名称</th><th>说明</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in departments" :key="String(item.id)">
                    <td>{{ text(item, "id") }}</td><td>{{ text(item, "code") }}</td><td>{{ text(item, "name") }}</td><td>{{ text(item, "description") }}</td>
                    <td><button type="button" @click="editDepartment(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!departments.length" class="empty-state">暂无科室数据。</div>
            </div>
          </div>

          <div v-show="activeCatalog === 'doctor'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="saveDoctor">
              <h3>{{ doctorForm.id ? "编辑医生" : "新增医生" }}</h3>
              <div class="form-grid three">
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
              <label>专长<textarea v-model.trim="doctorForm.specialty" rows="3" /></label>
              <button class="primary" type="submit" :disabled="loading.save">保存医生</button>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>医生</th><th>科室</th><th>职称</th><th>专长</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in doctors" :key="String(item.id)">
                    <td>{{ text(item, "name") }}</td><td>{{ text(item, "departmentName") }}</td><td>{{ text(item, "title") }}</td><td>{{ text(item, "specialty") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editDoctor(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!doctors.length" class="empty-state">暂无医生数据。</div>
            </div>
          </div>

          <div v-show="activeCatalog === 'drug'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="saveDrug">
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
              <label>禁忌<textarea v-model.trim="drugForm.contraindication" rows="3" /></label>
              <label>相互作用规则<textarea v-model.trim="drugForm.interactionRule" rows="3" /></label>
              <button class="primary" type="submit" :disabled="loading.save">保存药品</button>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>药品</th><th>规格</th><th>禁忌</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in drugs" :key="String(item.id)">
                    <td>{{ text(item, "name") }}</td><td>{{ text(item, "specification") }}</td><td>{{ text(item, "contraindication") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td class="table-actions"><button type="button" @click="editDrug(item)">编辑</button><button class="danger" type="button" @click="disableDrug(item)">停用</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!drugs.length" class="empty-state">暂无药品数据。</div>
            </div>
          </div>

          <div v-show="activeCatalog === 'knowledge'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="saveKnowledge">
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
              <label>建议<textarea v-model.trim="knowledgeForm.advice" rows="3" /></label>
              <button class="primary" type="submit" :disabled="loading.save">保存知识库</button>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>标题</th><th>科室</th><th>症状</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in knowledge" :key="String(item.id)">
                    <td>{{ text(item, "title") }}</td><td>{{ text(item, "departmentCode") }}</td><td>{{ text(item, "symptoms") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editKnowledge(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!knowledge.length" class="empty-state">暂无知识库数据。</div>
            </div>
          </div>

          <div v-show="activeCatalog === 'prompt'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="savePrompt">
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
              <label>模板内容<textarea v-model.trim="promptForm.templateContent" rows="5" /></label>
              <button class="primary" type="submit" :disabled="loading.save">保存 Prompt</button>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>任务</th><th>模板</th><th>科室</th><th>版本</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in prompts" :key="String(item.id)">
                    <td>{{ text(item, "taskType") }}</td><td>{{ text(item, "templateName") }}</td><td>{{ text(item, "departmentCode") }}</td><td>{{ text(item, "version") }}</td>
                    <td><span class="tag" :class="item.enabled ? 'success' : 'danger'">{{ item.enabled ? "启用" : "停用" }}</span></td>
                    <td><button type="button" @click="editPrompt(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!prompts.length" class="empty-state">暂无 Prompt 数据。</div>
            </div>
          </div>

          <div v-show="activeCatalog === 'dict'" class="catalog-grid">
            <form class="sub-panel" @submit.prevent="saveDict">
              <h3>{{ dictForm.id ? "编辑字典" : "新增字典" }}</h3>
              <div class="form-grid three">
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
              <button class="primary" type="submit" :disabled="loading.save">保存字典</button>
            </form>
            <div class="table-scroll">
              <table>
                <thead><tr><th>类型</th><th>键</th><th>值</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="item in dicts" :key="String(item.id)">
                    <td>{{ text(item, "dictType") }}</td><td>{{ text(item, "dictKey") }}</td><td>{{ text(item, "dictValue") }}</td><td>{{ text(item, "sort") }}</td>
                    <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                    <td><button type="button" @click="editDict(item)">编辑</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!dicts.length" class="empty-state">暂无字典数据。</div>
            </div>
          </div>
        </section>

        <section id="schedule" class="panel">
          <div class="panel-header">
            <div>
              <h2>排班管理</h2>
              <p>生成的是后端规则建议，发布后由医生服务生成排班和号源。</p>
            </div>
            <span class="tag warning">发布前需人工审核</span>
          </div>
          <div class="form-grid three">
            <label>开始日期<input v-model="scheduleForm.startDate" type="date" /></label>
            <label>天数<input v-model.number="scheduleForm.days" type="number" min="1" max="14" /></label>
            <div class="actions">
              <button type="button" :disabled="loading.schedule" @click="generateSchedule">生成建议</button>
              <button class="primary" type="button" :disabled="loading.schedule || !suggestions.length" @click="publishSchedule">发布号源</button>
            </div>
          </div>
          <div class="schedule-grid">
            <div>
              <h3>待发布建议</h3>
              <p v-for="item in suggestions" :key="String(item.id)" @click="showScheduleSuggestion(item.id)">
                #{{ text(item, "id") }} {{ text(item, "workDate") }} {{ text(item, "timeRange") }} {{ text(item, "doctorName") }} 容量 {{ text(item, "capacity") }}
              </p>
              <div v-if="!suggestions.length" class="empty-state">暂无待发布建议。</div>
            </div>
            <div>
              <h3>建议详情</h3>
              <p v-if="selectedScheduleSuggestion">
                {{ selectedScheduleSuggestion.doctorName }} / {{ selectedScheduleSuggestion.departmentName }} / {{ selectedScheduleSuggestion.reason }}
              </p>
              <div v-else class="empty-state">请选择一条建议查看详情。</div>
            </div>
            <div>
              <h3>已发布排班</h3>
              <p v-for="item in schedules" :key="String(item.id)">
                #{{ text(item, "id") }} {{ text(item, "workDate") }} {{ text(item, "timeRange") }} {{ text(item, "doctorName") }} 容量 {{ text(item, "capacity") }}
              </p>
              <div v-if="!schedules.length" class="empty-state">暂无已发布排班。</div>
            </div>
          </div>
        </section>

        <section id="triage" class="panel">
          <div class="panel-header">
            <div>
              <h2>分诊工作台</h2>
              <p>支持按状态、科室和关键词筛选，高风险或需人工处理记录会突出显示。</p>
            </div>
          </div>
          <div class="form-grid three">
            <label>状态筛选
              <select v-model="triageFilter.risk">
                <option value="">全部</option>
                <option value="MANUAL_REQUIRED">需人工处理</option>
                <option value="AI_RECOMMENDED">AI 已推荐</option>
                <option value="CLOSED">已关闭</option>
              </select>
            </label>
            <label>推荐科室<input v-model.trim="triageFilter.department" /></label>
            <label>关键词<input v-model.trim="triageFilter.keyword" /></label>
          </div>
          <div class="form-grid three">
            <label>分诊记录 ID<input v-model.number="assignForm.triageRecordId" type="number" /></label>
            <label>分配医生
              <select v-model.number="assignForm.doctorId">
                <option :value="0">请选择</option>
                <option v-for="doctor in doctors" :key="String(doctor.id)" :value="toNumber(doctor.id)">{{ doctor.name }} / {{ doctor.departmentName }}</option>
              </select>
            </label>
            <div class="actions"><button class="primary" type="button" :disabled="loading.triage" @click="assignTriage">分配医生</button></div>
          </div>
          <div v-if="filteredTriage.length" class="table-scroll">
            <table>
              <thead><tr><th>记录</th><th>主诉</th><th>推荐科室</th><th>医生</th><th>状态</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="item in filteredTriage" :key="String(item.triageRecordId)" :class="{ dangerRow: ['MANUAL_REQUIRED', 'HIGH'].includes(text(item, 'status')) }" @click="useTriage(item)">
                  <td>#{{ text(item, "triageRecordId") }}</td><td>{{ text(item, "chiefComplaint") }}</td><td>{{ text(item, "recommendedDepartment") }}</td><td>{{ text(item, "assignedDoctorName", "未分配") }}</td>
                  <td><span class="tag" :class="statusClass(item.status)">{{ text(item, "status") }}</span></td>
                  <td class="table-actions"><button type="button" @click.stop="showTriage(item)">详情</button><button class="danger" type="button" @click.stop="closeTriage(item)">关闭</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">当前筛选条件下暂无分诊记录。</div>
          <div v-if="selectedTriage" class="notice">
            <strong>分诊详情 #{{ selectedTriage.triageRecordId }}</strong>
            <span>{{ selectedTriage.chiefComplaint }} / {{ selectedTriage.reason }}</span>
          </div>
        </section>

        <section id="search" class="panel">
          <div class="panel-header">
            <div>
              <h2>知识、药品和 Prompt 检索</h2>
              <p>统一使用后端公开检索接口。</p>
            </div>
          </div>
          <div class="form-grid three">
            <label>关键词<input v-model.trim="search.q" /></label>
            <label>科室编码<input v-model.trim="search.departmentCode" /></label>
            <div class="actions"><button class="primary" type="button" :disabled="loading.search" @click="doSearch">检索</button></div>
          </div>
          <div v-if="searchCount" class="search-grid">
            <div>
              <h3>知识库</h3>
              <p v-for="item in searchResults.knowledge" :key="`k-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "title") }}</p>
            </div>
            <div>
              <h3>药品</h3>
              <p v-for="item in searchResults.drugs" :key="`d-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "name") }}</p>
            </div>
            <div>
              <h3>Prompt</h3>
              <p v-for="item in searchResults.prompts" :key="`p-${String(item.id)}`">#{{ text(item, "id") }} {{ text(item, "templateName") }}</p>
            </div>
          </div>
          <div v-else class="empty-state">请输入关键词后检索。</div>
        </section>
      </template>
    </section>
  </main>
</template>
