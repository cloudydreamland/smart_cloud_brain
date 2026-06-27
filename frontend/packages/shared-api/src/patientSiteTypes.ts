import type { PatientSitePagesConfig } from "./patientSitePageTypes";

export type RouteTargetConfig = {
  label: string;
  routeName: string;
  slug?: string;
  query?: Record<string, string>;
  description?: string;
  enabled?: boolean;
  sort?: number;
};

export type PatientNavMenu = {
  key: string;
  label: string;
  enabled?: boolean;
  sort?: number;
  lead?: string;
  description?: string;
  links?: RouteTargetConfig[];
  feature?: RouteTargetConfig;
};

export type PatientNavConfig = {
  brand: { name: string; homeRoute: string };
  menus: PatientNavMenu[];
  userLinks: RouteTargetConfig[];
};

export type PatientHomeConfig = {
  hero: {
    eyebrow?: string;
    title: string;
    primaryAction?: RouteTargetConfig;
    secondaryAction?: RouteTargetConfig;
    enabled?: boolean;
  };
  modules: PatientHomeModule[];
};

export type PatientHomeModule = {
  type: string;
  key: string;
  enabled?: boolean;
  sort?: number;
  content?: Record<string, unknown>;
};

export type StaticPageConfig = {
  routeName: string;
  label: string;
  title: string;
  intro: string;
  enabled?: boolean;
  sort?: number;
  points: { title: string; text: string }[];
  primary?: RouteTargetConfig;
};

export type PatientStaticPagesConfig = {
  pages: StaticPageConfig[];
};

export type PatientSiteConfig = {
  nav: PatientNavConfig;
  home: PatientHomeConfig;
  staticPages: PatientStaticPagesConfig;
  pages: PatientSitePagesConfig;
};
