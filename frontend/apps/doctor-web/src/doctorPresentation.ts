import { computed, type Ref } from "vue";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";

export type Tone = "success" | "info" | "warning" | "danger";

export const demoRegistrations: DataRow[] = [];
export const demoTriageRecords: DataRow[] = [];
export const demoRecords: DataRow[] = [];
export const demoPrescriptions: DataRow[] = [];
export const demoNotifications: DataRow[] = [];
export const demoDrugs: DataRow[] = [];

export function withDemo(source: Ref<DataRow[]>, _demo: DataRow[]) {
  return computed(() => source.value);
}

export function statusLabel(status: unknown, fallback = "-") {
  const raw = String(status ?? "").trim();
  if (!raw) return fallback;
  const labels: Record<string, string> = {
    CREATED: "Created",
    CONFIRMED: "Confirmed",
    COMPLETED: "Completed",
    CANCELLED: "Cancelled",
    PENDING: "Pending",
    DRAFT: "Draft",
    UNREVIEWED: "Unreviewed",
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
    INFO: "Info",
    READ: "Read",
    UNREAD: "Unread",
    FAILED: "Failed",
    DRAFT_READY: "Draft ready",
    GENERATING: "Generating",
    IDLE: "Idle",
    DOCTOR: "Doctor",
    PUBLISHED: "Published",
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
