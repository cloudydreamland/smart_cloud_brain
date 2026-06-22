<script setup lang="ts">
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import { statusLabel } from "../doctorPresentation";

const auth = useAuthStore();
</script>

<template>
  <section class="panel">
    <header>
      <div class="panel-title">
        <p class="eyebrow">医生设置</p>
        <h2>工作台偏好</h2>
        <p>设置默认科室、AI 草稿策略和通知方式。</p>
      </div>
      <button class="primary" type="button">保存设置</button>
    </header>
    <div class="panel-body stack">
      <div class="summary-strip">
        <div class="summary-item"><span>医生</span><strong>{{ auth.session?.name || "陈明 主治医师" }}</strong></div>
        <div class="summary-item"><span>角色</span><strong>{{ statusLabel(auth.session?.role, "医生") }}</strong></div>
        <div class="summary-item"><span>通知</span><strong>WebSocket + 轮询降级</strong></div>
      </div>
      <div class="settings-grid">
        <article class="setting-tile">
          <strong>默认科室</strong>
          <span>呼吸内科，进入工作台后自动加载本科室队列。</span>
        </article>
        <article class="setting-tile">
          <strong>AI 草稿</strong>
          <span>生成后先进入预览，不自动覆盖医生已编辑内容。</span>
        </article>
        <article class="setting-tile">
          <strong>高危提醒</strong>
          <span>高风险处方必须二次确认，通知保持置顶。</span>
        </article>
      </div>
    </div>
  </section>
</template>
