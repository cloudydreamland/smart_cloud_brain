<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean; text: string }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Modal :open="open" title="AI 病历草稿预览" description="医生确认后才能保存。" @close="$emit('close')">
    <pre class="stream-box">{{ text || "暂无生成内容" }}</pre>
  </Modal>
</template>
