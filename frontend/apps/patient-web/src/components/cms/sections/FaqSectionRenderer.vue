<script setup lang="ts">
import type { FaqSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: FaqSection }>();
</script>

<template>
  <section class="cms-section cms-faq">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <details v-for="item in section.items" :key="item.question">
      <summary>{{ item.question }}</summary>
      <p>{{ item.answer }}</p>
    </details>
  </section>
</template>
