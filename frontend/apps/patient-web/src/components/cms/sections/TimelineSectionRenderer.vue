<script setup lang="ts">
import type { TimelineSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: TimelineSection }>();
</script>

<template>
  <section class="cms-section cms-timeline">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <ol>
      <li v-for="(item, index) in section.items" :key="`${item.title}-${index}`">
        <span>{{ item.time || index + 1 }}</span>
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.text }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>
