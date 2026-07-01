import { computed, type Ref } from "vue";
import { displayText, statusText, type MedicalRecord, type Registration } from "@smart-cloud-brain/shared-api";

export type Tone = "success" | "info" | "warning" | "danger" | "low" | "pending" | "active";

export function liveRows<T>(source: Ref<T[]>) {
  return computed(() => source.value);
}

/** 复用 shared-api 的 statusText，保留 statusLabel 别名以兼容现有 import */
export const statusLabel = statusText;

export function statusTone(status: unknown): Tone {
  const value = String(status || "").toUpperCase();
  if (["LOW", "READ", "DRAFT_READY"].includes(value)) return "low";
  if (["COMPLETED", "PUBLISHED", "HANDLED"].includes(value)) return "success";
  if (["HIGH", "FAILED", "CANCELLED"].includes(value)) return "danger";
  if (["MEDIUM", "PENDING", "UNREVIEWED", "UNREAD", "GENERATING"].includes(value)) return "warning";
  if (["CREATED"].includes(value)) return "pending";
  if (["IGNORED"].includes(value)) return "info";
  if (["CONFIRMED"].includes(value)) return "active";
  return "info";
}

export function patientName(item: (Registration | MedicalRecord) | null | undefined) {
  return displayText(item?.patientName, `Patient ${displayText(item?.patientId)}`);
}

export function riskText(item: { riskLevel?: string } | null | undefined) {
  return statusLabel(displayText(item?.riskLevel, "LOW"));
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
    "主诉：",
    "现病史：",
    "既往史：",
    "体格检查：",
    "诊断：",
    "治疗建议：",
  ].join("\n");
}
