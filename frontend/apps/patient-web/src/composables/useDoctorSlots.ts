import { computed, ref, watch, type Ref } from "vue";
import { api, formatApiError, statusClass, statusText, toNumber, type AppointmentSlot, type Department, type Patient } from "@smart-cloud-brain/shared-api";

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

type TriageLike = {
  triageRecordId?: number | string;
  recommendedDepartment?: string;
  assignedDoctorId?: number | string;
  subjectType?: string;
  subjectId?: number | string;
};

type RegistrationLike = {
  status?: string;
  appointmentTime?: string;
  subjectType?: string;
  subjectId?: number | string;
  visitorType?: string;
  visitorId?: number | string;
};

export function useDoctorSlots(
  slots: Ref<AppointmentSlot[]>,
  registrations: Ref<RegistrationLike[]>,
  departments: Ref<Department[]>,
  latestTriage: Readonly<Ref<TriageLike | null>>,
  triageHistory?: Ref<TriageLike[]>
) {
  const selectedDate = ref("");
  const selectedDepartmentId = ref("");
  const selectedVisitorKey = ref("");
  const selectedSlot = ref<AppointmentSlot | null>(null);
  const visitors = ref<Patient[]>([]);
  const visitorLoading = ref(false);
  const filtersTouched = ref(false);

  const selectedDepartment = computed(() => departments.value.find((item) => String(item.id) === selectedDepartmentId.value) ?? null);
  const selectedVisitor = computed(() => visitors.value.find((item) => visitorKey(item) === selectedVisitorKey.value) ?? visitors.value[0] ?? null);
  const selectedTriage = computed(() => {
    if (!triageHistory?.value?.length || !selectedVisitor.value) return latestTriage.value;
    return [...triageHistory.value]
      .filter((record) => subjectMatches(record, selectedVisitor.value))
      .sort((left, right) => toNumber(right.triageRecordId, 0) - toNumber(left.triageRecordId, 0))[0] ?? null;
  });
  const recommendedDepartment = computed(() => selectedTriage.value?.recommendedDepartment || "");
  const assignedDoctorId = computed(() => toNumber(selectedTriage.value?.assignedDoctorId, 0));
  const departmentOptions = computed(() => [
    { label: "全部科室", value: "" },
    ...departments.value.map((item) => ({ label: departmentLabel(item), value: String(item.id) })),
  ]);
  const visitorOptions = computed(() => visitors.value.map((item) => ({ label: visitorLabel(item), value: visitorKey(item) })));

  const bookedTimes = computed(() => {
    const times = new Set<string>();
    registrations.value.forEach((reg) => {
      if (["CANCELLED", "REFUNDING", "REFUNDED", "NO_SHOW"].includes(String(reg.status || "").toUpperCase())) return;
      if (selectedVisitor.value && !subjectMatches(reg, selectedVisitor.value)) return;
      if (reg.appointmentTime) times.add(reg.appointmentTime);
    });
    return times;
  });
  const recommendedSlotIds = computed(() => {
    if (!recommendedDepartment.value) return new Set<string>();
    const ids = new Set<string>();
    slots.value.forEach((slot) => {
      if ((slot.departmentName || "").includes(recommendedDepartment.value)) ids.add(String(slot.slotId));
    });
    return ids;
  });
  const displaySlots = computed(() => {
    const department = selectedDepartment.value;
    const rows = department
      ? slots.value.filter((slot) => toNumber(slot.departmentId) === toNumber(department.id))
      : slots.value;
    return [...rows].sort((left, right) => {
      const leftRec = recommendedSlotIds.value.has(String(left.slotId)) ? 0 : 1;
      const rightRec = recommendedSlotIds.value.has(String(right.slotId)) ? 0 : 1;
      if (leftRec !== rightRec) return leftRec - rightRec;
      return slotTimestamp(left) - slotTimestamp(right);
    });
  });
  const dateGroups = computed<DateGroup[]>(() => {
    const groups = new Map<string, DateGroup>();
    displaySlots.value.forEach((slot) => {
      const time = slot.startTime || "";
      const date = slotDateKey(time);
      if (!groups.has(date)) groups.set(date, { date, label: slotDateLabel(time), total: 0, slots: [] });
      const group = groups.get(date)!;
      group.total += 1;
      group.slots.push(slot);
    });
    return [...groups.values()].sort((a, b) => a.date.localeCompare(b.date));
  });
  const activeDateGroup = computed(() => dateGroups.value.find((group) => group.date === selectedDate.value) ?? dateGroups.value[0]);
  const doctorGroups = computed<DoctorGroup[]>(() => groupSlotsByDoctor(activeDateGroup.value?.slots ?? []));
  const guidanceLabel = computed(() => assignedDoctorId.value ? "已分配医生" : recommendedDepartment.value ? "推荐科室" : "筛选方式");
  const guidanceValue = computed(() => recommendedDepartment.value || "全部科室");

  watch(dateGroups, (groups) => {
    if (!groups.length) selectedDate.value = "";
    else if (!groups.some((group) => group.date === selectedDate.value)) selectedDate.value = groups[0].date;
  }, { immediate: true });

  watch([departments, recommendedDepartment], () => {
    if (filtersTouched.value || selectedDepartmentId.value || !recommendedDepartment.value) return;
    const found = departments.value.find((item) => {
      const name = item.name || "";
      return name.includes(recommendedDepartment.value) || recommendedDepartment.value.includes(name);
    });
    if (found) selectedDepartmentId.value = String(found.id);
  }, { immediate: true });

  watch(visitors, (rows) => {
    if (!selectedVisitorKey.value && rows.length) selectedVisitorKey.value = visitorKey(rows[0]);
  }, { immediate: true });

  function changeDepartment(value: string) {
    filtersTouched.value = true;
    selectedDepartmentId.value = value;
  }

  function isSlotBooked(slot: AppointmentSlot) {
    return bookedTimes.value.has(slot.startTime || "");
  }

  function choose(slot: AppointmentSlot) {
    if (isSlotBooked(slot)) return false;
    if (slot.status !== "AVAILABLE" || !slot.remainingCapacity) return false;
    selectedSlot.value = slot;
    return true;
  }

  async function loadVisitors() {
    visitorLoading.value = true;
    try {
      visitors.value = await api.patientVisitors();
    } catch (err) {
      visitors.value = [];
      throw new Error(formatApiError(err, "就诊人加载失败"));
    } finally {
      visitorLoading.value = false;
    }
  }

  return {
    selectedDate,
    selectedDepartmentId,
    selectedVisitorKey,
    selectedSlot,
    selectedVisitor,
    visitors,
    visitorLoading,
    departmentOptions,
    visitorOptions,
    recommendedDepartment,
    selectedTriage,
    recommendedSlotIds,
    displaySlots,
    dateGroups,
    doctorGroups,
    guidanceLabel,
    guidanceValue,
    changeDepartment,
    isSlotBooked,
    choose,
    loadVisitors,
    statusClass,
    statusText,
    slotTimeText,
  };
}

export function visitorKey(visitor: Patient) {
  return `${visitor.visitorType || "ACCOUNT"}:${String(visitor.id)}`;
}

function subjectMatches(row: TriageLike | RegistrationLike, visitor: Patient) {
  const rowType = (row.subjectType || row.visitorType || "ACCOUNT").toUpperCase();
  const rowId = String(row.subjectId ?? row.visitorId ?? "");
  const visitorType = (visitor.visitorType || "ACCOUNT").toUpperCase();
  if (rowType === "ACCOUNT" && !rowId) return visitorType === "ACCOUNT";
  return rowType === visitorType && rowId === String(visitor.id);
}

function visitorLabel(visitor: Patient) {
  const relation = visitor.visitorType === "ACCOUNT" ? "本人" : visitor.relationship || "就诊人";
  return `${visitor.name || "未命名就诊人"}（${relation}）`;
}

function departmentLabel(department: Department) {
  return department.name || "未命名科室";
}

function groupSlotsByDoctor(slots: AppointmentSlot[]) {
  const groups = new Map<string, { doctorName: string; departmentName: string; slots: AppointmentSlot[] }>();
  slots.forEach((slot) => {
    const key = String(slot.doctorId || slot.doctorName || "unknown");
    if (!groups.has(key)) {
      groups.set(key, { doctorName: slot.doctorName || "未命名医生", departmentName: slot.departmentName || "未定科室", slots: [] });
    }
    groups.get(key)!.slots.push(slot);
  });
  return [...groups.entries()].map(([key, group]) => ({
    key,
    doctorName: group.doctorName,
    departmentName: group.departmentName,
    total: group.slots.length,
    periods: periodGroups(group.slots),
  }));
}

function periodGroups(slots: AppointmentSlot[]) {
  const periods = new Map<string, { key: string; label: string; slots: AppointmentSlot[] }>();
  slots.forEach((slot) => {
    const period = slotPeriod(slot.startTime || "");
    if (!periods.has(period.key)) periods.set(period.key, { key: period.key, label: period.label, slots: [] });
    periods.get(period.key)!.slots.push(slot);
  });
  return [...periods.values()].sort((left, right) => periodOrder(left.key) - periodOrder(right.key));
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
