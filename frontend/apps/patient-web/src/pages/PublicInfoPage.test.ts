// @vitest-environment jsdom
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { normalizeConfig } from "../site-config/usePatientSiteConfig";
import PublicInfoPage from "./PublicInfoPage.vue";

const mocks = vi.hoisted(() => ({
  route: { name: "about-hospital" },
  config: { value: {} },
  activePreviewToken: { value: "" },
}));

vi.mock("vue-router", async () => {
  const actual = await vi.importActual<typeof import("vue-router")>("vue-router");
  return {
    ...actual,
    useRoute: () => mocks.route,
  };
});

vi.mock("../site-config/usePatientSiteConfig", async () => {
  const actual = await vi.importActual<typeof import("../site-config/usePatientSiteConfig")>("../site-config/usePatientSiteConfig");
  return {
    ...actual,
    usePatientSiteConfig: () => ({
      config: mocks.config,
      activePreviewToken: mocks.activePreviewToken,
    }),
  };
});

describe("PublicInfoPage", () => {
  beforeEach(() => {
    mocks.route = { name: "about-hospital" };
    mocks.config.value = normalizeConfig({});
    mocks.activePreviewToken.value = "";
  });

  it("renders the CMS page bound to a fixed public route", async () => {
    mocks.config.value = normalizeConfig({
      staticPages: {
        pages: [
          {
            routeName: "about-hospital",
            label: "医院介绍",
            title: "医院介绍",
            intro: "旧静态介绍",
            contentSource: "cms-page",
            slug: "hospital-intro",
            enabled: true,
            points: [],
          },
        ],
      },
      pages: {
        pages: [
          {
            routeName: "about-hospital",
            slug: "hospital-intro",
            label: "CMS 医院",
            title: "CMS 医院介绍",
            intro: "来自 CMS 的介绍",
            enabled: true,
            sections: [
              { id: "cms-faq", type: "faq", enabled: true, sort: 10, title: "入院常见问题", items: [{ question: "如何预约？", answer: "通过患者端预约。" }] },
              { id: "off", type: "notice", enabled: false, sort: 20, level: "info", text: "禁用内容" },
            ],
          },
        ],
      },
    });

    const wrapper = mount(PublicInfoPage, {
      global: { stubs: { RouterLink: { name: "RouterLink", props: ["to"], template: "<a><slot /></a>" } } },
    });
    await nextTick();

    expect(wrapper.text()).toContain("CMS 医院介绍");
    expect(wrapper.text()).toContain("来自 CMS 的介绍");
    expect(wrapper.text()).toContain("入院常见问题");
    expect(wrapper.text()).toContain("如何预约？");
    expect(wrapper.text()).not.toContain("禁用内容");
  });

  it("shows disabled bound CMS pages and sections while previewing", async () => {
    mocks.activePreviewToken.value = "preview-token";
    mocks.config.value = normalizeConfig({
      staticPages: {
        pages: [
          {
            routeName: "about-hospital",
            label: "医院介绍",
            title: "医院介绍",
            intro: "旧静态介绍",
            contentSource: "cms-page",
            slug: "draft-hospital",
            enabled: false,
            points: [],
          },
        ],
      },
      pages: {
        pages: [
          {
            routeName: "about-hospital",
            slug: "draft-hospital",
            label: "CMS 草稿",
            title: "CMS 草稿医院介绍",
            intro: "预览草稿介绍",
            enabled: false,
            sections: [
              { id: "draft-notice", type: "notice", enabled: false, sort: 10, level: "info", text: "预览可见禁用区块" },
            ],
          },
        ],
      },
    }, { preserveDisabled: true });

    const wrapper = mount(PublicInfoPage, {
      global: { stubs: { RouterLink: { name: "RouterLink", props: ["to"], template: "<a><slot /></a>" } } },
    });
    await nextTick();

    expect(wrapper.text()).toContain("CMS 草稿医院介绍");
    expect(wrapper.text()).toContain("预览草稿介绍");
    expect(wrapper.text()).toContain("预览可见禁用区块");
  });

  it("keeps preview token on fallback public page links", async () => {
    mocks.route = { name: "about-contact" };
    mocks.activePreviewToken.value = "preview-token";

    const wrapper = mount(PublicInfoPage, {
      global: { stubs: { RouterLink: { name: "RouterLink", props: ["to"], template: "<a><slot /></a>" } } },
    });
    await nextTick();
    const routerLinks = wrapper.findAllComponents({ name: "RouterLink" });

    expect(routerLinks.some((link) => JSON.stringify(link.props("to")).includes('"previewToken":"preview-token"'))).toBe(true);
  });
});
