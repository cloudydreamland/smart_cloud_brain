<script setup lang="ts">
import { computed, inject, onMounted, ref, watch, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { api, formatApiError, statusClass, statusText, toNumber, usePatientWorkflowStore, type Doctor, type Registration, type AppointmentSlot, type TriageRecord } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";
import ConfirmAppointmentModal from "../components/ConfirmAppointmentModal.vue";

type DateGroup = {
  date: string;
  label: string;
  total: number;
  slots: AppointmentSlot[];
};

type DoctorGroup = {
  key: string;
  doctorName: string;
  departmentName: string;
  total: number;
  periods: { key: string; label: string; slots: AppointmentSlot[] }[];
};

const workflow = usePatientWorkflowStore();
const { slots, doctors, triageHistory, registrations } = storeToRefs(workflow);
const loading = ref(false);
const loaded = ref(false);
const saving = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const selectedDate = ref("");
const selectedSlot = ref<AppointmentSlot | null>(null);
const confirmOpen = ref(false);

const bookedTimes = computed(() => {
  const times = new Set<string>();
  registrations.value.forEach((reg) => {
    const status = reg.status || "";
    if (status === "CANCELLED") return;
    const time = reg.appointmentTime || "";
    if (time) times.add(time);
  });
  return times;
});

function isSlotBooked(slot: AppointmentSlot) {
  return bookedTimes.value.has(slot.startTime || "");
}

const latestTriage = computed(() => triageHistory.value[0] ?? null);
const assignedDoctorId = computed(() => toNumber(latestTriage.value?.assignedDoctorId, 0));
const recommendedDepartment = computed(() => latestTriage.value?.recommendedDepartment || "");
const assignedDoctorSlots = computed(() => slots.value.filter((slot) => assignedDoctorId.value && toNumber(slot.doctorId) === assignedDoctorId.value));
const recommendedDepartmentSlots = computed(() => slots.value.filter((slot) =>
  !assignedDoctorId.value && (!recommendedDepartment.value || (slot.departmentName || "").includes(recommendedDepartment.value))
));
const displaySlots = computed(() => {
  const source = assignedDoctorSlots.value.length
    ? assignedDoctorSlots.value
    : recommendedDepartmentSlots.value.length
      ? recommendedDepartmentSlots.value
      : slots.value;
  return [...source].sort((left, right) => slotTimestamp(left) - slotTimestamp(right));
});
const guidanceLabel = computed(() => assignedDoctorId.value ? "已分配医生" : "推荐科室");
const guidanceValue = computed(() => {
  if (!assignedDoctorId.value) return recommendedDepartment.value || "暂无";
  const slotDoctor = slots.value.find((slot) => toNumber(slot.doctorId) === assignedDoctorId.value);
  const doctor = doctors.value.find((item) => toNumber(item.id) === assignedDoctorId.value);
  return slotDoctor?.doctorName || doctor?.name || `医生 #${assignedDoctorId.value}`;
});
const dateGroups = computed<DateGroup[]>(() => {
  const groups = new Map<string, DateGroup>();
  displaySlots.value.forEach((slot) => {
    const time = slot.startTime || "";
    const date = slotDateKey(time);
    if (!groups.has(date)) {
      groups.set(date, { date, label: slotDateLabel(time), total: 0, slots: [] as AppointmentSlot[] });
    }
    const group = groups.get(date)!;
    group.total += 1;
    group.slots.push(slot);
  });
  return [...groups.values()];
});
const activeDateGroup = computed(() => dateGroups.value.find((group) => group.date === selectedDate.value) ?? dateGroups.value[0]);
const doctorGroups = computed<DoctorGroup[]>(() => {
  const groups = new Map<string, { doctorName: string; departmentName: string; slots: AppointmentSlot[] }>();
  activeDateGroup.value?.slots.forEach((slot) => {
    const key = String(slot.doctorId || slot.doctorName || "unknown");
    if (!groups.has(key)) {
      groups.set(key, {
        doctorName: slot.doctorName || "未命名医生",
        departmentName: slot.departmentName || "未定科室",
        slots: [],
      });
    }
    groups.get(key)!.slots.push(slot);
  });
  return [...groups.entries()].map(([key, group]) => {
    const periods = new Map<string, { key: string; label: string; slots: AppointmentSlot[] }>();
    group.slots.forEach((slot) => {
      const period = slotPeriod(slot.startTime || "");
      if (!periods.has(period.key)) {
        periods.set(period.key, { key: period.key, label: period.label, slots: [] });
      }
      periods.get(period.key)!.slots.push(slot);
    });
    return {
      key,
      doctorName: group.doctorName,
      departmentName: group.departmentName,
      total: group.slots.length,
      periods: [...periods.values()].sort((left, right) => periodOrder(left.key) - periodOrder(right.key)),
    };
  });
});

watch(dateGroups, (groups) => {
  if (!groups.length) {
    selectedDate.value = "";
  } else if (!groups.some((group) => group.date === selectedDate.value)) {
    selectedDate.value = groups[0].date;
  }
}, { immediate: true });

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated();
    loaded.value = true;
  } catch (err) {
    error.value = formatApiError(err, "号源加载失败");
  } finally {
    loading.value = false;
  }
}

function choose(slot: AppointmentSlot) {
  if (isSlotBooked(slot)) return;
  selectedSlot.value = slot;
  confirmOpen.value = true;
}

function slotTimestamp(slot: AppointmentSlot) {
  const value = Date.parse(slot.startTime || "");
  return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
}

function slotDateKey(time: string) {
  return time && time !== "-" ? time.slice(0, 10) : "unscheduled";
}

function slotDateLabel(time: string) {
  const key = slotDateKey(time);
  if (key === "unscheduled") return "未定日期";
  const date = new Date(`${key}T00:00:00`);
  if (Number.isNaN(date.getTime())) return key;
  return new Intl.DateTimeFormat("zh-CN", { month: "long", day: "numeric", weekday: "short" }).format(date);
}

function slotTimeText(slot: AppointmentSlot) {
  const value = slot.startTime || "";
  if (!value || value === "-") return "待定";
  const time = value.includes("T") ? value.split("T")[1] : value.split(" ")[1];
  return time ? time.slice(0, 5) : value;
}

function slotPeriod(time: string) {
  const raw = time.includes("T") ? time.split("T")[1] : time.split(" ")[1] || "";
  const hour = Number(raw.slice(0, 2));
  if (!Number.isFinite(hour)) return { key: "unknown", label: "待定时段" };
  if (hour < 12) return { key: "morning", label: "上午" };
  if (hour < 18) return { key: "afternoon", label: "下午" };
  return { key: "evening", label: "晚上" };
}

function periodOrder(key: string) {
  return { morning: 1, afternoon: 2, evening: 3, unknown: 4 }[key as "morning" | "afternoon" | "evening" | "unknown"] ?? 5;
}

async function confirmAppointment() {
  if (!selectedSlot.value) return;
  saving.value = true;
  error.value = "";
  try {
    await api.createRegistration({
      doctorId: toNumber(selectedSlot.value.doctorId),
      departmentId: toNumber(selectedSlot.value.departmentId),
      appointmentTime: selectedSlot.value?.startTime || "",
      slotId: toNumber(selectedSlot.value.slotId) || null,
      triageRecordId: toNumber(triageHistory.value[0]?.triageRecordId, 0) || null,
    });
    await workflow.refreshAuthenticated();
    confirmOpen.value = false;
    toast?.value?.success('预约已提交', '可在"我的挂号"页面查看或取消。');
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
        <p>按日期选择号源，查看当天出诊医生及可预约时间段。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新号源
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="summary-strip">
        <div class="summary-item"><span>{{ guidanceLabel }}</span><strong>{{ guidanceValue }}</strong></div>
        <div class="summary-item"><span>医生</span><strong>{{ doctors.length }}</strong></div>
        <div class="summary-item"><span>号源</span><strong>{{ displaySlots.length }}</strong></div>
        <div class="summary-item"><span>可约日期</span><strong>{{ dateGroups.length }}</strong></div>
      </div>

      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="displaySlots.length" class="slot-schedule">
        <div class="slot-date-picker" role="tablist" aria-label="选择预约日期">
          <button
            v-for="date in dateGroups"
            :key="date.date"
            type="button"
            class="slot-date-option"
            :class="{ active: selectedDate === date.date }"
            @click="selectedDate = date.date"
          >
            <strong>{{ date.label }}</strong>
            <span>{{ date.date }}</span>
            <em>{{ date.total }} 个号源</em>
          </button>
        </div>

        <div v-if="doctorGroups.length" class="doctor-schedule-list">
          <article v-for="doctor in doctorGroups" :key="doctor.key" class="doctor-schedule-card">
            <header class="doctor-schedule-header">
              <div>
                <strong>{{ doctor.doctorName }}</strong>
                <span>{{ doctor.departmentName }}</span>
              </div>
              <span class="tag success">{{ doctor.total }} 个时段</span>
            </header>
            <div class="doctor-period-list">
              <section v-for="period in doctor.periods" :key="`${doctor.key}-${period.key}`" class="doctor-period-group">
                <div class="doctor-period-label">
                  <strong>{{ period.label }}</strong>
                  <span>{{ period.slots.length }} 个可选</span>
                </div>
                <div class="slot-card-grid">
                  <article v-for="slot in period.slots" :key="String(slot.slotId)" class="slot-card" :class="{ selected: selectedSlot?.slotId === slot.slotId, booked: isSlotBooked(slot) }">
                    <div class="row-main">
                      <strong>{{ slotTimeText(slot) }}</strong>
                      <p>余号 {{ String(slot.remainingCapacity ?? 0) }}/{{ String(slot.capacity ?? 0) }}</p>
                    </div>
                    <div class="toolbar">
                      <template v-if="isSlotBooked(slot)">
                        <span class="tag booked-label">已选择</span>
                      </template>
                      <template v-else>
                        <StatusTag :status="statusText(slot.status)" :tone="statusClass(slot.status)" />
                        <button class="primary" type="button" @click="choose(slot)">选择</button>
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
      <EmptyState v-else title="暂无号源" message="当前推荐科室暂无可预约号源，请刷新或稍后再试。" />
    </div>
    <ConfirmAppointmentModal :open="confirmOpen" :slot="selectedSlot" :busy="saving" @close="confirmOpen = false" @confirm="confirmAppointment" />
  </section>
</template>
