<script setup lang="ts">
defineProps<{ title?: string; message?: string }>();
</script>

<template>
  <div class="empty-state">
    <strong>{{ title || "暂无数据" }}</strong>
    <span>{{ message || "当前条件下没有可展示的记录。" }}</span>
    <slot />
  </div>
</template>
