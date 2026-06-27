import type { RouteLocationRaw } from "vue-router";
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";

export function toPatientRoute(link: RouteTargetConfig): RouteLocationRaw {
  if (link.routeName === "cms-page" && link.slug) return { name: "cms-page", params: { slug: link.slug } };
  if (link.routeName === "cms-page") return { name: "patient-home" };
  return link.query ? { name: link.routeName, query: link.query } : { name: link.routeName };
}
