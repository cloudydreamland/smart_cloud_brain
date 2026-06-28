<script setup lang="ts">
import type { NoticeSection } from "@smart-cloud-brain/shared-api";
import { ScbSelect } from "@smart-cloud-brain/shared-ui";

defineProps<{ section: NoticeSection }>();

const levelOptions = [
  { value: "info", label: "info" },
  { value: "warning", label: "warning" },
  { value: "success", label: "success" },
];
</script>

<template>
  <div class="config-grid two">
    <label>
      <span>提示级别</span>
      <ScbSelect v-model="section.level" :options="levelOptions" />
    </label>
    <label><span>提示正文</span><textarea v-model.trim="section.text" rows="3"></textarea></label>
  </div>
</template>
