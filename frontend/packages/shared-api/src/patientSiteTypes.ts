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
  brand: {
    name: string;
    homeRoute: string;
    logoUrl?: string;
    logoAlt?: string;
    logoObjectKey?: string;
  };
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
    backgroundImageUrl?: string;
    backgroundImageAlt?: string;
    backgroundObjectKey?: string;
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

export type PatientHospitalLocationConfig = {
  name: string;
  address: string;
  phone?: string;
  imageUrl?: string;
  imageObjectKey?: string;
  sort?: number;
  enabled?: boolean;
};

export type PatientHospitalInfoConfig = {
  name: string;
  intro?: string;
  address?: string;
  phone?: string;
  workHours?: string;
  website?: string;
  locations: PatientHospitalLocationConfig[];
};

export type PatientFooterConfig = {
  brandName: string;
  description?: string;
  copyright?: string;
  contactPhone?: string;
  contactAddress?: string;
  links: RouteTargetConfig[];
  legalLinks: RouteTargetConfig[];
};

export type PatientSiteConfig = {
  nav: PatientNavConfig;
  home: PatientHomeConfig;
  staticPages: PatientStaticPagesConfig;
  pages: PatientSitePagesConfig;
  hospitalInfo: PatientHospitalInfoConfig;
  footer: PatientFooterConfig;
};
