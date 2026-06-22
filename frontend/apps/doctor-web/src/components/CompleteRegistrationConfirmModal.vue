<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <ConfirmDialog
    :open="open"
    title="完成接诊"
    message="确认完成当前接诊？请确保病历和处方已经处理。"
    confirm-text="完成接诊"
    :busy="busy"
    @close="$emit('close')"
    @confirm="$emit('confirm')"
  />
</template>
