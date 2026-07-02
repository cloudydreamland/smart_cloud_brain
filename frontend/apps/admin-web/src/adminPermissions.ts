import { adminApi } from "@smart-cloud-brain/shared-api";

export const PATIENT_SITE_MANAGE_PERMISSION = "patient-site:manage";

let permissionCache: Promise<Set<string>> | null = null;

export function loadAdminPermissions(force = false) {
  if (!permissionCache || force) {
    permissionCache = adminApi.myPermissions()
      .then((items) => new Set(items))
      .catch(() => new Set<string>());
  }
  return permissionCache;
}

export function clearAdminPermissionCache() {
  permissionCache = null;
}

export function hasAdminPermission(permissions: Set<string>, permission?: string) {
  return !permission || permissions.has(permission);
}
