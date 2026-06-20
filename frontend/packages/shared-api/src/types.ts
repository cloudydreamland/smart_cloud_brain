export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export type ApiResult<T> = { code: number; message: string; data: T };
export type Session = { token: string; userId: number; role: Role; name: string };
export type CurrentUser = { userId: number; role: Role; name: string };
export type DataRow = Record<string, unknown>;

export type PatientRegisterRequest = {
  name: string; phone: string; password: string; gender?: string; age?: number;
  allergyHistory?: string; pastHistory?: string;
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
export type SystemDictSaveRequest = {
  id?: number; dictType: string; dictKey: string; dictValue: string; sort?: number; status?: string;
};
export type ScheduleGenerateRequest = { startDate?: string; days?: number };
export type SchedulePublishRequest = { suggestionIds?: number[] };
export type TriageAssignRequest = { triageRecordId: number; doctorId: number };

export class ApiError extends Error {
  constructor(public message: string, public code: number, public status: number) {
    super(message);
    this.name = "ApiError";
  }
}
