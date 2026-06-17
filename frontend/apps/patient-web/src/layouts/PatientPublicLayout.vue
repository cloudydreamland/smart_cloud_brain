<script setup lang="ts">
import PatientHeader from "../components/PatientHeader.vue";
</script>

<template>
  <main class="patient-page theme-patient">
    <PatientHeader public-mode />
    <RouterView />
  </main>
</template>
