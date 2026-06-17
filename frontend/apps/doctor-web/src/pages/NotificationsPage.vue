<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusClass, toNumber, useAuthStore, useDoctorWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, StatusTag } from "@smart-cloud-brain/shared-ui";
import NotificationDetailModal from "../components/NotificationDetailModal.vue";

const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { notifications } = storeToRefs(workflow);
const selected = ref<DataRow | null>(null);
const error = ref("");

async function markRead(item = selected.value) {
  if (!item) return;
  error.value = "";
  try {
    await api.markNotificationRead(auth.token(), toNumber(item.notificationId));
    selected.value = null;
    emit("refresh");
  } catch (err) {
    error.value = formatApiError(err, "标记通知失败");
  }
}
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">NOTIFICATIONS</p><h2>风险通知</h2><p>处方审核和系统通知集中处理。</p></div></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notifications.length" class="list">
        <article v-for="item in notifications" :key="String(item.notificationId)" class="list-row">
          <div class="row-main"><strong>{{ fieldText(item, "title") }}</strong><p>{{ fieldText(item, "content") }}</p><div class="row-meta"><StatusTag :status="fieldText(item, 'riskLevel', 'INFO')" :tone="statusClass(item.riskLevel)" /><StatusTag :status="fieldText(item, 'readStatus')" :tone="statusClass(item.readStatus)" /></div></div>
          <div class="toolbar"><button type="button" @click="selected = item">详情</button><button type="button" :disabled="fieldText(item, 'readStatus') === 'READ'" @click="markRead(item)">已读</button></div>
        </article>
      </div>
      <EmptyState v-else title="暂无通知" />
    </div>
    <NotificationDetailModal :open="Boolean(selected)" :notification="selected" @close="selected = null" @read="markRead()" />
  </section>
</template>
