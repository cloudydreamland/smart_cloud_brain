import { ref } from "vue";
import { api, normalizePatientSiteConfig, type PatientSiteConfig } from "@smart-cloud-brain/shared-api";

const loading = ref(false);
const config = ref<PatientSiteConfig>(normalizeConfig({}));
let pending: Promise<void> | null = null;
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null;
let autoRefreshListenersBound = false;

type LoadOptions = {
  keepCurrentOnError?: boolean;
};

async function loadConfig(options: LoadOptions = {}) {
  if (pending) return pending;
  loading.value = true;
  pending = api.patientSiteConfig()
    .then((remote) => {
      config.value = normalizeConfig(remote);
    })
    .catch(() => {
      if (!options.keepCurrentOnError) {
        config.value = normalizeConfig({});
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

  return { config, loading, load, refresh: load };
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
