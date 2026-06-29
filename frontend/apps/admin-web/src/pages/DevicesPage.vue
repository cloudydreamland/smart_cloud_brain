<script setup lang="ts">
import { computed, inject, reactive, ref, type Ref } from "vue";
import { storeToRefs } from "pinia";
import { api, displayText, formatApiError, statusClass, toNumber, useAdminWorkflowStore, useAuthStore, usePagination, type Device, type DeviceSaveRequest, type DeviceUsage, type DeviceUsageSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, Modal, PaginationBar, ScbSelect, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";

const workflow = useAdminWorkflowStore();
const { departments } = storeToRefs(workflow);
const rows = ref<Device[]>([]);
const usages = ref<DeviceUsage[]>([]);
const keyword = ref("");
const status = ref("");
const loading = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const editorOpen = ref(false);
const usageOpen = ref(false);
const selectedDevice = ref<Device | null>(null);
const form = reactive<DeviceSaveRequest>({ deviceCode: "", name: "", category: "", departmentId: 0, location: "", status: "AVAILABLE", purchaseDate: "", remark: "" });
const usageForm = reactive<DeviceUsageSaveRequest>({ deviceId: 0, usageType: "USE", usedBy: "", patientId: 0, resultStatus: "NORMAL", remark: "" });
const retireTarget = ref<Device | null>(null);
const retireOpen = ref(false);
const filtered = computed(() => rows.value.filter((item) => {
  const q = keyword.value.toLowerCase();
  const haystack = `${displayText(item.deviceCode, "")} ${displayText(item.name, "")} ${displayText(item.category, "")} ${displayText(item.location, "")}`.toLowerCase();
  return (!q || haystack.includes(q)) && (!status.value || displayText(item.status) === status.value);
}));
const { currentPage, pageSize, total, pageRows } = usePagination(filtered, 8);

const deviceStatusFilterOptions = [
  { value: "", label: "全部状态" },
  { value: "AVAILABLE", label: "可用" },
  { value: "IN_USE", label: "使用中" },
  { value: "MAINTENANCE", label: "维修中" },
  { value: "RETIRED", label: "已停用" },
];
const deviceDeptOptions = computed(() => [
  { value: 0, label: "无" },
  ...departments.value.map((d) => ({ value: toNumber(d.id), label: displayText(d.name) })),
]);
const deviceFormStatusOptions = [
  { value: "AVAILABLE", label: "可用" },
  { value: "IN_USE", label: "使用中" },
  { value: "MAINTENANCE", label: "维修中" },
  { value: "RETIRED", label: "已停用" },
];
const usageTypeOptions = [
  { value: "USE", label: "使用" },
  { value: "MAINTENANCE", label: "维修" },
  { value: "REPAIR", label: "修理" },
];
const resultStatusOptions = [
  { value: "NORMAL", label: "正常" },
  { value: "ABNORMAL", label: "异常" },
  { value: "FAILED", label: "失败" },
];

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.devices() as Device[];
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
  try {
    await api.saveDevice({ ...form, departmentId: toNumber(form.departmentId) || undefined });
    editorOpen.value = false;
    toast?.value?.success("保存成功", "设备已保存");
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
    await api.updateDeviceStatus({ deviceId: toNumber(item.id), status: nextStatus });
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
  usages.value = await api.deviceUsages(usageForm.deviceId) as DeviceUsage[];
  usageOpen.value = true;
}

async function saveUsage() {
  loading.value = true;
  error.value = "";
  try {
    await api.saveDeviceUsage({ ...usageForm, patientId: usageForm.patientId ? toNumber(usageForm.patientId) : undefined });
    usages.value = await api.deviceUsages(usageForm.deviceId) as DeviceUsage[];
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "保存使用记录失败");
  } finally {
    loading.value = false;
  }
}

function deviceActions(item: Device) {
  const s = String(item.status || "").toUpperCase();
  const actions: { key: string; label: string; danger?: boolean }[] = [];
  actions.push({ key: "edit", label: "编辑" });
  if (s === "RETIRED") return actions;
  actions.push({ key: "usage", label: "使用记录" });
  if (s === "MAINTENANCE") {
    actions.push({ key: "restore", label: "恢复可用" });
  } else {
    actions.push({ key: "maintain", label: "维护" });
  }
  actions.push({ key: "retire", label: "停用", danger: true });
  return actions;
}

function handleAction(key: string, item: Device) {
  switch (key) {
    case "edit":    return openEditor(item);
    case "usage":   return openUsage(item);
    case "maintain": return changeStatus(item, "MAINTENANCE");
    case "restore":  return changeStatus(item, "AVAILABLE");
    case "retire":   return confirmRetire(item);
  }
}

function confirmRetire(item: Device) {
  retireTarget.value = item;
  retireOpen.value = true;
}

async function doRetire() {
  if (!retireTarget.value) return;
  retireOpen.value = false;
  await changeStatus(retireTarget.value, "RETIRED");
}

refresh();
</script>

<template>
  <section class="catalog-layout">
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><h2>设备管理</h2></div>
      <button class="primary" type="button" @click="openEditor()">新增设备</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="admin-filter-row">
        <input v-model.trim="keyword" placeholder="搜索编号、名称、类别、位置" />
        <ScbSelect v-model="status" :options="deviceStatusFilterOptions" />
      </div>
      <template v-if="filtered.length">
      <div class="table-scroll table-breakout">
        <table class="data-table">
          <thead><tr><th>编号</th><th>名称</th><th>类别</th><th>科室</th><th>状态</th><th>使用量</th><th class="actions-cell">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.id)">
              <td>{{ displayText(item.deviceCode) }}</td>
              <td>{{ displayText(item.name) }}</td>
              <td>{{ displayText(item.category) }}</td>
              <td>{{ displayText(item.departmentName) }}</td>
              <td><StatusTag :status="displayText(item.status)" :tone="statusClass(item.status)" /></td>
              <td>{{ displayText(item.usageCount, "0") }} / 异常 {{ displayText(item.abnormalCount, "0") }}</td>
              <td class="toolbar">
                <button v-for="a in deviceActions(item)" :key="a.key"
                  type="button" :class="['action-btn', { danger: a.danger }]"
                  @click="handleAction(a.key, item)">{{ a.label }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </template>
      <EmptyState v-else title="暂无设备" />
    </div>
    <Modal :open="editorOpen" title="设备编辑" description="填写或修改设备信息。" @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="编号"><input v-model.trim="form.deviceCode" /></FormField>
          <FormField label="名称"><input v-model.trim="form.name" /></FormField>
          <FormField label="类别"><input v-model.trim="form.category" /></FormField>
          <FormField label="科室"><ScbSelect v-model="form.departmentId" :options="deviceDeptOptions" /></FormField>
          <FormField label="位置"><input v-model.trim="form.location" /></FormField>
          <FormField label="状态"><ScbSelect v-model="form.status" :options="deviceFormStatusOptions" /></FormField>
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
          <FormField label="类型"><ScbSelect v-model="usageForm.usageType" :options="usageTypeOptions" /></FormField>
          <FormField label="操作人"><input v-model.trim="usageForm.usedBy" /></FormField>
          <FormField label="患者 ID"><input v-model.number="usageForm.patientId" type="number" /></FormField>
          <FormField label="结果"><ScbSelect v-model="usageForm.resultStatus" :options="resultStatusOptions" /></FormField>
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
    <Modal :open="retireOpen" title="停用确认" :description="`确认停用设备「${displayText(retireTarget?.name)}」？停用后可通过编辑恢复。`" @close="retireOpen = false">
      <template #footer>
        <button type="button" @click="retireOpen = false">取消</button>
        <button class="danger" type="button" @click="doRetire">确认停用</button>
      </template>
    </Modal>
  </section>
  </section>
</template>
