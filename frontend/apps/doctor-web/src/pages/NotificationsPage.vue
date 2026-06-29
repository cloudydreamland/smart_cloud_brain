<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  api,
  displayText,
  formatApiError,
  toNumber,
  useDoctorWorkflowStore,
  usePagination,
  type Notification,
} from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, Toast } from "@smart-cloud-brain/shared-ui";
import NotificationDetailModal from "../components/NotificationDetailModal.vue";
import { formatTime, liveRows, statusLabel, statusTone } from "../doctorPresentation";

type FilterKey = "read" | "type" | "risk" | "sort";

const emit = defineEmits<{ refresh: [] }>();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { notifications, registrations } = storeToRefs(workflow);
const displayNotifications = liveRows(notifications);
const displayRegistrations = liveRows(registrations);
const allRows = ref<Notification[]>([]);
const selected = ref<Notification | null>(null);
const error = ref("");
const loading = ref(false);
const loaded = ref(false);
const search = ref("");
const readFilter = ref("ALL");
const handleFilter = ref("ALL");
const typeFilter = ref("ALL");
const riskFilter = ref("ALL");
const sortBy = ref("DEFAULT");
const openFilter = ref<FilterKey | null>(null);
const openActionMenu = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");

const handleOptions = [
  { value: "ALL", label: "全部" },
  { value: "PENDING", label: "待处理" },
  { value: "HANDLED", label: "已处理" },
  { value: "IGNORED", label: "已忽略" },
];

const filterGroups = [
  {
    key: "read",
    label: "阅读状态",
    options: [
      { value: "ALL", label: "全部阅读" },
      { value: "UNREAD", label: "未读" },
      { value: "READ", label: "已读" },
    ],
  },
  {
    key: "type",
    label: "通知类型",
    options: [
      { value: "ALL", label: "全部类型" },
      { value: "PRESCRIPTION", label: "处方" },
      { value: "TRIAGE", label: "分诊" },
      { value: "MEDICAL_RECORD", label: "病历" },
      { value: "REGISTRATION", label: "挂号" },
      { value: "SYSTEM", label: "系统" },
    ],
  },
  {
    key: "risk",
    label: "风险等级",
    options: [
      { value: "ALL", label: "全部风险" },
      { value: "HIGH", label: "高风险" },
      { value: "MEDIUM", label: "中风险" },
      { value: "LOW", label: "低风险" },
    ],
  },
  {
    key: "sort",
    label: "排序方式",
    options: [
      { value: "DEFAULT", label: "待办优先" },
      { value: "CREATED_DESC", label: "最新优先" },
      { value: "CREATED_ASC", label: "最早优先" },
      { value: "RISK_DESC", label: "风险优先" },
      { value: "UNREAD_FIRST", label: "未读优先" },
      { value: "HANDLED_FIRST", label: "已处理优先" },
    ],
  },
] as const;

const stats = computed(() => {
  const all = allRows.value.length ? allRows.value : displayNotifications.value;
  const today = new Date().toDateString();
  return [
    {
      label: "待处理",
      value: all.filter((item) => handleValue(item) === "PENDING").length,
      tone: "teal",
      icon: "list",
    },
    {
      label: "未读",
      value: all.filter((item) => readValue(item) !== "READ").length,
      tone: "blue",
      icon: "bell",
    },
    {
      label: "高/中风险",
      value: all.filter((item) => ["HIGH", "MEDIUM"].includes(riskValue(item))).length,
      tone: "red",
      icon: "warning",
    },
    {
      label: "今日新增",
      value: all.filter((item) => item.createdAt && new Date(item.createdAt).toDateString() === today).length,
      tone: "green",
      icon: "check",
    },
  ];
});

const filteredRows = computed(() => {
  const keyword = search.value.trim().toLowerCase();
  return allRows.value
    .filter((item) => handleFilter.value === "ALL" || handleValue(item) === handleFilter.value)
    .filter((item) => readFilter.value === "ALL" || readValue(item) === readFilter.value)
    .filter((item) => typeFilter.value === "ALL" || typeCategory(item.type) === typeFilter.value)
    .filter((item) => riskFilter.value === "ALL" || riskValue(item) === riskFilter.value)
    .filter((item) => !keyword || searchText(item).includes(keyword))
    .slice()
    .sort(rowComparator(sortBy.value));
});

const { currentPage, pageSize, total, pageRows } = usePagination(filteredRows, 10);

function riskValue(item: Notification) {
  return displayText(item.riskLevel, "INFO").toUpperCase();
}

function readValue(item: Notification) {
  return displayText(item.readStatus, "UNREAD").toUpperCase();
}

function handleValue(item: Notification) {
  return displayText(item.handleStatus, readValue(item) === "READ" ? "HANDLED" : "PENDING").toUpperCase();
}

function typeLabel(type: unknown) {
  const category = typeCategory(type);
  if (category === "TRIAGE") return "分诊";
  if (category === "PRESCRIPTION") return "处方";
  if (category === "MEDICAL_RECORD") return "病历";
  if (category === "REGISTRATION") return "挂号";
  return "系统";
}

function typeTone(type: unknown) {
  const category = typeCategory(type);
  if (category === "PRESCRIPTION") return "warning";
  if (category === "TRIAGE") return "info";
  if (category === "MEDICAL_RECORD") return "success";
  return "active";
}

function typeCategory(type: unknown) {
  const value = displayText(type, "SYSTEM_NOTICE").toUpperCase();
  if (value.includes("PRESCRIPTION")) return "PRESCRIPTION";
  if (value.includes("TRIAGE")) return "TRIAGE";
  if (value.includes("MEDICAL_RECORD")) return "MEDICAL_RECORD";
  if (value.includes("REGISTRATION")) return "REGISTRATION";
  return "SYSTEM";
}

function relationText(item: Notification) {
  const parts = [];
  if (toNumber(item.patientId)) parts.push(`患者 #${item.patientId}`);
  if (toNumber(item.prescriptionId)) parts.push(`处方 #${item.prescriptionId}`);
  if (toNumber(item.triageRecordId)) parts.push(`分诊 #${item.triageRecordId}`);
  if (toNumber(item.medicalRecordId)) parts.push(`病历 #${item.medicalRecordId}`);
  return parts.join(" · ") || "未关联业务对象";
}

function searchText(item: Notification) {
  return [
    displayText(item.title, ""),
    displayText(item.content, ""),
    displayText(item.type, ""),
    typeLabel(item.type),
    statusLabel(riskValue(item)),
    statusLabel(readValue(item)),
    statusLabel(handleValue(item)),
    relationText(item),
    toNumber(item.patientId) ? `患者 ${item.patientId}` : "",
    toNumber(item.prescriptionId) ? `处方 ${item.prescriptionId}` : "",
    toNumber(item.triageRecordId) ? `分诊 ${item.triageRecordId}` : "",
    toNumber(item.medicalRecordId) ? `病历 ${item.medicalRecordId}` : "",
  ].join(" ").toLowerCase();
}

function createdTime(item: Notification) {
  const time = new Date(displayText(item.createdAt, "")).getTime();
  return Number.isFinite(time) ? time : 0;
}

function riskPriority(item: Notification) {
  const value = riskValue(item);
  if (value === "HIGH") return 0;
  if (value === "MEDIUM") return 1;
  if (value === "LOW") return 2;
  return 3;
}

function handlePriority(item: Notification) {
  const value = handleValue(item);
  if (value === "PENDING") return 0;
  if (value === "HANDLED") return 1;
  if (value === "IGNORED") return 2;
  return 3;
}

function rowComparator(sort: string) {
  return (a: Notification, b: Notification) => {
    if (sort === "CREATED_ASC") return createdTime(a) - createdTime(b);
    if (sort === "CREATED_DESC") return createdTime(b) - createdTime(a);
    if (sort === "RISK_DESC") return riskPriority(a) - riskPriority(b) || createdTime(b) - createdTime(a);
    if (sort === "UNREAD_FIRST") return (readValue(a) === "READ" ? 1 : 0) - (readValue(b) === "READ" ? 1 : 0) || createdTime(b) - createdTime(a);
    if (sort === "HANDLED_FIRST") return (handleValue(a) === "HANDLED" ? 0 : 1) - (handleValue(b) === "HANDLED" ? 0 : 1) || createdTime(b) - createdTime(a);
    return handlePriority(a) - handlePriority(b) || riskPriority(a) - riskPriority(b) || createdTime(b) - createdTime(a);
  };
}

function processable(item: Notification) {
  const category = typeCategory(item.type);
  return ["PRESCRIPTION", "MEDICAL_RECORD", "TRIAGE", "REGISTRATION"].includes(category);
}

function primaryActionLabel(item: Notification) {
  return processable(item) ? "去处理" : "查看详情";
}

function filterValue(key: FilterKey) {
  if (key === "read") return readFilter.value;
  if (key === "type") return typeFilter.value;
  if (key === "risk") return riskFilter.value;
  return sortBy.value;
}

function setFilterValue(key: FilterKey, value: string) {
  if (key === "read") readFilter.value = value;
  if (key === "type") typeFilter.value = value;
  if (key === "risk") riskFilter.value = value;
  if (key === "sort") sortBy.value = value;
  openFilter.value = null;
}

function filterLabel(key: FilterKey) {
  const group = filterGroups.find((item) => item.key === key);
  return group?.options.find((item) => item.value === filterValue(key))?.label ?? group?.label ?? "";
}

function notificationKey(item: Notification) {
  return String(item.notificationId ?? item.createdAt ?? item.title ?? "");
}

function toggleActionMenu(item: Notification) {
  const key = notificationKey(item);
  openFilter.value = null;
  openActionMenu.value = openActionMenu.value === key ? "" : key;
}

function closeMenus() {
  openFilter.value = null;
  openActionMenu.value = "";
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") closeMenus();
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    allRows.value = [...displayNotifications.value];
    loaded.value = true;
    toast?.value?.success("数据已刷新", "通知数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "通知列表加载失败，请稍后重试。");
    toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    loading.value = false;
  }
}

async function markRead(item = selected.value) {
  if (!item) return;
  loading.value = true;
  error.value = "";
  try {
    await api.markNotificationRead(toNumber(item.notificationId));
    selected.value = null;
    emit("refresh");
    await refresh();
    toast?.value?.success("已标记为已读", displayText(item.title));
  } catch (err) {
    selected.value = null;
    error.value = formatApiError(err, "标记通知失败，请刷新后重试。");
    toast?.value?.error("操作失败", error.value);
  } finally {
    loading.value = false;
  }
}

async function handleNotification(item: Notification | null, handleStatus: "HANDLED" | "IGNORED") {
  if (!item) return;
  loading.value = true;
  error.value = "";
  try {
    await api.handleNotification(toNumber(item.notificationId), handleStatus);
    selected.value = null;
    emit("refresh");
    await refresh();
    toast?.value?.success(handleStatus === "HANDLED" ? "已标记为已处理" : "已忽略通知", displayText(item.title));
  } catch (err) {
    selected.value = null;
    error.value = formatApiError(err, "处理通知失败，请刷新后重试。");
    toast?.value?.error("操作失败", error.value);
  } finally {
    loading.value = false;
  }
}

function goProcess(item: Notification) {
  const category = typeCategory(item.type);
  if (!processable(item)) {
    selected.value = item;
    return;
  }
  if (category === "TRIAGE" || category === "REGISTRATION") {
    const triageRecordId = toNumber(item.triageRecordId);
    const patientId = toNumber(item.patientId);
    const registration = displayRegistrations.value.find((row) => {
      const sameTriage = triageRecordId && toNumber(row.triageRecordId) === triageRecordId;
      const samePatient = patientId && toNumber(row.patientId) === patientId;
      return sameTriage || samePatient;
    });
    if (registration && toNumber(registration.registrationId)) {
      router.push(`/consult/${displayText(registration.registrationId)}`);
      return;
    }
    router.push({ name: "doctor-queue", query: { ...(patientId ? { patientId: String(patientId) } : {}), ...(triageRecordId ? { triageRecordId: String(triageRecordId) } : {}), notice: "select-patient" } });
    return;
  }
  if (category === "PRESCRIPTION") {
    router.push({ name: "doctor-prescriptions", query: toNumber(item.prescriptionId) ? { prescriptionId: String(item.prescriptionId) } : {} });
    return;
  }
  if (category === "MEDICAL_RECORD") {
    router.push({ name: "doctor-records", query: toNumber(item.medicalRecordId) ? { recordId: String(item.medicalRecordId) } : {} });
    return;
  }
}

watch([search, readFilter, handleFilter, typeFilter, riskFilter, sortBy], () => {
  currentPage.value = 1;
});
watch(displayNotifications, (value) => {
  allRows.value = [...value];
});
onMounted(() => {
  document.addEventListener("click", closeMenus);
  document.addEventListener("keydown", handleDocumentKeydown);
});
onBeforeUnmount(() => {
  document.removeEventListener("click", closeMenus);
  document.removeEventListener("keydown", handleDocumentKeydown);
});
refresh();
</script>

<template>
  <section class="panel notification-workbench">
    <header class="notification-hero">
      <div class="panel-title">
        <p class="eyebrow">待办与通知</p>
        <h2>通知中心</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>

    <div class="notification-body">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading && !allRows.length" title="正在同步通知" />

      <div class="notification-stats" aria-label="通知统计">
        <div v-for="item in stats" :key="item.label" class="notification-stat" :class="item.tone">
          <div class="notification-stat-head">
            <div class="notification-stat-icon" aria-hidden="true">
              <svg v-if="item.icon === 'list'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M4 6h16M4 12h10M4 18h14" />
              </svg>
              <svg v-else-if="item.icon === 'bell'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <svg v-else-if="item.icon === 'warning'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <path d="M12 9v4M12 17h.01" />
              </svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <path d="m22 4-10 10.01-3-3" />
              </svg>
            </div>
            <span>{{ item.label }}</span>
          </div>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <div class="notification-toolbar">
        <div class="notification-toolbar-row primary">
          <label class="notification-search">
            <span>搜索 · 匹配 {{ filteredRows.length }} 条</span>
            <div class="notification-search-box">
              <svg aria-hidden="true" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.35-4.35" />
              </svg>
              <input v-model.trim="search" type="search" placeholder="标题、内容、患者或业务编号" />
              <button v-if="search" class="notification-search-clear" type="button" aria-label="清空搜索" @click="search = ''">
                <svg aria-hidden="true" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
          </label>
          <div class="notification-segment" aria-label="处理状态">
            <span class="notification-segment-thumb" :style="{ transform: `translateX(${Math.max(0, handleOptions.findIndex((option) => option.value === handleFilter)) * 100}%)` }" aria-hidden="true"></span>
            <button
              v-for="option in handleOptions"
              :key="option.value"
              type="button"
              :class="{ active: handleFilter === option.value }"
              @click="handleFilter = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
        <div class="notification-toolbar-row filters">
          <div v-for="group in filterGroups" :key="group.key" class="notification-filter" @click.stop>
            <button
              class="notification-filter-trigger"
              type="button"
              :aria-expanded="openFilter === group.key"
              :aria-label="group.label"
              @click="openFilter = openFilter === group.key ? null : group.key"
            >
              <span>{{ group.label }}</span>
              <strong>{{ filterLabel(group.key) }}</strong>
              <svg aria-hidden="true" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m6 9 6 6 6-6" />
              </svg>
            </button>
            <div v-if="openFilter === group.key" class="notification-filter-menu" role="listbox">
              <button
                v-for="option in group.options"
                :key="option.value"
                type="button"
                role="option"
                :aria-selected="filterValue(group.key) === option.value"
                :class="{ selected: filterValue(group.key) === option.value }"
                @click="setFilterValue(group.key, option.value)"
              >
                <span>{{ option.label }}</span>
                <svg v-if="filterValue(group.key) === option.value" aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m20 6-11 11-5-5" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="notification-list-wrap">
        <div class="notification-list-head" aria-hidden="true">
          <span>通知内容</span>
          <span>状态</span>
          <span>时间</span>
          <span>操作</span>
        </div>
        <div class="notification-list" role="list">
          <article
            v-for="item in pageRows"
            :key="notificationKey(item)"
            class="notification-task-row"
            :class="{ unread: readValue(item) !== 'READ', pending: handleValue(item) === 'PENDING' }"
            role="listitem"
          >
            <div class="notification-main-cell">
              <div class="notification-title-line">
                <i v-if="readValue(item) !== 'READ'" aria-hidden="true"></i>
                <strong>{{ displayText(item.title, "系统通知") }}</strong>
              </div>
              <p>{{ displayText(item.content, "暂无通知正文") }}</p>
              <span>{{ relationText(item) }}</span>
            </div>
            <div class="notification-chip-cell" aria-label="通知状态">
              <span class="tag" :class="typeTone(item.type)">{{ typeLabel(item.type) }}</span>
              <span class="tag" :class="statusTone(riskValue(item))">{{ statusLabel(riskValue(item)) }}</span>
              <span class="tag" :class="statusTone(readValue(item))">{{ statusLabel(readValue(item)) }}</span>
              <span class="tag" :class="statusTone(handleValue(item))">{{ statusLabel(handleValue(item)) }}</span>
            </div>
            <time class="notification-time">{{ formatTime(item.createdAt) }}</time>
            <div class="notification-row-actions" @click.stop>
              <button type="button" class="action-btn primary" @click="goProcess(item)">{{ primaryActionLabel(item) }}</button>
              <div class="notification-action-menu">
                <button
                  type="button"
                  class="action-btn compact"
                  :aria-expanded="openActionMenu === notificationKey(item)"
                  @click="toggleActionMenu(item)"
                >
                  更多
                  <svg aria-hidden="true" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="m6 9 6 6 6-6" />
                  </svg>
                </button>
                <div v-if="openActionMenu === notificationKey(item)" class="notification-action-popover" role="menu">
                  <button type="button" role="menuitem" @click="selected = item; openActionMenu = ''">查看详情</button>
                  <button v-if="readValue(item) !== 'READ'" type="button" role="menuitem" @click="markRead(item)">标记已读</button>
                  <button v-if="handleValue(item) === 'PENDING'" type="button" role="menuitem" @click="handleNotification(item, 'HANDLED')">标记已处理</button>
                  <button v-if="handleValue(item) === 'PENDING'" type="button" role="menuitem" class="danger" @click="handleNotification(item, 'IGNORED')">忽略通知</button>
                </div>
              </div>
            </div>
          </article>
        </div>
        <EmptyState v-if="!loading && !pageRows.length" title="暂无匹配通知" message="调整筛选条件或刷新后再查看。" />
      </div>

      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>

    <NotificationDetailModal
      :open="Boolean(selected)"
      :notification="selected"
      @close="selected = null"
      @read="markRead(selected)"
      @handle="(status) => handleNotification(selected, status)"
      @process="selected && goProcess(selected)"
    />
  </section>
</template>
