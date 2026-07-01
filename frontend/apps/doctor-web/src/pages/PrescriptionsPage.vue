<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { displayText, formatApiError, formatDateTime, useDoctorWorkflowStore, usePagination, type Prescription } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar, Toast } from "@smart-cloud-brain/shared-ui";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import { liveRows, statusLabel, statusTone } from "../doctorPresentation";

const workflow = useDoctorWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const displayPrescriptions = liveRows(prescriptions);
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const selected = ref<Prescription | null>(null);
const router = useRouter();
const { currentPage, pageSize, total, pageRows } = usePagination(displayPrescriptions, 8);

function handleReturnToModify() {
  const regId = selected.value?.registrationId;
  selected.value = null;
  if (regId) {
    router.push(`/consult/${regId}`);
  }
}

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", "处方数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "处方加载失败，请稍后重试。");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

onMounted(() => refresh(true, false));
</script>

<template>
  <section class="panel">
    <header>
      <div class="panel-title">
        <p class="eyebrow">处方记录</p>
        <h2>处方审核结果</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading" title="正在同步处方" />
      <div class="table-wrap table-breakout">
        <table class="data-table">
          <thead>
            <tr>
              <th>处方号</th>
              <th>患者</th>
              <th>创建时间</th>
              <th>药品数</th>
              <th>状态</th>
              <th>风险</th>
              <th class="actions-cell">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.prescriptionId)">
              <td>#RX{{ displayText(item.prescriptionId) }}</td>
              <td>{{ displayText(item.patientName, displayText(item.patientId)) }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ displayText(item.drugCount ?? item.items?.length, "2") }}</td>
              <td><span class="tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td><span class="tag" :class="statusTone(item.riskLevel)">{{ statusLabel(item.riskLevel, "未审核") }}</span></td>
              <td class="doctor-prescription-actions"><button :class="displayText(item.riskLevel).toUpperCase() === 'HIGH' ? 'action-btn danger' : 'action-btn'" type="button" @click="selected = item">{{ displayText(item.riskLevel).toUpperCase() === "HIGH" ? "复核" : "详情" }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <PrescriptionRiskModal :open="Boolean(selected)" :result="selected" @close="handleReturnToModify" @confirm="selected = null" />
  </section>
</template>

<style scoped>
.doctor-prescription-actions {
  text-align: right;
}
</style>
