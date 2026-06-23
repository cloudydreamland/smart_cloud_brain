<script setup lang="ts">
import { computed, ref } from "vue";
import { api, fieldText, formatApiError, useAuthStore, type DataRow, type Role } from "@smart-cloud-brain/shared-api";
import { ErrorState, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const catalog = ref<DataRow[]>([]);
const grants = ref<DataRow[]>([]);
const activeRole = ref<Role>("ADMIN");
const selected = ref<Set<string>>(new Set());
const loading = ref(false);
const error = ref("");
const notice = ref("");

const roleOptions: Role[] = ["ADMIN", "DOCTOR", "PATIENT"];
const currentGrants = computed(() => grants.value.filter((item) => fieldText(item, "role") === activeRole.value));

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const response = await api.permissions(auth.token());
    catalog.value = (response.catalog as DataRow[] | undefined) ?? [];
    grants.value = (response.grants as DataRow[] | undefined) ?? [];
    loadRole(activeRole.value);
  } catch (err) {
    error.value = formatApiError(err, "Permission load failed");
  } finally {
    loading.value = false;
  }
}

function loadRole(role: Role) {
  activeRole.value = role;
  selected.value = new Set(grants.value
    .filter((item) => fieldText(item, "role") === role && item.enabled)
    .map((item) => fieldText(item, "permissionKey")));
}

function toggle(key: string) {
  const next = new Set(selected.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  selected.value = next;
}

async function save() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    const response = await api.saveRolePermissions(auth.token(), { role: activeRole.value, permissionKeys: [...selected.value] });
    grants.value = (response.grants as DataRow[] | undefined) ?? [];
    notice.value = "Permissions saved";
  } catch (err) {
    error.value = formatApiError(err, "Permission save failed");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><p class="eyebrow">Access control</p><h2>Menu and Operation Permissions</h2><p>Permissions are persisted by role and used by admin APIs.</p></div>
      <button class="primary" type="button" :disabled="loading" @click="save">Save role</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <div class="role-switch-list">
        <button v-for="role in roleOptions" :key="role" type="button" class="role-switch-card" :class="{ active: activeRole === role }" @click="loadRole(role)">
          <span>{{ role }}</span><strong>{{ currentGrants.filter((item) => item.enabled).length }}</strong>
        </button>
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead><tr><th>Enabled</th><th>Permission</th><th>Description</th><th>Status</th></tr></thead>
          <tbody>
            <tr v-for="item in catalog" :key="fieldText(item, 'key')">
              <td><input type="checkbox" :checked="selected.has(fieldText(item, 'key'))" @change="toggle(fieldText(item, 'key'))" /></td>
              <td><strong>{{ fieldText(item, "label") }}</strong><p>{{ fieldText(item, "key") }}</p></td>
              <td>{{ fieldText(item, "description") }}</td>
              <td><StatusTag :status="selected.has(fieldText(item, 'key')) ? 'ENABLED' : 'DISABLED'" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
