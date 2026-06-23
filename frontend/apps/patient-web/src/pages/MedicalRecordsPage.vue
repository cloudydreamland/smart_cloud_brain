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
    error.value = formatApiError(err, "病历记录加载失败");
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
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">我的病历</p>
        <h2>病历记录</h2>
        <p>医生保存后同步到患者服务，便于复诊前回看诊断、主诉和医嘱。</p>
      </div>
      <button type="button" @click="refresh">刷新</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading || detailLoading" />
      <div v-else-if="records.length" class="record-list">
        <article v-for="item in records" :key="String(item.medicalRecordId)" class="record-card">
          <div>
            <span class="record-kicker">病历 #{{ fieldText(item, "medicalRecordId") }}</span>
            <h3>{{ fieldText(item, "diagnosis", "诊断待同步") }}</h3>
            <p>{{ fieldText(item, "chiefComplaint", "暂无主诉记录") }}</p>
            <div class="record-meta">
              <span>{{ item.aiGenerated ? "AI草稿经医生确认" : "医生录入" }}</span>
              <span>{{ fieldText(item, "createdAt", "时间待同步") }}</span>
            </div>
          </div>
          <button type="button" @click="open(item)">查看详情</button>
        </article>
      </div>
      <EmptyState v-else title="暂无病历" message="医生保存病历后会显示在这里。" />
    </div>
    <MedicalRecordDetailModal :open="Boolean(selected)" :record="selected" @close="selected = null" />
  </section>
</template>
