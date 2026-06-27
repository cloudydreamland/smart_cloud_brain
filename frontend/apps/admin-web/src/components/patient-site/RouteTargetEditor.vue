<script setup lang="ts">
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";

defineProps<{
  model: RouteTargetConfig;
  prefix: string;
  patientRouteOptions: readonly { name: string; label: string }[];
  includeSort?: boolean;
  includeEnabled?: boolean;
}>();
</script>

<template>
  <label><span>{{ prefix }}.label</span><input v-model.trim="model.label" type="text"></label>
  <label>
    <span>{{ prefix }}.routeName</span>
    <select v-model="model.routeName">
      <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
    </select>
  </label>
  <label v-if="model.routeName === 'cms-page'"><span>{{ prefix }}.slug</span><input v-model.trim="model.slug" type="text" placeholder="hospital-guide"></label>
  <label v-if="includeSort"><span>{{ prefix }}.sort</span><input v-model.number="model.sort" type="number"></label>
  <label v-if="includeEnabled" class="check-field"><input v-model="model.enabled" type="checkbox"><span>enabled</span></label>
</template>
