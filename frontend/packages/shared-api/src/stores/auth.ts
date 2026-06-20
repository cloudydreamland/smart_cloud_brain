import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { SESSION_EVENT } from "../api";
import type { Role, Session } from "../types";

export const useAuthStore = defineStore("auth", () => {
  const session = ref<Session | null>(null);
  const storageKey = ref("smart-cloud-brain-session");
  const permissionError = ref("");
  const isAuthenticated = computed(() => Boolean(session.value?.token));

  function load(key: string, expectedRole?: Role) {
    storageKey.value = key;
    const raw = localStorage.getItem(key);
    session.value = raw ? JSON.parse(raw) as Session : null;
    permissionError.value = "";
    if (session.value && expectedRole && session.value.role !== expectedRole) {
      permissionError.value = `当前登录角色为 ${session.value.role}，无权访问 ${expectedRole} 端业务。`;
    }
  }

  function save(key: string, nextSession: Session, expectedRole?: Role) {
    storageKey.value = key;
    session.value = nextSession;
    localStorage.setItem(key, JSON.stringify(nextSession));
    permissionError.value = expectedRole && nextSession.role !== expectedRole
      ? `当前登录角色为 ${nextSession.role}，无权访问 ${expectedRole} 端业务。` : "";
  }

  function logout() {
    session.value = null;
    permissionError.value = "";
    localStorage.removeItem(storageKey.value);
  }

  function token() { return session.value?.token ?? ""; }

  function requireRole(role: Role) {
    if (!session.value) return false;
    if (session.value.role !== role) {
      permissionError.value = `当前登录角色为 ${session.value.role}，无权访问 ${role} 端业务。`;
      return false;
    }
    permissionError.value = "";
    return true;
  }

  function bindUnauthorized() {
    const handler = () => logout();
    if (typeof window === "undefined") return () => undefined;
    window.addEventListener(SESSION_EVENT, handler);
    return () => window.removeEventListener(SESSION_EVENT, handler);
  }

  return { session, permissionError, isAuthenticated, load, save, logout, token, requireRole, bindUnauthorized };
});
