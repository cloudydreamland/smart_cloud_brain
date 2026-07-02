import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  patientSiteConfigTemplates,
  type PatientSiteAdminConfigMap,
  type PatientSiteConfigKey,
  type PatientSiteConfigRecord,
} from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfigEditor } from "./usePatientSiteConfigEditor";

const mocks = vi.hoisted(() => ({
  patientSiteConfig: vi.fn(),
  patientSiteConfigHistory: vi.fn(),
  savePatientSiteConfig: vi.fn(),
  savePublishedPatientSiteConfig: vi.fn(),
  publishPatientSiteConfig: vi.fn(),
  patientSiteSitePreviewToken: vi.fn(),
}));

vi.mock("@smart-cloud-brain/shared-api", async () => {
  const actual = await vi.importActual<typeof import("@smart-cloud-brain/shared-api")>("@smart-cloud-brain/shared-api");
  return {
    ...actual,
    adminApi: {
      ...actual.adminApi,
      patientSiteConfig: mocks.patientSiteConfig,
    },
    api: {
      ...actual.api,
      patientSiteConfigHistory: mocks.patientSiteConfigHistory,
      savePatientSiteConfig: mocks.savePatientSiteConfig,
      savePublishedPatientSiteConfig: mocks.savePublishedPatientSiteConfig,
      publishPatientSiteConfig: mocks.publishPatientSiteConfig,
      patientSiteSitePreviewToken: mocks.patientSiteSitePreviewToken,
    },
    useAuthStore: () => ({
      session: { token: "admin-token", role: "ADMIN", username: "admin" },
    }),
  };
});

function record(configKey: PatientSiteConfigKey, status: PatientSiteConfigRecord["status"], config: unknown): PatientSiteConfigRecord {
  return {
    id: status === "DRAFT" ? 2 : 1,
    configKey,
    configJson: JSON.stringify(config),
    status,
    version: status === "DRAFT" ? 2 : 1,
    remark: `${configKey} ${status}`,
    createdAt: "2026-07-01T00:00:00",
  };
}

function overview(): PatientSiteAdminConfigMap {
  return {
    patient_nav: record("patient_nav", "DRAFT", {
      ...patientSiteConfigTemplates.patient_nav,
      brand: { ...patientSiteConfigTemplates.patient_nav.brand, name: "Draft Nav" },
    }),
    patient_home: record("patient_home", "PUBLISHED", patientSiteConfigTemplates.patient_home),
    patient_static_pages: record("patient_static_pages", "PUBLISHED", patientSiteConfigTemplates.patient_static_pages),
    patient_pages: record("patient_pages", "PUBLISHED", patientSiteConfigTemplates.patient_pages),
    patient_hospital_info: record("patient_hospital_info", "PUBLISHED", patientSiteConfigTemplates.patient_hospital_info),
    patient_footer: record("patient_footer", "PUBLISHED", patientSiteConfigTemplates.patient_footer),
  };
}

function flushPromises() {
  return new Promise<void>((resolve) => setTimeout(resolve, 0));
}

describe("usePatientSiteConfigEditor", () => {
  beforeEach(() => {
    mocks.patientSiteConfig.mockReset();
    mocks.patientSiteConfigHistory.mockReset();
    mocks.savePatientSiteConfig.mockReset();
    mocks.savePublishedPatientSiteConfig.mockReset();
    mocks.publishPatientSiteConfig.mockReset();
    mocks.patientSiteSitePreviewToken.mockReset();
    mocks.patientSiteConfig.mockResolvedValue(overview());
    mocks.savePatientSiteConfig.mockImplementation(({ configKey, configJson }: { configKey: PatientSiteConfigKey; configJson: string }) =>
      Promise.resolve(record(configKey, "DRAFT", JSON.parse(configJson) as unknown)),
    );
    mocks.patientSiteSitePreviewToken.mockResolvedValue({ token: "site-preview-token", expiresAt: "2026-07-01T00:15:00", scope: "site" });
    mocks.patientSiteConfigHistory.mockImplementation((configKey: PatientSiteConfigKey, page = 1, pageSize = 10) =>
      Promise.resolve({
        items: [record(configKey, "PUBLISHED", patientSiteConfigTemplates[configKey])],
        page,
        pageSize,
        total: 1,
        totalPages: 1,
      }),
    );
  });

  it("loads the admin overview once and uses draft records as the editing source", async () => {
    const editor = usePatientSiteConfigEditor();

    await editor.loadAll();

    expect(mocks.patientSiteConfig).toHaveBeenCalledTimes(1);
    expect(mocks.patientSiteConfig).toHaveBeenCalledWith();
    expect(mocks.patientSiteConfigHistory).toHaveBeenCalledTimes(1);
    expect(mocks.patientSiteConfigHistory).toHaveBeenCalledWith("patient_nav", 1, 10);
    expect(editor.activeEditingSource.value).toBe("DRAFT");
    expect(editor.navDraft.value.brand.name).toBe("Draft Nav");
  });

  it("lazy-loads tab history without reloading the overview", async () => {
    const editor = usePatientSiteConfigEditor();

    await editor.loadAll();
    editor.switchTab("patient_pages");
    await flushPromises();

    expect(mocks.patientSiteConfig).toHaveBeenCalledTimes(1);
    expect(mocks.patientSiteConfigHistory).toHaveBeenCalledTimes(2);
    expect(mocks.patientSiteConfigHistory).toHaveBeenLastCalledWith("patient_pages", 1, 10);

    editor.switchTab("patient_nav");
    await flushPromises();

    expect(mocks.patientSiteConfig).toHaveBeenCalledTimes(1);
    expect(mocks.patientSiteConfigHistory).toHaveBeenCalledTimes(2);
  });

  it("reorders patient home modules and resequences their sort values", async () => {
    const editor = usePatientSiteConfigEditor();

    await editor.loadAll();
    const firstKey = editor.homeDraft.value.modules[0].key;
    const secondKey = editor.homeDraft.value.modules[1].key;

    editor.reorderHomeModule(1, 0);

    expect(editor.homeDraft.value.modules[0].key).toBe(secondKey);
    expect(editor.homeDraft.value.modules[1].key).toBe(firstKey);
    expect(editor.homeDraft.value.modules.slice(0, 2).map((module) => module.sort)).toEqual([10, 20]);
  });

  it("keeps low-code delete targets unchanged when the confirmation is cancelled", async () => {
    const confirm = vi.fn(async () => false);
    const editor = usePatientSiteConfigEditor({ confirm });

    await editor.loadAll();
    const homeModule = editor.homeDraft.value.modules[0];
    const staticPage = editor.staticDraft.value.pages[0];
    editor.addCmsPage();
    const cmsPageIndex = editor.pagesDraft.value.pages.length - 1;
    editor.addPageSection(cmsPageIndex, "notice");
    const cmsPage = editor.pagesDraft.value.pages[cmsPageIndex];
    const cmsSection = cmsPage.sections[0];

    await editor.removeHomeModule(homeModule);
    await editor.removeStaticPage(0);
    await editor.removeCmsPage(cmsPageIndex);
    await editor.removePageSection(cmsPageIndex, 0);

    expect(confirm).toHaveBeenCalledTimes(4);
    expect(homeModule.enabled).not.toBe(false);
    expect(staticPage.enabled).not.toBe(false);
    expect(cmsPage.enabled).not.toBe(false);
    expect(cmsSection.enabled).not.toBe(false);

  });

  it("saves all config drafts before opening a site preview session", async () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    const editor = usePatientSiteConfigEditor();

    await editor.loadAll();
    await editor.previewSite();

    expect(mocks.savePatientSiteConfig).toHaveBeenCalledTimes(6);
    expect(mocks.savePatientSiteConfig.mock.calls.map((call) => call[0].configKey)).toEqual([
      "patient_nav",
      "patient_home",
      "patient_static_pages",
      "patient_pages",
      "patient_hospital_info",
      "patient_footer",
    ]);
    expect(mocks.patientSiteSitePreviewToken).toHaveBeenCalledTimes(1);
    expect(open).toHaveBeenCalledWith(expect.stringContaining("previewToken=site-preview-token"), "_blank", "noopener");

    open.mockRestore();
  });

  it("does not publish current edits when the confirmation is cancelled", async () => {
    const confirm = vi.fn(async () => false);
    const editor = usePatientSiteConfigEditor({ confirm });

    await editor.loadAll();
    await editor.saveAndApply();

    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({ title: "确认保存并生效" }));
    expect(mocks.savePublishedPatientSiteConfig).not.toHaveBeenCalled();
  });

  it("does not publish a draft when the confirmation is cancelled", async () => {
    const confirm = vi.fn(async () => false);
    const editor = usePatientSiteConfigEditor({ confirm });

    await editor.loadAll();
    await editor.publishDraft();

    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({
      title: "确认发布最新草稿",
      message: expect.stringContaining("变更摘要"),
    }));
    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({
      message: expect.stringContaining("$.brand.name"),
    }));
    expect(mocks.publishPatientSiteConfig).not.toHaveBeenCalled();
  });
});
