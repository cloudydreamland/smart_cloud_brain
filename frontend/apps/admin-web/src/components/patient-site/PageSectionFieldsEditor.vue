<script setup lang="ts">
import { computed } from "vue";
import type { PatientSiteSection } from "@smart-cloud-brain/shared-api";
import { sectionEditorMap } from "./section-editors/sectionEditorMap";

const props = defineProps<{
  section: PatientSiteSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const editorComponent = computed(() => sectionEditorMap[props.section.type]);
</script>

<template>
  <div class="page-section-fields">
    <div class="config-grid three">
      <label><span>id</span><input v-model.trim="section.id" type="text"></label>
      <label><span>title</span><input v-model.trim="section.title" type="text"></label>
      <label><span>sort</span><input v-model.number="section.sort" type="number"></label>
      <label class="check-field"><input v-model="section.enabled" type="checkbox"><span>enabled</span></label>
    </div>
    <component :is="editorComponent" :section="section" :patient-route-options="patientRouteOptions" />
  </div>
</template>
