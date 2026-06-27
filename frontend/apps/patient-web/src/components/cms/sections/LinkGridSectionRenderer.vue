<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { LinkGridSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: LinkGridSection }>();
</script>

<template>
  <section class="cms-section cms-link-grid">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-link-grid__items">
      <RouterLink v-for="link in section.links" :key="`${link.routeName}-${link.label}`" :to="toPatientRoute(link)">
        <strong>{{ link.label }}</strong>
        <span v-if="link.description">{{ link.description }}</span>
      </RouterLink>
    </div>
  </section>
</template>
