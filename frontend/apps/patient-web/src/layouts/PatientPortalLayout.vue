<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { fieldText, formatApiError, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { CollapsibleSidebar } from "@smart-cloud-brain/shared-ui";
import SessionExpiredModal from "../components/SessionExpiredModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const { patient, registrations } = storeToRefs(workflow);
const loading = ref(true);
const sessionExpired = ref(false);
const loadError = ref("");
let unbind: (() => void) | null = null;

const sidebarGroups = [
  {
    items: [
      { label: "首页", to: "/portal" },
      { label: "AI 分诊", to: "/portal/triage" },
      { label: "预约医生", to: "/portal/doctors" },
      { label: "我的挂号", to: "/portal/appointments" },
    ],
  },
  {
    items: [
      { label: "病历", to: "/portal/records" },
      { label: "处方", to: "/portal/prescriptions" },
      { label: "个人资料", to: "/portal/profile" },
    ],
  },
];

const activeAppointment = computed(() => registrations.value.find((item) => {
  const status = fieldText(item, "status");
  return !["COMPLETED", "CANCELLED"].includes(status);
}));

async function refresh() {
  if (!session.value || !auth.requireRole("PATIENT")) return;
  loading.value = true;
  loadError.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    loadError.value = formatApiError(err, "患者数据加载失败");
  } finally {
    loading.value = false;
  }
}

function logout() {
  auth.logout();
  router.push({ name: "patient-login" });
}

onMounted(async () => {
  unbind = auth.bindUnauthorized();
  window.addEventListener("smart-cloud-brain:unauthorized", () => {
    sessionExpired.value = true;
  });
  await refresh();
});

onBeforeUnmount(() => {
  unbind?.();
});
</script>

<template>
  <div class="patient-page theme-patient patient-portal-page">
    <CollapsibleSidebar
      mark="患"
      title="患者服务中心"
      :groups="sidebarGroups"
      :user-name="session?.name || '患者'"
      :user-meta="`患者 #${session?.userId || '-'}`"
      @logout="logout"
    />

    <div class="portal-workspace">
      <header class="portal-servicebar">
        <div>
          <strong>患者服务中心</strong>
          <span>{{ fieldText(patient, "name", session?.name || "患者") }} · 普通门诊预约</span>
        </div>
        <div class="portal-service-actions">
          <span class="portal-status online">在线</span>
          <span v-if="activeAppointment" class="portal-status">待就诊</span>
          <button type="button" @click="refresh">刷新</button>
          <button type="button" @click="logout">退出</button>
        </div>
      </header>

      <div v-if="permissionError" class="portal-message error">
        <span>{{ permissionError }}</span>
        <button type="button" @click="logout">切换账号</button>
      </div>
      <div v-else-if="loadError" class="portal-message error">
        <span>{{ loadError }}</span>
        <button type="button" @click="refresh">重试</button>
      </div>

      <RouterView :boot-loading="loading" @refresh="refresh" />
    </div>

    <SessionExpiredModal :open="sessionExpired" @close="logout" />
  </div>
</template>
