<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, statusClass, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";

const workflow = useDoctorWorkflowStore();
const { registrations } = storeToRefs(workflow);
const filter = ref("");
const keyword = ref("");
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
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">QUEUE</p><h2>待接诊挂号列表</h2><p>按状态和患者信息筛选。</p></div>
      <SegmentedControl v-model="filter" :options="options" />
    </header>
    <div class="panel-body stack">
      <input v-model.trim="keyword" placeholder="搜索姓名、患者 ID、科室" />
      <div v-if="rows.length" class="queue-list">
        <RouterLink v-for="item in rows" :key="String(item.registrationId)" class="button queue-item" :to="`/consult/${item.registrationId}`">
          <span><strong>{{ fieldText(item, "patientName", `患者 ${fieldText(item, "patientId")}`) }}</strong><br />#{{ fieldText(item, "registrationId") }} · {{ fieldText(item, "departmentName") }} · {{ fieldText(item, "appointmentTime") }}</span>
          <StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" />
        </RouterLink>
      </div>
      <EmptyState v-else title="暂无待接诊患者" message="当前筛选条件下无记录，可清空筛选或同步队列。" />
    </div>
  </section>
</template>
