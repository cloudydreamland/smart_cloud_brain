const DEFAULT_API_BASE = "/api";
const API_BASE_KEY = "patient-mobile-api-base";
const SESSION_KEY = "patient-session";

export function normalizeApiBase(value) {
  return String(value || DEFAULT_API_BASE).replace(/\/+$/, "");
}

export function getApiBase() {
  const stored = uni.getStorageSync(API_BASE_KEY);
  const globalConfig = typeof globalThis !== "undefined" ? globalThis.SMART_CLOUD_BRAIN_API_BASE : "";
  const envConfig = typeof process !== "undefined" && process.env ? process.env.UNI_APP_API_BASE || process.env.VUE_APP_API_BASE : "";
  return normalizeApiBase(stored || globalConfig || envConfig || DEFAULT_API_BASE);
}

export function saveApiBase(value) {
  const normalized = normalizeApiBase(value);
  uni.setStorageSync(API_BASE_KEY, normalized);
  return normalized;
}

export function getSession() {
  return uni.getStorageSync(SESSION_KEY) || null;
}

export function saveSession(session) {
  uni.setStorageSync(SESSION_KEY, session || null);
}

export function clearSession() {
  uni.removeStorageSync(SESSION_KEY);
}

export function isLoggedIn() {
  const session = getSession();
  return Boolean(session && session.token);
}

export function requireLogin() {
  if (isLoggedIn()) return true;
  uni.reLaunch({ url: "/pages/auth/login" });
  return false;
}

export function logoutToLogin() {
  clearSession();
  uni.reLaunch({ url: "/pages/auth/login" });
}
