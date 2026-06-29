import { computed, ref, toValue, watch, type MaybeRefOrGetter } from "vue";
import { ApiError, type DataRow } from "./types";

export function formatApiError(error: unknown, fallback: string) {
  if (error instanceof ApiError) return localizeApiMessage(error.message);
  if (error instanceof Error) return localizeApiMessage(error.message);
  return fallback;
}

function localizeApiMessage(message: string) {
  const text = message.trim();
  if (!text) return "请求失败";
  const lower = text.toLowerCase();
  if (lower.includes("unauthorized")) return "用户名或密码错误";
  if (lower.includes("triage-service unavailable")) return "分诊服务暂不可用，其他管理功能可继续使用。";
  if (lower.includes("doctor-service unavailable")) return "医生与号源服务暂不可用，请稍后重试。";
  if (lower.includes("ai service unavailable")) return "智能服务暂不可用，请稍后重试。";
  return text;
}

export function toNumber(value: unknown, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

export function fieldText(item: DataRow | null | undefined, key: string, fallback = "-") {
  const value = item?.[key];
  return value === undefined || value === null || value === "" ? fallback : String(value);
}

export function displayText(value: unknown, fallback = "-") {
  return value === undefined || value === null || value === "" ? fallback : String(value);
}

export function statusClass(status: unknown) {
  const value = String(status || "").toUpperCase();
  if (["CREATED", "CONFIRMED", "COMPLETED", "AVAILABLE", "ENABLED", "PUBLISHED", "AI_RECOMMENDED", "LOW", "READ"].includes(value)) return "success";
  if (["CANCELLED", "FAILED", "DISABLED", "HIGH", "CLOSED", "FULL", "RETIRED"].includes(value)) return "danger";
  if (["PENDING", "DRAFT", "UNPUBLISHED", "UNREVIEWED", "MEDIUM", "MANUAL_REQUIRED", "UNREAD", "MAINTENANCE"].includes(value)) return "warning";
  if (["IN_USE", "ASSIGNED"].includes(value)) return "info";
  return "info";
}

export function statusText(status: unknown, fallback = "-") {
  const raw = String(status ?? "").trim();
  if (!raw) return fallback;
  const labels: Record<string, string> = {
    CREATED: "已创建", CONFIRMED: "已确认", COMPLETED: "已完成", AVAILABLE: "可预约",
    ENABLED: "启用", PUBLISHED: "已发布", AI_RECOMMENDED: "智能推荐", LOW: "低风险",
    READ: "已读", CANCELLED: "已取消", FAILED: "失败", DISABLED: "停用", HIGH: "高风险",
    CLOSED: "已关闭", FULL: "已约满", PENDING: "待处理", HANDLED: "已处理", IGNORED: "已忽略", DRAFT: "草稿",
    UNPUBLISHED: "未发布", UNREVIEWED: "未审核", MEDIUM: "中风险",
    ASSIGNED: "已分配", SUCCESS: "成功",
    MANUAL_REQUIRED: "待人工处理", UNREAD: "未读", PATIENT: "患者", DOCTOR: "医生",
    ADMIN: "管理员", MALE: "男", FEMALE: "女", UNKNOWN: "未说明",
    /* 医生端 workflow 状态 */
    INFO: "提示", DRAFT_READY: "草稿就绪", GENERATING: "生成中",
    IDLE: "空闲", OPEN: "进行中", ACTIVE: "生效",
    IN_USE: "使用中", MAINTENANCE: "维修中", RETIRED: "已停用",
  };
  return labels[raw.toUpperCase()] ?? raw;
}

export function aiSourceLabel(provider: unknown) {
  const value = String(provider ?? "").trim().toLowerCase();
  if (!value) return "AI";
  return value === "mock" ? "本地模拟" : "AI";
}

const aiTaskLabels: Record<string, string> = {
  SCHEDULE: "排班",
  TRIAGE: "分诊",
  MEDICAL_RECORD: "病历",
  PRESCRIPTION_CHECK: "处方审核",
};

export function aiTaskLabel(taskType: unknown) {
  const raw = String(taskType ?? "").trim().toUpperCase();
  return aiTaskLabels[raw] ?? raw;
}

export function aiSourceTone(provider: unknown) {
  return String(provider ?? "").trim().toLowerCase() === "mock" ? "warning" : "success";
}

export function formatDateTime(value: unknown, fallback = "-") {
  if (!value) return fallback;
  try {
    const d = new Date(String(value));
    if (isNaN(d.getTime())) return fallback;
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return fallback;
  }
}

export function usePagination<T>(source: MaybeRefOrGetter<T[]>, initialPageSize = 8) {
  const currentPage = ref(1);
  const pageSize = ref(initialPageSize);
  const total = computed(() => toValue(source).length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / Math.max(1, pageSize.value))));
  const pageRows = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return toValue(source).slice(start, start + pageSize.value);
  });

  watch([total, pageCount], () => {
    currentPage.value = Math.min(Math.max(1, currentPage.value), pageCount.value);
  });

  return { currentPage, pageSize, total, pageCount, pageRows };
}
