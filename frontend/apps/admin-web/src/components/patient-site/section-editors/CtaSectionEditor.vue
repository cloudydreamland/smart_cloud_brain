<script setup lang="ts">
import type { CtaSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: CtaSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });
</script>

<template>
  <div class="nested-list">
    <label><span>说明文案</span><textarea v-model.trim="section.text" rows="3"></textarea></label>
    <div class="config-grid two">
      <div>
        <div class="nested-list-head">
          <strong>主按钮</strong>
          <button v-if="!section.primary" type="button" class="topbar-refresh" @click="section.primary = emptyLink()">添加</button>
        </div>
        <RouteTargetEditor v-if="section.primary" :model="section.primary" prefix="primary" :patient-route-options="patientRouteOptions" />
      </div>
      <div>
        <div class="nested-list-head">
          <strong>次按钮</strong>
          <button v-if="!section.secondary" type="button" class="topbar-refresh" @click="section.secondary = emptyLink()">添加</button>
        </div>
        <RouteTargetEditor v-if="section.secondary" :model="section.secondary" prefix="secondary" :patient-route-options="patientRouteOptions" />
      </div>
    </div>
  </div>
</template>
