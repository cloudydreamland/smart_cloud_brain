<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[101] flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-slate-950/55 backdrop-blur-md"
          @click="close"
        />

        <!-- Dialog -->
        <Transition
          enter-active-class="transition-all duration-200 ease-out"
          enter-from-class="opacity-0 scale-[0.98] translate-y-3"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition-all duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-[0.98] translate-y-3"
          appear
        >
          <div
            v-if="open"
            :class="[
              'relative w-full overflow-hidden rounded-[22px] border border-white/60 bg-white shadow-[0_24px_70px_rgba(15,35,42,0.18)]',
              'max-h-[calc(100vh-2rem)] overflow-y-auto',
              sizeClasses,
            ]"
            role="dialog"
            :aria-modal="true"
            :aria-labelledby="title ? 'modal-title' : undefined"
          >
            <!-- Close button -->
            <button
              class="absolute top-3 right-3 z-10 flex h-7 w-7 items-center justify-center rounded-lg text-[var(--muted)] transition-colors hover:bg-[var(--surface-alt)] hover:text-[var(--ink)]"
              @click="close"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>

            <!-- Header -->
            <div v-if="title || $slots.header" class="border-b border-[var(--line)]/70 px-6 pb-4 pt-6">
              <slot name="header">
                <h2 id="modal-title" class="text-lg font-semibold text-[var(--ink)]">{{ title }}</h2>
                <p v-if="description" class="text-sm text-[var(--muted)] mt-1">{{ description }}</p>
              </slot>
            </div>

            <!-- Body -->
            <div class="px-6 py-5">
              <slot />
            </div>

            <!-- Footer -->
            <div v-if="$slots.footer" class="border-t border-[var(--line)]/70 bg-[var(--surface-alt)]/40 px-6 py-4">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'

interface Props {
  open: boolean
  title?: string
  description?: string
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  description: '',
  size: 'md',
})

const emit = defineEmits<{ close: [] }>()

const sizeClasses = computed(() => {
  const map: Record<string, string> = {
    sm: 'max-w-[400px]',
    md: 'max-w-[520px]',
    lg: 'max-w-[680px]',
  }
  return map[props.size]
})

function close() {
  emit('close')
}

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.open) close()
}

onMounted(() => document.addEventListener('keydown', handleEscape))
onUnmounted(() => document.removeEventListener('keydown', handleEscape))
</script>
