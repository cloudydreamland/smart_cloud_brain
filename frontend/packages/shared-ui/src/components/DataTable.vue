<script setup lang="ts" generic="T extends Record<string, unknown>">
import EmptyState from "./EmptyState.vue";
import ErrorState from "./ErrorState.vue";
import LoadingState from "./LoadingState.vue";

defineProps<{
  rows: T[];
  loading?: boolean;
  error?: string;
  loadingTitle?: string;
  loadingMessage?: string;
  errorTitle?: string;
  emptyTitle?: string;
  emptyMessage?: string;
  scrollClass?: string;
}>();
</script>

<template>
  <LoadingState v-if="loading" :title="loadingTitle" :message="loadingMessage" />
  <ErrorState v-else-if="error" :title="errorTitle" :message="error" />
  <EmptyState v-else-if="!rows.length" :title="emptyTitle" :message="emptyMessage" />
  <div v-else class="table-scroll" :class="scrollClass">
    <table class="data-table">
      <slot />
    </table>
  </div>
</template>
