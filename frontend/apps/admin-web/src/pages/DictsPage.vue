<script setup lang="ts">
import AdminCatalogPage from "../components/AdminCatalogPage.vue";
</script>
<template><AdminCatalogPage entity="dict" @refresh="$emit('refresh')" /></template>
