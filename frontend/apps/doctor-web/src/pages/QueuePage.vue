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
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">QUEUE</p><h2>待接诊挂号队列</h2><p>按状态、患者和科室筛选；点击患者进入独立接诊页。</p></div>
      <div class="toolbar">
        <SegmentedControl v-model="filter" :options="options" />
        <button type="button" :disabled="loading" @click="refresh">刷新队列</button>
      </div>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <input v-model.trim="keyword" placeholder="搜索姓名、患者 ID、科室" />
      <LoadingState v-if="loading" title="正在同步队列" />
      <div v-else-if="rows.length" class="queue-list">
        <RouterLink v-for="item in rows" :key="String(item.registrationId)" class="button queue-item" :to="`/consult/${item.registrationId}`">
          <span><strong>{{ fieldText(item, "patientName", `患者 ${fieldText(item, "patientId")}`) }}</strong><br />#{{ fieldText(item, "registrationId") }} / {{ fieldText(item, "departmentName") }} / {{ fieldText(item, "appointmentTime") }}</span>
          <StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" />
        </RouterLink>
      </div>
      <EmptyState v-else title="暂无待接诊患者" message="当前筛选条件下无记录，可清空筛选或刷新队列。" />
    </div>
  </section>
</template>
