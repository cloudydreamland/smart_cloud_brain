<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from "vue";
import echarts from "../echarts";
import { api, fieldText, formatApiError, useAuthStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { ErrorState, LoadingState } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const loading = ref(false);
const error = ref("");
const startDate = ref(new Date(Date.now() - 14 * 86400000).toISOString().slice(0, 10));
const endDate = ref(new Date().toISOString().slice(0, 10));
const overview = ref<DataRow>({});
const trend = ref<DataRow[]>([]);
const workload = ref<DataRow[]>([]);
const distribution = ref<DataRow>({});
const deviceUsage = ref<DataRow[]>([]);
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
      api.statisticsOverview(auth.token()),
      api.statisticsTrend(auth.token(), params),
      api.doctorWorkload(auth.token(), params),
      api.patientDistribution(auth.token()),
      api.deviceUsageStatistics(auth.token()),
    ]);
    overview.value = nextOverview;
    trend.value = nextTrend;
    workload.value = nextWorkload;
    distribution.value = nextDistribution;
    deviceUsage.value = nextDeviceUsage;
    await nextTick();
    renderCharts();
  } catch (err) {
    error.value = formatApiError(err, "Statistics failed");
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  charts.forEach((chart) => chart.dispose());
  charts = [];
  if (trendEl.value) {
    const chart = echarts.init(trendEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: trend.value.map((item) => fieldText(item, "day")) },
      yAxis: { type: "value" },
      series: [{ type: "line", smooth: true, data: trend.value.map((item) => Number(item.registrations || 0)) }],
    });
    charts.push(chart);
  }
  if (workloadEl.value) {
    const chart = echarts.init(workloadEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: workload.value.slice(0, 10).map((item) => fieldText(item, "doctor_name")) },
      yAxis: { type: "value" },
      series: [{ type: "bar", data: workload.value.slice(0, 10).map((item) => Number(item.registrations || 0)) }],
    });
    charts.push(chart);
  }
  if (patientEl.value) {
    const chart = echarts.init(patientEl.value);
    const gender = (distribution.value.gender as DataRow[] | undefined) ?? [];
    chart.setOption({ tooltip: {}, series: [{ type: "pie", radius: "65%", data: gender }] });
    charts.push(chart);
  }
  if (deviceEl.value) {
    const chart = echarts.init(deviceEl.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: "category", data: deviceUsage.value.slice(0, 10).map((item) => fieldText(item, "name")) },
      yAxis: { type: "value" },
      series: [
        { name: "Usage", type: "bar", data: deviceUsage.value.slice(0, 10).map((item) => Number(item.usage_count || 0)) },
        { name: "Abnormal", type: "bar", data: deviceUsage.value.slice(0, 10).map((item) => Number(item.abnormal_count || 0)) },
      ],
    });
    charts.push(chart);
  }
}

async function exportCsv() {
  const rows = await api.statisticsReport(auth.token(), { startDate: startDate.value, endDate: endDate.value });
  const csv = ["metric,value", ...rows.map((row) => `${fieldText(row, "metric", "")},${fieldText(row, "value", "0")}`)].join("\n");
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
      <div class="panel-title"><p class="eyebrow">Analytics</p><h2>Diagnosis Data Statistics</h2><p>Statistics come from registration, patient, doctor and device tables.</p></div>
      <div class="toolbar"><input v-model="startDate" type="date" /><input v-model="endDate" type="date" /><button type="button" :disabled="loading" @click="refresh">Refresh</button><button class="primary" type="button" @click="exportCsv">CSV</button></div>
    </header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <LoadingState v-if="loading" />
      <div class="metrics">
        <div class="metric"><span>Registrations</span><strong>{{ fieldText(overview, "registrations", "0") }}</strong></div>
        <div class="metric"><span>Completed</span><strong>{{ fieldText(overview, "completedRegistrations", "0") }}</strong></div>
        <div class="metric"><span>Patients</span><strong>{{ fieldText(overview, "patients", "0") }}</strong></div>
        <div class="metric"><span>Doctors</span><strong>{{ fieldText(overview, "doctors", "0") }}</strong></div>
        <div class="metric"><span>Devices</span><strong>{{ fieldText(overview, "devices", "0") }}</strong></div>
        <div class="metric"><span>Device warnings</span><strong>{{ fieldText(overview, "deviceWarnings", "0") }}</strong></div>
      </div>
      <div class="main-grid admin-grid">
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>Visit Trend</h3></div></header><div ref="trendEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>Doctor Workload</h3></div></header><div ref="workloadEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>Patient Distribution</h3></div></header><div ref="patientEl" class="chart-box"></div></section>
        <section class="panel"><header class="panel-header"><div class="panel-title"><h3>Device Usage</h3></div></header><div ref="deviceEl" class="chart-box"></div></section>
      </div>
    </div>
  </section>
</template>
