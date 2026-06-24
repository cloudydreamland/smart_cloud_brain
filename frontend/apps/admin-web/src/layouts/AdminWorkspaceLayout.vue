<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { statusText, useAdminWorkflowStore, useAuthStore } from "@smart-cloud-brain/shared-api";
import { useRoute } from "vue-router";
import { CollapsibleSidebar, TopBar } from "@smart-cloud-brain/shared-ui";

const route = useRoute();
const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const { departments, doctors, triageDesk } = storeToRefs(workflow);
const loading = ref(false);
let unbind: (() => void) | null = null;

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
  ] },
  { label: "配置", items: [
    { label: "知识库", to: "/knowledge" },
    { label: "提示词", to: "/prompts" },
    { label: "字典", to: "/dicts" },
    { label: "患者端配置", to: "/patient-site" },
    { label: "搜索", to: "/search" },
  ] },
]);

async function refresh() {
  if (!session.value || !auth.requireRole("ADMIN")) return;
  loading.value = true;
  try {
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
      <TopBar eyebrow="管理端" title="运营管理工作台" description="基础数据、号源与智能配置统一维护">
        <template #actions>
          <span class="topbar-meta">管理员 #{{ session?.userId || '' }}</span>
          <button type="button" class="topbar-refresh" :disabled="loading" @click="refresh">刷新数据</button>
          <button type="button" class="topbar-logout" @click="logout">退出登录</button>
        </template>
      </TopBar>

      <div class="admin-notices"><div v-if="permissionError" class="notice error">{{ permissionError }}</div></div>
      <RouterView @refresh="refresh" />
    </div>
  </div>
</template>
