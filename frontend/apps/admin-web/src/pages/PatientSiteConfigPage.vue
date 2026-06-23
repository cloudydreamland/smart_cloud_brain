<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { api, ApiError, type DataRow, useAuthStore } from "@smart-cloud-brain/shared-api";
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

const tabs: ConfigTab[] = [
  { key: "patient_nav", label: "导航配置", description: "患者端顶部导航、下拉入口和登录后用户菜单。" },
  { key: "patient_home", label: "首页配置", description: "首页 hero、notice 和 quick actions 模块。" },
  { key: "patient_static_pages", label: "静态页配置", description: "按 routeName 匹配的内容页标题、说明、要点和主按钮。" },
];

const configKeys = new Set<ConfigKey>(tabs.map((tab) => tab.key));
const allowedHomeModules = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

const templates: ConfigDrafts = {
  patient_nav: {
    brand: { name: "智慧云脑", homeRoute: "patient-home" },
    menus: [
      {
        key: "care",
        label: "医疗服务",
        enabled: true,
        sort: 10,
        lead: "从需要帮助到获得照护",
        description: "分诊、挂号、医生科室、检查预约等入口。",
        links: [
          { label: "在线挂号", routeName: "patient-doctors", description: "查看可预约号源", enabled: true, sort: 10 },
          { label: "AI 智能分诊", routeName: "patient-triage", description: "获得科室建议", enabled: true, sort: 20 },
        ],
        feature: { label: "查看医疗服务", routeName: "service-internet-clinic" },
      },
    ],
    userLinks: [
      { label: "我的预约", routeName: "patient-appointments", enabled: true, sort: 10 },
      { label: "我的病历", routeName: "patient-records", enabled: true, sort: 20 },
    ],
  },
  patient_home: {
    hero: {
      eyebrow: "智慧云脑医疗服务",
      title: "专业照护，从清晰入口开始",
      primaryAction: { label: "预约就诊", routeName: "patient-doctors" },
      secondaryAction: { label: "AI 智能分诊", routeName: "patient-triage" },
      enabled: true,
    },
    modules: [
      {
        type: "notice",
        key: "emergency_notice",
        enabled: true,
        sort: 10,
        content: { level: "warning", text: "如出现胸痛、呼吸困难、意识改变等情况，请立即前往急诊。" },
      },
      {
        type: "quick_actions",
        key: "quick_actions",
        enabled: true,
        sort: 20,
        content: {
          items: [
            { label: "在线挂号", routeName: "patient-doctors", sort: 10 },
            { label: "AI 分诊", routeName: "patient-triage", sort: 20 },
          ],
        },
      },
    ],
  },
  patient_static_pages: {
    pages: [
      {
        routeName: "service-internet-clinic",
        label: "医疗服务",
        title: "互联网门诊",
        intro: "面向复诊、病情咨询和诊后随访场景。",
        enabled: true,
        sort: 10,
        points: [{ title: "适用场景", text: "复诊咨询、检查结果解读、慢病随访和用药问题整理。" }],
        primary: { label: "先做 AI 智能分诊", routeName: "patient-triage" },
      },
    ],
  },
};

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
  if (key === "patient_nav") return normalizeNav(value);
  if (key === "patient_home") return normalizeHome(value);
  return normalizeStaticPages(value);
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

async function loadConfig(key = activeKey.value) {
  if (!auth.session) return;
  loading.value = true;
  error.value = "";
  status.value = "";
  validationErrors[key] = [];
  publishConfirmOpen.value = false;
  try {
    const row = await api.adminPatientSiteConfig(auth.token(), key);
    latest[key] = row;
    const configJson = typeof row.configJson === "string" && row.configJson.trim() ? JSON.parse(row.configJson) : templates[key];
    setDraft(key, configJson);
    remarks[key] = typeof row.remark === "string" ? row.remark : "";
    status.value = "配置已加载";
  } catch (err) {
    latest[key] = null;
    setDraft(key, templates[key]);
    remarks[key] = "";
    status.value = "配置接口暂不可用，已显示本地模板";
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
  setDraft(activeKey.value, templates[activeKey.value]);
  status.value = "已填入默认模板，尚未保存";
  error.value = "";
  validationErrors[activeKey.value] = [];
  publishConfirmOpen.value = false;
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
    const history = await api.patientSiteConfigHistory(auth.token(), key);
    const published = history.find((item) => item.status === "PUBLISHED");
    if (!published || typeof published.configJson !== "string") {
      validationErrors[key] = ["没有找到当前已发布配置，无法复制为草稿。"];
      return;
    }
    const parsed = JSON.parse(published.configJson);
    setDraft(key, parsed);
    remarks[key] = `从已发布版本 v${String(published.version || "-")} 复制`;
    const payload = preparePayload(key);
    if (!payload) return;
    await saveDraftRecord(payload.configJson);
    status.value = "已复制当前发布配置为草稿";
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
    status.value = "草稿已保存";
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
    status.value = "配置已发布";
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
}

function removeMenu(index: number) {
  navDraft.value.menus.splice(index, 1);
}

function addMenuLink(menu: PatientNavMenu) {
  menu.links.push({ label: "新链接", routeName: "patient-home", description: "", enabled: true, sort: nextSort(menu.links) });
}

function removeMenuLink(menu: PatientNavMenu, index: number) {
  menu.links.splice(index, 1);
}

function addMenuFeature(menu: PatientNavMenu) {
  menu.feature = { label: "特色入口", routeName: "patient-home", enabled: true, sort: 0 };
}

function removeMenuFeature(menu: PatientNavMenu) {
  delete menu.feature;
}

function addUserLink() {
  navDraft.value.userLinks.push({ label: "新用户入口", routeName: "patient-dashboard", enabled: true, sort: nextSort(navDraft.value.userLinks) });
}

function removeUserLink(index: number) {
  navDraft.value.userLinks.splice(index, 1);
}

function addNoticeModule() {
  homeDraft.value.modules.push({
    type: "notice",
    key: `notice-${Date.now()}`,
    enabled: true,
    sort: nextSort(homeDraft.value.modules),
    content: { level: "info", text: "" },
  });
}

function addQuickActionsModule() {
  homeDraft.value.modules.push({
    type: "quick_actions",
    key: `quick-actions-${Date.now()}`,
    enabled: true,
    sort: nextSort(homeDraft.value.modules),
    content: { items: [] },
  });
}

function removeHomeModule(module: PatientHomeModule) {
  const index = homeDraft.value.modules.indexOf(module);
  if (index >= 0) homeDraft.value.modules.splice(index, 1);
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
  quickActionItems(module).splice(index, 1);
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
}

function removeStaticPage(index: number) {
  staticDraft.value.pages.splice(index, 1);
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
  delete page.primary;
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
          <template v-if="activeMode === 'json'">
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
  </section>
</template>
