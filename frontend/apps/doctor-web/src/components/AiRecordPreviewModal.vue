<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { formatAiDraft } from "../doctorPresentation";

defineProps<{ open: boolean; text: string }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Modal :open="open" title="AI 病历草稿预览" description="医生确认后才能保存病历。" @close="$emit('close')">
    <pre class="ai-draft">{{ text || formatAiDraft() }}</pre>
    <template #footer>
      <button type="button" @click="$emit('close')">返回编辑</button>
      <button type="button" class="primary" @click="$emit('close')">确认采用</button>
    </template>
  </Modal>
</template>
