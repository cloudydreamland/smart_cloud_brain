<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { CardGridSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: CardGridSection }>();
</script>

<template>
  <section class="cms-section cms-card-grid">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-card-grid__items">
      <article v-for="card in section.cards" :key="card.title" class="cms-card">
        <img v-if="card.image" :src="card.image.url" :alt="card.image.alt" />
        <span v-if="card.meta">{{ card.meta }}</span>
        <h3>{{ card.title }}</h3>
        <p>{{ card.text }}</p>
        <RouterLink v-if="card.target" :to="toPatientRoute(card.target)">{{ card.target.label }}</RouterLink>
      </article>
    </div>
  </section>
</template>
