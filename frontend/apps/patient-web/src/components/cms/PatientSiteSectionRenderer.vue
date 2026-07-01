<script setup lang="ts">
import { computed } from "vue";
import type { PatientSiteSection } from "@smart-cloud-brain/shared-api";
import { sectionRendererMap } from "./sectionRendererMap";

const props = defineProps<{ section: PatientSiteSection }>();

const sectionComponent = computed(() => sectionRendererMap[props.section.type]);
</script>

<template>
  <component :is="sectionComponent" :section="section" />
</template>
