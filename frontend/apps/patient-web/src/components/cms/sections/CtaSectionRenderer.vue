<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { CtaSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: CtaSection }>();
</script>

<template>
  <section class="cms-section cms-cta">
    <div>
      <h2 v-if="section.title">{{ section.title }}</h2>
      <p>{{ section.text }}</p>
    </div>
    <div class="cms-cta__actions">
      <RouterLink v-if="section.primary" :to="toPatientRoute(section.primary)">{{ section.primary.label }}</RouterLink>
      <RouterLink v-if="section.secondary" class="cms-link-secondary" :to="toPatientRoute(section.secondary)">
        {{ section.secondary.label }}
      </RouterLink>
    </div>
  </section>
</template>
