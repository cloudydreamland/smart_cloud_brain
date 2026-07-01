<script setup lang="ts">
import type { GallerySection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: GallerySection }>();
</script>

<template>
  <section class="cms-section cms-gallery">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-gallery__items">
      <figure v-for="image in section.images" :key="`${image.url}-${image.alt}`">
        <img :src="image.url" :alt="image.alt" />
        <figcaption v-if="image.alt">{{ image.alt }}</figcaption>
      </figure>
    </div>
  </section>
</template>
