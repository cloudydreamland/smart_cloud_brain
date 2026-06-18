<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatApiError, statusClass, useAuthStore, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { registrations } = storeToRefs(workflow);
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

const rows = computed(() => registrations.value.filter((item) => {
  const haystack = `${fieldText(item, "patientName", "")} ${fieldText(item, "patientId", "")} ${fieldText(item, "departmentName", "")}`.toLowerCase();
  return (!filter.value || fieldText(item, "status") === filter.value) && (!keyword.value || haystack.includes(keyword.value.toLowerCase()));
}));

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
  <section class="queue-workbench">
    <header class="workbench-strip">
      <div>
        <p class="eyebrow">QUEUE</p>
        <h2>待接诊挂号队列</h2>
      </div>
      <div class="queue-controls">
        <SegmentedControl v-model="filter" :options="options" />
        <button type="button" :disabled="loading" @click="refresh">刷新队列</button>
      </div>
    </header>

    <div class="queue-filterbar">
      <input v-model.trim="keyword" placeholder="搜索姓名、患者 ID、科室" />
      <span class="tag info">匹配 {{ rows.length }} 条</span>
    </div>

    <div class="workbench-section">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" title="正在同步队列" />
      <div v-else-if="rows.length" class="table-scroll">
        <table class="data-table compact-table queue-table">
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
            <tr v-for="(item, index) in rows" :key="String(item.registrationId)">
              <td>{{ index + 1 }}</td>
              <td><strong>{{ fieldText(item, "patientName", `患者 ${fieldText(item, "patientId")}`) }}</strong></td>
              <td>{{ fieldText(item, "patientId", "-") }}</td>
              <td>{{ fieldText(item, "departmentName", "-") }}</td>
              <td>{{ fieldText(item, "appointmentTime", "-") }}</td>
              <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
              <td><RouterLink class="button primary compact-action" :to="`/consult/${item.registrationId}`">接诊</RouterLink></td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无待接诊患者" message="当前筛选条件下无记录，可清空筛选或刷新队列。" />
    </div>
  </section>
</template>
