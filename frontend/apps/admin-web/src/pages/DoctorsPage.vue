<script setup lang="ts">
import AdminCatalogPage from "../components/AdminCatalogPage.vue";
</script>
<template><AdminCatalogPage entity="doctor" @refresh="$emit('refresh')" /></template>
