import type { PatientSiteSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";

export type PatientSiteEditorDraftItem = RouteTargetConfig & {
  title?: string;
  meta?: string;
  imageUrl?: string;
  alt?: string;
  imageObjectKey?: string;
};

export type PatientSiteEditorDraftContent = PatientSiteSection & {
  level?: string;
  title?: string;
  description?: string;
  text?: string;
  imageUrl?: string;
  imageAlt?: string;
  imageObjectKey?: string;
  action?: RouteTargetConfig;
  items: PatientSiteEditorDraftItem[];
  fallbackNames: string[];
  limit?: number;
};

export type PatientSiteEditorDraft = RouteTargetConfig & {
  [key: string]: unknown;
  name?: string;
  key?: string;
  sort?: number;
  enabled?: boolean;
  lead?: string;
  description?: string;
  homeRoute?: string;
  logoUrl?: string;
  logoAlt?: string;
  logoObjectKey?: string;
  links: RouteTargetConfig[];
  feature?: RouteTargetConfig;
  primaryAction: RouteTargetConfig;
  secondaryAction: RouteTargetConfig;
  eyebrow?: string;
  title?: string;
  backgroundImageUrl?: string;
  backgroundImageAlt?: string;
  backgroundObjectKey?: string;
  type?: string;
  content: PatientSiteEditorDraftContent;
  intro?: string;
  contentSource?: "static" | "cms-page";
  slug?: string;
  points: Array<{ title: string; text: string }>;
  primary?: RouteTargetConfig;
};

export function emptyPatientSiteEditorDraft(): PatientSiteEditorDraft {
  return {
    label: "",
    routeName: "patient-home",
    links: [],
    primaryAction: { label: "", routeName: "patient-home" },
    secondaryAction: { label: "", routeName: "patient-home" },
    content: {
      id: "",
      type: "notice",
      enabled: true,
      sort: 0,
      level: "info",
      text: "",
      items: [],
      fallbackNames: [],
    },
    points: [],
  };
}
