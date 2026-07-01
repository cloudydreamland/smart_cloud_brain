<script setup lang="ts">
import { RouterLink } from "vue-router";
import type { ContactPanelSection } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../../../site-config/routeTarget";

defineProps<{ section: ContactPanelSection }>();
</script>

<template>
  <section class="cms-section cms-contact-panel">
    <div>
      <h2 v-if="section.title">{{ section.title }}</h2>
      <p>{{ section.text }}</p>
      <dl>
        <template v-if="section.phone">
          <dt>电话</dt>
          <dd>{{ section.phone }}</dd>
        </template>
        <template v-if="section.address">
          <dt>地址</dt>
          <dd>{{ section.address }}</dd>
        </template>
        <template v-if="section.workHours">
          <dt>时间</dt>
          <dd>{{ section.workHours }}</dd>
        </template>
      </dl>
      <RouterLink v-if="section.primary" :to="toPatientRoute(section.primary)">{{ section.primary.label }}</RouterLink>
    </div>
    <img v-if="section.image?.url" :src="section.image.url" :alt="section.image.alt" />
  </section>
</template>
