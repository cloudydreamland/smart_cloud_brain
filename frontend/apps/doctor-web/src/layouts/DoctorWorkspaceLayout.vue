<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  notificationWebSocketUrl,
  statusText,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";
import { CollapsibleSidebar } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const route = useRoute();
const { session, permissionError } = storeToRefs(auth);
const { registrations, notifications } = storeToRefs(workflow);
const loading = ref(false);
const socketStatus = ref("未连接");
let socket: WebSocket | null = null;
let pollTimer: number | null = null;
let reconnectTimer: number | null = null;
let unbind: (() => void) | null = null;

const unread = computed(() => notifications.value.filter((item) => String(item.readStatus) !== "READ").length);
const activeQueue = computed(() => registrations.value.filter((item) => String(item.status) !== "COMPLETED").length);
const navItems = computed(() => [
  { label: "首页", to: "/", badge: activeQueue.value },
  { label: "队列", to: "/queue", badge: activeQueue.value },
  { label: "病历", to: "/records" },
  { label: "处方", to: "/prescriptions" },
  { label: "通知", to: "/notifications", badge: unread.value },
  { label: "设置", to: "/settings" },
]);

const sidebarGroups = computed(() => [
  { items: navItems.value.slice(0, 4) },
  { items: navItems.value.slice(4) },
]);

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
  socketStatus.value = "连接中";
  socket = new WebSocket(notificationWebSocketUrl(auth.token()));
  socket.onopen = () => { socketStatus.value = "实时"; };
  socket.onmessage = () => refresh().catch(() => undefined);
  socket.onerror = () => { socketStatus.value = "轮询"; startPolling(); };
  socket.onclose = () => {
    if (session.value) {
      socketStatus.value = "重连中";
      startPolling();
      reconnectTimer = window.setTimeout(connectNotifications, 5000);
    }
  };
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
    <CollapsibleSidebar
      mark="医"
      title="医生工作台"
      :groups="sidebarGroups"
      :user-name="session?.name || '医生'"
      :user-meta="`${statusText(session?.role, '医生')} #${session?.userId || '-'}`"
    />

    <div class="doctor-app">
      <header class="doctor-topline">
        <div class="doctor-session">
          <strong>{{ session?.name || "医生" }}</strong>
          <span>{{ statusText(session?.role, "医生") }} #{{ session?.userId || "-" }}</span>
        </div>
        <div class="doctor-topline-status">
          <span>队列 {{ activeQueue }}</span>
          <span>未读 {{ unread }}</span>
          <span>通知 {{ socketStatus }}</span>
          <button type="button" :disabled="loading" @click="refresh">同步</button>
          <button type="button" @click="logout">退出</button>
        </div>
      </header>

      <main class="doctor-main">
        <div v-if="permissionError" class="clinical-alert danger">{{ permissionError }}</div>
        <RouterView @refresh="refresh" />
      </main>
    </div>
  </div>
</template>
