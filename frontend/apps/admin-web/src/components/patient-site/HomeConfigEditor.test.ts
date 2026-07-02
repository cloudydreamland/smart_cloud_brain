import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import type { PatientHomeConfig, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import HomeConfigEditor from "./HomeConfigEditor.vue";

const homeDraft: PatientHomeConfig = {
  hero: { title: "首页", enabled: true },
  modules: [
    { type: "notice", key: "notice", enabled: true, sort: 10, content: { text: "公告" } },
    { type: "quick_actions", key: "quick", enabled: true, sort: 20, content: { items: [] } },
  ],
};

const baseProps = {
  homeDraft,
  sectionTypeOptions: [{ type: "hero" as PatientSiteSectionType, label: "首屏" }],
  moduleSummary: (module: { key?: string }) => module.key || "",
  toggleEnabled: vi.fn(),
  openEditor: vi.fn(),
  addNoticeModule: vi.fn(),
  addQuickActionsModule: vi.fn(),
  addIntroModule: vi.fn(),
  addLocationsModule: vi.fn(),
  addFeaturedDepartmentsModule: vi.fn(),
  addStaticContentModule: vi.fn(),
  addHomeSectionModule: vi.fn(),
  reorderHomeModule: vi.fn(),
  removeHomeModule: vi.fn(),
};

describe("HomeConfigEditor", () => {
  it("reorders homepage modules with drag and drop", async () => {
    const props = { ...baseProps, reorderHomeModule: vi.fn() };
    const wrapper = mount(HomeConfigEditor, { props });
    const modules = wrapper.findAll(".home-module-row");

    await modules[0].trigger("dragstart");
    await modules[1].trigger("drop");

    expect(props.reorderHomeModule).toHaveBeenCalledWith(0, 1);
  });
});
