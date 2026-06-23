<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";

const departments = ref<DataRow[]>([]);
const loading = ref(true);
const failed = ref(false);

const fallbackDepartments = [
  { name: "神经内科", description: "头痛、眩晕、脑血管病、认知障碍和周围神经相关问题。" },
  { name: "心血管内科", description: "胸闷、心悸、高血压、冠心病和心功能管理。" },
  { name: "呼吸内科", description: "咳嗽、哮喘、慢阻肺、肺部感染和睡眠呼吸问题。" },
  { name: "消化内科", description: "腹痛、胃肠功能紊乱、肝胆胰疾病和内镜评估。" },
  { name: "骨科", description: "关节疼痛、运动损伤、脊柱问题和骨折术后康复。" },
  { name: "妇科", description: "月经异常、妇科炎症、围绝经期管理和孕前咨询。" },
  { name: "儿科", description: "儿童发热、咳嗽、消化问题、生长发育和疫苗咨询。" },
  { name: "肿瘤科", description: "肿瘤筛查、治疗方案咨询、随访管理和症状支持。" },
  { name: "康复医学科", description: "术后康复、神经康复、疼痛管理和功能训练计划。" },
];

const visibleDepartments = computed(() => {
  if (!departments.value.length) return fallbackDepartments;
  return departments.value.map((row) => ({
    name: String(row.name || row.departmentName || "未命名科室"),
    description: String(row.description || row.remark || row.specialty || "提供常见病、慢病和复诊相关诊疗服务。"),
  }));
});

onMounted(async () => {
  try {
    departments.value = await api.departments();
  } catch {
    failed.value = true;
    departments.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <main class="public-info-page">
    <section class="public-hero">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <p class="home-eyebrow">Departments</p>
      <h1>科室与重点专科</h1>
      <p>优先展示后端科室数据；当接口暂不可用或暂无数据时，显示默认重点专科，方便患者继续理解就诊方向。</p>
      <div class="public-actions">
        <RouterLink class="home-pill outline-blue" :to="{ name: 'patient-triage' }">不确定科室，先做分诊</RouterLink>
        <RouterLink class="home-pill outline-blue" :to="{ name: 'patient-login' }">登录预约</RouterLink>
      </div>
    </section>

    <section class="public-section">
      <div class="public-section-head">
        <h2>{{ loading ? "正在加载科室" : "可预约科室" }}</h2>
        <p v-if="failed">当前未能连接科室接口，以下为默认重点专科。</p>
        <p v-else-if="!departments.length && !loading">后端暂无科室数据，以下为默认重点专科。</p>
        <p v-else>数据来自当前后端科室接口。</p>
      </div>
      <div class="department-list">
        <article v-for="department in visibleDepartments" :key="department.name">
          <div>
            <h3>{{ department.name }}</h3>
            <p>{{ department.description }}</p>
          </div>
          <RouterLink :to="{ name: 'public-search', query: { q: department.name } }">搜索相关医生</RouterLink>
        </article>
      </div>
    </section>
  </main>
</template>
