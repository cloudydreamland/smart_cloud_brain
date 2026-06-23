<template>
  <div class="relative inline-flex" @mouseenter="show = true" @mouseleave="show = false">
    <!-- Trigger -->
    <slot />

    <!-- Tooltip -->
    <Transition
      enter-active-class="transition-opacity duration-150"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-100"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="show && content"
        :class="[
          'absolute z-50 max-w-[280px] px-3 py-1.5 rounded-lg border border-[var(--line)]',
          'bg-white text-[var(--ink)] text-[13px] whitespace-nowrap shadow-md',
          positionClasses,
        ]"
      >
        {{ content }}
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  content?: string
  placement?: 'top' | 'bottom' | 'left' | 'right'
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  placement: 'top',
})

const show = ref(false)

const positionClasses = computed(() => {
  const map: Record<string, string> = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 -translate-y-1/2 ml-2',
  }
  return map[props.placement]
})
</script>
