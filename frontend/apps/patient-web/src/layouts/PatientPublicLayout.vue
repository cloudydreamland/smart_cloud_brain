<script setup lang="ts">
import PatientSiteFooter from "../components/PatientSiteFooter.vue";
import PatientSiteHeader from "../components/PatientSiteHeader.vue";
</script>

<template>
  <div class="patient-page theme-patient patient-public-page">
    <PatientSiteHeader />
    <main class="patient-site-main">
      <RouterView />
    </main>
    <PatientSiteFooter />
  </div>
</template>
