<script setup lang="ts">
import { Modal, EmptyState, StatusTag } from "@smart-cloud-brain/shared-ui";
import { fieldText, statusClass, statusText, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; prescription: DataRow | null }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Modal :open="open" title="处方详情" description="请按医嘱用药，如有疑问请联系医生。" @close="$emit('close')">
    <div v-if="prescription" class="stack">
      <StatusTag :status="statusText(prescription.riskLevel, '未审核')" :tone="statusClass(prescription.riskLevel)" />
      <dl class="detail-list">
        <div><dt>处方号</dt><dd>#{{ fieldText(prescription, "prescriptionId") }}</dd></div>
        <div><dt>状态</dt><dd>{{ statusText(prescription.status) }}</dd></div>
      </dl>
      <ul>
        <li v-for="(item, index) in ((prescription.items as DataRow[]) || [])" :key="index">
          {{ fieldText(item, "drugName") }} / {{ fieldText(item, "dosage") }} / {{ fieldText(item, "frequency") }} / {{ fieldText(item, "usageMethod") }}
        </li>
      </ul>
    </div>
    <EmptyState v-else title="未选择处方" message="请选择一张处方查看明细。" />
  </Modal>
</template>
