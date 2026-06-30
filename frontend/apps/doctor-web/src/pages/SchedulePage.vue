<script setup lang="ts">
import { computed, inject, ref, watch, type Ref } from "vue";
import { api, displayText, formatApiError, toNumber, usePagination, type Schedule } from "@smart-cloud-brain/shared-api";
import { DatePicker, EmptyState, ErrorState, LoadingState, PaginationBar, ScbSelect, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";

const rows = ref<Schedule[]>([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const startDate = ref(new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10));
const endDate = ref(new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10));
const status = ref("");
let scheduleRequestId = 0;
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

const statusOptions = [
  { value: "", label: "全部状态" },
  { value: "PUBLISHED", label: "已发布" },
  { value: "CANCELLED", label: "已取消" },
];

const scheduleCount = computed(() => rows.value.length);
const bookedTotal = computed(() => rows.value.reduce((sum, item) => sum + toNumber(item.booked), 0));
const remainingTotal = computed(() => rows.value.reduce((sum, item) => sum + toNumber(item.remainingCapacity), 0));
const capacityTotal = computed(() => rows.value.reduce((sum, item) => sum + toNumber(item.capacity), 0));
const utilizationRate = computed(() => capacityTotal.value
  ? Math.round((bookedTotal.value / capacityTotal.value) * 100)
  : 0);
const dateRangeLabel = computed(() => `${startDate.value} 至 ${endDate.value}`);

function updateStartDate(value: string) {
  startDate.value = value;
  if (value && endDate.value && value > endDate.value) endDate.value = value;
}

function updateEndDate(value: string) {
  endDate.value = value;
  if (value && startDate.value && value < startDate.value) startDate.value = value;
}

async function refresh(options: { notify?: boolean } = {}) {
  const requestId = ++scheduleRequestId;
  loading.value = true;
  error.value = "";
  try {
    const nextRows = await api.doctorSchedules({
      startDate: startDate.value,
      endDate: endDate.value,
      status: status.value,
    }) as Schedule[];
    if (requestId !== scheduleRequestId) return;
    rows.value = nextRows;
    loaded.value = true;
    if (options.notify) toast?.value?.success("数据已刷新", "排班数据已同步最新状态。");
  } catch (err) {
    if (requestId !== scheduleRequestId) return;
    error.value = formatApiError(err, "加载排班失败");
    toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (requestId === scheduleRequestId) loading.value = false;
  }
}

watch([startDate, endDate, status], () => {
  currentPage.value = 1;
  void refresh();
}, { immediate: true });
</script>

<template>
  <section class="clinical-page doctor-schedule-page">
    <section class="panel schedule-panel">
      <div class="schedule-overview">
        <div class="schedule-heading">
          <span class="schedule-heading-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="17" rx="2" />
              <path d="M8 2v4M16 2v4M3 10h18M8 14h3M8 17h6" />
            </svg>
          </span>
          <div>
            <p class="eyebrow">排班管理</p>
            <h2>我的排班</h2>
            <p class="schedule-range-copy">{{ dateRangeLabel }} · 当前筛选结果</p>
          </div>
        </div>

        <div class="schedule-metrics" aria-label="排班概览">
          <div class="schedule-metric">
            <span>排班数</span>
            <strong>{{ scheduleCount }}</strong>
            <small>个门诊时段</small>
          </div>
          <div class="schedule-metric">
            <span>已预约</span>
            <strong>{{ bookedTotal }}</strong>
            <small>人次</small>
          </div>
          <div class="schedule-metric">
            <span>剩余号源</span>
            <strong>{{ remainingTotal }}</strong>
            <small>个可预约名额</small>
          </div>
          <div class="schedule-metric">
            <span>预约利用率</span>
            <strong>{{ utilizationRate }}%</strong>
            <small>{{ bookedTotal }} / {{ capacityTotal }}</small>
          </div>
        </div>
      </div>

      <div class="schedule-filter-bar" aria-label="排班筛选">
        <div class="schedule-filter-field">
          <span>开始日期</span>
          <DatePicker
            :model-value="startDate"
            aria-label="排班开始日期"
            :clearable="false"
            @update:model-value="updateStartDate"
          />
        </div>
        <span class="schedule-filter-separator" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>
        </span>
        <div class="schedule-filter-field">
          <span>结束日期</span>
          <DatePicker
            :model-value="endDate"
            aria-label="排班结束日期"
            :clearable="false"
            @update:model-value="updateEndDate"
          />
        </div>
        <div class="schedule-filter-field schedule-status-field">
          <span>排班状态</span>
          <ScbSelect v-model="status" :options="statusOptions" />
        </div>
        <button class="refresh-btn schedule-refresh-btn" type="button" :disabled="loading" @click="refresh({ notify: true })">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </button>
      </div>

      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="!loaded && loading" />
        <div v-if="rows.length" class="table-wrap table-breakout">
          <table class="data-table">
            <thead><tr><th>日期</th><th>时间</th><th>科室</th><th>容量</th><th>已预约</th><th>剩余</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="item in pageRows" :key="String(item.id)">
                <td>{{ displayText(item.startTime?.slice(0, 10) || "") }}</td>
                <td>{{ displayText(item.startTime?.slice(11, 16) + "-" + item.endTime?.slice(11, 16) || "") }}</td>
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.capacity) }}</td>
                <td>{{ displayText(item.booked, "0") }}</td>
                <td>{{ displayText(item.remainingCapacity, "0") }}</td>
                <td><StatusTag :status="displayText(item.status)" /></td>
              </tr>
            </tbody>
          </table>
          <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
        </div>
        <EmptyState v-else title="暂无排班" />
      </div>
    </section>
  </section>
</template>
