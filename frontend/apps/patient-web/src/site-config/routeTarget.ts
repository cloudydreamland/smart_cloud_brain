import type { RouteLocationRaw } from "vue-router";
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import { getActivePatientSitePreviewToken } from "./usePatientSiteConfig";

type PreviewTokenSource = string | { value?: unknown };

export function toPatientRoute(link: RouteTargetConfig, previewToken: PreviewTokenSource = getActivePatientSitePreviewToken()): RouteLocationRaw {
  if (link.routeName === "cms-page" && link.slug) return withPatientPreview({ name: "cms-page", params: { slug: link.slug } }, previewToken);
  if (link.routeName === "cms-page") return withPatientPreview({ name: "patient-home" }, previewToken);
  return withPatientPreview(link.query ? { name: link.routeName, query: link.query } : { name: link.routeName }, previewToken);
}

export function withPatientPreview(route: RouteLocationRaw, previewToken: PreviewTokenSource = getActivePatientSitePreviewToken()): RouteLocationRaw {
  const token = normalizePreviewToken(previewToken);
  if (!token || typeof route !== "object" || Array.isArray(route)) return route;
  return { ...route, query: { ...(route.query || {}), previewToken: token } };
}

function normalizePreviewToken(source: PreviewTokenSource) {
  if (typeof source === "string") return source;
  return typeof source.value === "string" ? source.value : "";
}
