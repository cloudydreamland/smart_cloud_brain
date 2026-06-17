<script setup lang="ts">
defineProps<{ label: string; hint?: string }>();
</script>

<template>
  <label class="field">
    <span>{{ label }}</span>
    <slot />
    <span v-if="hint" class="field-hint">{{ hint }}</span>
  </label>
</template>
