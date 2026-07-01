<script setup lang="ts">
import { computed, inject, reactive, ref, watch, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { aiSourceLabel, aiSourceTone, api, displayText, formatApiError, statusClass, toNumber, useAdminWorkflowStore, usePagination, type Doctor, type Schedule, type ScheduleSaveRequest } from "@smart-cloud-brain/shared-api";
import { DatePicker, EmptyState, ErrorState, FormField, LoadingState, Modal, PaginationBar, ScbSelect, StatusTag, TimeRangePicker, Toast } from "@smart-cloud-brain/shared-ui";
import ConfirmDialog from "@smart-cloud-brain/shared-ui/src/components/ConfirmDialog.vue";
import DepartmentDoctorCascader from "../components/DepartmentDoctorCascader.vue";

const emit = defineEmits<{ refresh: [] }>();
const workflow = useAdminWorkflowStore();
const { suggestions, doctors, departments } = storeToRefs(workflow);
function toLocalDateStr(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}
const DAY_MS = 86_400_000;
const schedules = ref<Schedule[]>([]);
const scheduleDoctorCache = ref<Doctor[]>([]);
const generateForm = reactive({ startDate: toLocalDateStr(new Date(Date.now() + DAY_MS)), days: 3 });
const filter = reactive({ startDate: toLocalDateStr(new Date(Date.now() - 7 * DAY_MS)), endDate: toLocalDateStr(new Date(Date.now() + 30 * DAY_MS)), departmentId: 0, doctorId: 0, status: "" });
const form = reactive<ScheduleSaveRequest>({ doctorId: 0, departmentId: 0, workDate: toLocalDateStr(new Date(Date.now() + DAY_MS)), timeRange: "09:00-12:00", capacity: 20, status: "PUBLISHED" });
const loading = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>(("toast"));
const editorOpen = ref(false);
const cancelTarget = ref<Schedule | null>(null);
const cancelConfirmOpen = ref(false);
let scheduleRequestId = 0;
const { currentPage: schedulePage, pageSize: schedulePageSize, total: scheduleTotal, pageRows: pagedSchedules } = usePagination(schedules, 8);
const { currentPage: suggestionPage, pageSize: suggestionPageSize, total: suggestionTotal, pageRows: pagedSuggestions } = usePagination(suggestions, 5);

const departmentOptions = computed(() => [
  { value: 0, label: "全部科室" },
  ...departments.value.map((d) => ({ value: toNumber(d.id), label: displayText(d.name) })),
]);
const filterDoctors = computed(() => {
  const merged = new Map<string, Doctor>();
  scheduleDoctorCache.value.forEach((doctor) => merged.set(`${doctor.id}:${doctor.departmentId}`, doctor));
  doctors.value.forEach((doctor) => merged.set(`${doctor.id}:${doctor.departmentId}`, doctor));
  return Array.from(merged.values());
});
const editorDoctors = computed(() => {
  const merged = new Map<number, Doctor>();
  filterDoctors.value.forEach((doctor) => {
    if (!merged.has(toNumber(doctor.id))) merged.set(toNumber(doctor.id), doctor);
  });
  doctors.value.forEach((doctor) => merged.set(toNumber(doctor.id), doctor));
  return Array.from(merged.values());
});
const doctorOptionsNoAll = computed(() =>
  editorDoctors.value.map((d) => ({ value: toNumber(d.id), label: `${displayText(d.name)} / ${displayText(d.departmentName)}` }))
);
const filterStatusOptions = [
  { value: "", label: "全部状态" },
  { value: "PUBLISHED", label: "已发布" },
  { value: "CANCELLED", label: "已取消" },
];
const formStatusOptions = [
  { value: "PUBLISHED", label: "已发布" },
  { value: "CANCELLED", label: "已取消" },
];

async function loadSchedules() {
  const requestId = ++scheduleRequestId;
  loading.value = true;
  error.value = "";
  try {
    const nextSchedules = await api.filteredSchedules({
      startDate: filter.startDate,
      endDate: filter.endDate,
      departmentId: filter.departmentId || undefined,
      doctorId: filter.doctorId || undefined,
      status: filter.status,
    }) as Schedule[];
    if (requestId === scheduleRequestId) {
      const discoveredDoctors = nextSchedules
        .filter((item) => item.doctorId && item.departmentId && item.doctorName)
        .map((item) => ({
          id: toNumber(item.doctorId),
          name: displayText(item.doctorName),
          phone: "",
          departmentId: toNumber(item.departmentId),
          departmentName: displayText(item.departmentName),
        }));
      const doctorCache = new Map(scheduleDoctorCache.value.map((doctor) => [`${doctor.id}:${doctor.departmentId}`, doctor]));
      discoveredDoctors.forEach((doctor) => doctorCache.set(`${doctor.id}:${doctor.departmentId}`, doctor));
      scheduleDoctorCache.value = Array.from(doctorCache.values());
      schedules.value = nextSchedules;
    }
  } catch (err) {
    if (requestId === scheduleRequestId) error.value = formatApiError(err, "加载排班列表失败");
  } finally {
    if (requestId === scheduleRequestId) loading.value = false;
  }
}

function updateFilterStartDate(value: string) {
  filter.startDate = value;
  if (value && filter.endDate && value > filter.endDate) filter.endDate = value;
}

function updateFilterEndDate(value: string) {
  filter.endDate = value;
  if (value && filter.startDate && value < filter.startDate) filter.startDate = value;
}

async function generate() {
  loading.value = true;
  error.value = "";
  try {
    suggestions.value = await api.generateSchedule({ ...generateForm }) as typeof suggestions.value;
    toast?.value?.success("AI 建议", `已生成 ${suggestions.value.length} 条 AI 建议`);
  } catch (err) {
    error.value = formatApiError(err, "生成排班建议失败");
  } finally {
    loading.value = false;
  }
}

async function publish() {
  loading.value = true;
  error.value = "";
  try {
    const ids = suggestions.value.map((item) => toNumber(item.id)).filter(Boolean);
    await api.publishSchedule({ suggestionIds: ids });
    suggestions.value = [];
    await loadSchedules();
    emit("refresh");
    toast?.value?.success("操作成功", "排班已发布");
  } catch (err) {
    error.value = formatApiError(err, "发布排班失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: Schedule) {
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.doctorId = toNumber(item?.doctorId, toNumber(editorDoctors.value[0]?.id));
  form.departmentId = toNumber(item?.departmentId, toNumber(editorDoctors.value.find((doctor) => toNumber(doctor.id) === form.doctorId)?.departmentId, toNumber(departments.value[0]?.id)));
  form.workDate = displayText(item?.workDate, new Date(Date.now() + 86400000).toISOString().slice(0, 10));
  form.timeRange = displayText(item?.timeRange, "09:00-12:00");
  form.capacity = toNumber(item?.capacity, 20);
  form.status = displayText(item?.status, "PUBLISHED");
  editorOpen.value = true;
}

function syncDoctorDepartment() {
  const doctor = editorDoctors.value.find((item) => toNumber(item.id) === toNumber(form.doctorId));
  if (doctor) form.departmentId = toNumber(doctor.departmentId);
}

async function saveSchedule() {
  loading.value = true;
  error.value = "";
  try {
    await api.saveSchedule({ ...form, doctorId: toNumber(form.doctorId), departmentId: toNumber(form.departmentId), capacity: toNumber(form.capacity) });
    editorOpen.value = false;
    await loadSchedules();
    toast?.value?.success("保存成功", "排班已保存");
  } catch (err) {
    error.value = formatApiError(err, "保存排班失败");
  } finally {
    loading.value = false;
  }
}

function requestCancel(item: Schedule) {
  cancelTarget.value = item;
  cancelConfirmOpen.value = true;
}

async function confirmCancel() {
  const item = cancelTarget.value;
  if (!item) return;
  cancelConfirmOpen.value = false;
  loading.value = true;
  error.value = "";
  try {
    await api.cancelSchedule({ scheduleId: toNumber(item.id) });
    await loadSchedules();
    toast?.value?.success("操作成功", "排班已取消");
  } catch (err) {
    error.value = formatApiError(err, "取消排班失败");
  } finally {
    loading.value = false;
  }
}

watch(
  () => [filter.startDate, filter.endDate, filter.departmentId, filter.doctorId, filter.status],
  () => {
    schedulePage.value = 1;
    void loadSchedules();
  },
  { immediate: true }
);
</script>

<template>
  <section class="schedule-layout">
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>AI 排班建议</h2></div></header>
      <div class="panel-body stack">
        <div class="admin-filter-row">
          <DatePicker v-model="generateForm.startDate" aria-label="AI 建议开始日期" :clearable="false" />
          <input v-model.number="generateForm.days" type="number" min="1" max="14" placeholder="天数" />
          <button type="button" :disabled="loading" @click="generate">生成建议</button>
          <button class="primary" type="button" :disabled="loading || !suggestions.length" @click="publish">发布</button>
        </div>
        <article v-for="item in pagedSuggestions" :key="String(item.id)" class="list-row">
          <div class="row-main"><strong>{{ displayText(item.workDate) }} {{ displayText(item.timeRange) }}</strong><p>{{ displayText(item.doctorName) }} / 容量 {{ displayText(item.capacity) }}</p><span class="tag" :class="aiSourceTone(item.source)">{{ aiSourceLabel(item.source) }}</span></div>
        </article>
        <PaginationBar v-model="suggestionPage" :total="suggestionTotal" :page-size="suggestionPageSize" />
        <EmptyState v-if="!suggestions.length" title="暂无 AI 建议" />
      </div>
    </aside>
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title"><h2>排班管理</h2></div>
        <button class="primary" type="button" @click="openEditor()">新增排班</button>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div class="admin-filter-row">
          <DatePicker :model-value="filter.startDate" aria-label="筛选开始日期" :clearable="false" @update:model-value="updateFilterStartDate" />
          <DatePicker :model-value="filter.endDate" aria-label="筛选结束日期" :clearable="false" @update:model-value="updateFilterEndDate" />
          <DepartmentDoctorCascader
            v-model:department-id="filter.departmentId"
            v-model:doctor-id="filter.doctorId"
            :departments="departments"
            :doctors="filterDoctors"
          />
          <ScbSelect v-model="filter.status" :options="filterStatusOptions" />
        </div>
        <LoadingState v-if="loading" />
        <template v-if="schedules.length">
        <div class="table-scroll table-breakout">
          <table class="data-table">
            <thead><tr><th>日期</th><th>时间</th><th>医生</th><th>科室</th><th>容量</th><th>已预约</th><th>状态</th><th class="actions-cell">操作</th></tr></thead>
            <tbody>
              <tr v-for="item in pagedSchedules" :key="String(item.id)">
                <td>{{ displayText(item.workDate) }}</td>
                <td>{{ displayText(item.timeRange) }}</td>
                <td>{{ displayText(item.doctorName) }}</td>
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.capacity) }}</td>
                <td>{{ displayText(item.booked, "0") }}</td>
                <td><StatusTag :status="displayText(item.status)" :tone="statusClass(item.status)" /></td>
                <td class="toolbar"><button type="button" class="action-btn" @click="openEditor(item)">编辑</button><button class="action-btn danger" type="button" :disabled="displayText(item.status) === 'CANCELLED'" @click="requestCancel(item)">取消排班</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <PaginationBar v-model="schedulePage" :total="scheduleTotal" :page-size="schedulePageSize" />
        </template>
        <EmptyState v-else title="暂无排班" />
      </div>
    </section>
    <Modal :open="editorOpen" title="排班编辑" description="创建或修改医生排班及预约时段。" @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="医生"><ScbSelect v-model="form.doctorId" :options="doctorOptionsNoAll" @update:modelValue="syncDoctorDepartment" /></FormField>
          <FormField label="科室"><ScbSelect v-model="form.departmentId" :options="departmentOptions" :disabled="true" /></FormField>
          <FormField label="日期"><DatePicker v-model="form.workDate" aria-label="排班日期" :clearable="false" /></FormField>
          <FormField label="时段"><TimeRangePicker v-model="form.timeRange" /></FormField>
          <FormField label="容量"><input v-model.number="form.capacity" type="number" min="1" max="100" /></FormField>
          <FormField label="状态"><ScbSelect v-model="form.status" :options="formStatusOptions" /></FormField>
        </div>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">取消</button><button class="primary" type="button" :disabled="loading" @click="saveSchedule">保存</button></template>
    </Modal>
    <ConfirmDialog :open="cancelConfirmOpen" title="确认取消排班" :description="`确定要取消 ${cancelTarget?.workDate} ${cancelTarget?.timeRange} 的排班吗？此操作不可撤销。`" confirm-text="确认取消" @confirm="confirmCancel" @close="cancelConfirmOpen = false" />
  </section>
</template>
