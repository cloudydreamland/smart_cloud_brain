<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  api,
  ApiError,
  patientSiteConfigTemplates,
  resolvePatientSiteConfigSection,
  type DataRow,
  useAuthStore,
} from "@smart-cloud-brain/shared-api";
import { isAllowedPatientRoute, patientRouteOptions } from "../patientSiteRoutes";

type ConfigKey = "patient_nav" | "patient_home" | "patient_static_pages";
type EditMode = "form" | "json";
type JsonObject = Record<string, any>;

type RouteTargetConfig = {
  label: string;
  routeName: string;
  description?: string;
  enabled?: boolean;
  sort?: number;
  query?: Record<string, string>;
};

type PatientNavMenu = {
  key: string;
  label: string;
  enabled?: boolean;
  sort?: number;
  lead?: string;
  description?: string;
  links: RouteTargetConfig[];
  feature?: RouteTargetConfig;
};

type PatientNavConfig = {
  brand: { name: string; homeRoute: string };
  menus: PatientNavMenu[];
  userLinks: RouteTargetConfig[];
};

type PatientHomeModule = {
  type: string;
  key: string;
  enabled?: boolean;
  sort?: number;
  content: JsonObject;
};

type PatientHomeConfig = {
  hero: {
    enabled?: boolean;
    eyebrow?: string;
    title: string;
    primaryAction: RouteTargetConfig;
    secondaryAction: RouteTargetConfig;
  };
  modules: PatientHomeModule[];
};

type StaticPageConfig = {
  routeName: string;
  enabled?: boolean;
  sort?: number;
  label: string;
  title: string;
  intro: string;
  points: { title: string; text: string }[];
  primary?: RouteTargetConfig;
};

type PatientStaticPagesConfig = {
  pages: StaticPageConfig[];
};

type ConfigDrafts = {
  patient_nav: PatientNavConfig;
  patient_home: PatientHomeConfig;
  patient_static_pages: PatientStaticPagesConfig;
};

type ConfigTab = { key: ConfigKey; label: string; description: string };
type EditingTarget =
  | { type: "brand" }
  | { type: "nav-menu"; index: number }
  | { type: "user-link"; index: number }
  | { type: "home-hero" }
  | { type: "home-module"; index: number }
  | { type: "static-page"; index: number };

const tabs: ConfigTab[] = [
  { key: "patient_nav", label: "导航配置", description: "患者端顶部导航、下拉入口和登录后用户菜单。" },
  { key: "patient_home", label: "首页配置", description: "首页 hero、notice 和 quick actions 模块。" },
  { key: "patient_static_pages", label: "静态页配置", description: "按 routeName 匹配的内容页标题、说明、要点和主按钮。" },
];

const configKeys = new Set<ConfigKey>(tabs.map((tab) => tab.key));
const allowedHomeModules = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

const templates: ConfigDrafts = clone(patientSiteConfigTemplates) as ConfigDrafts;

const auth = useAuthStore();
const activeKey = ref<ConfigKey>("patient_nav");
const loading = ref(false);
const saving = ref(false);
const publishing = ref(false);
const status = ref("");
const error = ref("");
const applyingJson = ref(false);
const publishConfirmOpen = ref(false);
const publishPreparedJson = ref("");
const editorOpen = ref(false);
const editingTarget = ref<EditingTarget | null>(null);
const editingDraft = ref<any>(null);
const editingContentText = ref("");
const staticSearch = ref("");
const staticDisabledOnly = ref(false);

const modes = reactive<Record<ConfigKey, EditMode>>({
  patient_nav: "form",
  patient_home: "form",
  patient_static_pages: "form",
});
const editors = reactive<Record<ConfigKey, string>>({
  patient_nav: pretty(templates.patient_nav),
  patient_home: pretty(templates.patient_home),
  patient_static_pages: pretty(templates.patient_static_pages),
});
const drafts = reactive<ConfigDrafts>({
  patient_nav: clone(templates.patient_nav),
  patient_home: clone(templates.patient_home),
  patient_static_pages: clone(templates.patient_static_pages),
});
const latest = reactive<Record<ConfigKey, DataRow | null>>({
  patient_nav: null,
  patient_home: null,
  patient_static_pages: null,
});
const remarks = reactive<Record<ConfigKey, string>>({
  patient_nav: "",
  patient_home: "",
  patient_static_pages: "",
});
const validationErrors = reactive<Record<ConfigKey, string[]>>({
  patient_nav: [],
  patient_home: [],
  patient_static_pages: [],
});

const activeTab = computed(() => tabs.find((tab) => tab.key === activeKey.value) || tabs[0]);
const activeRecord = computed(() => latest[activeKey.value]);
const activeErrors = computed(() => validationErrors[activeKey.value]);
const activeMode = computed(() => modes[activeKey.value]);
const navDraft = computed(() => drafts.patient_nav);
const homeDraft = computed(() => drafts.patient_home);
const staticDraft = computed(() => drafts.patient_static_pages);
const noticeModules = computed(() => homeDraft.value.modules.filter((module) => module.type === "notice"));
const quickActionModules = computed(() => homeDraft.value.modules.filter((module) => module.type === "quick_actions"));
const filteredStaticPages = computed(() => {
  const keyword = staticSearch.value.trim().toLowerCase();
  return staticDraft.value.pages
    .map((page, index) => ({ page, index }))
    .filter(({ page }) => !staticDisabledOnly.value || page.enabled === false)
    .filter(({ page }) => {
      if (!keyword) return true;
      return [page.routeName, page.label, page.title].some((value) => String(value || "").toLowerCase().includes(keyword));
    });
});
const editorTitle = computed(() => {
  const target = editingTarget.value;
  if (!target) return "编辑配置";
  if (target.type === "brand") return "编辑品牌信息";
  if (target.type === "nav-menu") return "编辑导航菜单";
  if (target.type === "user-link") return "编辑用户菜单入口";
  if (target.type === "home-hero") return "编辑首页 Hero";
  if (target.type === "home-module") return "编辑首页模块";
  return "编辑静态页";
});
const editorDescription = computed(() => {
  const target = editingTarget.value;
  if (!target) return "";
  if (target.type === "brand") return "控制患者端页头品牌名称和点击后的目标路由。";
  if (target.type === "nav-menu") return "维护单个顶部菜单、下拉链接和 feature 入口。";
  if (target.type === "user-link") return "维护登录后用户菜单中的单个入口。";
  if (target.type === "home-hero") return "维护首页首屏标题和主要操作。";
  if (target.type === "home-module") return "维护单个首页模块的类型、排序和内容。";
  return "维护一个 routeName 对应的静态内容页。";
});

for (const tab of tabs) {
  watch(
    () => drafts[tab.key],
    () => {
      if (!applyingJson.value && modes[tab.key] === "form") {
        syncEditorFromDraft(tab.key);
      }
      publishConfirmOpen.value = false;
    },
    { deep: true },
  );
}

function pretty(value: unknown) {
  return JSON.stringify(value, null, 2);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function isRecord(value: unknown): value is JsonObject {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function messageFrom(err: unknown) {
  return err instanceof ApiError ? err.message : err instanceof Error ? err.message : "操作失败";
}

function loadMessageFrom(err: unknown) {
  const message = messageFrom(err);
  if (/internal server error/i.test(message)) {
    return "配置表可能尚未初始化，请执行数据库迁移或重新初始化演示库。";
  }
  return `${message}。如果是首次运行，请执行数据库迁移或重新初始化演示库。`;
}

function syncEditorFromDraft(key: ConfigKey) {
  editors[key] = pretty(drafts[key]);
}

function setDraft(key: ConfigKey, value: unknown) {
  applyingJson.value = true;
  drafts[key] = normalizeDraft(key, value) as never;
  editors[key] = pretty(drafts[key]);
  applyingJson.value = false;
}

function normalizeDraft(key: ConfigKey, value: unknown) {
  return resolvePatientSiteConfigSection(key, value, { preserveDisabled: true });
}

function normalizeNav(value: unknown): PatientNavConfig {
  const row = isRecord(value) ? value : {};
  const fallback = templates.patient_nav;
  const brand = isRecord(row.brand) ? row.brand : {};
  return {
    ...row,
    brand: {
      name: stringValue(brand.name, fallback.brand.name),
      homeRoute: routeValue(brand.homeRoute, fallback.brand.homeRoute),
    },
    menus: Array.isArray(row.menus) ? row.menus.map(normalizeNavMenu) : clone(fallback.menus),
    userLinks: Array.isArray(row.userLinks) ? row.userLinks.map((item) => normalizeLink(item)) : clone(fallback.userLinks),
  };
}

function normalizeNavMenu(value: unknown, index = 0): PatientNavMenu {
  const row = isRecord(value) ? value : {};
  const menu: PatientNavMenu = {
    ...row,
    key: stringValue(row.key, `menu-${index + 1}`),
    label: stringValue(row.label, ""),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    lead: stringValue(row.lead, ""),
    description: stringValue(row.description, ""),
    links: Array.isArray(row.links) ? row.links.map((item) => normalizeLink(item)) : [],
  };
  if (isRecord(row.feature)) {
    menu.feature = normalizeLink(row.feature);
  }
  return menu;
}

function normalizeHome(value: unknown): PatientHomeConfig {
  const row = isRecord(value) ? value : {};
  const fallback = templates.patient_home;
  const hero = isRecord(row.hero) ? row.hero : {};
  return {
    ...row,
    hero: {
      enabled: hero.enabled !== false,
      eyebrow: stringValue(hero.eyebrow, fallback.hero.eyebrow || ""),
      title: stringValue(hero.title, fallback.hero.title),
      primaryAction: isRecord(hero.primaryAction) ? normalizeLink(hero.primaryAction) : clone(fallback.hero.primaryAction),
      secondaryAction: isRecord(hero.secondaryAction) ? normalizeLink(hero.secondaryAction) : clone(fallback.hero.secondaryAction),
    },
    modules: Array.isArray(row.modules) ? row.modules.map(normalizeHomeModule) : clone(fallback.modules),
  };
}

function normalizeHomeModule(value: unknown, index = 0): PatientHomeModule {
  const row = isRecord(value) ? value : {};
  const type = stringValue(row.type, "notice");
  const content = isRecord(row.content) ? row.content : {};
  const module: PatientHomeModule = {
    ...row,
    type,
    key: stringValue(row.key, `${type || "module"}-${index + 1}`),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    content: { ...content },
  };
  if (type === "quick_actions") {
    module.content.items = Array.isArray(content.items) ? content.items.map((item) => normalizeLink(item)) : [];
  }
  if (type === "notice") {
    module.content.level = stringValue(content.level, "warning");
    module.content.text = stringValue(content.text, "");
  }
  return module;
}

function normalizeStaticPages(value: unknown): PatientStaticPagesConfig {
  const row = isRecord(value) ? value : {};
  const fallback = templates.patient_static_pages;
  return {
    ...row,
    pages: Array.isArray(row.pages) ? row.pages.map(normalizeStaticPage) : clone(fallback.pages),
  };
}

function normalizeStaticPage(value: unknown, index = 0): StaticPageConfig {
  const row = isRecord(value) ? value : {};
  const page: StaticPageConfig = {
    ...row,
    routeName: routeValue(row.routeName, "patient-home"),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    label: stringValue(row.label, ""),
    title: stringValue(row.title, ""),
    intro: stringValue(row.intro, ""),
    points: Array.isArray(row.points) ? row.points.map(normalizePoint) : [],
  };
  if (isRecord(row.primary)) {
    page.primary = normalizeLink(row.primary);
  }
  return page;
}

function normalizePoint(value: unknown) {
  const row = isRecord(value) ? value : {};
  return { title: stringValue(row.title, ""), text: stringValue(row.text, "") };
}

function normalizeLink(value: unknown): RouteTargetConfig {
  const row = isRecord(value) ? value : {};
  return {
    ...row,
    label: stringValue(row.label, ""),
    routeName: routeValue(row.routeName, "patient-home"),
    description: stringValue(row.description, ""),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, 0),
  };
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
  const max = items.reduce((value, item) => Math.max(value, numberValue(item.sort, 0)), 0);
  return max + 10;
}

function configSectionFromPublic(source: unknown, key: ConfigKey) {
  const row = isRecord(source) ? source : {};
  if (key === "patient_nav") return row.nav;
  if (key === "patient_home") return row.home;
  return row.staticPages;
}

async function loadEffectiveSection(key: ConfigKey) {
  const publicConfig = await api.patientSiteConfig();
  return configSectionFromPublic(publicConfig, key) || templates[key];
}

async function loadConfig(key = activeKey.value) {
  if (!auth.session) return;
  loading.value = true;
  error.value = "";
  status.value = "";
  validationErrors[key] = [];
  publishConfirmOpen.value = false;
  try {
    const row = await api.adminPatientSiteConfig(auth.token(), key);
    latest[key] = Object.keys(row).length ? row : null;
    const hasDraft = row.status === "DRAFT" && typeof row.configJson === "string" && row.configJson.trim();
    const configJson = hasDraft ? JSON.parse(row.configJson) : await loadEffectiveSection(key);
    setDraft(key, configJson);
    remarks[key] = typeof row.remark === "string" ? row.remark : "";
    status.value = hasDraft ? "已加载草稿配置；患者端仍读取已发布版本" : "已加载患者端当前生效配置";
  } catch (err) {
    latest[key] = null;
    try {
      setDraft(key, await loadEffectiveSection(key));
      status.value = "已加载患者端当前生效配置";
    } catch {
      setDraft(key, templates[key]);
      status.value = "配置接口暂不可用，已显示完整默认配置";
    }
    remarks[key] = "";
    error.value = loadMessageFrom(err);
  } finally {
    loading.value = false;
  }
}

async function loadAll() {
  await Promise.all(tabs.map((tab) => loadConfig(tab.key)));
  activeKey.value = tabs[0].key;
}

function switchTab(key: ConfigKey) {
  activeKey.value = key;
  error.value = "";
  status.value = "";
  publishConfirmOpen.value = false;
  if (!latest[key]) void loadConfig(key);
}

function setMode(mode: EditMode) {
  const key = activeKey.value;
  error.value = "";
  validationErrors[key] = [];
  publishConfirmOpen.value = false;
  if (mode === "json") {
    syncEditorFromDraft(key);
    modes[key] = "json";
    return;
  }
  try {
    const parsed = JSON.parse(editors[key]);
    setDraft(key, parsed);
    modes[key] = "form";
    status.value = "JSON 已同步到表单";
  } catch {
    validationErrors[key] = ["JSON 格式无效，无法切换到表单编辑。"];
  }
}

function useTemplate() {
  if (!window.confirm("这会用完整默认配置覆盖当前编辑草稿，且不会直接发布。是否继续？")) return;
  setDraft(activeKey.value, templates[activeKey.value]);
  status.value = "已填入默认模板，尚未保存";
  error.value = "";
  validationErrors[activeKey.value] = [];
  publishConfirmOpen.value = false;
}

function openEditor(target: EditingTarget) {
  editingTarget.value = target;
  editingDraft.value = clone(readEditingTarget(target) || {});
  hydrateEditingDraft(target);
  syncEditingContentText();
  editorOpen.value = true;
  publishConfirmOpen.value = false;
}

function closeEditor() {
  editorOpen.value = false;
  editingTarget.value = null;
  editingDraft.value = null;
  editingContentText.value = "";
}

function applyEditor() {
  if (!editingTarget.value) return;
  if (!applyEditingContentJson()) return;
  writeEditingTarget(editingTarget.value, editingDraft.value);
  status.value = "已更新当前草稿，发布前请保存草稿";
  error.value = "";
  closeEditor();
}

function syncEditingContentText() {
  if (editingTarget.value?.type === "home-module" && editingDraft.value && !isSimpleHomeModuleType(editingDraft.value.type)) {
    editingContentText.value = pretty(editingDraft.value.content || {});
    return;
  }
  editingContentText.value = "";
}

function applyEditingContentJson() {
  if (editingTarget.value?.type !== "home-module" || !editingDraft.value || isSimpleHomeModuleType(editingDraft.value.type)) {
    return true;
  }
  try {
    const parsed = JSON.parse(editingContentText.value || "{}");
    if (!isRecord(parsed)) {
      validationErrors[activeKey.value] = ["模块 content 必须是 JSON object。"];
      return false;
    }
    editingDraft.value.content = parsed;
    return true;
  } catch {
    validationErrors[activeKey.value] = ["模块 content JSON 格式无效。"];
    return false;
  }
}

function isSimpleHomeModuleType(type: unknown) {
  return type === "notice" || type === "quick_actions";
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
  if (target.type === "brand") {
    navDraft.value.brand = normalizeNav({ brand: value }).brand;
    return;
  }
  if (target.type === "nav-menu") {
    navDraft.value.menus[target.index] = normalizeNavMenu(value, target.index);
    return;
  }
  if (target.type === "user-link") {
    navDraft.value.userLinks[target.index] = normalizeLink(value);
    return;
  }
  if (target.type === "home-hero") {
    homeDraft.value.hero = normalizeHome({ hero: value }).hero;
    return;
  }
  if (target.type === "home-module") {
    homeDraft.value.modules[target.index] = normalizeHomeModule(value, target.index);
    return;
  }
  staticDraft.value.pages[target.index] = normalizeStaticPage(value, target.index);
}

function hydrateEditingDraft(target: EditingTarget) {
  if (!editingDraft.value) editingDraft.value = {};
  if (target.type === "nav-menu") {
    if (!Array.isArray(editingDraft.value.links)) editingDraft.value.links = [];
    return;
  }
  if (target.type === "home-hero") {
    if (!editingDraft.value.primaryAction) editingDraft.value.primaryAction = { label: "", routeName: "patient-home" };
    if (!editingDraft.value.secondaryAction) editingDraft.value.secondaryAction = { label: "", routeName: "patient-home" };
    return;
  }
  if (target.type === "home-module") {
    if (!editingDraft.value.content || typeof editingDraft.value.content !== "object") editingDraft.value.content = {};
    if (editingDraft.value.type === "quick_actions" && !Array.isArray(editingDraft.value.content.items)) {
      editingDraft.value.content.items = [];
    }
    return;
  }
  if (target.type === "static-page" && !Array.isArray(editingDraft.value.points)) {
    editingDraft.value.points = [];
  }
}

function moduleSummary(module: PatientHomeModule) {
  if (module.type === "notice") return String(module.content?.text || "未填写通知内容");
  if (module.type === "quick_actions") {
    const items = Array.isArray(module.content?.items) ? module.content.items : [];
    return `${items.length} 个快捷入口`;
  }
  return Object.keys(module.content || {}).length ? "JSON 内容已配置" : "暂无内容";
}

function routeLabel(routeName = "") {
  return patientRouteOptions.find((route) => route.name === routeName)?.label || routeName || "-";
}

function toggleEnabled(item: { enabled?: boolean }) {
  item.enabled = item.enabled === false;
  status.value = item.enabled === false ? "已切换为禁用，发布前请保存草稿" : "已切换为启用，发布前请保存草稿";
  error.value = "";
  publishConfirmOpen.value = false;
}

function editingArray(field: string) {
  if (!editingDraft.value) editingDraft.value = {};
  if (!editingDraft.value || !Array.isArray(editingDraft.value[field])) editingDraft.value[field] = [];
  return editingDraft.value[field] as any[];
}

function editingContentItems() {
  if (!editingDraft.value) editingDraft.value = {};
  if (!editingDraft.value.content || typeof editingDraft.value.content !== "object") editingDraft.value.content = {};
  if (!Array.isArray(editingDraft.value.content.items)) editingDraft.value.content.items = [];
  return editingDraft.value.content.items as RouteTargetConfig[];
}

function addEditingLink() {
  editingArray("links").push({ label: "新链接", routeName: "patient-home", description: "", enabled: true, sort: nextSort(editingArray("links")) });
}

function addEditingQuickAction() {
  const items = editingContentItems();
  items.push({ label: "新快捷入口", routeName: "patient-home", enabled: true, sort: nextSort(items) });
}

function addEditingPoint() {
  editingArray("points").push({ title: "新要点", text: "" });
}

async function copyPublishedToDraft() {
  if (!auth.session) return;
  const key = activeKey.value;
  saving.value = true;
  error.value = "";
  status.value = "";
  validationErrors[key] = [];
  publishConfirmOpen.value = false;
  try {
    const parsed = await loadEffectiveSection(key);
    setDraft(key, parsed);
    remarks[key] = "从患者端当前生效配置复制";
    const payload = preparePayload(key);
    if (!payload) return;
    await saveDraftRecord(payload.configJson);
    status.value = "已复制患者端当前生效配置为草稿";
  } catch (err) {
    error.value = messageFrom(err);
  } finally {
    saving.value = false;
  }
}

function preparePayload(key = activeKey.value) {
  error.value = "";
  validationErrors[key] = [];
  let source: unknown;
  if (!configKeys.has(key)) {
    validationErrors[key] = [`configKey 不合法：${key}`];
    return null;
  }
  try {
    source = modes[key] === "json" ? JSON.parse(editors[key]) : clone(drafts[key]);
  } catch {
    validationErrors[key] = ["JSON 格式无效，请修正后再提交。"];
    return null;
  }
  const normalized = normalizeDraft(key, source);
  sanitizeBeforeSubmit(key, normalized);
  const errors = validateConfig(key, normalized);
  if (errors.length) {
    validationErrors[key] = errors;
    return null;
  }
  if (modes[key] === "form") {
    setDraft(key, normalized);
  } else {
    editors[key] = pretty(normalized);
  }
  return { configJson: JSON.stringify(normalized), data: normalized };
}

function sanitizeBeforeSubmit(key: ConfigKey, value: unknown) {
  if (key !== "patient_home") return;
  const home = value as PatientHomeConfig;
  home.modules
    .filter((module) => module.type === "quick_actions")
    .forEach((module) => {
      const items = Array.isArray(module.content.items) ? module.content.items : [];
      module.content.items = items
        .filter((item) => isRecord(item))
        .map((item) => normalizeLink(item))
        .filter((item) => item.label.trim() && isAllowedPatientRoute(item.routeName));
    });
}

function validateConfig(key: ConfigKey, value: unknown) {
  const errors: string[] = [];
  collectInvalidRoutes(value, errors);
  if (key === "patient_nav") validateNav(value as PatientNavConfig, errors);
  if (key === "patient_home") validateHome(value as PatientHomeConfig, errors);
  if (key === "patient_static_pages") validateStaticPages(value as PatientStaticPagesConfig, errors);
  return errors;
}

function validateNav(nav: PatientNavConfig, errors: string[]) {
  requireText(nav.brand?.name, "brand.name 不能为空", errors);
  requireRoute(nav.brand?.homeRoute, "brand.homeRoute", errors);
  nav.menus.forEach((menu, menuIndex) => {
    requireText(menu.key, `menus[${menuIndex}].key 不能为空`, errors);
    requireText(menu.label, `menus[${menuIndex}].label 不能为空`, errors);
    menu.links.forEach((link, linkIndex) => validateLink(link, `menus[${menuIndex}].links[${linkIndex}]`, errors));
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
    if (module.type && !allowedHomeModules.has(module.type)) {
      errors.push(`modules[${moduleIndex}].type 不支持：${module.type}`);
    }
    if (module.type === "notice" && module.enabled !== false) {
      requireText(module.content?.text, `modules[${moduleIndex}].content.text 不能为空`, errors);
    }
    if (module.type === "quick_actions") {
      const items = Array.isArray(module.content?.items) ? module.content.items : [];
      items.forEach((item, itemIndex) => validateLink(item, `modules[${moduleIndex}].content.items[${itemIndex}]`, errors));
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
}

function requireText(value: unknown, message: string, errors: string[]) {
  if (typeof value !== "string" || !value.trim()) errors.push(message);
}

function requireRoute(value: unknown, path: string, errors: string[]) {
  if (typeof value !== "string" || !value.trim()) {
    errors.push(`${path} 不能为空`);
    return;
  }
  if (!isAllowedPatientRoute(value)) errors.push(`${path} 不在患者端路由白名单内：${value}`);
}

function collectInvalidRoutes(value: unknown, errors: string[], path = "") {
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectInvalidRoutes(item, errors, `${path}[${index}]`));
    return;
  }
  if (!isRecord(value)) return;
  Object.entries(value).forEach(([field, child]) => {
    const childPath = path ? `${path}.${field}` : field;
    if (field === "routeName" && typeof child === "string" && child && !isAllowedPatientRoute(child)) {
      errors.push(`${childPath} 不在患者端路由白名单内：${child}`);
    }
    collectInvalidRoutes(child, errors, childPath);
  });
}

async function saveDraftRecord(configJson: string) {
  const row = await api.savePatientSiteConfig(auth.token(), {
    configKey: activeKey.value,
    configJson,
    remark: remarks[activeKey.value],
  });
  latest[activeKey.value] = row;
  if (typeof row.configJson === "string") {
    setDraft(activeKey.value, JSON.parse(row.configJson));
  }
  return row;
}

async function saveDraft() {
  if (!auth.session) return;
  const payload = preparePayload();
  if (!payload) return;
  saving.value = true;
  status.value = "";
  publishConfirmOpen.value = false;
  try {
    await saveDraftRecord(payload.configJson);
    status.value = "草稿已保存；患者端不会变化，点击发布后才会生效";
  } catch (err) {
    error.value = messageFrom(err);
  } finally {
    saving.value = false;
  }
}

function requestPublish() {
  if (!auth.session) return;
  const payload = preparePayload();
  if (!payload) return;
  publishPreparedJson.value = payload.configJson;
  publishConfirmOpen.value = true;
  status.value = "";
}

async function confirmPublish() {
  if (!auth.session || !publishPreparedJson.value) return;
  const payload = preparePayload();
  if (!payload) return;
  publishing.value = true;
  error.value = "";
  status.value = "正在保存草稿...";
  try {
    await saveDraftRecord(payload.configJson);
    status.value = "正在发布...";
    const row = await api.publishPatientSiteConfig(auth.token(), { configKey: activeKey.value });
    latest[activeKey.value] = row;
    if (typeof row.configJson === "string") {
      setDraft(activeKey.value, JSON.parse(row.configJson));
    }
    publishConfirmOpen.value = false;
    publishPreparedJson.value = "";
    status.value = "配置已发布；患者端刷新后会读取 /api/patient-site/config 的最新内容";
  } catch (err) {
    error.value = messageFrom(err);
  } finally {
    publishing.value = false;
  }
}

function addMenu() {
  navDraft.value.menus.push({
    key: `menu-${Date.now()}`,
    label: "新菜单",
    enabled: true,
    sort: nextSort(navDraft.value.menus),
    lead: "",
    description: "",
    links: [],
  });
  openEditor({ type: "nav-menu", index: navDraft.value.menus.length - 1 });
}

function removeMenu(index: number) {
  navDraft.value.menus[index].enabled = false;
}

function addMenuLink(menu: PatientNavMenu) {
  menu.links.push({ label: "新链接", routeName: "patient-home", description: "", enabled: true, sort: nextSort(menu.links) });
}

function removeMenuLink(menu: PatientNavMenu, index: number) {
  menu.links[index].enabled = false;
}

function addMenuFeature(menu: PatientNavMenu) {
  menu.feature = { label: "特色入口", routeName: "patient-home", enabled: true, sort: 0 };
}

function removeMenuFeature(menu: PatientNavMenu) {
  if (menu.feature) menu.feature.enabled = false;
}

function addUserLink() {
  navDraft.value.userLinks.push({ label: "新用户入口", routeName: "patient-dashboard", enabled: true, sort: nextSort(navDraft.value.userLinks) });
  openEditor({ type: "user-link", index: navDraft.value.userLinks.length - 1 });
}

function removeUserLink(index: number) {
  navDraft.value.userLinks[index].enabled = false;
}

function addNoticeModule() {
  homeDraft.value.modules.push({
    type: "notice",
    key: `notice-${Date.now()}`,
    enabled: true,
    sort: nextSort(homeDraft.value.modules),
    content: { level: "info", text: "" },
  });
  openEditor({ type: "home-module", index: homeDraft.value.modules.length - 1 });
}

function addQuickActionsModule() {
  homeDraft.value.modules.push({
    type: "quick_actions",
    key: `quick-actions-${Date.now()}`,
    enabled: true,
    sort: nextSort(homeDraft.value.modules),
    content: { items: [] },
  });
  openEditor({ type: "home-module", index: homeDraft.value.modules.length - 1 });
}

function addHomeModule(type: string, keyPrefix: string, content: JsonObject = {}) {
  homeDraft.value.modules.push({
    type,
    key: `${keyPrefix}-${Date.now()}`,
    enabled: true,
    sort: nextSort(homeDraft.value.modules),
    content,
  });
  openEditor({ type: "home-module", index: homeDraft.value.modules.length - 1 });
}

function addIntroModule() {
  addHomeModule("intro", "intro");
}

function addLocationsModule() {
  addHomeModule("locations", "locations");
}

function addFeaturedDepartmentsModule() {
  addHomeModule("featured_departments", "featured-departments", { limit: 12 });
}

function addStaticContentModule() {
  addHomeModule("static_content", "static-content");
}

function removeHomeModule(module: PatientHomeModule) {
  module.enabled = false;
}

function quickActionItems(module: PatientHomeModule) {
  if (!Array.isArray(module.content.items)) module.content.items = [];
  return module.content.items as RouteTargetConfig[];
}

function addQuickAction(module: PatientHomeModule) {
  const items = quickActionItems(module);
  items.push({ label: "新快捷入口", routeName: "patient-home", enabled: true, sort: nextSort(items) });
}

function removeQuickAction(module: PatientHomeModule, index: number) {
  quickActionItems(module)[index].enabled = false;
}

function addStaticPage() {
  staticDraft.value.pages.push({
    routeName: "service-internet-clinic",
    enabled: true,
    sort: nextSort(staticDraft.value.pages),
    label: "静态页",
    title: "新页面",
    intro: "",
    points: [],
    primary: { label: "返回首页", routeName: "patient-home" },
  });
  openEditor({ type: "static-page", index: staticDraft.value.pages.length - 1 });
}

function removeStaticPage(index: number) {
  staticDraft.value.pages[index].enabled = false;
}

function addPoint(page: StaticPageConfig) {
  page.points.push({ title: "新要点", text: "" });
}

function removePoint(page: StaticPageConfig, index: number) {
  page.points.splice(index, 1);
}

function addPrimary(page: StaticPageConfig) {
  page.primary = { label: "返回首页", routeName: "patient-home" };
}

function removePrimary(page: StaticPageConfig) {
  if (page.primary) page.primary.enabled = false;
}

onMounted(loadAll);
</script>

<template>
  <section class="patient-site-page">
    <div class="panel">
      <div class="panel-header patient-site-header">
        <div>
          <strong>患者端站点配置</strong>
          <p>维护患者端导航、首页模块和静态内容页配置。</p>
        </div>
      </div>

      <div class="patient-site-tabs" role="tablist" aria-label="配置类型">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeKey === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="patient-site-editor">
        <aside>
          <p class="eyebrow">{{ activeKey }}</p>
          <h2>{{ activeTab.label }}</h2>
          <p>{{ activeTab.description }}</p>

          <dl>
            <div>
              <dt>状态</dt>
              <dd>{{ activeRecord?.status || "未保存" }}</dd>
            </div>
            <div>
              <dt>版本</dt>
              <dd>{{ activeRecord?.version || "-" }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ activeRecord?.updatedAt || "-" }}</dd>
            </div>
          </dl>

          <div class="patient-site-mode-switch" role="group" aria-label="编辑模式">
            <button type="button" :class="{ active: activeMode === 'form' }" @click="setMode('form')">表单编辑</button>
            <button type="button" :class="{ active: activeMode === 'json' }" @click="setMode('json')">JSON 高级编辑</button>
          </div>

          <label>
            <span>本次备注</span>
            <input v-model.trim="remarks[activeKey]" type="text" placeholder="本次调整说明">
          </label>

          <div class="patient-site-side-actions">
            <button type="button" class="topbar-refresh" :disabled="loading" @click="loadConfig()">重新加载</button>
            <button type="button" class="topbar-refresh" @click="useTemplate">使用模板</button>
            <button type="button" class="topbar-refresh" :disabled="saving" @click="copyPublishedToDraft">复制发布为草稿</button>
            <button type="button" class="quick-btn" :disabled="saving" @click="saveDraft">保存草稿</button>
            <button type="button" class="quick-btn publish" :disabled="publishing" @click="requestPublish">发布</button>
          </div>

          <div v-if="publishConfirmOpen" class="patient-site-confirm">
            <strong>确认发布</strong>
            <p>配置类型：{{ activeTab.label }}</p>
            <p>当前草稿：{{ activeRecord?.status || "未保存" }} / v{{ activeRecord?.version || "-" }}</p>
            <p>本次备注：{{ remarks[activeKey] || "无" }}</p>
            <p class="patient-site-confirm-warning">发布后患者端将读取该配置。</p>
            <p>公开读取接口：<code>/api/patient-site/config</code></p>
            <div>
              <button type="button" class="quick-btn publish" :disabled="publishing" @click="confirmPublish">确认发布</button>
              <button type="button" class="topbar-refresh" :disabled="publishing" @click="publishConfirmOpen = false">取消</button>
            </div>
          </div>

          <div v-if="status" class="notice success">{{ status }}</div>
          <div v-if="error" class="notice error">{{ error }}</div>
          <div v-if="activeErrors.length" class="notice error">
            <strong>请先修正以下问题</strong>
            <ul>
              <li v-for="item in activeErrors" :key="item">{{ item }}</li>
            </ul>
          </div>
        </aside>

        <div class="patient-site-main">
          <template v-if="activeMode === 'form'">
            <template v-if="activeKey === 'patient_nav'">
              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>品牌信息</h3>
                    <p>患者端页头品牌和首页路由。</p>
                  </div>
                  <button type="button" class="topbar-refresh" @click="openEditor({ type: 'brand' })">编辑</button>
                </div>
                <article class="config-card config-summary-card">
                  <div>
                    <strong>{{ navDraft.brand.name }}</strong>
                    <p>{{ routeLabel(navDraft.brand.homeRoute) }} / {{ navDraft.brand.homeRoute }}</p>
                  </div>
                  <span class="status-pill enabled">生效</span>
                </article>
              </section>

              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>顶部菜单</h3>
                    <p>每个菜单点击后在弹窗中维护链接和 feature 入口。</p>
                  </div>
                  <button type="button" class="topbar-refresh" @click="addMenu">新增菜单</button>
                </div>
                <div class="config-card-grid">
                  <article v-for="(menu, menuIndex) in navDraft.menus" :key="menu.key || menuIndex" class="config-card config-summary-card">
                    <div>
                      <strong>{{ menu.label || "未命名菜单" }}</strong>
                      <p>{{ menu.key }} · {{ menu.links?.length || 0 }} 个链接 · sort {{ menu.sort ?? "-" }}</p>
                      <small>{{ menu.lead || menu.description || "未填写导语" }}</small>
                    </div>
                    <div class="config-card-actions">
                      <button type="button" class="status-pill" :class="menu.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(menu)">{{ menu.enabled === false ? "禁用" : "启用" }}</button>
                      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'nav-menu', index: menuIndex })">编辑</button>
                      <button type="button" class="danger-link" @click="removeMenu(menuIndex)">删除</button>
                    </div>
                  </article>
                </div>
              </section>

              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>用户菜单入口</h3>
                    <p>登录后用户下拉菜单中的入口。</p>
                  </div>
                  <button type="button" class="topbar-refresh" @click="addUserLink">新增入口</button>
                </div>
                <div class="config-list">
                  <article v-for="(link, linkIndex) in navDraft.userLinks" :key="`user-${linkIndex}`" class="config-row-card config-summary-row">
                    <div>
                      <strong>{{ link.label || "未命名入口" }}</strong>
                      <span>{{ routeLabel(link.routeName) }} / {{ link.routeName }} · sort {{ link.sort ?? "-" }}</span>
                    </div>
                    <div class="config-card-actions">
                      <button type="button" class="status-pill" :class="link.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(link)">{{ link.enabled === false ? "禁用" : "启用" }}</button>
                      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'user-link', index: linkIndex })">编辑</button>
                      <button type="button" class="danger-link" @click="removeUserLink(linkIndex)">删除</button>
                    </div>
                  </article>
                </div>
              </section>
            </template>

            <template v-else-if="activeKey === 'patient_home'">
              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>Hero</h3>
                    <p>首页首屏标题、按钮和启用状态。</p>
                  </div>
                  <button type="button" class="topbar-refresh" @click="openEditor({ type: 'home-hero' })">编辑</button>
                </div>
                <article class="config-card config-summary-card">
                  <div>
                    <strong>{{ homeDraft.hero.title }}</strong>
                    <p>{{ homeDraft.hero.eyebrow || "无 eyebrow" }}</p>
                    <small>{{ homeDraft.hero.primaryAction?.label }} / {{ homeDraft.hero.secondaryAction?.label }}</small>
                  </div>
                  <button type="button" class="status-pill" :class="homeDraft.hero.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(homeDraft.hero)">{{ homeDraft.hero.enabled === false ? "禁用" : "启用" }}</button>
                </article>
              </section>

              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>首页模块</h3>
                    <p>模块详情在弹窗中维护，复杂内容可切换 JSON 编辑。</p>
                  </div>
                  <div class="inline-actions">
                    <button type="button" class="topbar-refresh" @click="addNoticeModule">新增 notice</button>
                    <button type="button" class="topbar-refresh" @click="addQuickActionsModule">新增 quick_actions</button>
                    <button type="button" class="topbar-refresh" @click="addIntroModule">新增 intro</button>
                    <button type="button" class="topbar-refresh" @click="addLocationsModule">新增 locations</button>
                    <button type="button" class="topbar-refresh" @click="addFeaturedDepartmentsModule">新增 featured_departments</button>
                    <button type="button" class="topbar-refresh" @click="addStaticContentModule">新增 static_content</button>
                  </div>
                </div>
                <div class="config-card-grid">
                  <article v-for="(module, moduleIndex) in homeDraft.modules" :key="module.key || moduleIndex" class="config-card config-summary-card">
                    <div>
                      <strong>{{ module.type }} / {{ module.key }}</strong>
                      <p>{{ moduleSummary(module) }}</p>
                      <small>sort {{ module.sort ?? "-" }}</small>
                    </div>
                    <div class="config-card-actions">
                      <button type="button" class="status-pill" :class="module.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(module)">{{ module.enabled === false ? "禁用" : "启用" }}</button>
                      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'home-module', index: moduleIndex })">编辑</button>
                      <button type="button" class="danger-link" @click="removeHomeModule(module)">删除</button>
                    </div>
                  </article>
                </div>
              </section>
            </template>

            <template v-else>
              <section class="config-section">
                <div class="config-section-head">
                  <div>
                    <h3>静态页面</h3>
                    <p>按患者端 routeName 匹配的内容页。</p>
                  </div>
                  <button type="button" class="topbar-refresh" @click="addStaticPage">新增页面</button>
                </div>
                <div class="static-page-tools">
                  <label>
                    <span>搜索页面</span>
                    <input v-model.trim="staticSearch" type="search" placeholder="routeName / label / title">
                  </label>
                  <label class="check-field compact">
                    <input v-model="staticDisabledOnly" type="checkbox">
                    <span>仅看停用项</span>
                  </label>
                </div>
                <div class="config-card-grid">
                  <article v-for="{ page, index } in filteredStaticPages" :key="`${page.routeName}-${index}`" class="config-card config-summary-card">
                    <div>
                      <strong>{{ page.title || "未命名页面" }}</strong>
                      <p>{{ page.routeName }} · {{ page.label || "无分组" }} · {{ page.points?.length || 0 }} 个要点</p>
                      <small>{{ page.intro || "未填写说明" }}</small>
                    </div>
                    <div class="config-card-actions">
                      <button type="button" class="status-pill" :class="page.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(page)">{{ page.enabled === false ? "禁用" : "启用" }}</button>
                      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'static-page', index })">编辑</button>
                      <button type="button" class="danger-link" @click="removeStaticPage(index)">删除</button>
                    </div>
                  </article>
                </div>
              </section>
            </template>
          </template>

          <template v-else-if="activeMode === 'json'">
            <textarea
              v-model="editors[activeKey]"
              spellcheck="false"
              :aria-label="`${activeTab.label} JSON`"
            />
          </template>

          <template v-else-if="activeKey === 'patient_nav'">
            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>品牌</h3>
                  <p>控制患者端页头品牌名称和点击品牌后的目标路由。</p>
                </div>
              </div>
              <div class="config-grid two">
                <label><span>brand.name</span><input v-model.trim="navDraft.brand.name" type="text"></label>
                <label>
                  <span>brand.homeRoute</span>
                  <select v-model="navDraft.brand.homeRoute">
                    <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                  </select>
                </label>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>菜单列表</h3>
                  <p>配置顶部导航下拉菜单及菜单内链接。</p>
                </div>
                <button type="button" class="topbar-refresh" @click="addMenu">新增菜单</button>
              </div>

              <article v-for="(menu, menuIndex) in navDraft.menus" :key="menu.key || menuIndex" class="config-card">
                <div class="config-card-head">
                  <strong>菜单 {{ menuIndex + 1 }}</strong>
                  <button type="button" class="danger-link" @click="removeMenu(menuIndex)">删除菜单</button>
                </div>
                <div class="config-grid four">
                  <label><span>key</span><input v-model.trim="menu.key" type="text"></label>
                  <label><span>label</span><input v-model.trim="menu.label" type="text"></label>
                  <label><span>sort</span><input v-model.number="menu.sort" type="number"></label>
                  <label class="check-field"><input v-model="menu.enabled" type="checkbox"><span>enabled</span></label>
                </div>
                <div class="config-grid two">
                  <label><span>lead</span><input v-model.trim="menu.lead" type="text"></label>
                  <label><span>description</span><input v-model.trim="menu.description" type="text"></label>
                </div>

                <div class="nested-list">
                  <div class="nested-list-head">
                    <strong>菜单链接</strong>
                    <button type="button" class="topbar-refresh" @click="addMenuLink(menu)">新增链接</button>
                  </div>
                  <div v-for="(link, linkIndex) in menu.links" :key="`${menu.key}-${linkIndex}`" class="config-row-card">
                    <div class="config-grid five">
                      <label><span>label</span><input v-model.trim="link.label" type="text"></label>
                      <label>
                        <span>routeName</span>
                        <select v-model="link.routeName">
                          <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                        </select>
                      </label>
                      <label><span>description</span><input v-model.trim="link.description" type="text"></label>
                      <label><span>sort</span><input v-model.number="link.sort" type="number"></label>
                      <label class="check-field"><input v-model="link.enabled" type="checkbox"><span>enabled</span></label>
                    </div>
                    <button type="button" class="danger-link" @click="removeMenuLink(menu, linkIndex)">删除链接</button>
                  </div>
                </div>

                <div class="nested-list">
                  <div class="nested-list-head">
                    <strong>feature</strong>
                    <button v-if="!menu.feature" type="button" class="topbar-refresh" @click="addMenuFeature(menu)">添加特色入口</button>
                    <button v-else type="button" class="danger-link" @click="removeMenuFeature(menu)">删除特色入口</button>
                  </div>
                  <div v-if="menu.feature" class="config-grid three">
                    <label><span>feature.label</span><input v-model.trim="menu.feature.label" type="text"></label>
                    <label>
                      <span>feature.routeName</span>
                      <select v-model="menu.feature.routeName">
                        <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                      </select>
                    </label>
                    <label><span>feature.sort</span><input v-model.number="menu.feature.sort" type="number"></label>
                  </div>
                </div>
              </article>
            </section>

            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>用户菜单链接</h3>
                  <p>登录后用户名下拉菜单中的入口。</p>
                </div>
                <button type="button" class="topbar-refresh" @click="addUserLink">新增用户链接</button>
              </div>
              <div v-for="(link, linkIndex) in navDraft.userLinks" :key="`user-${linkIndex}`" class="config-row-card">
                <div class="config-grid four">
                  <label><span>label</span><input v-model.trim="link.label" type="text"></label>
                  <label>
                    <span>routeName</span>
                    <select v-model="link.routeName">
                      <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                    </select>
                  </label>
                  <label><span>sort</span><input v-model.number="link.sort" type="number"></label>
                  <label class="check-field"><input v-model="link.enabled" type="checkbox"><span>enabled</span></label>
                </div>
                <button type="button" class="danger-link" @click="removeUserLink(linkIndex)">删除</button>
              </div>
            </section>
          </template>

          <template v-else-if="activeKey === 'patient_home'">
            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>Hero</h3>
                  <p>控制首页首屏文案和主操作。</p>
                </div>
              </div>
              <div class="config-grid two">
                <label><span>hero.eyebrow</span><input v-model.trim="homeDraft.hero.eyebrow" type="text"></label>
                <label><span>hero.title</span><input v-model.trim="homeDraft.hero.title" type="text"></label>
                <label class="check-field"><input v-model="homeDraft.hero.enabled" type="checkbox"><span>hero.enabled</span></label>
              </div>
              <div class="config-grid two">
                <label><span>primaryAction.label</span><input v-model.trim="homeDraft.hero.primaryAction.label" type="text"></label>
                <label>
                  <span>primaryAction.routeName</span>
                  <select v-model="homeDraft.hero.primaryAction.routeName">
                    <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                  </select>
                </label>
                <label><span>secondaryAction.label</span><input v-model.trim="homeDraft.hero.secondaryAction.label" type="text"></label>
                <label>
                  <span>secondaryAction.routeName</span>
                  <select v-model="homeDraft.hero.secondaryAction.routeName">
                    <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                  </select>
                </label>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>模块列表</h3>
                  <p>当前表单支持 notice 和 quick_actions，其他模块可在 JSON 高级模式维护。</p>
                </div>
                <div class="inline-actions">
                  <button type="button" class="topbar-refresh" @click="addNoticeModule">新增 notice</button>
                  <button type="button" class="topbar-refresh" @click="addQuickActionsModule">新增 quick_actions</button>
                </div>
              </div>

              <article v-for="module in noticeModules" :key="module.key" class="config-card">
                <div class="config-card-head">
                  <strong>notice / {{ module.key }}</strong>
                  <button type="button" class="danger-link" @click="removeHomeModule(module)">删除模块</button>
                </div>
                <div class="config-grid five">
                  <label><span>key</span><input v-model.trim="module.key" type="text"></label>
                  <label><span>sort</span><input v-model.number="module.sort" type="number"></label>
                  <label><span>content.level</span><input v-model.trim="module.content.level" type="text"></label>
                  <label><span>content.text</span><input v-model.trim="module.content.text" type="text"></label>
                  <label class="check-field"><input v-model="module.enabled" type="checkbox"><span>enabled</span></label>
                </div>
              </article>

              <article v-for="module in quickActionModules" :key="module.key" class="config-card">
                <div class="config-card-head">
                  <strong>quick_actions / {{ module.key }}</strong>
                  <button type="button" class="danger-link" @click="removeHomeModule(module)">删除模块</button>
                </div>
                <div class="config-grid three">
                  <label><span>key</span><input v-model.trim="module.key" type="text"></label>
                  <label><span>sort</span><input v-model.number="module.sort" type="number"></label>
                  <label class="check-field"><input v-model="module.enabled" type="checkbox"><span>enabled</span></label>
                </div>
                <div class="nested-list">
                  <div class="nested-list-head">
                    <strong>快捷入口</strong>
                    <button type="button" class="topbar-refresh" @click="addQuickAction(module)">新增 quick action</button>
                  </div>
                  <div v-for="(item, itemIndex) in quickActionItems(module)" :key="`${module.key}-${itemIndex}`" class="config-row-card">
                    <div class="config-grid four">
                      <label><span>label</span><input v-model.trim="item.label" type="text"></label>
                      <label>
                        <span>routeName</span>
                        <select v-model="item.routeName">
                          <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                        </select>
                      </label>
                      <label><span>sort</span><input v-model.number="item.sort" type="number"></label>
                      <label class="check-field"><input v-model="item.enabled" type="checkbox"><span>enabled</span></label>
                    </div>
                    <button type="button" class="danger-link" @click="removeQuickAction(module, itemIndex)">删除</button>
                  </div>
                </div>
              </article>

              <p v-if="homeDraft.modules.some((module) => module.type !== 'notice' && module.type !== 'quick_actions')" class="muted-hint">
                存在未在表单中展示的模块，请使用 JSON 高级模式维护。
              </p>
            </section>
          </template>

          <template v-else>
            <section class="config-section">
              <div class="config-section-head">
                <div>
                  <h3>静态页面</h3>
                  <p>按患者端 routeName 匹配静态内容页。</p>
                </div>
                <button type="button" class="topbar-refresh" @click="addStaticPage">新增页面</button>
              </div>

              <article v-for="(page, pageIndex) in staticDraft.pages" :key="`${page.routeName}-${pageIndex}`" class="config-card">
                <div class="config-card-head">
                  <strong>页面 {{ pageIndex + 1 }}</strong>
                  <button type="button" class="danger-link" @click="removeStaticPage(pageIndex)">删除页面</button>
                </div>
                <div class="config-grid four">
                  <label>
                    <span>routeName</span>
                    <select v-model="page.routeName">
                      <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                    </select>
                  </label>
                  <label><span>sort</span><input v-model.number="page.sort" type="number"></label>
                  <label><span>label</span><input v-model.trim="page.label" type="text"></label>
                  <label class="check-field"><input v-model="page.enabled" type="checkbox"><span>enabled</span></label>
                </div>
                <div class="config-grid two">
                  <label><span>title</span><input v-model.trim="page.title" type="text"></label>
                  <label><span>intro</span><input v-model.trim="page.intro" type="text"></label>
                </div>

                <div class="nested-list">
                  <div class="nested-list-head">
                    <strong>points</strong>
                    <button type="button" class="topbar-refresh" @click="addPoint(page)">新增 point</button>
                  </div>
                  <div v-for="(point, pointIndex) in page.points" :key="`${pageIndex}-${pointIndex}`" class="config-row-card">
                    <div class="config-grid two">
                      <label><span>point.title</span><input v-model.trim="point.title" type="text"></label>
                      <label><span>point.text</span><input v-model.trim="point.text" type="text"></label>
                    </div>
                    <button type="button" class="danger-link" @click="removePoint(page, pointIndex)">删除</button>
                  </div>
                </div>

                <div class="nested-list">
                  <div class="nested-list-head">
                    <strong>primary</strong>
                    <button v-if="!page.primary" type="button" class="topbar-refresh" @click="addPrimary(page)">添加主按钮</button>
                    <button v-else type="button" class="danger-link" @click="removePrimary(page)">删除主按钮</button>
                  </div>
                  <div v-if="page.primary" class="config-grid two">
                    <label><span>primary.label</span><input v-model.trim="page.primary.label" type="text"></label>
                    <label>
                      <span>primary.routeName</span>
                      <select v-model="page.primary.routeName">
                        <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                      </select>
                    </label>
                  </div>
                </div>
              </article>
            </section>
          </template>
        </div>
      </div>
    </div>

    <div v-if="editorOpen" class="patient-config-modal-backdrop" @click.self="closeEditor">
      <section class="patient-config-modal-card" role="dialog" aria-modal="true" :aria-label="editorTitle">
        <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="closeEditor">×</button>
        <header class="patient-config-modal-head">
          <h2>{{ editorTitle }}</h2>
          <p>{{ editorDescription }}</p>
        </header>

        <div v-if="editingTarget && editingDraft" class="patient-config-modal">
        <template v-if="editingTarget.type === 'brand'">
          <div class="config-grid two">
            <label><span>brand.name</span><input v-model.trim="editingDraft.name" type="text"></label>
            <label>
              <span>brand.homeRoute</span>
              <select v-model="editingDraft.homeRoute">
                <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
              </select>
            </label>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'nav-menu'">
          <div class="config-grid four">
            <label><span>key</span><input v-model.trim="editingDraft.key" type="text"></label>
            <label><span>label</span><input v-model.trim="editingDraft.label" type="text"></label>
            <label><span>sort</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>enabled</span></label>
          </div>
          <div class="config-grid two">
            <label><span>lead</span><input v-model.trim="editingDraft.lead" type="text"></label>
            <label><span>description</span><input v-model.trim="editingDraft.description" type="text"></label>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>菜单链接</strong>
              <button type="button" class="topbar-refresh" @click="addEditingLink">新增链接</button>
            </div>
            <div v-for="(link, linkIndex) in editingDraft.links" :key="`editing-link-${linkIndex}`" class="config-row-card">
              <div class="config-grid five">
                <label><span>label</span><input v-model.trim="link.label" type="text"></label>
                <label>
                  <span>routeName</span>
                  <select v-model="link.routeName">
                    <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                  </select>
                </label>
                <label><span>description</span><input v-model.trim="link.description" type="text"></label>
                <label><span>sort</span><input v-model.number="link.sort" type="number"></label>
                <label class="check-field"><input v-model="link.enabled" type="checkbox"><span>enabled</span></label>
              </div>
              <button type="button" class="danger-link" @click="editingDraft.links[linkIndex].enabled = false">删除</button>
            </div>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>feature</strong>
              <button v-if="!editingDraft.feature" type="button" class="topbar-refresh" @click="editingDraft.feature = { label: '特色入口', routeName: 'patient-home', enabled: true, sort: 0 }">添加入口</button>
              <button v-else type="button" class="danger-link" @click="editingDraft.feature.enabled = false">删除入口</button>
            </div>
            <div v-if="editingDraft.feature" class="config-grid three">
              <label><span>feature.label</span><input v-model.trim="editingDraft.feature.label" type="text"></label>
              <label>
                <span>feature.routeName</span>
                <select v-model="editingDraft.feature.routeName">
                  <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                </select>
              </label>
              <label><span>feature.sort</span><input v-model.number="editingDraft.feature.sort" type="number"></label>
            </div>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'user-link'">
          <div class="config-grid four">
            <label><span>label</span><input v-model.trim="editingDraft.label" type="text"></label>
            <label>
              <span>routeName</span>
              <select v-model="editingDraft.routeName">
                <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
              </select>
            </label>
            <label><span>sort</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>enabled</span></label>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'home-hero'">
          <div class="config-grid two">
            <label><span>hero.eyebrow</span><input v-model.trim="editingDraft.eyebrow" type="text"></label>
            <label><span>hero.title</span><input v-model.trim="editingDraft.title" type="text"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>hero.enabled</span></label>
          </div>
          <div class="config-grid two">
            <label><span>primaryAction.label</span><input v-model.trim="editingDraft.primaryAction.label" type="text"></label>
            <label>
              <span>primaryAction.routeName</span>
              <select v-model="editingDraft.primaryAction.routeName">
                <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
              </select>
            </label>
            <label><span>secondaryAction.label</span><input v-model.trim="editingDraft.secondaryAction.label" type="text"></label>
            <label>
              <span>secondaryAction.routeName</span>
              <select v-model="editingDraft.secondaryAction.routeName">
                <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
              </select>
            </label>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'home-module'">
          <div class="config-grid four">
            <label><span>type</span><input v-model.trim="editingDraft.type" type="text"></label>
            <label><span>key</span><input v-model.trim="editingDraft.key" type="text"></label>
            <label><span>sort</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>enabled</span></label>
          </div>
          <div v-if="editingDraft.type === 'notice'" class="config-grid two">
            <label><span>content.level</span><input v-model.trim="editingDraft.content.level" type="text"></label>
            <label><span>content.text</span><input v-model.trim="editingDraft.content.text" type="text"></label>
          </div>
          <div v-else-if="editingDraft.type === 'quick_actions'" class="nested-list">
            <div class="nested-list-head">
              <strong>快捷入口</strong>
              <button type="button" class="topbar-refresh" @click="addEditingQuickAction">新增 quick action</button>
            </div>
            <div v-for="(item, itemIndex) in editingContentItems()" :key="`editing-action-${itemIndex}`" class="config-row-card">
              <div class="config-grid four">
                <label><span>label</span><input v-model.trim="item.label" type="text"></label>
                <label>
                  <span>routeName</span>
                  <select v-model="item.routeName">
                    <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                  </select>
                </label>
                <label><span>sort</span><input v-model.number="item.sort" type="number"></label>
                <label class="check-field"><input v-model="item.enabled" type="checkbox"><span>enabled</span></label>
              </div>
              <button type="button" class="danger-link" @click="editingContentItems()[itemIndex].enabled = false">删除</button>
            </div>
          </div>
          <label v-else>
            <span>content JSON</span>
            <textarea v-model="editingContentText" spellcheck="false" rows="12"></textarea>
          </label>
        </template>

        <template v-else-if="editingTarget.type === 'static-page'">
          <div class="config-grid four">
            <label>
              <span>routeName</span>
              <select v-model="editingDraft.routeName">
                <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
              </select>
            </label>
            <label><span>sort</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label><span>label</span><input v-model.trim="editingDraft.label" type="text"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>enabled</span></label>
          </div>
          <div class="config-grid two">
            <label><span>title</span><input v-model.trim="editingDraft.title" type="text"></label>
            <label><span>intro</span><input v-model.trim="editingDraft.intro" type="text"></label>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>points</strong>
              <button type="button" class="topbar-refresh" @click="addEditingPoint">新增 point</button>
            </div>
            <div v-for="(point, pointIndex) in editingDraft.points" :key="`editing-point-${pointIndex}`" class="config-row-card">
              <div class="config-grid two">
                <label><span>point.title</span><input v-model.trim="point.title" type="text"></label>
                <label><span>point.text</span><input v-model.trim="point.text" type="text"></label>
              </div>
              <button type="button" class="danger-link" @click="editingDraft.points.splice(pointIndex, 1)">删除</button>
            </div>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>primary</strong>
              <button v-if="!editingDraft.primary" type="button" class="topbar-refresh" @click="editingDraft.primary = { label: '返回首页', routeName: 'patient-home' }">添加主按钮</button>
              <button v-else type="button" class="danger-link" @click="editingDraft.primary.enabled = false">删除主按钮</button>
            </div>
            <div v-if="editingDraft.primary" class="config-grid two">
              <label><span>primary.label</span><input v-model.trim="editingDraft.primary.label" type="text"></label>
              <label>
                <span>primary.routeName</span>
                <select v-model="editingDraft.primary.routeName">
                  <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
                </select>
              </label>
            </div>
          </div>
        </template>
        </div>
        <div class="patient-config-modal-footer">
          <button type="button" class="topbar-refresh" @click="closeEditor">取消</button>
          <button type="button" class="quick-btn" @click="applyEditor">保存到草稿</button>
        </div>
      </section>
    </div>
  </section>
</template>
