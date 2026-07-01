import { describe, expect, it } from "vitest";
import { patientRouteWhitelist } from "@smart-cloud-brain/shared-api";
import router from "./index";

const nonConfigurableRoutes = new Set(["patient-login", "patient-register", "not-found"]);

describe("patient routes", () => {
  it("keeps configurable patient route names in the low-code whitelist", () => {
    const missing = router
      .getRoutes()
      .map((route) => String(route.name || ""))
      .filter((name) => name && !nonConfigurableRoutes.has(name) && !patientRouteWhitelist.has(name));

    expect(missing).toEqual([]);
  });
});
