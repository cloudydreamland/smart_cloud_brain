<script setup lang="ts">
import { storeToRefs } from "pinia";
import { fieldText, useDoctorWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState } from "@smart-cloud-brain/shared-ui";

const workflow = useDoctorWorkflowStore();
const { records } = storeToRefs(workflow);
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">RECORDS</p><h2>已保存病历</h2><p>快速回看当前医生账号下的病历记录。</p></div></header>
    <div class="panel-body">
      <div v-if="records.length" class="table-scroll">
        <table class="data-table">
          <thead><tr><th>病历号</th><th>患者</th><th>主诉</th><th>诊断</th><th>方式</th></tr></thead>
          <tbody>
            <tr v-for="item in records" :key="String(item.medicalRecordId)">
              <td>#{{ fieldText(item, "medicalRecordId") }}</td>
              <td>{{ fieldText(item, "patientName", fieldText(item, "patientId")) }}</td>
              <td>{{ fieldText(item, "chiefComplaint") }}</td>
              <td>{{ fieldText(item, "diagnosis") }}</td>
              <td>{{ item.aiGenerated ? "AI 草稿确认" : "医生录入" }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无病历" message="保存病历后会显示在这里。" />
    </div>
  </section>
</template>
