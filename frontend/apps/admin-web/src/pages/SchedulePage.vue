<script setup lang="ts">
import { reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { aiSourceLabel, aiSourceTone, api, displayText, formatApiError, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type Schedule, type ScheduleSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
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

async function loadSchedules() {
  loading.value = true;
  error.value = "";
  try {
    schedules.value = await api.filteredSchedules(auth.token(), {
      startDate: filter.startDate,
      endDate: filter.endDate,
      departmentId: filter.departmentId || undefined,
      doctorId: filter.doctorId || undefined,
      status: filter.status,
    }) as Schedule[];
  } catch (err) {
    error.value = formatApiError(err, "Schedule list failed");
  } finally {
    loading.value = false;
  }
}

async function generate() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    suggestions.value = await api.generateSchedule(auth.token(), { ...generateForm }) as typeof suggestions.value;
    notice.value = `Generated ${suggestions.value.length} AI suggestions`;
  } catch (err) {
    error.value = formatApiError(err, "Schedule generation failed");
  } finally {
    loading.value = false;
  }
}

async function publish() {
  loading.value = true;
  error.value = "";
  try {
    const ids = suggestions.value.map((item) => toNumber(item.id)).filter(Boolean);
    await api.publishSchedule(auth.token(), { suggestionIds: ids });
    suggestions.value = [];
    await loadSchedules();
    emit("refresh");
    notice.value = "Schedules published";
  } catch (err) {
    error.value = formatApiError(err, "Schedule publish failed");
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
    await api.saveSchedule(auth.token(), { ...form, doctorId: toNumber(form.doctorId), departmentId: toNumber(form.departmentId), capacity: toNumber(form.capacity) });
    editorOpen.value = false;
    await loadSchedules();
    notice.value = "Schedule saved";
  } catch (err) {
    error.value = formatApiError(err, "Schedule save failed");
  } finally {
    loading.value = false;
  }
}

async function cancelSchedule(item: Schedule) {
  loading.value = true;
  error.value = "";
  try {
    await api.cancelSchedule(auth.token(), { scheduleId: toNumber(item.id) });
    await loadSchedules();
    notice.value = "Schedule cancelled";
  } catch (err) {
    error.value = formatApiError(err, "Schedule cancel failed");
  } finally {
    loading.value = false;
  }
}

loadSchedules();
</script>

<template>
  <section class="schedule-layout">
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
          <select v-model.number="filter.departmentId"><option :value="0">全部科室</option><option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">{{ displayText(department.name) }}</option></select>
          <select v-model.number="filter.doctorId"><option :value="0">全部医生</option><option v-for="doctor in doctors" :key="String(doctor.id)" :value="toNumber(doctor.id)">{{ displayText(doctor.name) }}</option></select>
          <select v-model="filter.status"><option value="">全部状态</option><option value="PUBLISHED">Published</option><option value="CANCELLED">Cancelled</option></select>
          <button type="button" :disabled="loading" @click="loadSchedules">搜索</button>
        </div>
        <LoadingState v-if="loading" />
        <div v-if="schedules.length" class="table-scroll table-breakout">
          <table class="data-table">
            <thead><tr><th>Date</th><th>Time</th><th>Doctor</th><th>Department</th><th>Capacity</th><th>Booked</th><th>Status</th><th class="actions-cell">Actions</th></tr></thead>
            <tbody>
              <tr v-for="item in pagedSchedules" :key="String(item.id)">
                <td>{{ displayText(item.workDate) }}</td>
                <td>{{ displayText(item.timeRange) }}</td>
                <td>{{ displayText(item.doctorName) }}</td>
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.capacity) }}</td>
                <td>{{ displayText(item.booked, "0") }}</td>
                <td><StatusTag :status="displayText(item.status)" /></td>
                <td class="toolbar"><button type="button" class="action-btn" @click="openEditor(item)">编辑</button><button class="action-btn danger" type="button" :disabled="displayText(item.status) === 'CANCELLED'" @click="cancelSchedule(item)">取消排班</button></td>
              </tr>
            </tbody>
          </table>
          <PaginationBar v-model="schedulePage" :total="scheduleTotal" :page-size="schedulePageSize" />
        </div>
        <EmptyState v-else title="No schedules" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>AI 排班建议</h2></div></header>
      <div class="panel-body stack">
        <div class="form-grid">
          <FormField label="Start date"><input v-model="generateForm.startDate" type="date" /></FormField>
          <FormField label="Days"><input v-model.number="generateForm.days" type="number" min="1" max="14" /></FormField>
        </div>
        <div class="toolbar"><button type="button" :disabled="loading" @click="generate">生成建议</button><button class="primary" type="button" :disabled="loading || !suggestions.length" @click="publish">发布</button></div>
        <article v-for="item in pagedSuggestions" :key="String(item.id)" class="list-row">
          <div class="row-main"><strong>{{ displayText(item.workDate) }} {{ displayText(item.timeRange) }}</strong><p>{{ displayText(item.doctorName) }} / capacity {{ displayText(item.capacity) }}</p><span class="tag" :class="aiSourceTone(item.source)">{{ aiSourceLabel(item.source) }}</span></div>
        </article>
        <PaginationBar v-model="suggestionPage" :total="suggestionTotal" :page-size="suggestionPageSize" />
        <EmptyState v-if="!suggestions.length" title="No AI suggestions" />
      </div>
    </aside>
    <Modal :open="editorOpen" title="Schedule" description="Create or update a real doctor schedule and its appointment slot." @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="Doctor"><select v-model.number="form.doctorId" @change="syncDoctorDepartment"><option v-for="doctor in doctors" :key="String(doctor.id)" :value="toNumber(doctor.id)">{{ displayText(doctor.name) }} / {{ displayText(doctor.departmentName) }}</option></select></FormField>
          <FormField label="Department"><select v-model.number="form.departmentId" disabled><option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">{{ displayText(department.name) }}</option></select></FormField>
          <FormField label="Date"><input v-model="form.workDate" type="date" /></FormField>
          <FormField label="Time range"><input v-model.trim="form.timeRange" placeholder="09:00-12:00" /></FormField>
          <FormField label="Capacity"><input v-model.number="form.capacity" type="number" min="1" max="100" /></FormField>
          <FormField label="Status"><select v-model="form.status"><option value="PUBLISHED">Published</option><option value="CANCELLED">Cancelled</option></select></FormField>
        </div>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">取消</button><button class="primary" type="button" :disabled="loading" @click="saveSchedule">保存</button></template>
    </Modal>
  </section>
</template>
