<script setup lang="ts">
import AdminCatalogPage from "../components/AdminCatalogPage.vue";
</script>
<template><AdminCatalogPage entity="department" @refresh="$emit('refresh')" /></template>
