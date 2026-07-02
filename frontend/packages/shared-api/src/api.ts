import {
  ApiError,
  type Account,
  type AccountSaveRequest,
  type Department,
  type Doctor,
  type MedicalRecord,
  type Notification,
  type Patient,
  type Prescription,
  type Registration,
  type AppointmentSlot,
  type Schedule,
  type TriageRecord,
  type ApiResult,
  type AssetUploadPolicyRequest,
  type AssetUploadPolicyResponse,
  type CurrentUser,
  type DataRow,
  type DepartmentSaveRequest,
  type DeviceSaveRequest,
  type DeviceStatusRequest,
  type DeviceUsageSaveRequest,
  type DoctorSaveRequest,
  type Drug,
  type DrugSaveRequest,
  type EmailConfig,
  type EmailConfigSaveRequest,
  type EmailConfigTestRequest,
  type MedicalRecordGenerateRequest,
  type MedicalRecordSaveRequest,
  type PatientNotice,
  type PatientNoticeSaveRequest,
  type PatientNoticeSortRequest,
  type PatientNoticeStatusRequest,
  type PatientRecommendation,
  type PatientRecommendationSaveRequest,
  type PatientRecommendationSortRequest,
  type PatientRecommendationStatusRequest,
  type PatientRecommendationType,
  type PatientSaveRequest,
  type PatientEmailCodeRequest,
  type PatientVisitorSaveRequest,
  type PatientRegisterRequest,
  type PatientSiteConfigRecord,
  type PatientSiteConfigHistoryPage,
  type PatientSiteAdminConfigMap,
  type PatientSiteConfigPublishRequest,
  type PatientSiteConfigSaveRequest,
  type PatientSiteHomeResponse,
  type PatientSitePreviewToken,
  type PatientSitePublicConfigResponse,
  type PrescriptionCheckRequest,
  type PrescriptionCheckResult,
  type PrescriptionCreateRequest,
  type RegistrationCreateRequest,
  type RolePermissionSaveRequest,
  type ScheduleCancelRequest,
  type ScheduleGenerateRequest,
  type SchedulePublishRequest,
  type ScheduleSaveRequest,
  type StatisticsQuery,
  type Session,
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

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const resolvedToken = _tokenProvider();
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

const get = <T>(path: string) => request<T>(path, jsonOptions("GET"));
const post = <T>(path: string, body?: unknown) => request<T>(path, jsonOptions("POST", body));

function getAdminPatientSiteConfig(): Promise<PatientSiteAdminConfigMap>;
function getAdminPatientSiteConfig(configKey: string): Promise<PatientSiteConfigRecord>;
function getAdminPatientSiteConfig(configKey?: string) {
  return get<PatientSiteAdminConfigMap | PatientSiteConfigRecord>(`/admin/patient-site/config${query({ configKey })}`);
}

export const authApi = {
  registerPatient: (body: PatientRegisterRequest) => post<DataRow>("/patient/register", body),
  sendPatientEmailCode: (body: PatientEmailCodeRequest) => post<DataRow>("/patient/email-code/send", body),
  loginPatient: (account: string, password: string) => post<Session>("/patient/login", { account, password }),
  loginDoctor: (account: string, password: string) => post<Session>("/doctor/login", { account, password }),
  loginAdmin: (account: string, password: string) => post<Session>("/admin/login", { account, password }),
  currentUser: () => get<CurrentUser>("/auth/me"),
};

export const patientApi = {
  info: () => get<Patient>("/patient/info"),
  siteConfig: () => get<PatientSitePublicConfigResponse>("/patient-site/config"),
  homeConfig: () => get<PatientSiteHomeResponse>("/patient-site/home"),
  sitePreviewConfig: (previewToken: string) => get<PatientSitePublicConfigResponse>(`/patient-site/preview${query({ token: previewToken })}`),
  notices: () => get<PatientNotice[]>("/patient-site/notices"),
  recommendations: (type: PatientRecommendationType) => get<PatientRecommendation[]>(`/patient-site/recommendations${query({ type })}`),
  departments: () => get<Department[]>("/doctor/department/list"),
  doctors: (departmentId?: number | string) => get<Doctor[]>(`/doctor/list${query({ departmentId })}`),
  doctorDetail: (id: number) => get<Doctor>(`/doctor/detail${query({ id })}`),
  triage: (body: TriageRequest | string) => post<TriageRecord>("/triage/consult", typeof body === "string" ? { chiefComplaint: body } : body),
  triageList: () => get<TriageRecord[]>("/triage/list"),
  registrationSlots: () => get<AppointmentSlot[]>("/registration/slots"),
  registrations: () => get<Registration[]>("/registration/list"),
  createRegistration: (body: RegistrationCreateRequest) => post<Registration>("/registration/create", body),
  cancelRegistration: (registrationId: number) => post<Registration>("/registration/cancel", { registrationId }),
  completeRegistration: (registrationId: number) => post<Registration>("/registration/complete", { registrationId }),
  medicalRecords: () => get<MedicalRecord[]>("/medical-record/list"),
  medicalRecordDetail: (id: number) => get<MedicalRecord>(`/medical-record/detail${query({ id })}`),
  prescriptions: () => get<Prescription[]>("/prescription/list"),
  prescriptionDetail: (id: number) => get<Prescription>(`/prescription/detail${query({ id })}`),
  saveProfile: (body: PatientSaveRequest) => post<Patient>("/patient/profile/save", body),
  visitors: () => get<Patient[]>("/patient/visitor/list"),
  saveVisitor: (body: PatientVisitorSaveRequest) => post<DataRow>("/patient/visitor/save", body),
  deleteVisitor: (id: number) => post<DataRow>("/patient/visitor/delete", { id }),
};

export const doctorApi = {
  login: authApi.loginDoctor,
  departments: patientApi.departments,
  registrations: () => get<Registration[]>("/registration/list"),
  completeRegistration: patientApi.completeRegistration,
  triageList: () => get<TriageRecord[]>("/triage/list"),
  medicalRecords: () => get<MedicalRecord[]>("/medical-record/list"),
  medicalRecordDetail: patientApi.medicalRecordDetail,
  generateMedicalRecord: (body: MedicalRecordGenerateRequest) => post<MedicalRecord>("/medical-record/generate", body),
  saveMedicalRecord: (body: MedicalRecordSaveRequest) => post<MedicalRecord>("/medical-record/save", body),
  checkPrescription: (body: PrescriptionCheckRequest) => post<PrescriptionCheckResult>("/prescription/check", body),
  createPrescription: (body: PrescriptionCreateRequest) => post<Prescription>("/prescription/create", body),
  drugs: () => get<Drug[]>("/prescription/drug/list"),
  prescriptions: () => get<Prescription[]>("/prescription/list"),
  prescriptionDetail: patientApi.prescriptionDetail,
  notifications: (params?: string | NotificationQuery) => get<Notification[]>(`/notification/list${query(typeof params === "string" ? { readStatus: params } : params ?? {})}`),
  markNotificationRead: (notificationId: number) => post<Notification>("/notification/read", { notificationId }),
  handleNotification: (notificationId: number, handleStatus: "HANDLED" | "IGNORED") => post<Notification>("/notification/handle", { notificationId, handleStatus }),
  schedules: (params: StatisticsQuery & { status?: string } = {}) => get<Schedule[]>(`/doctor/schedule/list${query(params)}`),
};

export const adminApi = {
  login: authApi.loginAdmin,
  accounts: () => get<DataRow[]>("/admin/account/list"),
  roles: () => get<DataRow[]>("/admin/role/list"),
  saveAccount: (body: AccountSaveRequest) => post<DataRow>("/admin/account/save", body),
  departments: () => get<DataRow[]>("/admin/department/list"),
  doctors: (departmentId?: number | string) => get<DataRow[]>(`/admin/doctor/list${query({ departmentId })}`),
  saveDepartment: (body: DepartmentSaveRequest) => post<DataRow>("/admin/department/save", body),
  saveDoctor: (body: DoctorSaveRequest) => post<DataRow>("/admin/doctor/save", body),
  drugs: () => get<DataRow[]>("/admin/drug/list"),
  saveDrug: (body: DrugSaveRequest) => post<DataRow>("/admin/drug/save", body),
  aiLogs: () => get<DataRow[]>("/admin/ai-log/list"),
  generateSchedule: (body: ScheduleGenerateRequest) => post<DataRow[]>("/admin/schedule/generate", body),
  publishSchedule: (body: SchedulePublishRequest) => post<DataRow[]>("/admin/schedule/publish", body),
  schedules: () => get<DataRow[]>("/admin/schedule/list"),
  filteredSchedules: (params: StatisticsQuery & { departmentId?: number; doctorId?: number; status?: string }) => get<DataRow[]>(`/admin/schedule/list${query(params)}`),
  saveSchedule: (body: ScheduleSaveRequest) => post<DataRow>("/admin/schedule/save", body),
  cancelSchedule: (body: ScheduleCancelRequest) => post<DataRow>("/admin/schedule/cancel", body),
  scheduleSuggestionDetail: (id: number) => get<DataRow>(`/admin/schedule/suggestion/detail${query({ id })}`),
  triageDesk: () => get<DataRow[]>("/admin/triage-desk/list"),
  triageDetail: (id: number) => get<DataRow>(`/admin/triage-desk/detail${query({ id })}`),
  assignTriage: (body: TriageAssignRequest) => post<DataRow>("/admin/triage-desk/assign", body),
  closeTriage: (triageRecordId: number) => post<DataRow>("/admin/triage-desk/close", { triageRecordId }),
  devices: (params: { keyword?: string; departmentId?: number; category?: string; status?: string } = {}) => get<DataRow[]>(`/admin/device/list${query(params)}`),
  saveDevice: (body: DeviceSaveRequest) => post<DataRow>("/admin/device/save", body),
  updateDeviceStatus: (body: DeviceStatusRequest) => post<DataRow>("/admin/device/status", body),
  deviceUsages: (deviceId?: number) => get<DataRow[]>(`/admin/device/usage/list${query({ deviceId })}`),
  saveDeviceUsage: (body: DeviceUsageSaveRequest) => post<DataRow>("/admin/device/usage/save", body),
  patients: (params: { keyword?: string; gender?: string; minAge?: number; maxAge?: number } = {}) => get<DataRow[]>(`/admin/patient/list${query(params)}`),
  patientDetail: (id: number) => get<DataRow>(`/admin/patient/detail${query({ id })}`),
  savePatient: (body: PatientSaveRequest) => post<DataRow>("/admin/patient/save", body),
  statisticsOverview: () => get<DataRow>("/admin/statistics/overview"),
  statisticsTrend: (params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/trend${query(params)}`),
  doctorWorkload: (params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/doctor-workload${query(params)}`),
  patientDistribution: () => get<DataRow>("/admin/statistics/patient-distribution"),
  deviceUsageStatistics: () => get<DataRow[]>("/admin/statistics/device-usage"),
  statisticsReport: (params: StatisticsQuery = {}) => get<DataRow[]>(`/admin/statistics/report${query(params)}`),
  permissions: () => get<DataRow>("/admin/permission/list"),
  myPermissions: () => get<string[]>("/admin/permission/my"),
  saveRolePermissions: (body: RolePermissionSaveRequest) => post<DataRow>("/admin/permission/save-role", body),
  emailConfig: () => get<EmailConfig>("/admin/system/email-config"),
  saveEmailConfig: (body: EmailConfigSaveRequest) => post<EmailConfig>("/admin/system/email-config", body),
  testEmailConfig: (body: EmailConfigTestRequest) => post<DataRow>("/admin/system/email-config/test", body),
  patientSiteConfig: getAdminPatientSiteConfig,
  savePatientSiteConfig: (body: PatientSiteConfigSaveRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/save", body),
  savePublishedPatientSiteConfig: (body: PatientSiteConfigSaveRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/save-published", body),
  publishPatientSiteConfig: (body: PatientSiteConfigPublishRequest) => post<PatientSiteConfigRecord>("/admin/patient-site/publish", body),
  assetUploadPolicy: (body: AssetUploadPolicyRequest) => post<AssetUploadPolicyResponse>("/admin/assets/upload-policy", body),
  patientSiteConfigHistory: (configKey: string, page = 1, pageSize = 10) =>
    get<PatientSiteConfigHistoryPage>(`/admin/patient-site/history${query({ configKey, page, pageSize })}`),
  patientSitePreviewToken: (configKey: string, version?: number) =>
    post<PatientSitePreviewToken>(`/admin/patient-site/preview-token${query({ configKey, version })}`, undefined),
  patientSiteSitePreviewToken: () => post<PatientSitePreviewToken>("/admin/patient-site/site-preview-token", undefined),
  patientSiteNotices: () => get<PatientNotice[]>("/admin/patient-site/notices"),
  savePatientSiteNotice: (body: PatientNoticeSaveRequest) => post<PatientNotice>("/admin/patient-site/notices/save", body),
  deletePatientSiteNotice: (id: number) => post<PatientNotice>("/admin/patient-site/notices/delete", { id }),
  updatePatientSiteNoticeStatus: (body: PatientNoticeStatusRequest) => post<PatientNotice>("/admin/patient-site/notices/status", body),
  sortPatientSiteNotices: (body: PatientNoticeSortRequest) => post<PatientNotice[]>("/admin/patient-site/notices/sort", body),
  patientSiteRecommendations: (type: PatientRecommendationType) => get<PatientRecommendation[]>(`/admin/patient-site/recommendations${query({ type })}`),
  savePatientSiteRecommendation: (body: PatientRecommendationSaveRequest) => post<PatientRecommendation>("/admin/patient-site/recommendations/save", body),
  deletePatientSiteRecommendation: (id: number) => post<PatientRecommendation>("/admin/patient-site/recommendations/delete", { id }),
  updatePatientSiteRecommendationStatus: (body: PatientRecommendationStatusRequest) => post<PatientRecommendation>("/admin/patient-site/recommendations/status", body),
  sortPatientSiteRecommendations: (body: PatientRecommendationSortRequest) => post<PatientRecommendation[]>("/admin/patient-site/recommendations/sort", body),
};

export const api = {
  registerPatient: authApi.registerPatient, sendPatientEmailCode: authApi.sendPatientEmailCode, currentUser: authApi.currentUser,
  loginPatient: authApi.loginPatient, loginDoctor: authApi.loginDoctor, loginAdmin: authApi.loginAdmin,
  patientInfo: patientApi.info, departments: patientApi.departments, doctors: patientApi.doctors,
  doctorDetail: patientApi.doctorDetail, drugs: adminApi.drugs, doctorDrugs: doctorApi.drugs, triage: patientApi.triage,
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
  saveDoctor: adminApi.saveDoctor, saveDrug: adminApi.saveDrug, aiLogs: adminApi.aiLogs,
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
  emailConfig: adminApi.emailConfig, saveEmailConfig: adminApi.saveEmailConfig, testEmailConfig: adminApi.testEmailConfig,
  myPermissions: adminApi.myPermissions,
  patientSiteConfig: patientApi.siteConfig,
  patientSiteHomeConfig: patientApi.homeConfig,
  patientSitePreviewConfig: patientApi.sitePreviewConfig,
  patientSiteNotices: patientApi.notices,
  patientSiteRecommendations: patientApi.recommendations,
  adminPatientSiteConfig: adminApi.patientSiteConfig,
  savePatientSiteConfig: adminApi.savePatientSiteConfig,
  savePublishedPatientSiteConfig: adminApi.savePublishedPatientSiteConfig,
  publishPatientSiteConfig: adminApi.publishPatientSiteConfig,
  patientSiteConfigHistory: adminApi.patientSiteConfigHistory,
  patientSitePreviewToken: adminApi.patientSitePreviewToken,
  patientSiteSitePreviewToken: adminApi.patientSiteSitePreviewToken,
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
