<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import {
  demoNotifications,
  demoPrescriptions,
  demoRecords,
  demoRegistrations,
  formatTime,
  patientName,
  riskText,
  statusLabel,
  statusTone,
  withDemo,
} from "../doctorPresentation";

const workflow = useDoctorWorkflowStore();
const { registrations, records, prescriptions, notifications } = storeToRefs(workflow);
const rows = withDemo(registrations, demoRegistrations);
const displayRecords = withDemo(records, demoRecords);
const displayPrescriptions = withDemo(prescriptions, demoPrescriptions);
const displayNotifications = withDemo(notifications, demoNotifications);

const activeRegistrations = computed(() => rows.value.filter((item) => fieldText(item, "status").toUpperCase() !== "COMPLETED"));
const completed = computed(() => rows.value.filter((item) => fieldText(item, "status").toUpperCase() === "COMPLETED").length);
const unread = computed(() => displayNotifications.value.filter((item) => fieldText(item, "readStatus").toUpperCase() !== "READ").length);
const highRisk = computed(() => displayPrescriptions.value.filter((item) => fieldText(item, "riskLevel").toUpperCase() === "HIGH").length);
const queueTotal = computed(() => Math.max(rows.value.length, 1));
const completionRate = computed(() => Math.round((completed.value / queueTotal.value) * 100));
const workloadBars = computed(() => [
  { label: "待接诊", value: activeRegistrations.value.length, width: `${Math.max(8, Math.round((activeRegistrations.value.length / queueTotal.value) * 100))}%` },
  { label: "已完成", value: completed.value, width: `${Math.max(8, completionRate.value)}%` },
  { label: "高风险", value: highRisk.value, width: `${Math.max(8, Math.min(100, highRisk.value * 18))}%` },
]);
</script>

<template>
  <section class="dashboard-v2">
    <!-- Flow Steps -->
    <div class="flow-strip">
      <div class="flow-step done">
        <div class="step-num">01</div>
        <div class="step-label">登录</div>
      </div>
      <div class="flow-step done">
        <div class="step-num">02</div>
        <div class="step-label">查看首页</div>
      </div>
      <div class="flow-step active">
        <div class="step-num">03</div>
        <div class="step-label">筛选队列</div>
      </div>
      <div class="flow-step">
        <div class="step-num">04</div>
        <div class="step-label">接诊病历</div>
      </div>
      <div class="flow-step">
        <div class="step-num">05</div>
        <div class="step-label">处方审核</div>
      </div>
      <div class="flow-step">
        <div class="step-num">06</div>
        <div class="step-label">通知闭环</div>
      </div>
    </div>

    <!-- Metrics -->
    <div class="metrics">
      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon" style="background: #e0f2f1; color: #0b5f78;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h10M4 18h14" stroke-linecap="round"/></svg>
          </div>
          <span class="metric-label">待接诊</span>
        </div>
        <div class="metric-value teal">{{ activeRegistrations.length }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon" style="background: #ecfdf5; color: #16a34a;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22,4 12,14.01 9,11.01"/></svg>
          </div>
          <span class="metric-label">完成率</span>
        </div>
        <div class="metric-value green">{{ completed }} / {{ queueTotal }}</div>
        <div class="metric-sub">完成率 {{ completionRate }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon" style="background: #eff6ff; color: #2563eb;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 3h7v7M14 14l7 7M3 10V4a1 1 0 0 1 1-1h6M10 21H4a1 1 0 0 1-1-1v-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <span class="metric-label">AI 草稿</span>
        </div>
        <div class="metric-value blue">{{ displayRecords.filter((item) => item.aiGenerated).length }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-card-head">
          <div class="metric-icon" style="background: #fef2f2; color: #dc2626;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <span class="metric-label">高风险处方</span>
        </div>
        <div class="metric-value orange">{{ highRisk }}</div>
      </div>
    </div>

    <!-- Quick Actions -->
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
      </div>
    </div>

    <!-- Two Column: Queue + Sidebar -->
    <div class="content-grid">
      <!-- Left: Queue Table -->
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
              <td>#{{ fieldText(item, "registrationId") }}</td>
              <td>{{ fieldText(item, "departmentName") }}</td>
              <td>{{ formatTime(fieldText(item, "appointmentTime")) }}</td>
              <td><span class="status-tag" :class="statusTone(item.riskLevel)">{{ riskText(item) }}</span></td>
              <td><span class="status-tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td><RouterLink class="action-link" :to="`/consult/${item.registrationId}`">接诊</RouterLink></td>
            </tr>
            <tr v-if="!activeRegistrations.length">
              <td colspan="7" style="text-align:center;color:#71717a;padding:24px;">暂无待接诊患者</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Right: Workload + Notifications -->
      <div class="side-panel">
        <div class="panel workload-card">
          <div class="panel-header" style="border:0; padding:0 0 4px;">
            <strong>工作负荷</strong>
          </div>
          <div class="workload-rows">
            <div v-for="item in workloadBars" :key="item.label" class="workload-row">
              <span>{{ item.label }}</span>
              <div class="bar-track"><div class="bar-fill" :class="item.label === '高风险' ? 'orange' : item.label === '已完成' ? 'green' : 'teal'" :style="{ width: item.width }"></div></div>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <div class="panel notification-card">
          <div class="panel-header" style="border:0; padding:0 0 4px;">
            <strong>风险与未读</strong>
            <RouterLink to="/notifications">全部 →</RouterLink>
          </div>
          <div v-for="item in displayNotifications.slice(0, 4)" :key="String(item.notificationId)" class="notif-item" :class="{ unread: fieldText(item, 'readStatus', 'UNREAD').toUpperCase() !== 'READ' }">
            <div>
              <strong>{{ fieldText(item, "title") }}</strong>
              <span>{{ fieldText(item, "content") }}</span>
            </div>
            <span class="notif-tag" :class="statusTone(item.riskLevel)">{{ statusLabel(fieldText(item, "riskLevel", "INFO")) }}</span>
          </div>
          <div v-if="!displayNotifications.length" style="text-align:center;color:#71717a;padding:20px;font-size:13px;">暂无通知</div>
        </div>
      </div>
    </div>
  </section>
</template>
