<script setup lang="ts">
import AdminCatalogPage from "../components/AdminCatalogPage.vue";
</script>
<template><AdminCatalogPage entity="prompt" @refresh="$emit('refresh')" /></template>
