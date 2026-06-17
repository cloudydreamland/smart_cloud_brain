<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, toNumber, useAuthStore, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, StatusTag } from "@smart-cloud-brain/shared-ui";
import CancelAppointmentModal from "../components/CancelAppointmentModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { registrations } = storeToRefs(workflow);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const selected = ref<DataRow | null>(null);

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
  try {
    await api.cancelRegistration(auth.token(), toNumber(selected.value.registrationId));
    selected.value = null;
    await refresh();
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
      <div class="panel-title"><p class="eyebrow">APPOINTMENTS</p><h2>我的挂号</h2><p>集中展示待就诊、已取消和已完成记录。</p></div>
      <button type="button" :disabled="loading" @click="refresh">刷新</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" />
      <div v-else-if="registrations.length" class="list">
        <article v-for="item in registrations" :key="String(item.registrationId)" class="list-row">
          <div class="row-main">
            <strong>#{{ fieldText(item, "registrationId") }} {{ fieldText(item, "departmentName") }} · {{ fieldText(item, "doctorName") }}</strong>
            <p>{{ fieldText(item, "appointmentTime") }}</p>
          </div>
          <div class="toolbar">
            <StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" />
            <button class="danger" type="button" :disabled="fieldText(item, 'status') === 'CANCELLED'" @click="selected = item">取消</button>
          </div>
        </article>
      </div>
      <EmptyState v-else title="暂无挂号记录" message="从号源页面选择医生后，记录会显示在这里。" />
    </div>
    <CancelAppointmentModal :open="Boolean(selected)" :busy="saving" @close="selected = null" @confirm="cancel" />
  </section>
</template>
