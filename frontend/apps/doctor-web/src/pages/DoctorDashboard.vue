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
  <section class="clinical-page dashboard-workbench">
    <div class="metrics">
      <article class="metric">
        <span>待接诊</span>
        <strong>{{ activeRegistrations.length }}</strong>
        <small>按预约时间和风险排序</small>
      </article>
      <article class="metric">
        <span>已完成</span>
        <strong>{{ completed }}</strong>
        <small>完成率 {{ completionRate }}%</small>
      </article>
      <article class="metric">
        <span>AI 草稿</span>
        <strong>{{ displayRecords.filter((item) => item.aiGenerated).length }}</strong>
        <small>医生确认后保存</small>
      </article>
      <article class="metric">
        <span>高风险处方</span>
        <strong>{{ highRisk }}</strong>
        <small>需二次确认</small>
      </article>
      <article class="metric">
        <span>未读通知</span>
        <strong>{{ unread }}</strong>
        <small>风险消息置顶</small>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <header>
          <div class="panel-title">
            <p class="eyebrow">今日待办</p>
            <h2>优先接诊队列</h2>
            <p>按预约时间、风险提示和状态排序。</p>
          </div>
          <RouterLink class="button primary" to="/queue">进入队列</RouterLink>
        </header>
        <div class="table-wrap">
          <table class="queue-table">
            <thead>
              <tr>
                <th>患者</th>
                <th>挂号</th>
                <th>科室</th>
                <th>预约时间</th>
                <th>风险</th>
                <th>状态</th>
                <th class="actions-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in activeRegistrations.slice(0, 8)" :key="String(item.registrationId)">
                <td><strong>{{ patientName(item) }}</strong></td>
                <td>#{{ fieldText(item, "registrationId") }}</td>
                <td>{{ fieldText(item, "departmentName") }}</td>
                <td>{{ formatTime(fieldText(item, "appointmentTime")) }}</td>
                <td><span class="tag" :class="statusTone(item.riskLevel)">{{ riskText(item) }}</span></td>
                <td><span class="tag" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</span></td>
                <td>
                  <RouterLink class="button primary" :to="`/consult/${item.registrationId}`">接诊</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <aside class="stack">
        <section class="panel">
          <header>
            <div class="panel-title">
              <h3>门诊负荷</h3>
              <p>按完成率和风险量动态展示。</p>
            </div>
          </header>
          <div class="workload">
            <div v-for="item in workloadBars" :key="item.label" class="workload-row">
              <span>{{ item.label }}</span>
              <div class="bar"><i :style="{ width: item.width }"></i></div>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="panel">
          <header>
            <div class="panel-title">
              <h3>风险与通知</h3>
              <p>待处理消息集中提醒。</p>
            </div>
            <RouterLink class="button" to="/notifications">全部</RouterLink>
          </header>
          <div class="feed">
            <article v-for="item in displayNotifications.slice(0, 4)" :key="String(item.notificationId)" class="feed-row">
              <div>
                <strong>{{ fieldText(item, "title") }}</strong>
                <span>{{ fieldText(item, "content") }}</span>
              </div>
              <span class="tag" :class="statusTone(item.riskLevel)">{{ statusLabel(fieldText(item, "riskLevel", "INFO")) }}</span>
            </article>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>
