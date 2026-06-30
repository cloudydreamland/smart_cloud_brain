import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  displayText,
  toNumber,
  type Notification,
} from "@smart-cloud-brain/shared-api";
import { statusLabel } from "../doctorPresentation";

export type FilterKey = "read" | "type" | "risk" | "sort";

export const handleOptions = [
  { value: "ALL", label: "全部" },
  { value: "PENDING", label: "待处理" },
  { value: "HANDLED", label: "已处理" },
  { value: "IGNORED", label: "已忽略" },
];

export const filterGroups = [
  {
    key: "read" as const,
    label: "阅读状态",
    options: [
      { value: "ALL", label: "全部阅读" },
      { value: "UNREAD", label: "未读" },
      { value: "READ", label: "已读" },
    ],
  },
  {
    key: "type" as const,
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
    key: "risk" as const,
    label: "风险等级",
    options: [
      { value: "ALL", label: "全部风险" },
      { value: "HIGH", label: "高风险" },
      { value: "MEDIUM", label: "中风险" },
      { value: "LOW", label: "低风险" },
    ],
  },
  {
    key: "sort" as const,
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

export function riskValue(item: Notification) {
  return displayText(item.riskLevel, "INFO").toUpperCase();
}

export function readValue(item: Notification) {
  return displayText(item.readStatus, "UNREAD").toUpperCase();
}

export function handleValue(item: Notification) {
  return displayText(item.handleStatus, readValue(item) === "READ" ? "HANDLED" : "PENDING").toUpperCase();
}

export function typeCategory(type: unknown) {
  const value = displayText(type, "SYSTEM_NOTICE").toUpperCase();
  if (value.includes("PRESCRIPTION")) return "PRESCRIPTION";
  if (value.includes("TRIAGE")) return "TRIAGE";
  if (value.includes("MEDICAL_RECORD")) return "MEDICAL_RECORD";
  if (value.includes("REGISTRATION")) return "REGISTRATION";
  return "SYSTEM";
}

export function typeLabel(type: unknown) {
  const category = typeCategory(type);
  if (category === "TRIAGE") return "分诊";
  if (category === "PRESCRIPTION") return "处方";
  if (category === "MEDICAL_RECORD") return "病历";
  if (category === "REGISTRATION") return "挂号";
  return "系统";
}

export function typeTone(type: unknown) {
  const category = typeCategory(type);
  if (category === "PRESCRIPTION") return "warning";
  if (category === "TRIAGE") return "info";
  if (category === "MEDICAL_RECORD") return "success";
  return "active";
}

export function relationText(item: Notification) {
  const parts = [];
  if (toNumber(item.patientId)) parts.push(`患者 #${item.patientId}`);
  if (toNumber(item.prescriptionId)) parts.push(`处方 #${item.prescriptionId}`);
  if (toNumber(item.triageRecordId)) parts.push(`分诊 #${item.triageRecordId}`);
  if (toNumber(item.medicalRecordId)) parts.push(`病历 #${item.medicalRecordId}`);
  return parts.join(" · ") || "未关联业务对象";
}

export function notificationKey(item: Notification) {
  return String(item.notificationId ?? item.createdAt ?? item.title ?? "");
}

export function processable(item: Notification) {
  const category = typeCategory(item.type);
  return ["PRESCRIPTION", "MEDICAL_RECORD", "TRIAGE", "REGISTRATION"].includes(category);
}

export function primaryActionLabel(item: Notification) {
  return processable(item) ? "去处理" : "查看详情";
}

export function useNotificationFilters(allRows: () => Notification[]) {
  const search = ref("");
  const readFilter = ref("ALL");
  const handleFilter = ref("ALL");
  const typeFilter = ref("ALL");
  const riskFilter = ref("ALL");
  const sortBy = ref("DEFAULT");
  const openFilter = ref<FilterKey | null>(null);
  const openActionMenu = ref("");

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

  const filteredRows = computed(() => {
    const keyword = search.value.trim().toLowerCase();
    return allRows()
      .filter((item) => handleFilter.value === "ALL" || handleValue(item) === handleFilter.value)
      .filter((item) => readFilter.value === "ALL" || readValue(item) === readFilter.value)
      .filter((item) => typeFilter.value === "ALL" || typeCategory(item.type) === typeFilter.value)
      .filter((item) => riskFilter.value === "ALL" || riskValue(item) === riskFilter.value)
      .filter((item) => !keyword || searchText(item).includes(keyword))
      .slice()
      .sort(rowComparator(sortBy.value));
  });

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

  onMounted(() => {
    document.addEventListener("click", closeMenus);
    document.addEventListener("keydown", handleDocumentKeydown);
  });

  onBeforeUnmount(() => {
    document.removeEventListener("click", closeMenus);
    document.removeEventListener("keydown", handleDocumentKeydown);
  });

  watch([search, readFilter, handleFilter, typeFilter, riskFilter, sortBy], () => {
    // Reset page handled by parent via watch on filteredRows
  });

  return {
    search,
    readFilter,
    handleFilter,
    typeFilter,
    riskFilter,
    sortBy,
    openFilter,
    openActionMenu,
    filteredRows,
    filterValue,
    setFilterValue,
    filterLabel,
    toggleActionMenu,
    closeMenus,
  };
}
