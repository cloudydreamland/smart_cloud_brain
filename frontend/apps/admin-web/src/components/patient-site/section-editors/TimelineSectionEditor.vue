<script setup lang="ts">
import type { TimelineSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: TimelineSection }>();

function addTimeline(section: TimelineSection) {
  section.items.push({ title: "New step", text: "" });
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>items</strong>
      <button type="button" class="topbar-refresh" @click="addTimeline(section)">Add step</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`timeline-${itemIndex}`" class="config-row-card">
      <div class="config-grid three">
        <label><span>time</span><input v-model.trim="item.time" type="text"></label>
        <label><span>title</span><input v-model.trim="item.title" type="text"></label>
        <label><span>text</span><textarea v-model.trim="item.text" rows="3"></textarea></label>
      </div>
      <button type="button" class="danger-link" @click="section.items.splice(itemIndex, 1)">Delete step</button>
    </div>
  </div>
</template>
