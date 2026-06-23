<template>
  <button
    :class="[
      'inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors',
      'outline-offset-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[var(--focus)]',
      'disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0',
      variantClasses,
      sizeClasses,
      $attrs.class,
    ]"
    :disabled="disabled || loading"
    v-bind="$attrs"
  >
    <svg
      v-if="loading"
      class="animate-spin -ml-1 mr-2 h-4 w-4"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'link'
  size?: 'default' | 'sm' | 'lg' | 'icon'
  disabled?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'default',
  disabled: false,
  loading: false,
})

const variantClasses = computed(() => {
  const map: Record<string, string> = {
    default:
      'bg-[var(--primary)] text-white shadow-sm hover:bg-[var(--primary-strong)]',
    secondary:
      'bg-[var(--surface-alt)] text-[var(--ink)] shadow-sm hover:bg-[var(--line)]',
    outline:
      'border border-[var(--line)] bg-white text-[var(--ink)] shadow-sm hover:bg-[var(--surface-alt)]',
    ghost:
      'text-[var(--ink)] hover:bg-[var(--surface-alt)]',
    danger:
      'bg-[var(--danger)] text-white shadow-sm hover:bg-red-700',
    link:
      'text-[var(--primary)] underline-offset-4 hover:underline',
  }
  return map[props.variant]
})

const sizeClasses = computed(() => {
  const map: Record<string, string> = {
    default: 'h-10 px-4 py-2',
    sm: 'h-8 rounded-md px-3 text-xs',
    lg: 'h-11 rounded-md px-8',
    icon: 'h-10 w-10',
  }
  return map[props.size]
})
</script>
