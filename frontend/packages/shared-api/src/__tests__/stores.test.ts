import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import {
  useAdminWorkflowStore,
  useAuthStore,
  useDoctorWorkflowStore,
  usePatientWorkflowStore,
} from "../index";

const storage = new Map<string, string>();

function ok(data: unknown) {
  return {
    ok: true,
    status: 200,
    statusText: "OK",
    json: async () => ({ code: 0, message: "success", data }),
  };
}

function routeData(url: string) {
  if (url.includes("/patient/info")) return { id: 1, name: "患者" };
  if (url.includes("/doctor/department/list")) return [{ id: 1, code: "CARDIOLOGY" }];
  if (url.includes("/doctor/list")) return [{ id: 2, name: "医生" }];
  if (url.includes("/triage/list")) return [{ triageRecordId: 3, status: "AI_RECOMMENDED" }];
  if (url.includes("/registration/slots")) return [{ id: 4 }];
  if (url.includes("/registration/list")) return [{ id: 5 }];
  if (url.includes("/medical-record/list")) return [{ id: 6 }];
  if (url.includes("/prescription/list")) return [{ id: 7 }];
  if (url.includes("/search/drugs")) return [{ id: 8 }];
  if (url.includes("/notification/list")) return [{ id: 9 }];
  if (url.includes("/admin/department/list")) return [{ id: 10 }];
  if (url.includes("/admin/drug/list")) return [{ id: 11 }];
  if (url.includes("/admin/prompt-template/list")) return [{ id: 12 }];
  if (url.includes("/admin/knowledge/list")) return [{ id: 13 }];
  if (url.includes("/admin/dict/list")) return [{ id: 14 }];
  if (url.includes("/admin/schedule/list")) return [{ id: 15 }];
  if (url.includes("/admin/triage-desk/list")) return [{ id: 16 }];
  return {};
}

describe("workflow stores", () => {
  beforeEach(() => {
    storage.clear();
    setActivePinia(createPinia());
    vi.stubGlobal("localStorage", {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
    });
    vi.stubGlobal("fetch", vi.fn(async (input: string | URL | Request) => ok(routeData(String(input)))));
  });

  it("manages auth persistence, roles and unauthorized events", () => {
    const eventTarget = new EventTarget();
    vi.stubGlobal("window", eventTarget);
    const store = useAuthStore();

    store.load("session", "PATIENT");
    expect(store.session).toBeNull();
    expect(store.token()).toBe("");
    expect(store.requireRole("PATIENT")).toBe(false);
    storage.set("other", JSON.stringify({ token: "stored", userId: 2, role: "ADMIN", name: "管理员" }));
    store.load("other", "PATIENT");
    expect(store.permissionError).toContain("ADMIN");
    store.save("session", { token: "jwt", userId: 1, role: "DOCTOR", name: "医生" }, "PATIENT");
    expect(store.token()).toBe("jwt");
    expect(store.permissionError).toContain("DOCTOR");
    expect(store.requireRole("PATIENT")).toBe(false);
    expect(store.requireRole("DOCTOR")).toBe(true);
    store.save("session", { token: "jwt", userId: 1, role: "DOCTOR", name: "医生" }, "DOCTOR");
    expect(store.permissionError).toBe("");
    const unbind = store.bindUnauthorized();
    eventTarget.dispatchEvent(new Event("smart-cloud-brain:unauthorized"));
    expect(store.session).toBeNull();
    unbind();
    vi.stubGlobal("window", undefined);
    expect(store.bindUnauthorized()()).toBeUndefined();
  });

  it("refreshes patient public and authenticated workflow data", async () => {
    const store = usePatientWorkflowStore();
    await store.refreshPublicData();
    await store.refreshAuthenticated();

    expect(store.departments).toHaveLength(1);
    expect(store.patient?.name).toBe("患者");
    expect(store.triage?.status).toBe("AI_RECOMMENDED");
    expect(store.prescriptions).toHaveLength(1);

    vi.stubGlobal("fetch", vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      return ok(url.includes("/triage/list")
        ? [{ triageRecordId: 2, status: "OLDER" }, { triageRecordId: 9, status: "LATEST" }]
        : routeData(url));
    }));
    await store.refreshAuthenticated();
    expect(store.triage?.status).toBe("LATEST");
    expect(store.triageHistory.map((item) => item.triageRecordId)).toEqual([9, 2]);

    store.triage = { id: 99, status: "PRESERVED" };
    vi.stubGlobal("fetch", vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      return ok(url.includes("/triage/list") ? [] : routeData(url));
    }));
    await store.refreshAuthenticated();
    expect(store.triage?.status).toBe("PRESERVED");
  });

  it("refreshes doctor workflow data", async () => {
    const store = useDoctorWorkflowStore();
    await store.refresh();

    expect(store.registrations).toHaveLength(1);
    expect(store.drugs).toHaveLength(1);
    expect(store.notifications).toHaveLength(1);
  });

  it("refreshes admin data and preserves a list when one request fails", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: string | URL | Request) => {
      const url = String(input);
      if (url.includes("/admin/prompt-template/list")) {
        return {
          ok: true,
          status: 200,
          statusText: "OK",
          json: async () => ({ code: 500, message: "prompt failed", data: null }),
        };
      }
      return ok(routeData(url));
    }));
    const store = useAdminWorkflowStore();
    store.prompts = [{ id: "kept" }];
    await store.refresh();

    expect(store.departments).toHaveLength(1);
    expect(store.prompts[0].id).toBe("kept");
    expect(store.refreshErrors.prompts).toBe("prompt failed");
    expect(store.triageDesk).toHaveLength(1);
  });
});
