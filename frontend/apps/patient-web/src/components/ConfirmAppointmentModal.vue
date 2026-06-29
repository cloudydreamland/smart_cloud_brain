<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { fieldText, formatDateTime, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; slot: DataRow | null; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <Modal :open="open" title="确认预约" description="请核对科室、医生和就诊时间。" @close="$emit('close')">
    <div v-if="slot" class="summary-strip">
      <div class="summary-item"><span>科室</span><strong>{{ fieldText(slot, "departmentName") }}</strong></div>
      <div class="summary-item"><span>医生</span><strong>{{ fieldText(slot, "doctorName") }}</strong></div>
      <div class="summary-item"><span>时间</span><strong>{{ formatDateTime(fieldText(slot, "startTime")) }}</strong></div>
    </div>
    <div class="notice warning" style="margin-top: 12px">分诊结果仅作为挂号推荐，最终诊断以医生接诊结论为准。</div>
    <template #footer>
      <button type="button" :disabled="busy" @click="$emit('close')">更换号源</button>
      <button type="button" class="primary" :disabled="busy || !slot" @click="$emit('confirm')">{{ busy ? "提交中" : "确认挂号" }}</button>
    </template>
  </Modal>
</template>
