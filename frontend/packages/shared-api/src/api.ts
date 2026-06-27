import {
  ApiError,
  type AccountSaveRequest,
  type ApiResult,
  type CurrentUser,
  type DataRow,
  type DepartmentSaveRequest,
  type DeviceSaveRequest,
  type DeviceStatusRequest,
  type DeviceUsageSaveRequest,
  type DoctorSaveRequest,
  type DrugSaveRequest,
  type KnowledgeEntrySaveRequest,
  type MedicalRecordGenerateRequest,
  type MedicalRecordSaveRequest,
  type PatientSaveRequest,
  type PatientVisitorSaveRequest,
  type PatientRegisterRequest,
  type PatientSiteConfigRecord,
  type PatientSiteConfigHistoryPage,
  type PatientSiteConfigPublishRequest,
  type PatientSiteConfigSaveRequest,
  type PatientSitePreviewToken,
  type PrescriptionCheckRequest,
  type PrescriptionCreateRequest,
  type PromptTestRequest,
  type PromptTemplateSaveRequest,
  type RegistrationCreateRequest,
  type RolePermissionSaveRequest,
  type ScheduleCancelRequest,
  type ScheduleGenerateRequest,
  type SchedulePublishRequest,
  type ScheduleSaveRequest,
  type StatisticsQuery,
  type Session,
  type SystemDictSaveRequest,
  type TriageAssignRequest,
  type TriageRequest,
} from "./types";

export type NotificationQuery = {
  readStatus?: string;
  handleStatus?: string;
  type?: string;
  riskLevel?: string;
  q?: string;
  sort?: string;
};

const API_BASE = normalizeBase(import.meta.env?.VITE_API_BASE ?? "/api");
export const SESSION_EVENT = "smart-cloud-brain:unauthorized";

/* ---------- Token Provider（自动注入，避免每个 API 函数手动传 token） ---------- */
let _tokenProvider: () => string = () => "";

/** 在应用入口调用一次，将 authStore.token() 注入为默认 token 来源 */
export function setTokenProvider(provider: () => string) {
  _tokenProvider = provider;
}

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
  const resolvedToken = token || _tokenProvider();
  const headers = new Headers(options.headers ?? {});
  if (!headers.has("Content-Type") && options.body !== undefined) headers.set("Content-Type", "application/json");
  if (resolvedToken) headers.set("Authorization", `Bearer ${resolvedToken}`);

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
  siteConfig: () => get<DataRow>("/patient-site/config"),
  sitePreviewConfig: (previewToken: string) => get<DataRow>(`/patient-site/preview${query({ token: previewToken })}`),
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
  saveProfile: (token: string, body: PatientSaveRequest) => post<DataRow>("/patient/profile/save", body, token),
  visitors: (token: string) => get<DataRow[]>("/patient/visitor/list", token),
  saveVisitor: (token: string, body: PatientVisitorSaveRequest) => post<DataRow>("/patient/visitor/save", body, token),
  deleteVisitor: (token: string, id: number) => post<DataRow>("/patient/visitor/delete", { id }, token),
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
  notifications: (token: string, params?: string | NotificationQuery) => get<DataRow[]>(`/notification/list${query(typeof params === "string" ? { readStatus: params } : params ?? {})}`, token),
  markNotificationRead: (token: string, notificationId: number) => post<DataRow>("/notification/read", { notificationId }, token),
  handleNotification: (token: string, notificationId: number, handleStatus: "HANDLED" | "IGNORED") => post<DataRow>("/notification/handle", { notificationId, handleStatus }, token),
  schedules: (token: string, params: StatisticsQuery & { status?: string } = {}) => get<DataRow[]>(`/doctor/schedule/list${query(params)}`, token),
};

export const adminApi = {
  login: authApi.loginAdmin,
  accounts: (token: string) => get<DataRow[]>("/admin/account/list", token),
  roles: (token: string) => get<DataRow[]>("/admin/role/list", token),
  saveAccount: (token: string, body: AccountSaveRequest) => post<DataRow>("/admin/account/save", body, token),
  departments: (token: string) => get<DataRow[]>("/admin/department/list", token),
  publicDoctors: patientApi.doctors,
  saveDepartment: (token: string, body: DepartmentSaveRequest) => post<DataRow>("/admin/department/save", body, token),
  saveDoctor: (token: string, body: DoctorSaveRequest) => post<DataRow>("/admin/doctor/save", body, token),
  drugs: (token: string) => get<DataRow[]>("/admin/drug/list", token),
  saveDrug: (token: string, body: DrugSaveRequest) => post<DataRow>("/admin/drug/save", body, token),
  prompts: (token: string) => get<DataRow[]>("/admin/prompt-template/list", token),
  savePrompt: (token: string, body: PromptTemplateSaveRequest) => post<DataRow>("/admin/prompt-template/save", body, token),
  testPrompt: (token: string, body: PromptTestRequest) => post<DataRow>("/admin/prompt-template/test", body, token),
  aiLogs: (token: string) => get<DataRow[]>("/admin/ai-log/list", token),
  knowledgeEntries: (token: string) => get<DataRow[]>("/admin/knowledge/list", token),
  saveKnowledgeEntry: (token: string, body: KnowledgeEntrySaveRequest) => post<DataRow>("/admin/knowledge/save", body, token),
  dicts: (token: string, dictType = "") => get<DataRow[]>(`/admin/dict/list${query({ dictType })}`, token),
  saveDict: (token: string, body: SystemDictSaveRequest) => post<DataRow>("/admin/dict/save", body, token),
  generateSchedule: (token: string, body: ScheduleGenerateRequest) => post<DataRow[]>("/admin/schedule/generate", body, token),
  publishSchedule: (token: string, body: SchedulePublishRequest) => post<DataRow[]>("/admin/schedule/publish", body, token),
  schedules: (token: string) => get<DataRow[]>("/admin/schedule/list", token),
  filteredSchedules: (token: string, params: StatisticsQuery & { departmentId?: number; doctorId?: number; status?: string }) => get<DataRow[]>(`/admin/schedule/list${query(params)}`, token),
  saveSchedule: (token: string, body: ScheduleSaveRequest) => post<DataRow>("/admin/schedule/save", body, token),
  cancelSchedule: (token: string, body: ScheduleCancelRequest) => post<DataRow>("/admin/schedule/cancel", body, token),
  scheduleSuggestionDetail: (token: string, id: number) => get<DataRow>(`/admin/schedule/suggestion/detail${query({ id })}`, token),
  triageDesk: (token: string) => get<DataRow[]>("/admin/triage-desk/list", token),
  triageDetail: (token: string, id: number) => get<DataRow>(`/admin/triage-desk/detail${query({ id })}`, token),
  assignTriage: (token: string, body: TriageAssignRequest) => post<DataRow>("/admin/triage-desk/assign", body, token),
  closeTriage: (token: string, triageRecordId: number) => post<DataRow>("/admin/triage-desk/close", { triageRecordId }, token),
  devices: (token: string, params: { keyword?: string; departmentId?: number; category?: string; status?: string } = {}) => get<DataRow[]>(`/admin/device/list${query(params)}`, token),
  saveDevice: (token: string, body: DeviceSaveRequest) => post<DataRow>("/admin/device/save", body, token),
  updateDeviceStatus: (token: string, body: DeviceStatusRequest) => post<DataRow>("/admin/device/status", body, token),
  deviceUsages: (token: string, deviceId?: number) => get<DataRow[]>(`/admin/device/usage/list${query({ deviceId })}`, token),
  saveDeviceUsage: (token: string, body: DeviceUsageSaveRequest) => post<DataRow>("/admin/device/usage/save", body, token),
  patients: (token: string, params: { keyword?: string; gender?: string; minAge?: number; maxAge?: number } = {}) => get<DataRow[]>(`/admin/patient/list${query(params)}`, token),
  patientDetail: (token: string, id: number) => get<DataRow>(`/admin/patient/detail${query({ id })}`, token),
  savePatient: (token: string, body: PatientSaveRequest) => post<DataRow>("/admin/patient/save", body, token),
  statisticsOverview: (token: string) => get<DataRow>("/admin/statistics/overview", token),
  statisticsTrend: (token: string, params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/trend${query(params)}`, token),
  doctorWorkload: (token: string, params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/doctor-workload${query(params)}`, token),
  patientDistribution: (token: string) => get<DataRow>("/admin/statistics/patient-distribution", token),
  deviceUsageStatistics: (token: string) => get<DataRow[]>("/admin/statistics/device-usage", token),
  statisticsReport: (token: string, params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/report${query(params)}`, token),
  permissions: (token: string) => get<DataRow>("/admin/permission/list", token),
  myPermissions: (token: string) => get<string[]>("/admin/permission/my", token),
  saveRolePermissions: (token: string, body: RolePermissionSaveRequest) => post<DataRow>("/admin/permission/save-role", body, token),
  patientSiteConfig: (token: string, configKey?: string) => get<DataRow>(`/admin/patient-site/config${query({ configKey })}`, token),
  savePatientSiteConfig: (token: string, body: PatientSiteConfigSaveRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/save", body, token),
  savePublishedPatientSiteConfig: (token: string, body: PatientSiteConfigSaveRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/save-published", body, token),
  publishPatientSiteConfig: (token: string, body: PatientSiteConfigPublishRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/publish", body, token),
  patientSiteConfigHistory: (token: string, configKey: string, page = 1, pageSize = 10) =>
    get<PatientSiteConfigHistoryPage>(`/admin/patient-site/history${query({ configKey, page, pageSize })}`, token),
  patientSitePreviewToken: (token: string, configKey: string, version?: number) =>
    post<PatientSitePreviewToken>(`/admin/patient-site/preview-token${query({ configKey, version })}`, undefined, token),
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
  notifications: doctorApi.notifications, markNotificationRead: doctorApi.markNotificationRead, handleNotification: doctorApi.handleNotification,
  saveDepartment: adminApi.saveDepartment, adminDepartments: adminApi.departments,
  accounts: adminApi.accounts, roles: adminApi.roles, saveAccount: adminApi.saveAccount,
  saveDoctor: adminApi.saveDoctor, saveDrug: adminApi.saveDrug, prompts: adminApi.prompts,
  savePrompt: adminApi.savePrompt, testPrompt: adminApi.testPrompt, aiLogs: adminApi.aiLogs, knowledgeEntries: adminApi.knowledgeEntries,
  saveKnowledgeEntry: adminApi.saveKnowledgeEntry, dicts: adminApi.dicts, saveDict: adminApi.saveDict,
  generateSchedule: adminApi.generateSchedule, publishSchedule: adminApi.publishSchedule,
  schedules: adminApi.schedules, scheduleSuggestionDetail: adminApi.scheduleSuggestionDetail,
  triageDesk: adminApi.triageDesk, triageDetail: adminApi.triageDetail,
  assignTriage: adminApi.assignTriage, closeTriage: adminApi.closeTriage,
  saveProfile: patientApi.saveProfile, doctorSchedules: doctorApi.schedules,
  patientVisitors: patientApi.visitors, savePatientVisitor: patientApi.saveVisitor, deletePatientVisitor: patientApi.deleteVisitor,
  filteredSchedules: adminApi.filteredSchedules, saveSchedule: adminApi.saveSchedule, cancelSchedule: adminApi.cancelSchedule,
  devices: adminApi.devices, saveDevice: adminApi.saveDevice, updateDeviceStatus: adminApi.updateDeviceStatus,
  deviceUsages: adminApi.deviceUsages, saveDeviceUsage: adminApi.saveDeviceUsage,
  patients: adminApi.patients, patientDetail: adminApi.patientDetail, savePatient: adminApi.savePatient,
  statisticsOverview: adminApi.statisticsOverview, statisticsTrend: adminApi.statisticsTrend,
  doctorWorkload: adminApi.doctorWorkload, patientDistribution: adminApi.patientDistribution,
  deviceUsageStatistics: adminApi.deviceUsageStatistics, statisticsReport: adminApi.statisticsReport,
  permissions: adminApi.permissions, saveRolePermissions: adminApi.saveRolePermissions,
  myPermissions: adminApi.myPermissions,
  patientSiteConfig: patientApi.siteConfig,
  patientSitePreviewConfig: patientApi.sitePreviewConfig,
  adminPatientSiteConfig: adminApi.patientSiteConfig,
  savePatientSiteConfig: adminApi.savePatientSiteConfig,
  savePublishedPatientSiteConfig: adminApi.savePublishedPatientSiteConfig,
  publishPatientSiteConfig: adminApi.publishPatientSiteConfig,
  patientSiteConfigHistory: adminApi.patientSiteConfigHistory,
  patientSitePreviewToken: adminApi.patientSitePreviewToken,
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
