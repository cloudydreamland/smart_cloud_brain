import { computed, reactive, ref } from "vue";
import {
  api,
  ApiError,
  createDefaultPatientSiteSection,
  patientSiteConfigTemplates,
  patientSiteSectionRegistry,
  patientSiteSectionTypes,
  resolvePatientSiteConfigSection,
  validatePatientSitePagesConfig,
  type DataRow,
  type PatientHomeConfig,
  type PatientHomeModule,
  type PatientNavConfig,
  type PatientNavMenu,
  type PatientSiteConfigHistoryPage,
  type PatientSiteConfigRecord,
  type PatientSiteConfigKey,
  type PatientSitePageConfig,
  type PatientSitePagesConfig,
  type PatientSiteSectionType,
  type PatientStaticPagesConfig,
  type RouteTargetConfig,
  type StaticPageConfig,
  useAuthStore,
} from "@smart-cloud-brain/shared-api";
import { isAllowedPatientRoute, patientRouteOptions } from "../patientSiteRoutes";
import { homeModuleTypeLabel } from "../patientSitePresentation";

type ConfigKey = PatientSiteConfigKey;
type EditorDraft = any;

export type ConfigTab = { key: ConfigKey; label: string; description: string };
export type EditingTarget =
  | { type: "brand" }
  | { type: "nav-menu"; index: number }
  | { type: "user-link"; index: number }
  | { type: "home-hero" }
  | { type: "home-module"; index: number }
  | { type: "static-page"; index: number };

type ConfigDrafts = {
  patient_nav: PatientNavConfig;
  patient_home: PatientHomeConfig;
  patient_static_pages: PatientStaticPagesConfig;
  patient_pages: PatientSitePagesConfig;
};

export const configTabs: ConfigTab[] = [
  { key: "patient_nav", label: "导航配置", description: "患者端顶部导航、下拉入口和登录后用户菜单。" },
  { key: "patient_home", label: "首页配置", description: "首页横幅、通知公告和快捷入口模块。" },
  { key: "patient_static_pages", label: "静态页配置", description: "按页面入口匹配的内容页标题、说明、要点和主按钮。" },
  { key: "patient_pages", label: "CMS 动态页", description: "配置患者端动态页面及页面区块内容。" },
];

export const homeModuleTypeOptions = [
  { value: "notice", label: homeModuleTypeLabel("notice") },
  { value: "quick_actions", label: homeModuleTypeLabel("quick_actions") },
  { value: "intro", label: homeModuleTypeLabel("intro") },
  { value: "locations", label: homeModuleTypeLabel("locations") },
  { value: "featured_departments", label: homeModuleTypeLabel("featured_departments") },
  { value: "static_content", label: homeModuleTypeLabel("static_content") },
] as const;

const configKeys = new Set<ConfigKey>(configTabs.map((tab) => tab.key));
const historyPageSize = 10;
const allowedHomeModules = new Set(homeModuleTypeOptions.map((item) => item.value));
const templates = clone(patientSiteConfigTemplates) as ConfigDrafts;
const emptyAction = (): RouteTargetConfig => ({ label: "", routeName: "" });
const emptyDrafts: ConfigDrafts = {
  patient_nav: { brand: { name: "", homeRoute: "" }, menus: [], userLinks: [] },
  patient_home: { hero: { enabled: false, eyebrow: "", title: "", primaryAction: emptyAction(), secondaryAction: emptyAction() }, modules: [] },
  patient_static_pages: { pages: [] },
  patient_pages: { pages: [] },
};

export function usePatientSiteConfigEditor() {
  const auth = useAuthStore();
  const activeKey = ref<ConfigKey>("patient_nav");
  const loading = ref(false);
  const saving = ref(false);
  const status = ref("");
  const error = ref("");
  const editorOpen = ref(false);
  const editingTarget = ref<EditingTarget | null>(null);
  const editingDraft = ref<EditorDraft>(null);
  const staticSearch = ref("");
  const staticDisabledOnly = ref(false);
  const drafts = reactive<ConfigDrafts>(clone(emptyDrafts));
  const latest = reactive<Record<ConfigKey, PatientSiteConfigRecord | null>>({ patient_nav: null, patient_home: null, patient_static_pages: null, patient_pages: null });
  const histories = reactive<Record<ConfigKey, PatientSiteConfigRecord[]>>({ patient_nav: [], patient_home: [], patient_static_pages: [], patient_pages: [] });
  const historyPages = reactive<Record<ConfigKey, PatientSiteConfigHistoryPage>>({
    patient_nav: emptyHistoryPage(),
    patient_home: emptyHistoryPage(),
    patient_static_pages: emptyHistoryPage(),
    patient_pages: emptyHistoryPage(),
  });
  const historyLoading = ref(false);
  const remarks = reactive<Record<ConfigKey, string>>({ patient_nav: "", patient_home: "", patient_static_pages: "", patient_pages: "" });
  const validationErrors = reactive<Record<ConfigKey, string[]>>({ patient_nav: [], patient_home: [], patient_static_pages: [], patient_pages: [] });

  const activeTab = computed(() => configTabs.find((tab) => tab.key === activeKey.value) || configTabs[0]);
  const activeRecord = computed(() => latest[activeKey.value]);
  const activeErrors = computed(() => validationErrors[activeKey.value]);
  const navDraft = computed(() => drafts.patient_nav);
  const homeDraft = computed(() => drafts.patient_home);
  const staticDraft = computed(() => drafts.patient_static_pages);
  const pagesDraft = computed(() => drafts.patient_pages);
  const filteredStaticPages = computed(() => filterStaticPages(staticDraft.value.pages, staticSearch.value, staticDisabledOnly.value));
  const sectionTypeOptions = computed(() => patientSiteSectionTypes.map((type) => ({ type, label: patientSiteSectionRegistry[type].label })));
  const editorTitle = computed(() => editorText(editingTarget.value, "title"));
  const editorDescription = computed(() => editorText(editingTarget.value, "description"));

  function setDraft(key: ConfigKey, value: unknown) {
    drafts[key] = resolvePatientSiteConfigSection(key, value, { preserveDisabled: true }) as never;
  }

  async function loadConfig(key = activeKey.value) {
    if (!auth.session) return;
    loading.value = true;
    error.value = "";
    status.value = "";
    validationErrors[key] = [];
    try {
      setDraft(key, await loadEffectiveSection(key));
      await refreshHistory(key, 1, true);
      remarks[key] = typeof latest[key]?.remark === "string" ? latest[key]?.remark || "" : "";
      status.value = "已加载患者端当前生效配置";
    } catch (err) {
      latest[key] = null;
      histories[key] = [];
      historyPages[key] = emptyHistoryPage();
      setDraft(key, {});
      remarks[key] = "";
      error.value = loadMessageFrom(err);
    } finally {
      loading.value = false;
    }
  }

  async function loadAll() {
    await Promise.all(configTabs.map((tab) => loadConfig(tab.key)));
    activeKey.value = configTabs[0].key;
  }

  function switchTab(key: ConfigKey) {
    activeKey.value = key;
    error.value = "";
    status.value = "";
    if (!latest[key]) void loadConfig(key);
  }

  function useTemplate() {
    if (!window.confirm("这会将当前编辑内容改为完整默认模板；保存并生效后会写入数据库成为当前真实配置。是否继续？")) return;
    setDraft(activeKey.value, templates[activeKey.value]);
    status.value = "已填入默认模板，保存后会直接生效";
    error.value = "";
    validationErrors[activeKey.value] = [];
  }

  function openEditor(target: EditingTarget) {
    editingTarget.value = target;
    editingDraft.value = clone(readEditingTarget(target) || {});
    hydrateEditingDraft(target);
    editorOpen.value = true;
  }

  function closeEditor() {
    editorOpen.value = false;
    editingTarget.value = null;
    editingDraft.value = null;
  }

  function applyEditor() {
    if (!editingTarget.value) return;
    writeEditingTarget(editingTarget.value, editingDraft.value);
    status.value = "已更新当前编辑内容，保存后会直接生效";
    error.value = "";
    closeEditor();
  }

  async function saveAndApply() {
    if (!auth.session) return;
    const payload = preparePayload(activeKey.value);
    if (!payload) return;
    const remark = publishRemark();
    if (!remark) return;
    saving.value = true;
    status.value = "";
    try {
      const row = await api.savePublishedPatientSiteConfig({
        configKey: activeKey.value,
        configJson: payload.configJson,
        remark,
      });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      if (typeof row.configJson === "string") setDraft(activeKey.value, JSON.parse(row.configJson));
      status.value = "配置已保存并生效；患者端刷新后会读取最新内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function saveDraft() {
    if (!auth.session) return;
    const payload = preparePayload(activeKey.value);
    if (!payload) return;
    saving.value = true;
    status.value = "";
    try {
      await api.savePatientSiteConfig({
        configKey: activeKey.value,
        configJson: payload.configJson,
        remark: remarks[activeKey.value],
      });
      await refreshHistory(activeKey.value);
      status.value = "草稿已保存，患者端不会读取；发布后才会生效";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function publishDraft() {
    if (!auth.session) return;
    const remark = publishRemark();
    if (!remark) return;
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      const row = await api.publishPatientSiteConfig({ configKey: activeKey.value, remark });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      if (typeof row.configJson === "string") setDraft(activeKey.value, JSON.parse(row.configJson));
      status.value = "最新草稿已发布，患者端刷新后会读取最新内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function rollbackTo(record: PatientSiteConfigRecord) {
    if (!auth.session || typeof record.configJson !== "string") return;
    if (!window.confirm(`确认回滚到版本 ${record.version || "-"}？这会生成一个新的已发布版本。`)) return;
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      const row = await api.savePublishedPatientSiteConfig({
        configKey: activeKey.value,
        configJson: record.configJson,
        remark: `回滚到版本 ${record.version || "-"}`,
      });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      setDraft(activeKey.value, JSON.parse(record.configJson));
      status.value = "已回滚并发布为新版本；患者端刷新后会读取回滚内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  function preparePayload(key: ConfigKey) {
    error.value = "";
    validationErrors[key] = [];
    if (!configKeys.has(key)) {
      validationErrors[key] = [`configKey 不合法：${key}`];
      return null;
    }
    const normalized = resolvePatientSiteConfigSection(key, clone(drafts[key]), { preserveDisabled: true });
    sanitizeBeforeSubmit(key, normalized);
    const errors = validateConfig(key, normalized);
    if (errors.length) {
      validationErrors[key] = errors;
      return null;
    }
    drafts[key] = normalized as never;
    return { configJson: JSON.stringify(normalized), data: normalized };
  }

  function publishRemark() {
    const remark = remarks[activeKey.value].trim();
    if (!remark) {
      validationErrors[activeKey.value] = ["发布前必须填写本次备注"];
      error.value = "发布前必须填写本次备注";
      return "";
    }
    return remark;
  }

  async function refreshHistory(key: ConfigKey, page = historyPages[key].page, syncLatest = false) {
    historyLoading.value = true;
    try {
      const result = await loadHistory(key, page, historyPageSize);
      histories[key] = result.items;
      historyPages[key] = result;
      if (syncLatest) {
        latest[key] = result.items.find((row) => row.status === "PUBLISHED") || latest[key] || null;
      }
    } finally {
      historyLoading.value = false;
    }
  }

  async function loadHistoryPage(page: number) {
    if (!auth.session) return;
    await refreshHistory(activeKey.value, page);
  }

  function readEditingTarget(target: EditingTarget) {
    if (target.type === "brand") return navDraft.value.brand;
    if (target.type === "nav-menu") return navDraft.value.menus[target.index];
    if (target.type === "user-link") return navDraft.value.userLinks[target.index];
    if (target.type === "home-hero") return homeDraft.value.hero;
    if (target.type === "home-module") return homeDraft.value.modules[target.index];
    return staticDraft.value.pages[target.index];
  }

  function writeEditingTarget(target: EditingTarget, value: unknown) {
    if (target.type === "brand") navDraft.value.brand = normalizeNav({ brand: value }).brand;
    else if (target.type === "nav-menu") navDraft.value.menus[target.index] = normalizeNavMenu(value, target.index);
    else if (target.type === "user-link") navDraft.value.userLinks[target.index] = normalizeLink(value);
    else if (target.type === "home-hero") homeDraft.value.hero = normalizeHome({ hero: value }).hero;
    else if (target.type === "home-module") homeDraft.value.modules[target.index] = normalizeHomeModule(value, target.index);
    else staticDraft.value.pages[target.index] = normalizeStaticPage(value, target.index);
  }

  function hydrateEditingDraft(target: EditingTarget) {
    if (!editingDraft.value) editingDraft.value = {};
    if (target.type === "nav-menu" && !Array.isArray(editingDraft.value.links)) editingDraft.value.links = [];
    if (target.type === "home-hero") {
      if (!editingDraft.value.primaryAction) editingDraft.value.primaryAction = { label: "", routeName: "patient-home" };
      if (!editingDraft.value.secondaryAction) editingDraft.value.secondaryAction = { label: "", routeName: "patient-home" };
    }
    if (target.type === "home-module") {
      if (!isRow(editingDraft.value.content)) editingDraft.value.content = {};
      hydrateEditingHomeModuleContent();
    }
    if (target.type === "static-page" && !Array.isArray(editingDraft.value.points)) editingDraft.value.points = [];
  }

  function hydrateEditingHomeModuleContent() {
    if (!editingDraft.value) return;
    const content = editingContent();
    if (editingDraft.value.type === "notice") {
      content.level = stringValue(content.level, "info");
      content.text = stringValue(content.text, "");
    } else if (editingDraft.value.type === "quick_actions") {
      ensureObjectArray("items");
    } else if (editingDraft.value.type === "intro") {
      ensureContentAction("action", "进入患者服务", "patient-dashboard");
    } else if (editingDraft.value.type === "locations") {
      ensureObjectArray("items");
    } else if (editingDraft.value.type === "featured_departments") {
      content.limit = numberValue(content.limit, 12);
      ensureObjectArray("items");
      ensureStringArray("fallbackNames");
    } else if (editingDraft.value.type === "static_content") {
      ensureContentAction("action", "了解科研与教育", "public-research");
    }
  }

  function editingContent() {
    if (!editingDraft.value) editingDraft.value = {};
    if (!isRow(editingDraft.value.content)) editingDraft.value.content = {};
    return editingDraft.value.content as DataRow;
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
    if (!editingDraft.value) editingDraft.value = {};
    if (!Array.isArray(editingDraft.value[field])) editingDraft.value[field] = [];
    return editingDraft.value[field] as unknown[];
  }

  function editingContentItems() {
    const content = editingContent();
    if (!Array.isArray(content.items)) content.items = [];
    return content.items as RouteTargetConfig[];
  }

  function addMenu() {
    const index = navDraft.value.menus.length;
    navDraft.value.menus.push({
      key: `menu-${Date.now()}`,
      label: "新菜单",
      enabled: true,
      sort: nextSort(navDraft.value.menus),
      lead: "",
      description: "",
      links: [{ label: "患者首页", routeName: "patient-home", enabled: true, sort: 10 }],
    });
    openEditor({ type: "nav-menu", index });
  }

  function addUserLink() {
    const index = navDraft.value.userLinks.length;
    navDraft.value.userLinks.push({
      label: "新入口",
      routeName: "patient-home",
      enabled: true,
      sort: nextSort(navDraft.value.userLinks),
    });
    openEditor({ type: "user-link", index });
  }

  function addHomeModule(type: string, keyPrefix: string, content: PatientHomeModule["content"] = {}) {
    homeDraft.value.modules.push({ type, key: `${keyPrefix}-${Date.now()}`, enabled: true, sort: nextSort(homeDraft.value.modules), content });
    openEditor({ type: "home-module", index: homeDraft.value.modules.length - 1 });
  }

  async function previewCmsPage(page: PatientSitePageConfig) {
    if (!auth.session) return;
    const slug = typeof page.slug === "string" ? page.slug.trim().toLowerCase() : "";
    if (!slug) {
      validationErrors.patient_pages = ["预览 CMS 动态页前必须填写 slug"];
      return;
    }
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      const normalized = resolvePatientSiteConfigSection("patient_pages", clone(pagesDraft.value), { preserveDisabled: true }) as PatientSitePagesConfig;
      drafts.patient_pages = normalized;
      const row = await api.savePatientSiteConfig({
        configKey: "patient_pages",
        configJson: JSON.stringify(normalized),
        remark: remarks.patient_pages || "预览草稿",
      });
      await refreshHistory("patient_pages");
      const token = await api.patientSitePreviewToken("patient_pages", Number(row.version));
      window.open(`${patientPreviewOrigin()}/pages/${encodeURIComponent(slug)}?previewToken=${encodeURIComponent(token.token)}`, "_blank", "noopener");
      status.value = "预览草稿已保存，并已打开患者端预览窗口。";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  function addCmsPage() {
    pagesDraft.value.pages.push({
      routeName: "about-hospital",
      slug: `cms-page-${Date.now()}`,
      label: "CMS 动态页",
      title: "新的 CMS 动态页",
      intro: "",
      enabled: true,
      sort: nextSort(pagesDraft.value.pages),
      sections: [],
    });
  }

  function addStaticPage() {
    const index = staticDraft.value.pages.length;
    staticDraft.value.pages.push({
      routeName: "about-hospital",
      label: "关于医院",
      title: "新静态页",
      intro: "",
      enabled: true,
      sort: nextSort(staticDraft.value.pages),
      points: [],
      primary: { label: "返回首页", routeName: "patient-home", enabled: true },
    });
    openEditor({ type: "static-page", index });
  }

  function addPageSection(pageIndex: number, type: PatientSiteSectionType) {
    const page = pagesDraft.value.pages[pageIndex];
    if (!page) return;
    const section = createDefaultPatientSiteSection(type);
    section.sort = nextSort(page.sections);
    page.sections.push(section);
  }

  function removeCmsPage(index: number) {
    const page = pagesDraft.value.pages[index];
    if (page) page.enabled = false;
  }

  function reorderCmsPage(fromIndex: number, toIndex: number) {
    moveItem(pagesDraft.value.pages, fromIndex, toIndex);
    resequenceSort(pagesDraft.value.pages);
  }

  function removePageSection(pageIndex: number, sectionIndex: number) {
    const section = pagesDraft.value.pages[pageIndex]?.sections[sectionIndex];
    if (section) section.enabled = false;
  }

  function reorderPageSection(pageIndex: number, fromIndex: number, toIndex: number) {
    const page = pagesDraft.value.pages[pageIndex];
    if (!page) return;
    moveItem(page.sections, fromIndex, toIndex);
    resequenceSort(page.sections);
  }

  return {
    tabs: configTabs,
    activeKey,
    activeTab,
    activeRecord,
    activeErrors,
    histories,
    loading,
    saving,
    status,
    error,
    remarks,
    historyPages,
    historyLoading,
    navDraft,
    homeDraft,
    staticDraft,
    pagesDraft,
    staticSearch,
    staticDisabledOnly,
    filteredStaticPages,
    sectionTypeOptions,
    editorOpen,
    editingTarget,
    editingDraft,
    editorTitle,
    editorDescription,
    patientRouteOptions,
    homeModuleTypeOptions,
    loadAll,
    loadConfig,
    loadHistoryPage,
    switchTab,
    useTemplate,
    openEditor,
    closeEditor,
    applyEditor,
    saveAndApply,
    saveDraft,
    publishDraft,
    rollbackTo,
    routeLabel,
    moduleSummary,
    hydrateEditingHomeModuleContent,
    editingContentItems,
    addEditingLink: () => editingArray("links").push({ label: "新链接", routeName: "patient-home", description: "", enabled: true, sort: nextSort(editingArray("links")) }),
    addEditingQuickAction: () => editingContentItems().push({ label: "新快捷入口", routeName: "patient-home", enabled: true, sort: nextSort(editingContentItems()) }),
    addEditingPoint: () => editingArray("points").push({ title: "新要点", text: "" }),
    addEditingLocationItem: () => ensureObjectArray("items").push({ title: "新院区", meta: "", imageUrl: "", alt: "" }),
    addEditingDepartmentLink: () => ensureObjectArray("items").push({ label: "新诊疗领域", routeName: "public-search", enabled: true, sort: nextSort(ensureObjectArray("items")) }),
    addEditingFallbackName: () => ensureStringArray("fallbackNames").push("新科室"),
    addMenu,
    removeMenu: (index: number) => (navDraft.value.menus[index].enabled = false),
    addUserLink,
    removeUserLink: (index: number) => (navDraft.value.userLinks[index].enabled = false),
    addNoticeModule: () => addHomeModule("notice", "notice", { level: "info", text: "" }),
    addQuickActionsModule: () => addHomeModule("quick_actions", "quick-actions", { items: [] }),
    addIntroModule: () => addHomeModule("intro", "intro"),
    addLocationsModule: () => addHomeModule("locations", "locations"),
    addFeaturedDepartmentsModule: () => addHomeModule("featured_departments", "featured-departments", { limit: 12 }),
    addStaticContentModule: () => addHomeModule("static_content", "static-content"),
    removeHomeModule: (module: PatientHomeModule) => (module.enabled = false),
    addStaticPage,
    removeStaticPage: (index: number) => (staticDraft.value.pages[index].enabled = false),
    addCmsPage,
    removeCmsPage,
    addPageSection,
    removePageSection,
    reorderCmsPage,
    reorderPageSection,
    previewCmsPage,
    toggleEnabled: (item: { enabled?: boolean }) => {
      item.enabled = item.enabled === false;
      status.value = item.enabled === false ? "已切换为禁用，保存后会直接生效" : "已切换为启用，保存后会直接生效";
      error.value = "";
    },
  };
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function emptyHistoryPage(): PatientSiteConfigHistoryPage {
  return { items: [], page: 1, pageSize: historyPageSize, total: 0, totalPages: 1 };
}

function isRow(value: unknown): value is DataRow {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function stringValue(value: unknown, fallback: string) {
  return typeof value === "string" ? value : fallback;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function routeValue(value: unknown, fallback: string) {
  return typeof value === "string" ? value : fallback;
}

function nextSort(items: Array<{ sort?: number }>) {
  return items.reduce((value, item) => Math.max(value, numberValue(item.sort, 0)), 0) + 10;
}

function moveItem<T>(items: T[], fromIndex: number, toIndex: number) {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= items.length || toIndex >= items.length) return;
  const [item] = items.splice(fromIndex, 1);
  items.splice(toIndex, 0, item);
}

function resequenceSort(items: Array<{ sort?: number }>) {
  items.forEach((item, index) => {
    item.sort = (index + 1) * 10;
  });
}

function messageFrom(err: unknown) {
  return err instanceof ApiError ? err.message : err instanceof Error ? err.message : "操作失败";
}

function loadMessageFrom(err: unknown) {
  const message = messageFrom(err);
  if (/internal server error/i.test(message)) return "配置表可能尚未初始化，请执行数据库迁移或重新初始化演示库。";
  return `${message}。请确认患者端配置接口和数据库迁移已正常运行。`;
}

function configSectionFromPublic(source: unknown, key: ConfigKey) {
  const row = isRow(source) ? source : {};
  if (key === "patient_nav") return row.nav;
  if (key === "patient_home") return row.home;
  if (key === "patient_pages") return row.pages;
  return row.staticPages;
}

async function loadEffectiveSection(key: ConfigKey) {
  const section = configSectionFromPublic(await api.patientSiteConfig(), key);
  return isRow(section) ? section : {};
}

async function loadHistory(key: ConfigKey, page: number, pageSize: number) {
  try {
    return await api.patientSiteConfigHistory(key, page, pageSize);
  } catch {
    return emptyHistoryPage();
  }
}

function filterStaticPages(pages: StaticPageConfig[], search: string, disabledOnly: boolean) {
  const keyword = search.trim().toLowerCase();
  return pages
    .map((page, index) => ({ page, index }))
    .filter(({ page }) => !disabledOnly || page.enabled === false)
    .filter(({ page }) => !keyword || [page.routeName, page.label, page.title].some((value) => String(value || "").toLowerCase().includes(keyword)));
}

function editorText(target: EditingTarget | null, field: "title" | "description") {
  if (!target) return field === "title" ? "编辑配置" : "";
  const text = {
    brand: ["编辑品牌信息", "控制患者端页头品牌名称和点击后的目标路由。"],
    "nav-menu": ["编辑导航菜单", "维护单个顶部菜单、下拉链接和 feature 入口。"],
    "user-link": ["编辑用户菜单入口", "维护登录后用户菜单中的单个入口。"],
    "home-hero": ["编辑首页 Hero", "维护首页首屏标题和主要操作。"],
    "home-module": ["编辑首页模块", "维护单个首页模块的类型、排序和内容。"],
    "static-page": ["编辑静态页", "维护一个 routeName 对应的静态内容页。"],
  }[target.type];
  return field === "title" ? text[0] : text[1];
}

function normalizeNav(value: unknown): PatientNavConfig {
  const normalized = resolvePatientSiteConfigSection("patient_nav", value, { preserveDisabled: true }) as PatientNavConfig;
  return normalized;
}

function normalizeNavMenu(value: unknown, index = 0): PatientNavMenu {
  const row = isRow(value) ? value : {};
  return {
    key: stringValue(row.key, `menu-${index + 1}`),
    label: stringValue(row.label, ""),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    lead: stringValue(row.lead, ""),
    description: stringValue(row.description, ""),
    links: Array.isArray(row.links) ? row.links.map((item) => normalizeLink(item)) : [],
    feature: isRow(row.feature) ? normalizeLink(row.feature) : undefined,
  };
}

function normalizeHome(value: unknown): PatientHomeConfig {
  return resolvePatientSiteConfigSection("patient_home", value, { preserveDisabled: true }) as PatientHomeConfig;
}

function normalizeHomeModule(value: unknown, index = 0): PatientHomeModule {
  const row = isRow(value) ? value : {};
  const type = stringValue(row.type, "notice");
  const content = isRow(row.content) ? row.content : {};
  const module: PatientHomeModule = {
    type,
    key: stringValue(row.key, `${type || "module"}-${index + 1}`),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    content: { ...content },
  };
  if (type === "quick_actions") module.content.items = Array.isArray(content.items) ? content.items.map((item) => normalizeLink(item)) : [];
  if (type === "notice") {
    module.content.level = stringValue(content.level, "warning");
    module.content.text = stringValue(content.text, "");
  }
  return module;
}

function normalizeStaticPage(value: unknown, index = 0): StaticPageConfig {
  const row = isRow(value) ? value : {};
  return {
    routeName: routeValue(row.routeName, "patient-home"),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    label: stringValue(row.label, ""),
    title: stringValue(row.title, ""),
    intro: stringValue(row.intro, ""),
    points: Array.isArray(row.points) ? row.points.map(normalizePoint) : [],
    primary: isRow(row.primary) ? normalizeLink(row.primary) : undefined,
  };
}

function normalizePoint(value: unknown) {
  const row = isRow(value) ? value : {};
  return { title: stringValue(row.title, ""), text: stringValue(row.text, "") };
}

function normalizeLink(value: unknown): RouteTargetConfig {
  const row = isRow(value) ? value : {};
  return {
    label: stringValue(row.label, ""),
    routeName: routeValue(row.routeName, "patient-home"),
    slug: typeof row.slug === "string" && row.slug.trim() ? row.slug.trim().toLowerCase() : undefined,
    description: stringValue(row.description, ""),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, 0),
    query: isStringMap(row.query) ? row.query : undefined,
  };
}

function isStringMap(value: unknown): value is Record<string, string> {
  return isRow(value) && Object.values(value).every((item) => typeof item === "string");
}

function sanitizeBeforeSubmit(key: ConfigKey, value: unknown) {
  if (key !== "patient_home") return;
  const home = value as PatientHomeConfig;
  home.modules
    .filter((module) => module.type === "quick_actions")
    .forEach((module) => {
      const items = Array.isArray(module.content?.items) ? module.content.items : [];
      module.content = {
        ...module.content,
        items: items
          .filter(isRow)
          .map((item) => normalizeLink(item))
          .filter((item) => item.label.trim() && isAllowedPatientRoute(item.routeName)),
      };
    });
}

function validateConfig(key: ConfigKey, value: unknown) {
  const errors: string[] = [];
  collectInvalidRoutes(value, errors);
  if (key === "patient_nav") validateNav(value as PatientNavConfig, errors);
  if (key === "patient_home") validateHome(value as PatientHomeConfig, errors);
  if (key === "patient_static_pages") validateStaticPages(value as PatientStaticPagesConfig, errors);
  if (key === "patient_pages") errors.push(...validatePatientSitePagesConfig(value as PatientSitePagesConfig));
  return errors;
}

function validateNav(nav: PatientNavConfig, errors: string[]) {
  requireText(nav.brand?.name, "brand.name 不能为空", errors);
  requireRoute(nav.brand?.homeRoute, "brand.homeRoute", errors);
  nav.menus.forEach((menu, menuIndex) => {
    requireText(menu.key, `menus[${menuIndex}].key 不能为空`, errors);
    requireText(menu.label, `menus[${menuIndex}].label 不能为空`, errors);
    menu.links?.forEach((link, linkIndex) => validateLink(link, `menus[${menuIndex}].links[${linkIndex}]`, errors));
    if (menu.feature) validateLink(menu.feature, `menus[${menuIndex}].feature`, errors);
  });
  nav.userLinks.forEach((link, linkIndex) => validateLink(link, `userLinks[${linkIndex}]`, errors));
}

function validateHome(home: PatientHomeConfig, errors: string[]) {
  if (home.hero?.enabled !== false) {
    requireText(home.hero?.title, "hero.title 不能为空", errors);
    if (home.hero?.primaryAction) validateLink(home.hero.primaryAction, "hero.primaryAction", errors);
    if (home.hero?.secondaryAction) validateLink(home.hero.secondaryAction, "hero.secondaryAction", errors);
  }
  home.modules.forEach((module, moduleIndex) => {
    requireText(module.type, `modules[${moduleIndex}].type 不能为空`, errors);
    requireText(module.key, `modules[${moduleIndex}].key 不能为空`, errors);
    if (module.type && !allowedHomeModules.has(module.type as never)) errors.push(`modules[${moduleIndex}].type 不支持：${module.type}`);
    if (module.type === "notice" && module.enabled !== false) requireText(module.content?.text, `modules[${moduleIndex}].content.text 不能为空`, errors);
    if (module.type === "quick_actions") {
      const items = Array.isArray(module.content?.items) ? module.content.items : [];
      items.forEach((item, itemIndex) => validateLink(item as RouteTargetConfig, `modules[${moduleIndex}].content.items[${itemIndex}]`, errors));
    }
  });
}

function validateStaticPages(staticPages: PatientStaticPagesConfig, errors: string[]) {
  staticPages.pages.forEach((page, pageIndex) => {
    requireRoute(page.routeName, `pages[${pageIndex}].routeName`, errors);
    requireText(page.label, `pages[${pageIndex}].label 不能为空`, errors);
    requireText(page.title, `pages[${pageIndex}].title 不能为空`, errors);
    page.points.forEach((point, pointIndex) => {
      requireText(point.title, `pages[${pageIndex}].points[${pointIndex}].title 不能为空`, errors);
      requireText(point.text, `pages[${pageIndex}].points[${pointIndex}].text 不能为空`, errors);
    });
    if (page.primary) validateLink(page.primary, `pages[${pageIndex}].primary`, errors);
  });
}

function validateLink(link: RouteTargetConfig, path: string, errors: string[]) {
  requireText(link.label, `${path}.label 不能为空`, errors);
  requireRoute(link.routeName, `${path}.routeName`, errors);
  if (link.routeName === "cms-page" && !link.slug?.trim()) errors.push(`${path}.slug 在 routeName 为 cms-page 时必填`);
}

function requireText(value: unknown, message: string, errors: string[]) {
  if (typeof value !== "string" || !value.trim()) errors.push(message);
}

function requireRoute(value: unknown, path: string, errors: string[]) {
  if (typeof value !== "string" || !value.trim()) {
    errors.push(`${path} 不能为空`);
  } else if (!isAllowedPatientRoute(value)) {
    errors.push(`${path} 不在患者端路由白名单内：${value}`);
  }
}

function collectInvalidRoutes(value: unknown, errors: string[], path = "") {
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectInvalidRoutes(item, errors, `${path}[${index}]`));
    return;
  }
  if (!isRow(value)) return;
  Object.entries(value).forEach(([field, child]) => {
    const childPath = path ? `${path}.${field}` : field;
    if (field === "routeName" && typeof child === "string" && child && !isAllowedPatientRoute(child)) {
      errors.push(`${childPath} 不在患者端路由白名单内：${child}`);
    }
    collectInvalidRoutes(child, errors, childPath);
  });
}

function moduleSummary(module: PatientHomeModule) {
  if (module.type === "notice") return String(module.content?.text || "未填写通知内容");
  if (module.type === "quick_actions") return `${Array.isArray(module.content?.items) ? module.content.items.length : 0} 个快捷入口`;
  return Object.keys(module.content || {}).length ? "内容已配置" : "暂无内容";
}

function routeLabel(routeName = "") {
  return patientRouteOptions.find((route) => route.name === routeName)?.label || routeName || "-";
}

function patientPreviewOrigin() {
  const configured = String(import.meta.env.VITE_PATIENT_WEB_ORIGIN || "").trim();
  if (configured) return configured.replace(/\/$/, "");
  if (typeof window === "undefined") return "";
  if (window.location.port === "5175") return `${window.location.protocol}//${window.location.hostname}:5173`;
  return window.location.origin;
}
