<script setup lang="ts">
import type { StatsSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: StatsSection }>();
</script>

<template>
  <section class="cms-section cms-stats">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-stats__items">
      <article v-for="item in section.items" :key="`${item.value}-${item.label}`">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
        <p v-if="item.caption">{{ item.caption }}</p>
      </article>
    </div>
  </section>
</template>
