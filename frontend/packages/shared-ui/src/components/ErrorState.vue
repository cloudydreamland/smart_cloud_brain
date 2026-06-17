<script setup lang="ts">
defineProps<{ title?: string; message?: string }>();
</script>

<template>
  <div class="error-state">
    <strong>{{ title || "请求失败" }}</strong>
    <span>{{ message || "服务暂不可用，请稍后重试。" }}</span>
    <slot />
  </div>
</template>
