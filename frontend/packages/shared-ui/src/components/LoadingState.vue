<script setup lang="ts">
defineProps<{ title?: string; message?: string }>();
</script>

<template>
  <div class="loading-state">
    <strong>{{ title || "正在加载" }}</strong>
    <span>{{ message || "请稍候，数据正在同步。" }}</span>
  </div>
</template>
