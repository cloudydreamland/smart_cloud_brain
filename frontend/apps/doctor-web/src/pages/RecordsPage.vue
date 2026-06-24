<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, useAuthStore, useDoctorWorkflowStore, usePagination } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, Modal, PaginationBar } from "@smart-cloud-brain/shared-ui";
import { liveRows } from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { records } = storeToRefs(workflow);
const displayRecords = liveRows(records);
const loading = ref(false);
const error = ref("");
const viewMode = ref<"grid" | "list">("grid");
const selectedRecord = ref<any>(null);
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
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <div class="view-segmented" role="tablist">
          <button
            class="seg-btn"
            :class="{ active: viewMode === 'grid' }"
            title="宫格视图"
            @click="viewMode = 'grid'"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          </button>
          <button
            class="seg-btn"
            :class="{ active: viewMode === 'list' }"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>
          </button>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">{{ loading ? "刷新中" : "刷新" }}</button>
      </div>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" title="正在同步病历" />
      <div class="card-grid" :class="{ 'list-view': viewMode === 'list' }">
        <article v-for="item in pageRows" :key="String(item.medicalRecordId)" class="record-card">
          <template v-if="viewMode === 'grid'">
            <span class="tag" :class="item.aiGenerated ? 'success' : 'info'">{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</span>
            <strong>#MR{{ fieldText(item, "medicalRecordId") }} · {{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</strong>
            <p>主诉：{{ fieldText(item, "chiefComplaint") }}</p>
            <p>诊断：{{ fieldText(item, "diagnosis") }}</p>
            <p>时间：{{ fieldText(item, "createdAt", "2026-06-22 09:34") }}</p>
            <button class="detail-btn" type="button" @click="selectedRecord = item">查看详情</button>
          </template>
          <template v-else>
            <div class="list-left">
              <div class="list-title-row">
                <span class="tag" :class="item.aiGenerated ? 'success' : 'info'">{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</span>
                <strong>#MR{{ fieldText(item, "medicalRecordId") }} · {{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</strong>
              </div>
              <p>主诉：{{ fieldText(item, "chiefComplaint") }}　诊断：{{ fieldText(item, "diagnosis") }}　时间：{{ fieldText(item, "createdAt", "2026-06-22 09:34") }}</p>
            </div>
            <div class="list-detail">
              <button class="detail-btn" type="button" @click="selectedRecord = item">查看详情</button>
            </div>
          </template>
        </article>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <Modal :open="Boolean(selectedRecord)" title="病历详情" @close="selectedRecord = null">
      <div v-if="selectedRecord" style="display:grid;gap:8px;">
        <div><strong>病历号：</strong>#MR{{ fieldText(selectedRecord, "medicalRecordId") }}</div>
        <div><strong>患者：</strong>{{ fieldText(selectedRecord, "patientName", fieldText(selectedRecord, "patientId")) }}</div>
        <div><strong>类型：</strong>{{ selectedRecord.aiGenerated ? "AI 草稿确认" : "医生录入" }}</div>
        <div><strong>主诉：</strong>{{ fieldText(selectedRecord, "chiefComplaint") }}</div>
        <div><strong>诊断：</strong>{{ fieldText(selectedRecord, "diagnosis") }}</div>
        <div><strong>时间：</strong>{{ fieldText(selectedRecord, "createdAt") }}</div>
      </div>
      <template #footer>
        <button class="btn" @click="selectedRecord = null">关闭</button>
      </template>
    </Modal>
  </section>
</template>
