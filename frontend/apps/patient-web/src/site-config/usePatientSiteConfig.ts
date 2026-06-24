import { ref } from "vue";
import { api, normalizePatientSiteConfig, type PatientSiteConfig } from "@smart-cloud-brain/shared-api";

const loading = ref(false);
const config = ref<PatientSiteConfig>(normalizeConfig({}));
let pending: Promise<void> | null = null;

export function usePatientSiteConfig() {
  async function load() {
    if (pending) return pending;
    loading.value = true;
    pending = api.patientSiteConfig()
      .then((remote) => {
        config.value = normalizeConfig(remote);
      })
      .catch(() => {
        config.value = normalizeConfig({});
      })
      .finally(() => {
        loading.value = false;
        pending = null;
      });
    return pending;
  }

  return { config, loading, load, refresh: load };
}

export function normalizeConfig(source: unknown): PatientSiteConfig {
  return normalizePatientSiteConfig(source);
}
