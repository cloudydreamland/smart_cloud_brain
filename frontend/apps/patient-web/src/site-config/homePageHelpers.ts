import { isPatientSiteSectionType, type Department, type Doctor, type PatientRecommendation, type PatientSiteSection } from "@smart-cloud-brain/shared-api";
import type { PatientHomeModule, RouteTargetConfig } from "./types";

const legacyHomeModuleTypes = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);

export function isSectionHomeModule(module: PatientHomeModule) {
  return isPatientSiteSectionType(module.type) && !legacyHomeModuleTypes.has(module.type);
}

export function homeModuleToSection(module: PatientHomeModule): PatientSiteSection {
  return {
    ...contentOf(module),
    id: module.key,
    type: module.type,
    enabled: module.enabled,
    sort: module.sort,
  } as PatientSiteSection;
}

export function contentOf(module: PatientHomeModule | undefined): Record<string, unknown> {
  return record(module?.content);
}

export function contentArray(module: PatientHomeModule | undefined, key: string): unknown[] {
  const value = contentOf(module)[key];
  return Array.isArray(value) ? value : [];
}

export function record(value: unknown): Record<string, unknown> {
  return Boolean(value && typeof value === "object" && !Array.isArray(value)) ? value as Record<string, unknown> : {};
}

export function textValue(value: unknown, fallback: string) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

export function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) && value > 0 ? value : fallback;
}

export function actionValue(value: unknown, fallback: RouteTargetConfig): RouteTargetConfig {
  return isRouteAction(value) ? value : fallback;
}

export function isRouteAction(value: unknown): value is RouteTargetConfig {
  if (!value || typeof value !== "object" || !("label" in value) || !("routeName" in value)) return false;
  const link = value as RouteTargetConfig;
  return link.routeName !== "cms-page" || Boolean(link.slug);
}

export function recommendationTitle(item: PatientRecommendation | Department | Doctor) {
  return String(("title" in item && item.title) || ("targetName" in item && item.targetName) || ("name" in item && item.name) || "科室");
}

export function recommendationDescription(item: PatientRecommendation) {
  return item.description || item.departmentName || item.specialty || "";
}

export function recommendationKey(item: PatientRecommendation | Department | Doctor) {
  return String(("id" in item && item.id) || ("targetId" in item && item.targetId) || ("name" in item && item.name) || recommendationTitle(item));
}

export function onImageError(event: Event) {
  const image = event.target instanceof HTMLImageElement ? event.target : null;
  if (image) image.hidden = true;
}
