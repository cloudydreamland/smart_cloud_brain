import {
  api,
  createDefaultPatientSiteSection,
  isPatientSiteSectionType,
  normalizePatientSiteSection,
  resolvePatientSiteConfigSection,
  type PatientHomeModule,
  type PatientSitePageConfig,
  type PatientSitePagesConfig,
  type PatientSiteSectionType,
  type PatientStaticPagesConfig,
  type PatientSiteConfigKey,
  type RouteTargetConfig,
} from "@smart-cloud-brain/shared-api";
import {
  clone,
  isRow,
  messageFrom,
  moveItem,
  nextSort,
  normalizeHome,
  normalizeHomeModule,
  normalizeLink,
  normalizeNav,
  normalizeNavMenu,
  normalizeStaticPage,
  numberValue,
  patientPreviewOrigin,
  resequenceSort,
  routeValue,
  stringValue,
} from "./patientSiteConfigEditorUtils";
import type { ConfigDrafts, EditingTarget } from "./patientSiteConfigEditorState";
import { emptyPatientSiteEditorDraft, type PatientSiteEditorDraft, type PatientSiteEditorDraftContent } from "./patientSiteEditorDraftTypes";
import type { PatientSiteConfirm } from "./patientSiteConfirm";

type ValueRef<T> = { value: T };
type EditableRow = Record<string, unknown>;

type DraftActionContext = {
  auth: { session: unknown };
  drafts: ConfigDrafts;
  remarks: Record<string, string>;
  saving: ValueRef<boolean>;
  status: ValueRef<string>;
  error: ValueRef<string>;
  editingDraft: ValueRef<PatientSiteEditorDraft | null>;
  navDraft: ValueRef<ConfigDrafts["patient_nav"]>;
  homeDraft: ValueRef<ConfigDrafts["patient_home"]>;
  staticDraft: ValueRef<PatientStaticPagesConfig>;
  pagesDraft: ValueRef<PatientSitePagesConfig>;
  validationErrors: Record<string, string[]>;
  confirm: PatientSiteConfirm;
  openEditor: (target: EditingTarget) => void;
  refreshHistory: (key: PatientSiteConfigKey) => Promise<void>;
};

const configKeys: PatientSiteConfigKey[] = [
  "patient_nav",
  "patient_home",
  "patient_static_pages",
  "patient_pages",
  "patient_hospital_info",
  "patient_footer",
];
const legacyHomeModuleTypes = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

export function createPatientSiteDraftActions(ctx: DraftActionContext) {
  function readEditingTarget(target: EditingTarget) {
    if (target.type === "brand") return ctx.navDraft.value.brand;
    if (target.type === "nav-menu") return ctx.navDraft.value.menus[target.index];
    if (target.type === "user-link") return ctx.navDraft.value.userLinks[target.index];
    if (target.type === "home-hero") return ctx.homeDraft.value.hero;
    if (target.type === "home-module") return ctx.homeDraft.value.modules[target.index];
    return ctx.staticDraft.value.pages[target.index];
  }

  function writeEditingTarget(target: EditingTarget, value: unknown) {
    if (target.type === "brand") ctx.navDraft.value.brand = normalizeNav({ brand: value }).brand;
    else if (target.type === "nav-menu") ctx.navDraft.value.menus[target.index] = normalizeNavMenu(value, target.index);
    else if (target.type === "user-link") ctx.navDraft.value.userLinks[target.index] = normalizeLink(value);
    else if (target.type === "home-hero") ctx.homeDraft.value.hero = normalizeHome({ hero: value }).hero;
    else if (target.type === "home-module") {
      const row = isRow(value) ? value : {};
      if (isHomeSectionType(stringValue(row.type, "")) && isRow(row.content)) {
        row.key = stringValue(row.content.id, stringValue(row.key, `${row.type || "module"}-${target.index + 1}`));
        row.sort = numberValue(row.content.sort, numberValue(row.sort, target.index * 10));
        row.enabled = row.content.enabled !== false;
      }
      ctx.homeDraft.value.modules[target.index] = normalizeHomeModule(row, target.index);
    }
    else ctx.staticDraft.value.pages[target.index] = normalizeStaticPage(value, target.index);
  }

  function hydrateEditingDraft(target: EditingTarget) {
    if (!ctx.editingDraft.value) ctx.editingDraft.value = emptyPatientSiteEditorDraft();
    const draft = ctx.editingDraft.value;
    if (target.type === "nav-menu" && !Array.isArray(draft.links)) draft.links = [];
    if (target.type === "home-hero") {
      if (!draft.primaryAction) draft.primaryAction = { label: "", routeName: "patient-home" };
      if (!draft.secondaryAction) draft.secondaryAction = { label: "", routeName: "patient-home" };
    }
    if (target.type === "home-module") {
      if (!isRow(draft.content)) draft.content = emptyPatientSiteEditorDraft().content;
      hydrateEditingHomeModuleContent();
    }
    if (target.type === "static-page" && !Array.isArray(draft.points)) draft.points = [];
  }

  function hydrateEditingHomeModuleContent() {
    if (!ctx.editingDraft.value) return;
    const content = editingContent();
    const type = stringValue(ctx.editingDraft.value.type, "");
    if (isHomeSectionType(type)) {
      hydrateEditingHomeSectionContent(type);
    } else if (ctx.editingDraft.value.type === "notice") {
      content.level = stringValue(content.level, "info");
      content.text = stringValue(content.text, "");
    } else if (ctx.editingDraft.value.type === "quick_actions") {
      ensureObjectArray("items");
    } else if (ctx.editingDraft.value.type === "intro") {
      ensureContentAction("action", "进入患者服务", "patient-dashboard");
    } else if (ctx.editingDraft.value.type === "locations") {
      ensureObjectArray("items");
    } else if (ctx.editingDraft.value.type === "featured_departments") {
      content.limit = numberValue(content.limit, 12);
      ensureObjectArray("items");
      ensureStringArray("fallbackNames");
    } else if (ctx.editingDraft.value.type === "static_content") {
      ensureContentAction("action", "了解科研与教育", "public-research");
    }
  }

  function editingContent() {
    if (!ctx.editingDraft.value) ctx.editingDraft.value = emptyPatientSiteEditorDraft();
    if (!isRow(ctx.editingDraft.value.content)) ctx.editingDraft.value.content = emptyPatientSiteEditorDraft().content;
    return ctx.editingDraft.value.content as EditableRow;
  }

  function ensureContentAction(field: string, label: string, routeName: string) {
    const content = editingContent();
    if (!isRow(content[field])) content[field] = { label, routeName, enabled: true, sort: 0 };
    const action = content[field] as EditableRow;
    action.label = stringValue(action.label, label);
    action.routeName = routeValue(action.routeName, routeName);
    action.enabled = action.enabled !== false;
    action.sort = numberValue(action.sort, 0);
    return action as RouteTargetConfig;
  }

  function ensureObjectArray(field: string) {
    const content = editingContent();
    content[field] = Array.isArray(content[field]) ? (content[field] as unknown[]).filter(isRow) : [];
    return content[field] as EditableRow[];
  }

  function ensureStringArray(field: string) {
    const content = editingContent();
    content[field] = Array.isArray(content[field]) ? (content[field] as unknown[]).map((item) => String(item || "")) : [];
    return content[field] as string[];
  }

  function editingArray(field: string) {
    if (!ctx.editingDraft.value) ctx.editingDraft.value = emptyPatientSiteEditorDraft();
    if (!Array.isArray(ctx.editingDraft.value[field])) ctx.editingDraft.value[field] = [];
    return ctx.editingDraft.value[field] as unknown[];
  }

  function editingContentItems() {
    const content = editingContent();
    if (!Array.isArray(content.items)) content.items = [];
    return content.items as RouteTargetConfig[];
  }

  function addMenu() {
    const index = ctx.navDraft.value.menus.length;
    ctx.navDraft.value.menus.push({
      key: `menu-${Date.now()}`,
      label: "新菜单",
      enabled: true,
      sort: nextSort(ctx.navDraft.value.menus),
      lead: "",
      description: "",
      links: [{ label: "患者首页", routeName: "patient-home", enabled: true, sort: 10 }],
    });
    ctx.openEditor({ type: "nav-menu", index });
  }

  function addUserLink() {
    const index = ctx.navDraft.value.userLinks.length;
    ctx.navDraft.value.userLinks.push({
      label: "新入口",
      routeName: "patient-home",
      enabled: true,
      sort: nextSort(ctx.navDraft.value.userLinks),
    });
    ctx.openEditor({ type: "user-link", index });
  }

  function addHomeModule(type: string, keyPrefix: string, content: PatientHomeModule["content"] = {}) {
    const key = `${keyPrefix}-${Date.now()}`;
    const sort = nextSort(ctx.homeDraft.value.modules);
    const moduleContent = isHomeSectionType(type)
      ? { ...createDefaultPatientSiteSection(type), id: key, sort, enabled: true }
      : content;
    ctx.homeDraft.value.modules.push({ type, key, enabled: true, sort, content: moduleContent });
    ctx.openEditor({ type: "home-module", index: ctx.homeDraft.value.modules.length - 1 });
  }

  function isHomeSectionType(type: string): type is PatientSiteSectionType {
    return isPatientSiteSectionType(type) && !legacyHomeModuleTypes.has(type);
  }

  async function previewCmsPage(page: PatientSitePageConfig) {
    if (!ctx.auth.session) return;
    const slug = typeof page.slug === "string" ? page.slug.trim().toLowerCase() : "";
    if (!slug) {
      ctx.validationErrors.patient_pages = ["预览 CMS 动态页前必须填写 slug"];
      return;
    }
    ctx.saving.value = true;
    ctx.status.value = "";
    ctx.error.value = "";
    try {
      const normalized = resolvePatientSiteConfigSection("patient_pages", clone(ctx.pagesDraft.value), { preserveDisabled: true }) as PatientSitePagesConfig;
      ctx.drafts.patient_pages = normalized;
      const row = await api.savePatientSiteConfig({
        configKey: "patient_pages",
        configJson: JSON.stringify(normalized),
        remark: ctx.remarks.patient_pages || "预览草稿",
      });
      await ctx.refreshHistory("patient_pages");
      const token = await api.patientSitePreviewToken("patient_pages", Number(row.version));
      window.open(`${patientPreviewOrigin()}/pages/${encodeURIComponent(slug)}?previewToken=${encodeURIComponent(token.token)}`, "_blank", "noopener");
      ctx.status.value = "预览草稿已保存，并已打开患者端预览窗口。";
    } catch (err) {
      ctx.error.value = messageFrom(err);
    } finally {
      ctx.saving.value = false;
    }
  }

  async function previewSite() {
    if (!ctx.auth.session) return;
    ctx.saving.value = true;
    ctx.status.value = "";
    ctx.error.value = "";
    try {
      for (const key of configKeys) {
        const normalized = resolvePatientSiteConfigSection(key, clone(ctx.drafts[key]), { preserveDisabled: true });
        ctx.drafts[key] = normalized as never;
        await api.savePatientSiteConfig({
          configKey: key,
          configJson: JSON.stringify(normalized),
          remark: ctx.remarks[key] || "整站预览草稿",
        });
      }
      await ctx.refreshHistory("patient_nav");
      const token = await api.patientSiteSitePreviewToken();
      window.open(`${patientPreviewOrigin()}?previewToken=${encodeURIComponent(token.token)}`, "_blank", "noopener");
      ctx.status.value = "整站预览草稿已保存，并已打开患者端预览窗口。";
    } catch (err) {
      ctx.error.value = messageFrom(err);
    } finally {
      ctx.saving.value = false;
    }
  }

  function hydrateEditingHomeSectionContent(type: PatientSiteSectionType) {
    const existing = editingContent();
    const draft = ctx.editingDraft.value;
    if (!draft) return;
    const section = normalizePatientSiteSection({
      ...createDefaultPatientSiteSection(type),
      ...existing,
      id: stringValue(existing.id, stringValue(draft.key, `${type}-${Date.now()}`)),
      type,
      enabled: draft.enabled !== false,
      sort: numberValue(draft.sort, 0),
    });
    draft.content = (section || createDefaultPatientSiteSection(type)) as PatientSiteEditorDraftContent;
  }

  function addCmsPage() {
    ctx.pagesDraft.value.pages.push({
      routeName: "about-hospital",
      slug: `cms-page-${Date.now()}`,
      label: "CMS 动态页",
      title: "新的 CMS 动态页",
      intro: "",
      enabled: true,
      sort: nextSort(ctx.pagesDraft.value.pages),
      sections: [],
    });
  }

  function addStaticPage() {
    const index = ctx.staticDraft.value.pages.length;
    ctx.staticDraft.value.pages.push({
      routeName: "about-hospital",
      label: "关于医院",
      title: "新静态页",
      intro: "",
      contentSource: "static",
      enabled: true,
      sort: nextSort(ctx.staticDraft.value.pages),
      points: [],
      primary: { label: "返回首页", routeName: "patient-home", enabled: true },
    });
    ctx.openEditor({ type: "static-page", index });
  }

  function addPageSection(pageIndex: number, type: PatientSiteSectionType) {
    const page = ctx.pagesDraft.value.pages[pageIndex];
    if (!page) return;
    const section = createDefaultPatientSiteSection(type);
    section.sort = nextSort(page.sections);
    page.sections.push(section);
  }

  async function removeCmsPage(index: number) {
    const page = ctx.pagesDraft.value.pages[index];
    if (!page) return false;
    if (!(await ctx.confirm({
      title: "确认删除 CMS 页面",
      message: `将从当前编辑稿中禁用 CMS 页面「${page.title || page.label || page.slug || "未命名页面"}」。保存草稿不会影响患者端，保存并生效或发布后，患者端才会停止读取这份页面配置。`,
      confirmText: "确认删除",
      tone: "danger",
    }))) return false;
    page.enabled = false;
    ctx.status.value = "已删除 CMS 页面，保存草稿或发布后生效";
    ctx.error.value = "";
    return true;
  }

  function reorderCmsPage(fromIndex: number, toIndex: number) {
    moveItem(ctx.pagesDraft.value.pages, fromIndex, toIndex);
    resequenceSort(ctx.pagesDraft.value.pages);
  }

  async function removePageSection(pageIndex: number, sectionIndex: number) {
    const section = ctx.pagesDraft.value.pages[pageIndex]?.sections[sectionIndex];
    if (!section) return false;
    if (!(await ctx.confirm({
      title: "确认删除页面区块",
      message: `将从当前编辑稿中禁用页面区块「${section.title || section.type}」。保存草稿不会影响患者端，保存并生效或发布后，患者端对应页面才会不再展示该区块。`,
      confirmText: "确认删除",
      tone: "danger",
    }))) return false;
    section.enabled = false;
    ctx.status.value = "已删除页面区块，保存草稿或发布后生效";
    ctx.error.value = "";
    return true;
  }

  function reorderPageSection(pageIndex: number, fromIndex: number, toIndex: number) {
    const page = ctx.pagesDraft.value.pages[pageIndex];
    if (!page) return;
    moveItem(page.sections, fromIndex, toIndex);
    resequenceSort(page.sections);
  }

  return {
    readEditingTarget,
    writeEditingTarget,
    hydrateEditingDraft,
    hydrateEditingHomeModuleContent,
    editingArray,
    editingContentItems,
    ensureObjectArray,
    ensureStringArray,
    addMenu,
    addUserLink,
    addHomeModule,
    previewSite,
    previewCmsPage,
    addCmsPage,
    addStaticPage,
    addPageSection,
    removeCmsPage,
    reorderCmsPage,
    removePageSection,
    reorderPageSection,
  };
}
