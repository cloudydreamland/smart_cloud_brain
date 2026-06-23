<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";

type SearchItem = {
  type: "科室" | "医生" | "疾病/症状" | "服务资料";
  title: string;
  description: string;
  meta: string[];
  to?: { name: string; query?: Record<string, string> };
};

const route = useRoute();
const router = useRouter();
const keyword = ref(String(route.query.q || ""));
const departments = ref<DataRow[]>([]);
const doctors = ref<DataRow[]>([]);
const loading = ref(true);

const staticItems: SearchItem[] = [
  {
    type: "服务资料",
    title: "患者服务指南",
    description: "查看就诊前准备、智能分诊、预约挂号、到院流程和诊后资料管理。",
    meta: ["就诊流程", "资料准备", "复诊管理"],
    to: { name: "public-guide" },
  },
  {
    type: "服务资料",
    title: "智能分诊",
    description: "当你不确定挂哪个科室时，先描述症状，系统会给出推荐科室和原因。",
    meta: ["症状整理", "推荐科室", "需登录"],
    to: { name: "patient-triage" },
  },
  {
    type: "服务资料",
    title: "预约就诊",
    description: "登录后查看号源，选择医生、日期、时段和就诊人，完成挂号确认。",
    meta: ["号源", "医生排班", "需登录"],
    to: { name: "patient-login" },
  },
  {
    type: "疾病/症状",
    title: "头痛",
    description: "可能涉及神经内科、眼科、耳鼻喉或急诊。突发剧烈头痛、肢体无力、说话不清需立即就医。",
    meta: ["神经内科", "红旗症状", "影像资料"],
    to: { name: "public-conditions" },
  },
  {
    type: "疾病/症状",
    title: "胸痛",
    description: "可与心血管、呼吸、消化系统有关。压榨样胸痛、出汗、气短或晕厥应立即急诊。",
    meta: ["心血管内科", "急诊提醒", "心电图"],
    to: { name: "public-conditions" },
  },
  {
    type: "疾病/症状",
    title: "咳嗽",
    description: "持续咳嗽、发热、气短、咯血或胸部影像异常，建议呼吸内科评估。",
    meta: ["呼吸内科", "胸部 CT", "用药史"],
    to: { name: "public-conditions" },
  },
  {
    type: "疾病/症状",
    title: "腹痛",
    description: "记录腹痛部位、持续时间、进食关系和排便变化；黑便、呕血或持续加重需尽快就医。",
    meta: ["消化内科", "腹部超声", "胃肠镜"],
    to: { name: "public-conditions" },
  },
  {
    type: "疾病/症状",
    title: "高血压",
    description: "长期管理建议记录家庭血压趋势、用药调整和不良反应，复诊时带完整数据。",
    meta: ["心血管内科", "慢病复诊", "家庭血压"],
    to: { name: "public-conditions" },
  },
];

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase());

const departmentItems = computed<SearchItem[]>(() =>
  departments.value.map((row) => {
    const title = String(row.name || row.departmentName || "未命名科室");
    return {
      type: "科室",
      title,
      description: String(row.description || row.remark || row.specialty || "提供门诊、复诊和专科评估服务。"),
      meta: ["科室资料", "常见症状", "就诊准备"],
      to: { name: "public-departments", query: { q: title } },
    };
  }),
);

const doctorItems = computed<SearchItem[]>(() =>
  doctors.value.map((row) => ({
    type: "医生",
    title: String(row.name || row.doctorName || "未命名医生"),
    description: `${String(row.departmentName || row.department || "未定科室")} · ${String(row.title || "门诊医生")}。登录后可查看可预约号源。`,
    meta: [String(row.departmentName || row.department || "科室待定"), String(row.specialty || "门诊服务"), "需登录预约"],
    to: { name: "patient-login" },
  })),
);

const allItems = computed(() => [...departmentItems.value, ...doctorItems.value, ...staticItems]);

const results = computed(() => {
  const q = normalizedKeyword.value;
  if (!q) return allItems.value;
  return allItems.value.filter((item) => `${item.title} ${item.description} ${item.type} ${item.meta.join(" ")}`.toLowerCase().includes(q));
});

const groupedResults = computed(() => {
  const groups: Record<SearchItem["type"], SearchItem[]> = {
    科室: [],
    医生: [],
    "疾病/症状": [],
    服务资料: [],
  };
  results.value.forEach((item) => groups[item.type].push(item));
  return Object.entries(groups)
    .map(([type, items]) => ({ type, items }))
    .filter((group) => group.items.length);
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
    <header class="resource-header">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>搜索</span>
      </nav>
      <p class="resource-label">资料检索</p>
      <h1>搜索</h1>
      <p>搜索科室、医生、疾病症状和服务资料。地址栏会保留 q 参数，便于返回同一次查询。</p>
      <form class="resource-search-form" @submit.prevent="submitSearch">
        <span class="search-symbol" aria-hidden="true"></span>
        <input v-model="keyword" type="search" placeholder="搜索科室、医生、症状或服务" autofocus>
        <button type="submit">搜索</button>
      </form>
    </header>

    <section class="resource-search-page">
      <div class="resource-section-head">
        <h2>{{ keyword.trim() ? `“${keyword.trim()}” 的搜索结果` : "推荐资料" }}</h2>
        <p v-if="loading">正在同步后端科室与医生数据。</p>
        <p v-else>共 {{ results.length }} 条结果。</p>
      </div>

      <div v-if="groupedResults.length" class="search-group-list">
        <section v-for="group in groupedResults" :key="group.type" class="search-group">
          <h3>{{ group.type }}</h3>
          <article v-for="item in group.items" :key="`${item.type}-${item.title}`" class="search-result-card">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
              <div>
                <span v-for="tag in item.meta" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <RouterLink v-if="item.to" :to="item.to">打开</RouterLink>
          </article>
        </section>
      </div>

      <p v-else-if="!loading" class="public-empty">没有找到匹配结果。可以换一个症状、科室或医生姓名重新搜索。</p>
    </section>
  </main>
</template>
