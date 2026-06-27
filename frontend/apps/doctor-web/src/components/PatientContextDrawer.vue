<script setup lang="ts">
import { computed } from "vue";
import { Drawer } from "@smart-cloud-brain/shared-ui";
import { displayText, formatDateTime, type Registration, type TriageRecord } from "@smart-cloud-brain/shared-api";
import { patientName, statusLabel, statusTone } from "../doctorPresentation";

const props = defineProps<{ open: boolean; registration: Registration | null; triage: TriageRecord | null }>();
defineEmits<{ close: [] }>();

const triageRisk = computed(() => displayText(props.triage?.riskLevel, displayText(props.registration?.riskLevel, "MEDIUM")));
</script>

<template>
  <Drawer :open="open" title="患者上下文" description="接诊过程中快速查看患者、挂号与分诊信息。" @close="$emit('close')">
    <div v-if="registration" class="stack">
      <section class="panel">
        <header>
          <div class="panel-title"><h3>基本信息</h3></div>
        </header>
        <div class="info-card-grid">
          <div class="info-card">
            <span class="info-label">姓名</span>
            <strong class="info-value">{{ patientName(registration) }}</strong>
          </div>
          <div class="info-card">
            <span class="info-label">患者 ID</span>
            <strong class="info-value">{{ displayText(registration.patientId) }}</strong>
          </div>
          <div class="info-card">
            <span class="info-label">挂号 ID</span>
            <strong class="info-value">#{{ displayText(registration.registrationId) }}</strong>
          </div>
          <div class="info-card">
            <span class="info-label">医保类型</span>
            <strong class="info-value">职工医保</strong>
          </div>
          <div class="info-card">
            <span class="info-label">科室</span>
            <strong class="info-value">{{ displayText(registration.departmentName) }}</strong>
          </div>
          <div class="info-card">
            <span class="info-label">预约</span>
            <strong class="info-value">{{ formatDateTime(registration?.appointmentTime) }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <header>
          <div class="panel-title"><h3>分诊记录</h3></div>
          <span class="tag" :class="statusTone(triageRisk)">{{ statusLabel(triageRisk, "中风险") }}</span>
        </header>
        <div class="panel-body stack">
          <div class="notice">
            <strong>主诉：</strong>{{ displayText(triage?.chiefComplaint, displayText(registration.chiefComplaint, "待补充分诊主诉")) }}
          </div>
          <div class="notice">
            <strong>既往史：</strong>{{ displayText(triage?.pastHistory, "青霉素过敏；无糖尿病、高血压病史。") }}
          </div>
          <div class="notice warning">
            <strong>系统建议：</strong>{{ displayText(triage?.reason, "存在发热和黄痰，建议呼吸内科优先接诊并完成感染指标检查。") }}
          </div>
        </div>
      </section>

      <section class="panel">
        <header>
          <div class="panel-title"><h3>历史记录</h3></div>
        </header>
        <div class="feed">
          <article class="feed-row">
            <div><strong>2026-05-18 呼吸内科</strong><span>过敏性鼻炎，开具氯雷他定。</span></div>
            <span class="tag info">门诊</span>
          </article>
          <article class="feed-row">
            <div><strong>2025-12-02 急诊</strong><span>药物过敏记录：青霉素。</span></div>
            <span class="tag danger">过敏</span>
          </article>
        </div>
      </section>
    </div>
    <div v-else class="empty-state">未选择患者</div>
  </Drawer>
</template>

<style scoped>
.info-card-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  transition: border-color 0.15s ease;
}
.info-card:hover {
  border-color: var(--primary, #0b5f78);
}
.info-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-secondary, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.info-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text, #1f2937);
}
</style>
