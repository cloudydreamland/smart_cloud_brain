import { ref } from "vue";
import { api, normalizePatientSiteConfig, type DataRow, type PatientSiteConfig } from "@smart-cloud-brain/shared-api";

const loading = ref(false);
const config = ref<PatientSiteConfig>(normalizeConfig({}));
const disabledStaticPageRouteNames = ref<Set<string>>(new Set());
const activePreviewToken = ref("");
let pending: Promise<void> | null = null;
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null;
let autoRefreshListenersBound = false;

type LoadOptions = {
  keepCurrentOnError?: boolean;
};

async function loadConfig(options: LoadOptions = {}) {
  if (pending) return pending;
  loading.value = true;
  const request = activePreviewToken.value ? api.patientSitePreviewConfig(activePreviewToken.value) : api.patientSiteConfig();
  pending = request
    .then((remote) => {
      config.value = normalizeConfig(remote);
      disabledStaticPageRouteNames.value = collectDisabledStaticPageRouteNames(remote);
    })
    .catch(() => {
      if (!options.keepCurrentOnError) {
        config.value = normalizeConfig({});
        disabledStaticPageRouteNames.value = new Set();
      }
    })
    .finally(() => {
      loading.value = false;
      pending = null;
    });
  return pending;
}

export function usePatientSiteConfig() {
  const load = () => loadConfig();
  const loadPreview = (token: string) => {
    activePreviewToken.value = token;
    if (pending) return pending.finally(() => loadConfig());
    return loadConfig();
  };
  const clearPreview = () => {
    activePreviewToken.value = "";
  };

  return { config, disabledStaticPageRouteNames, loading, activePreviewToken, load, loadPreview, clearPreview, refresh: load };
}

export function startPatientSiteConfigAutoRefresh(intervalMs = 5000) {
  if (typeof window === "undefined" || typeof document === "undefined" || autoRefreshTimer) return;

  const refreshVisibleConfig = () => {
    if (document.visibilityState !== "hidden") {
      void loadConfig({ keepCurrentOnError: true });
    }
  };

  autoRefreshTimer = window.setInterval(refreshVisibleConfig, intervalMs);
  if (!autoRefreshListenersBound) {
    window.addEventListener("focus", refreshVisibleConfig);
    document.addEventListener("visibilitychange", refreshVisibleConfig);
    autoRefreshListenersBound = true;
  }
}

export function normalizeConfig(source: unknown): PatientSiteConfig {
  return normalizePatientSiteConfig(source);
}

function collectDisabledStaticPageRouteNames(source: unknown) {
  const row = isRow(source) ? source : {};
  const staticPages = isRow(row.staticPages) ? row.staticPages : {};
  const pages = Array.isArray(staticPages.pages) ? staticPages.pages : [];
  return new Set(
    pages
      .filter((page): page is DataRow => isRow(page) && page.enabled === false && typeof page.routeName === "string")
      .map((page) => String(page.routeName)),
  );
}

function isRow(value: unknown): value is DataRow {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}
