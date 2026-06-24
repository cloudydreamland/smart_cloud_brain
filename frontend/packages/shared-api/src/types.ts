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
export type KnowledgeEntrySaveRequest = {
  id?: number; title: string; symptoms: string; riskSignals?: string; advice: string;
  departmentCode?: string; status?: string;
};
export type PromptTemplateSaveRequest = {
  id?: number; taskType: string; departmentCode?: string; templateName: string;
  templateContent: string; outputSchema?: string; version?: string; enabled?: boolean;
};
export type PromptTestRequest = PromptTemplateSaveRequest & { sampleInput?: string };
export type SystemDictSaveRequest = {
  id?: number; dictType: string; dictKey: string; dictValue: string; sort?: number; status?: string;
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
export type PatientSiteConfigPublishRequest = { configKey: string };

export class ApiError extends Error {
  constructor(public message: string, public code: number, public status: number) {
    super(message);
    this.name = "ApiError";
  }
}

/* ======================================================================
 * 实体接口（基于后端 API 返回字段定义，供后续逐步迁移使用）
 * 当前代码仍用 DataRow + fieldText，定义这些接口是为了：
 *   1. 文档化每个实体有哪些字段
 *   2. 后续迁移时有具体类型可替换 DataRow
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

export interface KnowledgeEntry {
  id: number;
  title: string;
  symptoms: string;
  riskSignals?: string;
  advice: string;
  departmentCode?: string;
  status?: string;
}

export interface PromptTemplate {
  id: number;
  taskType: string;
  departmentCode?: string;
  templateName: string;
  templateContent: string;
  outputSchema?: string;
  version?: string;
  enabled?: boolean;
}

export interface SystemDict {
  id: number;
  dictType: string;
  dictKey: string;
  dictValue: string;
  sort?: number;
  status?: string;
}

export interface Patient {
  id: number;
  name: string;
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
  status?: string;
  riskLevel?: string;
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
  urgencyLevel?: string;
  riskLevel?: string;
  status?: string;
}

export interface MedicalRecord {
  medicalRecordId: number;
  registrationId: number;
  patientId?: number;
  chiefComplaint: string;
  presentIllness?: string;
  pastHistory?: string;
  physicalExam?: string;
  diagnosis: string;
  treatmentAdvice?: string;
  aiGenerated?: boolean;
  provider?: string;
  model?: string;
}

export interface Prescription {
  prescriptionId: number;
  registrationId?: number;
  patientId?: number;
  medicalRecordId?: number;
  riskLevel?: string;
  riskDescription?: string;
  drugs?: DrugItem[];
  drugCount?: number;
  status?: string;
}

export interface Account {
  id: number;
  role: Role;
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

export interface Schedule {
  id: number;
  doctorId: number;
  doctorName?: string;
  departmentId: number;
  departmentName?: string;
  workDate: string;
  timeRange: string;
  capacity: number;
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
  remark?: string;
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
  title?: string;
  content?: string;
  riskLevel?: string;
  readStatus?: string;
  createdAt?: string;
}
