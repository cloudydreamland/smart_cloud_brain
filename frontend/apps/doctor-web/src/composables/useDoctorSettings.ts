import { computed, reactive } from "vue";

export type DoctorSettings = {
  aiDraftMode: "preview" | "auto";
  highRiskConfirm: boolean;
  notifyMode: "realtime" | "queue" | "dnd";
};

const STORAGE_KEY = "doctor-settings";

const defaults: DoctorSettings = {
  aiDraftMode: "preview",
  highRiskConfirm: true,
  notifyMode: "realtime",
};

const settings = reactive<DoctorSettings>({ ...defaults });
let loaded = false;

function isAiDraftMode(value: unknown): value is DoctorSettings["aiDraftMode"] {
  return value === "preview" || value === "auto";
}

function isNotifyMode(value: unknown): value is DoctorSettings["notifyMode"] {
  return value === "realtime" || value === "queue" || value === "dnd";
}

function storageAvailable() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function loadDoctorSettings() {
  Object.assign(settings, defaults);
  if (!storageAvailable()) return settings;
  try {
    const saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || "{}");
    if (isAiDraftMode(saved.aiDraftMode)) settings.aiDraftMode = saved.aiDraftMode;
    if (typeof saved.highRiskConfirm === "boolean") settings.highRiskConfirm = saved.highRiskConfirm;
    if (isNotifyMode(saved.notifyMode)) settings.notifyMode = saved.notifyMode;
  } catch {
    Object.assign(settings, defaults);
  }
  loaded = true;
  return settings;
}

export function saveDoctorSettings() {
  if (!storageAvailable()) return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...settings }));
}

function ensureLoaded() {
  if (!loaded) loadDoctorSettings();
}

export function useDoctorSettings() {
  ensureLoaded();

  const notifyLabel = computed(() => {
    const map: Record<DoctorSettings["notifyMode"], string> = {
      realtime: "实时推送",
      queue: "仅队列刷新",
      dnd: "免打扰",
    };
    return map[settings.notifyMode];
  });

  function setAiDraftMode(value: DoctorSettings["aiDraftMode"]) {
    settings.aiDraftMode = value;
    saveDoctorSettings();
  }

  function setHighRiskConfirm(value: boolean) {
    settings.highRiskConfirm = value;
    saveDoctorSettings();
  }

  function setNotifyMode(value: DoctorSettings["notifyMode"]) {
    settings.notifyMode = value;
    saveDoctorSettings();
  }

  return {
    settings,
    notifyLabel,
    setAiDraftMode,
    setHighRiskConfirm,
    setNotifyMode,
  };
}
