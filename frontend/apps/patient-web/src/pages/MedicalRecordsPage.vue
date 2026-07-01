<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { api, formatApiError, formatDateTime, usePatientWorkflowStore, type MedicalRecord } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, Toast } from "@smart-cloud-brain/shared-ui";
import MedicalRecordDetailModal from "../components/MedicalRecordDetailModal.vue";

const workflow = usePatientWorkflowStore();
const { records } = storeToRefs(workflow);
const loading = ref(false);
const loaded = ref(false);
const detailLoading = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const selected = ref<MedicalRecord | null>(null);

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated();
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

async function open(item: MedicalRecord) {
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

function recordId(item: MedicalRecord) {
  const value = Number(item.medicalRecordId);
  return Number.isFinite(value) && value > 0 ? value : 0;
}

</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">我的病历</p>
        <h2>病历记录</h2>
        <p>医生保存后同步到患者服务，便于复诊前回看诊断、主诉和医嘱。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="records.length" class="record-list">
        <article v-for="item in records" :key="String(recordId(item) || item.createdAt || '')" class="record-card">
          <div>
            <span class="record-kicker">病历 #{{ String(item.medicalRecordId) }}</span>
            <h3>{{ item.diagnosis || "诊断待同步" }}</h3>
            <p>{{ item.chiefComplaint || "暂无主诉记录" }}</p>
            <div class="record-meta">
              <span>{{ item.aiGenerated ? "AI草稿经医生确认" : "医生录入" }}</span>
              <span>{{ formatDateTime(item.createdAt, "时间待同步") }}</span>
            </div>
          </div>
          <button type="button" :disabled="detailLoading" @click="open(item)">{{ detailLoading ? '加载中...' : '查看详情' }}</button>
        </article>
      </div>
      <EmptyState v-else title="暂无病历" message="医生保存病历后会显示在这里。" />
    </div>
    <MedicalRecordDetailModal :open="Boolean(selected)" :record="selected" @close="selected = null" />
  </section>
</template>
