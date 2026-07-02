// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import router from "./index";

describe("admin routes", () => {
  it("sends the removed statistics path to the not-found route", () => {
    const resolved = router.resolve("/statistics");

    expect(resolved.name).toBe("not-found");
  });

  it("requires patient-site manage permission for the patient site editor", () => {
    const resolved = router.resolve("/patient-site");

    expect(resolved.name).toBe("admin-patient-site");
    expect(resolved.meta.permission).toBe("patient-site:manage");
  });
});
