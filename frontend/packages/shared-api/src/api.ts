import {
  ApiError,
  type ApiResult,
  type CurrentUser,
  type DataRow,
  type DepartmentSaveRequest,
  type DoctorSaveRequest,
  type DrugSaveRequest,
  type KnowledgeEntrySaveRequest,
  type MedicalRecordGenerateRequest,
  type MedicalRecordSaveRequest,
  type PatientRegisterRequest,
  type PrescriptionCheckRequest,
  type PrescriptionCreateRequest,
  type PromptTemplateSaveRequest,
  type RegistrationCreateRequest,
  type ScheduleGenerateRequest,
  type SchedulePublishRequest,
  type Session,
  type SystemDictSaveRequest,
  type TriageAssignRequest,
  type TriageRequest,
} from "./types";

const API_BASE = normalizeBase(import.meta.env?.VITE_API_BASE ?? "/api");
export const SESSION_EVENT = "smart-cloud-brain:unauthorized";

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
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
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
  if (!text) return { code: response.ok ? 0 : response.status, message: response.statusText, data: undefined as T };
  try {
    return JSON.parse(text) as ApiResult<T>;
  } catch {
    throw new ApiError("服务返回的数据格式无法解析", response.status || 500, response.status || 500);
  }
}

export async function request<T>(path: string, options: RequestInit = {}, token = ""): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  if (!headers.has("Content-Type") && options.body !== undefined) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(endpoint(path), { ...options, headers });
  } catch {
    throw new ApiError("无法连接到网关服务，请确认后端 gateway 已启动", 0, 0);
  }

  const payload = await parsePayload<T>(response);
  const code = payload.code ?? response.status;
  if (!response.ok || code !== 0) {
    const error = new ApiError(payload.message || response.statusText || "请求失败", code, response.status);
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
  currentUser: (token: string) => get<CurrentUser>("/auth/me", token),
};

export const patientApi = {
  info: (token: string) => get<DataRow>("/patient/info", token),
  departments: () => get<DataRow[]>("/doctor/department/list"),
  doctors: (departmentId?: number | string) => get<DataRow[]>(`/doctor/list${query({ departmentId })}`),
  doctorDetail: (id: number) => get<DataRow>(`/doctor/detail${query({ id })}`),
  triage: (token: string, body: TriageRequest | string) => post<DataRow>("/triage/consult", typeof body === "string" ? { chiefComplaint: body } : body, token),
  triageList: (token: string) => get<DataRow[]>("/triage/list", token),
  registrationSlots: (token: string) => get<DataRow[]>("/registration/slots", token),
  registrations: (token: string) => get<DataRow[]>("/registration/list", token),
  createRegistration: (token: string, body: RegistrationCreateRequest) => post<DataRow>("/registration/create", body, token),
  cancelRegistration: (token: string, registrationId: number) => post<DataRow>("/registration/cancel", { registrationId }, token),
  completeRegistration: (token: string, registrationId: number) => post<DataRow>("/registration/complete", { registrationId }, token),
  medicalRecords: (token: string) => get<DataRow[]>("/medical-record/list", token),
  medicalRecordDetail: (token: string, id: number) => get<DataRow>(`/medical-record/detail${query({ id })}`, token),
  prescriptions: (token: string) => get<DataRow[]>("/prescription/list", token),
  prescriptionDetail: (token: string, id: number) => get<DataRow>(`/prescription/detail${query({ id })}`, token),
};

export const doctorApi = {
  login: authApi.loginDoctor,
  departments: patientApi.departments,
  registrations: (token: string) => get<DataRow[]>("/registration/list", token),
  completeRegistration: patientApi.completeRegistration,
  triageList: (token: string) => get<DataRow[]>("/triage/list", token),
  medicalRecords: (token: string) => get<DataRow[]>("/medical-record/list", token),
  medicalRecordDetail: patientApi.medicalRecordDetail,
  generateMedicalRecord: (token: string, body: MedicalRecordGenerateRequest) => post<DataRow>("/medical-record/generate", body, token),
  saveMedicalRecord: (token: string, body: MedicalRecordSaveRequest) => post<DataRow>("/medical-record/save", body, token),
  checkPrescription: (token: string, body: PrescriptionCheckRequest) => post<DataRow>("/prescription/check", body, token),
  createPrescription: (token: string, body: PrescriptionCreateRequest) => post<DataRow>("/prescription/create", body, token),
  prescriptions: (token: string) => get<DataRow[]>("/prescription/list", token),
  prescriptionDetail: patientApi.prescriptionDetail,
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
  searchKnowledge: (token: string, q: string, departmentCode = "") => get<DataRow[]>(`/search/knowledge${query({ q, departmentCode })}`, token),
  searchDrugs: (token: string, q: string) => get<DataRow[]>(`/search/drugs${query({ q })}`, token),
  searchPrompts: (token: string, q: string) => get<DataRow[]>(`/admin/search/prompts${query({ q })}`, token),
};

export const api = {
  registerPatient: authApi.registerPatient, currentUser: authApi.currentUser,
  loginPatient: authApi.loginPatient, loginDoctor: authApi.loginDoctor, loginAdmin: authApi.loginAdmin,
  patientInfo: patientApi.info, departments: patientApi.departments, doctors: patientApi.doctors,
  doctorDetail: patientApi.doctorDetail, drugs: adminApi.drugs, triage: patientApi.triage,
  triageList: patientApi.triageList, createRegistration: patientApi.createRegistration,
  registrationSlots: patientApi.registrationSlots, registrations: patientApi.registrations,
  cancelRegistration: patientApi.cancelRegistration, completeRegistration: doctorApi.completeRegistration,
  generateMedicalRecord: doctorApi.generateMedicalRecord, saveMedicalRecord: doctorApi.saveMedicalRecord,
  medicalRecords: patientApi.medicalRecords, medicalRecordDetail: patientApi.medicalRecordDetail,
  checkPrescription: doctorApi.checkPrescription, createPrescription: doctorApi.createPrescription,
  prescriptions: patientApi.prescriptions, prescriptionDetail: patientApi.prescriptionDetail,
  notifications: doctorApi.notifications, markNotificationRead: doctorApi.markNotificationRead,
  saveDepartment: adminApi.saveDepartment, adminDepartments: adminApi.departments,
  saveDoctor: adminApi.saveDoctor, saveDrug: adminApi.saveDrug, prompts: adminApi.prompts,
  savePrompt: adminApi.savePrompt, knowledgeEntries: adminApi.knowledgeEntries,
  saveKnowledgeEntry: adminApi.saveKnowledgeEntry, dicts: adminApi.dicts, saveDict: adminApi.saveDict,
  generateSchedule: adminApi.generateSchedule, publishSchedule: adminApi.publishSchedule,
  schedules: adminApi.schedules, scheduleSuggestionDetail: adminApi.scheduleSuggestionDetail,
  triageDesk: adminApi.triageDesk, triageDetail: adminApi.triageDetail,
  assignTriage: adminApi.assignTriage, closeTriage: adminApi.closeTriage,
  searchKnowledge: adminApi.searchKnowledge, searchDrugs: adminApi.searchDrugs, searchPrompts: adminApi.searchPrompts,
};

export function gatewayBase() {
  return API_BASE.startsWith("http") ? API_BASE.replace(/\/api$/, "") : `${location.protocol}//${location.host}`;
}

function websocketBase() {
  return API_BASE.startsWith("http")
    ? API_BASE.replace(/^http/, "ws").replace(/\/api$/, "")
    : `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}`;
}

export function notificationWebSocketUrl(token = "") {
  const base = `${websocketBase()}/ws/notifications`;
  return token ? `${base}?token=${encodeURIComponent(token)}` : base;
}

export function medicalRecordStreamUrl(registrationId: number, dialogueText: string, departmentCode = "") {
  const params = new URLSearchParams({ registrationId: String(registrationId), dialogueText });
  if (departmentCode) params.set("departmentCode", departmentCode);
  return `${gatewayBase()}/api/medical-record/generate/stream?${params.toString()}`;
}
