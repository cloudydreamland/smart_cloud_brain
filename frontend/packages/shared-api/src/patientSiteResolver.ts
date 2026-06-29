import { isAllowedPatientRoute } from "./patientSiteRoutes";
import { normalizePatientSitePagesConfig } from "./patientSiteSectionRegistry";
import type {
  PatientHomeConfig,
  PatientHomeModule,
  PatientFooterConfig,
  PatientHospitalInfoConfig,
  PatientHospitalLocationConfig,
  PatientNavConfig,
  PatientNavMenu,
  PatientSiteConfig,
  PatientStaticPagesConfig,
  RouteTargetConfig,
  StaticPageConfig,
} from "./patientSiteTypes";
import type { PatientSitePagesConfig } from "./patientSitePageTypes";
import type { DataRow } from "./types";

export type PatientSiteConfigKey = "patient_nav" | "patient_home" | "patient_static_pages" | "patient_pages" | "patient_hospital_info" | "patient_footer";

type ResolveOptions = {
  preserveDisabled?: boolean;
};

const allowedHomeModules = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);
const emptyPatientSiteConfig: PatientSiteConfig = {
  nav: { brand: { name: "", homeRoute: "" }, menus: [], userLinks: [] },
  home: { hero: { title: "", enabled: false }, modules: [] },
  staticPages: { pages: [] },
  pages: { pages: [] },
  hospitalInfo: { name: "", locations: [] },
  footer: { brandName: "", links: [], legalLinks: [] },
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
    hospitalInfo: resolvePatientSiteConfigSection("patient_hospital_info", row.hospitalInfo, options, fallback) as PatientHospitalInfoConfig,
    footer: resolvePatientSiteConfigSection("patient_footer", row.footer, options, fallback) as PatientFooterConfig,
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
  if (key === "patient_hospital_info") return normalizeHospitalInfo(sectionOrFallback(source, fallbackConfig.hospitalInfo), options);
  if (key === "patient_footer") return normalizeFooter(sectionOrFallback(source, fallbackConfig.footer), options);
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

function normalizeHospitalInfo(source: unknown, options: ResolveOptions): PatientHospitalInfoConfig {
  const row = isRecord(source) ? source : {};
  return {
    name: text(row.name, ""),
    intro: text(row.intro, ""),
    address: text(row.address, ""),
    phone: text(row.phone, ""),
    workHours: text(row.workHours, ""),
    website: text(row.website, ""),
    locations: normalizeArray(row.locations, (item, index) => normalizeHospitalLocation(item, index, options)),
  };
}

function normalizeHospitalLocation(
  source: unknown,
  index: number,
  options: ResolveOptions,
): PatientHospitalLocationConfig | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const name = text(row.name, "");
  const address = text(row.address, "");
  if (!name || !address) return undefined;
  return {
    name,
    address,
    phone: text(row.phone, ""),
    imageUrl: optionalText(row.imageUrl),
    imageObjectKey: optionalText(row.imageObjectKey),
    sort: numberValue(row.sort, index * 10),
    enabled: row.enabled === false ? false : true,
  };
}

function normalizeFooter(source: unknown, options: ResolveOptions): PatientFooterConfig {
  const row = isRecord(source) ? source : {};
  return {
    brandName: text(row.brandName, ""),
    description: text(row.description, ""),
    copyright: text(row.copyright, ""),
    contactPhone: text(row.contactPhone, ""),
    contactAddress: text(row.contactAddress, ""),
    links: normalizeArray(row.links, (item, index) => normalizeLink(item, index, options)),
    legalLinks: normalizeArray(row.legalLinks, (item, index) => normalizeLink(item, index, options)),
  };
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
