<template>
  <div
    :class="[
      'flex flex-wrap items-center gap-2 rounded-2xl border border-[var(--line)] bg-white p-1',
      'shadow-sm',
      $attrs.class,
    ]"
    v-bind="$attrs"
  >
    <template v-for="(tab, i) in tabs" :key="i">
      <!-- Separator -->
      <div
        v-if="tab.type === 'separator'"
        class="w-px h-6 bg-[var(--line)] shrink-0"
      />
      <!-- Tab button -->
      <button
        v-else
        :class="[
          'inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors',
          modelValue === tab.value
            ? 'bg-[var(--surface-alt)] text-[var(--primary)]'
            : 'text-[var(--muted)] hover:bg-[var(--surface-alt)] hover:text-[var(--ink)]',
        ]"
        @click="$emit('update:modelValue', tab.value)"
      >
        <component :is="tab.icon" v-if="tab.icon" class="h-[18px] w-[18px]" />
        {{ tab.label }}
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface TabItem {
  label: string
  value: string
  icon?: Component
  type?: 'separator'
}

interface Props {
  tabs: TabItem[]
  modelValue?: string
}

withDefaults(defineProps<Props>(), {
  modelValue: '',
})

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>
