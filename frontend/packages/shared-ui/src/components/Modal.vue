<template>
  <Teleport to="body">
    <Transition
      enter-active-class="modal-enter-active"
      enter-from-class="modal-enter-from"
      enter-to-class="modal-enter-to"
      leave-active-class="modal-leave-active"
      leave-from-class="modal-leave-from"
      leave-to-class="modal-leave-to"
      appear
    >
      <div
        v-if="open"
        class="modal-backdrop"
      >
        <div class="modal-backdrop-overlay" @click="close" />

        <Transition
          enter-active-class="modal-dialog-enter-active"
          enter-from-class="modal-dialog-enter-from"
          enter-to-class="modal-dialog-enter-to"
          leave-active-class="modal-dialog-leave-active"
          leave-from-class="modal-dialog-leave-from"
          leave-to-class="modal-dialog-leave-to"
          appear
        >
          <div
            v-if="open"
            :class="['modal-dialog', sizeClass]"
            role="dialog"
            :aria-modal="true"
            :aria-labelledby="title ? 'modal-title' : undefined"
          >
            <button class="modal-close-btn" @click="close">
              <svg class="modal-close-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>

            <div v-if="title || $slots.header" class="modal-header">
              <slot name="header">
                <h2 id="modal-title" class="modal-title">{{ title }}</h2>
                <p v-if="description" class="modal-description">{{ description }}</p>
              </slot>
            </div>

            <div class="modal-body">
              <slot />
            </div>

            <div v-if="$slots.footer" class="modal-footer">
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

const sizeClass = computed(() => {
  const map: Record<string, string> = {
    sm: 'modal-dialog-sm',
    md: 'modal-dialog-md',
    lg: 'modal-dialog-lg',
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
