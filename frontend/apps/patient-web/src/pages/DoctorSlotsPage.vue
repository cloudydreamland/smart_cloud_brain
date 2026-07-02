<script setup lang="ts">
import { computed, inject, onMounted, ref, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { api, formatApiError, toNumber, usePatientWorkflowStore, type AppointmentSlot } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";
import ConfirmAppointmentModal from "../components/ConfirmAppointmentModal.vue";
import { useDoctorSlots } from "../composables/useDoctorSlots";

const workflow = usePatientWorkflowStore();
const { slots, doctors, departments, triageHistory, registrations } = storeToRefs(workflow);
const loading = ref(false);
const loaded = ref(false);
const saving = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const confirmOpen = ref(false);
const latestTriage = computed(() => triageHistory.value[0] ?? null);
const slotState = useDoctorSlots(slots, registrations, departments, latestTriage, triageHistory);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated();
    await slotState.loadVisitors();
    loaded.value = true;
  } catch (err) {
    error.value = formatApiError(err, "号源加载失败");
  } finally {
    loading.value = false;
  }
}

function choose(slot: AppointmentSlot) {
  if (slotState.choose(slot)) confirmOpen.value = true;
}

function onDepartmentChange(event: Event) {
  const target = event.target as HTMLSelectElement | null;
  slotState.changeDepartment(target?.value || "");
}

async function confirmAppointment() {
  if (!slotState.selectedSlot.value) return;
  const visitor = slotState.selectedVisitor.value;
  if (!visitor) {
    error.value = "请选择就诊人";
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    await api.createRegistration({
      doctorId: toNumber(slotState.selectedSlot.value.doctorId),
      departmentId: toNumber(slotState.selectedSlot.value.departmentId),
      appointmentTime: slotState.selectedSlot.value.startTime || "",
      slotId: toNumber(slotState.selectedSlot.value.slotId) || null,
      triageRecordId: toNumber(slotState.selectedTriage.value?.triageRecordId, 0) || null,
      visitorId: toNumber(visitor.id) || null,
      visitorType: visitor.visitorType || "ACCOUNT",
      subjectId: toNumber(visitor.id) || null,
      subjectType: visitor.visitorType || "ACCOUNT",
    });
    await workflow.refreshAuthenticated();
    confirmOpen.value = false;
    slotState.selectedSlot.value = null;
    toast?.value?.success("预约已提交", "可在“我的挂号”页面查看或取消。");
  } catch (err) {
    error.value = formatApiError(err, "挂号失败");
  } finally {
    saving.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">预约医生</p>
        <h2>可预约号源</h2>
        <p>先选择就诊人和科室，再按日期查看当天出诊医生及可预约时间段。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ spin: loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新号源
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="registration-filter-bar">
        <label>
          <span>就诊人</span>
          <select v-model="slotState.selectedVisitorKey.value" :disabled="slotState.visitorLoading.value">
            <option v-for="visitor in slotState.visitorOptions.value" :key="String(visitor.value)" :value="String(visitor.value)">
              {{ visitor.label }}
            </option>
          </select>
        </label>
        <label>
          <span>科室</span>
          <select :value="slotState.selectedDepartmentId.value" @change="onDepartmentChange">
            <option v-for="department in slotState.departmentOptions.value" :key="String(department.value)" :value="String(department.value)">
              {{ department.label }}
            </option>
          </select>
        </label>
      </div>
      <div class="summary-strip">
        <div class="summary-item"><span>{{ slotState.guidanceLabel.value }}</span><strong>{{ slotState.guidanceValue.value }}</strong></div>
        <div class="summary-item"><span>医生</span><strong>{{ doctors.length }}</strong></div>
        <div class="summary-item"><span>当前号源</span><strong>{{ slotState.displaySlots.value.length }}</strong></div>
        <div class="summary-item"><span>可约日期</span><strong>{{ slotState.dateGroups.value.length }}</strong></div>
      </div>

      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="slotState.displaySlots.value.length" class="slot-schedule">
        <div class="slot-date-picker" role="tablist" aria-label="选择预约日期">
          <button
            v-for="date in slotState.dateGroups.value"
            :key="date.date"
            type="button"
            class="slot-date-option"
            :class="{ active: slotState.selectedDate.value === date.date }"
            @click="slotState.selectedDate.value = date.date"
          >
            <strong>{{ date.label }}</strong>
            <span>{{ date.date }}</span>
            <em>{{ date.total }} 个号源</em>
          </button>
        </div>

        <div v-if="slotState.doctorGroups.value.length" class="doctor-schedule-list">
          <article v-for="doctor in slotState.doctorGroups.value" :key="doctor.key" class="doctor-schedule-card">
            <header class="doctor-schedule-header">
              <div><strong>{{ doctor.doctorName }}</strong><span>{{ doctor.departmentName }}</span></div>
              <span class="tag success">{{ doctor.total }} 个时段</span>
            </header>
            <div class="doctor-period-list">
              <section v-for="period in doctor.periods" :key="`${doctor.key}-${period.key}`" class="doctor-period-group">
                <div class="doctor-period-label"><strong>{{ period.label }}</strong><span>{{ period.slots.length }} 个可选</span></div>
                <div class="slot-card-grid">
                  <article v-for="slot in period.slots" :key="String(slot.slotId)" class="slot-card" :class="{ selected: slotState.selectedSlot.value?.slotId === slot.slotId, booked: slotState.isSlotBooked(slot) }">
                    <div class="row-main"><strong>{{ slotState.slotTimeText(slot) }}</strong><p>余号 {{ String(slot.remainingCapacity ?? 0) }}/{{ String(slot.capacity ?? 0) }}</p></div>
                    <div class="toolbar">
                      <span v-if="slotState.isSlotBooked(slot)" class="tag booked-label">已选择</span>
                      <template v-else>
                        <span v-if="slotState.recommendedSlotIds.value.has(String(slot.slotId))" class="tag recommended-tag">推荐科室</span>
                        <StatusTag :status="slotState.statusText(slot.status)" :tone="slotState.statusClass(slot.status)" />
                        <button class="primary" type="button" :disabled="slot.status !== 'AVAILABLE' || !slot.remainingCapacity" @click="choose(slot)">{{ !slot.remainingCapacity ? "已满" : "选择" }}</button>
                      </template>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </article>
        </div>
        <EmptyState v-else title="当天暂无医生出诊" message="请选择其他日期或刷新号源。" />
      </div>
      <EmptyState v-else title="暂无号源" message="当前科室暂无可预约号源，可切换其他科室或稍后再试。" />
    </div>
    <ConfirmAppointmentModal :open="confirmOpen" :slot="slotState.selectedSlot.value" :visitor="slotState.selectedVisitor.value" :busy="saving" @close="confirmOpen = false" @confirm="confirmAppointment" />
  </section>
</template>

<style scoped>
.registration-filter-bar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.registration-filter-bar label {
  display: grid;
  gap: 8px;
}

.registration-filter-bar span {
  color: var(--hospital-muted);
  font-size: 13px;
  font-weight: 500;
}

.registration-filter-bar select {
  min-height: 42px;
  border: 1px solid var(--hospital-line);
  background: var(--surface);
  color: var(--hospital-ink);
  padding: 0 12px;
}

.summary-strip {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.recommended-tag {
  background: var(--primary-soft);
  color: var(--primary);
}

@media (max-width: 720px) {
  .registration-filter-bar {
    grid-template-columns: 1fr;
  }

  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
