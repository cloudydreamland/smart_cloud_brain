<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { aiTaskLabel, displayText, statusClass, statusText, useAdminWorkflowStore } from "@smart-cloud-brain/shared-api";
import { StatusTag } from "@smart-cloud-brain/shared-ui";
import AdminAnalyticsSection from "../components/AdminAnalyticsSection.vue";

const workflow = useAdminWorkflowStore();
const { departments, doctors, drugs, schedules, triageDesk, aiLogs } = storeToRefs(workflow);
const highRisk = computed(() => triageDesk.value.filter((item) => ["MANUAL_REQUIRED", "HIGH"].includes(String(item.status))).length);
</script>

<template>
  <section>
    <div class="dashboard">
      <!-- Metric Cards -->
      <div class="metrics">
        <div class="metric-card">
          <div class="metric-card-head">
            <div class="metric-icon admin-metric-icon-indigo">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>
            </div>
            <span class="metric-label">科室</span>
          </div>
          <div class="metric-value">{{ departments.length }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-card-head">
            <div class="metric-icon admin-metric-icon-violet">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <span class="metric-label">医生</span>
          </div>
          <div class="metric-value blue">{{ doctors.length }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-card-head">
            <div class="metric-icon admin-metric-icon-emerald">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
            </div>
            <span class="metric-label">药品</span>
          </div>
          <div class="metric-value">{{ drugs.length }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-card-head">
            <div class="metric-icon admin-metric-icon-amber">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
            <span class="metric-label">待处理分诊</span>
          </div>
          <div class="metric-value orange">{{ highRisk }}</div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="panel">
        <div class="panel-header">
          <strong>运营入口</strong>
        </div>
        <div class="quick-actions">
          <RouterLink class="quick-btn" to="/departments">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9Z"/></svg>
            维护科室
          </RouterLink>
          <RouterLink class="quick-btn" to="/doctors">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            维护医生
          </RouterLink>
          <RouterLink class="quick-btn" to="/drugs">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Z"/><path d="M9 12h6M12 9v6" stroke-linecap="round"/></svg>
            维护药品
          </RouterLink>
          <RouterLink class="quick-btn" to="/schedule">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
            生成排班
          </RouterLink>
          <RouterLink class="quick-btn" to="/triage-desk">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 14l2 2 4-4"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
            分诊工作台
          </RouterLink>
        </div>
      </div>

      <!-- Two column tables -->
      <div class="content-grid">
        <div class="panel">
          <div class="panel-header">
            <strong>最近号源</strong>
            <RouterLink to="/schedule">查看全部 →</RouterLink>
          </div>
          <table>
            <thead>
              <tr><th>科室</th><th>医生</th><th>日期</th><th>时段</th><th>状态</th></tr>
            </thead>
            <tbody>
              <tr v-for="item in schedules.slice(0, 5)" :key="String(item.id)">
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.doctorName) }}</td>
                <td>{{ displayText(item.workDate) }}</td>
                <td>{{ displayText(item.timeRange) }}</td>
                <td>
                  <StatusTag :status="displayText(item.status)" :tone="statusClass(item.status)" />
                </td>
              </tr>
              <tr v-if="!schedules.length">
                <td class="admin-empty-table-cell" colspan="5">暂无排班数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="panel">
          <div class="panel-header">
            <strong>AI 日志</strong>
          </div>
          <table>
            <thead>
              <tr><th>任务类型</th><th>Provider</th><th>耗时</th><th>状态</th></tr>
            </thead>
            <tbody>
              <tr v-for="item in aiLogs.slice(0, 5)" :key="String(item.requestId || item.createdAt)">
                <td><strong>{{ aiTaskLabel(item.taskType) }}</strong></td>
                <td>{{ displayText(item.provider) }}</td>
                <td>{{ displayText(item.latencyMs, "0") }}ms</td>
                <td>
                  <StatusTag :status="displayText(item.status)" :tone="statusClass(item.status)" />
                </td>
              </tr>
              <tr v-if="!aiLogs.length">
                <td class="admin-empty-table-cell" colspan="4">暂无 AI 日志</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <AdminAnalyticsSection />
    </div>
  </section>
</template>
