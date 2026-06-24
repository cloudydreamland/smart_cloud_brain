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
let unbind: (() => void) | null = null;

const toastRef = ref<InstanceType<typeof Toast> | null>(null);
provide("toast", toastRef);

const highRisk = computed(() => triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(String(item.status))).length);
const navGroups = computed(() => [
  { label: "运维入口", items: [
    { label: "工作台", to: "/" },
    { label: "科室", to: "/departments", badge: departments.value.length },
    { label: "医生", to: "/doctors", badge: doctors.value.length },
    { label: "药品", to: "/drugs" },
    { label: "排班", to: "/schedule" },
    { label: "分诊台", to: "/triage-desk", badge: highRisk.value },
    { label: "账户权限", to: "/accounts" },
    { label: "统计分析", to: "/statistics" },
  ] },
  { label: "配置", items: [
    { label: "患者端配置", to: "/patient-site" },
    { label: "搜索", to: "/search" },
  ] },
]);

async function refresh() {
  if (!session.value || !auth.requireRole("ADMIN")) return;
  loading.value = true;
  try {
    await workflow.refresh(auth.token());
    toastRef.value?.success("数据已刷新", "所有模块数据已同步最新状态。");
  } catch {
    toastRef.value?.error("刷新失败", "请检查网络后重试。");
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
          <a href="http://localhost" target="_blank" rel="noopener" type="button" class="topbar-btn">Dify 管理</a>
          <button type="button" class="topbar-btn" :disabled="loading" @click="refresh">刷新</button>
          <button type="button" class="topbar-btn danger" @click="logout">退出</button>
        </div>
      </header>

      <div class="admin-notices"><div v-if="permissionError" class="notice error">{{ permissionError }}</div></div>
      <RouterView @refresh="refresh" />
    </div>
    <Toast ref="toastRef" />
  </div>
</template>
