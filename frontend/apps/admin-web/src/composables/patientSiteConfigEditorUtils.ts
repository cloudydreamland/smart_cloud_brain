import {
  api,
  ApiError,
  isPatientSiteSectionType,
  resolvePatientSiteConfigSection,
  validatePatientSitePagesConfig,
  type DataRow,
  type PatientFooterConfig,
  type PatientHomeConfig,
  type PatientHomeModule,
  type PatientHospitalInfoConfig,
  type PatientNavConfig,
  type PatientNavMenu,
  type PatientSiteConfigHistoryPage,
  type PatientSiteConfigKey,
  type PatientSiteConfigRecord,
  type PatientStaticPagesConfig,
  type RouteTargetConfig,
  type StaticPageConfig,
} from "@smart-cloud-brain/shared-api";
import { isAllowedPatientRoute, patientRouteOptions } from "../patientSiteRoutes";
import type { EditingTarget } from "./usePatientSiteConfigEditor";

const legacyHomeModuleTypes = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

export function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

export function emptyHistoryPage(pageSize: number): PatientSiteConfigHistoryPage {
  return { items: [], page: 1, pageSize, total: 0, totalPages: 1 };
}

export function isRow(value: unknown): value is DataRow {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

export function stringValue(value: unknown, fallback: string) {
  return typeof value === "string" ? value : fallback;
}

export function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

export function routeValue(value: unknown, fallback: string) {
  return typeof value === "string" ? value : fallback;
}

export function nextSort(items: Array<{ sort?: number }>) {
  return items.reduce((value, item) => Math.max(value, numberValue(item.sort, 0)), 0) + 10;
}

export function moveItem<T>(items: T[], fromIndex: number, toIndex: number) {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= items.length || toIndex >= items.length) return;
  const [item] = items.splice(fromIndex, 1);
  items.splice(toIndex, 0, item);
}

export function resequenceSort(items: Array<{ sort?: number }>) {
  items.forEach((item, index) => {
    item.sort = (index + 1) * 10;
  });
}

export function messageFrom(err: unknown) {
  return err instanceof ApiError ? err.message : err instanceof Error ? err.message : "操作失败";
}

export function loadMessageFrom(err: unknown) {
  const message = messageFrom(err);
  if (/internal server error/i.test(message)) return "配置表可能尚未初始化，请执行数据库迁移或重新初始化演示库。";
  return `${message}。请确认患者端配置接口和数据库迁移已正常运行。`;
}

export async function loadHistory(key: PatientSiteConfigKey, page: number, pageSize: number): Promise<PatientSiteConfigHistoryPage> {
  try {
    const raw = await api.patientSiteConfigHistory(key, page, pageSize);
    if (Array.isArray(raw)) {
      return { items: raw as PatientSiteConfigRecord[], page, pageSize, total: raw.length, totalPages: Math.max(1, Math.ceil(raw.length / pageSize)) };
    }
    const row = raw as Record<string, unknown>;
    if (row && typeof row === "object" && Array.isArray(row.items)) return raw as PatientSiteConfigHistoryPage;
    return emptyHistoryPage(pageSize);
  } catch {
    return emptyHistoryPage(pageSize);
  }
}

export function filterStaticPages(pages: StaticPageConfig[], search: string, disabledOnly: boolean) {
  const keyword = search.trim().toLowerCase();
  return pages
    .map((page, index) => ({ page, index }))
    .filter(({ page }) => !disabledOnly || page.enabled === false)
    .filter(({ page }) => !keyword || [page.routeName, page.label, page.title].some((value) => String(value || "").toLowerCase().includes(keyword)));
}

export function editorText(target: EditingTarget | null, field: "title" | "description") {
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

export function normalizeNav(value: unknown): PatientNavConfig {
  return resolvePatientSiteConfigSection("patient_nav", value, { preserveDisabled: true }) as PatientNavConfig;
}

export function normalizeNavMenu(value: unknown, index = 0): PatientNavMenu {
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

export function normalizeHome(value: unknown): PatientHomeConfig {
  return resolvePatientSiteConfigSection("patient_home", value, { preserveDisabled: true }) as PatientHomeConfig;
}

export function normalizeHomeModule(value: unknown, index = 0): PatientHomeModule {
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
  const moduleContent = module.content ?? {};
  if (type === "quick_actions") moduleContent.items = Array.isArray(content.items) ? content.items.map((item) => normalizeLink(item)) : [];
  if (type === "notice") {
    moduleContent.level = stringValue(content.level, "warning");
    moduleContent.text = stringValue(content.text, "");
  }
  module.content = moduleContent;
  return module;
}

export function normalizeStaticPage(value: unknown, index = 0): StaticPageConfig {
  const row = isRow(value) ? value : {};
  return {
    routeName: routeValue(row.routeName, "patient-home"),
    enabled: row.enabled !== false,
    sort: numberValue(row.sort, index * 10),
    label: stringValue(row.label, ""),
    title: stringValue(row.title, ""),
    intro: stringValue(row.intro, ""),
    contentSource: row.contentSource === "cms-page" ? "cms-page" : "static",
    slug: typeof row.slug === "string" && row.slug.trim() ? row.slug.trim().toLowerCase() : undefined,
    points: Array.isArray(row.points) ? row.points.map(normalizePoint) : [],
    primary: isRow(row.primary) ? normalizeLink(row.primary) : undefined,
  };
}

export function normalizePoint(value: unknown) {
  const row = isRow(value) ? value : {};
  return { title: stringValue(row.title, ""), text: stringValue(row.text, "") };
}

export function normalizeLink(value: unknown): RouteTargetConfig {
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

export function sanitizeBeforeSubmit(key: PatientSiteConfigKey, value: unknown) {
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

export function validateConfig(key: PatientSiteConfigKey, value: unknown, allowedHomeModules: Set<string>) {
  const errors: string[] = [];
  collectInvalidRoutes(value, errors);
  if (key === "patient_nav") validateNav(value as PatientNavConfig, errors);
  if (key === "patient_home") validateHome(value as PatientHomeConfig, errors, allowedHomeModules);
  if (key === "patient_static_pages") validateStaticPages(value as PatientStaticPagesConfig, errors);
  if (key === "patient_pages") errors.push(...validatePatientSitePagesConfig(value as never));
  if (key === "patient_hospital_info") validateHospitalInfo(value as PatientHospitalInfoConfig, errors);
  if (key === "patient_footer") validateFooter(value as PatientFooterConfig, errors);
  return errors;
}

export function moduleSummary(module: PatientHomeModule) {
  if (module.type === "notice") return String(module.content?.text || "未填写通知内容");
  if (module.type === "quick_actions") return `${Array.isArray(module.content?.items) ? module.content.items.length : 0} 个快捷入口`;
  return Object.keys(module.content || {}).length ? "内容已配置" : "暂无内容";
}

export function routeLabel(routeName = "") {
  return patientRouteOptions.find((route) => route.name === routeName)?.label || routeName || "-";
}

export function patientPreviewOrigin() {
  const configured = String(import.meta.env.VITE_PATIENT_WEB_ORIGIN || "").trim();
  if (configured) return configured.replace(/\/$/, "");
  if (typeof window === "undefined") return "";
  if (window.location.port === "5175") return `${window.location.protocol}//${window.location.hostname}:5173`;
  return window.location.origin;
}

function isStringMap(value: unknown): value is Record<string, string> {
  return isRow(value) && Object.values(value).every((item) => typeof item === "string");
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

function validateHome(home: PatientHomeConfig, errors: string[], allowedHomeModules: Set<string>) {
  if (home.hero?.enabled !== false) {
    requireText(home.hero?.title, "hero.title 不能为空", errors);
    if (home.hero?.primaryAction) validateLink(home.hero.primaryAction, "hero.primaryAction", errors);
    if (home.hero?.secondaryAction) validateLink(home.hero.secondaryAction, "hero.secondaryAction", errors);
  }
  home.modules.forEach((module, moduleIndex) => {
    requireText(module.type, `modules[${moduleIndex}].type 不能为空`, errors);
    requireText(module.key, `modules[${moduleIndex}].key 不能为空`, errors);
    if (module.type && !allowedHomeModules.has(module.type)) errors.push(`modules[${moduleIndex}].type 不支持：${module.type}`);
    if (module.type === "notice" && module.enabled !== false) requireText(module.content?.text, `modules[${moduleIndex}].content.text 不能为空`, errors);
    if (module.type === "quick_actions") {
      const items = Array.isArray(module.content?.items) ? module.content.items : [];
      items.forEach((item, itemIndex) => validateLink(item as RouteTargetConfig, `modules[${moduleIndex}].content.items[${itemIndex}]`, errors));
    }
    if (module.type && isPatientSiteSectionType(module.type) && !legacyHomeModuleTypes.has(module.type)) {
      const sectionErrors = validatePatientSitePagesConfig({
        pages: [{
          routeName: "about-hospital",
          label: "首页模块",
          title: "首页模块",
          intro: "",
          sections: [{ ...module.content, type: module.type, id: module.key, sort: module.sort, enabled: module.enabled } as never],
        }],
      });
      errors.push(...sectionErrors.map((item) => `modules[${moduleIndex}].content.${item}`));
    }
  });
}

function validateStaticPages(staticPages: PatientStaticPagesConfig, errors: string[]) {
  staticPages.pages.forEach((page, pageIndex) => {
    requireRoute(page.routeName, `pages[${pageIndex}].routeName`, errors);
    requireText(page.label, `pages[${pageIndex}].label 不能为空`, errors);
    requireText(page.title, `pages[${pageIndex}].title 不能为空`, errors);
    if (page.contentSource === "cms-page") requireText(page.slug, `pages[${pageIndex}].slug 在绑定 CMS 页面时必填`, errors);
    page.points.forEach((point, pointIndex) => {
      requireText(point.title, `pages[${pageIndex}].points[${pointIndex}].title 不能为空`, errors);
      requireText(point.text, `pages[${pageIndex}].points[${pointIndex}].text 不能为空`, errors);
    });
    if (page.primary) validateLink(page.primary, `pages[${pageIndex}].primary`, errors);
  });
}

function validateHospitalInfo(hospitalInfo: PatientHospitalInfoConfig, errors: string[]) {
  requireText(hospitalInfo.name, "name 不能为空", errors);
  hospitalInfo.locations.forEach((location, index) => {
    if (location.enabled === false) return;
    requireText(location.name, `locations[${index}].name 不能为空`, errors);
    requireText(location.address, `locations[${index}].address 不能为空`, errors);
  });
}

function validateFooter(footer: PatientFooterConfig, errors: string[]) {
  requireText(footer.brandName, "brandName 不能为空", errors);
  footer.links.forEach((link, index) => validateLink(link, `links[${index}]`, errors));
  footer.legalLinks.forEach((link, index) => validateLink(link, `legalLinks[${index}]`, errors));
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
