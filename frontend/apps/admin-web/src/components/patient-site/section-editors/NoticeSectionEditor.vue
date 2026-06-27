<script setup lang="ts">
import type { NoticeSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: NoticeSection }>();
</script>

<template>
  <div class="config-grid two">
    <label>
      <span>提示级别</span>
      <select v-model="section.level">
        <option value="info">info</option>
        <option value="warning">warning</option>
        <option value="success">success</option>
      </select>
    </label>
    <label><span>提示正文</span><textarea v-model.trim="section.text" rows="3"></textarea></label>
  </div>
</template>
