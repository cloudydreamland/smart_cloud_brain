<script setup lang="ts">
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { formatDateTime, statusClass, statusText, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { Badge, Button, EmptyState, LoadingState, StatusTag } from "@smart-cloud-brain/shared-ui";

defineProps<{ bootLoading?: boolean }>();
defineEmits<{ refresh: [] }>();

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { patient, triageHistory, registrations, records, prescriptions, slots } = storeToRefs(workflow);

const latestTriage = computed(() => triageHistory.value[0] ?? workflow.triage ?? null);
const activeRegistration = computed(() => registrations.value.find((item) => {
  const status = item.status || "";
  return !["COMPLETED", "CANCELLED"].includes(status);
}) ?? registrations.value[0] ?? null);
const latestRecord = computed(() => records.value[0] ?? null);
const latestPrescription = computed(() => prescriptions.value[0] ?? null);
const patientName = computed(() => patient.value?.name || auth.session?.name || "患者");
const nextRoute = computed(() => latestTriage.value ? { name: "patient-doctors" } : { name: "patient-triage" });
const nextText = computed(() => latestTriage.value ? "查看推荐号源" : "开始 AI 分诊");
</script>

<template>
  <LoadingState v-if="bootLoading" title="正在同步患者资料" />
  <template v-else>
    <section class="portal-page-head">
      <div>
        <h1>您好，{{ patientName }}</h1>
        <p>当前就诊旅程会根据分诊、挂号、病历和处方状态自动推进。</p>
      </div>
      <RouterLink :to="nextRoute">
        <Button>{{ nextText }}</Button>
      </RouterLink>
    </section>

    <section class="portal-metrics">
      <article><span>当前患者</span><strong>{{ patientName }}</strong></article>
      <article><span>推荐科室</span><strong>{{ latestTriage?.recommendedDepartment || "待分诊" }}</strong></article>
      <article><span>可预约号源</span><strong>{{ slots.length }} 个</strong></article>
      <article><span>诊后资料</span><strong>{{ records.length + prescriptions.length }} 份</strong></article>
    </section>

    <section class="portal-two-column">
      <article class="portal-feature-card">
        <h2>完成预约的下一步</h2>
        <p v-if="latestTriage">系统已生成推荐科室，优先展示与分诊结果匹配的医生号源。医生接诊时仍会再次确认诊断。</p>
        <p v-else>描述症状后，系统会生成推荐科室和安全提示，并把结果带入号源筛选。</p>
        <RouterLink :to="nextRoute">
          <Button>{{ nextText }}</Button>
        </RouterLink>
        <div class="journey-rail">
          <span class="done"></span>
          <span :class="{ done: latestTriage }"></span>
          <span :class="{ done: activeRegistration }"></span>
          <span :class="{ done: latestRecord || latestPrescription }"></span>
        </div>
      </article>

      <section class="portal-list-card">
        <div v-if="latestTriage" class="portal-list-row">
          <div>
            <strong>当前分诊结果</strong>
            <p>{{ latestTriage?.recommendedDepartment || "待确认" }} · {{ latestTriage?.reason || "暂无说明" }}</p>
          </div>
          <StatusTag :status="statusText(latestTriage.status)" :tone="statusClass(latestTriage.status)" />
        </div>
        <div v-else class="portal-list-row">
          <div><strong>分诊建议</strong><p>尚未提交本次症状。</p></div>
          <Badge variant="warning">待处理</Badge>
        </div>

        <div v-if="activeRegistration" class="portal-list-row">
          <div>
            <strong>最近挂号</strong>
            <p>{{ activeRegistration?.departmentName || "" }} · {{ activeRegistration?.doctorName || "" }} · {{ formatDateTime(activeRegistration?.appointmentTime, "待定") }}</p>
          </div>
          <StatusTag :status="statusText(activeRegistration.status)" :tone="statusClass(activeRegistration.status)" />
        </div>
        <div v-else class="portal-list-row">
          <div><strong>挂号记录</strong><p>暂无未完成挂号。</p></div>
          <Badge variant="muted">0 条</Badge>
        </div>

        <div v-if="latestRecord || latestPrescription" class="portal-list-row">
          <div>
            <strong>诊后资料</strong>
            <p>{{ latestRecord?.diagnosis || "病历待同步" }} · 处方 {{ latestPrescription ? `#${latestPrescription?.prescriptionId || ""}` : "待同步" }}</p>
          </div>
          <RouterLink class="portal-link" :to="{ name: 'patient-records' }">查看</RouterLink>
        </div>
        <EmptyState v-if="!latestTriage && !activeRegistration && !latestRecord && !latestPrescription" title="暂无就诊记录" message="完成分诊后，旅程状态会显示在这里。" />
      </section>
    </section>
  </template>
</template>
