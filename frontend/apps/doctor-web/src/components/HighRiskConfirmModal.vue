<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <ConfirmDialog :open="open" title="确认高风险处方" message="审核结果提示存在高风险，请确认已人工复核后再创建处方。" confirm-text="已复核，继续" tone="danger" :busy="busy" @close="$emit('close')" @confirm="$emit('confirm')" />
</template>
