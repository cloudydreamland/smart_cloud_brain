// @vitest-environment jsdom
import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import HomePage from "./HomePage.vue";

const mocks = vi.hoisted(() => ({
  routeQuery: {} as Record<string, string>,
  push: vi.fn(),
  patientSiteConfig: vi.fn(),
  patientSiteHomeConfig: vi.fn(),
  patientSitePreviewConfig: vi.fn(),
  departments: vi.fn(),
  doctors: vi.fn(),
}));

vi.mock("vue-router", async () => {
  const actual = await vi.importActual<typeof import("vue-router")>("vue-router");
  return {
    ...actual,
    useRoute: () => ({ query: mocks.routeQuery }),
    useRouter: () => ({ push: mocks.push }),
  };
});

vi.mock("@smart-cloud-brain/shared-api", async () => {
  const actual = await vi.importActual<typeof import("@smart-cloud-brain/shared-api")>("@smart-cloud-brain/shared-api");
  return {
    ...actual,
    api: {
      ...actual.api,
      patientSiteConfig: mocks.patientSiteConfig,
      patientSiteHomeConfig: mocks.patientSiteHomeConfig,
      patientSitePreviewConfig: mocks.patientSitePreviewConfig,
      departments: mocks.departments,
      doctors: mocks.doctors,
    },
  };
});

function flushPromises() {
  return new Promise<void>((resolve) => setTimeout(resolve, 0));
}

describe("HomePage", () => {
  beforeEach(() => {
    mocks.routeQuery = {};
    mocks.push.mockReset();
    mocks.patientSiteConfig.mockReset();
    mocks.patientSiteHomeConfig.mockReset();
    mocks.patientSitePreviewConfig.mockReset();
    mocks.departments.mockReset();
    mocks.doctors.mockReset();
    mocks.departments.mockResolvedValue([]);
    mocks.doctors.mockResolvedValue([]);
    mocks.patientSiteConfig.mockResolvedValue({});
    mocks.patientSiteHomeConfig.mockResolvedValue({});
    mocks.patientSitePreviewConfig.mockResolvedValue({});
  });

  it("renders low-code home section modules from preview config", async () => {
    mocks.routeQuery = { previewToken: "draft-preview-token" };
    mocks.patientSitePreviewConfig.mockResolvedValueOnce({
      home: {
        hero: { enabled: false, title: "" },
        modules: [
          {
            type: "hero",
            key: "draft-home-hero",
            enabled: true,
            sort: 10,
            content: {
              title: "Draft homepage campaign",
              eyebrow: "Preview",
              text: "Preview-only section text",
              primary: { label: "Book now", routeName: "patient-doctors" },
            },
          },
        ],
      },
    });

    const wrapper = mount(HomePage, {
      global: {
        stubs: {
          RouterLink: { props: ["to"], template: "<a><slot /></a>" },
        },
      },
    });
    await flushPromises();
    await nextTick();

    expect(mocks.patientSitePreviewConfig).toHaveBeenCalledWith("draft-preview-token");
    expect(mocks.patientSiteConfig).not.toHaveBeenCalled();
    expect(mocks.patientSiteHomeConfig).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("Draft homepage campaign");
    expect(wrapper.text()).toContain("Preview-only section text");
  });

  it("does not show recommended doctors unless the doctor_list module is enabled", async () => {
    mocks.patientSiteHomeConfig.mockResolvedValueOnce({
      home: { hero: { enabled: false, title: "" }, modules: [] },
      recommendedDoctors: [{ title: "未配置展示医生", description: "不应显示" }],
    });

    const wrapper = mount(HomePage, {
      global: { stubs: { RouterLink: { props: ["to"], template: "<a><slot /></a>" } } },
    });
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).not.toContain("未配置展示医生");
  });

  it("hides configured home modules when they are disabled", async () => {
    mocks.patientSiteHomeConfig.mockResolvedValueOnce({
      home: {
        hero: { enabled: false, title: "" },
        modules: [
          { type: "notice", key: "notice", enabled: false, sort: 10, content: { text: "禁用公告" } },
          { type: "featured_departments", key: "departments", enabled: false, sort: 20, content: { fallbackNames: ["禁用科室"] } },
          { type: "doctor_list", key: "doctors", enabled: false, sort: 30, content: { title: "禁用医生模块", fallbackNames: ["禁用医生"] } },
        ],
      },
      notices: [{ title: "后端公告", content: "不应显示" }],
      hotDepartments: [{ title: "后端科室", description: "不应显示" }],
      recommendedDoctors: [{ title: "后端医生", description: "不应显示" }],
    });

    const wrapper = mount(HomePage, {
      global: { stubs: { RouterLink: { props: ["to"], template: "<a><slot /></a>" } } },
    });
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).not.toContain("禁用公告");
    expect(wrapper.text()).not.toContain("后端公告");
    expect(wrapper.text()).not.toContain("禁用科室");
    expect(wrapper.text()).not.toContain("后端科室");
    expect(wrapper.text()).not.toContain("禁用医生模块");
    expect(wrapper.text()).not.toContain("后端医生");
  });

  it("shows recommended doctors through the doctor_list module limit", async () => {
    mocks.patientSiteHomeConfig.mockResolvedValueOnce({
      home: {
        hero: { enabled: false, title: "" },
        modules: [
          {
            type: "doctor_list",
            key: "recommended-doctors",
            enabled: true,
            sort: 10,
            content: { title: "本周推荐医生", limit: 1 },
          },
        ],
      },
      recommendedDoctors: [
        { title: "配置展示医生", description: "互联网医院" },
        { title: "超过数量医生", description: "不应显示" },
      ],
    });

    const wrapper = mount(HomePage, {
      global: { stubs: { RouterLink: { props: ["to"], template: "<a><slot /></a>" } } },
    });
    await flushPromises();
    await nextTick();

    expect(wrapper.text()).toContain("本周推荐医生");
    expect(wrapper.text()).toContain("配置展示医生");
    expect(wrapper.text()).not.toContain("超过数量医生");
  });
});
