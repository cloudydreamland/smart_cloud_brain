import { isAllowedPatientRoute } from "./patientSiteRoutes";
import { normalizePatientSitePagesConfig } from "./patientSiteSectionRegistry";
import type {
  PatientHomeConfig,
  PatientHomeModule,
  PatientNavConfig,
  PatientNavMenu,
  PatientSiteConfig,
  PatientStaticPagesConfig,
  RouteTargetConfig,
  StaticPageConfig,
} from "./patientSiteTypes";
import type { PatientSitePagesConfig } from "./patientSitePageTypes";
import type { DataRow } from "./types";

export type PatientSiteConfigKey = "patient_nav" | "patient_home" | "patient_static_pages" | "patient_pages";

type ResolveOptions = {
  preserveDisabled?: boolean;
};

const allowedHomeModules = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);
const emptyPatientSiteConfig: PatientSiteConfig = {
  nav: { brand: { name: "", homeRoute: "" }, menus: [], userLinks: [] },
  home: { hero: { title: "", enabled: false }, modules: [] },
  staticPages: { pages: [] },
  pages: { pages: [] },
};

export function resolvePatientSiteConfig(
  fallback: PatientSiteConfig = emptyPatientSiteConfig,
  source: unknown = {},
  options: ResolveOptions = {},
): PatientSiteConfig {
  const row = isRecord(source) ? source : {};
  return {
    nav: resolvePatientSiteConfigSection("patient_nav", row.nav, options, fallback) as PatientNavConfig,
    home: resolvePatientSiteConfigSection("patient_home", row.home, options, fallback) as PatientHomeConfig,
    staticPages: resolvePatientSiteConfigSection("patient_static_pages", row.staticPages, options, fallback) as PatientStaticPagesConfig,
    pages: normalizePages(sectionOrFallback(row.pages, fallback.pages)),
  };
}

export function normalizePatientSiteConfig(source: unknown = {}, options: ResolveOptions = {}): PatientSiteConfig {
  return resolvePatientSiteConfig(emptyPatientSiteConfig, source, options);
}

export function resolvePatientSiteConfigSection(
  key: PatientSiteConfigKey,
  source: unknown,
  options: ResolveOptions = {},
  fallbackConfig: PatientSiteConfig = emptyPatientSiteConfig,
) {
  if (key === "patient_nav") return normalizeNav(sectionOrFallback(source, fallbackConfig.nav), options);
  if (key === "patient_home") return normalizeHome(sectionOrFallback(source, fallbackConfig.home), options);
  if (key === "patient_pages") return normalizePages(sectionOrFallback(source, fallbackConfig.pages));
  return normalizeStaticPages(sectionOrFallback(source, fallbackConfig.staticPages), options);
}

function sectionOrFallback<T>(source: unknown, fallback: T): unknown {
  return isRecord(source) && Object.keys(source).length ? source : fallback;
}

function normalizeNav(source: unknown, options: ResolveOptions): PatientNavConfig {
  const row = isRecord(source) ? source : {};
  const brand = isRecord(row.brand) ? row.brand : {};
  return {
    brand: {
      name: text(brand.name, ""),
      homeRoute: routeName(brand.homeRoute, "patient-home"),
      logoUrl: optionalText(brand.logoUrl),
      logoAlt: optionalText(brand.logoAlt),
      logoObjectKey: optionalText(brand.logoObjectKey),
    },
    menus: normalizeArray(row.menus, (item, index) => normalizeMenu(item, index, options)),
    userLinks: normalizeArray(row.userLinks, (item, index) => normalizeLink(item, index, options)),
  };
}

function normalizeMenu(
  source: unknown,
  index: number,
  options: ResolveOptions,
): PatientNavMenu | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const key = text(row.key, `menu-${index + 1}`);
  const label = text(row.label, "");
  const links = normalizeArray(row.links, (item, linkIndex) => normalizeLink(item, linkIndex, options));
  const feature = normalizeLink(row.feature, 0, options);
  if (!label || (!links.length && !feature && row.enabled !== false)) return undefined;
  return {
    key,
    label,
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
    lead: text(row.lead, ""),
    description: text(row.description, ""),
    links,
    feature,
  };
}

function normalizeHome(source: unknown, options: ResolveOptions): PatientHomeConfig {
  const row = isRecord(source) ? source : {};
  const hero = isRecord(row.hero) ? row.hero : {};
  const hasHero = isRecord(row.hero);
  return {
    hero: {
      eyebrow: text(hero.eyebrow, ""),
      title: text(hero.title, ""),
      primaryAction: normalizeLink(hero.primaryAction, 0, options),
      secondaryAction: normalizeLink(hero.secondaryAction, 0, options),
      enabled: hasHero && hero.enabled !== false ? true : false,
      backgroundImageUrl: optionalText(hero.backgroundImageUrl),
      backgroundImageAlt: optionalText(hero.backgroundImageAlt),
      backgroundObjectKey: optionalText(hero.backgroundObjectKey),
    },
    modules: normalizeArray(row.modules, (item, index) => normalizeModule(item, index, options)),
  };
}

function normalizeModule(
  source: unknown,
  index: number,
  options: ResolveOptions,
): PatientHomeModule | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const type = text(row.type, "");
  if (!allowedHomeModules.has(type)) return undefined;
  const content = normalizeModuleContent(type, row.content, options);
  return {
    type,
    key: text(row.key, `${type || "module"}-${index + 1}`),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
    content,
  };
}

function normalizeModuleContent(
  type: string,
  source: unknown,
  options: ResolveOptions,
): Record<string, unknown> {
  const content = isRecord(source) ? source : {};
  if (type === "quick_actions") {
    return {
      ...content,
      items: normalizeArray(content.items, (item, index) => normalizeLink(item, index, options)),
    };
  }
  if (type === "notice") {
    return {
      ...content,
      text: text(content.text, ""),
    };
  }
  return { ...content };
}

function normalizeStaticPages(source: unknown, options: ResolveOptions): PatientStaticPagesConfig {
  const row = isRecord(source) ? source : {};
  return {
    pages: normalizeArray(row.pages, (item, index) => normalizePage(item, index, options)),
  };
}

function normalizePages(source: unknown): PatientSitePagesConfig {
  return normalizePatientSitePagesConfig(source);
}

function normalizePage(
  source: unknown,
  index: number,
  options: ResolveOptions,
): StaticPageConfig | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const route = routeName(row.routeName, "");
  const title = text(row.title, "");
  if (!route || !title) return undefined;
  return {
    routeName: route,
    label: text(row.label, ""),
    title,
    intro: text(row.intro, ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
    points: normalizeArray(row.points, (item) => normalizePoint(item)),
    primary: normalizeLink(row.primary, 0, options),
  };
}

function normalizePoint(source: unknown) {
  const row = isRecord(source) ? source : {};
  const title = text(row.title, "");
  const value = text(row.text, "");
  return title && value ? { title, text: value } : undefined;
}

function normalizeLink(
  source: unknown,
  index: number,
  options: ResolveOptions,
): RouteTargetConfig | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const label = text(row.label, "");
  const route = routeName(row.routeName, "");
  if (!label || !route) return undefined;
  return {
    label,
    routeName: route,
    slug: slugValue(row.slug),
    query: isStringRecord(row.query) ? row.query : undefined,
    description: text(row.description, ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
  };
}

function normalizeArray<T>(
  source: unknown,
  normalize: (item: unknown, index: number) => T | undefined,
): T[] {
  if (!Array.isArray(source)) return [];
  return source
    .map((item, index) => (isRecord(item) ? normalize(item, index) : undefined))
    .filter((item): item is T => Boolean(item))
    .sort(bySort) as T[];
}

function bySort(left: unknown, right: unknown) {
  const leftSort = isRecord(left) ? left.sort : undefined;
  const rightSort = isRecord(right) ? right.sort : undefined;
  return numberValue(leftSort, 0) - numberValue(rightSort, 0);
}

function routeName(value: unknown, fallback: string) {
  const candidate = text(value, "");
  if (candidate && isAllowedPatientRoute(candidate)) return candidate;
  return fallback && isAllowedPatientRoute(fallback) ? fallback : "";
}

function isRecord(value: unknown): value is DataRow {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function isStringRecord(value: unknown): value is Record<string, string> {
  return isRecord(value) && Object.values(value).every((item) => typeof item === "string");
}

function text(value: unknown, fallback: string) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function optionalText(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function slugValue(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim().toLowerCase() : undefined;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}
