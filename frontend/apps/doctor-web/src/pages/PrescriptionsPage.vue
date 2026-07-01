<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { displayText, formatApiError, formatDateTime, useDoctorWorkflowStore, usePagination, type Prescription } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, PaginationBar, Toast } from "@smart-cloud-brain/shared-ui";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import { liveRows, statusLabel, statusTone } from "../doctorPresentation";
import { usePrescriptionFilters, filterGroups } from "../composables/usePrescriptionFilters";

const workflow = useDoctorWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
const displayPrescriptions = liveRows(prescriptions);
const allRows = ref<Prescription[]>([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>(`toast`);
const selected = ref<Prescription | null>(null);
const router = useRouter();

const {
  search, filteredRows,
  openFilter, filterValue, setFilterValue, filterLabel, closeMenu,
} = usePrescriptionFilters(() => allRows.value);

const { currentPage, pageSize, total, pageRows } = usePagination(filteredRows, 8);

/* ── Teleport filter menu positioning ── */
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
      ? { position: "fixed", left: `${rect.left}px`, top: `${rect.top - 6}px`, transform: `translateY(-100%)`, width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px`, zIndex: `9999` }
      : { position: "fixed", left: `${rect.left}px`, top: `${rect.bottom + 6}px`, width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px`, zIndex: `9999` };
  });
}

watch(openFilter, () => { if (openFilter.value) positionFilterMenu(); });
watch([search, filteredRows], () => { currentPage.value = 1; });
watch(displayPrescriptions, (value) => { allRows.value = [...value]; });

function handleReturnToModify() {
  const regId = selected.value?.registrationId;
  selected.value = null;
  if (regId) {
    router.push(`/consult/${regId}`);
  }
}

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    allRows.value = [...displayPrescriptions.value];
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", "处方数据已同步最新状态。");
  } catch (err) {
    error.value = formatApiError(err, "处方加载失败，请稍后重试。");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

onMounted(() => {
  refresh(true, false);
  window.addEventListener("scroll", closeMenu, true);
  window.addEventListener("resize", closeMenu);
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", closeMenu, true);
  window.removeEventListener("resize", closeMenu);
});
</script>

<template>
  <section class="panel prescription-workbench">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">处方记录</p>
        <h2>处方审核结果</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="refresh()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        刷新
      </button>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="!loaded && loading" title="正在同步处方" />

      <!-- Filter toolbar -->
      <div class="prescription-toolbar">
        <div class="prescription-toolbar-row primary">
          <label class="prescription-search">
            <span>搜索 · 匹配 {{ filteredRows.length }} 条</span>
            <div class="prescription-search-box">
              <svg aria-hidden="true" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.35-4.35" />
              </svg>
              <input v-model.trim="search" type="search" placeholder="患者姓名、处方号" />
              <button v-if="search" class="prescription-search-clear" type="button" aria-label="清空搜索" @click="search = ''">
                <svg aria-hidden="true" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
          </label>
        </div>
        <div class="prescription-toolbar-row filters">
          <div v-for="group in filterGroups" :key="group.key" class="prescription-filter" @click.stop>
            <button
              :ref="(el: any) => { if (el) filterTriggerRefs[group.key] = el }"
              class="prescription-filter-trigger"
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
              <div v-if="openFilter === group.key" class="prescription-filter-menu" role="listbox" :style="filterMenuStyle">
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

      <div class="table-wrap table-breakout">
        <table class="data-table">
          <thead>
            <tr>
              <th>处方号</th>
              <th>患者</th>
              <th>创建时间</th>
              <th>药品数</th>
              <th>状态</th>
              <th>风险</th>
              <th class="actions-cell">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.prescriptionId)">
              <td>#RX{{ displayText(item.prescriptionId) }}</td>
              <td>{{ displayText(item.patientName, String(item.patientId ?? "")) }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ displayText(item.drugCount ?? item.items?.length, "0") }}</td>
              <td><span class="tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td><span class="tag" :class="statusTone(item.riskLevel)">{{ statusLabel(item.riskLevel, "未审核") }}</span></td>
              <td class="doctor-prescription-actions"><button :class="displayText(item.riskLevel).toUpperCase() === 'HIGH' ? 'action-btn danger' : 'action-btn'" type="button" @click="selected = item">{{ displayText(item.riskLevel).toUpperCase() === "HIGH" ? "复核" : "详情" }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-if="!loading && !pageRows.length" title="暂无匹配处方" message="调整筛选条件或刷新后再查看。" />
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>
    <PrescriptionRiskModal :open="Boolean(selected)" :result="selected" @close="handleReturnToModify" @confirm="selected = null" />
  </section>
</template>

<style scoped>
.doctor-prescription-actions {
  text-align: right;
}

.prescription-toolbar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prescription-toolbar-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.prescription-toolbar-row.primary {
  gap: 16px;
}

.prescription-search {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 200px;
}

.prescription-search > span {
  font-size: 12px;
  color: var(--muted, #64748b);
}

.prescription-search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  padding: 6px 12px;
  background: var(--surface, #fff);
}

.prescription-search-box svg {
  flex-shrink: 0;
  color: var(--muted, #94a3b8);
}

.prescription-search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
}

.prescription-search-clear {
  display: flex;
  align-items: center;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--muted, #94a3b8);
  padding: 0;
}

.prescription-segment {
  display: inline-flex;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.prescription-segment-thumb {
  position: absolute;
  inset: 0;
  background: var(--primary, #0b5f78);
  border-radius: 7px;
  transition: transform 0.2s ease;
  pointer-events: none;
}

.prescription-segment button {
  position: relative;
  padding: 6px 16px;
  border: none;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  color: var(--muted, #64748b);
  transition: color 0.15s;
  z-index: 1;
}

.prescription-segment button.active {
  color: #fff;
}

.prescription-segment button:not(:last-child) {
  border-right: 1px solid var(--border, #e2e8f0);
}

.prescription-filter {
  position: relative;
}

.prescription-filter-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: var(--surface, #fff);
  font-size: 13px;
  cursor: pointer;
  color: var(--muted, #64748b);
  white-space: nowrap;
}

.prescription-filter-trigger strong {
  color: var(--text, #1e293b);
  font-weight: 500;
}

.prescription-filter-menu {
  background: var(--surface, #fff);
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
}

.prescription-filter-menu button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 14px;
  border: none;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  color: var(--text, #1e293b);
}

.prescription-filter-menu button:hover {
  background: var(--surface-alt, #f1f5f9);
}

.prescription-filter-menu button.selected {
  color: var(--primary, #0b5f78);
  font-weight: 500;
}
</style>
