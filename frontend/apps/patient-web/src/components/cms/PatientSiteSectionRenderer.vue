<script setup lang="ts">
import { computed, type Component } from "vue";
import type { PatientSiteSection, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import CardGridSectionRenderer from "./sections/CardGridSectionRenderer.vue";
import CtaSectionRenderer from "./sections/CtaSectionRenderer.vue";
import DepartmentLinksSectionRenderer from "./sections/DepartmentLinksSectionRenderer.vue";
import FaqSectionRenderer from "./sections/FaqSectionRenderer.vue";
import LinkGridSectionRenderer from "./sections/LinkGridSectionRenderer.vue";
import NoticeSectionRenderer from "./sections/NoticeSectionRenderer.vue";
import RichTextSectionRenderer from "./sections/RichTextSectionRenderer.vue";
import TimelineSectionRenderer from "./sections/TimelineSectionRenderer.vue";

const props = defineProps<{ section: PatientSiteSection }>();

const sectionRenderers: { [Type in PatientSiteSectionType]: Component } = {
  notice: NoticeSectionRenderer,
  rich_text: RichTextSectionRenderer,
  card_grid: CardGridSectionRenderer,
  faq: FaqSectionRenderer,
  timeline: TimelineSectionRenderer,
  cta: CtaSectionRenderer,
  link_grid: LinkGridSectionRenderer,
  department_links: DepartmentLinksSectionRenderer,
};

const sectionComponent = computed(() => sectionRenderers[props.section.type]);
</script>

<template>
  <component :is="sectionComponent" :section="section" />
</template>
