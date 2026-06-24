<template>
  <div class="admin-not-found-page">
    <div class="admin-not-found-card">
      <h1 class="admin-not-found-title">404</h1>
      <p class="admin-not-found-message">页面不存在</p>
      <a class="admin-not-found-link" href="/">
        返回首页
      </a>
    </div>
  </div>
</template>

<style scoped>
.admin-not-found-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  font-family: system-ui, sans-serif;
  color: var(--color-gray-600);
}

.admin-not-found-card {
  text-align: center;
}

.admin-not-found-title {
  margin: 0;
  color: var(--color-gray-900);
  font-size: var(--font-size-404);
  font-weight: 800;
}

.admin-not-found-message {
  margin: var(--space-4) 0 var(--space-9);
  font-size: var(--font-size-lg);
}

.admin-not-found-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--space-3) var(--space-9);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  color: var(--color-gray-700);
  font-size: var(--font-size-base);
  text-decoration: none;
  transition: background 0.15s;
}
</style>
