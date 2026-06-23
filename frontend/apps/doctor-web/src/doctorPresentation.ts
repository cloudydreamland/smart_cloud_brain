import { computed, type Ref } from "vue";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";

export type Tone = "success" | "info" | "warning" | "danger";

export function liveRows(source: Ref<DataRow[]>) {
  return computed(() => source.value);
}

export function statusLabel(status: unknown, fallback = "-") {
  const raw = String(status ?? "").trim();
  if (!raw) return fallback;
  const labels: Record<string, string> = {
    CREATED: "已创建",
    CONFIRMED: "已确认",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    PENDING: "待处理",
    DRAFT: "草稿",
    UNREVIEWED: "待审核",
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险",
    INFO: "提示",
    READ: "已读",
    UNREAD: "未读",
    FAILED: "失败",
    DRAFT_READY: "草稿就绪",
    GENERATING: "生成中",
    IDLE: "空闲",
    DOCTOR: "医生",
    PUBLISHED: "已发布",
  };
  return labels[raw.toUpperCase()] ?? raw;
}

export function statusTone(status: unknown): Tone {
  const value = String(status || "").toUpperCase();
  if (["COMPLETED", "CREATED", "LOW", "READ", "DRAFT_READY", "PUBLISHED"].includes(value)) return "success";
  if (["HIGH", "FAILED", "CANCELLED"].includes(value)) return "danger";
  if (["MEDIUM", "PENDING", "UNREVIEWED", "UNREAD", "GENERATING", "CONFIRMED"].includes(value)) return "warning";
  return "info";
}

export function patientName(item: DataRow | null | undefined) {
  return fieldText(item, "patientName", `Patient ${fieldText(item, "patientId", "-")}`);
}

export function riskText(item: DataRow | null | undefined) {
  return statusLabel(fieldText(item, "riskLevel", "LOW"));
}

export function formatTime(iso: unknown) {
  const raw = String(iso ?? "").trim();
  if (!raw) return "-";
  try {
    const d = new Date(raw);
    if (isNaN(d.getTime())) return raw;
    const m = d.getMonth() + 1;
    const day = d.getDate();
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    return `${m}月${day}日 ${hh}:${mm}`;
  } catch {
    return raw;
  }
}

export function formatAiDraft() {
  return [
    "Chief complaint: ",
    "Present illness: ",
    "Past history: ",
    "Physical exam: ",
    "Diagnosis: ",
    "Treatment advice: ",
  ].join("\n");
}
