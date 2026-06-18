<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, statusClass, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, StatusTag } from "@smart-cloud-brain/shared-ui";

const workflow = useDoctorWorkflowStore();
const { registrations, records, prescriptions, notifications } = storeToRefs(workflow);
const unread = computed(() => notifications.value.filter((item) => String(item.readStatus) !== "READ").length);
const completed = computed(() => registrations.value.filter((item) => fieldText(item, "status") === "COMPLETED").length);
const active = computed(() => registrations.value.filter((item) => fieldText(item, "status") !== "COMPLETED").length);
const highRisk = computed(() => prescriptions.value.filter((item) => fieldText(item, "riskLevel").toUpperCase() === "HIGH").length);
const queueTotal = computed(() => Math.max(registrations.value.length, 1));
const completionRate = computed(() => Math.round((completed.value / queueTotal.value) * 100));
const workloadBars = computed(() => [
  { label: "待接诊", value: active.value, width: `${Math.max(4, Math.round((active.value / queueTotal.value) * 100))}%` },
  { label: "已完成", value: completed.value, width: `${Math.max(4, completionRate.value)}%` },
  { label: "高风险", value: highRisk.value, width: `${Math.max(4, Math.min(100, highRisk.value * 16))}%` },
]);
</script>

<template>
  <section class="clinical-page dashboard-workbench">
    <header class="clinical-statusbar">
      <div class="status-cell">
        <span>队列数</span>
        <strong>{{ registrations.length }}</strong>
      </div>
      <div class="status-cell">
        <span>完成率</span>
        <strong>{{ completionRate }}%</strong>
      </div>
      <div class="status-cell">
        <span>病历数</span>
        <strong>{{ records.length }}</strong>
      </div>
      <div class="status-cell">
        <span>处方数</span>
        <strong>{{ prescriptions.length }}</strong>
      </div>
      <div class="status-cell">
        <span>未读通知</span>
        <strong>{{ unread }}</strong>
      </div>
    </header>

    <div class="dashboard-grid">
      <section class="clinical-section dashboard-queue">
        <header class="section-toolbar">
          <h2>今日待办</h2>
          <RouterLink class="button primary compact-action" to="/queue">队列</RouterLink>
        </header>
        <div class="table-scroll">
          <table v-if="registrations.length" class="clinical-table">
            <thead>
              <tr>
                <th>患者</th>
                <th>挂号</th>
                <th>科室</th>
                <th>预约时间</th>
                <th>状态</th>
                <th class="actions-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in registrations.slice(0, 10)" :key="String(item.registrationId)">
                <td><strong>{{ fieldText(item, "patientName", `患者${fieldText(item, "patientId")}`) }}</strong></td>
                <td>#{{ fieldText(item, "registrationId") }}</td>
                <td>{{ fieldText(item, "departmentName", "-") }}</td>
                <td>{{ fieldText(item, "appointmentTime", "-") }}</td>
                <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
                <td><RouterLink class="button compact-action" :to="`/consult/${item.registrationId}`">接诊</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-else title="暂无队列" message="" />
        </div>
      </section>

      <aside class="clinical-section dashboard-side">
        <header class="section-toolbar">
          <h2>负荷</h2>
        </header>
        <div class="workload-chart">
          <div v-for="item in workloadBars" :key="item.label" class="workload-row">
            <span>{{ item.label }}</span>
            <div class="bar-track"><i :style="{ width: item.width }" /></div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </aside>

      <aside class="clinical-section dashboard-side">
        <header class="section-toolbar">
          <h2>风险与未读</h2>
          <RouterLink class="button compact-action" to="/notifications">全部</RouterLink>
        </header>
        <div class="clinical-feed">
          <article v-for="item in notifications.slice(0, 6)" :key="String(item.notificationId)" class="feed-row">
            <strong>{{ item.title }}</strong>
            <span>{{ item.content }}</span>
          </article>
          <EmptyState v-if="!notifications.length" title="暂无通知" message="" />
        </div>
      </aside>
    </div>
  </section>
</template>
