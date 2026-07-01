// @vitest-environment jsdom
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick, ref } from "vue";
import { normalizeConfig } from "../site-config/usePatientSiteConfig";
import CmsPage from "./CmsPage.vue";

const mocks = vi.hoisted(() => ({
  route: { params: { slug: "draft-guide" }, query: {} as Record<string, string> },
  load: vi.fn(),
  loadPreview: vi.fn(),
  activePreviewToken: { value: "" },
  config: { value: {} },
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
      loading: ref(false),
      activePreviewToken: mocks.activePreviewToken,
      load: mocks.load,
      loadPreview: mocks.loadPreview,
    }),
  };
});

function flushPromises() {
  return new Promise<void>((resolve) => setTimeout(resolve, 0));
}

describe("CmsPage", () => {
  beforeEach(() => {
    mocks.route = { params: { slug: "draft-guide" }, query: {} };
    mocks.load.mockReset();
    mocks.loadPreview.mockReset();
    mocks.activePreviewToken.value = "";
    mocks.config.value = normalizeConfig({});
  });

  it("renders disabled preview pages and sections when previewToken is present", async () => {
    mocks.route = { params: { slug: "draft-guide" }, query: { previewToken: "site-preview" } };
    mocks.config.value = normalizeConfig({
      pages: {
        pages: [
          {
            routeName: "about-hospital",
            slug: "draft-guide",
            label: "Draft Guide",
            title: "Draft Guide",
            intro: "Preview intro",
            enabled: false,
            sections: [
              { id: "draft-notice", type: "notice", enabled: false, sort: 10, level: "info", text: "Hidden until preview" },
            ],
          },
        ],
      },
    });

    const wrapper = mount(CmsPage, {
      global: { stubs: { RouterLink: { props: ["to"], template: "<a><slot /></a>" } } },
    });
    await flushPromises();
    await nextTick();

    expect(mocks.loadPreview).toHaveBeenCalledWith("site-preview");
    expect(mocks.load).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("PREVIEW");
    expect(wrapper.text()).toContain("Draft Guide");
    expect(wrapper.text()).toContain("Hidden until preview");
  });
});
