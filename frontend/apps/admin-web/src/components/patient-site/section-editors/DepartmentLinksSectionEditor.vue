<script setup lang="ts">
import type { DepartmentLinksSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: DepartmentLinksSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>limit</span><input v-model.number="section.limit" type="number"></label>
    </div>
    <div class="nested-list-head">
      <strong>links</strong>
      <button type="button" class="topbar-refresh" @click="section.links.push(emptyLink())">Add link</button>
    </div>
    <div v-for="(link, linkIndex) in section.links" :key="`department-link-${linkIndex}`" class="config-row-card">
      <div class="config-grid three">
        <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
      </div>
      <button type="button" class="danger-link" @click="section.links.splice(linkIndex, 1)">Delete link</button>
    </div>
    <div class="nested-list-head">
      <strong>fallbackNames</strong>
      <button type="button" class="topbar-refresh" @click="section.fallbackNames.push('New department')">Add name</button>
    </div>
    <div v-for="(_name, nameIndex) in section.fallbackNames" :key="`fallback-name-${nameIndex}`" class="config-row-card">
      <label><span>name</span><input v-model.trim="section.fallbackNames[nameIndex]" type="text"></label>
      <button type="button" class="danger-link" @click="section.fallbackNames.splice(nameIndex, 1)">Delete name</button>
    </div>
  </div>
</template>
