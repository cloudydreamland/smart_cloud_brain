<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, statusClass, useAuthStore, useDoctorWorkflowStore, usePagination } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { registrations } = storeToRefs(workflow);
const filter = ref("CREATED");
const keyword = ref("");
const loading = ref(false);
const error = ref("");
const options = [
  { label: "全部", value: "" },
  { label: "待接诊", value: "CREATED" },
  { label: "已确认", value: "CONFIRMED" },
  { label: "已完成", value: "COMPLETED" },
];

const rows = computed(() => registrations.value.filter((item) => {
  const haystack = `${fieldText(item, "patientName", "")} ${fieldText(item, "patientId", "")} ${fieldText(item, "departmentName", "")}`.toLowerCase();
  return (!filter.value || fieldText(item, "status") === filter.value) && (!keyword.value || haystack.includes(keyword.value.toLowerCase()));
}));
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 10);

function isCompleted(item: Record<string, unknown>) {
  return fieldText(item, "status").toUpperCase() === "COMPLETED";
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "队列加载失败");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="clinical-page queue-workbench">
    <header class="queue-toolbar">
      <div class="queue-filter-group">
        <SegmentedControl v-model="filter" :options="options" />
        <input v-model.trim="keyword" class="queue-search" placeholder="搜索姓名 / 患者 ID / 科室" />
      </div>
      <div class="queue-actions">
        <span class="queue-count">匹配 {{ rows.length }} 条</span>
        <button type="button" :disabled="loading" @click="refresh">刷新</button>
      </div>
    </header>

    <section class="clinical-section">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" title="正在同步队列" />
      <div v-else-if="rows.length" class="table-scroll">
        <table class="clinical-table queue-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>患者</th>
              <th>患者 ID</th>
              <th>科室</th>
              <th>预约时间</th>
              <th>状态</th>
              <th class="actions-cell">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in pageRows" :key="String(item.registrationId)">
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td><strong>{{ fieldText(item, "patientName", `患者${fieldText(item, "patientId")}`) }}</strong></td>
              <td>{{ fieldText(item, "patientId", "-") }}</td>
              <td>{{ fieldText(item, "departmentName", "-") }}</td>
              <td>{{ fieldText(item, "appointmentTime", "-") }}</td>
              <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
              <td>
                <RouterLink class="button compact-action" :class="{ primary: !isCompleted(item) }" :to="`/consult/${item.registrationId}`">
                  {{ isCompleted(item) ? "查看" : "接诊" }}
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无待接诊患者" message="" />
    </section>
  </section>
</template>
