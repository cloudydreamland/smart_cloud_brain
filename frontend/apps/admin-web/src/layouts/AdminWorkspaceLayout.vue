<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, provide, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { statusText, useAdminWorkflowStore, useAuthStore } from "@smart-cloud-brain/shared-api";
import { useRoute } from "vue-router";
import { CollapsibleSidebar, Toast } from "@smart-cloud-brain/shared-ui";

const route = useRoute();
const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const { departments, doctors, triageDesk } = storeToRefs(workflow);
const loading = ref(false);
const difyUrl = import.meta.env.VITE_DIFY_URL || "";
let unbind: (() => void) | null = null;

const toastRef = ref<InstanceType<typeof Toast> | null>(null);
provide("toast", toastRef);

const highRisk = computed(() => triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(String(item.status))).length);
const navGroups = computed(() => [
  { label: "运维入口", items: [
    { label: "工作台", to: "/" },
    { label: "科室", to: "/departments", badge: departments.value.length },
    { label: "医生", to: "/doctors", badge: doctors.value.length },
    { label: "患者", to: "/patients" },
    { label: "药品", to: "/drugs" },
    { label: "排班", to: "/schedule" },
    { label: "分诊台", to: "/triage-desk", badge: highRisk.value },
    { label: "账户权限", to: "/accounts" },
    { label: "权限", to: "/permissions" },
    { label: "设备", to: "/devices" },
  ] },
  { label: "配置", items: [
    { label: "患者端配置", to: "/patient-site" },
    { label: "邮箱配置", to: "/email-config" },
  ] },
]);

async function refresh(silent = false, showLoading = true) {
  if (!session.value || !auth.requireRole("ADMIN")) return;
  if (showLoading) loading.value = true;
  try {
    await workflow.refresh();
    if (!silent) toastRef.value?.success("数据已刷新", "所有模块数据已同步最新状态。");
  } catch {
    if (!silent) toastRef.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}
function logout() {
  auth.logout();
  router.push({ name: "admin-login" });
}

onMounted(async () => {
  unbind = auth.bindUnauthorized();
  await refresh(true, false);
});

onBeforeUnmount(() => unbind?.());
</script>

<template>
  <div class="admin-shell">
    <CollapsibleSidebar
      mark="管"
      title="管理控制台"
      :groups="navGroups"
      :user-name="session?.name || '管理员'"
      :user-meta="`${statusText(session?.role, '')} #${session?.userId || ''}`"
      :current-path="route.path"
      @navigate="router.push"
    />

    <div class="admin-app">
      <header class="admin-topline">
        <div class="admin-session">
          <strong>{{ session?.name || '管理员' }}</strong>
          <span>管理员 #{{ session?.userId || '-' }}</span>
        </div>
        <div class="admin-topline-actions">
          <a :href="difyUrl" target="_blank" rel="noopener" type="button" class="topbar-btn ghost" v-if="difyUrl">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            Dify
          </a>
          <button type="button" class="topbar-btn ghost" :disabled="loading" @click="refresh()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新
          </button>
          <button type="button" class="topbar-btn text-danger" @click="logout">退出</button>
        </div>
      </header>

      <div class="admin-notices"><div v-if="permissionError" class="notice error">{{ permissionError }}</div></div>
      <RouterView @refresh="refresh" />
    </div>
    <Toast ref="toastRef" />
  </div>
</template>
