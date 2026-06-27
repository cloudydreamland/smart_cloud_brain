import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  api,
  fieldText,
  formatApiError,
  gatewayBase,
  medicalRecordStreamUrl,
  normalizePatientSitePagesConfig,
  notificationWebSocketUrl,
  patientSiteSectionTypes,
  request,
  statusClass,
  statusText,
  toNumber,
  validatePatientSitePagesConfig,
} from "../index";

function jsonResponse(data: unknown = {}) {
  return {
    ok: true,
    status: 200,
    statusText: "OK",
    json: async () => ({ code: 0, message: "success", data }),
  };
}

describe("shared api business logic", () => {
  afterEach(() => vi.restoreAllMocks());

  it("builds every public API request with compatible arguments", async () => {
    const fetch = vi.fn(async () => jsonResponse([]));
    vi.stubGlobal("fetch", fetch);
    const token = "jwt";
    await Promise.all([
      api.registerPatient({ name: "患者", phone: "13800000001", password: "123456" }),
      api.currentUser(token), api.loginPatient("p", "x"), api.loginDoctor("d", "x"), api.loginAdmin("a", "x"),
      api.patientInfo(token), api.departments(), api.doctors(1), api.doctorDetail(1), api.drugs(token),
      api.triage(token, "胸痛"), api.triageList(token),
      api.createRegistration(token, { doctorId: 1, departmentId: 1, appointmentTime: "2026-06-21T09:00:00" }),
      api.registrationSlots(token), api.registrations(token), api.cancelRegistration(token, 1), api.completeRegistration(token, 1),
      api.generateMedicalRecord(token, { registrationId: 1, dialogueText: "胸痛" }),
      api.saveMedicalRecord(token, { registrationId: 1, chiefComplaint: "胸痛", diagnosis: "待查" }),
      api.medicalRecords(token), api.medicalRecordDetail(token, 1),
      api.checkPrescription(token, { drugs: [] }),
      api.createPrescription(token, { patientId: 1, medicalRecordId: 1, drugs: [] }),
      api.prescriptions(token), api.prescriptionDetail(token, 1), api.notifications(token), api.markNotificationRead(token, 1),
      api.saveDepartment(token, { code: "C", name: "科室" }), api.adminDepartments(token),
      api.saveDoctor(token, { name: "医生", phone: "1", departmentId: 1 }),
      api.saveDrug(token, { name: "药品" }), api.prompts(token),
      api.savePrompt(token, { taskType: "SCHEDULE", templateName: "t", templateContent: "c" }),
      api.knowledgeEntries(token), api.saveKnowledgeEntry(token, { title: "t", symptoms: "s", advice: "a" }),
      api.dicts(token), api.saveDict(token, { dictType: "t", dictKey: "k", dictValue: "v" }),
      api.generateSchedule(token, { startDate: "2026-06-21", days: 2 }), api.publishSchedule(token, { suggestionIds: [1] }),
      api.schedules(token), api.scheduleSuggestionDetail(token, 1), api.triageDesk(token), api.triageDetail(token, 1),
      api.assignTriage(token, { triageRecordId: 1, doctorId: 2 }), api.closeTriage(token, 1),
      api.searchKnowledge(token, "胸痛", "CARDIOLOGY"), api.searchDrugs(token, "阿司匹林"), api.searchPrompts(token, "排班"),
    ]);
    expect(fetch.mock.calls.length).toBeGreaterThan(40);
    expect(String(fetch.mock.calls.find(([url]) => String(url).includes("departmentId=1"))?.[0])).toContain("departmentId=1");
  });

  it("formats values, states and localized errors", () => {
    expect(toNumber("12")).toBe(12);
    expect(toNumber("bad", 7)).toBe(7);
    expect(fieldText({ name: "Alice" }, "name")).toBe("Alice");
    expect(fieldText({}, "name", "none")).toBe("none");
    expect(statusClass("PUBLISHED")).toBe("success");
    expect(statusClass("HIGH")).toBe("danger");
    expect(statusClass("DRAFT")).toBe("warning");
    expect(statusClass("OTHER")).toBe("info");
    expect(statusText("PATIENT")).toBe("患者");
    expect(statusText("custom")).toBe("custom");
    expect(statusText("", "none")).toBe("none");
    expect(formatApiError(new ApiError("triage-service unavailable", 500, 500), "fallback")).toContain("分诊服务");
    expect(formatApiError(new Error("doctor-service unavailable"), "fallback")).toContain("医生与号源");
    expect(formatApiError(new Error("AI service unavailable"), "fallback")).toContain("智能服务");
    expect(formatApiError(new Error("plain message"), "fallback")).toBe("plain message");
    expect(formatApiError(new Error(""), "fallback")).toBe("请求失败");
    expect(formatApiError("unknown", "fallback")).toBe("fallback");
  });

  it("builds gateway, websocket and SSE URLs", () => {
    vi.stubGlobal("location", { protocol: "http:", host: "localhost:5173" });
    expect(gatewayBase()).toBe("http://localhost:5173");
    expect(notificationWebSocketUrl("a b")).toContain("token=a%20b");
    expect(medicalRecordStreamUrl(3, "胸痛", "CARDIOLOGY")).toContain("departmentCode=CARDIOLOGY");
  });

  it("reports network, invalid JSON and empty response errors", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new Error("offline"); }));
    await expect(request("/x")).rejects.toThrow("无法连接到网关");

    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: false, status: 502, statusText: "Bad Gateway", text: async () => "not-json",
    })));
    await expect(request("/x")).rejects.toThrow("数据格式无法解析");

    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: false, status: 503, statusText: "Unavailable", text: async () => "",
    })));
    await expect(request("/x")).rejects.toThrow("Unavailable");
  });

  it("normalizes and validates patient site page sections through the registry", () => {
    expect(patientSiteSectionTypes).toEqual([
      "notice",
      "rich_text",
      "card_grid",
      "faq",
      "timeline",
      "cta",
      "link_grid",
      "department_links",
    ]);

    const config = normalizePatientSitePagesConfig({
      pages: [
        {
          routeName: "about-hospital",
          slug: "hospital-intro",
          label: "关于智慧云脑",
          title: "医院介绍",
          intro: "简介",
          sections: [
            { id: "b", type: "rich_text", sort: 20, body: "正文" },
            { id: "a", type: "notice", sort: 10, level: "warning", text: "提醒" },
            { id: "bad", type: "unknown", text: "bad" },
            { id: "links", type: "link_grid", links: [{ label: "联系", routeName: "about-contact" }, { label: "专题", routeName: "cms-page", slug: "hospital-intro" }, { label: "Bad", routeName: "bad-route" }] },
          ],
        },
      ],
    });

    expect(config.pages).toHaveLength(1);
    expect(config.pages[0].sections.map((section) => section.type)).toEqual(["notice", "rich_text", "link_grid"]);
    const linkGrid = config.pages[0].sections.find((section) => section.type === "link_grid");
    expect(linkGrid?.type === "link_grid" && linkGrid.links).toHaveLength(2);
    expect(linkGrid?.type === "link_grid" && linkGrid.links[1].slug).toBe("hospital-intro");
    expect(validatePatientSitePagesConfig(config)).toEqual([]);
  });

  it("returns validation errors for incomplete patient site pages", () => {
    const config = normalizePatientSitePagesConfig({
      pages: [
        {
          routeName: "bad-route",
          label: "",
          title: "Bad",
          sections: [
            { type: "notice", text: "" },
            { type: "card_grid", cards: [] },
            { type: "link_grid", links: [{ label: "CMS", routeName: "cms-page" }] },
          ],
        },
      ],
    });

    const errors = validatePatientSitePagesConfig(config);
    expect(errors.some((item) => item.includes("routeName"))).toBe(true);
    expect(errors.some((item) => item.includes("label"))).toBe(true);
    expect(errors.some((item) => item.includes("text"))).toBe(true);
    expect(errors.some((item) => item.includes("cards"))).toBe(true);
    expect(errors.some((item) => item.includes("slug"))).toBe(true);
  });
});
