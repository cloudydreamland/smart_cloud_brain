<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, statusText, toNumber, useAuthStore, usePagination, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";
import PrescriptionDetailModal from "../components/PrescriptionDetailModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const loading = ref(false);
const error = ref("");
const selected = ref<DataRow | null>(null);
const { currentPage, pageSize, total, pageRows } = usePagination(prescriptions, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "处方列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function openDetail(item: DataRow) {
  loading.value = true;
  error.value = "";
  try {
    selected.value = await api.prescriptionDetail(auth.token(), toNumber(item.prescriptionId));
  } catch (err) {
    error.value = formatApiError(err, "处方详情加载失败");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">处方记录</p><h2>处方列表</h2><p>查看处方和风险审核结果。</p></div><button type="button" @click="refresh">刷新</button></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" />
      <div v-else-if="prescriptions.length" class="list">
        <article v-for="item in pageRows" :key="String(item.prescriptionId)" class="list-row">
          <div class="row-main"><strong>处方 #{{ fieldText(item, "prescriptionId") }}</strong><p>{{ fieldText(item, "createdAt") }} · {{ statusText(item.status) }}</p></div>
          <div class="toolbar">
            <StatusTag :status="statusText(item.riskLevel, '未审核')" :tone="statusClass(item.riskLevel)" />
            <button type="button" @click="openDetail(item)">详情</button>
          </div>
        </article>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无处方" message="医生确认处方后会显示在这里。" />
    </div>
    <PrescriptionDetailModal :open="Boolean(selected)" :prescription="selected" @close="selected = null" />
  </section>
</template>
