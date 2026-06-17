<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
defineProps<{ open: boolean }>();
defineEmits<{ close: [] }>();
</script>
<template><Modal :open="open" title="科室编辑" @close="$emit('close')"><slot /></Modal></template>
