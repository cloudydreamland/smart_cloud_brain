export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export type ApiResult<T> = { code: number; message: string; data: T };
export type Session = { token: string; userId: number; role: Role; name: string };
export type CurrentUser = { userId: number; role: Role; name: string };
export type DataRow = Record<string, unknown>;

export type PatientRegisterRequest = {
  name: string; phone: string; password: string; gender?: string; age?: number;
};
export type TriageRequest = { patientId?: number; chiefComplaint: string };
export type RegistrationCreateRequest = {
  doctorId: number; departmentId: number; appointmentTime: string;
  triageRecordId?: number | null; slotId?: number | null;
};
export type MedicalRecordGenerateRequest = { registrationId: number; departmentCode?: string; dialogueText: string };
export type MedicalRecordSaveRequest = {
  registrationId: number; chiefComplaint: string; presentIllness?: string; pastHistory?: string;
  physicalExam?: string; diagnosis: string; treatmentAdvice?: string; aiGenerated?: boolean;
};
export type DrugItem = {
  drugName: string; dosage: string; frequency: string; usageMethod: string; days?: number; remark?: string;
};
export type PrescriptionCheckRequest = {
  patientId?: number; doctorId?: number; medicalRecordId?: number; diagnosis?: string;
  patientAge?: number; patientGender?: string; allergyHistory?: string; pastHistory?: string; drugs: DrugItem[];
};
export type PrescriptionCreateRequest = {
  patientId: number; medicalRecordId: number; registrationId?: number; riskLevel?: string; drugs: DrugItem[];
};
export type DepartmentSaveRequest = { id?: number; code: string; name: string; description?: string };
export type DoctorSaveRequest = {
  id?: number; name: string; phone: string; password?: string; departmentId: number;
  title?: string; specialty?: string; status?: string;
};
export type AccountSaveRequest = {
  id?: number; role: Role; account: string; name: string; password?: string;
  departmentId?: number; title?: string; specialty?: string; status?: string;
};
export type DrugSaveRequest = {
  id?: number; name: string; specification?: string; contraindication?: string;
  interactionRule?: string; status?: string;
};
export type ScheduleGenerateRequest = { startDate?: string; days?: number };
export type SchedulePublishRequest = { suggestionIds?: number[] };
export type TriageAssignRequest = { triageRecordId: number; doctorId: number };
export type DeviceSaveRequest = {
  id?: number; deviceCode: string; name: string; category?: string; departmentId?: number;
  location?: string; status?: string; purchaseDate?: string; remark?: string;
};
export type DeviceStatusRequest = { deviceId: number; status: string };
export type DeviceUsageSaveRequest = {
  id?: number; deviceId: number; usageType: string; usedBy?: string; patientId?: number;
  startedAt?: string; endedAt?: string; resultStatus?: string; remark?: string;
};
export type PatientSaveRequest = {
  id?: number; name: string; gender?: string; age?: number; allergyHistory?: string; pastHistory?: string;
  address?: string; emergencyContact?: string; emergencyPhone?: string; bloodType?: string;
  heightCm?: number; weightKg?: number;
};
export type PatientVisitorSaveRequest = {
  id?: number; name: string; relationship?: string; phone?: string; gender?: string; age?: number;
  address?: string; emergencyContact?: string; emergencyPhone?: string; bloodType?: string;
  heightCm?: number; weightKg?: number; allergyHistory?: string; pastHistory?: string;
};
export type ScheduleSaveRequest = {
  id?: number; doctorId: number; departmentId: number; workDate: string; timeRange: string; capacity: number; status?: string;
};
export type ScheduleCancelRequest = { scheduleId: number };
export type RolePermissionSaveRequest = { role: Role; permissionKeys: string[] };
export type StatisticsQuery = { startDate?: string; endDate?: string };
export type StatisticsReportRow = { metric: string; value: number | string };
export type PatientSiteConfigSaveRequest = { configKey: string; configJson: string; remark?: string };
export type PatientSiteConfigPublishRequest = { configKey: string; remark: string };
export type PatientNotice = {
  id?: number;
  title: string;
  content: string;
  linkType?: "NONE" | "INTERNAL" | "EXTERNAL" | string;
  linkUrl?: string;
  startTime?: string;
  endTime?: string;
  pinned?: boolean;
  sort?: number;
  status?: "ENABLED" | "DISABLED" | string;
  createdAt?: string;
  updatedAt?: string;
};
export type PatientNoticeSaveRequest = Omit<PatientNotice, "createdAt" | "updatedAt">;
export type PatientNoticeStatusRequest = { id: number; status: string };
export type PatientNoticeSortRequest = { items: { id: number; sort: number }[] };
export type PatientRecommendationType = "DEPARTMENT" | "DOCTOR";
export type PatientRecommendation = {
  id?: number;
  recommendType: PatientRecommendationType | string;
  targetId: number;
  title?: string;
  description?: string;
  imageUrl?: string;
  imageObjectKey?: string;
  sort?: number;
  status?: "ENABLED" | "DISABLED" | string;
  targetAvailable?: boolean;
  targetName?: string;
  departmentId?: number;
  departmentName?: string;
  doctorTitle?: string;
  specialty?: string;
  createdAt?: string;
  updatedAt?: string;
};
export type PatientRecommendationSaveRequest = Omit<
  PatientRecommendation,
  "targetAvailable" | "targetName" | "departmentId" | "departmentName" | "doctorTitle" | "specialty" | "createdAt" | "updatedAt"
>;
export type PatientRecommendationStatusRequest = { id: number; status: string };
export type PatientRecommendationSortRequest = { items: { id: number; sort: number }[] };
export type AssetUploadPolicyRequest = {
  scene: "patient-site";
  fileName: string;
  contentType: string;
  size: number;
};
export type AssetUploadPolicyResponse = {
  provider: string;
  bucket: string;
  objectKey: string;
  uploadMethod: "PUT" | "POST";
  uploadUrl: string;
  headers?: Record<string, string>;
  formData?: Record<string, string>;
  publicUrl: string;
  expiresAt: number;
};
export type PatientSiteConfigRecord = {
  id?: number;
  configKey?: string;
  configJson?: string;
  status?: string;
  version?: number;
  remark?: string;
  createdBy?: number;
  updatedBy?: number;
  createdAt?: string;
  updatedAt?: string;
};
export type PatientSiteConfigHistoryPage = {
  items: PatientSiteConfigRecord[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};
export type PatientSitePreviewToken = { token: string; configKey: string; version: number; expiresAt: number };

export class ApiError extends Error {
  constructor(public message: string, public code: number, public status: number) {
    super(message);
    this.name = "ApiError";
  }
}

/* ======================================================================
 * 实体接口（基于后端 API 返回字段定义，供后续逐步迁移使用）
 * DataRow 仍作为后端兼容层保留，定义这些接口是为了：
 *   1. 文档化每个实体有哪些字段
 *   2. 让 store、页面和组件层优先使用具体类型
 * ====================================================================== */

export interface Department {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export interface Doctor {
  id: number;
  name: string;
  phone: string;
  departmentId: number;
  departmentCode?: string;
  departmentName?: string;
  title?: string;
  specialty?: string;
  status?: string;
}

export interface Drug {
  id: number;
  name: string;
  specification?: string;
  contraindication?: string;
  interactionRule?: string;
  status?: string;
}

export interface Patient {
  id: number;
  name: string;
  visitorType?: string;
  relationship?: string;
  editable?: boolean;
  gender?: string;
  age?: number;
  phone?: string;
  allergyHistory?: string;
  pastHistory?: string;
  address?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  bloodType?: string;
  heightCm?: number;
  weightKg?: number;
  registrationCount?: number;
}

export interface Registration {
  registrationId: number;
  patientId: number;
  patientName?: string;
  doctorId?: number;
  doctorName?: string;
  departmentId?: number;
  departmentName?: string;
  appointmentTime?: string;
  createdAt?: string;
  status?: string;
  riskLevel?: string;
  chiefComplaint?: string;
  triageRecordId?: number;
  slotId?: number;
}

export interface TriageRecord {
  triageRecordId: number;
  patientId: number;
  patientName?: string;
  chiefComplaint?: string;
  departmentCode?: string;
  recommendedDepartment?: string;
  recommendedDoctorDirection?: string;
  recommendedDoctorIds?: number[] | string;
  assignedDoctorId?: number;
  assignedDoctorName?: string;
  urgencyLevel?: string;
  confidence?: number | string;
  riskLevel?: string;
  reason?: string;
  pastHistory?: string;
  degraded?: boolean;
  provider?: string;
  model?: string;
  status?: string;
}

export interface MedicalRecord {
  medicalRecordId: number;
  registrationId: number;
  patientId?: number;
  patientName?: string;
  doctorId?: number;
  chiefComplaint: string;
  presentIllness?: string;
  pastHistory?: string;
  physicalExam?: string;
  diagnosis: string;
  treatmentAdvice?: string;
  aiGenerated?: boolean;
  provider?: string;
  model?: string;
  createdAt?: string;
}

export interface Prescription {
  prescriptionId: number;
  registrationId?: number;
  patientId?: number;
  patientName?: string;
  doctorId?: number;
  medicalRecordId?: number;
  riskLevel?: string;
  riskDescription?: string;
  items?: DrugItem[];
  drugs?: DrugItem[];
  drugCount?: number;
  suggestions?: string[] | string;
  provider?: string;
  model?: string;
  status?: string;
  createdAt?: string;
}

export interface Account {
  id: number;
  role: Role;
  roleLabel?: string;
  account: string;
  name: string;
  departmentId?: number;
  departmentName?: string;
  title?: string;
  specialty?: string;
  status?: string;
  permissions?: string;
}

export interface RoleInfo {
  role: Role;
  label: string;
  permissions?: string;
}

/** 医生排班（/doctor/schedule/list） */
export interface Schedule {
  id: number;
  doctorId: number;
  doctorName?: string;
  departmentId: number;
  departmentName?: string;
  workDate: string;
  timeRange: string;
  capacity: number;
  booked?: number;
  remainingCapacity?: number;
  slotId?: number;
  status?: string;
}

/** 患者端可预约号源（/registration/slots） */
export interface AppointmentSlot {
  slotId: number;
  doctorId: number;
  doctorName?: string;
  departmentId: number;
  departmentName?: string;
  startTime: string;
  endTime: string;
  capacity: number;
  remainingCapacity?: number;
  status?: string;
}

export interface Device {
  id: number;
  deviceCode: string;
  name: string;
  category?: string;
  departmentId?: number;
  departmentName?: string;
  location?: string;
  status?: string;
  purchaseDate?: string;
  lastMaintenanceAt?: string;
  remark?: string;
  usageCount?: number;
  abnormalCount?: number;
}

export interface DeviceUsage {
  id: number;
  deviceId: number;
  usageType: string;
  usedBy?: string;
  patientId?: number;
  startedAt?: string;
  endedAt?: string;
  resultStatus?: string;
  remark?: string;
}

export interface Notification {
  notificationId: number;
  doctorId?: number;
  patientId?: number;
  prescriptionId?: number;
  triageRecordId?: number;
  medicalRecordId?: number;
  type?: string;
  title?: string;
  content?: string;
  riskLevel?: string;
  readStatus?: string;
  handleStatus?: string;
  createdAt?: string;
  handledAt?: string;
}

export interface ScheduleSuggestion extends Schedule {
  reason?: string;
  source?: string;
  degraded?: boolean;
}

export interface AiLog {
  id?: number;
  taskType?: string;
  provider?: string;
  model?: string;
  latencyMs?: number;
  status?: string;
  createdAt?: string;
  requestId?: string;
  errorMessage?: string;
}

export interface PermissionCatalogItem {
  key: string;
  label: string;
  description?: string;
}

export interface PermissionGrant {
  role: Role;
  permissionKey: string;
  enabled?: boolean;
}

export interface PermissionPayload {
  catalog: PermissionCatalogItem[];
  roles: Role[];
  grants: PermissionGrant[];
}

export interface StatisticsOverview {
  registrations?: number;
  completedRegistrations?: number;
  patients?: number;
  doctors?: number;
  devices?: number;
  deviceWarnings?: number;
}

export interface StatisticsTrendRow {
  day?: string;
  registrations?: number;
}

export interface DoctorWorkloadRow {
  doctor_id?: number;
  doctor_name?: string;
  department_name?: string;
  registrations?: number;
  completed?: number;
}

export interface DistributionItem {
  name?: string;
  value?: number;
}

export interface PatientDistribution {
  gender?: DistributionItem[];
  age?: DistributionItem[];
}

export interface DeviceUsageStatsRow {
  device_id?: number;
  name?: string;
  device_code?: string;
  category?: string;
  status?: string;
  usage_count?: number;
  abnormal_count?: number;
}

export interface PatientDetail extends Patient {
  registrations?: Record<string, unknown>[];
  triageRecords?: Record<string, unknown>[];
  medicalRecords?: Record<string, unknown>[];
  prescriptions?: Record<string, unknown>[];
}

export interface PrescriptionCheckResult {
  riskLevel?: string;
  riskDescription?: string;
  suggestions?: string[];
  provider?: string;
  model?: string;
  degraded?: boolean;
}

export interface MedicalRecordDraft {
  chiefComplaint?: string;
  presentIllness?: string;
  pastHistory?: string;
  physicalExam?: string;
  diagnosis?: string;
  treatmentAdvice?: string;
  provider?: string;
  model?: string;
  degraded?: boolean;
}
