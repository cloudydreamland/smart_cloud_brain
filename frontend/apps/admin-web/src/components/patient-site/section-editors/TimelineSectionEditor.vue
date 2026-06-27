<script setup lang="ts">
import type { TimelineSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: TimelineSection }>();

function addTimeline(section: TimelineSection) {
  section.items.push({ title: "新步骤", text: "" });
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>步骤列表</strong>
      <button type="button" class="topbar-refresh" @click="addTimeline(section)">新增步骤</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`timeline-${itemIndex}`" class="config-row-card">
      <div class="config-grid three">
        <label><span>时间</span><input v-model.trim="item.time" type="text"></label>
        <label><span>标题</span><input v-model.trim="item.title" type="text"></label>
        <label><span>说明</span><textarea v-model.trim="item.text" rows="3"></textarea></label>
      </div>
      <button type="button" class="danger-link" @click="section.items.splice(itemIndex, 1)">删除步骤</button>
    </div>
  </div>
</template>
