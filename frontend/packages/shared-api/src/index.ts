export type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type Session = {
  token: string;
  userId: number;
  role: "PATIENT" | "DOCTOR" | "ADMIN";
  name: string;
};

const API_BASE = import.meta.env?.VITE_API_BASE ?? "/api";

async function request<T>(path: string, options: RequestInit = {}, token = ""): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers ?? {}),
    },
  });
  const payload = (await response.json()) as ApiResult<T>;
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

export const api = {
  registerPatient: (body: unknown) => request<{ patientId: number }>("/patient/register", { method: "POST", body: JSON.stringify(body) }),
  loginPatient: (account: string, password: string) => request<Session>("/patient/login", { method: "POST", body: JSON.stringify({ account, password }) }),
  loginDoctor: (account: string, password: string) => request<Session>("/doctor/login", { method: "POST", body: JSON.stringify({ account, password }) }),
  loginAdmin: (account: string, password: string) => request<Session>("/admin/login", { method: "POST", body: JSON.stringify({ account, password }) }),
  patientInfo: (token: string) => request<Record<string, unknown>>("/patient/info", {}, token),
  departments: () => request<Array<Record<string, unknown>>>("/admin/department/list"),
  doctors: (departmentId = "") => request<Array<Record<string, unknown>>>(`/doctor/list${departmentId ? `?departmentId=${departmentId}` : ""}`),
  drugs: () => request<Array<Record<string, unknown>>>("/admin/drug/list"),
  triage: (token: string, chiefComplaint: string) => request<Record<string, unknown>>("/triage/consult", { method: "POST", body: JSON.stringify({ chiefComplaint }) }, token),
  triageList: (token: string) => request<Array<Record<string, unknown>>>("/triage/list", {}, token),
  createRegistration: (token: string, body: unknown) => request<Record<string, unknown>>("/registration/create", { method: "POST", body: JSON.stringify(body) }, token),
  registrations: (token: string) => request<Array<Record<string, unknown>>>("/registration/list", {}, token),
  cancelRegistration: (token: string, registrationId: number) => request<Record<string, unknown>>("/registration/cancel", { method: "POST", body: JSON.stringify({ registrationId }) }, token),
  generateMedicalRecord: (token: string, body: unknown) => request<Record<string, unknown>>("/medical-record/generate", { method: "POST", body: JSON.stringify(body) }, token),
  saveMedicalRecord: (token: string, body: unknown) => request<Record<string, unknown>>("/medical-record/save", { method: "POST", body: JSON.stringify(body) }, token),
  medicalRecords: (token: string) => request<Array<Record<string, unknown>>>("/medical-record/list", {}, token),
  checkPrescription: (token: string, body: unknown) => request<Record<string, unknown>>("/prescription/check", { method: "POST", body: JSON.stringify(body) }, token),
  createPrescription: (token: string, body: unknown) => request<Record<string, unknown>>("/prescription/create", { method: "POST", body: JSON.stringify(body) }, token),
  prescriptions: (token: string) => request<Array<Record<string, unknown>>>("/prescription/list", {}, token),
  notifications: (token: string) => request<Array<Record<string, unknown>>>("/notification/list", {}, token),
  saveDepartment: (body: unknown) => request<Record<string, unknown>>("/admin/department/save", { method: "POST", body: JSON.stringify(body) }),
  saveDoctor: (body: unknown) => request<Record<string, unknown>>("/admin/doctor/save", { method: "POST", body: JSON.stringify(body) }),
  saveDrug: (body: unknown) => request<Record<string, unknown>>("/admin/drug/save", { method: "POST", body: JSON.stringify(body) }),
  prompts: () => request<Array<Record<string, unknown>>>("/admin/prompt-template/list"),
  savePrompt: (body: unknown) => request<Record<string, unknown>>("/admin/prompt-template/save", { method: "POST", body: JSON.stringify(body) }),
  knowledgeEntries: () => request<Array<Record<string, unknown>>>("/admin/knowledge/list"),
  saveKnowledgeEntry: (body: unknown) => request<Record<string, unknown>>("/admin/knowledge/save", { method: "POST", body: JSON.stringify(body) }),
};
