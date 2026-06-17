<script setup lang="ts">
import { Drawer, EmptyState, StatusTag } from "@smart-cloud-brain/shared-ui";
import { fieldText, statusClass, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; registration: DataRow | null; triage: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Drawer :open="open" title="患者上下文" description="接诊过程中快速查看患者、挂号与分诊信息。" @close="$emit('close')">
    <div v-if="registration" class="stack">
      <div class="summary-strip">
        <div class="summary-item"><span>患者</span><strong>{{ fieldText(registration, "patientName", `患者 ${fieldText(registration, "patientId")}`) }}</strong></div>
        <div class="summary-item"><span>挂号</span><strong>#{{ fieldText(registration, "registrationId") }}</strong></div>
        <div class="summary-item"><span>科室</span><strong>{{ fieldText(registration, "departmentName") }}</strong></div>
      </div>
      <div v-if="triage" class="clinical-note">
        <StatusTag :status="fieldText(triage, 'status')" :tone="statusClass(triage.status)" />
        <p>{{ fieldText(triage, "chiefComplaint") }}</p>
        <p>{{ fieldText(triage, "reason", "暂无分诊说明") }}</p>
      </div>
    </div>
    <EmptyState v-else title="未选择患者" />
  </Drawer>
</template>
