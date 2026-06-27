<script setup lang="ts">
import type { RichTextSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: RichTextSection }>();
</script>

<template>
  <label><span>body</span><textarea v-model.trim="section.body" rows="6"></textarea></label>
</template>
