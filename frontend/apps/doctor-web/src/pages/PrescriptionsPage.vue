<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, statusClass, useAuthStore, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { DataTable, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const loading = ref(false);
const error = ref("");

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "处方列表加载失败");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">PRESCRIPTIONS</p><h2>处方记录</h2><p>查看已创建处方、处方状态和风险等级。</p></div>
      <button type="button" :disabled="loading" @click="refresh">刷新</button>
    </header>
    <div class="panel-body">
      <DataTable :rows="prescriptions" :loading="loading" :error="error" empty-title="暂无处方" empty-message="创建处方后会显示在这里。">
        <thead><tr><th>处方号</th><th>患者</th><th>创建时间</th><th>状态</th><th>风险</th></tr></thead>
        <tbody>
          <tr v-for="item in prescriptions" :key="String(item.prescriptionId)">
            <td>#{{ fieldText(item, "prescriptionId") }}</td>
            <td>{{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</td>
            <td>{{ fieldText(item, "createdAt") }}</td>
            <td>{{ fieldText(item, "status") }}</td>
            <td><StatusTag :status="fieldText(item, 'riskLevel', '未审核')" :tone="statusClass(item.riskLevel)" /></td>
          </tr>
        </tbody>
      </DataTable>
    </div>
  </section>
</template>
