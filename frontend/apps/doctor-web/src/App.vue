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
  <div v-if="error" class="doctor-error-screen">
    <div class="doctor-error-card">
      <h2 class="doctor-error-title">页面出错了</h2>
      <p class="doctor-error-message">{{ error }}</p>
      <button class="doctor-error-action" @click="error = null; $router.go(0)">
        刷新页面
      </button>
    </div>
  </div>
  <RouterView v-else />
</template>

<style scoped>
.doctor-error-screen {
  display: grid;
  place-items: center;
  min-height: 100vh;
  font-family: system-ui, sans-serif;
}

.doctor-error-card {
  padding: var(--space-10);
  text-align: center;
}

.doctor-error-title {
  margin: 0 0 var(--space-4);
  color: var(--color-red-600);
}

.doctor-error-message {
  margin: 0 0 var(--space-9);
  color: var(--color-gray-600);
}

.doctor-error-action {
  padding: var(--space-3) var(--space-9);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  background: var(--color-white);
  cursor: pointer;
  font-size: var(--font-size-base);
}
</style>
