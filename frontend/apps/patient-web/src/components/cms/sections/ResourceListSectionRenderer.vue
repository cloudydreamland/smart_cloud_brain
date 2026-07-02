<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { DepartmentListSection, DoctorListSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: DoctorListSection | DepartmentListSection }>();
</script>

<template>
  <section class="cms-section cms-resource-list">
    <div v-if="section.title" class="cms-section-head">
      <h2>{{ section.title }}</h2>
    </div>
    <div class="cms-department-links__items">
      <RouterLink v-for="link in section.links" :key="`${link.routeName}-${link.slug || link.label}`" :to="toPatientRoute(link)">
        {{ link.label }}
      </RouterLink>
      <span v-for="name in section.fallbackNames" :key="name">{{ name }}</span>
    </div>
  </section>
</template>
