<script setup lang="ts">
import { computed, inject, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { storeToRefs } from "pinia";
import { displayText, formatApiError, useDoctorWorkflowStore, usePagination } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar, Toast } from "@smart-cloud-brain/shared-ui";
import { formatTime, liveRows, patientName, riskText, statusLabel, statusTone } from "../doctorPresentation";

const workflow = useDoctorWorkflowStore();
const route = useRoute();
const { registrations } = storeToRefs(workflow);
const sourceRows = liveRows(registrations);
const filter = ref("");
const keyword = ref(displayText(route.query.patientId ?? route.query.triageRecordId, ""));
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const options = [
  { label: "全部", value: "" },
  { label: "待接诊", value: "CREATED" },
  { label: "已确认", value: "CONFIRMED" },
  { label: "已完成", value: "COMPLETED" },
];

const rows = computed(() => sourceRows.value.filter((item) => {
  const haystack = `${patientName(item)} ${displayText(item.patientId, "")} ${displayText(item.registrationId, "")} ${displayText(item.triageRecordId, "")} ${displayText(item.departmentName, "")} ${displayText(item.chiefComplaint, "")}`.toLowerCase();
  return (!filter.value || displayText(item.status) === filter.value) && (!keyword.value || haystack.includes(keyword.value.toLowerCase()));
}));
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 10);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    loaded.value = true;
    toast?.value?.success("数据已刷新", "队列数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "队列加载失败，请稍后重试。");
    toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    loading.value = false;
  }
}

refresh();

watch(() => [route.query.patientId, route.query.triageRecordId], ([patientId, triageRecordId]) => {
  const next = displayText(patientId ?? triageRecordId, "");
  if (next) keyword.value = next;
});
</script>

<template>
  <section class="clinical-page queue-workbench">
    <header class="search-row">
      <input v-model.trim="keyword" placeholder="搜索姓名 / 患者 ID / 科室 / 主诉" />
      <div class="segmented" role="tablist" aria-label="队列状态">
        <button
          v-for="item in options"
          :key="item.value"
          type="button"
          :class="{ active: filter === item.value }"
          @click="filter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <span class="match-pill">匹配 {{ rows.length }} 条</span>
    </header>
    <div v-if="route.query.notice === 'select-patient'" class="notice info">当前通知没有可直接进入的挂号记录，请在队列中选择患者接诊。</div>

    <section class="panel">
      <header>
        <div class="panel-title">
          <p class="eyebrow">接诊队列</p>
          <h2>患者队列筛选</h2>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="!loaded && loading" title="正在同步队列" />
        <div v-else class="table-wrap table-breakout">
          <table class="data-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>患者</th>
                <th>患者 ID</th>
                <th>科室</th>
                <th>预约</th>
                <th>主诉摘要</th>
                <th>风险</th>
                <th>状态</th>
                <th class="actions-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in pageRows" :key="String(item.registrationId)">
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td><strong>{{ patientName(item) }}</strong></td>
                <td>{{ displayText(item.patientId) }}</td>
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ formatTime(displayText(item.appointmentTime)) }}</td>
                <td>{{ displayText(item.chiefComplaint, "待医生问诊补充") }}</td>
                <td><span class="tag" :class="statusTone(item.riskLevel)">{{ riskText(item) }}</span></td>
                <td><span class="tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
                <td class="actions-cell">
                  <RouterLink class="action-btn primary" :to="`/consult/${item.registrationId}`">
                    {{ displayText(item.status).toUpperCase() === "COMPLETED" ? "查看" : "接诊" }}
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
          <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" centered />
        </div>
      </div>
    </section>
  </section>
</template>
