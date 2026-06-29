<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { displayText, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import {
  formatTime,
  liveRows,
  patientName,
  riskText,
  statusLabel,
  statusTone,
} from "../doctorPresentation";

const workflow = useDoctorWorkflowStore();
const { registrations, records, prescriptions, notifications } = storeToRefs(workflow);
const rows = liveRows(registrations);
const displayRecords = liveRows(records);
const displayPrescriptions = liveRows(prescriptions);
const displayNotifications = liveRows(notifications);

const activeRegistrations = computed(() => rows.value.filter((item) => displayText(item.status).toUpperCase() !== "COMPLETED"));
const completed = computed(() => rows.value.filter((item) => displayText(item.status).toUpperCase() === "COMPLETED").length);
const unread = computed(() => displayNotifications.value.filter((item) => displayText(item.readStatus).toUpperCase() !== "READ").length);
const highRisk = computed(() => displayPrescriptions.value.filter((item) => displayText(item.riskLevel).toUpperCase() === "HIGH").length);
const queueTotal = computed(() => Math.max(rows.value.length, 1));
const completionRate = computed(() => Math.round((completed.value / queueTotal.value) * 100));
const workloadBars = computed(() => [
  { key: "pending", label: "待接诊", tone: "teal", value: activeRegistrations.value.length, width: `${Math.max(8, Math.round((activeRegistrations.value.length / queueTotal.value) * 100))}%` },
  { key: "completed", label: "已完成", tone: "green", value: completed.value, width: `${Math.max(8, completionRate.value)}%` },
  { key: "high-risk", label: "高风险", tone: "orange", value: highRisk.value, width: `${Math.max(8, Math.min(100, highRisk.value * 18))}%` },
]);
</script>

<template>
  <section class="dashboard-v2">
    <div class="metrics">

      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon doctor-metric-icon-teal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h10M4 18h14" stroke-linecap="round"/></svg>
          </div>
          <span class="metric-label">待接诊</span>
        </div>
        <div class="metric-value teal">{{ activeRegistrations.length }}</div>
      </div>

      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon doctor-metric-icon-green">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22,4 12,14.01 9,11.01"/></svg>
          </div>
          <span class="metric-label">完成率</span>
        </div>
        <div class="metric-value green">{{ completed }} / {{ queueTotal }}</div>
        <div class="metric-sub">完成率 {{ completionRate }}%</div>
      </div>

      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon doctor-metric-icon-blue">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 3h7v7M14 14l7 7M3 10V4a1 1 0 0 1 1-1h6M10 21H4a1 1 0 0 1-1-1v-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <span class="metric-label">AI 草稿</span>
        </div>
        <div class="metric-value blue">{{ displayRecords.filter((item) => item.aiGenerated).length }}</div>
      </div>

      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon doctor-metric-icon-red">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <span class="metric-label">高风险处方</span>
        </div>
        <div class="metric-value orange">{{ highRisk }}</div>
      </div>

    </div>

    <!-- Two Column: Left (Quick Actions + Queue) | Right (Workload + Notifications) -->
    <div class="content-grid">
      <!-- Left Column -->
      <div class="left-col">
        <div class="panel">
          <div class="panel-header"><strong>快捷操作</strong></div>
          <div class="quick-actions">
            <RouterLink class="quick-btn" to="/queue">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 6h16M4 12h10M4 18h14"/></svg>
              查看队列
            </RouterLink>
            <RouterLink class="quick-btn" to="/records">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3h7v7M14 14l7 7M3 10V4a1 1 0 0 1 1-1h6M10 21H4a1 1 0 0 1-1-1v-6"/></svg>
              新建病历
            </RouterLink>
            <RouterLink class="quick-btn" to="/prescriptions">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="9" y="5" width="6" height="14" rx="1"/><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/></svg>
              开具处方
            </RouterLink>
            <RouterLink class="quick-btn" to="/notifications">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" stroke-linecap="round" stroke-linejoin="round"/><path d="M13.73 21a2 2 0 0 1-3.46 0" stroke-linecap="round"/></svg>
              查看通知
            </RouterLink>
            <RouterLink class="quick-btn" to="/schedule">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
              查看排班
            </RouterLink>
          </div>
        </div>

        <!-- Queue Table -->
        <div class="panel">
        <div class="panel-header">
          <strong>今日待办 · 优先接诊队列</strong>
          <RouterLink to="/queue">查看全部 →</RouterLink>
        </div>
        <table>
          <thead>
            <tr><th>患者</th><th>挂号</th><th>科室</th><th>预约时间</th><th>风险</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in activeRegistrations.slice(0, 8)" :key="String(item.registrationId)">
              <td><strong>{{ patientName(item) }}</strong></td>
              <td>#{{ displayText(item.registrationId) }}</td>
              <td>{{ displayText(item.departmentName) }}</td>
              <td>{{ formatTime(displayText(item.appointmentTime)) }}</td>
              <td><span class="status-tag" :class="statusTone(item.riskLevel)">{{ riskText(item) }}</span></td>
              <td><span class="status-tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td><RouterLink class="action-link" :to="`/consult/${item.registrationId}`">接诊</RouterLink></td>
            </tr>
            <tr v-if="!activeRegistrations.length">
              <td class="doctor-empty-table-cell" colspan="7">暂无待接诊患者</td>
            </tr>
          </tbody>
        </table>
        </div>
      </div>

      <!-- Right: Workload + Notifications -->
      <div class="side-panel">
        <div class="panel workload-card">
          <div class="panel-header doctor-compact-panel-header">
            <strong>工作负荷</strong>
          </div>
          <div class="workload-rows">
            <div v-for="item in workloadBars" :key="item.label" class="workload-row">
              <span>{{ item.label }}</span>
              <div class="bar-track"><div class="bar-fill" :class="item.tone" :style="{ width: item.width }"></div></div>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <div class="panel notification-card">
          <div class="panel-header doctor-compact-panel-header">
            <strong>风险与未读</strong>
            <RouterLink to="/notifications">全部 →</RouterLink>
          </div>
          <div v-for="item in displayNotifications.slice(0, 4)" :key="String(item.notificationId)" class="notif-item" :class="{ unread: displayText(item.readStatus, 'UNREAD').toUpperCase() !== 'READ' }">
            <div>
              <strong>{{ displayText(item.title) }}</strong>
              <span>{{ displayText(item.content) }}</span>
            </div>
            <span class="notif-tag" :class="statusTone(item.riskLevel)">{{ statusLabel(displayText(item.riskLevel, "INFO")) }}</span>
          </div>
          <div v-if="!displayNotifications.length" class="doctor-empty-notification">暂无通知</div>
        </div>
      </div>
    </div>
  </section>
</template>
