import { computed, ref } from "vue";
import { displayText, type Prescription } from "@smart-cloud-brain/shared-api";

export type PrescriptionFilterKey = "status" | "risk" | "sort";

export const statusSegmentOptions = [
  { value: "ALL", label: "全部" },
  { value: "CONFIRMED", label: "生效" },
  { value: "PENDING", label: "待审核" },
  { value: "REJECTED", label: "已驳回" },
];

export const filterGroups = [
  {
    key: "status" as const,
    label: "处方状态",
    options: [
      { value: "ALL", label: "全部状态" },
      { value: "CONFIRMED", label: "生效" },
      { value: "PENDING", label: "待审核" },
      { value: "REJECTED", label: "已驳回" },
    ],
  },
  {
    key: "risk" as const,
    label: "风险等级",
    options: [
      { value: "ALL", label: "全部风险" },
      { value: "LOW", label: "低风险" },
      { value: "MEDIUM", label: "中风险" },
      { value: "HIGH", label: "高风险" },
    ],
  },
  {
    key: "sort" as const,
    label: "排序方式",
    options: [
      { value: "CREATED_DESC", label: "最新优先" },
      { value: "CREATED_ASC", label: "最早优先" },
      { value: "RISK_DESC", label: "风险优先" },
    ],
  },
] as const;

function statusValue(item: Prescription) {
  return displayText(item.status, "").toUpperCase();
}

function riskValue(item: Prescription) {
  return displayText(item.riskLevel, "UNREVIEWED").toUpperCase();
}

function createdTime(item: Prescription) {
  const time = new Date(displayText(item.createdAt, "")).getTime();
  return Number.isFinite(time) ? time : 0;
}

function riskPriority(item: Prescription) {
  const v = riskValue(item);
  if (v === "HIGH") return 0;
  if (v === "MEDIUM") return 1;
  if (v === "LOW") return 2;
  return 3;
}

export function usePrescriptionFilters(allRows: () => Prescription[]) {
  const search = ref("");
  const statusFilter = ref("ALL");
  const riskFilter = ref("ALL");
  const sortBy = ref("CREATED_DESC");
  const openFilter = ref<PrescriptionFilterKey | null>(null);

  function searchText(item: Prescription) {
    return [
      displayText(item.patientName, ""),
      displayText(item.prescriptionId, ""),
      String(item.patientId ?? ""),
      statusValue(item),
      riskValue(item),
    ].join(" ").toLowerCase();
  }

  const filteredRows = computed(() => {
    const keyword = search.value.trim().toLowerCase();
    return allRows()
      .filter((item) => statusFilter.value === "ALL" || statusValue(item) === statusFilter.value)
      .filter((item) => riskFilter.value === "ALL" || riskValue(item) === riskFilter.value)
      .filter((item) => !keyword || searchText(item).includes(keyword))
      .slice()
      .sort((a, b) => {
        if (sortBy.value === "CREATED_ASC") return createdTime(a) - createdTime(b);
        if (sortBy.value === "RISK_DESC") return riskPriority(a) - riskPriority(b) || createdTime(b) - createdTime(a);
        return createdTime(b) - createdTime(a);
      });
  });

  function filterValue(key: PrescriptionFilterKey) {
    if (key === "status") return statusFilter.value;
    if (key === "risk") return riskFilter.value;
    return sortBy.value;
  }

  function setFilterValue(key: PrescriptionFilterKey, value: string) {
    if (key === "status") statusFilter.value = value;
    if (key === "risk") riskFilter.value = value;
    if (key === "sort") sortBy.value = value;
    openFilter.value = null;
  }

  function filterLabel(key: PrescriptionFilterKey) {
    const group = filterGroups.find((item) => item.key === key);
    return group?.options.find((item) => item.value === filterValue(key))?.label ?? group?.label ?? "";
  }

  function closeMenu() {
    openFilter.value = null;
  }

  return {
    search,
    statusFilter,
    riskFilter,
    sortBy,
    openFilter,
    filteredRows,
    filterValue,
    setFilterValue,
    filterLabel,
    closeMenu,
  };
}
