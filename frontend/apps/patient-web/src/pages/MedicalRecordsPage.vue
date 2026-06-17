<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, useAuthStore, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState } from "@smart-cloud-brain/shared-ui";
import MedicalRecordDetailModal from "../components/MedicalRecordDetailModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { records } = storeToRefs(workflow);
const loading = ref(false);
const detailLoading = ref(false);
const error = ref("");
const selected = ref<DataRow | null>(null);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "病历列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function open(item: DataRow) {
  detailLoading.value = true;
  error.value = "";
  try {
    selected.value = await api.medicalRecordDetail(auth.token(), Number(item.medicalRecordId));
  } catch (err) {
    error.value = formatApiError(err, "病历详情加载失败");
  } finally {
    detailLoading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">MEDICAL RECORDS</p><h2>病历列表</h2><p>医生保存后同步到患者端。</p></div><button type="button" @click="refresh">刷新</button></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading || detailLoading" />
      <div v-else-if="records.length" class="table-scroll">
        <table class="data-table">
          <thead><tr><th>病历号</th><th>主诉</th><th>诊断</th><th>方式</th><th class="actions-cell">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in records" :key="String(item.medicalRecordId)">
              <td>#{{ fieldText(item, "medicalRecordId") }}</td>
              <td>{{ fieldText(item, "chiefComplaint") }}</td>
              <td>{{ fieldText(item, "diagnosis") }}</td>
              <td>{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</td>
              <td><button type="button" @click="open(item)">详情</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无病历" message="医生保存病历后会显示在这里。" />
    </div>
    <MedicalRecordDetailModal :open="Boolean(selected)" :record="selected" @close="selected = null" />
  </section>
</template>
