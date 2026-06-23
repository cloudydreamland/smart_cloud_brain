<template>
  <div
    :class="[
      'flex items-start gap-3 rounded-lg border px-4 py-3 text-sm',
      variantClasses,
    ]"
    role="alert"
  >
    <!-- Icon -->
    <div class="flex-shrink-0 mt-0.5">
      <slot name="icon">
        <svg v-if="variant === 'info'" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
        </svg>
        <svg v-else-if="variant === 'success'" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <svg v-else-if="variant === 'warning'" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>
        </svg>
        <svg v-else-if="variant === 'danger'" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>
        </svg>
        <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a8 8 0 0 0-8 8c0 3.4 2.1 6.3 5 7.4V22h6v-4.6c2.9-1.1 5-4 5-7.4a8 8 0 0 0-8-8z"/><path d="M12 8v4"/><path d="M10 12h4"/>
        </svg>
      </slot>
    </div>

    <!-- Content -->
    <div class="flex-1 min-w-0">
      <div v-if="title" class="font-semibold leading-tight">{{ title }}</div>
      <div v-if="$slots.default || description" class="mt-1 text-sm opacity-85 leading-relaxed">
        <slot>{{ description }}</slot>
      </div>
    </div>

    <!-- Close button -->
    <button
      v-if="closable"
      class="flex-shrink-0 flex items-center justify-center h-5 w-5 rounded-md text-current opacity-60 hover:opacity-100 transition-opacity"
      @click="$emit('close')"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'info' | 'success' | 'warning' | 'danger' | 'primary'
  title?: string
  description?: string
  closable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'info',
  title: '',
  description: '',
  closable: false,
})

defineEmits<{ close: [] }>()

const variantClasses = computed(() => {
  const map: Record<string, string> = {
    info: 'bg-[var(--info-soft)] border-[var(--info)]/20 text-[var(--ink)] [&_svg]:text-[var(--info)] [&_.font-semibold]:text-[var(--info)]',
    success: 'bg-[var(--success-soft)] border-[var(--success)]/20 text-[var(--ink)] [&_svg]:text-[var(--success)] [&_.font-semibold]:text-[var(--success)]',
    warning: 'bg-[var(--warning-soft)] border-[var(--warning)]/20 text-[var(--ink)] [&_svg]:text-[var(--warning)] [&_.font-semibold]:text-[var(--warning)]',
    danger: 'bg-[var(--danger-soft)] border-[var(--danger)]/20 text-[var(--ink)] [&_svg]:text-[var(--danger)] [&_.font-semibold]:text-[var(--danger)]',
    primary: 'bg-[var(--primary-soft)] border-[var(--primary)]/20 text-[var(--ink)] [&_svg]:text-[var(--primary)] [&_.font-semibold]:text-[var(--primary)]',
  }
  return map[props.variant]
})
</script>
