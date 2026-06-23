<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";

type SearchItem = {
  type: string;
  title: string;
  description: string;
  to?: { name: string; query?: Record<string, string> };
};

const route = useRoute();
const router = useRouter();
const keyword = ref(String(route.query.q || ""));
const departments = ref<DataRow[]>([]);
const doctors = ref<DataRow[]>([]);
const loading = ref(true);

const staticItems: SearchItem[] = [
  { type: "服务", title: "智能分诊", description: "描述症状后获得推荐科室和原因。", to: { name: "patient-triage" } },
  { type: "服务", title: "预约就诊", description: "登录后选择医生、日期和时段完成预约。", to: { name: "patient-login" } },
  { type: "服务", title: "患者服务指南", description: "了解就诊前准备、预约流程和诊后资料查看。", to: { name: "public-guide" } },
  { type: "疾病", title: "头痛", description: "可涉及神经内科、眼科、耳鼻喉或急诊评估，需关注突发剧烈头痛和神经功能异常。" },
  { type: "疾病", title: "胸痛", description: "胸痛可能与心血管、呼吸或消化系统相关，持续压榨样胸痛应立即急诊。" },
  { type: "疾病", title: "咳嗽", description: "持续咳嗽、发热、气短或咯血建议呼吸内科评估。" },
  { type: "疾病", title: "腹痛", description: "腹痛伴发热、呕吐、黑便或持续加重时需要尽快就医。" },
  { type: "疾病", title: "高血压", description: "长期血压管理建议心血管内科随访，记录家庭血压趋势。" },
];

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase());

const departmentItems = computed<SearchItem[]>(() =>
  departments.value.map((row) => ({
    type: "科室",
    title: String(row.name || row.departmentName || "未命名科室"),
    description: String(row.description || row.remark || row.specialty || "提供门诊、复诊和专科评估服务。"),
    to: { name: "public-departments", query: { q: String(row.name || row.departmentName || "") } },
  })),
);

const doctorItems = computed<SearchItem[]>(() =>
  doctors.value.map((row) => ({
    type: "医生",
    title: String(row.name || row.doctorName || "未命名医生"),
    description: `${String(row.departmentName || row.department || "未定科室")} · ${String(row.title || row.specialty || "门诊医生")}`,
    to: { name: "patient-login" },
  })),
);

const results = computed(() => {
  const all = [...departmentItems.value, ...doctorItems.value, ...staticItems];
  const q = normalizedKeyword.value;
  if (!q) return all;
  return all.filter((item) => `${item.title} ${item.description} ${item.type}`.toLowerCase().includes(q));
});

function submitSearch() {
  router.replace({ name: "public-search", query: keyword.value.trim() ? { q: keyword.value.trim() } : {} });
}

watch(
  () => route.query.q,
  (value) => {
    keyword.value = String(value || "");
  },
);

onMounted(async () => {
  try {
    const [departmentRows, doctorRows] = await Promise.all([api.departments(), api.doctors()]);
    departments.value = departmentRows;
    doctors.value = doctorRows;
  } catch {
    departments.value = [];
    doctors.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <main class="public-info-page">
    <section class="public-hero compact">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <p class="home-eyebrow">Search</p>
      <h1>搜索页</h1>
      <p>可搜索科室、医生，以及常见疾病和患者服务条目。搜索词会同步到地址栏 q 参数，便于分享或返回。</p>
      <form class="public-search-form" @submit.prevent="submitSearch">
        <span class="search-symbol" aria-hidden="true"></span>
        <input v-model="keyword" type="search" placeholder="搜索科室、医生、症状或服务" autofocus>
        <button type="submit">搜索</button>
      </form>
    </section>

    <section class="public-section">
      <div class="public-section-head">
        <h2>{{ keyword.trim() ? `“${keyword.trim()}” 的搜索结果` : "推荐搜索内容" }}</h2>
        <p v-if="loading">正在同步后端科室与医生数据。</p>
        <p v-else>共 {{ results.length }} 条结果。</p>
      </div>
      <div class="search-results">
        <article v-for="item in results" :key="`${item.type}-${item.title}`">
          <span>{{ item.type }}</span>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
          <RouterLink v-if="item.to" :to="item.to">打开</RouterLink>
        </article>
        <p v-if="!loading && !results.length" class="public-empty">没有找到匹配结果。可以换一个症状、科室或医生姓名重新搜索。</p>
      </div>
    </section>
  </main>
</template>
