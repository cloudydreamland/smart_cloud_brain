import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import type { PatientSitePagesConfig, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import PagesConfigEditor from "./PagesConfigEditor.vue";

const routeOptions = [
  { name: "about-hospital", label: "关于医院" },
  { name: "cms-page", label: "CMS 页面" },
];
const sectionTypeOptions: { type: PatientSiteSectionType; label: string }[] = [
  { type: "notice", label: "通知提示" },
  { type: "faq", label: "常见问题" },
];

type PagesConfigEditorProps = InstanceType<typeof PagesConfigEditor>["$props"];

function mountEditor(overrides: Partial<PagesConfigEditorProps> = {}) {
  const pagesDraft: PatientSitePagesConfig = {
    pages: [
      {
        routeName: "about-hospital",
        slug: "hospital-intro",
        label: "医院介绍",
        title: "医院介绍",
        intro: "",
        enabled: true,
        sort: 10,
        sections: [
          { id: "notice-1", type: "notice", enabled: true, sort: 10, level: "info", text: "提示" },
        ],
      },
    ],
  };
  const props = {
    pagesDraft,
    patientRouteOptions: routeOptions,
    sectionTypeOptions,
    toggleEnabled: vi.fn(),
    addCmsPage: vi.fn(),
    removeCmsPage: vi.fn(),
    previewCmsPage: vi.fn(),
    addPageSection: vi.fn(),
    removePageSection: vi.fn(),
    reorderCmsPage: vi.fn(),
    reorderPageSection: vi.fn(),
    ...overrides,
  };
  return { pagesDraft, props, wrapper: mount(PagesConfigEditor, { props }) };
}

describe("PagesConfigEditor", () => {
  it("requests adding a selected CMS section type", async () => {
    const { props, wrapper } = mountEditor();

    await wrapper.find(".section-add-buttons button").trigger("click");

    expect(props.addPageSection).toHaveBeenCalledWith(0, "notice");
  });

  it("requests section deletion and reordering", async () => {
    const { props, wrapper } = mountEditor();

    await wrapper.find(".page-section-card .danger-link").trigger("click");
    await wrapper.find(".page-section-card").trigger("dragstart");
    await wrapper.find(".page-section-card").trigger("drop");

    expect(props.removePageSection).toHaveBeenCalledWith(0, 0);
    expect(props.reorderPageSection).toHaveBeenCalledWith(0, 0, 0);
  });
});
