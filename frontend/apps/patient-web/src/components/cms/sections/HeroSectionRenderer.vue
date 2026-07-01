<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { HeroSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: HeroSection }>();
</script>

<template>
  <section class="cms-section cms-hero">
    <div>
      <span v-if="section.eyebrow">{{ section.eyebrow }}</span>
      <h2 v-if="section.title">{{ section.title }}</h2>
      <p>{{ section.text }}</p>
      <div class="cms-cta__actions">
        <RouterLink v-if="section.primary" :to="toPatientRoute(section.primary)">{{ section.primary.label }}</RouterLink>
        <RouterLink v-if="section.secondary" class="cms-link-secondary" :to="toPatientRoute(section.secondary)">
          {{ section.secondary.label }}
        </RouterLink>
      </div>
    </div>
    <img v-if="section.image?.url" :src="section.image.url" :alt="section.image.alt" />
  </section>
</template>
