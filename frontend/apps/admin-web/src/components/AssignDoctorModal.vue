<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
defineProps<{ open: boolean; busy?: boolean }>();
defineEmits<{ close: []; confirm: [] }>();
</script>
<template><Modal :open="open" title="分配医生" @close="$emit('close')"><slot /><template #footer><button type="button" :disabled="busy" @click="$emit('close')">取消</button><button type="button" class="primary" :disabled="busy" @click="$emit('confirm')">分配</button></template></Modal></template>
