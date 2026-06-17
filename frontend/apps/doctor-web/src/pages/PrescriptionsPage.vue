<script setup lang="ts">
import { storeToRefs } from "pinia";
import { fieldText, statusClass, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, StatusTag } from "@smart-cloud-brain/shared-ui";

const workflow = useDoctorWorkflowStore();
const { prescriptions } = storeToRefs(workflow);
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">PRESCRIPTIONS</p><h2>处方记录</h2><p>查看已创建处方和风险等级。</p></div></header>
    <div class="panel-body">
      <div v-if="prescriptions.length" class="list">
        <article v-for="item in prescriptions" :key="String(item.prescriptionId)" class="list-row">
          <div class="row-main"><strong>处方 #{{ fieldText(item, "prescriptionId") }}</strong><p>{{ fieldText(item, "createdAt") }} · {{ fieldText(item, "status") }}</p></div>
          <StatusTag :status="fieldText(item, 'riskLevel', '未审核')" :tone="statusClass(item.riskLevel)" />
        </article>
      </div>
      <EmptyState v-else title="暂无处方" message="创建处方后会显示在这里。" />
    </div>
  </section>
</template>
