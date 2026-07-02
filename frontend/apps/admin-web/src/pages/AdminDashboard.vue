<script setup lang="ts">
import { computed, onMounted, provide, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  aiTaskLabel,
  displayText,
  statusClass,
  statusText,
  useAdminWorkflowStore,
} from "@smart-cloud-brain/shared-api";
import { StatusTag } from "@smart-cloud-brain/shared-ui";
import { useAdminAnalytics } from "../composables/useAdminAnalytics";
import AdminAnalyticsOverview from "../components/AdminAnalyticsOverview.vue";
import AdminAnalyticsSection from "../components/AdminAnalyticsSection.vue";
import { hasAdminPermission, loadAdminPermissions, PATIENT_SITE_MANAGE_PERMISSION } from "../adminPermissions";

const workflow = useAdminWorkflowStore();
const { schedules, aiLogs } = storeToRefs(workflow);
const permissions = ref<Set<string>>(new Set());
const canManagePatientSite = computed(() => hasAdminPermission(permissions.value, PATIENT_SITE_MANAGE_PERMISSION));

const analytics = useAdminAnalytics();
provide("adminAnalytics", analytics);

onMounted(() => {
  void loadAdminPermissions().then((items) => {
    permissions.value = items;
  });
  analytics.refresh();
});
</script>

<template>
  <section>
    <div class="dashboard">
      <!-- 1. 运营数据概览：标题 + 日期筛选 + 指标卡 -->
      <AdminAnalyticsOverview />

      <!-- 2. 运营入口 + 最近号源 + AI日志 -->
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
          <RouterLink class="quick-btn" to="/patients">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            患者管理
          </RouterLink>
          <RouterLink class="quick-btn" to="/drugs">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Z"/><path d="M9 12h6M12 9v6" stroke-linecap="round"/></svg>
            维护药品
          </RouterLink>
          <RouterLink class="quick-btn" to="/devices">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6v0a6 6 0 0 0 6-6v-4"/><line x1="2" y1="21" x2="22" y2="21"/></svg>
            设备管理
          </RouterLink>
          <RouterLink class="quick-btn" to="/schedule">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
            生成排班
          </RouterLink>
          <RouterLink class="quick-btn" to="/triage-desk">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 14l2 2 4-4"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
            分诊工作台
          </RouterLink>
          <RouterLink class="quick-btn" to="/accounts">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            账号管理
          </RouterLink>
          <RouterLink class="quick-btn" to="/permissions">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            权限管理
          </RouterLink>
          <RouterLink v-if="canManagePatientSite" class="quick-btn" to="/patient-site">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            患者端配置
          </RouterLink>
        </div>
      </div>

      <div class="content-grid">
        <div class="panel">
          <div class="panel-header">
            <strong>最近号源</strong>
            <RouterLink to="/schedule">查看全部 →</RouterLink>
          </div>
          <table>
            <thead>
              <tr>
                <th>科室</th>
                <th>医生</th>
                <th>日期</th>
                <th>时段</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in schedules.slice(0, 5)"
                :key="String(item.id)"
              >
                <td>{{ displayText(item.departmentName) }}</td>
                <td>{{ displayText(item.doctorName) }}</td>
                <td>{{ displayText(item.workDate) }}</td>
                <td>{{ displayText(item.timeRange) }}</td>
                <td>
                  <StatusTag
                    :status="displayText(item.status)"
                    :tone="statusClass(item.status)"
                  />
                </td>
              </tr>
              <tr v-if="!schedules.length">
                <td class="admin-empty-table-cell" colspan="5">
                  暂无排班数据
                </td>
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
              <tr>
                <th>任务类型</th>
                <th>Provider</th>
                <th>耗时</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in aiLogs.slice(0, 5)"
                :key="String(item.requestId || item.createdAt)"
              >
                <td>
                  <strong>{{ aiTaskLabel(item.taskType) }}</strong>
                </td>
                <td>{{ displayText(item.provider) }}</td>
                <td>
                  {{ displayText(item.latencyMs, "0") }}ms
                </td>
                <td>
                  <StatusTag
                    :status="displayText(item.status)"
                    :tone="statusClass(item.status)"
                  />
                </td>
              </tr>
              <tr v-if="!aiLogs.length">
                <td class="admin-empty-table-cell" colspan="4">
                  暂无 AI 日志
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 4. 图表区域 -->
      <AdminAnalyticsSection />
    </div>
  </section>
</template>
