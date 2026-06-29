<script setup lang="ts">
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import {
  IconAlertTriangle,
  IconChartLine,
  IconCircleCheck,
  IconDownload,
  IconLoader2,
  IconRefresh,
  IconStethoscope,
  IconUsers,
} from "@tabler/icons-vue";
import {
  api,
  displayText,
  formatApiError,
  type DeviceUsageStatsRow,
  type DoctorWorkloadRow,
  type PatientDistribution,
  type StatisticsOverview,
  type StatisticsReportRow,
  type StatisticsTrendRow,
} from "@smart-cloud-brain/shared-api";
import { ErrorState } from "@smart-cloud-brain/shared-ui";
import echarts, { type ECharts } from "../echarts";

const DAY_MS = 86_400_000;
const loading = ref(false);
const exporting = ref(false);
const loaded = ref(false);
const error = ref("");
const startDate = ref(new Date(Date.now() - 14 * DAY_MS).toISOString().slice(0, 10));
const endDate = ref(new Date().toISOString().slice(0, 10));
const overview = ref<StatisticsOverview>({});
const trend = ref<StatisticsTrendRow[]>([]);
const workload = ref<DoctorWorkloadRow[]>([]);
const distribution = ref<PatientDistribution>({});
const deviceUsage = ref<DeviceUsageStatsRow[]>([]);

const trendEl = ref<HTMLDivElement | null>(null);
const workloadEl = ref<HTMLDivElement | null>(null);
const patientEl = ref<HTMLDivElement | null>(null);
const deviceEl = ref<HTMLDivElement | null>(null);
let charts: ECharts[] = [];
let resizeObserver: ResizeObserver | null = null;

const metricCards = computed(() => [
  {
    label: "就诊总数",
    value: displayText(overview.value.registrations, "0"),
    icon: markRaw(IconChartLine),
    tone: "blue",
  },
  {
    label: "已完成",
    value: displayText(overview.value.completedRegistrations, "0"),
    icon: markRaw(IconCircleCheck),
    tone: "emerald",
  },
  {
    label: "患者数",
    value: displayText(overview.value.patients, "0"),
    icon: markRaw(IconUsers),
    tone: "violet",
  },
  {
    label: "设备",
    value: displayText(overview.value.devices, "0"),
    icon: markRaw(IconStethoscope),
    tone: "cyan",
    sub: overview.value.deviceWarnings ? `${overview.value.deviceWarnings} 预警` : "",
  },
]);

const genderData = computed(() => (distribution.value.gender ?? [])
  .map((item) => ({
    name: displayText(item.name, "未知"),
    value: Number(item.value || 0),
  }))
  .filter((item) => item.value > 0));
const patientTotal = computed(() => genderData.value.reduce((sum, item) => sum + item.value, 0));
const hasTrendData = computed(() => trend.value.length > 0);
const hasWorkloadData = computed(() => workload.value.length > 0);
const hasPatientData = computed(() => genderData.value.length > 0);
const hasDeviceData = computed(() => deviceUsage.value.length > 0);

function formatDay(iso: unknown): string {
  const value = displayText(iso);
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${String(date.getMonth() + 1).padStart(2, "0")}/${String(date.getDate()).padStart(2, "0")}`;
}

function disposeCharts() {
  charts.forEach((chart) => chart.dispose());
  charts = [];
}

function renderCharts() {
  disposeCharts();

  const axisText = { color: "#71807c", fontSize: 11 };
  const axisLine = { lineStyle: { color: "#d9e1de" } };
  const splitLine = { lineStyle: { color: "#edf1ef", type: "dashed" as const } };
  const tooltip = {
    trigger: "axis" as const,
    backgroundColor: "rgba(23, 36, 34, 0.94)",
    borderWidth: 0,
    padding: [10, 12],
    textStyle: { color: "#ffffff", fontSize: 12 },
    axisPointer: { type: "line" as const, lineStyle: { color: "#9fb1ac", type: "dashed" as const } },
  };

  if (trendEl.value && hasTrendData.value) {
    const chart = echarts.init(trendEl.value);
    chart.setOption({
      color: ["#2563eb"],
      grid: { left: 48, right: 22, top: 28, bottom: 42 },
      tooltip,
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: trend.value.map((item) => formatDay(item.day)),
        axisLabel: axisText,
        axisLine,
        axisTick: { show: false },
      },
      yAxis: {
        type: "value",
        minInterval: 1,
        axisLabel: axisText,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine,
      },
      series: [{
        name: "就诊量",
        type: "line",
        smooth: 0.35,
        symbol: "circle",
        symbolSize: 7,
        showSymbol: trend.value.length <= 14,
        lineStyle: { width: 3 },
        itemStyle: { borderColor: "#ffffff", borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(37, 99, 235, 0.24)" },
            { offset: 1, color: "rgba(37, 99, 235, 0.02)" },
          ]),
        },
        data: trend.value.map((item) => Number(item.registrations || 0)),
      }],
    });
    charts.push(chart);
  }

  if (workloadEl.value && hasWorkloadData.value) {
    const rows = workload.value.slice(0, 10);
    const chart = echarts.init(workloadEl.value);
    chart.setOption({
      color: ["#007a7a"],
      grid: { left: 88, right: 44, top: 24, bottom: 34 },
      tooltip: { ...tooltip, axisPointer: { type: "shadow" as const } },
      xAxis: {
        type: "value",
        minInterval: 1,
        axisLabel: axisText,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine,
      },
      yAxis: {
        type: "category",
        inverse: true,
        data: rows.map((item) => displayText(item.doctor_name, "未命名医生")),
        axisLabel: { ...axisText, width: 72, overflow: "truncate" },
        axisLine,
        axisTick: { show: false },
      },
      series: [{
        name: "就诊量",
        type: "bar",
        barMaxWidth: 18,
        itemStyle: { borderRadius: [0, 7, 7, 0] },
        label: { show: true, position: "right", color: "#60716d", fontSize: 11 },
        data: rows.map((item) => Number(item.registrations || 0)),
      }],
    });
    charts.push(chart);
  }

  if (patientEl.value && hasPatientData.value) {
    const chart = echarts.init(patientEl.value);
    chart.setOption({
      color: ["#2563eb", "#8b5cf6", "#10b981", "#f59e0b"],
      title: {
        text: String(patientTotal.value),
        subtext: "患者",
        left: "37%",
        top: "39%",
        textAlign: "center",
        textStyle: { color: "#172422", fontSize: 24, fontWeight: 700 },
        subtextStyle: { color: "#60716d", fontSize: 11, lineHeight: 18 },
      },
      tooltip: {
        trigger: "item",
        backgroundColor: "rgba(23, 36, 34, 0.94)",
        borderWidth: 0,
        textStyle: { color: "#ffffff", fontSize: 12 },
        formatter: "{b}<br/>{c} 人（{d}%）",
      },
      legend: {
        orient: "vertical",
        right: "8%",
        top: "center",
        icon: "circle",
        itemWidth: 9,
        itemHeight: 9,
        itemGap: 16,
        textStyle: { color: "#60716d", fontSize: 12 },
      },
      series: [{
        name: "患者分布",
        type: "pie",
        radius: ["48%", "70%"],
        center: ["38%", "52%"],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: "#ffffff", borderWidth: 4, borderRadius: 8 },
        label: { show: false },
        emphasis: { scaleSize: 5 },
        data: genderData.value,
      }],
    });
    charts.push(chart);
  }

  if (deviceEl.value && hasDeviceData.value) {
    const rows = deviceUsage.value.slice(0, 10);
    const chart = echarts.init(deviceEl.value);
    chart.setOption({
      color: ["#0b8f87", "#dc6b36"],
      grid: { left: 48, right: 22, top: 50, bottom: 54 },
      tooltip: { ...tooltip, axisPointer: { type: "shadow" as const } },
      legend: {
        top: 8,
        right: 12,
        icon: "roundRect",
        itemWidth: 12,
        itemHeight: 7,
        textStyle: { color: "#60716d", fontSize: 11 },
      },
      xAxis: {
        type: "category",
        data: rows.map((item) => displayText(item.name, "未命名设备")),
        axisLabel: { ...axisText, interval: 0, rotate: rows.length > 5 ? 22 : 0, width: 68, overflow: "truncate" },
        axisLine,
        axisTick: { show: false },
      },
      yAxis: {
        type: "value",
        minInterval: 1,
        axisLabel: axisText,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine,
      },
      series: [
        {
          name: "使用量",
          type: "bar",
          barMaxWidth: 22,
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          data: rows.map((item) => Number(item.usage_count || 0)),
        },
        {
          name: "异常数",
          type: "bar",
          barMaxWidth: 22,
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          data: rows.map((item) => Number(item.abnormal_count || 0)),
        },
      ],
    });
    charts.push(chart);
  }
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const params = { startDate: startDate.value, endDate: endDate.value };
    const [nextOverview, nextTrend, nextWorkload, nextDistribution, nextDeviceUsage] = await Promise.all([
      api.statisticsOverview(),
      api.statisticsTrend(params),
      api.doctorWorkload(params),
      api.patientDistribution(),
      api.deviceUsageStatistics(),
    ]);
    overview.value = nextOverview as StatisticsOverview;
    trend.value = nextTrend as StatisticsTrendRow[];
    workload.value = nextWorkload as DoctorWorkloadRow[];
    distribution.value = nextDistribution as PatientDistribution;
    deviceUsage.value = nextDeviceUsage as DeviceUsageStatsRow[];
    loaded.value = true;
    await nextTick();
    renderCharts();
  } catch (err) {
    error.value = formatApiError(err, "运营统计数据加载失败");
  } finally {
    loading.value = false;
  }
}

async function exportCsv() {
  exporting.value = true;
  error.value = "";
  try {
    const rows = await api.statisticsReport({ startDate: startDate.value, endDate: endDate.value });
    const csv = [
      "metric,value",
      ...(rows as StatisticsReportRow[]).map((row) => `${displayText(row.metric, "")},${displayText(row.value, "0")}`),
    ].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `statistics-${startDate.value}-${endDate.value}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  } catch (err) {
    error.value = formatApiError(err, "统计报表导出失败");
  } finally {
    exporting.value = false;
  }
}

onMounted(async () => {
  resizeObserver = new ResizeObserver(() => charts.forEach((chart) => chart.resize()));
  [trendEl.value, workloadEl.value, patientEl.value, deviceEl.value]
    .filter((element): element is HTMLDivElement => Boolean(element))
    .forEach((element) => resizeObserver?.observe(element));
  await refresh();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  disposeCharts();
});
</script>

<template>
  <section class="analytics-section" aria-labelledby="analytics-title" :aria-busy="loading">
    <header class="analytics-header">
      <div class="analytics-heading">
        <h2 id="analytics-title">运营数据概览</h2>
      </div>
      <div class="analytics-toolbar">
        <label class="analytics-date-field">
          <span>开始日期</span>
          <input v-model="startDate" type="date" />
        </label>
        <span class="analytics-date-separator" aria-hidden="true">至</span>
        <label class="analytics-date-field">
          <span>结束日期</span>
          <input v-model="endDate" type="date" />
        </label>
        <button class="analytics-button" type="button" :disabled="loading" @click="refresh">
          <IconLoader2 v-if="loading" class="analytics-spin" :size="17" />
          <IconRefresh v-else :size="17" />
          {{ loading ? "更新中" : "刷新" }}
        </button>
        <button class="analytics-button primary" type="button" :disabled="exporting" @click="exportCsv">
          <IconLoader2 v-if="exporting" class="analytics-spin" :size="17" />
          <IconDownload v-else :size="17" />
          {{ exporting ? "导出中" : "导出 CSV" }}
        </button>
      </div>
    </header>

    <ErrorState v-if="error" class="analytics-error" title="统计数据暂不可用" :message="error">
      <button type="button" @click="refresh">重新加载</button>
    </ErrorState>

    <div v-if="loading && !loaded" class="analytics-loading" role="status">
      <IconLoader2 class="analytics-spin" :size="18" />
      正在同步运营数据…
    </div>

    <div class="analytics-metric-grid">
      <article v-for="metric in metricCards" :key="metric.label" class="analytics-metric-card">
        <div class="analytics-metric-head">
          <span class="analytics-metric-icon" :class="`is-${metric.tone}`">
            <component :is="metric.icon" :size="22" :stroke-width="1.9" />
          </span>
          <span>{{ metric.label }}</span>
        </div>
        <strong>{{ metric.value }}</strong>
        <span v-if="metric.sub" class="analytics-metric-sub">{{ metric.sub }}</span>
      </article>
    </div>

    <div class="analytics-chart-grid">
      <article class="analytics-chart-card">
        <header>
          <div>
            <h3>就诊趋势</h3>
            <p>所选日期范围内的每日就诊量</p>
          </div>
          <span class="analytics-chart-badge is-blue">趋势</span>
        </header>
        <div class="analytics-chart-body">
          <div ref="trendEl" class="analytics-chart-canvas" :class="{ 'is-hidden': !hasTrendData }"></div>
          <div v-if="loaded && !hasTrendData" class="analytics-chart-empty">暂无就诊趋势数据</div>
        </div>
      </article>

      <article class="analytics-chart-card">
        <header>
          <div>
            <h3>医生工作量</h3>
            <p>就诊量排名前 10 的医生</p>
          </div>
          <span class="analytics-chart-badge is-teal">排名</span>
        </header>
        <div class="analytics-chart-body">
          <div ref="workloadEl" class="analytics-chart-canvas" :class="{ 'is-hidden': !hasWorkloadData }"></div>
          <div v-if="loaded && !hasWorkloadData" class="analytics-chart-empty">暂无医生工作量数据</div>
        </div>
      </article>

      <article class="analytics-chart-card">
        <header>
          <div>
            <h3>患者分布</h3>
            <p>当前患者性别构成</p>
          </div>
          <span class="analytics-chart-badge is-violet">构成</span>
        </header>
        <div class="analytics-chart-body">
          <div ref="patientEl" class="analytics-chart-canvas" :class="{ 'is-hidden': !hasPatientData }"></div>
          <div v-if="loaded && !hasPatientData" class="analytics-chart-empty">暂无患者分布数据</div>
        </div>
      </article>

      <article class="analytics-chart-card">
        <header>
          <div>
            <h3>设备使用</h3>
            <p>设备使用量与异常数对比</p>
          </div>
          <span class="analytics-chart-badge is-amber">设备</span>
        </header>
        <div class="analytics-chart-body">
          <div ref="deviceEl" class="analytics-chart-canvas" :class="{ 'is-hidden': !hasDeviceData }"></div>
          <div v-if="loaded && !hasDeviceData" class="analytics-chart-empty">暂无设备使用数据</div>
        </div>
      </article>
    </div>
  </section>
</template>
