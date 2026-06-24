<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(defineProps<{
  modelValue: number;
  total: number;
  pageSize?: number;
  centered?: boolean;
}>(), {
  pageSize: 8,
  centered: false,
});

const emit = defineEmits<{ "update:modelValue": [value: number] }>();
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / Math.max(1, props.pageSize))));
const start = computed(() => props.total ? (props.modelValue - 1) * props.pageSize + 1 : 0);
const end = computed(() => Math.min(props.total, props.modelValue * props.pageSize));

/** 生成页码按钮列表（含省略号） */
const pageNumbers = computed(() => {
  const total = pageCount.value;
  const current = props.modelValue;
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: (number | "...")[] = [1];
  if (current > 3) pages.push("...");
  const rangeStart = Math.max(2, current - 1);
  const rangeEnd = Math.min(total - 1, current + 1);
  for (let i = rangeStart; i <= rangeEnd; i++) pages.push(i);
  if (current < total - 2) pages.push("...");
  if (total > 1) pages.push(total);
  return pages;
});

function go(page: number) {
  emit("update:modelValue", Math.min(Math.max(1, page), pageCount.value));
}
</script>

<template>
  <!-- 居中页码按钮模式（设计稿样式） -->
  <nav v-if="centered && total > pageSize" class="pagination-centered" aria-label="分页">
    <button type="button" class="page-btn" :disabled="modelValue <= 1" @click="go(modelValue - 1)">‹</button>
    <template v-for="(p, i) in pageNumbers" :key="i">
      <span v-if="p === '...'" class="page-ellipsis">…</span>
      <button v-else type="button" class="page-btn" :class="{ active: p === modelValue }" @click="go(p)">{{ p }}</button>
    </template>
    <button type="button" class="page-btn" :disabled="modelValue >= pageCount" @click="go(modelValue + 1)">›</button>
  </nav>

  <!-- 默认模式（左右分布） -->
  <nav v-else-if="total > pageSize" class="pagination-bar" aria-label="分页">
    <span class="pagination-summary">第 {{ start }}-{{ end }} 条 / 共 {{ total }} 条</span>
    <div class="pagination-actions">
      <button type="button" :disabled="modelValue <= 1" @click="go(modelValue - 1)">上一页</button>
      <span class="pagination-page">{{ modelValue }} / {{ pageCount }}</span>
      <button type="button" :disabled="modelValue >= pageCount" @click="go(modelValue + 1)">下一页</button>
    </div>
  </nav>
</template>
