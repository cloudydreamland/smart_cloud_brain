<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState } from "@smart-cloud-brain/shared-ui";

const workflow = useDoctorWorkflowStore();
const { registrations, records, prescriptions, notifications } = storeToRefs(workflow);
const unread = computed(() => notifications.value.filter((item) => String(item.readStatus) !== "READ").length);
</script>

<template>
  <section>
    <div class="metrics">
      <div class="metric"><span>待接诊</span><strong>{{ registrations.length }}</strong></div>
      <div class="metric"><span>已保存病历</span><strong>{{ records.length }}</strong></div>
      <div class="metric"><span>处方</span><strong>{{ prescriptions.length }}</strong></div>
      <div class="metric"><span>未读通知</span><strong>{{ unread }}</strong></div>
    </div>
    <div class="main-grid">
      <section class="panel">
        <header class="panel-header"><div class="panel-title"><h2>今日队列</h2><p>点击进入接诊页面。</p></div><RouterLink class="button primary" to="/queue">查看队列</RouterLink></header>
        <div class="list">
          <article v-for="item in registrations.slice(0, 6)" :key="String(item.registrationId)" class="list-row">
            <div class="row-main"><strong>{{ item.patientName || `患者 ${item.patientId}` }}</strong><p>#{{ item.registrationId }} · {{ item.departmentName }} · {{ item.appointmentTime }}</p></div>
            <RouterLink class="button" :to="`/consult/${item.registrationId}`">接诊</RouterLink>
          </article>
          <EmptyState v-if="!registrations.length" title="暂无队列" />
        </div>
      </section>
      <aside class="panel">
        <header class="panel-header"><div class="panel-title"><h2>风险通知</h2><p>处方审核与系统通知。</p></div><RouterLink class="button" to="/notifications">全部</RouterLink></header>
        <div class="list">
          <article v-for="item in notifications.slice(0, 4)" :key="String(item.notificationId)" class="list-row">
            <div class="row-main"><strong>{{ item.title }}</strong><p>{{ item.content }}</p></div>
          </article>
          <EmptyState v-if="!notifications.length" title="暂无通知" />
        </div>
      </aside>
    </div>
  </section>
</template>
