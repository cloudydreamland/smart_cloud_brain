import type { Component } from "vue";
import type { PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import CardGridSectionEditor from "./CardGridSectionEditor.vue";
import ContactPanelSectionEditor from "./ContactPanelSectionEditor.vue";
import CtaSectionEditor from "./CtaSectionEditor.vue";
import DepartmentLinksSectionEditor from "./DepartmentLinksSectionEditor.vue";
import FaqSectionEditor from "./FaqSectionEditor.vue";
import GallerySectionEditor from "./GallerySectionEditor.vue";
import HeroSectionEditor from "./HeroSectionEditor.vue";
import ImageTextSectionEditor from "./ImageTextSectionEditor.vue";
import LinkGridSectionEditor from "./LinkGridSectionEditor.vue";
import NoticeSectionEditor from "./NoticeSectionEditor.vue";
import RichTextSectionEditor from "./RichTextSectionEditor.vue";
import ResourceListSectionEditor from "./ResourceListSectionEditor.vue";
import StatsSectionEditor from "./StatsSectionEditor.vue";
import TimelineSectionEditor from "./TimelineSectionEditor.vue";

export const sectionEditorMap: Record<PatientSiteSectionType, Component> = {
  notice: NoticeSectionEditor,
  rich_text: RichTextSectionEditor,
  card_grid: CardGridSectionEditor,
  faq: FaqSectionEditor,
  timeline: TimelineSectionEditor,
  cta: CtaSectionEditor,
  link_grid: LinkGridSectionEditor,
  department_links: DepartmentLinksSectionEditor,
  image_text: ImageTextSectionEditor,
  hero: HeroSectionEditor,
  gallery: GallerySectionEditor,
  contact_panel: ContactPanelSectionEditor,
  stats: StatsSectionEditor,
  doctor_list: ResourceListSectionEditor,
  department_list: ResourceListSectionEditor,
};
