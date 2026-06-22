<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { notificationWebSocketUrl, useAuthStore, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { statusLabel, withDemo, demoNotifications, demoRegistrations } from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const route = useRoute();
const { session, permissionError } = storeToRefs(auth);
const { registrations, notifications } = storeToRefs(workflow);
const displayRegistrations = withDemo(registrations, demoRegistrations);
const displayNotifications = withDemo(notifications, demoNotifications);
const loading = ref(false);
const socketStatus = ref("Disconnected");
let socket: WebSocket | null = null;
let pollTimer: number | null = null;
let reconnectTimer: number | null = null;
let unbind: (() => void) | null = null;

const unread = computed(() => displayNotifications.value.filter((item) => String(item.readStatus).toUpperCase() !== "READ").length);
const activeQueue = computed(() => displayRegistrations.value.filter((item) => String(item.status).toUpperCase() !== "COMPLETED").length);
const navItems = computed(() => [
  { label: "Dashboard", icon: "D", to: "/", name: "doctor-dashboard", badge: activeQueue.value },
  { label: "Queue", icon: "Q", to: "/queue", name: "doctor-queue", badge: activeQueue.value },
  { label: "Consult", icon: "C", to: activeQueue.value ? `/consult/${displayRegistrations.value[0]?.registrationId}` : "/queue", name: "doctor-consult" },
  { label: "Records", icon: "R", to: "/records", name: "doctor-records" },
  { label: "Prescriptions", icon: "P", to: "/prescriptions", name: "doctor-prescriptions" },
  { label: "Schedule", icon: "S", to: "/schedule", name: "doctor-schedule" },
  { label: "Notifications", icon: "N", to: "/notifications", name: "doctor-notifications", badge: unread.value },
  { label: "Settings", icon: "G", to: "/settings", name: "doctor-settings" },
]);
const pageTitle = computed(() => navItems.value.find((item) => route.name === item.name || (item.to !== "/" && route.path.startsWith(item.to)))?.label || "Doctor workspace");

async function refresh() {
  if (!session.value || !auth.requireRole("DOCTOR")) return;
  loading.value = true;
  try {
    await workflow.refresh(auth.token());
  } finally {
    loading.value = false;
  }
}

function startPolling() {
  if (pollTimer) return;
  pollTimer = window.setInterval(() => refresh().catch(() => undefined), 15000);
}

function stopRealtime() {
  socket?.close();
  socket = null;
  if (pollTimer) window.clearInterval(pollTimer);
  if (reconnectTimer) window.clearTimeout(reconnectTimer);
  pollTimer = null;
  reconnectTimer = null;
}

function connectNotifications() {
  if (!session.value) return;
  stopRealtime();
  socketStatus.value = "Connecting";
  try {
    socket = new WebSocket(notificationWebSocketUrl(auth.token()));
    socket.onopen = () => { socketStatus.value = "Realtime"; };
    socket.onmessage = () => refresh().catch(() => undefined);
    socket.onerror = () => { socketStatus.value = "Polling"; startPolling(); };
    socket.onclose = () => {
      if (session.value) {
        socketStatus.value = "Reconnecting";
        startPolling();
        reconnectTimer = window.setTimeout(connectNotifications, 5000);
      }
    };
  } catch {
    socketStatus.value = "Polling";
    startPolling();
  }
}

function logout() {
  stopRealtime();
  auth.logout();
  router.push({ name: "doctor-login" });
}

onMounted(async () => {
  unbind = auth.bindUnauthorized();
  await refresh();
  connectNotifications();
});

onBeforeUnmount(() => {
  unbind?.();
  stopRealtime();
});
</script>

<template>
  <div class="doctor-shell">
    <aside class="doctor-nav" aria-label="Doctor navigation">
      <div class="nav-head">
        <div class="brand-mark">DR</div>
        <div><strong>Doctor Portal</strong><span>Clinic workstation</span></div>
      </div>
      <nav>
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          class="doctor-nav-link"
          :class="{ active: route.name === item.name || (item.to !== '/' && route.path.startsWith(item.to)) }"
          :to="item.to"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
          <b v-if="item.badge">{{ item.badge }}</b>
        </RouterLink>
      </nav>
      <div class="doctor-card">
        <strong>{{ session?.name || "Doctor" }}</strong>
        <span>{{ statusLabel(session?.role, "Doctor") }} #{{ session?.userId || "-" }}</span>
        <span>{{ socketStatus }}</span>
      </div>
    </aside>
    <div class="doctor-app">
      <header class="doctor-topline">
        <div class="top-title"><strong>{{ pageTitle }}</strong><span>Real queue, records, prescriptions and schedules</span></div>
        <div class="doctor-topline-status">
          <span class="status-pill"><i class="dot"></i>{{ socketStatus }}</span>
          <span class="status-pill">Queue {{ activeQueue }}</span>
          <span class="status-pill">Unread {{ unread }}</span>
          <button type="button" class="ghost" :disabled="loading" @click="refresh">{{ loading ? "Syncing..." : "Sync" }}</button>
          <button type="button" @click="logout">Logout</button>
        </div>
      </header>
      <main class="doctor-main">
        <div v-if="permissionError" class="clinical-alert danger">{{ permissionError }}</div>
        <RouterView @refresh="refresh" />
      </main>
    </div>
  </div>
</template>
