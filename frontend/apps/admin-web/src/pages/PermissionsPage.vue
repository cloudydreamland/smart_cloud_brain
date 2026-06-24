<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { api, displayText, formatApiError, useAuthStore, type PermissionCatalogItem, type PermissionPayload, type Role } from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, SegmentedControl, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const payload = ref<PermissionPayload | null>(null);
const activeRole = ref<Role>("ADMIN");
const selectedKeys = ref<string[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const notice = ref("");

const catalog = computed<PermissionCatalogItem[]>(() => (payload.value?.catalog ?? []).map((item) => ({
  key: displayText(item.key),
  label: displayText(item.label),
  description: displayText(item.description),
})).filter((item) => item.key));

const roles = computed(() => {
  const values = payload.value?.roles ?? ["ADMIN", "DOCTOR", "PATIENT"];
  return values.map((role) => ({ value: role, label: role }));
});

const enabledCount = computed(() => selectedKeys.value.length);

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
    payload.value = await api.permissions(auth.token()) as PermissionPayload;
    syncSelectedKeys();
  } catch (err) {
    error.value = formatApiError(err, "Permission list failed");
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    payload.value = await api.saveRolePermissions(auth.token(), {
      role: activeRole.value,
      permissionKeys: selectedKeys.value,
    }) as PermissionPayload;
    notice.value = "Role permissions saved";
    syncSelectedKeys();
  } catch (err) {
    error.value = formatApiError(err, "Permission save failed");
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
        <h2>角色权限</h2>
      </div>
      <div class="toolbar">
        <button type="button" :disabled="loading" @click="refresh">刷新</button>
        <button type="button" class="primary" :disabled="saving || loading" @click="save">保存</button>
      </div>
    </header>

    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>

      <div class="account-role-bar">
        <SegmentedControl v-model="activeRole" :options="roles" />
        <div class="account-role-summary">
          <strong>{{ activeRole }}</strong>
          <span>{{ enabledCount }} / {{ catalog.length }} permissions enabled</span>
        </div>
      </div>

      <DataTable :rows="catalog" :loading="loading" :error="error" :breakout="true" empty-title="No permissions">
        <thead>
          <tr>
            <th>Enabled</th>
            <th>Permission</th>
            <th>Description</th>
            <th>Status</th>
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
</template>
