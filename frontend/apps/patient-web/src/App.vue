<script setup lang="ts">
import { ref, onErrorCaptured } from "vue";

const error = ref<string | null>(null);

onErrorCaptured((err) => {
  console.error("[ErrorBoundary]", err);
  error.value = err instanceof Error ? err.message : "页面加载异常";
  return false;
});
</script>

<template>
  <div v-if="error" style="display:grid;place-items:center;min-height:100vh;font-family:system-ui,sans-serif;">
    <div style="text-align:center;padding:32px;">
      <h2 style="color:#dc2626;margin:0 0 12px;">页面出错了</h2>
      <p style="color:#6b7280;margin:0 0 24px;">{{ error }}</p>
      <button @click="error = null; $router.go(0)" style="padding:10px 24px;border:1px solid #d1d5db;border-radius:8px;background:#fff;cursor:pointer;font-size:14px;">
        刷新页面
      </button>
    </div>
  </div>
  <RouterView v-else />
</template>
