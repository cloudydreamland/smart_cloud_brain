<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { aiSourceLabel, aiSourceTone, api, displayText, formatApiError, statusClass, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type Schedule, type ScheduleSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState, Modal, PaginationBar, ScbSelect, StatusTag } from "@smart-cloud-brain/shared-ui";

const emit = defineEmits<{ refresh: [] }>();
const workflow = useAdminWorkflowStore();
const { suggestions, schedules, doctors, departments } = storeToRefs(workflow);
const generateForm = reactive({ startDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10), days: 3 });
const filter = reactive({ startDate: new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10), endDate: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10), departmentId: 0, doctorId: 0, status: "" });
const form = reactive<ScheduleSaveRequest>({ doctorId: 0, departmentId: 0, workDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10), timeRange: "09:00-12:00", capacity: 20, status: "PUBLISHED" });
const loading = ref(false);
const error = ref("");
const notice = ref("");
const editorOpen = ref(false);
const { currentPage: schedulePage, pageSize: schedulePageSize, total: scheduleTotal, pageRows: pagedSchedules } = usePagination(schedules, 8);
const { currentPage: suggestionPage, pageSize: suggestionPageSize, total: suggestionTotal, pageRows: pagedSuggestions } = usePagination(suggestions, 5);

const departmentOptions = computed(() => [
  { value: 0, label: "全部科室" },
  ...departments.value.map((d) => ({ value: toNumber(d.id), label: displayText(d.name) })),
]);
const doctorOptions = computed(() => [
  { value: 0, label: "全部医生" },
  ...doctors.value.map((d) => ({ value: toNumber(d.id), label: `${displayText(d.name)} / ${displayText(d.departmentName)}` })),
]);
const doctorOptionsNoAll = computed(() =>
  doctors.value.map((d) => ({ value: toNumber(d.id), label: `${displayText(d.name)} / ${displayText(d.departmentName)}` }))
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
  loading.value = true;
  error.value = "";
  try {
    schedules.value = await api.filteredSchedules({
      startDate: filter.startDate,
      endDate: filter.endDate,
      departmentId: filter.departmentId || undefined,
      doctorId: filter.doctorId || undefined,
      status: filter.status,
    }) as Schedule[];
  } catch (err) {
    error.value = formatApiError(err, "加载排班列表失败");
  } finally {
    loading.value = false;
  }
}

async function generate() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    suggestions.value = await api.generateSchedule({ ...generateForm }) as typeof suggestions.value;
    notice.value = `已生成 ${suggestions.value.length} 条 AI 建议`;
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
    notice.value = "排班已发布";
  } catch (err) {
    error.value = formatApiError(err, "发布排班失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: Schedule) {
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.doctorId = toNumber(item?.doctorId, toNumber(doctors.value[0]?.id));
  form.departmentId = toNumber(item?.departmentId, toNumber(doctors.value.find((doctor) => toNumber(doctor.id) === form.doctorId)?.departmentId, toNumber(departments.value[0]?.id)));
  form.workDate = displayText(item?.workDate, new Date(Date.now() + 86400000).toISOString().slice(0, 10));
  form.timeRange = displayText(item?.timeRange, "09:00-12:00");
  form.capacity = toNumber(item?.capacity, 20);
  form.status = displayText(item?.status, "PUBLISHED");
  editorOpen.value = true;
}

function syncDoctorDepartment() {
  const doctor = doctors.value.find((item) => toNumber(item.id) === toNumber(form.doctorId));
  if (doctor) form.departmentId = toNumber(doctor.departmentId);
}

async function saveSchedule() {
  loading.value = true;
  error.value = "";
  try {
    await api.saveSchedule({ ...form, doctorId: toNumber(form.doctorId), departmentId: toNumber(form.departmentId), capacity: toNumber(form.capacity) });
    editorOpen.value = false;
    await loadSchedules();
    notice.value = "排班已保存";
  } catch (err) {
    error.value = formatApiError(err, "保存排班失败");
  } finally {
    loading.value = false;
  }
}

async function cancelSchedule(item: Schedule) {
  loading.value = true;
  error.value = "";
  try {
    await api.cancelSchedule({ scheduleId: toNumber(item.id) });
    await loadSchedules();
    notice.value = "排班已取消";
  } catch (err) {
    error.value = formatApiError(err, "取消排班失败");
  } finally {
    loading.value = false;
  }
}

loadSchedules();
</script>

<template>
  <section class="schedule-layout">
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>AI 排班建议</h2></div></header>
      <div class="panel-body stack">
        <div class="admin-filter-row">
          <input v-model="generateForm.startDate" type="date" />
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
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="admin-filter-row">
          <input v-model="filter.startDate" type="date" />
          <input v-model="filter.endDate" type="date" />
          <ScbSelect v-model="filter.departmentId" :options="departmentOptions" />
          <ScbSelect v-model="filter.doctorId" :options="doctorOptions" />
          <ScbSelect v-model="filter.status" :options="filterStatusOptions" />
          <button type="button" :disabled="loading" @click="loadSchedules">搜索</button>
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
                <td class="toolbar"><button type="button" class="action-btn" @click="openEditor(item)">编辑</button><button class="action-btn danger" type="button" :disabled="displayText(item.status) === 'CANCELLED'" @click="cancelSchedule(item)">取消排班</button></td>
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
          <FormField label="日期"><input v-model="form.workDate" type="date" /></FormField>
          <FormField label="时段"><input v-model.trim="form.timeRange" placeholder="09:00-12:00" /></FormField>
          <FormField label="容量"><input v-model.number="form.capacity" type="number" min="1" max="100" /></FormField>
          <FormField label="状态"><ScbSelect v-model="form.status" :options="formStatusOptions" /></FormField>
        </div>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">取消</button><button class="primary" type="button" :disabled="loading" @click="saveSchedule">保存</button></template>
    </Modal>
  </section>
</template>
