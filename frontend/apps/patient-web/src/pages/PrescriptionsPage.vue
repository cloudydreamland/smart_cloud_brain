<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { api, formatApiError, formatDateTime, statusClass, statusText, toNumber, usePagination, usePatientWorkflowStore, type Prescription } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";
import PrescriptionDetailModal from "../components/PrescriptionDetailModal.vue";

const workflow = usePatientWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const loading = ref(false);
const detailLoading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const selected = ref<Prescription | null>(null);
const { currentPage, pageSize, total, pageRows } = usePagination(prescriptions, 8);

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated();
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", "处方数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "处方加载失败，请稍后重试。");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

async function openDetail(item: Prescription) {
  detailLoading.value = true;
  error.value = "";
  try {
    selected.value = await api.prescriptionDetail(toNumber(item.prescriptionId));
  } catch (err) {
    error.value = formatApiError(err, "处方详情加载失败");
  } finally {
    detailLoading.value = false;
  }
}

function subjectText(item: Prescription) {
  const relation = item.subjectRelationship || (item.subjectType === "VISITOR" ? "家属" : "本人");
  return `${item.subjectName || item.patientName || "就诊人"}（${relation}）`;
}

onMounted(() => refresh(true, false));
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">处方记录</p><h2>处方列表</h2><p>查看处方和风险审核结果。</p></div><button type="button" :disabled="loading" @click="refresh()"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg> 刷新</button></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="prescriptions.length" class="list">
        <article v-for="item in pageRows" :key="String(item.prescriptionId)" class="list-row">
          <div class="row-main"><strong>处方 #{{ String(item.prescriptionId) }}</strong><p>{{ subjectText(item) }} · {{ formatDateTime(item.createdAt) }} · {{ statusText(item.status) }}</p></div>
          <div class="toolbar">
            <StatusTag :status="statusText(item.riskLevel, '未审核')" :tone="statusClass(item.riskLevel)" />
            <button type="button" :disabled="detailLoading" @click="openDetail(item)">{{ detailLoading ? '加载中...' : '详情' }}</button>
          </div>
        </article>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无处方" message="医生确认处方后会显示在这里。" />
    </div>
    <PrescriptionDetailModal :open="Boolean(selected)" :prescription="selected" @close="selected = null" />
  </section>
</template>
