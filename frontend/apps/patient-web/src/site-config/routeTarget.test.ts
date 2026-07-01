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

  it("keeps preview token while preserving route query parameters", () => {
    expect(toPatientRoute({ label: "Search", routeName: "public-search", query: { q: "doctor" } }, "preview-token")).toEqual({
      name: "public-search",
      query: { q: "doctor", previewToken: "preview-token" },
    });
    expect(toPatientRoute({ label: "CMS", routeName: "cms-page", slug: "hospital-guide" }, "preview-token")).toEqual({
      name: "cms-page",
      params: { slug: "hospital-guide" },
      query: { previewToken: "preview-token" },
    });
  });

  it("accepts ref-like preview token sources from Vue templates", () => {
    expect(toPatientRoute({ label: "Home", routeName: "patient-home" }, { value: "template-preview" })).toEqual({
      name: "patient-home",
      query: { previewToken: "template-preview" },
    });
  });
});
