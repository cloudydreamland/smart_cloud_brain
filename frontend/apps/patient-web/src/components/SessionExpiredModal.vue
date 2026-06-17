<script setup lang="ts">
import { Modal } from "@smart-cloud-brain/shared-ui";

defineProps<{ open: boolean }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <Modal :open="open" title="登录已失效" description="为了保护患者隐私，请重新登录后继续操作。" @close="$emit('close')">
    <template #footer>
      <button type="button" class="primary" @click="$emit('close')">重新登录</button>
    </template>
  </Modal>
</template>
