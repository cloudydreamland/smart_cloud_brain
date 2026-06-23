<template>
  <div
    :class="[
      'bg-white border border-[var(--line)] rounded-2xl p-5 flex items-center gap-4',
      'shadow-sm',
      $attrs.class,
    ]"
    v-bind="$attrs"
  >
    <!-- Icon -->
    <div
      :class="[
        'w-10 h-10 rounded-xl flex items-center justify-center shrink-0',
        iconBgClass,
      ]"
    >
      <slot name="icon">
        <svg v-if="icon === 'patient'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
        </svg>
        <svg v-else-if="icon === 'department'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/><path d="M9 21v-4h6v4"/>
        </svg>
        <svg v-else-if="icon === 'slot'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/>
        </svg>
        <svg v-else-if="icon === 'record'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/>
        </svg>
      </slot>
    </div>

    <!-- Info -->
    <div class="flex-1 min-w-0">
      <div class="text-2xl font-bold">{{ value }}</div>
      <div class="text-xs text-[var(--muted)]">{{ label }}</div>
    </div>

    <!-- Trend -->
    <div v-if="trend" :class="['text-xs font-semibold', trend > 0 ? 'text-[var(--success)]' : 'text-[var(--danger)]']">
      {{ trend > 0 ? '+' : '' }}{{ trend }}%
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  value: string | number
  label: string
  icon?: 'patient' | 'department' | 'slot' | 'record'
  color?: 'primary' | 'success' | 'warning' | 'danger'
  trend?: number
}

const props = withDefaults(defineProps<Props>(), {
  icon: 'patient',
  color: 'primary',
  trend: 0,
})

const iconBgClass = computed(() => {
  const map: Record<string, string> = {
    primary: 'bg-[var(--primary-soft)] text-[var(--primary)]',
    success: 'bg-[var(--success-soft)] text-[var(--success)]',
    warning: 'bg-[var(--warning-soft)] text-[var(--warning)]',
    danger: 'bg-[var(--danger-soft)] text-[var(--danger)]',
  }
  return map[props.color]
})
</script>
