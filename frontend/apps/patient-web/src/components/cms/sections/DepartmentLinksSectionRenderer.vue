<script setup lang="ts">
import { computed } from "vue";
import { RouterLink } from "vue-router";
import type { DepartmentLinksSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

const props = defineProps<{ section: DepartmentLinksSection }>();

const fallbackNames = computed(() => props.section.fallbackNames.slice(0, props.section.limit || 12));
</script>

<template>
  <section class="cms-section cms-department-links">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-department-links__items">
      <RouterLink v-for="link in section.links" :key="`${link.routeName}-${link.label}`" :to="toPatientRoute(link)">
        {{ link.label }}
      </RouterLink>
      <RouterLink v-for="name in fallbackNames" :key="name" :to="{ name: 'public-search', query: { q: name } }">
        {{ name }}
      </RouterLink>
    </div>
  </section>
</template>
