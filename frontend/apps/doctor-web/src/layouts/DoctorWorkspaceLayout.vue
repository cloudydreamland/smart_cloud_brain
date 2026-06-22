<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  notificationWebSocketUrl,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";
import {
  statusLabel,
  statusTone,
  withDemo,
  demoNotifications,
  demoRegistrations,
} from "../doctorPresentation";

const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const route = useRoute();
const { session, permissionError } = storeToRefs(auth);
const { registrations, notifications } = storeToRefs(workflow);
const displayRegistrations = withDemo(registrations, demoRegistrations);
const displayNotifications = withDemo(notifications, demoNotifications);
const loading = ref(false);
const socketStatus = ref("未连接");
let socket: WebSocket | null = null;
let pollTimer: number | null = null;
let reconnectTimer: number | null = null;
let unbind: (() => void) | null = null;

const unread = computed(() => displayNotifications.value.filter((item) => String(item.readStatus).toUpperCase() !== "READ").length);
const activeQueue = computed(() => displayRegistrations.value.filter((item) => String(item.status).toUpperCase() !== "COMPLETED").length);
const pageTitle = computed(() => {
  const matched = navItems.value.find((item) => route.name === item.name || (item.to !== "/" && route.path.startsWith(item.to)));
  return matched?.label || "医生工作台";
});
const flowSteps = computed(() => [
  { no: "01", label: "登录", state: "done" },
  { no: "02", label: "查看首页", state: route.name === "doctor-dashboard" ? "now" : "done" },
  { no: "03", label: "筛选队列", state: route.name === "doctor-queue" ? "now" : route.path.startsWith("/consult") ? "done" : "" },
  { no: "04", label: "接诊病历", state: route.path.startsWith("/consult") ? "now" : "" },
  { no: "05", label: "处方审核", state: route.name === "doctor-prescriptions" ? "now" : "" },
  { no: "06", label: "通知闭环", state: route.name === "doctor-notifications" ? "now" : "" },
]);
const navItems = computed(() => [
  { label: "首页", icon: "首", to: "/", name: "doctor-dashboard", badge: activeQueue.value },
  { label: "队列", icon: "列", to: "/queue", name: "doctor-queue", badge: activeQueue.value },
  { label: "接诊", icon: "诊", to: activeQueue.value ? `/consult/${displayRegistrations.value[0]?.registrationId || 10023}` : "/queue", name: "doctor-consult" },
  { label: "病历", icon: "历", to: "/records", name: "doctor-records" },
  { label: "处方", icon: "方", to: "/prescriptions", name: "doctor-prescriptions", badge: 2 },
  { label: "通知", icon: "知", to: "/notifications", name: "doctor-notifications", badge: unread.value },
  { label: "设置", icon: "设", to: "/settings", name: "doctor-settings" },
]);

async function refresh() {
  if (!session.value || !auth.requireRole("DOCTOR")) return;
  loading.value = true;
  try {
    await workflow.refresh(auth.token());
  } catch {
    socketStatus.value = "演示数据";
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
  try {
    socket = new WebSocket(notificationWebSocketUrl(auth.token()));
    socket.onopen = () => { socketStatus.value = "实时通知"; };
    socket.onmessage = () => refresh().catch(() => undefined);
    socket.onerror = () => { socketStatus.value = "轮询"; startPolling(); };
    socket.onclose = () => {
      if (session.value) {
        socketStatus.value = "重连中";
        startPolling();
        reconnectTimer = window.setTimeout(connectNotifications, 5000);
      }
    };
  } catch {
    socketStatus.value = "演示数据";
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
    <aside class="doctor-nav" aria-label="医生端导航">
      <div class="nav-head">
        <div class="brand-mark">医</div>
        <div>
          <strong>智慧云脑医生端</strong>
          <span>门诊接诊中心</span>
        </div>
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
        <strong>{{ session?.name || "陈明 主治医师" }}</strong>
        <span>呼吸内科 · {{ statusLabel(session?.role, "医生") }} #{{ session?.userId || "D2048" }}</span>
        <span>{{ socketStatus }}，15 秒自动同步</span>
      </div>
    </aside>

    <div class="doctor-app">
      <header class="doctor-topline">
        <div class="top-title">
          <strong>{{ pageTitle }}</strong>
          <span>2026-06-22 · 呼吸内科上午门诊</span>
        </div>
        <div class="doctor-topline-status">
          <span class="status-pill"><i class="dot"></i>{{ socketStatus }}</span>
          <span class="status-pill">队列 {{ activeQueue }}</span>
          <span class="status-pill">未读 {{ unread }}</span>
          <button type="button" class="ghost" :disabled="loading" @click="refresh">{{ loading ? "同步中" : "同步" }}</button>
          <button type="button" @click="logout">退出</button>
        </div>
      </header>

      <main class="doctor-main">
        <section class="flow-strip" aria-label="医生端流程">
          <div v-for="step in flowSteps" :key="step.no" class="flow-step" :class="step.state">
            <span>{{ step.no }}</span>
            <strong>{{ step.label }}</strong>
          </div>
        </section>
        <div v-if="permissionError" class="clinical-alert danger">{{ permissionError }}</div>
        <RouterView @refresh="refresh" />
      </main>
    </div>
  </div>
</template>
