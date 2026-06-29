import {
  computed,
  markRaw,
  ref,
  type ComputedRef,
  type Ref,
} from "vue";
import {
  IconChartLine,
  IconCircleCheck,
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

const DAY_MS = 86_400_000;

export interface AdminAnalyticsState {
  loading: Ref<boolean>;
  exporting: Ref<boolean>;
  loaded: Ref<boolean>;
  error: Ref<string>;
  startDate: Ref<string>;
  endDate: Ref<string>;
  overview: Ref<StatisticsOverview>;
  trend: Ref<StatisticsTrendRow[]>;
  workload: Ref<DoctorWorkloadRow[]>;
  distribution: Ref<PatientDistribution>;
  deviceUsage: Ref<DeviceUsageStatsRow[]>;
  metricCards: ComputedRef<
    {
      label: string;
      value: string;
      icon: typeof IconChartLine;
      tone: string;
      sub?: string;
    }[]
  >;
  refresh: () => Promise<void>;
  exportCsv: () => Promise<void>;
}

export function useAdminAnalytics(): AdminAnalyticsState {
  const loading = ref(false);
  const exporting = ref(false);
  const loaded = ref(false);
  const error = ref("");
  const startDate = ref(
    new Date(Date.now() - 14 * DAY_MS).toISOString().slice(0, 10),
  );
  const endDate = ref(new Date().toISOString().slice(0, 10));
  const overview = ref<StatisticsOverview>({});
  const trend = ref<StatisticsTrendRow[]>([]);
  const workload = ref<DoctorWorkloadRow[]>([]);
  const distribution = ref<PatientDistribution>({});
  const deviceUsage = ref<DeviceUsageStatsRow[]>([]);

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
      sub: overview.value.deviceWarnings
        ? `${overview.value.deviceWarnings} 预警`
        : "",
    },
  ]);

  async function refresh() {
    loading.value = true;
    error.value = "";
    try {
      const params = { startDate: startDate.value, endDate: endDate.value };
      const [
        nextOverview,
        nextTrend,
        nextWorkload,
        nextDistribution,
        nextDeviceUsage,
      ] = await Promise.all([
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
      const rows = await api.statisticsReport({
        startDate: startDate.value,
        endDate: endDate.value,
      });
      const csv = [
        "metric,value",
        ...(rows as StatisticsReportRow[]).map(
          (row) =>
            `${displayText(row.metric, "")},${displayText(row.value, "0")}`,
        ),
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

  return {
    loading,
    exporting,
    loaded,
    error,
    startDate,
    endDate,
    overview,
    trend,
    workload,
    distribution,
    deviceUsage,
    metricCards,
    refresh,
    exportCsv,
  };
}
