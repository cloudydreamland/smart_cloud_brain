<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";
defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>
<template><ConfirmDialog :open="open" title="发布号源" message="确认发布当前排班建议并生成号源？" confirm-text="发布" :busy="busy" @close="$emit('close')" @confirm="$emit('confirm')" /></template>
