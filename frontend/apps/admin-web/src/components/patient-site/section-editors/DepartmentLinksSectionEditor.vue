<script setup lang="ts">
import type { DepartmentLinksSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: DepartmentLinksSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function removeLink(section: DepartmentLinksSection, index: number) {
  const link = section.links[index];
  if (!link || !window.confirm(`确认删除科室链接「${link.label || "未命名链接"}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  section.links.splice(index, 1);
}

function removeFallbackName(section: DepartmentLinksSection, index: number) {
  const name = section.fallbackNames[index];
  if (!name || !window.confirm(`确认删除科室「${name}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  section.fallbackNames.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>数量上限</span><input v-model.number="section.limit" type="number"></label>
    </div>
    <div class="nested-list-head">
      <strong>固定链接</strong>
      <button type="button" class="topbar-refresh" @click="section.links.push(emptyLink())">新增链接</button>
    </div>
    <div v-for="(link, linkIndex) in section.links" :key="`department-link-${linkIndex}`" class="config-row-card">
      <div class="config-grid three">
        <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
      </div>
      <button type="button" class="danger-link" @click="removeLink(section, linkIndex)">删除链接</button>
    </div>
    <div class="nested-list-head">
      <strong>默认科室名</strong>
      <button type="button" class="topbar-refresh" @click="section.fallbackNames.push('新科室')">新增科室</button>
    </div>
    <div v-for="(_name, nameIndex) in section.fallbackNames" :key="`fallback-name-${nameIndex}`" class="config-row-card">
      <label><span>科室名</span><input v-model.trim="section.fallbackNames[nameIndex]" type="text"></label>
      <button type="button" class="danger-link" @click="removeFallbackName(section, nameIndex)">删除科室</button>
    </div>
  </div>
</template>
