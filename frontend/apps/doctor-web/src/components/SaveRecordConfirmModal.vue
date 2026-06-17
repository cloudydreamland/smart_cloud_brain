<script setup lang="ts">
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <ConfirmDialog :open="open" title="保存病历" message="确认保存当前病历？保存后患者端可查看。" confirm-text="保存病历" :busy="busy" @close="$emit('close')" @confirm="$emit('confirm')" />
</template>
