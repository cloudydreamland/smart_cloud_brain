<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from "vue";
import echarts from "../echarts";
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
import { ErrorState, LoadingState } from "@smart-cloud-brain/shared-ui";

const loading = ref(false);
const error = ref("");
const startDate = ref(new Date(Date.now() - 14 * 86400000).toISOString().slice(0, 10));
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
let charts: import("../echarts").ECharts[] = [];

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
    await nextTick();
    renderCharts();
  } catch (err) {
    error.value = formatApiError(err, "Statistics failed");
  } finally {
    loading.value = false;
  }
}

/** 将 ISO 日期字符串格式化为 MM/DD 格式 */
function formatDay(iso: unknown): string {
  const s = displayText(iso);
  if (!s) return "-";
  const d = new Date(s);
  if (isNaN(d.getTime())) return s;
  return `${String(d.getMonth() + 1).padStart(2, "0")}/${String(d.getDate()).padStart(2, "0")}`;
}

function renderCharts() {
  charts.forEach((chart) => chart.dispose());
  charts = [];
  if (trendEl.value) {
    const chart = echarts.init(trendEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: trend.value.map((item) => formatDay(item.day)), axisLabel: { rotate: 30 } },
      yAxis: { type: "value" },
      series: [{ type: "line", smooth: true, data: trend.value.map((item) => Number(item.registrations || 0)) }],
    });
    charts.push(chart);
  }
  if (workloadEl.value) {
    const chart = echarts.init(workloadEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: workload.value.slice(0, 10).map((item) => displayText(item.doctor_name)) },
      yAxis: { type: "value" },
      series: [{ type: "bar", data: workload.value.slice(0, 10).map((item) => Number(item.registrations || 0)) }],
    });
    charts.push(chart);
  }
  if (patientEl.value) {
    const chart = echarts.init(patientEl.value);
    const gender = distribution.value.gender ?? [];
    chart.setOption({ tooltip: {}, series: [{ type: "pie", radius: "65%", data: gender }] });
    charts.push(chart);
  }
  if (deviceEl.value) {
    const chart = echarts.init(deviceEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: deviceUsage.value.slice(0, 10).map((item) => displayText(item.name)) },
      yAxis: { type: "value" },
      series: [
        { name: "使用量", type: "bar", data: deviceUsage.value.slice(0, 10).map((item) => Number(item.usage_count || 0)) },
        { name: "异常数", type: "bar", data: deviceUsage.value.slice(0, 10).map((item) => Number(item.abnormal_count || 0)) },
      ],
    });
    charts.push(chart);
  }
}

async function exportCsv() {
  const rows = await api.statisticsReport({ startDate: startDate.value, endDate: endDate.value });
  const csv = ["metric,value", ...(rows as StatisticsReportRow[]).map((row) => `${displayText(row.metric, "")},${displayText(row.value, "0")}`)].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `statistics-${startDate.value}-${endDate.value}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}

onBeforeUnmount(() => charts.forEach((chart) => chart.dispose()));
refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title"><h2>数据统计</h2></div>
      <div class="toolbar"><input v-model="startDate" type="date" /><input v-model="endDate" type="date" /><button type="button" :disabled="loading" @click="refresh">刷新</button><button class="primary" type="button" @click="exportCsv">导出 CSV</button></div>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" />
      <div class="metrics">
        <div class="metric"><span>就诊总数</span><strong>{{ displayText(overview.registrations, "0") }}</strong></div>
        <div class="metric"><span>已完成</span><strong>{{ displayText(overview.completedRegistrations, "0") }}</strong></div>
        <div class="metric"><span>患者数</span><strong>{{ displayText(overview.patients, "0") }}</strong></div>
        <div class="metric"><span>医生数</span><strong>{{ displayText(overview.doctors, "0") }}</strong></div>
        <div class="metric"><span>设备数</span><strong>{{ displayText(overview.devices, "0") }}</strong></div>
        <div class="metric"><span>设备预警</span><strong>{{ displayText(overview.deviceWarnings, "0") }}</strong></div>
      </div>
      <div class="main-grid admin-grid">
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>就诊趋势</h3></div></header><div ref="trendEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>医生工作量</h3></div></header><div ref="workloadEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>患者分布</h3></div></header><div ref="patientEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>设备使用</h3></div></header><div ref="deviceEl" class="chart-box"></div></section>
      </div>
    </div>
  </section>
</template>
