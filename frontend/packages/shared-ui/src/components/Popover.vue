<template>
  <div class="relative inline-flex" ref="containerRef">
    <!-- Trigger -->
    <div @click="toggle" class="cursor-pointer">
      <slot name="trigger" />
    </div>

    <!-- Popover content -->
    <Transition
      enter-active-class="transition-all duration-200 ease-out"
      enter-from-class="opacity-0 scale-95 -translate-y-1"
      enter-to-class="opacity-100 scale-100 translate-y-0"
      leave-active-class="transition-all duration-150 ease-in"
      leave-from-class="opacity-100 scale-100 translate-y-0"
      leave-to-class="opacity-0 scale-95 -translate-y-1"
    >
      <div
        v-if="open"
        :class="[
          'absolute z-50 min-w-[12rem] rounded-xl border border-[var(--line)] bg-white p-4 shadow-lg',
          positionClasses,
        ]"
      >
        <!-- Title -->
        <div v-if="title" class="text-sm font-semibold mb-2">{{ title }}</div>

        <!-- Content -->
        <slot />

        <!-- Arrow -->
        <div
          v-if="arrow"
          :class="[
            'absolute h-2 w-2 rotate-45 bg-white border-[var(--line)]',
            arrowClasses,
          ]"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  title?: string
  placement?: 'top' | 'bottom' | 'left' | 'right'
  arrow?: boolean
  trigger?: 'click' | 'hover'
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  placement: 'bottom',
  arrow: true,
  trigger: 'click',
})

const open = ref(false)
const containerRef = ref<HTMLElement>()

const positionClasses = computed(() => {
  const map: Record<string, string> = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 -translate-y-1/2 ml-2',
  }
  return map[props.placement]
})

const arrowClasses = computed(() => {
  const map: Record<string, string> = {
    top: 'bottom-[-5px] left-1/2 -translate-x-1/2 rotate-[135deg] border-r border-b',
    bottom: 'top-[-5px] left-1/2 -translate-x-1/2 rotate-45 border-l border-t',
    left: 'right-[-5px] top-1/2 -translate-y-1/2 rotate-45 border-r border-b',
    right: 'left-[-5px] top-1/2 -translate-y-1/2 rotate-[135deg] border-l border-t',
  }
  return map[props.placement]
})

function toggle() {
  if (props.trigger === 'click') {
    open.value = !open.value
  }
}

function handleMouseEnter() {
  if (props.trigger === 'hover') open.value = true
}

function handleMouseLeave() {
  if (props.trigger === 'hover') open.value = false
}

function handleClickOutside(e: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscape)
})

defineExpose({ open, close: () => { open.value = false } })
</script>
