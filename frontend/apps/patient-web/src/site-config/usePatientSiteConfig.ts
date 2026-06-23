import { ref } from "vue";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";
import { defaultPatientSiteConfig } from "./defaultConfig";
import { isAllowedRoute } from "./routeWhitelist";
import type {
  PatientHomeConfig,
  PatientHomeModule,
  PatientNavConfig,
  PatientNavMenu,
  PatientSiteConfig,
  PatientStaticPagesConfig,
  RouteTargetConfig,
  StaticPageConfig,
} from "./types";

const loading = ref(false);
const config = ref<PatientSiteConfig>(normalizeConfig({}));
let loaded = false;
let pending: Promise<void> | null = null;

export function usePatientSiteConfig() {
  async function load() {
    if (loaded) return;
    if (pending) return pending;
    loading.value = true;
    pending = api.patientSiteConfig()
      .then((remote) => {
        config.value = normalizeConfig(remote);
        loaded = true;
      })
      .catch(() => {
        config.value = normalizeConfig({});
        loaded = false;
      })
      .finally(() => {
        loading.value = false;
        pending = null;
      });
    return pending;
  }

  return { config, loading, load };
}

export function normalizeConfig(source: unknown): PatientSiteConfig {
  const row = isRecord(source) ? source : {};
  return {
    nav: normalizeNav(row.nav),
    home: normalizeHome(row.home),
    staticPages: normalizeStaticPages(row.staticPages),
  };
}

function normalizeNav(source: unknown): PatientNavConfig {
  const fallback = defaultPatientSiteConfig.nav;
  const row = isRecord(source) ? source : {};
  const brand = isRecord(row.brand) ? row.brand : {};
  const menus = normalizeMenus(row.menus, fallback.menus);
  const userLinks = normalizeLinks(row.userLinks, fallback.userLinks);
  return {
    brand: {
      name: text(brand.name, fallback.brand.name),
      homeRoute: isAllowedRoute(text(brand.homeRoute, "")) ? text(brand.homeRoute, fallback.brand.homeRoute) : fallback.brand.homeRoute,
    },
    menus,
    userLinks,
  };
}

function normalizeMenus(source: unknown, fallback: PatientNavMenu[]) {
  const menus = Array.isArray(source) ? source : fallback;
  return menus
    .filter(isRecord)
    .filter((menu) => menu.enabled !== false)
    .map((menu, index) => ({
      key: text(menu.key, `menu-${index}`),
      label: text(menu.label, ""),
      enabled: true,
      sort: numberValue(menu.sort, index * 10),
      lead: text(menu.lead, ""),
      description: text(menu.description, ""),
      links: normalizeLinks(menu.links, []),
      feature: normalizeLink(menu.feature),
    }))
    .filter((menu) => menu.label && (menu.links.length || menu.feature))
    .sort(bySort);
}

function normalizeLinks(source: unknown, fallback: RouteTargetConfig[]) {
  const links = Array.isArray(source) ? source : fallback;
  return links
    .filter(isRecord)
    .map((link, index) => normalizeLink(link, index))
    .filter((link): link is RouteTargetConfig => Boolean(link))
    .sort(bySort);
}

function normalizeLink(source: unknown, index = 0): RouteTargetConfig | undefined {
  if (!isRecord(source) || source.enabled === false) return undefined;
  const routeName = text(source.routeName, "");
  const label = text(source.label, "");
  if (!label || !isAllowedRoute(routeName)) return undefined;
  return {
    label,
    routeName,
    query: isStringRecord(source.query) ? source.query : undefined,
    description: text(source.description, ""),
    enabled: true,
    sort: numberValue(source.sort, index * 10),
  };
}

function normalizeHome(source: unknown): PatientHomeConfig {
  const fallback = defaultPatientSiteConfig.home;
  const row = isRecord(source) ? source : {};
  const hero = isRecord(row.hero) ? row.hero : fallback.hero;
  const primaryAction = normalizeLink(hero.primaryAction) ?? fallback.hero.primaryAction;
  const secondaryAction = normalizeLink(hero.secondaryAction) ?? fallback.hero.secondaryAction;
  return {
    hero: {
      eyebrow: text(hero.eyebrow, fallback.hero.eyebrow ?? ""),
      title: text(hero.title, fallback.hero.title),
      primaryAction,
      secondaryAction,
      enabled: hero.enabled !== false,
    },
    modules: normalizeModules(row.modules, fallback.modules),
  };
}

function normalizeModules(source: unknown, fallback: PatientHomeModule[]) {
  const allowed = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);
  const modules = Array.isArray(source) ? source : fallback;
  return modules
    .filter(isRecord)
    .filter((module) => module.enabled !== false)
    .map((module, index) => ({
      type: text(module.type, ""),
      key: text(module.key, `module-${index}`),
      enabled: true,
      sort: numberValue(module.sort, index * 10),
      content: normalizeModuleContent(text(module.type, ""), module.content),
    }))
    .filter((module) => allowed.has(module.type) && module.key)
    .sort(bySort);
}

function normalizeModuleContent(type: string, source: unknown) {
  const content = isRecord(source) ? source : {};
  if (type === "quick_actions") {
    return { ...content, items: normalizeLinks(content.items, []) };
  }
  if (type === "notice") {
    return { ...content, text: text(content.text, "") };
  }
  return content;
}

function normalizeStaticPages(source: unknown): PatientStaticPagesConfig {
  const fallback = defaultPatientSiteConfig.staticPages;
  const row = isRecord(source) ? source : {};
  const pages = Array.isArray(row.pages) ? row.pages : fallback.pages;
  return {
    pages: pages
      .filter(isRecord)
      .filter((page) => page.enabled !== false && isAllowedRoute(text(page.routeName, "")))
      .map((page, index) => normalizePage(page, index))
      .filter((page): page is StaticPageConfig => Boolean(page))
      .sort(bySort),
  };
}

function normalizePage(source: DataRow, index: number): StaticPageConfig | undefined {
  const routeName = text(source.routeName, "");
  const title = text(source.title, "");
  if (!isAllowedRoute(routeName) || !title) return undefined;
  const points = Array.isArray(source.points)
    ? source.points
        .filter(isRecord)
        .map((point) => ({ title: text(point.title, ""), text: text(point.text, "") }))
        .filter((point) => point.title && point.text)
    : [];
  return {
    routeName,
    label: text(source.label, "智慧云脑"),
    title,
    intro: text(source.intro, ""),
    enabled: true,
    sort: numberValue(source.sort, index * 10),
    points,
    primary: normalizeLink(source.primary),
  };
}

function bySort<T extends { sort?: number }>(left: T, right: T) {
  return numberValue(left.sort, 0) - numberValue(right.sort, 0);
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
