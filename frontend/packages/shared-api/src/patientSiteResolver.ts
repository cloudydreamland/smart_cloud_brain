import { defaultPatientSiteConfig } from "./patientSiteDefaults";
import { isAllowedPatientRoute } from "./patientSiteRoutes";
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
import type { DataRow } from "./types";

export type PatientSiteConfigKey = "patient_nav" | "patient_home" | "patient_static_pages";

type ResolveOptions = {
  preserveDisabled?: boolean;
};

const allowedHomeModules = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

export function resolvePatientSiteConfig(
  fallback: PatientSiteConfig = defaultPatientSiteConfig,
  source: unknown = {},
  options: ResolveOptions = {},
): PatientSiteConfig {
  const row = isRecord(source) ? source : {};
  return {
    nav: resolvePatientSiteConfigSection("patient_nav", row.nav, options, fallback) as PatientNavConfig,
    home: resolvePatientSiteConfigSection("patient_home", row.home, options, fallback) as PatientHomeConfig,
    staticPages: resolvePatientSiteConfigSection("patient_static_pages", row.staticPages, options, fallback) as PatientStaticPagesConfig,
  };
}

export function normalizePatientSiteConfig(source: unknown = {}, options: ResolveOptions = {}): PatientSiteConfig {
  return resolvePatientSiteConfig(defaultPatientSiteConfig, source, options);
}

export function resolvePatientSiteConfigSection(
  key: PatientSiteConfigKey,
  source: unknown,
  options: ResolveOptions = {},
  fallbackConfig: PatientSiteConfig = defaultPatientSiteConfig,
) {
  if (key === "patient_nav") return normalizeNav(source, fallbackConfig.nav, options);
  if (key === "patient_home") return normalizeHome(source, fallbackConfig.home, options);
  return normalizeStaticPages(source, fallbackConfig.staticPages, options);
}

function normalizeNav(source: unknown, fallback: PatientNavConfig, options: ResolveOptions): PatientNavConfig {
  const row = isRecord(source) ? source : {};
  const brand = isRecord(row.brand) ? row.brand : {};
  return {
    brand: {
      name: text(brand.name, fallback.brand.name),
      homeRoute: routeName(brand.homeRoute, fallback.brand.homeRoute),
    },
    menus: mergeArray(row.menus, fallback.menus, menuIdentity, (item, base, index) => normalizeMenu(item, base, index, options)),
    userLinks: mergeArray(row.userLinks, fallback.userLinks, linkIdentity, (item, base, index) => normalizeLink(item, base, index, options)),
  };
}

function normalizeMenu(
  source: unknown,
  fallback: PatientNavMenu | undefined,
  index: number,
  options: ResolveOptions,
): PatientNavMenu | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const key = text(row.key, fallback?.key || `menu-${index + 1}`);
  const label = text(row.label, fallback?.label || "");
  const links = mergeArray(row.links, fallback?.links || [], linkIdentity, (item, base, linkIndex) =>
    normalizeLink(item, base, linkIndex, options),
  );
  const feature = normalizeLink(row.feature, fallback?.feature, 0, options);
  if (!label || (!links.length && !feature && row.enabled !== false)) return undefined;
  return {
    key,
    label,
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, fallback?.sort ?? index * 10),
    lead: text(row.lead, fallback?.lead || ""),
    description: text(row.description, fallback?.description || ""),
    links,
    feature,
  };
}

function normalizeHome(source: unknown, fallback: PatientHomeConfig, options: ResolveOptions): PatientHomeConfig {
  const row = isRecord(source) ? source : {};
  const hero = isRecord(row.hero) ? row.hero : {};
  return {
    hero: {
      eyebrow: text(hero.eyebrow, fallback.hero.eyebrow || ""),
      title: text(hero.title, fallback.hero.title),
      primaryAction: normalizeLink(hero.primaryAction, fallback.hero.primaryAction, 0, options),
      secondaryAction: normalizeLink(hero.secondaryAction, fallback.hero.secondaryAction, 0, options),
      enabled: hero.enabled === false ? false : true,
    },
    modules: mergeArray(row.modules, fallback.modules, moduleIdentity, (item, base, index) => normalizeModule(item, base, index, options)),
  };
}

function normalizeModule(
  source: unknown,
  fallback: PatientHomeModule | undefined,
  index: number,
  options: ResolveOptions,
): PatientHomeModule | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const type = text(row.type, fallback?.type || "");
  if (!allowedHomeModules.has(type)) return undefined;
  const content = normalizeModuleContent(type, row.content, fallback?.content || {}, options);
  return {
    type,
    key: text(row.key, fallback?.key || `${type || "module"}-${index + 1}`),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, fallback?.sort ?? index * 10),
    content,
  };
}

function normalizeModuleContent(
  type: string,
  source: unknown,
  fallback: Record<string, unknown>,
  options: ResolveOptions,
): Record<string, unknown> {
  const content = isRecord(source) ? source : {};
  if (type === "quick_actions") {
    return {
      ...fallback,
      ...content,
      items: mergeArray(content.items, Array.isArray(fallback.items) ? fallback.items : [], linkIdentity, (item, base, index) =>
        normalizeLink(item, base, index, options),
      ),
    };
  }
  if (type === "notice") {
    return {
      ...fallback,
      ...content,
      text: text(content.text, typeof fallback.text === "string" ? fallback.text : ""),
    };
  }
  return { ...fallback, ...content };
}

function normalizeStaticPages(source: unknown, fallback: PatientStaticPagesConfig, options: ResolveOptions): PatientStaticPagesConfig {
  const row = isRecord(source) ? source : {};
  return {
    pages: mergeArray(row.pages, fallback.pages, pageIdentity, (item, base, index) => normalizePage(item, base, index, options)),
  };
}

function normalizePage(
  source: unknown,
  fallback: StaticPageConfig | undefined,
  index: number,
  options: ResolveOptions,
): StaticPageConfig | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const route = routeName(row.routeName, fallback?.routeName || "");
  const title = text(row.title, fallback?.title || "");
  if (!route || !title) return undefined;
  return {
    routeName: route,
    label: text(row.label, fallback?.label || ""),
    title,
    intro: text(row.intro, fallback?.intro || ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, fallback?.sort ?? index * 10),
    points: mergeArray(row.points, fallback?.points || [], pointIdentity, (item, base) => normalizePoint(item, base)),
    primary: normalizeLink(row.primary, fallback?.primary, 0, options),
  };
}

function normalizePoint(source: unknown, fallback: { title: string; text: string } | undefined) {
  const row = isRecord(source) ? source : {};
  const title = text(row.title, fallback?.title || "");
  const value = text(row.text, fallback?.text || "");
  return title && value ? { title, text: value } : undefined;
}

function normalizeLink(
  source: unknown,
  fallback: RouteTargetConfig | undefined,
  index: number,
  options: ResolveOptions,
): RouteTargetConfig | undefined {
  const row = isRecord(source) ? source : {};
  if (row.enabled === false && !options.preserveDisabled) return undefined;
  const label = text(row.label, fallback?.label || "");
  const route = routeName(row.routeName, fallback?.routeName || "");
  if (!label || !route) return undefined;
  return {
    label,
    routeName: route,
    query: isStringRecord(row.query) ? row.query : fallback?.query,
    description: text(row.description, fallback?.description || ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, fallback?.sort ?? index * 10),
  };
}

function mergeArray<T>(
  source: unknown,
  fallback: readonly T[],
  identity: (item: unknown) => string,
  normalize: (item: unknown, fallbackItem: T | undefined, index: number) => T | undefined,
): T[] {
  const fallbackItems = fallback.map((item, index) => normalize(item, undefined, index)).filter(Boolean) as T[];
  const byKey = new Map<string, { item: T; index: number }>();
  fallbackItems.forEach((item, index) => {
    const key = identity(item);
    if (key) byKey.set(key, { item, index });
  });

  const merged = [...fallbackItems];
  if (Array.isArray(source)) {
    source.forEach((item, sourceIndex) => {
      if (!isRecord(item)) return;
      const key = identity(item);
      const match = key ? byKey.get(key) : undefined;
      const normalized = normalize(item, match?.item, match?.index ?? fallbackItems.length + sourceIndex);
      const currentIndex = key ? merged.findIndex((entry) => identity(entry) === key) : -1;
      if (!normalized) {
        if (currentIndex >= 0) merged.splice(currentIndex, 1);
        return;
      }
      if (currentIndex >= 0) {
        merged[currentIndex] = normalized;
      } else {
        merged.push(normalized);
      }
    });
  }

  return merged.filter(Boolean).sort(bySort);
}

function menuIdentity(item: unknown) {
  return isRecord(item) ? text(item.key, "") : "";
}

function moduleIdentity(item: unknown) {
  return isRecord(item) ? text(item.key, "") : "";
}

function pageIdentity(item: unknown) {
  return isRecord(item) ? routeName(item.routeName, "") : "";
}

function linkIdentity(item: unknown) {
  if (!isRecord(item)) return "";
  const route = routeName(item.routeName, "");
  const query = isStringRecord(item.query) ? JSON.stringify(item.query) : "";
  return route ? `${route}:${query}` : "";
}

function pointIdentity(item: unknown) {
  return isRecord(item) ? text(item.title, "") : "";
}

function bySort<T extends { sort?: number }>(left: T, right: T) {
  return numberValue(left.sort, 0) - numberValue(right.sort, 0);
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

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}
