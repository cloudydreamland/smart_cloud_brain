import { defineStore } from "pinia";
import { ref } from "vue";

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

const post = (body: unknown) => ({ method: "POST", body: JSON.stringify(body) });

export const api = {
  registerPatient: (body: unknown) => request<{ patientId: number }>("/patient/register", post(body)),
  loginPatient: (account: string, password: string) => request<Session>("/patient/login", post({ account, password })),
  loginDoctor: (account: string, password: string) => request<Session>("/doctor/login", post({ account, password })),
  loginAdmin: (account: string, password: string) => request<Session>("/admin/login", post({ account, password })),
  patientInfo: (token: string) => request<Record<string, unknown>>("/patient/info", {}, token),
  departments: () => request<Array<Record<string, unknown>>>("/doctor/department/list"),
  adminDepartments: (token: string) => request<Array<Record<string, unknown>>>("/admin/department/list", {}, token),
  doctors: (departmentId = "") => request<Array<Record<string, unknown>>>(`/doctor/list${departmentId ? `?departmentId=${departmentId}` : ""}`),
  drugs: (token: string) => request<Array<Record<string, unknown>>>("/admin/drug/list", {}, token),
  triage: (token: string, chiefComplaint: string) => request<Record<string, unknown>>("/triage/consult", post({ chiefComplaint }), token),
  triageList: (token: string) => request<Array<Record<string, unknown>>>("/triage/list", {}, token),
  createRegistration: (token: string, body: unknown) => request<Record<string, unknown>>("/registration/create", post(body), token),
  registrationSlots: (token: string) => request<Array<Record<string, unknown>>>("/registration/slots", {}, token),
  registrations: (token: string) => request<Array<Record<string, unknown>>>("/registration/list", {}, token),
  cancelRegistration: (token: string, registrationId: number) => request<Record<string, unknown>>("/registration/cancel", post({ registrationId }), token),
  generateMedicalRecord: (token: string, body: unknown) => request<Record<string, unknown>>("/medical-record/generate", post(body), token),
  saveMedicalRecord: (token: string, body: unknown) => request<Record<string, unknown>>("/medical-record/save", post(body), token),
  medicalRecords: (token: string) => request<Array<Record<string, unknown>>>("/medical-record/list", {}, token),
  checkPrescription: (token: string, body: unknown) => request<Record<string, unknown>>("/prescription/check", post(body), token),
  createPrescription: (token: string, body: unknown) => request<Record<string, unknown>>("/prescription/create", post(body), token),
  prescriptions: (token: string) => request<Array<Record<string, unknown>>>("/prescription/list", {}, token),
  notifications: (token: string) => request<Array<Record<string, unknown>>>("/notification/list", {}, token),
  saveDepartment: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/department/save", post(body), token),
  saveDoctor: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/doctor/save", post(body), token),
  saveDrug: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/drug/save", post(body), token),
  prompts: (token: string) => request<Array<Record<string, unknown>>>("/admin/prompt-template/list", {}, token),
  savePrompt: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/prompt-template/save", post(body), token),
  knowledgeEntries: (token: string) => request<Array<Record<string, unknown>>>("/admin/knowledge/list", {}, token),
  saveKnowledgeEntry: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/knowledge/save", post(body), token),
  dicts: (token: string, dictType = "") => request<Array<Record<string, unknown>>>(`/admin/dict/list${dictType ? `?dictType=${encodeURIComponent(dictType)}` : ""}`, {}, token),
  saveDict: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/dict/save", post(body), token),
  generateSchedule: (token: string, body: unknown) => request<Array<Record<string, unknown>>>("/admin/schedule/generate", post(body), token),
  publishSchedule: (token: string, body: unknown) => request<Array<Record<string, unknown>>>("/admin/schedule/publish", post(body), token),
  schedules: (token: string) => request<Array<Record<string, unknown>>>("/admin/schedule/list", {}, token),
  scheduleSuggestionDetail: (token: string, id: number) => request<Record<string, unknown>>(`/admin/schedule/suggestion/detail?id=${id}`, {}, token),
  triageDesk: (token: string) => request<Array<Record<string, unknown>>>("/admin/triage-desk/list", {}, token),
  triageDetail: (token: string, id: number) => request<Record<string, unknown>>(`/admin/triage-desk/detail?id=${id}`, {}, token),
  assignTriage: (token: string, body: unknown) => request<Record<string, unknown>>("/admin/triage-desk/assign", post(body), token),
  closeTriage: (token: string, triageRecordId: number) => request<Record<string, unknown>>("/admin/triage-desk/close", post({ triageRecordId }), token),
  searchKnowledge: (token: string, q: string, departmentCode = "") =>
    request<Array<Record<string, unknown>>>(`/search/knowledge?q=${encodeURIComponent(q)}${departmentCode ? `&departmentCode=${encodeURIComponent(departmentCode)}` : ""}`, {}, token),
  searchDrugs: (token: string, q: string) =>
    request<Array<Record<string, unknown>>>(`/search/drugs?q=${encodeURIComponent(q)}`, {}, token),
  searchPrompts: (token: string, q: string) =>
    request<Array<Record<string, unknown>>>(`/admin/search/prompts?q=${encodeURIComponent(q)}`, {}, token),
};

function gatewayBase() {
  return API_BASE.startsWith("http")
    ? API_BASE.replace(/\/api$/, "")
    : `${location.protocol}//${location.host}`;
}

function websocketBase() {
  return API_BASE.startsWith("http")
    ? API_BASE.replace(/^http/, "ws").replace(/\/api$/, "")
    : `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}`;
}

export function notificationWebSocketUrl(doctorId: number, token = "") {
  const params = new URLSearchParams({ doctorId: String(doctorId) });
  if (token) params.set("token", token);
  return `${websocketBase()}/ws/notifications?${params.toString()}`;
}

export function medicalRecordStreamUrl(registrationId: number, token: string) {
  const params = new URLSearchParams({ registrationId: String(registrationId), token });
  return `${gatewayBase()}/api/medical-record/generate/stream?${params.toString()}`;
}

export const useAuthStore = defineStore("auth", () => {
  const session = ref<Session | null>(null);
  const storageKey = ref("smart-cloud-brain-session");

  function load(key: string) {
    storageKey.value = key;
    session.value = JSON.parse(localStorage.getItem(key) || "null");
  }

  function save(key: string, nextSession: Session) {
    storageKey.value = key;
    session.value = nextSession;
    localStorage.setItem(key, JSON.stringify(nextSession));
  }

  function logout() {
    session.value = null;
    localStorage.removeItem(storageKey.value);
  }

  function token() {
    return session.value?.token ?? "";
  }

  return { session, load, save, logout, token };
});

export const usePatientWorkflowStore = defineStore("patientWorkflow", () => {
  const departments = ref<Array<Record<string, unknown>>>([]);
  const triage = ref<Record<string, unknown> | null>(null);
  const slots = ref<Array<Record<string, unknown>>>([]);
  const registrations = ref<Array<Record<string, unknown>>>([]);
  const records = ref<Array<Record<string, unknown>>>([]);
  const prescriptions = ref<Array<Record<string, unknown>>>([]);

  async function refreshPublicData() {
    departments.value = await api.departments();
  }

  async function refreshAuthenticated(token: string) {
    slots.value = await api.registrationSlots(token);
    registrations.value = await api.registrations(token);
    records.value = await api.medicalRecords(token);
    prescriptions.value = await api.prescriptions(token);
  }

  return { departments, triage, slots, registrations, records, prescriptions, refreshPublicData, refreshAuthenticated };
});

export const useDoctorWorkflowStore = defineStore("doctorWorkflow", () => {
  const registrations = ref<Array<Record<string, unknown>>>([]);
  const records = ref<Array<Record<string, unknown>>>([]);
  const drugs = ref<Array<Record<string, unknown>>>([]);
  const notifications = ref<Array<Record<string, unknown>>>([]);
  const streamText = ref("");
  const streamStatus = ref("IDLE");

  async function refresh(token: string) {
    drugs.value = await api.searchDrugs(token, "");
    registrations.value = await api.registrations(token);
    records.value = await api.medicalRecords(token);
    notifications.value = await api.notifications(token);
  }

  return { registrations, records, drugs, notifications, streamText, streamStatus, refresh };
});

export const useAdminWorkflowStore = defineStore("adminWorkflow", () => {
  const departments = ref<Array<Record<string, unknown>>>([]);
  const doctors = ref<Array<Record<string, unknown>>>([]);
  const drugs = ref<Array<Record<string, unknown>>>([]);
  const prompts = ref<Array<Record<string, unknown>>>([]);
  const knowledge = ref<Array<Record<string, unknown>>>([]);
  const dicts = ref<Array<Record<string, unknown>>>([]);
  const suggestions = ref<Array<Record<string, unknown>>>([]);
  const schedules = ref<Array<Record<string, unknown>>>([]);
  const triageDesk = ref<Array<Record<string, unknown>>>([]);
  const selectedScheduleSuggestion = ref<Record<string, unknown> | null>(null);
  const selectedTriage = ref<Record<string, unknown> | null>(null);

  async function refresh(token: string) {
    departments.value = await api.adminDepartments(token);
    doctors.value = await api.doctors();
    drugs.value = await api.drugs(token);
    prompts.value = await api.prompts(token);
    knowledge.value = await api.knowledgeEntries(token);
    dicts.value = await api.dicts(token);
    schedules.value = await api.schedules(token);
    triageDesk.value = await api.triageDesk(token);
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
    refresh,
  };
});
