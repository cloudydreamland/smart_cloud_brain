<template>
  <Teleport to="body">
    <div class="fixed bottom-6 right-6 z-[200] flex flex-col gap-2 pointer-events-none">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="translate-x-full opacity-0"
        enter-to-class="translate-x-0 opacity-100"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="translate-x-0 opacity-100"
        leave-to-class="translate-x-full opacity-0"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'flex items-center justify-between min-w-[280px] max-w-[380px] px-4 py-3',
            'rounded-xl border bg-white shadow-lg pointer-events-auto',
            toastVariantClasses(toast.variant),
          ]"
        >
          <div class="flex items-start gap-2.5">
            <!-- Icon -->
            <svg v-if="toast.variant === 'success'" class="h-4 w-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            <svg v-else-if="toast.variant === 'error'" class="h-4 w-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
            <svg v-else-if="toast.variant === 'warning'" class="h-4 w-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>
            </svg>
            <svg v-else class="h-4 w-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
            </svg>
            <div>
              <div class="text-[13px] font-semibold">{{ toast.title }}</div>
              <div v-if="toast.message" class="text-[13px] text-[var(--muted)]">{{ toast.message }}</div>
            </div>
          </div>
          <button
            class="flex-shrink-0 flex items-center justify-center h-6 w-6 rounded-full text-[var(--muted)] hover:bg-[var(--surface-alt)] transition-colors"
            @click="remove(toast.id)"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'

type ToastVariant = 'default' | 'success' | 'error' | 'warning'

interface ToastItem {
  id: number
  title: string
  message: string
  variant: ToastVariant
}

const toasts = ref<ToastItem[]>([])
let nextId = 0

function toastVariantClasses(variant: ToastVariant) {
  const map: Record<ToastVariant, string> = {
    default: 'border-[var(--line)]',
    success: 'border-[var(--success)]/40',
    error: 'border-[var(--danger)]/40',
    warning: 'border-[var(--warning)]/40',
  }
  return map[variant]
}

function add(title: string, message = '', variant: ToastVariant = 'default', duration = 4000) {
  const id = nextId++
  toasts.value.push({ id, title, message, variant })
  if (duration > 0) {
    setTimeout(() => remove(id), duration)
  }
  return id
}

function remove(id: number) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

function success(title: string, message?: string) { return add(title, message, 'success') }
function error(title: string, message?: string) { return add(title, message, 'error') }
function warning(title: string, message?: string) { return add(title, message, 'warning') }
function info(title: string, message?: string) { return add(title, message, 'default') }

defineExpose({ add, remove, success, error, warning, info })
</script>
