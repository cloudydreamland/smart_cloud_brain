<script setup lang="ts">
defineProps<{ status?: string | number | null; tone?: string }>();
</script>

<template>
  <span class="tag" :class="tone">{{ status ?? "-" }}</span>
</template>
