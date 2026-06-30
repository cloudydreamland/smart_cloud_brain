<script setup lang="ts">
import { computed, inject, onMounted, ref, watch, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { displayText, formatApiError, formatDateTime, useDoctorWorkflowStore, usePagination, type MedicalRecord } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, Modal, PaginationBar, Toast } from "@smart-cloud-brain/shared-ui";
import { liveRows } from "../doctorPresentation";

const workflow = useDoctorWorkflowStore();
const { records } = storeToRefs(workflow);
const displayRecords = liveRows(records);
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const viewMode = ref<"grid" | "list">("list");
const keyword = ref("");
const sourceFilter = ref<"all" | "ai" | "manual">("all");
const selectedRecord = ref<MedicalRecord | null>(null);
const filteredRecords = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  return displayRecords.value.filter((item) => {
    const sourceMatched =
      sourceFilter.value === "all"
      || (sourceFilter.value === "ai" && item.aiGenerated)
      || (sourceFilter.value === "manual" && !item.aiGenerated);
    const searchable = [
      `MR${displayText(item.medicalRecordId, "")}`,
      displayText(item.medicalRecordId, ""),
      displayText(item.patientName, ""),
      displayText(item.patientId, ""),
      displayText(item.chiefComplaint, ""),
      displayText(item.diagnosis, ""),
    ].join(" ").toLowerCase();
    return sourceMatched && (!q || searchable.includes(q));
  });
});
const recordStats = computed(() => {
  const totalRecords = displayRecords.value.length;
  const aiRecords = displayRecords.value.filter((item) => item.aiGenerated).length;
  return [
    { label: "总病历", value: totalRecords },
    { label: "AI 草稿确认", value: aiRecords },
    { label: "医生录入", value: totalRecords - aiRecords },
    { label: "当前结果", value: filteredRecords.value.length },
  ];
});
const sourceOptions = [
  { value: "all", label: "全部" },
  { value: "ai", label: "AI 草稿确认" },
  { value: "manual", label: "医生录入" },
] as const;
const { currentPage, pageSize, total, pageRows } = usePagination(filteredRecords, 8);

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", "病历数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "病历加载失败，请稍后重试。");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

onMounted(() => refresh(true, false));
watch([keyword, sourceFilter], () => {
  currentPage.value = 1;
});
</script>

<template>
  <section class="panel records-workbench">
    <header class="records-hero">
      <div class="panel-title">
        <p class="eyebrow">病历记录</p>
        <h2>已保存病历</h2>
      </div>
      <div class="doctor-records-toolbar">
        <div class="view-segmented" role="tablist">
          <button
            class="seg-btn"
            :class="{ active: viewMode === 'grid' }"
            title="宫格视图"
            @click="viewMode = 'grid'"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          </button>
          <button
            class="seg-btn"
            :class="{ active: viewMode === 'list' }"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>
          </button>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="refresh()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
      </div>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading" title="正在同步病历" />
      <div class="records-summary" aria-label="病历统计">
        <div v-for="item in recordStats" :key="item.label" class="records-summary-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div class="records-filter-bar">
        <label class="records-search">
          <span>搜索病历</span>
          <div class="records-search-box">
            <svg aria-hidden="true" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.35-4.35" />
            </svg>
            <input v-model.trim="keyword" type="search" placeholder="患者、病历号、主诉或诊断" />
            <button v-if="keyword" type="button" aria-label="清空搜索" @click="keyword = ''">
              <svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </div>
        </label>
        <div class="records-source-filter" aria-label="病历来源筛选">
          <button
            v-for="option in sourceOptions"
            :key="option.value"
            type="button"
            :class="{ active: sourceFilter === option.value }"
            @click="sourceFilter = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
      <div class="card-grid" :class="{ 'list-view': viewMode === 'list' }">
        <article v-for="item in pageRows" :key="String(item.medicalRecordId)" class="record-card">
          <template v-if="viewMode === 'grid'">
            <span class="tag" :class="item.aiGenerated ? 'success' : 'info'">{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</span>
            <strong>#MR{{ displayText(item.medicalRecordId) }} · {{ displayText(item.patientName, displayText(item.patientId)) }}</strong>
            <p>主诉：{{ displayText(item.chiefComplaint) }}</p>
            <p>诊断：{{ displayText(item.diagnosis) }}</p>
            <p>时间：{{ formatDateTime(item.createdAt) }}</p>
            <button class="detail-btn" type="button" @click="selectedRecord = item">查看详情</button>
          </template>
          <template v-else>
            <div class="list-left">
              <div class="list-title-row">
                <strong>#MR{{ displayText(item.medicalRecordId) }} · {{ displayText(item.patientName, displayText(item.patientId)) }}</strong>
              </div>
              <p><b>主诉</b>{{ displayText(item.chiefComplaint) }}</p>
              <p><b>诊断</b>{{ displayText(item.diagnosis) }}</p>
            </div>
            <div class="list-meta">
              <span class="tag" :class="item.aiGenerated ? 'success' : 'info'">{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</span>
              <time>{{ formatDateTime(item.createdAt) }}</time>
            </div>
            <div class="list-detail">
              <button class="detail-btn" type="button" @click="selectedRecord = item">查看详情</button>
            </div>
          </template>
        </article>
      </div>
      <div v-if="!loading && !pageRows.length" class="records-empty-inline">
        暂无匹配病历
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <Modal :open="Boolean(selectedRecord)" title="病历详情" @close="selectedRecord = null">
      <div v-if="selectedRecord" class="doctor-record-detail-stack">
        <div><strong>病历号：</strong>#MR{{ displayText(selectedRecord.medicalRecordId) }}</div>
        <div><strong>患者：</strong>{{ displayText(selectedRecord.patientName, displayText(selectedRecord.patientId)) }}</div>
        <div><strong>类型：</strong>{{ selectedRecord.aiGenerated ? "AI 草稿确认" : "医生录入" }}</div>
        <div><strong>主诉：</strong>{{ displayText(selectedRecord.chiefComplaint) }}</div>
        <div><strong>诊断：</strong>{{ displayText(selectedRecord.diagnosis) }}</div>
        <div><strong>时间：</strong>{{ formatDateTime(selectedRecord.createdAt) }}</div>
      </div>
      <template #footer>
        <button class="btn" @click="selectedRecord = null">关闭</button>
      </template>
    </Modal>
  </section>
</template>

<style scoped>
.doctor-records-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.doctor-record-detail-stack {
  display: grid;
  gap: var(--space-2);
}
</style>
