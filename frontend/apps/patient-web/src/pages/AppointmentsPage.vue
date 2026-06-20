<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, statusText, toNumber, useAuthStore, usePagination, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";
import CancelAppointmentModal from "../components/CancelAppointmentModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { registrations } = storeToRefs(workflow);
const statusFilter = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const notice = ref("");
const selected = ref<DataRow | null>(null);
const filterOptions = [
  { label: "全部", value: "" },
  { label: "未完成", value: "UNFINISHED" },
  { label: "已完成", value: "COMPLETED" },
];
const filteredRegistrations = computed(() => registrations.value.filter((item) => {
  if (statusFilter.value === "COMPLETED") return fieldText(item, "status") === "COMPLETED";
  if (statusFilter.value === "UNFINISHED") return fieldText(item, "status") !== "COMPLETED";
  return true;
}));
const { currentPage, pageSize, total, pageRows } = usePagination(filteredRegistrations, 8);

watch(statusFilter, () => {
  currentPage.value = 1;
});

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "挂号记录加载失败");
  } finally {
    loading.value = false;
  }
}

async function cancel() {
  if (!selected.value) return;
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.cancelRegistration(auth.token(), toNumber(selected.value.registrationId));
    selected.value = null;
    await refresh();
    notice.value = "挂号已取消。";
  } catch (err) {
    error.value = formatApiError(err, "取消挂号失败");
  } finally {
    saving.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">我的挂号</p><h2>我的挂号</h2><p>按完成状态分组查看挂号记录。</p></div>
      <button type="button" :disabled="loading" @click="refresh">刷新</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <div class="toolbar">
        <SegmentedControl v-model="statusFilter" :options="filterOptions" />
        <span class="row-meta">当前 {{ total }} 条 / 全部 {{ registrations.length }} 条</span>
      </div>
      <LoadingState v-if="loading" />
      <div v-else-if="filteredRegistrations.length" class="list">
        <article v-for="item in pageRows" :key="String(item.registrationId)" class="list-row">
          <div class="row-main">
            <strong>#{{ fieldText(item, "registrationId") }} {{ fieldText(item, "departmentName") }} · {{ fieldText(item, "doctorName") }}</strong>
            <p>{{ fieldText(item, "appointmentTime") }}</p>
          </div>
          <div class="toolbar">
            <StatusTag :status="statusText(item.status)" :tone="statusClass(item.status)" />
            <button class="danger" type="button" :disabled="['CANCELLED', 'COMPLETED'].includes(fieldText(item, 'status'))" @click="selected = item">取消</button>
          </div>
        </article>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无挂号记录" message="当前分类下没有挂号记录。" />
    </div>
    <CancelAppointmentModal :open="Boolean(selected)" :busy="saving" @close="selected = null" @confirm="cancel" />
  </section>
</template>
