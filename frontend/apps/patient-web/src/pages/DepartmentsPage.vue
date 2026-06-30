<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { api, type DataRow, type PatientRecommendation } from "@smart-cloud-brain/shared-api";

type DepartmentResource = {
  name: string;
  description: string;
  symptoms: string[];
  preparation: string[];
};

const departments = ref<DataRow[]>([]);
const recommendedDepartments = ref<PatientRecommendation[]>([]);
const loading = ref(true);
const failed = ref(false);

function normalizeDepartment(row: DataRow | PatientRecommendation): DepartmentResource {
  const name = String(("title" in row && row.title) || ("targetName" in row && row.targetName) || ("name" in row && row.name) || ("departmentName" in row && row.departmentName) || "未命名科室");
  return {
    name,
    description: String(("description" in row && row.description) || ("remark" in row && row.remark) || ("specialty" in row && row.specialty) || "提供门诊、复诊和专科评估服务。"),
    symptoms: ["常见病和慢病复诊", "专科症状评估", "检查结果解读", "治疗方案咨询"],
    preparation: ["准备既往病历和检查报告", "记录主要症状的时间线", "整理长期用药和过敏史"],
  };
}

const visibleDepartments = computed(() => {
  if (recommendedDepartments.value.length) return recommendedDepartments.value.map(normalizeDepartment);
  return departments.value.length ? departments.value.map(normalizeDepartment) : [];
});

onMounted(async () => {
  try {
    const [recommendationRows, departmentRows] = await Promise.all([
      api.patientSiteRecommendations("DEPARTMENT").catch(() => []),
      api.departments(),
    ]);
    recommendedDepartments.value = recommendationRows;
    departments.value = departmentRows;
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
    <header class="resource-header">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>科室与重点专科</span>
      </nav>
      <p class="resource-label">科室资料</p>
      <h1>科室与重点专科</h1>
      <p>优先展示后端科室数据，同时补充常见症状和就诊准备。无后端数据时展示默认重点专科资料。</p>
    </header>

    <div class="resource-layout">
      <aside class="resource-toc" aria-label="科室目录">
        <strong>科室目录</strong>
        <a v-for="department in visibleDepartments" :key="department.name" :href="`#${department.name}`">{{ department.name }}</a>
      </aside>

      <section class="resource-content">
        <div class="resource-alert subtle">
          <strong>{{ loading ? "正在同步科室数据" : recommendedDepartments.length ? "数据来自患者端推荐配置" : failed || !departments.length ? "当前显示默认重点专科" : "数据来自后端科室接口" }}</strong>
          <p v-if="loading">请稍候，正在读取可用科室。</p>
          <p v-else-if="failed">科室接口暂不可用，以下资料用于就诊方向参考。</p>
          <p v-else-if="!departments.length">后端暂无科室数据，以下资料用于就诊方向参考。</p>
          <p v-else>每个科室条目已结合本地资料补充就诊准备信息。</p>
        </div>

        <article v-for="department in visibleDepartments" :id="department.name" :key="department.name" class="resource-block department-resource">
          <div>
            <h2>{{ department.name }}</h2>
            <p>{{ department.description }}</p>
          </div>
          <div class="resource-columns">
            <section>
              <h3>常见就诊问题</h3>
              <ul>
                <li v-for="symptom in department.symptoms" :key="symptom">{{ symptom }}</li>
              </ul>
            </section>
            <section>
              <h3>建议准备</h3>
              <ul>
                <li v-for="item in department.preparation" :key="item">{{ item }}</li>
              </ul>
            </section>
          </div>
          <div class="resource-links">
            <RouterLink :to="{ name: 'public-search', query: { q: department.name } }">搜索相关医生和资料</RouterLink>
            <RouterLink :to="{ name: 'patient-triage' }">不确定科室，先做分诊</RouterLink>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
