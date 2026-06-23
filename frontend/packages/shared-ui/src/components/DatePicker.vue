<template>
  <div class="relative" ref="containerRef">
    <!-- Label -->
    <label v-if="label" class="text-sm font-medium text-[var(--ink)] mb-1.5 block">
      {{ label }}
    </label>

    <!-- Input trigger -->
    <div
      class="flex items-center h-10 w-full rounded-md border border-[var(--line)] bg-white px-3 py-2 text-sm cursor-pointer transition-colors hover:border-[var(--line-strong)] focus-within:border-[var(--primary)] focus-within:ring-2 focus-within:ring-[var(--focus)]"
      @click="toggle"
    >
      <svg class="h-4 w-4 mr-2 text-[var(--subtle)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/>
      </svg>
      <span :class="modelValue ? 'text-[var(--ink)]' : 'text-[var(--subtle)]'">
        {{ displayValue || placeholder || '请选择日期' }}
      </span>
    </div>

    <!-- Calendar popover -->
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
        class="absolute z-50 mt-1 w-[280px] rounded-xl border border-[var(--line)] bg-white p-3 shadow-lg"
      >
        <!-- Header -->
        <div class="flex items-center justify-between mb-3">
          <button
            class="flex items-center justify-center h-7 w-7 rounded-md hover:bg-[var(--surface-alt)] transition-colors"
            @click="changeMonth(-1)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6"/></svg>
          </button>
          <span class="text-sm font-semibold">{{ viewYear }}年{{ viewMonth }}月</span>
          <button
            class="flex items-center justify-center h-7 w-7 rounded-md hover:bg-[var(--surface-alt)] transition-colors"
            @click="changeMonth(1)"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
          </button>
        </div>

        <!-- Weekday headers -->
        <div class="grid grid-cols-7 mb-1">
          <div v-for="day in weekDays" :key="day" class="text-center text-xs font-medium text-[var(--subtle)] py-1">
            {{ day }}
          </div>
        </div>

        <!-- Day grid -->
        <div class="grid grid-cols-7 gap-0.5">
          <button
            v-for="(cell, i) in calendarCells"
            :key="i"
            :disabled="cell.disabled"
            :class="[
              'h-8 w-full rounded-md text-sm transition-colors',
              cell.disabled
                ? 'text-[var(--subtle)]/40 cursor-not-allowed'
                : cell.selected
                  ? 'bg-[var(--primary)] text-white font-medium'
                  : cell.isToday
                    ? 'bg-[var(--primary-soft)] text-[var(--primary)] font-medium'
                    : 'text-[var(--ink)] hover:bg-[var(--surface-alt)]',
            ]"
            @click="cell.disabled || selectDate(cell.day)"
          >
            {{ cell.day || '' }}
          </button>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-between mt-3 pt-3 border-t border-[var(--line)]">
          <button
            class="text-xs text-[var(--primary)] hover:underline"
            @click="selectToday"
          >
            今天
          </button>
          <button
            v-if="modelValue"
            class="text-xs text-[var(--muted)] hover:text-[var(--danger)]"
            @click="clear"
          >
            清除
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  modelValue?: string
  label?: string
  placeholder?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  label: '',
  placeholder: '',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const open = ref(false)
const containerRef = ref<HTMLElement>()
const now = new Date()
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1)

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const displayValue = computed(() => {
  if (!props.modelValue) return ''
  const d = new Date(props.modelValue)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const calendarCells = computed(() => {
  const year = viewYear.value
  const month = viewMonth.value
  const firstDay = new Date(year, month - 1, 1).getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${today.getMonth() + 1}-${today.getDate()}`
  const selectedStr = props.modelValue ? (() => {
    const d = new Date(props.modelValue)
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
  })() : ''

  const cells: { day: number; disabled: boolean; selected: boolean; isToday: boolean }[] = []

  // Empty cells before first day
  for (let i = 0; i < firstDay; i++) {
    cells.push({ day: 0, disabled: true, selected: false, isToday: false })
  }

  // Day cells
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${month}-${d}`
    cells.push({
      day: d,
      disabled: false,
      selected: dateStr === selectedStr,
      isToday: dateStr === todayStr,
    })
  }

  return cells
})

function toggle() {
  if (props.disabled) return
  open.value = !open.value
}

function changeMonth(delta: number) {
  viewMonth.value += delta
  if (viewMonth.value > 12) { viewMonth.value = 1; viewYear.value++ }
  if (viewMonth.value < 1) { viewMonth.value = 12; viewYear.value-- }
}

function selectDate(day: number) {
  const m = String(viewMonth.value).padStart(2, '0')
  const d = String(day).padStart(2, '0')
  emit('update:modelValue', `${viewYear.value}-${m}-${d}`)
  open.value = false
}

function selectToday() {
  const today = new Date()
  viewYear.value = today.getFullYear()
  viewMonth.value = today.getMonth() + 1
  selectDate(today.getDate())
}

function clear() {
  emit('update:modelValue', '')
  open.value = false
}

function handleClickOutside(e: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>
