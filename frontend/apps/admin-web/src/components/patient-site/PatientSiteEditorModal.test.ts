import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { patientSiteConfirmKey, type PatientSiteConfirm } from "../../composables/patientSiteConfirm";
import { homeModuleTypeOptions, type EditingTarget } from "../../composables/usePatientSiteConfigEditor";
import { emptyPatientSiteEditorDraft, type PatientSiteEditorDraft } from "../../composables/patientSiteEditorDraftTypes";
import PatientSiteEditorModal from "./PatientSiteEditorModal.vue";

function draft(): PatientSiteEditorDraft {
  return {
    ...emptyPatientSiteEditorDraft(),
    key: "services",
    label: "服务菜单",
    links: [{ label: "预约挂号", routeName: "patient-doctors", enabled: true, sort: 10 }],
  };
}

function mountModal(
  editingDraft = draft(),
  editingTarget: EditingTarget = { type: "nav-menu", index: 0 },
  confirm: PatientSiteConfirm = async () => true,
) {
  return mount(PatientSiteEditorModal, {
    props: {
      title: "编辑菜单",
      description: "编辑导航菜单",
      editingTarget,
      editingDraft,
      patientRouteOptions: [{ name: "patient-doctors", label: "预约挂号" }, { name: "cms-page", label: "CMS 页面" }],
      homeModuleTypeOptions,
      closeEditor: vi.fn(),
      applyEditor: vi.fn(),
      hydrateEditingHomeModuleContent: vi.fn(),
      editingContentItems: () => editingDraft.content.items,
      addEditingLink: vi.fn(),
      addEditingQuickAction: vi.fn(),
      addEditingPoint: vi.fn(),
      addEditingLocationItem: vi.fn(),
      addEditingDepartmentLink: vi.fn(),
      addEditingFallbackName: vi.fn(),
    },
    global: {
      provide: {
        [patientSiteConfirmKey as symbol]: confirm,
      },
      stubs: {
        ScbSelect: true,
        RouteTargetEditor: { props: ["model", "prefix", "patientRouteOptions", "includeSort", "includeEnabled"], template: "<div />" },
        OssImageUploadField: true,
        PageSectionFieldsEditor: true,
      },
    },
  });
}

describe("PatientSiteEditorModal", () => {
  it("keeps a menu link when delete confirmation is cancelled", async () => {
    const confirm = vi.fn(async () => false);
    const editingDraft = draft();
    const wrapper = mountModal(editingDraft, { type: "nav-menu", index: 0 }, confirm);

    await wrapper.find("button.danger-link").trigger("click");

    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({ title: "确认删除链接「预约挂号」" }));
    expect(editingDraft.links[0].enabled).toBe(true);
  });

  it("disables a menu link after delete confirmation", async () => {
    const confirm = vi.fn(async () => true);
    const editingDraft = draft();
    const wrapper = mountModal(editingDraft, { type: "nav-menu", index: 0 }, confirm);

    await wrapper.find("button.danger-link").trigger("click");

    expect(editingDraft.links[0].enabled).toBe(false);
  });

  it("exposes CMS binding slug input for static public pages", async () => {
    const editingDraft: PatientSiteEditorDraft = {
      ...emptyPatientSiteEditorDraft(),
      routeName: "about-hospital",
      label: "医院介绍",
      title: "医院介绍",
      intro: "",
      contentSource: "cms-page",
      slug: "",
      enabled: true,
      points: [],
    };
    const wrapper = mountModal(editingDraft, { type: "static-page", index: 0 });

    const slugInput = wrapper.find('input[placeholder="例如：hospital-intro"]');
    expect(slugInput.exists()).toBe(true);
    await slugInput.setValue("hospital-intro");

    expect(editingDraft.slug).toBe("hospital-intro");
  });
});
