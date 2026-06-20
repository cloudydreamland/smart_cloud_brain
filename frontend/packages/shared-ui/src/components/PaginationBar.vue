<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(defineProps<{
  modelValue: number;
  total: number;
  pageSize?: number;
}>(), {
  pageSize: 8,
});

const emit = defineEmits<{ "update:modelValue": [value: number] }>();
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / Math.max(1, props.pageSize))));
const start = computed(() => props.total ? (props.modelValue - 1) * props.pageSize + 1 : 0);
const end = computed(() => Math.min(props.total, props.modelValue * props.pageSize));

function go(page: number) {
  emit("update:modelValue", Math.min(Math.max(1, page), pageCount.value));
}
</script>

<template>
  <nav v-if="total > pageSize" class="pagination-bar" aria-label="分页">
    <span class="pagination-summary">第 {{ start }}-{{ end }} 条 / 共 {{ total }} 条</span>
    <div class="pagination-actions">
      <button type="button" :disabled="modelValue <= 1" @click="go(modelValue - 1)">上一页</button>
      <span class="pagination-page">{{ modelValue }} / {{ pageCount }}</span>
      <button type="button" :disabled="modelValue >= pageCount" @click="go(modelValue + 1)">下一页</button>
    </div>
  </nav>
</template>
