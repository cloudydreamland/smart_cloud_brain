<script setup lang="ts">
import Modal from "./Modal.vue";

withDefaults(defineProps<{
  open: boolean;
  title: string;
  message: string;
  confirmText?: string;
  tone?: "primary" | "danger" | "warning";
  busy?: boolean;
}>(), {
  confirmText: "确认",
  tone: "primary",
  busy: false,
});

defineEmits<{ close: []; confirm: [] }>();
</script>

<template>
  <Modal :open="open" :title="title" :description="message" @close="$emit('close')">
    <div class="notice warning">该操作会提交到后端，请确认信息无误。</div>
    <template #footer>
      <button type="button" :disabled="busy" @click="$emit('close')">取消</button>
      <button type="button" :class="tone" :disabled="busy" @click="$emit('confirm')">{{ busy ? "处理中..." : confirmText }}</button>
    </template>
  </Modal>
</template>
