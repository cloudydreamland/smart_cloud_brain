<template>
  <span
    :class="[
      'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors',
      variantClasses,
      $attrs.class,
    ]"
    v-bind="$attrs"
  >
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'muted'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
})

const variantClasses = computed(() => {
  const map: Record<string, string> = {
    default: 'bg-[var(--surface-alt)] text-[var(--muted)] border border-[var(--line)]',
    primary: 'bg-[var(--primary-soft)] text-[var(--primary)]',
    success: 'bg-[var(--success-soft)] text-[var(--success)]',
    warning: 'bg-[var(--warning-soft)] text-[var(--warning)]',
    danger: 'bg-[var(--danger-soft)] text-[var(--danger)]',
    info: 'bg-[var(--info-soft)] text-[var(--info)]',
    muted: 'bg-[var(--surface-alt)] text-[var(--subtle)]',
  }
  return map[props.variant]
})
</script>
