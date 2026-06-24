<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type DataRow, type DeviceSaveRequest, type DeviceUsageSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { departments } = storeToRefs(workflow);
const rows = ref<DataRow[]>([]);
const usages = ref<DataRow[]>([]);
const keyword = ref("");
const status = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const editorOpen = ref(false);
const usageOpen = ref(false);
const selectedDevice = ref<DataRow | null>(null);
const form = reactive<DeviceSaveRequest>({ deviceCode: "", name: "", category: "", departmentId: 0, location: "", status: "AVAILABLE", purchaseDate: "", remark: "" });
const usageForm = reactive<DeviceUsageSaveRequest>({ deviceId: 0, usageType: "USE", usedBy: "", patientId: 0, resultStatus: "NORMAL", remark: "" });
const filtered = computed(() => rows.value.filter((item) => {
  const q = keyword.value.toLowerCase();
  const haystack = `${fieldText(item, "deviceCode", "")} ${fieldText(item, "name", "")} ${fieldText(item, "category", "")} ${fieldText(item, "location", "")}`.toLowerCase();
  return (!q || haystack.includes(q)) && (!status.value || fieldText(item, "status") === status.value);
}));
const { currentPage, pageSize, total, pageRows } = usePagination(filtered, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.devices(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "Device list failed");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: DataRow) {
  selectedDevice.value = item ?? null;
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.deviceCode = fieldText(item, "deviceCode", "");
  form.name = fieldText(item, "name", "");
  form.category = fieldText(item, "category", "");
  form.departmentId = toNumber(item?.departmentId, toNumber(departments.value[0]?.id, 0));
  form.location = fieldText(item, "location", "");
  form.status = fieldText(item, "status", "AVAILABLE");
  form.purchaseDate = fieldText(item, "purchaseDate", "");
  form.remark = fieldText(item, "remark", "");
  editorOpen.value = true;
}

async function save() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.saveDevice(auth.token(), { ...form, departmentId: toNumber(form.departmentId) || undefined });
    editorOpen.value = false;
    notice.value = "Device saved";
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "Device save failed");
  } finally {
    loading.value = false;
  }
}

async function changeStatus(item: DataRow, nextStatus: string) {
  loading.value = true;
  error.value = "";
  try {
    await api.updateDeviceStatus(auth.token(), { deviceId: toNumber(item.id), status: nextStatus });
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "Device status failed");
  } finally {
    loading.value = false;
  }
}

async function openUsage(item: DataRow) {
  selectedDevice.value = item;
  usageForm.deviceId = toNumber(item.id);
  usageForm.usageType = "USE";
  usageForm.usedBy = "";
  usageForm.patientId = 0;
  usageForm.resultStatus = "NORMAL";
  usageForm.remark = "";
  usages.value = await api.deviceUsages(auth.token(), usageForm.deviceId);
  usageOpen.value = true;
}

async function saveUsage() {
  loading.value = true;
  error.value = "";
  try {
    await api.saveDeviceUsage(auth.token(), { ...usageForm, patientId: usageForm.patientId ? toNumber(usageForm.patientId) : undefined });
    usages.value = await api.deviceUsages(auth.token(), usageForm.deviceId);
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "Usage save failed");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><h2>设备管理</h2></div>
      <button class="primary" type="button" @click="openEditor()">新增设备</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <div class="admin-filter-row">
        <input v-model.trim="keyword" placeholder="Search code, name, category, location" />
        <select v-model="status"><option value="">All status</option><option value="AVAILABLE">Available</option><option value="IN_USE">In use</option><option value="MAINTENANCE">Maintenance</option><option value="RETIRED">Retired</option></select>
      </div>
      <div v-if="filtered.length" class="table-scroll">
        <table class="data-table">
          <thead><tr><th>Code</th><th>Name</th><th>Category</th><th>Department</th><th>Status</th><th>Usage</th><th class="actions-cell">Actions</th></tr></thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.id)">
              <td>{{ fieldText(item, "deviceCode") }}</td>
              <td>{{ fieldText(item, "name") }}</td>
              <td>{{ fieldText(item, "category") }}</td>
              <td>{{ fieldText(item, "departmentName") }}</td>
              <td><StatusTag :status="fieldText(item, 'status')" /></td>
              <td>{{ fieldText(item, "usageCount", "0") }} / abnormal {{ fieldText(item, "abnormalCount", "0") }}</td>
              <td class="toolbar">
                <button type="button" @click="openEditor(item)">编辑</button>
                <button type="button" @click="openUsage(item)">使用记录</button>
                <button type="button" @click="changeStatus(item, 'MAINTENANCE')">维护</button>
                <button class="danger" type="button" @click="changeStatus(item, 'RETIRED')">停用</button>
              </td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="No devices" />
    </div>
    <Modal :open="editorOpen" title="Device" description="Save a real device record." @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="Code"><input v-model.trim="form.deviceCode" /></FormField>
          <FormField label="Name"><input v-model.trim="form.name" /></FormField>
          <FormField label="Category"><input v-model.trim="form.category" /></FormField>
          <FormField label="Department"><select v-model.number="form.departmentId"><option :value="0">None</option><option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">{{ fieldText(department, "name") }}</option></select></FormField>
          <FormField label="Location"><input v-model.trim="form.location" /></FormField>
          <FormField label="Status"><select v-model="form.status"><option value="AVAILABLE">Available</option><option value="IN_USE">In use</option><option value="MAINTENANCE">Maintenance</option><option value="RETIRED">Retired</option></select></FormField>
          <FormField label="Purchase date"><input v-model="form.purchaseDate" type="date" /></FormField>
        </div>
        <FormField label="Remark"><textarea v-model.trim="form.remark" /></FormField>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">取消</button><button class="primary" type="button" :disabled="loading" @click="save">保存</button></template>
    </Modal>
    <Modal :open="usageOpen" title="Device Usage" description="Record use, maintenance and abnormal result." @close="usageOpen = false">
      <div class="stack">
        <strong>{{ fieldText(selectedDevice, "name") }}</strong>
        <div class="form-grid">
          <FormField label="Type"><select v-model="usageForm.usageType"><option value="USE">Use</option><option value="MAINTENANCE">Maintenance</option><option value="REPAIR">Repair</option></select></FormField>
          <FormField label="Operator"><input v-model.trim="usageForm.usedBy" /></FormField>
          <FormField label="Patient ID"><input v-model.number="usageForm.patientId" type="number" /></FormField>
          <FormField label="Result"><select v-model="usageForm.resultStatus"><option value="NORMAL">Normal</option><option value="ABNORMAL">Abnormal</option><option value="FAILED">Failed</option></select></FormField>
        </div>
        <FormField label="Remark"><textarea v-model.trim="usageForm.remark" /></FormField>
        <button class="primary" type="button" :disabled="loading" @click="saveUsage">保存使用记录</button>
        <div class="list">
          <article v-for="item in usages.slice(0, 8)" :key="String(item.id)" class="list-row">
            <div class="row-main"><strong>{{ fieldText(item, "usageType") }} / {{ fieldText(item, "resultStatus") }}</strong><p>{{ fieldText(item, "startedAt") }} - {{ fieldText(item, "remark", "") }}</p></div>
          </article>
        </div>
      </div>
    </Modal>
  </section>
</template>
