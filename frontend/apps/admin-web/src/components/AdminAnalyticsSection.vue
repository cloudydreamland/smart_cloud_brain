<script setup lang="ts">
import {
  computed,
  inject,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import {
  displayText,
  type DistributionItem,
  type DoctorWorkloadRow,
  type StatisticsTrendRow,
  type DeviceUsageStatsRow,
} from "@smart-cloud-brain/shared-api";
import type { AdminAnalyticsState } from "../composables/useAdminAnalytics";
import echarts, { type ECharts } from "../echarts";

const {
  loaded,
  trend,
  workload,
  distribution,
  deviceUsage,
} = inject<AdminAnalyticsState>("adminAnalytics")!;

const trendEl = ref<HTMLDivElement | null>(null);
const workloadEl = ref<HTMLDivElement | null>(null);
const patientEl = ref<HTMLDivElement | null>(null);
const deviceEl = ref<HTMLDivElement | null>(null);
let charts: ECharts[] = [];
let resizeObserver: ResizeObserver | null = null;

const genderData = computed(() =>
  (distribution.value?.gender ?? [])
    .map((item: DistributionItem) => ({
      name: displayText(item.name, "未知"),
      value: Number(item.value || 0),
    }))
    .filter((item: { name: string; value: number }) => item.value > 0),
);
const patientTotal = computed(() =>
  genderData.value.reduce(
    (sum: number, item: { name: string; value: number }) => sum + item.value,
    0,
  ),
);
const hasTrendData = computed(() => (trend.value?.length ?? 0) > 0);
const hasWorkloadData = computed(() => (workload.value?.length ?? 0) > 0);
const hasPatientData = computed(() => genderData.value.length > 0);
const hasDeviceData = computed(() => (deviceUsage.value?.length ?? 0) > 0);

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
  const splitLine = {
    lineStyle: { color: "#edf1ef", type: "dashed" as const },
  };
  const tooltip = {
    trigger: "axis" as const,
    backgroundColor: "rgba(23, 36, 34, 0.94)",
    borderWidth: 0,
    padding: [10, 12],
    textStyle: { color: "#ffffff", fontSize: 12 },
    axisPointer: {
      type: "line" as const,
      lineStyle: { color: "#9fb1ac", type: "dashed" as const },
    },
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
        data: (trend.value ?? []).map((item: StatisticsTrendRow) => formatDay(item.day)),
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
      series: [
        {
          name: "就诊量",
          type: "line",
          smooth: 0.35,
          symbol: "circle",
          symbolSize: 7,
          showSymbol: (trend.value?.length ?? 0) <= 14,
          lineStyle: { width: 3 },
          itemStyle: { borderColor: "#ffffff", borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: "rgba(37, 99, 235, 0.24)" },
              { offset: 1, color: "rgba(37, 99, 235, 0.02)" },
            ]),
          },
          data: (trend.value ?? []).map((item: StatisticsTrendRow) => Number(item.registrations || 0)),
        },
      ],
    });
    charts.push(chart);
  }

  if (workloadEl.value && hasWorkloadData.value) {
    const rows = (workload.value ?? []).slice(0, 10);
    const chart = echarts.init(workloadEl.value);
    chart.setOption({
      color: ["#007a7a"],
      grid: { left: 88, right: 44, top: 24, bottom: 34 },
      tooltip: {
        ...tooltip,
        axisPointer: { type: "shadow" as const },
      },
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
        data: rows.map((item: DoctorWorkloadRow) =>
          displayText(item.doctor_name, "未命名医生"),
        ),
        axisLabel: { ...axisText, width: 72, overflow: "truncate" },
        axisLine,
        axisTick: { show: false },
      },
      series: [
        {
          name: "就诊量",
          type: "bar",
          barMaxWidth: 18,
          itemStyle: { borderRadius: [0, 7, 7, 0] },
          label: {
            show: true,
            position: "right",
            color: "#60716d",
            fontSize: 11,
          },
          data: rows.map((item: DoctorWorkloadRow) => Number(item.registrations || 0)),
        },
      ],
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
      series: [
        {
          name: "患者分布",
          type: "pie",
          radius: ["48%", "70%"],
          center: ["38%", "52%"],
          avoidLabelOverlap: true,
          itemStyle: {
            borderColor: "#ffffff",
            borderWidth: 4,
            borderRadius: 8,
          },
          label: { show: false },
          emphasis: { scaleSize: 5 },
          data: genderData.value,
        },
      ],
    });
    charts.push(chart);
  }

  if (deviceEl.value && hasDeviceData.value) {
    const rows = (deviceUsage.value ?? []).slice(0, 10);
    const chart = echarts.init(deviceEl.value);
    chart.setOption({
      color: ["#0b8f87", "#dc6b36"],
      grid: { left: 48, right: 22, top: 50, bottom: 54 },
      tooltip: {
        ...tooltip,
        axisPointer: { type: "shadow" as const },
      },
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
        data: rows.map((item: DeviceUsageStatsRow) =>
          displayText(item.name, "未命名设备"),
        ),
        axisLabel: {
          ...axisText,
          interval: 0,
          rotate: rows.length > 5 ? 22 : 0,
          width: 68,
          overflow: "truncate",
        },
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
          data: rows.map((item: DeviceUsageStatsRow) => Number(item.usage_count || 0)),
        },
        {
          name: "异常数",
          type: "bar",
          barMaxWidth: 22,
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          data: rows.map((item: DeviceUsageStatsRow) => Number(item.abnormal_count || 0)),
        },
      ],
    });
    charts.push(chart);
  }
}

function setupResizeObserver() {
  resizeObserver?.disconnect();
  resizeObserver = new ResizeObserver(() =>
    charts.forEach((chart) => chart.resize()),
  );
  [trendEl.value, workloadEl.value, patientEl.value, deviceEl.value]
    .filter((el): el is HTMLDivElement => Boolean(el))
    .forEach((el) => resizeObserver?.observe(el));
}

onMounted(() => {
  setupResizeObserver();
});

watch(loaded, (val) => {
  if (val) {
    nextTick(() => {
      setupResizeObserver();
      renderCharts();
    });
  }
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  disposeCharts();
});
</script>

<template>
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
        <div
          ref="trendEl"
          class="analytics-chart-canvas"
          :class="{ 'is-hidden': !hasTrendData }"
        ></div>
        <div
          v-if="loaded && !hasTrendData"
          class="analytics-chart-empty"
        >
          暂无就诊趋势数据
        </div>
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
        <div
          ref="workloadEl"
          class="analytics-chart-canvas"
          :class="{ 'is-hidden': !hasWorkloadData }"
        ></div>
        <div
          v-if="loaded && !hasWorkloadData"
          class="analytics-chart-empty"
        >
          暂无医生工作量数据
        </div>
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
        <div
          ref="patientEl"
          class="analytics-chart-canvas"
          :class="{ 'is-hidden': !hasPatientData }"
        ></div>
        <div
          v-if="loaded && !hasPatientData"
          class="analytics-chart-empty"
        >
          暂无患者分布数据
        </div>
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
        <div
          ref="deviceEl"
          class="analytics-chart-canvas"
          :class="{ 'is-hidden': !hasDeviceData }"
        ></div>
        <div
          v-if="loaded && !hasDeviceData"
          class="analytics-chart-empty"
        >
          暂无设备使用数据
        </div>
      </div>
    </article>
  </div>
</template>
