<script setup lang="ts">
import { useAuthStore } from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">SETTINGS</p><h2>医生账号设置</h2><p>当前版本展示登录态和实时通知策略。</p></div></header>
    <div class="panel-body">
      <div class="summary-strip">
        <div class="summary-item"><span>医生</span><strong>{{ auth.session?.name }}</strong></div>
        <div class="summary-item"><span>角色</span><strong>{{ auth.session?.role }}</strong></div>
        <div class="summary-item"><span>通知</span><strong>WebSocket + 轮询降级</strong></div>
      </div>
    </div>
  </section>
</template>
