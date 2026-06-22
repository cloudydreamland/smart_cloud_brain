<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, statusText, toNumber, useAuthStore, usePagination, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";
import ConfirmAppointmentModal from "../components/ConfirmAppointmentModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { slots, doctors, departments, triageHistory } = storeToRefs(workflow);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const notice = ref("");
const selectedSlot = ref<DataRow | null>(null);
const confirmOpen = ref(false);
const recommendedDepartment = computed(() => fieldText(triageHistory.value[0], "recommendedDepartment", ""));
const visibleSlots = computed(() => slots.value.filter((slot) => !recommendedDepartment.value || fieldText(slot, "departmentName", "").includes(recommendedDepartment.value)));
const displaySlots = computed(() => visibleSlots.value.length ? visibleSlots.value : slots.value);
const {
  currentPage: slotPage,
  pageSize: slotPageSize,
  total: slotTotal,
  pageRows: pagedSlots,
} = usePagination(displaySlots, 6);
const {
  currentPage: departmentPage,
  pageSize: departmentPageSize,
  total: departmentTotal,
  pageRows: pagedDepartments,
} = usePagination(departments, 5);

async function refresh() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "号源加载失败");
  } finally {
    loading.value = false;
  }
}

function choose(slot: DataRow) {
  selectedSlot.value = slot;
  confirmOpen.value = true;
}

async function confirmAppointment() {
  if (!selectedSlot.value) return;
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.createRegistration(auth.token(), {
      doctorId: toNumber(selectedSlot.value.doctorId),
      departmentId: toNumber(selectedSlot.value.departmentId),
      appointmentTime: fieldText(selectedSlot.value, "startTime", ""),
      slotId: toNumber(selectedSlot.value.slotId) || null,
      triageRecordId: toNumber(triageHistory.value[0]?.triageRecordId, 0) || null,
    });
    await workflow.refreshAuthenticated(auth.token());
    confirmOpen.value = false;
    notice.value = "预约已提交，可在“我的挂号”页面查看或取消。";
  } catch (err) {
    error.value = formatApiError(err, "挂号失败");
  } finally {
    saving.value = false;
  }
}

refresh();
</script>

<template>
  <section class="portal-grid">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <p class="eyebrow">预约医生</p>
          <h2>可预约号源</h2>
          <p>优先展示与当前分诊推荐科室匹配的医生号源。</p>
        </div>
        <button type="button" :disabled="loading" @click="refresh">刷新号源</button>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="summary-strip">
          <div class="summary-item"><span>推荐科室</span><strong>{{ recommendedDepartment || "暂无" }}</strong></div>
          <div class="summary-item"><span>医生</span><strong>{{ doctors.length }}</strong></div>
          <div class="summary-item"><span>号源</span><strong>{{ displaySlots.length }}</strong></div>
        </div>
        <LoadingState v-if="loading" />
        <div v-else-if="displaySlots.length" class="stack">
          <article v-for="slot in pagedSlots" :key="String(slot.slotId)" class="slot-card" :class="{ selected: selectedSlot?.slotId === slot.slotId }">
            <div class="row-main">
              <strong>{{ fieldText(slot, "departmentName") }} · {{ fieldText(slot, "doctorName") }}</strong>
              <p>{{ fieldText(slot, "startTime") }} · 余号 {{ fieldText(slot, "remainingCapacity", "0") }}/{{ fieldText(slot, "capacity", "0") }}</p>
            </div>
            <div class="toolbar">
              <StatusTag :status="statusText(slot.status)" :tone="statusClass(slot.status)" />
              <button class="primary" type="button" @click="choose(slot)">选择</button>
            </div>
          </article>
          <PaginationBar v-model="slotPage" :total="slotTotal" :page-size="slotPageSize" />
        </div>
        <EmptyState v-else title="暂无号源" message="当前推荐科室暂无可预约号源，请刷新或稍后再试。" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>科室与医生</h2><p>后端公开科室和医生信息。</p></div></header>
      <div class="panel-body stack">
        <div v-for="department in pagedDepartments" :key="String(department.id)" class="clinical-note">
          <strong>{{ fieldText(department, "name") }}</strong>
          <p>{{ fieldText(department, "description", "暂无说明") }}</p>
        </div>
        <PaginationBar v-model="departmentPage" :total="departmentTotal" :page-size="departmentPageSize" />
        <EmptyState v-if="!departments.length" title="暂无科室数据" />
      </div>
    </aside>
    <ConfirmAppointmentModal :open="confirmOpen" :slot="selectedSlot" :busy="saving" @close="confirmOpen = false" @confirm="confirmAppointment" />
  </section>
</template>
