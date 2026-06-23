<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { fieldText, formatApiError, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import PatientSiteFooter from "../components/PatientSiteFooter.vue";
import PatientSiteHeader from "../components/PatientSiteHeader.vue";
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

const activeAppointment = computed(() => registrations.value.find((item) => {
  const status = fieldText(item, "status");
  return !["COMPLETED", "CANCELLED"].includes(status);
}));

const patientName = computed(() => fieldText(patient.value, "name", session.value?.name || "患者"));

async function refresh() {
  if (!session.value || !auth.requireRole("PATIENT")) return;
  loading.value = true;
  loadError.value = "";
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated(auth.token());
  } catch (err) {
    loadError.value = formatApiError(err, "患者资料加载失败");
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
      <section class="portal-servicebar">
        <div>
          <strong>患者服务</strong>
          <span>{{ patientName }} · 同一医院官网内的预约、病历、处方与消息服务</span>
        </div>
        <div class="portal-service-actions">
          <span class="portal-status online">已登录</span>
          <span v-if="activeAppointment" class="portal-status">待就诊</span>
          <button type="button" @click="refresh">刷新</button>
          <button type="button" @click="logout">退出登录</button>
        </div>
      </section>

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
    <SessionExpiredModal :open="sessionExpired" @close="logout" />
  </div>
</template>
