<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue";
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
import {
  useNotificationFilters,
  handleOptions,
  filterGroups,
  riskValue,
  readValue,
  handleValue,
  typeLabel,
  typeTone,
  typeCategory,
  relationText,
  notificationKey,
  processable,
  primaryActionLabel,
} from "../composables/useNotificationFilters";

const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { notifications, registrations } = storeToRefs(workflow);
const displayNotifications = liveRows(notifications);
const displayRegistrations = liveRows(registrations);
const allRows = ref<Notification[]>([]);
const selected = ref<Notification | null>(null);
const error = ref("");
const loading = ref(false);
const markingRead = ref(false);
const loaded = ref(false);
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");

const {
  search, readFilter, handleFilter, typeFilter, riskFilter, sortBy,
  openFilter, openActionMenu, filteredRows,
  filterValue, setFilterValue, filterLabel, toggleActionMenu, closeMenus,
} = useNotificationFilters(() => allRows.value);

/* ── Teleport 筛选菜单定位（ScbSelect 模式） ── */
const filterTriggerRefs = ref<Record<string, HTMLElement>>({});
const filterMenuStyle = ref<Record<string, string>>({});

function positionFilterMenu() {
  const key = openFilter.value;
  if (!key) return;
  nextTick(() => {
    const el = filterTriggerRefs.value[key];
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const MENU_MAX_H = 300;
    const spaceBelow = window.innerHeight - rect.bottom;
    const openAbove = spaceBelow < MENU_MAX_H + 8 && rect.top > spaceBelow;
    filterMenuStyle.value = openAbove
      ? { position: "fixed", left: `${rect.left}px`, top: `${rect.top - 6}px`, transform: "translateY(-100%)", width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px`, zIndex: "9999" }
      : { position: "fixed", left: `${rect.left}px`, top: `${rect.bottom + 6}px`, width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px`, zIndex: "9999" };
  });
}

watch(openFilter, () => { if (openFilter.value) positionFilterMenu(); });

const stats = computed(() => {
  const all = allRows.value.length ? allRows.value : displayNotifications.value;
  const today = new Date().toDateString();
  return [
    { label: "待处理", value: all.filter((item) => handleValue(item) === "PENDING").length, tone: "teal", icon: "list" },
    { label: "未读", value: all.filter((item) => readValue(item) !== "READ").length, tone: "blue", icon: "bell" },
    { label: "高/中风险", value: all.filter((item) => ["HIGH", "MEDIUM"].includes(riskValue(item))).length, tone: "red", icon: "warning" },
    { label: "今日新增", value: all.filter((item) => item.createdAt && new Date(item.createdAt).toDateString() === today).length, tone: "green", icon: "check" },
  ];
});

const { currentPage, pageSize, total, pageRows } = usePagination(filteredRows, 10);

watch([search, readFilter, handleFilter, typeFilter, riskFilter, sortBy], () => {
  currentPage.value = 1;
});
watch(displayNotifications, (value) => {
  allRows.value = [...value];
});

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    allRows.value = [...displayNotifications.value];
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", "通知数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "通知加载失败，请稍后重试。");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

async function markRead(item = selected.value) {
  if (!item) return;
  markingRead.value = true;
  error.value = "";
  try {
    await api.markNotificationRead(toNumber(item.notificationId));
    selected.value = null;
    await refresh();
    toast?.value?.success("已标记为已读", displayText(item.title));
  } catch (err) {
    selected.value = null;
    error.value = formatApiError(err, "标记通知失败，请刷新后重试。");
    toast?.value?.error("操作失败", error.value);
  } finally {
    markingRead.value = false;
  }
}

async function handleNotificationAction(item: Notification | null, handleStatus: "HANDLED" | "IGNORED") {
  if (!item) return;
  loading.value = true;
  error.value = "";
  try {
    await api.handleNotification(toNumber(item.notificationId), handleStatus);
    selected.value = null;
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

onMounted(() => {
  refresh(true, false);
  window.addEventListener("scroll", closeMenus, true);
  window.addEventListener("resize", closeMenus);
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", closeMenus, true);
  window.removeEventListener("resize", closeMenus);
});
</script>

<template>
  <section class="panel notification-workbench">
    <header class="notification-hero">
      <div class="panel-title">
        <p class="eyebrow">待办与通知</p>
        <h2>通知中心</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh()">
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
            <span class="notification-segment-thumb" :style="{ transform: `translateX(${Math.max(0, handleOptions.findIndex((option) => option.value === handleFilter) * 100)}%)` }" aria-hidden="true"></span>
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
              :ref="(el: any) => { if (el) filterTriggerRefs[group.key] = el }"
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
            <Teleport to="body">
              <div v-if="openFilter === group.key" class="notification-filter-menu" role="listbox" :style="filterMenuStyle">
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
            </Teleport>
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
                  <button v-if="handleValue(item) === 'PENDING'" type="button" role="menuitem" @click="handleNotificationAction(item, 'HANDLED')">标记已处理</button>
                  <button v-if="handleValue(item) === 'PENDING'" type="button" role="menuitem" class="danger" @click="handleNotificationAction(item, 'IGNORED')">忽略通知</button>
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
      @handle="(status) => handleNotificationAction(selected, status)"
      @process="selected && goProcess(selected)"
    />
  </section>
</template>
