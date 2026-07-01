// @vitest-environment jsdom
import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import PatientSiteFooter from "./PatientSiteFooter.vue";

const mocks = vi.hoisted(() => ({
  activePreviewToken: { value: "preview-token" },
  config: { value: {
    footer: {
      brandName: "智慧云脑",
      description: "患者端页脚",
      links: [{ label: "专题", routeName: "cms-page", slug: "hospital-guide", enabled: true }],
      legalLinks: [],
    },
  } },
}));

vi.mock("../site-config/usePatientSiteConfig", async () => {
  const actual = await vi.importActual<typeof import("../site-config/usePatientSiteConfig")>("../site-config/usePatientSiteConfig");
  return {
    ...actual,
    usePatientSiteConfig: () => ({
      config: mocks.config,
      activePreviewToken: mocks.activePreviewToken,
    }),
    getActivePatientSitePreviewToken: () => mocks.activePreviewToken.value,
  };
});

describe("PatientSiteFooter", () => {
  it("keeps preview token on footer brand and CMS links", () => {
    const wrapper = mount(PatientSiteFooter, {
      global: { stubs: { RouterLink: { name: "RouterLink", props: ["to"], template: "<a><slot /></a>" } } },
    });
    const links = wrapper.findAllComponents({ name: "RouterLink" });

    expect(links[0].props("to")).toEqual({ name: "patient-home", query: { previewToken: "preview-token" } });
    expect(links[1].props("to")).toEqual({
      name: "cms-page",
      params: { slug: "hospital-guide" },
      query: { previewToken: "preview-token" },
    });
  });
});
