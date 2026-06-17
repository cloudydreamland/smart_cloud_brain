<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  notificationWebSocketProtocols,
  notificationWebSocketUrl,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";
import { AppShell, TopBar } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { session, permissionError } = storeToRefs(auth);
const { registrations, notifications } = storeToRefs(workflow);
const loading = ref(false);
const socketStatus = ref("未连接");
let socket: WebSocket | null = null;
let pollTimer: number | null = null;
let reconnectTimer: number | null = null;
let unbind: (() => void) | null = null;

const unread = computed(() => notifications.value.filter((item) => String(item.readStatus) !== "READ").length);
const navGroups = computed(() => [
  { label: "诊疗任务", items: [
    { label: "工作台", to: "/", badge: registrations.value.length },
    { label: "待接诊", to: "/queue", badge: registrations.value.length },
    { label: "病历", to: "/records" },
    { label: "处方", to: "/prescriptions" },
  ] },
  { label: "诊后", items: [
    { label: "风险通知", to: "/notifications", badge: unread.value },
    { label: "设置", to: "/settings" },
  ] },
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
  socket = new WebSocket(notificationWebSocketUrl(), notificationWebSocketProtocols(auth.token()));
  socket.onopen = () => { socketStatus.value = "实时通知已连接"; };
  socket.onmessage = () => refresh().catch(() => undefined);
  socket.onerror = () => { socketStatus.value = "WebSocket 不可用，轮询中"; startPolling(); };
  socket.onclose = () => {
    if (session.value) {
      socketStatus.value = "实时通知断开，重连中";
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
  <AppShell
    mark="医"
    title="医生诊疗工作台"
    subtitle="接诊 · AI 病历 · 处方审核"
    :user-name="session?.name"
    :user-meta="`${session?.role || ''} #${session?.userId || ''}`"
    :nav-groups="navGroups"
    @logout="logout"
  >
    <template #user>
      <div class="row-meta"><span class="tag success">在线接诊</span><span class="tag warning">{{ unread }} 条未读</span></div>
    </template>

    <TopBar eyebrow="Doctor Web" title="医生接诊与病历处方确认" description="队列、患者信息、AI 草稿和处方风险分页面处理。">
      <template #actions>
        <span class="tag info">{{ socketStatus }}</span>
        <button type="button" :disabled="loading" @click="refresh">同步队列</button>
      </template>
    </TopBar>

    <div class="doctor-notices">
      <div v-if="permissionError" class="notice error">{{ permissionError }}</div>
    </div>

    <RouterView @refresh="refresh" />
  </AppShell>
</template>
