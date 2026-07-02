<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { formatDateTime, type AppointmentSlot, type Patient } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; slot: AppointmentSlot | null; visitor: Patient | null; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <Modal :open="open" title="确认预约" description="请核对就诊人、科室、医生和就诊时间。" @close="$emit('close')">
    <div v-if="slot" class="summary-strip">
      <div class="summary-item"><span>就诊人</span><strong>{{ visitor?.name || "未选择" }}</strong></div>
      <div class="summary-item"><span>科室</span><strong>{{ slot.departmentName || "未定科室" }}</strong></div>
      <div class="summary-item"><span>医生</span><strong>{{ slot.doctorName || "未定医生" }}</strong></div>
      <div class="summary-item"><span>时间</span><strong>{{ formatDateTime(slot.startTime) }}</strong></div>
    </div>
    <div class="appointment-warning notice warning">分诊结果仅作为挂号推荐，最终诊断以医生接诊结论为准。</div>
    <template #footer>
      <button type="button" :disabled="busy" @click="$emit('close')">更换号源</button>
      <button type="button" class="primary" :disabled="busy || !slot" @click="$emit('confirm')">{{ busy ? "提交中" : "确认挂号" }}</button>
    </template>
  </Modal>
</template>

<style scoped>
.appointment-warning {
  margin-top: 12px;
}
</style>
