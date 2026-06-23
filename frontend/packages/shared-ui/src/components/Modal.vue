<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[101] flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/60 backdrop-blur-sm"
          @click="close"
        />

        <!-- Dialog -->
        <Transition
          enter-active-class="transition-all duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition-all duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-95 translate-y-2"
          appear
        >
          <div
            v-if="open"
            :class="[
              'relative w-full bg-white rounded-2xl shadow-xl',
              'max-h-[calc(100vh-2rem)] overflow-y-auto',
              sizeClasses,
            ]"
            role="dialog"
            :aria-modal="true"
            :aria-labelledby="title ? 'modal-title' : undefined"
          >
            <!-- Close button -->
            <button
              class="absolute top-3 right-3 flex items-center justify-center h-7 w-7 rounded-lg text-[var(--muted)] hover:bg-[var(--surface-alt)] transition-colors z-10"
              @click="close"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>

            <!-- Header -->
            <div v-if="title || $slots.header" class="px-6 pt-6 pb-4">
              <slot name="header">
                <h2 id="modal-title" class="text-lg font-semibold text-[var(--ink)]">{{ title }}</h2>
                <p v-if="description" class="text-sm text-[var(--muted)] mt-1">{{ description }}</p>
              </slot>
            </div>

            <!-- Body -->
            <div class="px-6 pb-4">
              <slot />
            </div>

            <!-- Footer -->
            <div v-if="$slots.footer" class="px-6 pb-6 pt-2">
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
