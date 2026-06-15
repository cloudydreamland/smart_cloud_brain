<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api, type Session } from "@smart-cloud-brain/shared-api";

const session = ref<Session | null>(JSON.parse(localStorage.getItem("admin-session") || "null"));
const message = ref("");
const busy = ref(false);
const loginForm = ref({ account: "admin", password: "123456" });

const departments = ref<Array<Record<string, unknown>>>([]);
const doctors = ref<Array<Record<string, unknown>>>([]);
const drugs = ref<Array<Record<string, unknown>>>([]);
const prompts = ref<Array<Record<string, unknown>>>([]);
const knowledge = ref<Array<Record<string, unknown>>>([]);

const departmentForm = ref({ code: "GENERAL", name: "全科门诊", description: "常见轻症初诊与复诊" });
const doctorForm = ref({ name: "李医生", phone: "13900000002", password: "123456", departmentId: 2, title: "主治医师", specialty: "发热、咽痛、腹泻、皮肤过敏", status: "ENABLED" });
const drugForm = ref({ name: "对乙酰氨基酚", specification: "0.5g", contraindication: "严重肝功能不全慎用", interactionRule: "避免与其他含同成分复方药重复使用", status: "ENABLED" });
const promptForm = ref({ taskType: "MEDICAL_RECORD", departmentCode: "GENERAL", templateName: "GENERAL_MEDICAL_RECORD_v1", templateContent: "根据轻症问诊文本生成结构化病历 JSON，必须提示医生确认。", outputSchema: "{\"type\":\"object\"}", version: "v1", enabled: true });
const knowledgeForm = ref({ title: "普通感冒", symptoms: "鼻塞、流涕、咽痛、低热、轻微咳嗽", riskSignals: "持续高热、呼吸困难、胸痛、意识异常", advice: "注意休息和补液，症状加重或超过三天建议线下就诊。", departmentCode: "GENERAL", status: "ENABLED" });

const search = ref({ q: "感冒", departmentCode: "" });
const searchResults = ref({
  knowledge: [] as Array<Record<string, unknown>>,
  drugs: [] as Array<Record<string, unknown>>,
  prompts: [] as Array<Record<string, unknown>>,
});

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
    session.value = await api.loginAdmin(loginForm.value.account, loginForm.value.password);
    localStorage.setItem("admin-session", JSON.stringify(session.value));
    await refresh();
  });
}

async function refresh() {
  if (!session.value) return;
  departments.value = await api.adminDepartments(token());
  doctors.value = await api.doctors();
  drugs.value = await api.drugs(token());
  prompts.value = await api.prompts(token());
  knowledge.value = await api.knowledgeEntries(token());
}

async function saveDepartment() {
  await run("保存科室", async () => {
    await api.saveDepartment(token(), departmentForm.value);
    await refresh();
  });
}

async function saveDoctor() {
  await run("保存医生", async () => {
    await api.saveDoctor(token(), doctorForm.value);
    await refresh();
  });
}

async function saveDrug() {
  await run("保存药品", async () => {
    await api.saveDrug(token(), drugForm.value);
    await refresh();
  });
}

async function savePrompt() {
  await run("保存 Prompt", async () => {
    await api.savePrompt(token(), promptForm.value);
    await refresh();
  });
}

async function saveKnowledge() {
  await run("保存知识库", async () => {
    await api.saveKnowledgeEntry(token(), knowledgeForm.value);
    await refresh();
  });
}

async function doSearch() {
  await run("搜索", async () => {
    const [knowledgeList, drugList, promptList] = await Promise.all([
      api.searchKnowledge(token(), search.value.q, search.value.departmentCode),
      api.searchDrugs(token(), search.value.q),
      api.searchPrompts(token(), search.value.q),
    ]);
    searchResults.value = { knowledge: knowledgeList, drugs: drugList, prompts: promptList };
  });
}

function logout() {
  session.value = null;
  localStorage.removeItem("admin-session");
}

onMounted(refresh);
</script>

<template>
  <main class="app-shell">
    <aside class="rail">
      <p class="eyebrow">Admin Web</p>
      <h1>管理端</h1>
      <p>维护真实科室、医生、药品、Prompt 和轻症知识库数据，搜索结果直接来自金仓数据库。</p>
      <div class="session" v-if="session">
        <strong>{{ session.name }}</strong>
        <span>{{ session.role }} #{{ session.userId }}</span>
        <button @click="logout">退出</button>
      </div>
    </aside>

    <section class="workspace">
      <div class="notice" v-if="message">{{ message }}</div>

      <section class="panel" v-if="!session">
        <h2>管理员登录</h2>
        <div class="grid two">
          <label>账号<input v-model="loginForm.account" /></label>
          <label>密码<input v-model="loginForm.password" type="password" /></label>
        </div>
        <button class="primary" :disabled="busy" @click="login">进入管理端</button>
      </section>

      <template v-else>
        <section class="panel">
          <h2>科室与医生</h2>
          <div class="grid three">
            <label>科室编码<input v-model="departmentForm.code" /></label>
            <label>科室名称<input v-model="departmentForm.name" /></label>
            <label>说明<input v-model="departmentForm.description" /></label>
          </div>
          <button @click="saveDepartment">保存科室</button>
          <div class="grid three">
            <label>医生姓名<input v-model="doctorForm.name" /></label>
            <label>手机号<input v-model="doctorForm.phone" /></label>
            <label>科室 ID<input v-model.number="doctorForm.departmentId" type="number" /></label>
            <label>职称<input v-model="doctorForm.title" /></label>
            <label>专长<input v-model="doctorForm.specialty" /></label>
            <label>密码<input v-model="doctorForm.password" /></label>
          </div>
          <button class="primary" @click="saveDoctor">保存医生</button>
        </section>

        <section class="panel">
          <h2>药品基础信息</h2>
          <div class="grid three">
            <label>药品名称<input v-model="drugForm.name" /></label>
            <label>规格<input v-model="drugForm.specification" /></label>
            <label>状态<input v-model="drugForm.status" /></label>
            <label>禁忌<textarea v-model="drugForm.contraindication" rows="2" /></label>
            <label>相互作用<textarea v-model="drugForm.interactionRule" rows="2" /></label>
          </div>
          <button class="primary" @click="saveDrug">保存药品</button>
        </section>

        <section class="panel">
          <h2>Prompt 与知识库</h2>
          <div class="grid two">
            <label>任务类型<input v-model="promptForm.taskType" /></label>
            <label>科室编码<input v-model="promptForm.departmentCode" /></label>
            <label>模板名称<input v-model="promptForm.templateName" /></label>
            <label>版本<input v-model="promptForm.version" /></label>
          </div>
          <textarea v-model="promptForm.templateContent" rows="3" />
          <button @click="savePrompt">保存 Prompt</button>
          <div class="grid two">
            <label>知识标题<input v-model="knowledgeForm.title" /></label>
            <label>科室编码<input v-model="knowledgeForm.departmentCode" /></label>
            <label>症状<textarea v-model="knowledgeForm.symptoms" rows="2" /></label>
            <label>危险信号<textarea v-model="knowledgeForm.riskSignals" rows="2" /></label>
          </div>
          <textarea v-model="knowledgeForm.advice" rows="3" />
          <button class="primary" @click="saveKnowledge">保存知识库条目</button>
        </section>

        <section class="panel">
          <h2>金仓搜索</h2>
          <div class="grid three">
            <label>关键词<input v-model="search.q" /></label>
            <label>科室编码<input v-model="search.departmentCode" /></label>
            <div class="actions">
              <button class="primary" @click="doSearch">搜索</button>
            </div>
          </div>
          <div class="data-grid">
            <div><h3>知识库</h3><p v-for="item in searchResults.knowledge" :key="String(item.id)">#{{ item.id }} {{ item.title }} | {{ item.departmentCode }}</p></div>
            <div><h3>药品</h3><p v-for="item in searchResults.drugs" :key="String(item.id)">#{{ item.id }} {{ item.name }} | {{ item.specification }}</p></div>
            <div><h3>Prompt</h3><p v-for="item in searchResults.prompts" :key="String(item.id)">#{{ item.id }} {{ item.templateName }}</p></div>
          </div>
        </section>

        <section class="panel">
          <h2>当前基础数据</h2>
          <div class="data-grid">
            <div><h3>科室</h3><p v-for="item in departments" :key="String(item.id)">#{{ item.id }} {{ item.name }}</p></div>
            <div><h3>医生</h3><p v-for="item in doctors" :key="String(item.id)">#{{ item.id }} {{ item.name }} | {{ item.specialty }}</p></div>
            <div><h3>药品</h3><p v-for="item in drugs" :key="String(item.id)">#{{ item.id }} {{ item.name }} | {{ item.specification }}</p></div>
            <div><h3>知识库</h3><p v-for="item in knowledge" :key="String(item.id)">#{{ item.id }} {{ item.title }} | {{ item.departmentCode }}</p></div>
            <div><h3>Prompt</h3><p v-for="item in prompts" :key="String(item.id)">#{{ item.id }} {{ item.templateName }}</p></div>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
