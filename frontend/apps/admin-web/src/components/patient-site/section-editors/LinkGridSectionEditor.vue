<script setup lang="ts">
import type { LinkGridSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: LinkGridSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>links</strong>
      <button type="button" class="topbar-refresh" @click="section.links.push(emptyLink())">Add link</button>
    </div>
    <div v-for="(link, linkIndex) in section.links" :key="`section-link-${linkIndex}`" class="config-row-card">
      <div class="config-grid three">
        <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
      </div>
      <button type="button" class="danger-link" @click="section.links.splice(linkIndex, 1)">Delete link</button>
    </div>
  </div>
</template>
