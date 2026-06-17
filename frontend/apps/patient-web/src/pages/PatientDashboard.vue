<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, statusClass, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState, LoadingState, StatusTag } from "@smart-cloud-brain/shared-ui";
import PatientHero from "../components/PatientHero.vue";

defineProps<{ bootLoading?: boolean }>();
defineEmits<{ refresh: [] }>();

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { patient, triageHistory, registrations, records, prescriptions, slots } = storeToRefs(workflow);
const latestTriage = computed(() => triageHistory.value[0] ?? workflow.triage ?? null);
const latestRegistration = computed(() => registrations.value[0] ?? null);
const latestRecord = computed(() => records.value[0] ?? null);
const latestPrescription = computed(() => prescriptions.value[0] ?? null);
</script>

<template>
  <PatientHero />
  <LoadingState v-if="bootLoading" title="正在同步患者资料" />
  <template v-else>
    <section class="patient-card-grid">
      <div class="patient-summary-card"><span>当前患者</span><strong>{{ fieldText(patient, "name", auth.session?.name || "-") }}</strong></div>
      <div class="patient-summary-card"><span>推荐科室</span><strong>{{ fieldText(latestTriage, "recommendedDepartment", "待分诊") }}</strong></div>
      <div class="patient-summary-card"><span>可约号源</span><strong>{{ slots.length }}</strong></div>
      <div class="patient-summary-card"><span>诊后记录</span><strong>{{ records.length + prescriptions.length }}</strong></div>
    </section>
    <section class="patient-dashboard-grid">
      <div class="stack">
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h2>当前分诊结果</h2><p>用于挂号推荐，医生接诊时会再次确认。</p></div></header>
          <div class="panel-body">
            <div v-if="latestTriage" class="clinical-note">
              <StatusTag :status="fieldText(latestTriage, 'status')" :tone="statusClass(latestTriage.status)" />
              <h3>{{ fieldText(latestTriage, "recommendedDepartment", "待确认") }}</h3>
              <p>{{ fieldText(latestTriage, "reason", "暂无说明") }}</p>
            </div>
            <EmptyState v-else title="暂无分诊记录" message="请先提交症状信息，系统会给出科室建议。" />
          </div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h2>最近挂号</h2><p>展示当前患者最新预约状态。</p></div></header>
          <div class="panel-body">
            <div v-if="latestRegistration" class="record-card">
              <strong>{{ fieldText(latestRegistration, "departmentName") }} · {{ fieldText(latestRegistration, "doctorName") }}</strong>
              <span>{{ fieldText(latestRegistration, "appointmentTime") }}</span>
              <StatusTag :status="fieldText(latestRegistration, 'status')" :tone="statusClass(latestRegistration.status)" />
            </div>
            <EmptyState v-else title="暂无挂号" message="选择号源后，预约会显示在这里。" />
          </div>
        </section>
      </div>
      <aside class="stack">
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h2>下一步</h2><p>按实际就诊流程继续。</p></div></header>
          <div class="panel-body toolbar">
            <RouterLink class="button primary" :to="latestTriage ? '/doctors' : '/triage'">{{ latestTriage ? "查看号源" : "提交分诊" }}</RouterLink>
            <button type="button" @click="$emit('refresh')">刷新资料</button>
          </div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h2>诊后信息</h2><p>病历和处方由医生保存后同步。</p></div></header>
          <div class="panel-body stack">
            <div v-if="latestRecord" class="clinical-note"><strong>{{ fieldText(latestRecord, "diagnosis") }}</strong><p>{{ fieldText(latestRecord, "chiefComplaint") }}</p></div>
            <div v-if="latestPrescription" class="clinical-note"><strong>处方 #{{ fieldText(latestPrescription, "prescriptionId") }}</strong><p>风险等级：{{ fieldText(latestPrescription, "riskLevel", "未审核") }}</p></div>
            <EmptyState v-if="!latestRecord && !latestPrescription" title="暂无诊后记录" />
          </div>
        </section>
      </aside>
    </section>
  </template>
</template>
