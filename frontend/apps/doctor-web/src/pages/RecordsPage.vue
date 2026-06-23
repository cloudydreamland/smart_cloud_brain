<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, useAuthStore, useDoctorWorkflowStore, usePagination } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar } from "@smart-cloud-brain/shared-ui";
import { liveRows } from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { records } = storeToRefs(workflow);
const displayRecords = liveRows(records);
const loading = ref(false);
const error = ref("");
const { currentPage, pageSize, total, pageRows } = usePagination(displayRecords, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "病历列表加载失败，请稍后重试。");
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
        <p class="eyebrow">病历记录</p>
        <h2>已保存病历</h2>
        <p>按患者和诊断快速回看。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh">{{ loading ? "刷新中" : "刷新" }}</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" title="正在同步病历" />
      <div class="card-grid">
        <article v-for="item in pageRows" :key="String(item.medicalRecordId)" class="record-card">
          <span class="tag" :class="item.aiGenerated ? 'success' : 'info'">{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</span>
          <strong>#MR{{ fieldText(item, "medicalRecordId") }} · {{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</strong>
          <p>主诉：{{ fieldText(item, "chiefComplaint") }}</p>
          <p>诊断：{{ fieldText(item, "diagnosis") }}</p>
          <p>时间：{{ fieldText(item, "createdAt", "2026-06-22 09:34") }}</p>
          <button type="button">查看详情</button>
        </article>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
  </section>
</template>
