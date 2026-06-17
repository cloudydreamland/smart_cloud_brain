<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import { useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import PatientHeader from "../components/PatientHeader.vue";
import SessionExpiredModal from "../components/SessionExpiredModal.vue";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const loading = ref(true);
const sessionExpired = ref(false);
let unbind: (() => void) | null = null;

async function refresh() {
  if (!session.value || !auth.requireRole("PATIENT")) return;
  loading.value = true;
  try {
    await workflow.refreshPublicData();
    await workflow.refreshAuthenticated(auth.token());
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
  <main class="patient-page theme-patient">
    <PatientHeader :user-name="session?.name" @logout="logout" />
    <div class="portal-notices">
      <div v-if="permissionError" class="notice error">
        <span>{{ permissionError }}</span>
        <button type="button" @click="logout">切换账号</button>
      </div>
    </div>
    <section class="mayo-container">
      <RouterView :boot-loading="loading" @refresh="refresh" />
    </section>
    <SessionExpiredModal :open="sessionExpired" @close="logout" />
  </main>
</template>
