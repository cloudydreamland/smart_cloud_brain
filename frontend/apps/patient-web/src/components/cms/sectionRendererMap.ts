import type { Component } from "vue";
import type { PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import CardGridSectionRenderer from "./sections/CardGridSectionRenderer.vue";
import ContactPanelSectionRenderer from "./sections/ContactPanelSectionRenderer.vue";
import CtaSectionRenderer from "./sections/CtaSectionRenderer.vue";
import DepartmentLinksSectionRenderer from "./sections/DepartmentLinksSectionRenderer.vue";
import FaqSectionRenderer from "./sections/FaqSectionRenderer.vue";
import GallerySectionRenderer from "./sections/GallerySectionRenderer.vue";
import HeroSectionRenderer from "./sections/HeroSectionRenderer.vue";
import ImageTextSectionRenderer from "./sections/ImageTextSectionRenderer.vue";
import LinkGridSectionRenderer from "./sections/LinkGridSectionRenderer.vue";
import NoticeSectionRenderer from "./sections/NoticeSectionRenderer.vue";
import RichTextSectionRenderer from "./sections/RichTextSectionRenderer.vue";
import ResourceListSectionRenderer from "./sections/ResourceListSectionRenderer.vue";
import StatsSectionRenderer from "./sections/StatsSectionRenderer.vue";
import TimelineSectionRenderer from "./sections/TimelineSectionRenderer.vue";

export const sectionRendererMap: Record<PatientSiteSectionType, Component> = {
  notice: NoticeSectionRenderer,
  rich_text: RichTextSectionRenderer,
  card_grid: CardGridSectionRenderer,
  faq: FaqSectionRenderer,
  timeline: TimelineSectionRenderer,
  cta: CtaSectionRenderer,
  link_grid: LinkGridSectionRenderer,
  department_links: DepartmentLinksSectionRenderer,
  image_text: ImageTextSectionRenderer,
  hero: HeroSectionRenderer,
  gallery: GallerySectionRenderer,
  contact_panel: ContactPanelSectionRenderer,
  stats: StatsSectionRenderer,
  doctor_list: ResourceListSectionRenderer,
  department_list: ResourceListSectionRenderer,
};
