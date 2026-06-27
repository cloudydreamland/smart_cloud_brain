<script setup lang="ts">
import { computed } from "vue";
import type { RichTextSection } from "@smart-cloud-brain/shared-api";

const props = defineProps<{ section: RichTextSection }>();

const paragraphs = computed(() =>
  props.section.body
    .split(/\n{2,}/)
    .map((item) => item.trim())
    .filter(Boolean),
);
</script>

<template>
  <section class="cms-section cms-rich-text">
    <h2 v-if="section.title">{{ section.title }}</h2>
    <p v-for="paragraph in paragraphs" :key="paragraph">{{ paragraph }}</p>
  </section>
</template>
