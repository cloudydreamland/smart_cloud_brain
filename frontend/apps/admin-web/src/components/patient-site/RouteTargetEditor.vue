<script setup lang="ts">
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";

defineProps<{
  model: RouteTargetConfig;
  prefix: string;
  patientRouteOptions: readonly { name: string; label: string }[];
  includeSort?: boolean;
  includeEnabled?: boolean;
}>();

function targetLabel(prefix: string) {
  const labels: Record<string, string> = {
    action: "操作入口",
    feature: "特色入口",
    item: "入口",
    link: "链接",
    primary: "主按钮",
    secondary: "次按钮",
    target: "目标",
    userLink: "用户入口",
  };
  return labels[prefix] || prefix;
}
</script>

<template>
  <label><span>{{ targetLabel(prefix) }}文案</span><input v-model.trim="model.label" type="text"></label>
  <label>
    <span>{{ targetLabel(prefix) }}路由</span>
    <select v-model="model.routeName">
      <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
    </select>
  </label>
  <label v-if="model.routeName === 'cms-page'"><span>{{ targetLabel(prefix) }} slug</span><input v-model.trim="model.slug" type="text" placeholder="hospital-guide"></label>
  <label v-if="includeSort"><span>{{ targetLabel(prefix) }}排序</span><input v-model.number="model.sort" type="number"></label>
  <label v-if="includeEnabled" class="check-field"><input v-model="model.enabled" type="checkbox"><span>启用</span></label>
</template>
