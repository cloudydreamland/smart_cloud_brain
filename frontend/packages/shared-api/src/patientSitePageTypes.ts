import type { RouteTargetConfig } from "./patientSiteTypes";

export type PatientSitePagesConfig = {
  pages: PatientSitePageConfig[];
};

export type PatientSitePageConfig = {
  routeName: string;
  slug?: string;
  label: string;
  title: string;
  intro: string;
  enabled?: boolean;
  sort?: number;
  seo?: PatientSiteSeoConfig;
  sections: PatientSiteSection[];
};

export type PatientSiteSeoConfig = {
  title?: string;
  description?: string;
};

export type PatientSiteSection =
  | NoticeSection
  | RichTextSection
  | CardGridSection
  | FaqSection
  | TimelineSection
  | CtaSection
  | LinkGridSection
  | DepartmentLinksSection;

export type PatientSiteSectionType = PatientSiteSection["type"];

export type BaseSection = {
  id: string;
  type: string;
  title?: string;
  enabled?: boolean;
  sort?: number;
};

export type NoticeSection = BaseSection & {
  type: "notice";
  level: "info" | "warning" | "success";
  text: string;
};

export type RichTextSection = BaseSection & {
  type: "rich_text";
  body: string;
};

export type CardGridSection = BaseSection & {
  type: "card_grid";
  cards: PatientSiteCard[];
};

export type PatientSiteCard = {
  title: string;
  text: string;
  meta?: string;
  image?: PatientSiteImage;
  target?: RouteTargetConfig;
};

export type PatientSiteImage = {
  url: string;
  alt: string;
};

export type FaqSection = BaseSection & {
  type: "faq";
  items: PatientSiteFaqItem[];
};

export type PatientSiteFaqItem = {
  question: string;
  answer: string;
};

export type TimelineSection = BaseSection & {
  type: "timeline";
  items: PatientSiteTimelineItem[];
};

export type PatientSiteTimelineItem = {
  title: string;
  text: string;
  time?: string;
};

export type CtaSection = BaseSection & {
  type: "cta";
  text: string;
  primary?: RouteTargetConfig;
  secondary?: RouteTargetConfig;
};

export type LinkGridSection = BaseSection & {
  type: "link_grid";
  links: RouteTargetConfig[];
};

export type DepartmentLinksSection = BaseSection & {
  type: "department_links";
  limit?: number;
  fallbackNames: string[];
  links: RouteTargetConfig[];
};

export type SectionFieldSchema =
  | { key: string; label: string; kind: "text" | "textarea" | "number" | "boolean"; required?: boolean }
  | { key: string; label: string; kind: "select"; options: readonly string[]; required?: boolean }
  | { key: string; label: string; kind: "route_target" | "route_target_list" | "image" | "card_list" | "faq_list" | "timeline_list" | "string_list"; required?: boolean };

export type SectionRegistryItem<T extends PatientSiteSection = PatientSiteSection> = {
  type: T["type"];
  label: string;
  description: string;
  fields: SectionFieldSchema[];
  createDefault: () => T;
};
