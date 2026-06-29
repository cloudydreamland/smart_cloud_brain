import {
  api,
  createDefaultPatientSiteSection,
  resolvePatientSiteConfigSection,
  type DataRow,
  type PatientHomeModule,
  type PatientSitePageConfig,
  type PatientSitePagesConfig,
  type PatientSiteSectionType,
  type PatientStaticPagesConfig,
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

type ValueRef<T> = { value: T };

type DraftActionContext = {
  auth: { session: unknown };
  drafts: ConfigDrafts;
  remarks: Record<string, string>;
  saving: ValueRef<boolean>;
  status: ValueRef<string>;
  error: ValueRef<string>;
  editingDraft: ValueRef<any>;
  navDraft: ValueRef<ConfigDrafts["patient_nav"]>;
  homeDraft: ValueRef<ConfigDrafts["patient_home"]>;
  staticDraft: ValueRef<PatientStaticPagesConfig>;
  pagesDraft: ValueRef<PatientSitePagesConfig>;
  validationErrors: Record<string, string[]>;
  openEditor: (target: EditingTarget) => void;
  refreshHistory: (key: "patient_pages") => Promise<void>;
};

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
    else if (target.type === "home-module") ctx.homeDraft.value.modules[target.index] = normalizeHomeModule(value, target.index);
    else ctx.staticDraft.value.pages[target.index] = normalizeStaticPage(value, target.index);
  }

  function hydrateEditingDraft(target: EditingTarget) {
    if (!ctx.editingDraft.value) ctx.editingDraft.value = {};
    if (target.type === "nav-menu" && !Array.isArray(ctx.editingDraft.value.links)) ctx.editingDraft.value.links = [];
    if (target.type === "home-hero") {
      if (!ctx.editingDraft.value.primaryAction) ctx.editingDraft.value.primaryAction = { label: "", routeName: "patient-home" };
      if (!ctx.editingDraft.value.secondaryAction) ctx.editingDraft.value.secondaryAction = { label: "", routeName: "patient-home" };
    }
    if (target.type === "home-module") {
      if (!isRow(ctx.editingDraft.value.content)) ctx.editingDraft.value.content = {};
      hydrateEditingHomeModuleContent();
    }
    if (target.type === "static-page" && !Array.isArray(ctx.editingDraft.value.points)) ctx.editingDraft.value.points = [];
  }

  function hydrateEditingHomeModuleContent() {
    if (!ctx.editingDraft.value) return;
    const content = editingContent();
    if (ctx.editingDraft.value.type === "notice") {
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
    if (!ctx.editingDraft.value) ctx.editingDraft.value = {};
    if (!isRow(ctx.editingDraft.value.content)) ctx.editingDraft.value.content = {};
    return ctx.editingDraft.value.content as DataRow;
  }

  function ensureContentAction(field: string, label: string, routeName: string) {
    const content = editingContent();
    if (!isRow(content[field])) content[field] = { label, routeName, enabled: true, sort: 0 };
    const action = content[field] as DataRow;
    action.label = stringValue(action.label, label);
    action.routeName = routeValue(action.routeName, routeName);
    action.enabled = action.enabled !== false;
    action.sort = numberValue(action.sort, 0);
    return action as RouteTargetConfig;
  }

  function ensureObjectArray(field: string) {
    const content = editingContent();
    content[field] = Array.isArray(content[field]) ? (content[field] as unknown[]).filter(isRow) : [];
    return content[field] as DataRow[];
  }

  function ensureStringArray(field: string) {
    const content = editingContent();
    content[field] = Array.isArray(content[field]) ? (content[field] as unknown[]).map((item) => String(item || "")) : [];
    return content[field] as string[];
  }

  function editingArray(field: string) {
    if (!ctx.editingDraft.value) ctx.editingDraft.value = {};
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
    ctx.homeDraft.value.modules.push({ type, key: `${keyPrefix}-${Date.now()}`, enabled: true, sort: nextSort(ctx.homeDraft.value.modules), content });
    ctx.openEditor({ type: "home-module", index: ctx.homeDraft.value.modules.length - 1 });
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

  function removeCmsPage(index: number) {
    const page = ctx.pagesDraft.value.pages[index];
    if (page) page.enabled = false;
  }

  function reorderCmsPage(fromIndex: number, toIndex: number) {
    moveItem(ctx.pagesDraft.value.pages, fromIndex, toIndex);
    resequenceSort(ctx.pagesDraft.value.pages);
  }

  function removePageSection(pageIndex: number, sectionIndex: number) {
    const section = ctx.pagesDraft.value.pages[pageIndex]?.sections[sectionIndex];
    if (section) section.enabled = false;
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
