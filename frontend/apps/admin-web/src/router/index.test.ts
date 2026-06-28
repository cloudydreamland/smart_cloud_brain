// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import router from "./index";

describe("admin routes", () => {
  it("sends the removed statistics path to the not-found route", () => {
    const resolved = router.resolve("/statistics");

    expect(resolved.name).toBe("not-found");
  });
});
