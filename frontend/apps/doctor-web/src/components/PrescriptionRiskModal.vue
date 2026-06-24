<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { displayText, type Prescription, type PrescriptionCheckResult } from "@smart-cloud-brain/shared-api";
import { statusLabel, statusTone } from "../doctorPresentation";

defineProps<{ open: boolean; result: Prescription | PrescriptionCheckResult | null }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <Modal :open="open" title="处方风险复核" description="高风险处方需要医生二次确认。" @close="$emit('close')">
    <div class="stack">
      <div class="notice danger">
        <strong>{{ statusLabel(displayText(result?.riskLevel, "HIGH")) }}：</strong>
        {{ displayText(result?.suggestions, "患者有青霉素过敏史，当前处方包含阿莫西林胶囊。建议替换为非青霉素类抗菌药，或补充明确的医生确认说明。") }}
      </div>
      <div class="dl-grid">
        <div><b>风险等级</b><span><span class="tag" :class="statusTone(displayText(result?.riskLevel, 'HIGH'))">{{ statusLabel(displayText(result?.riskLevel, "HIGH")) }}</span></span></div>
        <div><b>审核来源</b><span>{{ displayText(result?.provider, "AI 审方") }}</span></div>
        <div class="span"><b>系统建议</b><span>{{ displayText(result?.suggestions, "复核过敏史，调整抗菌药物；保留氨溴索对症处理。") }}</span></div>
      </div>
    </div>
    <template #footer>
      <button type="button" @click="$emit('close')">返回修改</button>
      <button type="button" class="primary" @click="$emit('confirm')">医生确认并继续</button>
    </template>
  </Modal>
</template>
