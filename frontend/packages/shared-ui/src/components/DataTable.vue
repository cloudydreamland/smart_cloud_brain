<script setup lang="ts" generic="T extends Record<string, unknown>">
import EmptyState from "./EmptyState.vue";
import ErrorState from "./ErrorState.vue";
import LoadingState from "./LoadingState.vue";

defineProps<{
  rows: T[];
  loading?: boolean;
  error?: string;
  emptyTitle?: string;
  emptyMessage?: string;
}>();
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" />
  <EmptyState v-else-if="!rows.length" :title="emptyTitle" :message="emptyMessage" />
  <div v-else class="table-scroll">
    <table class="data-table">
      <slot />
    </table>
  </div>
</template>
