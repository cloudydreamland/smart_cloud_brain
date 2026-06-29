<script setup lang="ts">
import { computed, inject, onMounted, ref, type Ref } from "vue";
import { api, fieldText, formatApiError, formatDateTime, toNumber, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, SegmentedControl, Toast } from "@smart-cloud-brain/shared-ui";

type MessageRow = {
  id: number;
  type: "预约提醒" | "诊后通知" | "系统通知";
  title: string;
  content: string;
  time: string;
  read: boolean;
  source?: DataRow;
};

const rows = ref<DataRow[]>([]);
const loading = ref(false);
const loaded = ref(false);
const saving = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const filter = ref("");
const selected = ref<MessageRow | null>(null);
const options = [
  { label: "全部", value: "" },
  { label: "未读", value: "UNREAD" },
  { label: "已读", value: "READ" },
];

const fallbackRows: MessageRow[] = [
  { id: 0, type: "系统通知", title: "患者服务已整合分诊、挂号和诊后资料", content: "你可以从患者服务台查看当前就诊旅程，或进入我的预约、病历、处方继续处理。", time: "持续有效", read: false },
  { id: -1, type: "预约提醒", title: "预约后请核对就诊人和到院时间", content: "无法按时到院时，请提前取消预约，避免影响后续号源使用。", time: "就诊前", read: false },
  { id: -2, type: "诊后通知", title: "病历和处方生成后将归档到个人服务", content: "复诊前建议回看诊断、医嘱、处方药品和风险审核提示。", time: "诊后", read: true },
];
const messages = computed<MessageRow[]>(() => {
  const normalized = rows.value.map((row, index) => ({
    id: toNumber(row.notificationId ?? row.id, index + 1),
    type: normalizeType(fieldText(row, "type", fieldText(row, "category", ""))),
    title: fieldText(row, "title", "系统通知"),
    content: fieldText(row, "content", fieldText(row, "message", "暂无通知正文")),
    time: formatDateTime(fieldText(row, "createdAt"), fieldText(row, "time", "时间待同步")),
    read: fieldText(row, "readStatus", fieldText(row, "status", "")).toUpperCase() === "READ" || row.read === true,
    source: row,
  }));
  return normalized.length ? normalized : fallbackRows;
});
const filteredMessages = computed(() => messages.value.filter((item) => {
  if (filter.value === "READ") return item.read;
  if (filter.value === "UNREAD") return !item.read;
  return true;
}));
const unreadCount = computed(() => messages.value.filter((item) => !item.read).length);

function normalizeType(value: string): MessageRow["type"] {
  if (value.includes("预约") || value.toUpperCase().includes("REGISTRATION")) return "预约提醒";
  if (value.includes("病历") || value.includes("处方") || value.toUpperCase().includes("MEDICAL")) return "诊后通知";
  return "系统通知";
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.notifications(filter.value);
    loaded.value = true;
    toast?.value?.success("数据已刷新", "消息数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "消息加载失败，当前显示本地服务提醒");
    rows.value = [];
    toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    loading.value = false;
  }
}

async function markRead(item: MessageRow) {
  selected.value = item;
  if (item.read || item.id <= 0) return;
  saving.value = true;
  error.value = "";
  try {
    await api.markNotificationRead(item.id);
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "标记已读失败");
  } finally {
    saving.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">消息中心</p>
        <h2>通知与提醒</h2>
        <p>集中查看预约提醒、诊后资料通知和系统服务公告。</p>
      </div>
      <button type="button" :disabled="loading" @click="refresh">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="summary-strip">
        <div class="summary-item"><span>全部消息</span><strong>{{ messages.length }}</strong></div>
        <div class="summary-item"><span>未读</span><strong>{{ unreadCount }}</strong></div>
        <div class="summary-item"><span>当前筛选</span><strong>{{ filteredMessages.length }}</strong></div>
      </div>
      <div class="toolbar">
        <SegmentedControl v-model="filter" :options="options" @update:model-value="refresh" />
        <span class="row-meta">{{ saving ? "正在更新消息状态" : "点击消息可查看详情并标记已读" }}</span>
      </div>
      <LoadingState v-if="!loaded && loading" />
      <div v-else-if="filteredMessages.length" class="message-list">
        <button v-for="item in filteredMessages" :key="`${item.id}-${item.title}`" type="button" class="message-row" :class="{ unread: !item.read }" @click="markRead(item)">
          <span>{{ item.type }} · {{ item.time }}</span>
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
        </button>
      </div>
      <EmptyState v-else title="暂无消息" message="当前筛选下没有消息。" />

      <div v-if="selected" class="portal-detail-drawer" role="dialog" aria-modal="true">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <span>{{ selected.type }} · {{ selected.time }}</span>
        <h2>{{ selected.title }}</h2>
        <p>{{ selected.content }}</p>
        <ul>
          <li>预约、病历、处方和报告类消息可回到对应患者服务页面继续处理。</li>
          <li>如消息内容与实际就诊安排不一致，请以医生和医院现场确认为准。</li>
          <li>出现急症或病情明显变化时，请直接线下就医。</li>
        </ul>
      </div>
    </div>
  </section>
</template>
