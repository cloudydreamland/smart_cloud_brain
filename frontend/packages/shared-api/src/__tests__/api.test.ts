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
        data: { token: "mock-token-patient-1", userId: 1, role: "PATIENT", name: "测试患者" },
      }),
    })));

    await expect(api.loginPatient("13800000001", "123456")).resolves.toMatchObject({
      token: "mock-token-patient-1",
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
});
