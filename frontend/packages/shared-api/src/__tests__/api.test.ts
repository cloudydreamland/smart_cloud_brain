import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "../index";

describe("shared api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns login session data", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: true,
      json: async () => ({
        code: 0,
        message: "success",
        data: { token: "jwt.patient.token", userId: 1, role: "PATIENT", name: "测试患者" },
      }),
    })));

    await expect(api.loginPatient("13800000001", "123456")).resolves.toMatchObject({
      token: "jwt.patient.token",
      role: "PATIENT",
    });
  });

  it("throws backend business message", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: true,
      json: async () => ({ code: 401, message: "unauthorized", data: null }),
    })));

    await expect(api.loginPatient("bad", "bad")).rejects.toThrow("unauthorized");
  });

  it("loads current user from auth context", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({
      ok: true,
      json: async () => ({ code: 0, message: "success", data: { userId: 3, role: "ADMIN", name: "管理员" } }),
    })));

    await expect(api.currentUser("jwt.admin.token")).resolves.toMatchObject({
      userId: 3,
      role: "ADMIN",
    });
  });

  it("loads prescription detail by id", async () => {
    const fetch = vi.fn(async () => ({
      ok: true,
      json: async () => ({
        code: 0,
        message: "success",
        data: { prescriptionId: 11, items: [{ drugName: "aspirin" }] },
      }),
    }));
    vi.stubGlobal("fetch", fetch);

    await expect(api.prescriptionDetail("jwt.patient.token", 11)).resolves.toMatchObject({
      prescriptionId: 11,
    });
    expect(String(fetch.mock.calls[0][0])).toContain("/prescription/detail?id=11");
  });
});
