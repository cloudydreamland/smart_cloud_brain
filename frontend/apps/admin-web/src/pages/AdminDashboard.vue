<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { aiSourceLabel, aiSourceTone, fieldText, statusClass, useAdminWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, StatusTag } from "@smart-cloud-brain/shared-ui";

const workflow = useAdminWorkflowStore();
const { departments, doctors, drugs, schedules, triageDesk, aiLogs } = storeToRefs(workflow);
const highRisk = computed(() => triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(String(item.status))).length);
</script>

<template>
  <section>
    <div class="metrics">
      <div class="metric" style="--accent: #3b82f6">
        <div class="metric-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>
        </div>
        <div class="metric-info">
          <span>科室</span>
          <strong>{{ departments.length }}</strong>
        </div>
      </div>
      <div class="metric" style="--accent: #8b5cf6">
        <div class="metric-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <div class="metric-info">
          <span>医生</span>
          <strong>{{ doctors.length }}</strong>
        </div>
      </div>
      <div class="metric" style="--accent: #10b981">
        <div class="metric-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
        </div>
        <div class="metric-info">
          <span>药品</span>
          <strong>{{ drugs.length }}</strong>
        </div>
      </div>
      <div class="metric" style="--accent: #f59e0b">
        <div class="metric-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
        </div>
        <div class="metric-info">
          <span>待处理分诊</span>
          <strong>{{ highRisk }}</strong>
        </div>
      </div>
    </div>
    <div class="main-grid admin-grid">
      <section class="panel">
        <header class="panel-header"><div class="panel-title"><h2>运营入口</h2><p>按维护对象进入独立页面。</p></div></header>
        <div class="panel-body toolbar">
          <RouterLink class="button primary" to="/departments">维护科室</RouterLink>
          <RouterLink class="button" to="/doctors">维护医生</RouterLink>
          <RouterLink class="button" to="/drugs">维护药品</RouterLink>
          <RouterLink class="button" to="/schedule">生成排班</RouterLink>
          <RouterLink class="button" to="/triage-desk">分诊工作台</RouterLink>
        </div>
      </section>
      <aside class="panel">
        <header class="panel-header"><div class="panel-title"><h2>最近号源</h2><p>已发布排班概览。</p></div></header>
        <div class="list">
          <article v-for="item in schedules.slice(0, 5)" :key="String(item.id)" class="list-row">
            <div class="row-main"><strong>{{ item.workDate }} {{ item.timeRange }}</strong><p>{{ item.doctorName }} · 容量 {{ item.capacity }}</p></div>
          </article>
          <EmptyState v-if="!schedules.length" title="暂无排班" />
        </div>
      </aside>
      <aside class="panel">
        <header class="panel-header"><div class="panel-title"><h2>AI 日志</h2><p>最近调用的 provider / model。</p></div></header>
        <div class="list">
          <article v-for="item in aiLogs.slice(0, 6)" :key="String(item.requestId || item.createdAt)" class="list-row">
            <div class="row-main">
              <strong>{{ fieldText(item, "taskType", "UNKNOWN") }}</strong>
              <p>{{ fieldText(item, "provider", "-") }} / {{ fieldText(item, "model", "-") }} · {{ fieldText(item, "latencyMs", "0") }}ms</p>
              <span class="tag" :class="aiSourceTone(item.provider)">{{ aiSourceLabel(item.provider) }}</span>
            </div>
            <StatusTag :status="fieldText(item, 'status', 'UNKNOWN')" :tone="statusClass(item.status)" />
          </article>
          <EmptyState v-if="!aiLogs.length" title="暂无 AI 日志" />
        </div>
      </aside>
    </div>
  </section>
</template>
