<script setup lang="ts">
import type { StatsSection } from "@smart-cloud-brain/shared-api";

defineProps<{
  section: StatsSection;
}>();

function removeStat(section: StatsSection, index: number) {
  const item = section.items[index];
  if (!item || !window.confirm(`确认删除指标「${item.label || item.value || "未命名指标"}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  section.items.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>指标列表</strong>
      <button type="button" class="topbar-refresh" @click="section.items.push({ label: '指标', value: '0' })">新增指标</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`stat-${itemIndex}`" class="config-row-card">
      <div class="config-grid three">
        <label><span>数值</span><input v-model.trim="item.value" type="text"></label>
        <label><span>名称</span><input v-model.trim="item.label" type="text"></label>
        <label><span>说明</span><input v-model.trim="item.caption" type="text"></label>
      </div>
      <button type="button" class="danger-link" @click="removeStat(section, itemIndex)">删除指标</button>
    </div>
  </div>
</template>
