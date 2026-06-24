<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { displayText, formatApiError, useAuthStore, useDoctorWorkflowStore, usePagination } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar } from "@smart-cloud-brain/shared-ui";
import { formatTime, liveRows, patientName, riskText, statusLabel, statusTone } from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { registrations } = storeToRefs(workflow);
const sourceRows = liveRows(registrations);
const filter = ref("");
const keyword = ref("");
const loading = ref(false);
const error = ref("");
const options = [
  { label: "全部", value: "" },
  { label: "待接诊", value: "CREATED" },
  { label: "已确认", value: "CONFIRMED" },
  { label: "已完成", value: "COMPLETED" },
];

const rows = computed(() => sourceRows.value.filter((item) => {
  const haystack = `${patientName(item)} ${displayText(item.patientId, "")} ${displayText(item.departmentName, "")} ${displayText(item.chiefComplaint, "")}`.toLowerCase();
  return (!filter.value || displayText(item.status) === filter.value) && (!keyword.value || haystack.includes(keyword.value.toLowerCase()));
}));
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 10);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "队列加载失败，请稍后重试。");
  } finally {
    loading.value = false;
  }
}

refresh();
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

    <section class="panel">
      <header>
        <div class="panel-title">
          <p class="eyebrow">接诊队列</p>
          <h2>患者队列筛选</h2>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">{{ loading ? "刷新中" : "刷新" }}</button>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" title="正在同步队列" />
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
