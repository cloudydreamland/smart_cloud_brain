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
      <div class="metric"><span>科室</span><strong>{{ departments.length }}</strong></div>
      <div class="metric"><span>医生</span><strong>{{ doctors.length }}</strong></div>
      <div class="metric"><span>药品</span><strong>{{ drugs.length }}</strong></div>
      <div class="metric"><span>待处理分诊</span><strong>{{ highRisk }}</strong></div>
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
