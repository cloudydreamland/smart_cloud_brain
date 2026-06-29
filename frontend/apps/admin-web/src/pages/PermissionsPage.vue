<script setup lang="ts">
import { computed, inject, onMounted, ref, watch, type Ref } from "vue";
import { api, displayText, formatApiError, type PermissionCatalogItem, type PermissionPayload, type Role } from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, SegmentedControl, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";

const payload = ref<PermissionPayload | null>(null);
const activeRole = ref<Role>("ADMIN");
const selectedKeys = ref<string[]>([]);
const loading = ref(false);
const loaded = ref(false);
const saving = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");

const permissionLabels: Record<string, string> = {
  "dashboard:view": "工作台", "department:manage": "科室管理", "doctor:manage": "医生管理",
  "drug:manage": "药品管理", "schedule:manage": "排班管理", "triage:manage": "分诊台",
  "device:manage": "设备管理", "patient:manage": "患者管理", "account:manage": "账户管理",
  "permission:manage": "权限管理", "statistics:view": "统计分析", "ai-log:view": "AI日志",
  "notification:manage": "通知管理", "medical-record:manage": "病历管理", "prescription:manage": "处方管理",
  "registration:manage": "挂号管理",
  "statistics:export": "报表导出", "patient-site:manage": "患者端配置",
};
const permissionDescriptions: Record<string, string> = {
  "dashboard:view": "查看管理端工作台", "department:manage": "维护科室信息",
  "doctor:manage": "维护医生账号和信息", "drug:manage": "维护药品和规格信息",
  "schedule:manage": "管理排班和预约号源", "triage:manage": "分配和关闭分诊记录",
  "device:manage": "管理医疗设备和使用记录", "patient:manage": "查看和管理患者资料",
  "account:manage": "管理系统用户账号", "permission:manage": "配置角色权限",
  "statistics:view": "查看统计分析数据", "ai-log:view": "查看AI调用日志",
  "notification:manage": "管理系统通知", "medical-record:manage": "管理患者病历",
  "prescription:manage": "管理处方和审核", "registration:manage": "管理挂号记录",
  "statistics:export": "导出统计数据为CSV",
  "patient-site:manage": "管理患者端导航和内容",
};
const catalog = computed<PermissionCatalogItem[]>(() => (payload.value?.catalog ?? []).map((item) => ({
  key: displayText(item.key),
  label: permissionLabels[item.key] ?? displayText(item.label),
  description: permissionDescriptions[item.key] ?? displayText(item.description),
})).filter((item) => item.key));

const roles = computed(() => {
  const values = payload.value?.roles ?? ["ADMIN", "DOCTOR", "PATIENT"];
  const roleLabels: Record<string, string> = { ADMIN: "管理员", DOCTOR: "医生", PATIENT: "患者" };
  return values.map((role) => ({ value: role, label: roleLabels[role] ?? role }));
});

const enabledCount = computed(() => selectedKeys.value.length);
const roleLabel = computed(() => {
  const roleLabels: Record<string, string> = { ADMIN: "管理员", DOCTOR: "医生", PATIENT: "患者" };
  return roleLabels[activeRole.value] ?? activeRole.value;
});

watch([activeRole, catalog, payload], syncSelectedKeys);

function grantEnabled(role: string, key: string) {
  return (payload.value?.grants ?? []).some((item) => (
    item.role === role
    && item.permissionKey === key
    && Boolean(item.enabled)
  ));
}

function syncSelectedKeys() {
  selectedKeys.value = catalog.value.filter((item) => grantEnabled(activeRole.value, item.key)).map((item) => item.key);
}

function togglePermission(key: string, checked: boolean) {
  const next = new Set(selectedKeys.value);
  if (checked) next.add(key);
  else next.delete(key);
  selectedKeys.value = [...next];
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    payload.value = await api.permissions() as PermissionPayload;
    syncSelectedKeys();
    loaded.value = true;
  } catch (err) {
    error.value = formatApiError(err, "权限列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  error.value = "";
  try {
    payload.value = await api.saveRolePermissions({
      role: activeRole.value,
      permissionKeys: selectedKeys.value,
    }) as PermissionPayload;
    toast?.value?.success("保存成功", "角色权限已保存");
    syncSelectedKeys();
  } catch (err) {
    error.value = formatApiError(err, "权限保存失败");
  } finally {
    saving.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="catalog-layout">
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <h2>角色权限</h2>
      </div>
      <div class="toolbar">
        <button type="button" :disabled="loading" @click="refresh">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
        <button type="button" class="primary" :disabled="saving || loading" @click="save">保存</button>
      </div>
    </header>

    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />

      <div class="account-role-bar">
        <SegmentedControl v-model="activeRole" :options="roles" />
        <div class="account-role-summary">
          <strong>{{ roleLabel }}</strong>
          <span>{{ enabledCount }} / {{ catalog.length }} 项权限已启用</span>
        </div>
      </div>

      <DataTable :rows="catalog" :loading="!loaded && loading" :error="error" :breakout="true" empty-title="暂无权限数据">
        <thead>
          <tr>
            <th>启用</th>
            <th>权限</th>
            <th>描述</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in catalog" :key="item.key">
            <td>
              <input
                type="checkbox"
                :checked="selectedKeys.includes(item.key)"
                @change="togglePermission(item.key, ($event.target as HTMLInputElement).checked)"
              />
            </td>
            <td><strong>{{ item.label }}</strong><p>{{ item.key }}</p></td>
            <td>{{ item.description }}</td>
            <td><StatusTag :status="selectedKeys.includes(item.key) ? 'ENABLED' : 'DISABLED'" /></td>
          </tr>
        </tbody>
      </DataTable>
    </div>
  </section>
  </section>
</template>
