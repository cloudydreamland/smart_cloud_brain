import type { Component } from "vue";
import type { PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import CardGridSectionEditor from "./CardGridSectionEditor.vue";
import CtaSectionEditor from "./CtaSectionEditor.vue";
import DepartmentLinksSectionEditor from "./DepartmentLinksSectionEditor.vue";
import FaqSectionEditor from "./FaqSectionEditor.vue";
import LinkGridSectionEditor from "./LinkGridSectionEditor.vue";
import NoticeSectionEditor from "./NoticeSectionEditor.vue";
import RichTextSectionEditor from "./RichTextSectionEditor.vue";
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
};
