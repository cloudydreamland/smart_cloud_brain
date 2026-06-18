<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, useAuthStore, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { DataTable } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { records } = storeToRefs(workflow);
const loading = ref(false);
const error = ref("");

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "病历列表加载失败");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">RECORDS</p><h2>已保存病历</h2><p>快速回看当前医生账号下的病历记录。</p></div>
      <button type="button" :disabled="loading" @click="refresh">刷新</button>
    </header>
    <div class="panel-body">
      <DataTable :rows="records" :loading="loading" :error="error" empty-title="暂无病历" empty-message="保存病历后会显示在这里。">
        <thead><tr><th>病历号</th><th>患者</th><th>主诉</th><th>诊断</th><th>方式</th></tr></thead>
        <tbody>
          <tr v-for="item in records" :key="String(item.medicalRecordId)">
            <td>#{{ fieldText(item, "medicalRecordId") }}</td>
            <td>{{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</td>
            <td>{{ fieldText(item, "chiefComplaint") }}</td>
            <td>{{ fieldText(item, "diagnosis") }}</td>
            <td>{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</td>
          </tr>
        </tbody>
      </DataTable>
    </div>
  </section>
</template>
