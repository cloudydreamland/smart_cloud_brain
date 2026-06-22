import { createPinia, setActivePinia } from "pinia";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  api,
  fieldText,
  formatApiError,
  gatewayBase,
  medicalRecordStreamUrl,
  notificationWebSocketUrl,
  statusClass,
  statusText,
  toNumber,
  useAdminWorkflowStore,
  useAuthStore,
  useDoctorWorkflowStore,
  usePatientWorkflowStore,
} from "../index";

const ok = (data: unknown) => new Response(JSON.stringify({ code: 0, message: "success", data }), { status: 200 });
const fail = (code: number, message: string, status = 200) =>
  new Response(JSON.stringify({ code, message, data: null }), { status });

describe("shared api request handling", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("sends json requests with authorization and query parameters", async () => {
    const fetch = vi.fn(async () => ok([{ id: 1 }]));
    vi.stubGlobal("fetch", fetch);

    await expect(api.searchKnowledge("jwt", "cold", "CARD")).resolves.toEqual([{ id: 1 }]);

    expect(String(fetch.mock.calls[0][0])).toContain("/search/knowledge?q=cold&departmentCode=CARD");
    const init = fetch.mock.calls[0][1] as RequestInit;
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer jwt");
  });

  it("covers patient doctor and admin api wrapper routes", async () => {
    const fetch = vi.fn(async () => ok([]));
    vi.stubGlobal("fetch", fetch);

    await Promise.all([
      api.registerPatient({ name: "Alice", phone: "13800000001", password: "123456" }),
      api.loginDoctor("doctor1", "123456"),
      api.loginAdmin("admin", "123456"),
      api.patientInfo("jwt"),
      api.departments(),
      api.doctors(1),
      api.doctorDetail(1),
      api.triage("jwt", "headache"),
      api.createRegistration("jwt", { doctorId: 1, departmentId: 2, appointmentTime: "2026-01-01T09:00:00" }),
      api.cancelRegistration("jwt", 1),
      api.completeRegistration("jwt", 1),
      api.generateMedicalRecord("jwt", { registrationId: 1, dialogueText: "dialogue" }),
      api.saveMedicalRecord("jwt", { registrationId: 1, chiefComplaint: "pain", diagnosis: "cold" }),
      api.medicalRecordDetail("jwt", 1),
      api.checkPrescription("jwt", { drugs: [] }),
      api.createPrescription("jwt", { patientId: 1, medicalRecordId: 2, drugs: [] }),
      api.notifications("jwt", "UNREAD"),
      api.markNotificationRead("jwt", 1),
      api.accounts("jwt"),
      api.roles("jwt"),
      api.saveAccount("jwt", { role: "ADMIN", account: "admin2", name: "管理员", password: "123456" }),
      api.adminDepartments("jwt"),
      api.saveDepartment("jwt", { code: "CARD", name: "Cardiology" }),
      api.saveDoctor("jwt", { name: "doctor", phone: "13900000001", departmentId: 1 }),
      api.drugs("jwt"),
      api.saveDrug("jwt", { name: "aspirin" }),
      api.prompts("jwt"),
      api.savePrompt("jwt", { taskType: "TRIAGE", templateName: "default", templateContent: "content" }),
      api.testPrompt("jwt", { taskType: "TRIAGE", templateContent: "content", outputSchema: "{\"type\":\"object\"}" }),
      api.knowledgeEntries("jwt"),
      api.saveKnowledgeEntry("jwt", { title: "cold", symptoms: "cough", advice: "rest" }),
      api.dicts("jwt", "gender"),
      api.saveDict("jwt", { dictType: "gender", dictKey: "MALE", dictValue: "male" }),
      api.generateSchedule("jwt", { days: 3 }),
      api.publishSchedule("jwt", { suggestionIds: [1] }),
      api.schedules("jwt"),
      api.scheduleSuggestionDetail("jwt", 1),
      api.triageDesk("jwt"),
      api.triageDetail("jwt", 1),
      api.assignTriage("jwt", { triageRecordId: 1, doctorId: 2 }),
      api.closeTriage("jwt", 1),
      api.searchDrugs("jwt", "aspirin"),
      api.searchPrompts("jwt", "triage"),
    ]);

    expect(fetch.mock.calls.length).toBeGreaterThan(30);
    expect(fetch.mock.calls.map((call) => String(call[0])).join("\n")).toContain("/admin/schedule/publish");
    expect(fetch.mock.calls.map((call) => String(call[0])).join("\n")).toContain("/admin/prompt-template/test");
    expect(fetch.mock.calls.map((call) => String(call[0])).join("\n")).toContain("/admin/account/list");
  });

  it("throws business, http, network, empty-body and invalid-json errors", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => fail(409, "conflict")));
    await expect(api.loginPatient("bad", "bad")).rejects.toMatchObject({ code: 409 });

    vi.stubGlobal("fetch", vi.fn(async () => fail(500, "server", 500)));
    await expect(api.currentUser("jwt")).rejects.toMatchObject({ status: 500 });

    vi.stubGlobal("fetch", vi.fn(async () => { throw new Error("offline"); }));
    await expect(api.currentUser("jwt")).rejects.toBeInstanceOf(ApiError);

    vi.stubGlobal("fetch", vi.fn(async () => new Response("", { status: 200 })));
    await expect(api.currentUser("jwt")).resolves.toBeUndefined();

    vi.stubGlobal("fetch", vi.fn(async () => new Response("not json", { status: 200 })));
    await expect(api.currentUser("jwt")).rejects.toBeInstanceOf(ApiError);
  });

  it("dispatches unauthorized event for 401 responses", async () => {
    const listener = vi.fn();
    window.addEventListener("smart-cloud-brain:unauthorized", listener);
    vi.stubGlobal("fetch", vi.fn(async () => fail(401, "unauthorized", 401)));

    await expect(api.currentUser("expired")).rejects.toMatchObject({ code: 401 });
    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener("smart-cloud-brain:unauthorized", listener);
  });
});

describe("shared api utilities and stores", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("formats numbers, fields, statuses and gateway urls", () => {
    expect(toNumber("42")).toBe(42);
    expect(toNumber("bad", 7)).toBe(7);
    expect(fieldText({ name: "" }, "name", "fallback")).toBe("fallback");
    expect(fieldText({ name: "Alice" }, "name")).toBe("Alice");
    expect(statusClass("COMPLETED")).toBe("success");
    expect(statusClass("HIGH")).toBe("danger");
    expect(statusClass("PENDING")).toBe("warning");
    expect(statusText("UNKNOWN", "fallback")).not.toBe("fallback");
    expect(formatApiError(new ApiError("custom", 400, 400), "fallback")).toBe("custom");
    expect(gatewayBase()).toContain(window.location.host);
    expect(notificationWebSocketUrl("a b")).toContain("token=a%20b");
    expect(medicalRecordStreamUrl(3, "hello world", "CARD")).toContain("departmentCode=CARD");
  });

  it("persists auth session and clears it on unauthorized event", () => {
    const auth = useAuthStore();
    auth.save("patient-session", { token: "jwt", userId: 1, role: "PATIENT", name: "Alice" }, "PATIENT");

    expect(auth.isAuthenticated).toBe(true);
    expect(auth.token()).toBe("jwt");
    expect(auth.requireRole("PATIENT")).toBe(true);
    expect(auth.requireRole("ADMIN")).toBe(false);

    const unbind = auth.bindUnauthorized();
    window.dispatchEvent(new CustomEvent("smart-cloud-brain:unauthorized"));
    expect(auth.isAuthenticated).toBe(false);
    unbind();
  });

  it("refreshes patient and doctor workflow stores from backend data", async () => {
    vi.stubGlobal("fetch", vi.fn(async (url: string) => {
      if (url.includes("/patient/info")) return ok({ id: 1, name: "Alice" });
      if (url.includes("/triage/list")) return ok([{ triageRecordId: 1 }]);
      if (url.includes("/registration/slots")) return ok([{ slotId: 1 }]);
      if (url.includes("/registration/list")) return ok([{ registrationId: 1 }]);
      if (url.includes("/medical-record/list")) return ok([{ medicalRecordId: 1 }]);
      if (url.includes("/prescription/list")) return ok([{ prescriptionId: 1 }]);
      if (url.includes("/search/drugs")) return ok([{ drugName: "aspirin" }]);
      if (url.includes("/notification/list")) return ok([{ notificationId: 1 }]);
      return ok([]);
    }));

    const patient = usePatientWorkflowStore();
    await patient.refreshAuthenticated("jwt");
    expect(patient.patient?.name).toBe("Alice");
    expect(patient.triage?.triageRecordId).toBe(1);

    const doctor = useDoctorWorkflowStore();
    await doctor.refresh("jwt");
    expect(doctor.registrations).toHaveLength(1);
    expect(doctor.notifications).toHaveLength(1);
  });

  it("keeps admin cached data when one refresh branch fails", async () => {
    vi.stubGlobal("fetch", vi.fn(async (url: string) => {
      if (url.includes("/admin/drug/list")) return fail(500, "drug-service unavailable");
      return ok([{ id: 1 }]);
    }));
    const admin = useAdminWorkflowStore();
    admin.drugs = [{ id: 99 }];

    await admin.refresh("jwt");

    expect(admin.departments).toEqual([{ id: 1 }]);
    expect(admin.drugs).toEqual([{ id: 99 }]);
    expect(admin.refreshErrors.drugs).toBeTruthy();
  });
});
