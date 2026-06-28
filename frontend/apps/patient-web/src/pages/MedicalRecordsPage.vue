<script setup lang="ts">
import { onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState } from "@smart-cloud-brain/shared-ui";
import MedicalRecordDetailModal from "../components/MedicalRecordDetailModal.vue";

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
    await workflow.refreshAuthenticated();
  } catch (err) {
    error.value = formatApiError(err, "病历记录加载失败");
  } finally {
    loading.value = false;
  }
}

async function open(item: DataRow) {
  const id = recordId(item);
  if (!id) {
    error.value = "无法识别这条病历记录，请刷新后重试。";
    return;
  }
  detailLoading.value = true;
  error.value = "";
  try {
    selected.value = await api.medicalRecordDetail(id);
  } catch (err) {
    error.value = formatApiError(err, "病历详情加载失败，请稍后重试");
  } finally {
    detailLoading.value = false;
  }
}

function recordId(item: DataRow) {
  const value = Number(item.medicalRecordId ?? item.id);
  return Number.isFinite(value) && value > 0 ? value : 0;
}

onMounted(refresh);
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
        <article v-for="item in records" :key="String(recordId(item) || fieldText(item, 'createdAt'))" class="record-card">
          <div>
            <span class="record-kicker">病历 #{{ fieldText(item, "medicalRecordId", fieldText(item, "id")) }}</span>
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
