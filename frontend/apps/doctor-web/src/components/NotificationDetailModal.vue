<script setup lang="ts">
import { Modal, StatusTag } from "@smart-cloud-brain/shared-ui";
import { fieldText, statusClass, type DataRow } from "@smart-cloud-brain/shared-api";

defineProps<{ open: boolean; notification: DataRow | null }>();
defineEmits<{ close: []; read: [] }>();
</script>

<template>
  <Modal :open="open" title="通知详情" @close="$emit('close')">
    <div v-if="notification" class="stack">
      <StatusTag :status="fieldText(notification, 'riskLevel', 'INFO')" :tone="statusClass(notification.riskLevel)" />
      <h3>{{ fieldText(notification, "title") }}</h3>
      <p>{{ fieldText(notification, "content") }}</p>
    </div>
    <template #footer>
      <button type="button" @click="$emit('close')">关闭</button>
      <button type="button" class="primary" @click="$emit('read')">标记已读</button>
    </template>
  </Modal>
</template>
