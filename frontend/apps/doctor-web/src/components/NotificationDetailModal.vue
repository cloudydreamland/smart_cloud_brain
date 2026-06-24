<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";
import { displayText, type Notification } from "@smart-cloud-brain/shared-api";
import { statusLabel, statusTone } from "../doctorPresentation";

defineProps<{ open: boolean; notification: Notification | null }>();
defineEmits<{ close: []; read: [] }>();
</script>

<template>
  <Modal :open="open" title="通知详情" @close="$emit('close')">
    <div v-if="notification" class="stack">
      <span class="tag" :class="statusTone(displayText(notification.riskLevel, 'INFO'))">{{ statusLabel(displayText(notification.riskLevel, "INFO")) }}</span>
      <h3>{{ displayText(notification.title) }}</h3>
      <p>{{ displayText(notification.content) }}</p>
    </div>
    <template #footer>
      <button type="button" @click="$emit('close')">关闭</button>
      <button type="button" class="primary" @click="$emit('read')">标记已读</button>
    </template>
  </Modal>
</template>
