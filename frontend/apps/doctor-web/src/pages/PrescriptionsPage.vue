<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, useAuthStore, useDoctorWorkflowStore, usePagination, type DataRow } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar } from "@smart-cloud-brain/shared-ui";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import { liveRows, statusLabel, statusTone } from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const displayPrescriptions = liveRows(prescriptions);
const loading = ref(false);
const error = ref("");
const selected = ref<DataRow | null>(null);
const { currentPage, pageSize, total, pageRows } = usePagination(displayPrescriptions, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "处方列表加载失败，请稍后重试。");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header>
      <div class="panel-title">
        <p class="eyebrow">处方记录</p>
        <h2>处方审核结果</h2>
        <p>重点突出高风险和待复核处方。</p>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">{{ loading ? "刷新中" : "刷新" }}</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" title="正在同步处方" />
      <div class="table-wrap">
        <table class="record-table">
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
              <td>#RX{{ fieldText(item, "prescriptionId") }}</td>
              <td>{{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</td>
              <td>{{ fieldText(item, "createdAt") }}</td>
              <td>{{ fieldText(item, "drugCount", "2") }}</td>
              <td><span class="tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td><span class="tag" :class="statusTone(item.riskLevel)">{{ statusLabel(item.riskLevel, "未审核") }}</span></td>
              <td style="text-align:right"><button :class="fieldText(item, 'riskLevel').toUpperCase() === 'HIGH' ? 'action-btn danger' : 'action-btn'" type="button" @click="selected = item">{{ fieldText(item, "riskLevel").toUpperCase() === "HIGH" ? "复核" : "详情" }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <PrescriptionRiskModal :open="Boolean(selected)" :result="selected" @close="selected = null" @confirm="selected = null" />
  </section>
</template>
