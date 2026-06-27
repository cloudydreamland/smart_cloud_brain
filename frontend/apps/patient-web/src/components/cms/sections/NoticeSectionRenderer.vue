<script setup lang="ts">
import type { NoticeSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: NoticeSection }>();
</script>

<template>
  <section class="cms-section cms-notice" :class="`cms-notice--${section.level}`">
    <h2 v-if="section.title">{{ section.title }}</h2>
    <p>{{ section.text }}</p>
  </section>
</template>
