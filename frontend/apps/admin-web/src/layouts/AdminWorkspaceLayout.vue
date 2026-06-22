<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { api, statusText, useAdminWorkflowStore, useAuthStore } from "@smart-cloud-brain/shared-api";
import { AppShell, TopBar } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const { departments, doctors, triageDesk } = storeToRefs(workflow);
const loading = ref(false);
const permissions = ref<Set<string>>(new Set());
let unbind: (() => void) | null = null;

const highRisk = computed(() => triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(String(item.status))).length);
const can = (key: string) => !permissions.value.size || permissions.value.has(key);
const navGroups = computed(() => [
  {
    label: "Operations",
    items: [
      { label: "Workspace", to: "/", permission: "dashboard:view" },
      { label: "Departments", to: "/departments", badge: departments.value.length, permission: "department:manage" },
      { label: "Doctors", to: "/doctors", badge: doctors.value.length, permission: "doctor:manage" },
      { label: "Drugs", to: "/drugs", permission: "drug:manage" },
      { label: "Schedules", to: "/schedule", permission: "schedule:manage" },
      { label: "Triage desk", to: "/triage-desk", badge: highRisk.value, permission: "triage:manage" },
      { label: "Devices", to: "/devices", permission: "device:manage" },
      { label: "Patients", to: "/patients", permission: "patient:manage" },
      { label: "Statistics", to: "/statistics", permission: "statistics:view" },
      { label: "Accounts", to: "/accounts", permission: "account:manage" },
      { label: "Permissions", to: "/permissions", permission: "permission:manage" },
    ].filter((item) => can(item.permission)),
  },
  {
    label: "Configuration",
    items: [
      { label: "Knowledge", to: "/knowledge", permission: "knowledge:manage" },
      { label: "Prompts", to: "/prompts", permission: "prompt:manage" },
      { label: "Dictionaries", to: "/dicts", permission: "dict:manage" },
      { label: "Search", to: "/search", permission: "search:view" },
    ].filter((item) => can(item.permission)),
  },
]);

async function refresh() {
  if (!session.value || !auth.requireRole("ADMIN")) return;
  loading.value = true;
  try {
    permissions.value = new Set(await api.myPermissions(auth.token()));
    await workflow.refresh(auth.token());
  } finally {
    loading.value = false;
  }
}

function logout() {
  auth.logout();
  router.push({ name: "admin-login" });
}

onMounted(async () => {
  unbind = auth.bindUnauthorized();
  await refresh();
});

onBeforeUnmount(() => unbind?.());
</script>

<template>
  <AppShell
    mark="ADM"
    title="Operations Console"
    subtitle="Catalogs / schedules / devices / patients / permissions"
    :user-name="session?.name"
    :user-meta="`${statusText(session?.role, '')} #${session?.userId || ''}`"
    :nav-groups="navGroups"
    @logout="logout"
  >
    <template #user>
      <div class="row-meta">
        <span class="tag success">Signed in</span>
        <span class="tag warning">{{ highRisk }} triage alerts</span>
      </div>
    </template>
    <TopBar
      eyebrow="Admin"
      title="Real operational data and access control"
      description="Manage catalogs, schedules, triage, devices, patients, statistics and role permissions from live APIs."
    >
      <template #actions><button type="button" :disabled="loading" @click="refresh">Refresh</button></template>
    </TopBar>
    <div class="admin-notices"><div v-if="permissionError" class="notice error">{{ permissionError }}</div></div>
    <RouterView @refresh="refresh" />
  </AppShell>
</template>
