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
  { label: "待接诊", value: active.value, width: `${Math.max(8, Math.round((active.value / queueTotal.value) * 100))}%` },
  { label: "已完成", value: completed.value, width: `${Math.max(8, completionRate.value)}%` },
  { label: "高风险", value: highRisk.value, width: `${Math.max(8, Math.min(100, highRisk.value * 18))}%` },
]);
</script>

<template>
  <section class="dashboard-workbench">
    <header class="workbench-strip">
      <div>
        <p class="eyebrow">TODAY</p>
        <h2>当日接诊工作台</h2>
      </div>
      <div class="status-cells" aria-label="今日工作概览">
        <div><span>队列</span><strong>{{ registrations.length }}</strong></div>
        <div><span>完成率</span><strong>{{ completionRate }}%</strong></div>
        <div><span>病历</span><strong>{{ records.length }}</strong></div>
        <div><span>处方</span><strong>{{ prescriptions.length }}</strong></div>
        <div><span>未读</span><strong>{{ unread }}</strong></div>
      </div>
    </header>

    <div class="dashboard-grid">
      <section class="workbench-section">
        <header class="section-title">
          <div>
            <h3>接诊待办</h3>
            <p>按预约时间和当前队列顺序处理。</p>
          </div>
          <RouterLink class="button primary compact-action" to="/queue">查看队列</RouterLink>
        </header>
        <div class="table-scroll">
          <table v-if="registrations.length" class="data-table compact-table">
            <thead>
              <tr>
                <th>患者</th>
                <th>挂号</th>
                <th>科室</th>
                <th>时间</th>
                <th>状态</th>
                <th class="actions-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in registrations.slice(0, 8)" :key="String(item.registrationId)">
                <td><strong>{{ fieldText(item, "patientName", `患者 ${fieldText(item, "patientId")}`) }}</strong></td>
                <td>#{{ fieldText(item, "registrationId") }}</td>
                <td>{{ fieldText(item, "departmentName", "-") }}</td>
                <td>{{ fieldText(item, "appointmentTime", "-") }}</td>
                <td><StatusTag :status="fieldText(item, 'status')" :tone="statusClass(item.status)" /></td>
                <td><RouterLink class="button compact-action" :to="`/consult/${item.registrationId}`">接诊</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-else title="暂无队列" />
        </div>
      </section>

      <aside class="workbench-section">
        <header class="section-title">
          <div>
            <h3>负荷趋势</h3>
            <p>当前队列、完成和风险占比。</p>
          </div>
        </header>
        <div class="workload-chart">
          <div v-for="item in workloadBars" :key="item.label" class="workload-row">
            <span>{{ item.label }}</span>
            <div class="bar-track"><i :style="{ width: item.width }" /></div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </aside>

      <aside class="workbench-section risk-feed">
        <header class="section-title">
          <div>
            <h3>风险通知</h3>
            <p>处方审核与系统通知。</p>
          </div>
          <RouterLink class="button compact-action" to="/notifications">全部</RouterLink>
        </header>
        <div class="clinical-list">
          <article v-for="item in notifications.slice(0, 5)" :key="String(item.notificationId)" class="clinical-list-row">
            <strong>{{ item.title }}</strong>
            <p>{{ item.content }}</p>
          </article>
          <EmptyState v-if="!notifications.length" title="暂无通知" />
        </div>
      </aside>
    </div>
  </section>
</template>
