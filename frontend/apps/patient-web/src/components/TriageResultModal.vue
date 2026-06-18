<script setup lang="ts">
import { Modal, StatusTag } from "@smart-cloud-brain/shared-ui";
import { fieldText, statusClass, statusText, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; result: DataRow | null }>();
defineEmits<{ close: []; doctors: [] }>();
</script>

<template>
  <Modal :open="open" title="分诊结果" description="智能分诊仅用于挂号推荐，最终诊断以医生接诊为准。" @close="$emit('close')">
    <div v-if="result" class="triage-result">
      <StatusTag :status="statusText(result.status)" :tone="statusClass(result.status)" />
      <h3>{{ fieldText(result, "recommendedDepartment", "待人工确认") }}</h3>
      <p>{{ fieldText(result, "reason", "暂无说明") }}</p>
    </div>
    <template #footer>
      <button type="button" @click="$emit('close')">继续编辑</button>
      <button type="button" class="primary" @click="$emit('doctors')">查看号源</button>
    </template>
  </Modal>
</template>
