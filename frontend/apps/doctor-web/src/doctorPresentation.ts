import { computed, type Ref } from "vue";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";

export type Tone = "success" | "info" | "warning" | "danger";

export const demoRegistrations: DataRow[] = [
  {
    registrationId: 10023,
    patientId: "P2026062201",
    patientName: "王晓敏",
    departmentName: "呼吸内科",
    departmentCode: "RESP",
    appointmentTime: "09:20",
    status: "CREATED",
    riskLevel: "MEDIUM",
    triageRecordId: 3001,
    chiefComplaint: "咳嗽、咽痛、发热 3 天，夜间加重",
  },
  {
    registrationId: 10024,
    patientId: "P2026062202",
    patientName: "李建国",
    departmentName: "呼吸内科",
    departmentCode: "RESP",
    appointmentTime: "09:40",
    status: "CONFIRMED",
    riskLevel: "HIGH",
    triageRecordId: 3002,
    chiefComplaint: "慢阻肺复诊，活动后气促",
  },
  {
    registrationId: 10025,
    patientId: "P2026062203",
    patientName: "赵静",
    departmentName: "呼吸内科",
    departmentCode: "RESP",
    appointmentTime: "10:00",
    status: "CREATED",
    riskLevel: "LOW",
    triageRecordId: 3003,
    chiefComplaint: "胸闷伴低热，需排查感染",
  },
];

export const demoTriageRecords: DataRow[] = [
  {
    triageRecordId: 3001,
    patientName: "王晓敏",
    status: "MEDIUM",
    chiefComplaint: "咳嗽、咽痛、发热 3 天，夜间加重，伴少量黄痰。",
    pastHistory: "青霉素过敏；无糖尿病、高血压病史。",
    reason: "存在发热和黄痰，建议呼吸内科优先接诊并完成感染指标检查。",
  },
  {
    triageRecordId: 3002,
    patientName: "李建国",
    status: "HIGH",
    chiefComplaint: "慢阻肺复诊，活动后气促。",
    pastHistory: "慢阻肺 8 年，长期吸入治疗。",
    reason: "基础肺病合并气促，需关注血氧和感染风险。",
  },
  {
    triageRecordId: 3003,
    patientName: "赵静",
    status: "LOW",
    chiefComplaint: "胸闷伴低热 1 天。",
    pastHistory: "否认明确药物过敏史。",
    reason: "症状较轻，建议常规门诊评估。",
  },
];

export const demoRecords: DataRow[] = [
  {
    medicalRecordId: 90031,
    patientName: "王晓敏",
    patientId: "P2026062201",
    chiefComplaint: "咳嗽、咽痛、发热 3 天",
    diagnosis: "急性上呼吸道感染",
    aiGenerated: true,
    createdAt: "2026-06-22 09:34",
  },
  {
    medicalRecordId: 90030,
    patientName: "李建国",
    patientId: "P2026062202",
    chiefComplaint: "慢阻肺复诊，活动后气促",
    diagnosis: "慢阻肺稳定期",
    aiGenerated: false,
    createdAt: "2026-06-22 09:08",
  },
];

export const demoPrescriptions: DataRow[] = [
  {
    prescriptionId: 70102,
    patientName: "王晓敏",
    patientId: "P2026062201",
    createdAt: "09:36",
    status: "PENDING",
    riskLevel: "HIGH",
    drugCount: 2,
    suggestions: "患者有青霉素过敏史，当前处方包含阿莫西林胶囊。建议替换药物或由医生二次确认。",
  },
  {
    prescriptionId: 70101,
    patientName: "李建国",
    patientId: "P2026062202",
    createdAt: "09:12",
    status: "CREATED",
    riskLevel: "LOW",
    drugCount: 3,
  },
];

export const demoNotifications: DataRow[] = [
  {
    notificationId: 81001,
    title: "高风险处方待确认",
    content: "王晓敏存在青霉素过敏史，阿莫西林需替换或确认。",
    riskLevel: "HIGH",
    readStatus: "UNREAD",
  },
  {
    notificationId: 81002,
    title: "病历草稿已保存",
    content: "#MR90031 已由陈明医生确认保存。",
    riskLevel: "LOW",
    readStatus: "READ",
  },
  {
    notificationId: 81003,
    title: "队列同步完成",
    content: "新增 2 名已确认预约患者。",
    riskLevel: "INFO",
    readStatus: "UNREAD",
  },
];

export const demoDrugs: DataRow[] = [
  { id: 1, name: "阿莫西林胶囊" },
  { id: 2, name: "氨溴索片" },
  { id: 3, name: "布洛芬缓释胶囊" },
  { id: 4, name: "氯雷他定片" },
];

export function withDemo(source: Ref<DataRow[]>, demo: DataRow[]) {
  return computed(() => (source.value.length ? source.value : demo));
}

export function statusLabel(status: unknown, fallback = "-") {
  const raw = String(status ?? "").trim();
  if (!raw) return fallback;
  const labels: Record<string, string> = {
    CREATED: "待接诊",
    CONFIRMED: "已确认",
    COMPLETED: "已完成",
    PENDING: "待确认",
    DRAFT: "草稿",
    UNREVIEWED: "未审核",
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险",
    INFO: "系统",
    READ: "已读",
    UNREAD: "未读",
    FAILED: "失败",
    DRAFT_READY: "草稿已生成",
    GENERATING: "生成中",
    IDLE: "草稿待生成",
    DOCTOR: "医生",
  };
  return labels[raw.toUpperCase()] ?? raw;
}

export function statusTone(status: unknown): Tone {
  const value = String(status || "").toUpperCase();
  if (["COMPLETED", "CREATED", "LOW", "READ", "DRAFT_READY"].includes(value)) return "success";
  if (["HIGH", "FAILED", "CANCELLED"].includes(value)) return "danger";
  if (["MEDIUM", "PENDING", "UNREVIEWED", "UNREAD", "GENERATING", "CONFIRMED"].includes(value)) return "warning";
  return "info";
}

export function patientName(item: DataRow | null | undefined) {
  return fieldText(item, "patientName", `患者 ${fieldText(item, "patientId", "-")}`);
}

export function riskText(item: DataRow | null | undefined) {
  const risk = fieldText(item, "riskLevel", "LOW");
  return statusLabel(risk);
}

export function formatAiDraft() {
  return [
    "主诉：咳嗽、咽痛、发热 3 天。",
    "现病史：3 天前无明显诱因出现咳嗽、咽痛、发热，最高体温 38.4°C，夜间咳嗽加重，伴少量黄痰。",
    "既往史：青霉素过敏。",
    "体格检查：咽部充血，双肺呼吸音粗，未闻及明显湿啰音。",
    "初步诊断：急性上呼吸道感染。",
    "处理建议：完善血常规及 CRP；对症退热、止咳化痰；避免使用青霉素类药物。",
  ].join("\n");
}
