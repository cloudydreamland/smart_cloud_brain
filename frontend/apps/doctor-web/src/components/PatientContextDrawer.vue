<script setup lang="ts">
import { Drawer } from "@smart-cloud-brain/shared-ui";
import { fieldText, type DataRow } from "@smart-cloud-brain/shared-api";
import { patientName, statusLabel, statusTone } from "../doctorPresentation";

defineProps<{ open: boolean; registration: DataRow | null; triage: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Drawer :open="open" title="患者上下文" description="接诊过程中快速查看患者、挂号与分诊信息。" @close="$emit('close')">
    <div v-if="registration" class="stack">
      <section class="panel">
        <header>
          <div class="panel-title"><h3>基本信息</h3></div>
        </header>
        <div class="dl-grid">
          <div><b>姓名</b><span>{{ patientName(registration) }}</span></div>
          <div><b>患者 ID</b><span>{{ fieldText(registration, "patientId") }}</span></div>
          <div><b>挂号 ID</b><span>#{{ fieldText(registration, "registrationId") }}</span></div>
          <div><b>医保类型</b><span>职工医保</span></div>
          <div><b>科室</b><span>{{ fieldText(registration, "departmentName") }}</span></div>
          <div><b>预约</b><span>{{ fieldText(registration, "appointmentTime") }}</span></div>
        </div>
      </section>

      <section class="panel">
        <header>
          <div class="panel-title"><h3>分诊记录</h3></div>
          <span class="tag" :class="statusTone(triage?.status)">{{ statusLabel(triage?.status, "中风险") }}</span>
        </header>
        <div class="panel-body stack">
          <div class="notice">
            <strong>主诉：</strong>{{ fieldText(triage, "chiefComplaint", fieldText(registration, "chiefComplaint", "待补充分诊主诉")) }}
          </div>
          <div class="notice">
            <strong>既往史：</strong>{{ fieldText(triage, "pastHistory", "青霉素过敏；无糖尿病、高血压病史。") }}
          </div>
          <div class="notice warning">
            <strong>系统建议：</strong>{{ fieldText(triage, "reason", "存在发热和黄痰，建议呼吸内科优先接诊并完成感染指标检查。") }}
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
