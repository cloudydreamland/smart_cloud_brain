<script setup lang="ts">
import AdminCatalogPage from "../components/AdminCatalogPage.vue";
</script>
<template><AdminCatalogPage entity="knowledge" @refresh="$emit('refresh')" /></template>
