import { describe, expect, it } from "vitest";
import { toPatientRoute } from "./routeTarget";

describe("toPatientRoute", () => {
  it("builds a named route for cms-page links with a slug", () => {
    expect(toPatientRoute({ label: "CMS", routeName: "cms-page", slug: "hospital-guide" })).toEqual({
      name: "cms-page",
      params: { slug: "hospital-guide" },
    });
  });

  it("falls back to home when cms-page slug is missing", () => {
    expect(toPatientRoute({ label: "CMS", routeName: "cms-page" })).toEqual({ name: "patient-home" });
  });

  it("keeps built-in route query parameters", () => {
    expect(toPatientRoute({ label: "Search", routeName: "public-search", query: { q: "doctor" } })).toEqual({
      name: "public-search",
      query: { q: "doctor" },
    });
  });
});
