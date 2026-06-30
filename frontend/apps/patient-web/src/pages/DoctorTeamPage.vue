<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { api, type Doctor, type Department, type PatientRecommendation } from "@smart-cloud-brain/shared-api";

type DoctorCard = {
  id: string;
  name: string;
  title: string;
  department: string;
  specialty: string;
  profile: string;
  tags: string[];
};

const doctors = ref<Doctor[]>([]);
const departments = ref<Department[]>([]);
const recommendedDoctors = ref<PatientRecommendation[]>([]);
const loading = ref(true);
const failed = ref(false);
const query = ref("");
const department = ref("全部科室");
const selected = ref<DoctorCard | null>(null);

const cards = computed<DoctorCard[]>(() => {
  if (recommendedDoctors.value.length) return recommendedDoctors.value.map(recommendationCard);
  if (!doctors.value.length) return [];
  return doctors.value.map((row, index) => ({
    id: String(row.id || index),
    name: row.name || "未命名医生",
    title: row.title || "门诊医生",
    department: row.departmentName || "未定科室",
    specialty: row.specialty || "常见病、慢病复诊和专科评估",
    profile: row.specialty || "提供门诊评估、复诊沟通和检查结果解读服务。",
    tags: splitTags(row.specialty || ""),
  }));
});
const departmentOptions = computed(() => {
  const names = new Set(["全部科室"]);
  departments.value.forEach((row) => names.add(row.name || "未定科室"));
  cards.value.forEach((item) => names.add(item.department));
  return [...names].filter(Boolean);
});
const filteredCards = computed(() => {
  const text = query.value.trim().toLowerCase();
  return cards.value.filter((item) => {
    const byDepartment = department.value === "全部科室" || item.department === department.value;
    const haystack = `${item.name} ${item.title} ${item.department} ${item.specialty} ${item.profile} ${item.tags.join(" ")}`.toLowerCase();
    return byDepartment && (!text || haystack.includes(text));
  });
});

watch(departmentOptions, (options) => {
  if (!options.includes(department.value)) department.value = "全部科室";
});

function splitTags(value: string) {
  return value.split(/[、,，\s]+/).filter(Boolean).slice(0, 4);
}

function recommendationCard(row: PatientRecommendation, index: number): DoctorCard {
  const name = row.title || row.targetName || "未命名医生";
  const specialty = row.description || row.specialty || "常见病、慢病复诊和专科评估";
  return {
    id: String(row.id || row.targetId || `recommendation-${index}`),
    name,
    title: row.doctorTitle || "推荐医生",
    department: row.departmentName || "未定科室",
    specialty,
    profile: row.description || "提供门诊评估、复诊沟通和检查结果解读服务。",
    tags: splitTags(specialty),
  };
}

onMounted(async () => {
  try {
    const [recommendationRows, departmentRows, doctorRows] = await Promise.all([
      api.patientSiteRecommendations("DOCTOR").catch(() => []),
      api.departments(),
      api.doctors(),
    ]);
    recommendedDoctors.value = recommendationRows;
    departments.value = departmentRows;
    doctors.value = doctorRows;
  } catch {
    failed.value = true;
    doctors.value = [];
    departments.value = [];
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
        <span>医生团队</span>
      </nav>
      <p class="resource-label">医生与科室</p>
      <h1>医生团队</h1>
      <p>按科室、专长和关键词查找医生。具体号源、出诊时间和可预约状态以在线挂号页面为准。</p>
    </header>

    <section class="resource-search-page">
      <div class="doctor-team-toolbar">
        <input v-model.trim="query" type="search" placeholder="搜索医生、科室、疾病方向">
        <select v-model="department" aria-label="按科室筛选">
          <option v-for="item in departmentOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <RouterLink :to="{ name: 'patient-doctors' }">在线挂号</RouterLink>
      </div>
      <div class="resource-alert subtle">
        <strong>{{ loading ? "正在同步医生数据" : recommendedDoctors.length ? "数据来自患者端推荐配置" : failed || !doctors.length ? "当前显示默认专家资料" : "医生数据来自后端接口" }}</strong>
        <p>专家介绍用于就诊方向参考。选择医生前，请结合症状、科室范围和实际号源确认。</p>
      </div>

      <div class="doctor-team-grid">
        <article v-for="doctor in filteredCards" :key="doctor.id" class="doctor-team-card">
          <div>
            <p>{{ doctor.department }}</p>
            <h2>{{ doctor.name }}</h2>
            <strong>{{ doctor.title }} · {{ doctor.specialty }}</strong>
            <span>{{ doctor.profile }}</span>
          </div>
          <div>
            <em v-for="tag in doctor.tags" :key="tag">{{ tag }}</em>
          </div>
          <footer>
            <button type="button" @click="selected = doctor">查看详情</button>
            <RouterLink :to="{ name: 'public-search', query: { q: doctor.name } }">搜索资料</RouterLink>
          </footer>
        </article>
      </div>
      <p v-if="!filteredCards.length" class="public-empty">没有匹配医生。可切换科室或减少关键词后再试。</p>

      <Transition name="fade">
      <div v-if="selected" class="public-detail-panel" role="dialog" aria-modal="true">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <p>{{ selected.department }}</p>
        <h2>{{ selected.name }}</h2>
        <strong>{{ selected.title }} · {{ selected.specialty }}</strong>
        <span>{{ selected.profile }}</span>
        <ul>
          <li>建议就诊前整理既往病历、检查报告、影像资料和正在使用的药物。</li>
          <li>不确定是否适合该专科时，可先使用 AI 智能分诊获得推荐方向。</li>
          <li>具体出诊时间、余号和预约状态以在线挂号页面为准。</li>
        </ul>
        <RouterLink :to="{ name: 'patient-doctors' }">查看可预约号源</RouterLink>
      </div>
      </Transition>
    </section>
  </main>
</template>
