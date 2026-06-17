<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";
defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>
<template><ConfirmDialog :open="open" title="关闭分诊" message="确认关闭该分诊记录？关闭后将不再进入待处理列表。" confirm-text="关闭分诊" tone="danger" :busy="busy" @close="$emit('close')" @confirm="$emit('confirm')" /></template>
