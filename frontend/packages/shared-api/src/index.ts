import { defineStore } from "pinia";
import { computed, ref } from "vue";

export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type Session = {
  token: string;
  userId: number;
  role: Role;
  name: string;
};

export type DataRow = Record<string, unknown>;

export type PatientRegisterRequest = {
  name: string;
  phone: string;
  password: string;
  gender?: string;
  age?: number;
  allergyHistory?: string;
  pastHistory?: string;
};

export type TriageRequest = {
  patientId?: number;
  chiefComplaint: string;
};

export type RegistrationCreateRequest = {
  doctorId: number;
  departmentId: number;
  appointmentTime: string;
  triageRecordId?: number | null;
  slotId?: number | null;
};

export type MedicalRecordGenerateRequest = {
  registrationId: number;
  departmentCode?: string;
  dialogueText: string;
};

export type MedicalRecordSaveRequest = {
  registrationId: number;
  chiefComplaint: string;
  presentIllness?: string;
  pastHistory?: string;
  physicalExam?: string;
  diagnosis: string;
  treatmentAdvice?: string;
  aiGenerated?: boolean;
};

export type DrugItem = {
  drugName: string;
  dosage: string;
  frequency: string;
  usageMethod: string;
  days?: number;
  remark?: string;
};

export type PrescriptionCheckRequest = {
  patientId?: number;
  doctorId?: number;
  medicalRecordId?: number;
  diagnosis?: string;
  patientAge?: number;
  patientGender?: string;
  allergyHistory?: string;
  pastHistory?: string;
  drugs: DrugItem[];
};

export type PrescriptionCreateRequest = {
  patientId: number;
  medicalRecordId: number;
  registrationId?: number;
  riskLevel?: string;
  drugs: DrugItem[];
};

export type DepartmentSaveRequest = {
  id?: number;
  code: string;
  name: string;
  description?: string;
};

export type DoctorSaveRequest = {
  id?: number;
  name: string;
  phone: string;
  password?: string;
  departmentId: number;
  title?: string;
  specialty?: string;
  status?: string;
};

export type DrugSaveRequest = {
  id?: number;
  name: string;
  specification?: string;
  contraindication?: string;
  interactionRule?: string;
  status?: string;
};

export type KnowledgeEntrySaveRequest = {
  id?: number;
  title: string;
  symptoms: string;
  riskSignals?: string;
  advice: string;
  departmentCode?: string;
  status?: string;
};

export type PromptTemplateSaveRequest = {
  id?: number;
  taskType: string;
  departmentCode?: string;
  templateName: string;
  templateContent: string;
  outputSchema?: string;
  version?: string;
  enabled?: boolean;
};

export type SystemDictSaveRequest = {
  id?: number;
  dictType: string;
  dictKey: string;
  dictValue: string;
  sort?: number;
  status?: string;
};

export type ScheduleGenerateRequest = {
  startDate?: string;
  days?: number;
};

export type SchedulePublishRequest = {
  suggestionIds?: number[];
};

export type TriageAssignRequest = {
  triageRecordId: number;
  doctorId: number;
};

export class ApiError extends Error {
  code: number;
  status: number;

  constructor(message: string, code: number, status: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

const API_BASE = normalizeBase(import.meta.env?.VITE_API_BASE ?? "/api");
const SESSION_EVENT = "smart-cloud-brain:unauthorized";

function normalizeBase(base: string) {
  const value = base.trim() || "/api";
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

function endpoint(path: string) {
  return path.startsWith("/api") ? `${API_BASE}${path.slice(4)}` : `${API_BASE}${path}`;
}

function query(params: Record<string, string | number | boolean | null | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

function jsonOptions(method: string, body?: unknown): RequestInit {
  return body === undefined ? { method } : { method, body: JSON.stringify(body) };
}

async function parsePayload<T>(response: Response): Promise<ApiResult<T>> {
  if (typeof response.text !== "function" && typeof response.json === "function") {
    return response.json() as Promise<ApiResult<T>>;
  }
  const text = await response.text();
  if (!text) {
    return { code: response.ok ? 0 : response.status, message: response.statusText, data: undefined as T };
  }
  try {
    return JSON.parse(text) as ApiResult<T>;
  } catch {
    throw new ApiError("服务返回的数据格式无法解析", response.status || 500, response.status || 500);
  }
}

export async function request<T>(path: string, options: RequestInit = {}, token = ""): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  if (!headers.has("Content-Type") && options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(endpoint(path), { ...options, headers });
  } catch {
    throw new ApiError("无法连接到网关服务，请确认后端 gateway 已启动", 0, 0);
  }

  const payload = await parsePayload<T>(response);
  const code = payload.code ?? response.status;
  if (!response.ok || code !== 0) {
    const message = payload.message || response.statusText || "请求失败";
    const error = new ApiError(message, code, response.status);
    if ((response.status === 401 || code === 401) && typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent(SESSION_EVENT));
    }
    throw error;
  }
  return payload.data;
}

const get = <T>(path: string, token = "") => request<T>(path, jsonOptions("GET"), token);
const post = <T>(path: string, body?: unknown, token = "") => request<T>(path, jsonOptions("POST", body), token);

export const authApi = {
  registerPatient: (body: PatientRegisterRequest) => post<DataRow>("/patient/register", body),
  loginPatient: (account: string, password: string) => post<Session>("/patient/login", { account, password }),
  loginDoctor: (account: string, password: string) => post<Session>("/doctor/login", { account, password }),
  loginAdmin: (account: string, password: string) => post<Session>("/admin/login", { account, password }),
};

export const patientApi = {
  info: (token: string) => get<DataRow>("/patient/info", token),
  departments: () => get<DataRow[]>("/doctor/department/list"),
  doctors: (departmentId?: number | string) => get<DataRow[]>(`/doctor/list${query({ departmentId })}`),
  doctorDetail: (id: number) => get<DataRow>(`/doctor/detail${query({ id })}`),
  triage: (token: string, body: TriageRequest | string) =>
    post<DataRow>("/triage/consult", typeof body === "string" ? { chiefComplaint: body } : body, token),
  triageList: (token: string) => get<DataRow[]>("/triage/list", token),
  registrationSlots: (token: string) => get<DataRow[]>("/registration/slots", token),
  registrations: (token: string) => get<DataRow[]>("/registration/list", token),
  createRegistration: (token: string, body: RegistrationCreateRequest) => post<DataRow>("/registration/create", body, token),
  cancelRegistration: (token: string, registrationId: number) => post<DataRow>("/registration/cancel", { registrationId }, token),
  completeRegistration: (token: string, registrationId: number) => post<DataRow>("/registration/complete", { registrationId }, token),
  medicalRecords: (token: string) => get<DataRow[]>("/medical-record/list", token),
  medicalRecordDetail: (token: string, id: number) => get<DataRow>(`/medical-record/detail${query({ id })}`, token),
  prescriptions: (token: string) => get<DataRow[]>("/prescription/list", token),
};

export const doctorApi = {
  login: authApi.loginDoctor,
  departments: patientApi.departments,
  registrations: (token: string) => get<DataRow[]>("/registration/list", token),
  completeRegistration: (token: string, registrationId: number) => post<DataRow>("/registration/complete", { registrationId }, token),
  triageList: (token: string) => get<DataRow[]>("/triage/list", token),
  medicalRecords: (token: string) => get<DataRow[]>("/medical-record/list", token),
  medicalRecordDetail: (token: string, id: number) => get<DataRow>(`/medical-record/detail${query({ id })}`, token),
  generateMedicalRecord: (token: string, body: MedicalRecordGenerateRequest) => post<DataRow>("/medical-record/generate", body, token),
  saveMedicalRecord: (token: string, body: MedicalRecordSaveRequest) => post<DataRow>("/medical-record/save", body, token),
  checkPrescription: (token: string, body: PrescriptionCheckRequest) => post<DataRow>("/prescription/check", body, token),
  createPrescription: (token: string, body: PrescriptionCreateRequest) => post<DataRow>("/prescription/create", body, token),
  prescriptions: (token: string) => get<DataRow[]>("/prescription/list", token),
  searchDrugs: (token: string, q = "") => get<DataRow[]>(`/search/drugs${query({ q })}`, token),
  notifications: (token: string, readStatus?: string) => get<DataRow[]>(`/notification/list${query({ readStatus })}`, token),
  markNotificationRead: (token: string, notificationId: number) => post<DataRow>("/notification/read", { notificationId }, token),
};

export const adminApi = {
  login: authApi.loginAdmin,
  departments: (token: string) => get<DataRow[]>("/admin/department/list", token),
  publicDoctors: patientApi.doctors,
  saveDepartment: (token: string, body: DepartmentSaveRequest) => post<DataRow>("/admin/department/save", body, token),
  saveDoctor: (token: string, body: DoctorSaveRequest) => post<DataRow>("/admin/doctor/save", body, token),
  drugs: (token: string) => get<DataRow[]>("/admin/drug/list", token),
  saveDrug: (token: string, body: DrugSaveRequest) => post<DataRow>("/admin/drug/save", body, token),
  prompts: (token: string) => get<DataRow[]>("/admin/prompt-template/list", token),
  savePrompt: (token: string, body: PromptTemplateSaveRequest) => post<DataRow>("/admin/prompt-template/save", body, token),
  knowledgeEntries: (token: string) => get<DataRow[]>("/admin/knowledge/list", token),
  saveKnowledgeEntry: (token: string, body: KnowledgeEntrySaveRequest) => post<DataRow>("/admin/knowledge/save", body, token),
  dicts: (token: string, dictType = "") => get<DataRow[]>(`/admin/dict/list${query({ dictType })}`, token),
  saveDict: (token: string, body: SystemDictSaveRequest) => post<DataRow>("/admin/dict/save", body, token),
  generateSchedule: (token: string, body: ScheduleGenerateRequest) => post<DataRow[]>("/admin/schedule/generate", body, token),
  publishSchedule: (token: string, body: SchedulePublishRequest) => post<DataRow[]>("/admin/schedule/publish", body, token),
  schedules: (token: string) => get<DataRow[]>("/admin/schedule/list", token),
  scheduleSuggestionDetail: (token: string, id: number) => get<DataRow>(`/admin/schedule/suggestion/detail${query({ id })}`, token),
  triageDesk: (token: string) => get<DataRow[]>("/admin/triage-desk/list", token),
  triageDetail: (token: string, id: number) => get<DataRow>(`/admin/triage-desk/detail${query({ id })}`, token),
  assignTriage: (token: string, body: TriageAssignRequest) => post<DataRow>("/admin/triage-desk/assign", body, token),
  closeTriage: (token: string, triageRecordId: number) => post<DataRow>("/admin/triage-desk/close", { triageRecordId }, token),
  searchKnowledge: (token: string, q: string, departmentCode = "") =>
    get<DataRow[]>(`/search/knowledge${query({ q, departmentCode })}`, token),
  searchDrugs: (token: string, q: string) => get<DataRow[]>(`/search/drugs${query({ q })}`, token),
  searchPrompts: (token: string, q: string) => get<DataRow[]>(`/admin/search/prompts${query({ q })}`, token),
};

export const api = {
  registerPatient: authApi.registerPatient,
  loginPatient: authApi.loginPatient,
  loginDoctor: authApi.loginDoctor,
  loginAdmin: authApi.loginAdmin,
  patientInfo: patientApi.info,
  departments: patientApi.departments,
  doctors: patientApi.doctors,
  doctorDetail: patientApi.doctorDetail,
  drugs: adminApi.drugs,
  triage: patientApi.triage,
  triageList: patientApi.triageList,
  createRegistration: patientApi.createRegistration,
  registrationSlots: patientApi.registrationSlots,
  registrations: patientApi.registrations,
  cancelRegistration: patientApi.cancelRegistration,
  completeRegistration: doctorApi.completeRegistration,
  generateMedicalRecord: doctorApi.generateMedicalRecord,
  saveMedicalRecord: doctorApi.saveMedicalRecord,
  medicalRecords: patientApi.medicalRecords,
  medicalRecordDetail: patientApi.medicalRecordDetail,
  checkPrescription: doctorApi.checkPrescription,
  createPrescription: doctorApi.createPrescription,
  prescriptions: patientApi.prescriptions,
  notifications: doctorApi.notifications,
  markNotificationRead: doctorApi.markNotificationRead,
  saveDepartment: adminApi.saveDepartment,
  adminDepartments: adminApi.departments,
  saveDoctor: adminApi.saveDoctor,
  saveDrug: adminApi.saveDrug,
  prompts: adminApi.prompts,
  savePrompt: adminApi.savePrompt,
  knowledgeEntries: adminApi.knowledgeEntries,
  saveKnowledgeEntry: adminApi.saveKnowledgeEntry,
  dicts: adminApi.dicts,
  saveDict: adminApi.saveDict,
  generateSchedule: adminApi.generateSchedule,
  publishSchedule: adminApi.publishSchedule,
  schedules: adminApi.schedules,
  scheduleSuggestionDetail: adminApi.scheduleSuggestionDetail,
  triageDesk: adminApi.triageDesk,
  triageDetail: adminApi.triageDetail,
  assignTriage: adminApi.assignTriage,
  closeTriage: adminApi.closeTriage,
  searchKnowledge: adminApi.searchKnowledge,
  searchDrugs: adminApi.searchDrugs,
  searchPrompts: adminApi.searchPrompts,
};

export function gatewayBase() {
  return API_BASE.startsWith("http")
    ? API_BASE.replace(/\/api$/, "")
    : `${location.protocol}//${location.host}`;
}

function websocketBase() {
  return API_BASE.startsWith("http")
    ? API_BASE.replace(/^http/, "ws").replace(/\/api$/, "")
    : `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}`;
}

export function notificationWebSocketUrl() {
  return `${websocketBase()}/ws/notifications`;
}

export function notificationWebSocketProtocols(token: string) {
  return token ? ["bearer", token] : [];
}

export function medicalRecordStreamUrl(registrationId: number, dialogueText: string, departmentCode = "") {
  const params = new URLSearchParams({ registrationId: String(registrationId), dialogueText });
  if (departmentCode) {
    params.set("departmentCode", departmentCode);
  }
  return `${gatewayBase()}/api/medical-record/generate/stream?${params.toString()}`;
}

export function formatApiError(error: unknown, fallback: string) {
  if (error instanceof ApiError) return localizeApiMessage(error.message);
  if (error instanceof Error) return localizeApiMessage(error.message);
  return fallback;
}

function localizeApiMessage(message: string) {
  const text = message.trim();
  if (!text) return "请求失败";
  const lower = text.toLowerCase();
  if (lower.includes("triage-service unavailable")) {
    return "分诊服务暂不可用，其他管理功能可继续使用。";
  }
  if (lower.includes("doctor-service unavailable")) {
    return "医生与号源服务暂不可用，请稍后重试。";
  }
  if (lower.includes("ai service unavailable")) {
    return "智能服务暂不可用，请稍后重试。";
  }
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

export function statusClass(status: unknown) {
  const value = String(status || "").toUpperCase();
  if (["CREATED", "CONFIRMED", "COMPLETED", "AVAILABLE", "ENABLED", "PUBLISHED", "AI_RECOMMENDED", "LOW", "READ"].includes(value)) {
    return "success";
  }
  if (["CANCELLED", "FAILED", "DISABLED", "HIGH", "CLOSED", "FULL"].includes(value)) {
    return "danger";
  }
  if (["PENDING", "DRAFT", "UNPUBLISHED", "UNREVIEWED", "MEDIUM", "MANUAL_REQUIRED", "UNREAD"].includes(value)) {
    return "warning";
  }
  return "info";
}

export function statusText(status: unknown, fallback = "-") {
  const raw = String(status ?? "").trim();
  if (!raw) return fallback;
  const labels: Record<string, string> = {
    CREATED: "已创建",
    CONFIRMED: "已确认",
    COMPLETED: "已完成",
    AVAILABLE: "可预约",
    ENABLED: "启用",
    PUBLISHED: "已发布",
    AI_RECOMMENDED: "智能推荐",
    LOW: "低风险",
    READ: "已读",
    CANCELLED: "已取消",
    FAILED: "失败",
    DISABLED: "停用",
    HIGH: "高风险",
    CLOSED: "已关闭",
    FULL: "已约满",
    PENDING: "待处理",
    DRAFT: "草稿",
    UNPUBLISHED: "未发布",
    UNREVIEWED: "未审核",
    MEDIUM: "中风险",
    MANUAL_REQUIRED: "待人工处理",
    UNREAD: "未读",
    PATIENT: "患者",
    DOCTOR: "医生",
    ADMIN: "管理员",
    MALE: "男",
    FEMALE: "女",
    UNKNOWN: "未说明",
  };
  return labels[raw.toUpperCase()] ?? raw;
}

export const useAuthStore = defineStore("auth", () => {
  const session = ref<Session | null>(null);
  const storageKey = ref("smart-cloud-brain-session");
  const permissionError = ref("");

  const isAuthenticated = computed(() => Boolean(session.value?.token));

  function load(key: string, expectedRole?: Role) {
    storageKey.value = key;
    const raw = localStorage.getItem(key);
    session.value = raw ? JSON.parse(raw) as Session : null;
    permissionError.value = "";
    if (session.value && expectedRole && session.value.role !== expectedRole) {
      permissionError.value = `当前登录角色为 ${session.value.role}，无权访问 ${expectedRole} 端业务。`;
    }
  }

  function save(key: string, nextSession: Session, expectedRole?: Role) {
    storageKey.value = key;
    session.value = nextSession;
    localStorage.setItem(key, JSON.stringify(nextSession));
    permissionError.value = expectedRole && nextSession.role !== expectedRole
      ? `当前登录角色为 ${nextSession.role}，无权访问 ${expectedRole} 端业务。`
      : "";
  }

  function logout() {
    session.value = null;
    permissionError.value = "";
    localStorage.removeItem(storageKey.value);
  }

  function token() {
    return session.value?.token ?? "";
  }

  function requireRole(role: Role) {
    if (!session.value) return false;
    if (session.value.role !== role) {
      permissionError.value = `当前登录角色为 ${session.value.role}，无权访问 ${role} 端业务。`;
      return false;
    }
    permissionError.value = "";
    return true;
  }

  function bindUnauthorized() {
    const handler = () => logout();
    if (typeof window === "undefined") {
      return () => undefined;
    }
    window.addEventListener(SESSION_EVENT, handler);
    return () => window.removeEventListener(SESSION_EVENT, handler);
  }

  return { session, permissionError, isAuthenticated, load, save, logout, token, requireRole, bindUnauthorized };
});

export const usePatientWorkflowStore = defineStore("patientWorkflow", () => {
  const patient = ref<DataRow | null>(null);
  const departments = ref<DataRow[]>([]);
  const doctors = ref<DataRow[]>([]);
  const triage = ref<DataRow | null>(null);
  const triageHistory = ref<DataRow[]>([]);
  const slots = ref<DataRow[]>([]);
  const registrations = ref<DataRow[]>([]);
  const records = ref<DataRow[]>([]);
  const prescriptions = ref<DataRow[]>([]);

  async function refreshPublicData() {
    departments.value = await patientApi.departments();
    doctors.value = await patientApi.doctors();
  }

  async function refreshAuthenticated(token: string) {
    const [info, triages, slotList, registrationList, recordList, prescriptionList] = await Promise.all([
      patientApi.info(token),
      patientApi.triageList(token),
      patientApi.registrationSlots(token),
      patientApi.registrations(token),
      patientApi.medicalRecords(token),
      patientApi.prescriptions(token),
    ]);
    patient.value = info;
    triageHistory.value = triages;
    slots.value = slotList;
    registrations.value = registrationList;
    records.value = recordList;
    prescriptions.value = prescriptionList;
    triage.value = triages[0] ?? triage.value;
  }

  return {
    patient,
    departments,
    doctors,
    triage,
    triageHistory,
    slots,
    registrations,
    records,
    prescriptions,
    refreshPublicData,
    refreshAuthenticated,
  };
});

export const useDoctorWorkflowStore = defineStore("doctorWorkflow", () => {
  const registrations = ref<DataRow[]>([]);
  const triageRecords = ref<DataRow[]>([]);
  const records = ref<DataRow[]>([]);
  const prescriptions = ref<DataRow[]>([]);
  const drugs = ref<DataRow[]>([]);
  const notifications = ref<DataRow[]>([]);
  const streamText = ref("");
  const streamStatus = ref("IDLE");

  async function refresh(token: string) {
    const [registrationList, triageList, recordList, prescriptionList, drugList, notificationList] = await Promise.all([
      doctorApi.registrations(token),
      doctorApi.triageList(token),
      doctorApi.medicalRecords(token),
      doctorApi.prescriptions(token),
      doctorApi.searchDrugs(token, ""),
      doctorApi.notifications(token),
    ]);
    registrations.value = registrationList;
    triageRecords.value = triageList;
    records.value = recordList;
    prescriptions.value = prescriptionList;
    drugs.value = drugList;
    notifications.value = notificationList;
  }

  return { registrations, triageRecords, records, prescriptions, drugs, notifications, streamText, streamStatus, refresh };
});

export const useAdminWorkflowStore = defineStore("adminWorkflow", () => {
  const departments = ref<DataRow[]>([]);
  const doctors = ref<DataRow[]>([]);
  const drugs = ref<DataRow[]>([]);
  const prompts = ref<DataRow[]>([]);
  const knowledge = ref<DataRow[]>([]);
  const dicts = ref<DataRow[]>([]);
  const suggestions = ref<DataRow[]>([]);
  const schedules = ref<DataRow[]>([]);
  const triageDesk = ref<DataRow[]>([]);
  const selectedScheduleSuggestion = ref<DataRow | null>(null);
  const selectedTriage = ref<DataRow | null>(null);
  const refreshErrors = ref<Record<string, string>>({});

  async function refresh(token: string) {
    const nextErrors: Record<string, string> = {};
    const keepCurrent = <T>(key: string, fallbackMessage: string, current: T) => (error: unknown) => {
      nextErrors[key] = formatApiError(error, fallbackMessage);
      return current;
    };
    const [departmentList, doctorList, drugList, promptList, knowledgeList, dictList, scheduleList, triageList] = await Promise.all([
      adminApi.departments(token).catch(keepCurrent("departments", "科室列表加载失败", departments.value)),
      adminApi.publicDoctors().catch(keepCurrent("doctors", "医生列表加载失败", doctors.value)),
      adminApi.drugs(token).catch(keepCurrent("drugs", "药品列表加载失败", drugs.value)),
      adminApi.prompts(token).catch(keepCurrent("prompts", "提示词模板加载失败", prompts.value)),
      adminApi.knowledgeEntries(token).catch(keepCurrent("knowledge", "知识库加载失败", knowledge.value)),
      adminApi.dicts(token).catch(keepCurrent("dicts", "字典加载失败", dicts.value)),
      adminApi.schedules(token).catch(keepCurrent("schedules", "排班列表加载失败", schedules.value)),
      adminApi.triageDesk(token).catch(keepCurrent("triageDesk", "分诊台加载失败", [] as DataRow[])),
    ]);
    departments.value = departmentList;
    doctors.value = doctorList;
    drugs.value = drugList;
    prompts.value = promptList;
    knowledge.value = knowledgeList;
    dicts.value = dictList;
    schedules.value = scheduleList;
    triageDesk.value = triageList;
    refreshErrors.value = nextErrors;
  }

  return {
    departments,
    doctors,
    drugs,
    prompts,
    knowledge,
    dicts,
    suggestions,
    schedules,
    triageDesk,
    selectedScheduleSuggestion,
    selectedTriage,
    refreshErrors,
    refresh,
  };
});
