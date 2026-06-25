<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, displayText, formatApiError, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type Device, type DeviceSaveRequest, type DeviceUsage, type DeviceUsageSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { departments } = storeToRefs(workflow);
const rows = ref<Device[]>([]);
const usages = ref<DeviceUsage[]>([]);
const keyword = ref("");
const status = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const editorOpen = ref(false);
const usageOpen = ref(false);
const selectedDevice = ref<Device | null>(null);
const form = reactive<DeviceSaveRequest>({ deviceCode: "", name: "", category: "", departmentId: 0, location: "", status: "AVAILABLE", purchaseDate: "", remark: "" });
const usageForm = reactive<DeviceUsageSaveRequest>({ deviceId: 0, usageType: "USE", usedBy: "", patientId: 0, resultStatus: "NORMAL", remark: "" });
const filtered = computed(() => rows.value.filter((item) => {
  const q = keyword.value.toLowerCase();
  const haystack = `${displayText(item.deviceCode, "")} ${displayText(item.name, "")} ${displayText(item.category, "")} ${displayText(item.location, "")}`.toLowerCase();
  return (!q || haystack.includes(q)) && (!status.value || displayText(item.status) === status.value);
}));
const { currentPage, pageSize, total, pageRows } = usePagination(filtered, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.devices(auth.token()) as Device[];
  } catch (err) {
    error.value = formatApiError(err, "加载设备列表失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: Device) {
  selectedDevice.value = item ?? null;
  form.id = item ? toNumber(item.id, undefined) : undefined;
  form.deviceCode = displayText(item?.deviceCode, "");
  form.name = displayText(item?.name, "");
  form.category = displayText(item?.category, "");
  form.departmentId = toNumber(item?.departmentId, toNumber(departments.value[0]?.id, 0));
  form.location = displayText(item?.location, "");
  form.status = displayText(item?.status, "AVAILABLE");
  form.purchaseDate = displayText(item?.purchaseDate, "");
  form.remark = displayText(item?.remark, "");
  editorOpen.value = true;
}

async function save() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.saveDevice(auth.token(), { ...form, departmentId: toNumber(form.departmentId) || undefined });
    editorOpen.value = false;
    notice.value = "设备已保存";
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "保存设备失败");
  } finally {
    loading.value = false;
  }
}

async function changeStatus(item: Device, nextStatus: string) {
  loading.value = true;
  error.value = "";
  try {
    await api.updateDeviceStatus(auth.token(), { deviceId: toNumber(item.id), status: nextStatus });
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "更新设备状态失败");
  } finally {
    loading.value = false;
  }
}

async function openUsage(item: Device) {
  selectedDevice.value = item;
  usageForm.deviceId = toNumber(item.id);
  usageForm.usageType = "USE";
  usageForm.usedBy = "";
  usageForm.patientId = 0;
  usageForm.resultStatus = "NORMAL";
  usageForm.remark = "";
  usages.value = await api.deviceUsages(auth.token(), usageForm.deviceId) as DeviceUsage[];
  usageOpen.value = true;
}

async function saveUsage() {
  loading.value = true;
  error.value = "";
  try {
    await api.saveDeviceUsage(auth.token(), { ...usageForm, patientId: usageForm.patientId ? toNumber(usageForm.patientId) : undefined });
    usages.value = await api.deviceUsages(auth.token(), usageForm.deviceId) as DeviceUsage[];
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "保存使用记录失败");
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
        <input v-model.trim="keyword" placeholder="搜索编号、名称、类别、位置" />
        <select v-model="status"><option value="">全部状态</option><option value="AVAILABLE">可用</option><option value="IN_USE">使用中</option><option value="MAINTENANCE">维修中</option><option value="RETIRED">已停用</option></select>
      </div>
      <div v-if="filtered.length" class="table-scroll table-breakout">
        <table class="data-table">
          <thead><tr><th>编号</th><th>名称</th><th>类别</th><th>科室</th><th>状态</th><th>使用量</th><th class="actions-cell">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.id)">
              <td>{{ displayText(item.deviceCode) }}</td>
              <td>{{ displayText(item.name) }}</td>
              <td>{{ displayText(item.category) }}</td>
              <td>{{ displayText(item.departmentName) }}</td>
              <td><StatusTag :status="displayText(item.status)" /></td>
              <td>{{ displayText(item.usageCount, "0") }} / 异常 {{ displayText(item.abnormalCount, "0") }}</td>
              <td class="toolbar">
                <button type="button" class="action-btn" @click="openEditor(item)">编辑</button>
                <button type="button" class="action-btn" @click="openUsage(item)">使用记录</button>
                <button type="button" class="action-btn" @click="changeStatus(item, 'MAINTENANCE')">维护</button>
                <button class="action-btn danger" type="button" @click="changeStatus(item, 'RETIRED')">停用</button>
              </td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="暂无设备" />
    </div>
    <Modal :open="editorOpen" title="设备编辑" description="填写或修改设备信息。" @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="编号"><input v-model.trim="form.deviceCode" /></FormField>
          <FormField label="名称"><input v-model.trim="form.name" /></FormField>
          <FormField label="类别"><input v-model.trim="form.category" /></FormField>
          <FormField label="科室"><select v-model.number="form.departmentId"><option :value="0">无</option><option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">{{ displayText(department.name) }}</option></select></FormField>
          <FormField label="位置"><input v-model.trim="form.location" /></FormField>
          <FormField label="状态"><select v-model="form.status"><option value="AVAILABLE">可用</option><option value="IN_USE">使用中</option><option value="MAINTENANCE">维修中</option><option value="RETIRED">已停用</option></select></FormField>
          <FormField label="采购日期"><input v-model="form.purchaseDate" type="date" /></FormField>
        </div>
        <FormField label="备注"><textarea v-model.trim="form.remark" /></FormField>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">取消</button><button class="primary" type="button" :disabled="loading" @click="save">保存</button></template>
    </Modal>
    <Modal :open="usageOpen" title="设备使用记录" description="记录使用、维修和异常结果。" @close="usageOpen = false">
      <div class="stack">
        <strong>{{ displayText(selectedDevice?.name) }}</strong>
        <div class="form-grid">
          <FormField label="类型"><select v-model="usageForm.usageType"><option value="USE">使用</option><option value="MAINTENANCE">维修</option><option value="REPAIR">修理</option></select></FormField>
          <FormField label="操作人"><input v-model.trim="usageForm.usedBy" /></FormField>
          <FormField label="患者 ID"><input v-model.number="usageForm.patientId" type="number" /></FormField>
          <FormField label="结果"><select v-model="usageForm.resultStatus"><option value="NORMAL">正常</option><option value="ABNORMAL">异常</option><option value="FAILED">失败</option></select></FormField>
        </div>
        <FormField label="备注"><textarea v-model.trim="usageForm.remark" /></FormField>
        <button class="primary" type="button" :disabled="loading" @click="saveUsage">保存使用记录</button>
        <div class="list">
          <article v-for="item in usages.slice(0, 8)" :key="String(item.id)" class="list-row">
            <div class="row-main"><strong>{{ displayText(item.usageType) }} / {{ displayText(item.resultStatus) }}</strong><p>{{ displayText(item.startedAt) }} - {{ displayText(item.remark, "") }}</p></div>
          </article>
        </div>
      </div>
    </Modal>
  </section>
</template>
