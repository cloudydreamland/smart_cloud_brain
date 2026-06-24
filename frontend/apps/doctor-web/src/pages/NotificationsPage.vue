<script setup lang="ts">
import { ref } from "vue";
import { storeToRefs } from "pinia";
import { api, displayText, formatApiError, toNumber, useAuthStore, useDoctorWorkflowStore, usePagination, type Notification } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState, PaginationBar } from "@smart-cloud-brain/shared-ui";
import NotificationDetailModal from "../components/NotificationDetailModal.vue";
import { liveRows, statusLabel, statusTone } from "../doctorPresentation";

const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const { notifications } = storeToRefs(workflow);
const displayNotifications = liveRows(notifications);
const selected = ref<Notification | null>(null);
const error = ref("");
const notice = ref("");
const loading = ref(false);
const { currentPage, pageSize, total, pageRows } = usePagination(displayNotifications, 8);

async function refresh() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await workflow.refresh(auth.token());
  } catch (err) {
    error.value = formatApiError(err, "通知列表加载失败，请稍后重试。");
  } finally {
    loading.value = false;
  }
}

async function markRead(item = selected.value) {
  if (!item) return;
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.markNotificationRead(auth.token(), toNumber(item.notificationId));
    selected.value = null;
    emit("refresh");
    await refresh();
    notice.value = "通知已标记为已读。";
  } catch (err) {
    selected.value = null;
    error.value = formatApiError(err, "标记通知失败，请刷新后重试。");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header>
      <div class="panel-title">
        <p class="eyebrow">风险通知</p>
        <h2>通知中心</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">{{ loading ? "刷新中" : "刷新" }}</button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <LoadingState v-if="loading" title="正在同步通知" />
      <div class="feed">
        <article
          v-for="item in pageRows"
          :key="String(item.notificationId)"
          class="feed-row"
          :class="{ unread: displayText(item.readStatus, 'UNREAD').toUpperCase() !== 'READ' }"
        >
          <div class="feed-content">
            <strong>{{ displayText(item.title) }}</strong>
            <span>{{ displayText(item.content) }}</span>
          </div>
          <div class="feed-actions">
            <span class="tag" :class="statusTone(displayText(item.riskLevel, 'INFO'))">{{ statusLabel(displayText(item.riskLevel, "INFO")) }}</span>
            <span class="status-dot" :class="displayText(item.readStatus, 'UNREAD').toUpperCase() !== 'READ' ? 'unread' : 'read'" :title="displayText(item.readStatus, 'UNREAD').toUpperCase() !== 'READ' ? '未读' : '已读'"></span>
            <button type="button" class="btn" @click="selected = item">详情</button>
          </div>
        </article>
      </div>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <NotificationDetailModal :open="Boolean(selected)" :notification="selected" @close="selected = null" @read="markRead()" />
  </section>
</template>
