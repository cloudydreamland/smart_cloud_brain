<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { ImageTextSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: ImageTextSection }>();
</script>

<template>
  <section class="cms-section cms-image-text" :class="`cms-image-text--${section.imagePosition}`">
    <img v-if="section.image?.url" :src="section.image.url" :alt="section.image.alt" />
    <div>
      <h2 v-if="section.title">{{ section.title }}</h2>
      <p>{{ section.text }}</p>
      <RouterLink v-if="section.primary" :to="toPatientRoute(section.primary)">{{ section.primary.label }}</RouterLink>
    </div>
  </section>
</template>
