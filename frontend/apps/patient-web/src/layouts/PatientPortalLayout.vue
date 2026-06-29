<script setup lang="ts">
import { onBeforeUnmount, onMounted, provide, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { formatApiError, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { Toast } from "@smart-cloud-brain/shared-ui";
import PatientSiteFooter from "../components/PatientSiteFooter.vue";
import PatientSiteHeader from "../components/PatientSiteHeader.vue";
import SessionExpiredModal from "../components/SessionExpiredModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const loading = ref(true);
const sessionExpired = ref(false);
const loadError = ref("");
const toastRef = ref<InstanceType<typeof Toast> | null>(null);
provide("toast", toastRef);
let unbind: (() => void) | null = null;

async function refresh() {
  if (!session.value || !auth.requireRole("PATIENT")) return;
  loading.value = true;
  loadError.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated();
    toastRef.value?.success("数据已刷新", "所有模块数据已同步最新状态。");
  } catch (err) {
    loadError.value = formatApiError(err, "患者资料加载失败");
    toastRef.value?.error("刷新失败", "请检查网络后重试。");
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
    <PatientSiteHeader />

    <main class="patient-site-main">
      <div v-if="permissionError" class="portal-message error">
        <span>{{ permissionError }}</span>
        <button type="button" @click="logout">切换账号</button>
      </div>
      <div v-else-if="loadError" class="portal-message error">
        <span>{{ loadError }}</span>
        <button type="button" @click="refresh">重试</button>
      </div>

      <RouterView :boot-loading="loading" @refresh="refresh" />
    </main>

    <PatientSiteFooter />
    <Toast ref="toastRef" />
    <SessionExpiredModal :open="sessionExpired" @close="logout" />
  </div>
</template>
