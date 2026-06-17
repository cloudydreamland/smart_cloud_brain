<script setup lang="ts">
defineProps<{
  groups: Array<{ label: string; items: Array<{ label: string; to: string; badge?: string | number }> }>;
}>();
</script>

<template>
  <nav class="nav-group">
    <template v-for="group in groups" :key="group.label">
      <span>{{ group.label }}</span>
      <RouterLink v-for="item in group.items" :key="item.to" class="nav-item" :to="item.to">
        {{ item.label }}
        <b v-if="item.badge !== undefined">{{ item.badge }}</b>
      </RouterLink>
    </template>
  </nav>
</template>
