<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <ConfirmDialog
    :open="open"
    title="取消挂号"
    message="确认取消该挂号记录？取消后需要重新选择号源。"
    confirm-text="确认取消"
    tone="danger"
    :busy="busy"
    @close="$emit('close')"
    @confirm="$emit('confirm')"
  />
</template>
