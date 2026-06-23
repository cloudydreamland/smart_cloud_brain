<template>
  <div class="flex flex-col gap-1.5">
    <label
      v-if="label"
      :for="inputId"
      class="text-sm font-medium text-[var(--ink)]"
    >
      {{ label }}
      <span v-if="required" class="text-[var(--danger)] ml-0.5">*</span>
    </label>
    <input
      :id="inputId"
      ref="inputRef"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :class="[
        'flex h-10 w-full rounded-md border bg-white px-3 py-2 text-sm text-[var(--ink)]',
        'placeholder:text-[var(--subtle)]',
        'transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium',
        'focus-visible:outline-none focus-visible:border-[var(--primary)] focus-visible:ring-2 focus-visible:ring-[var(--focus)]',
        'disabled:cursor-not-allowed disabled:opacity-50',
        error
          ? 'border-[var(--danger)] focus-visible:ring-[var(--danger)]'
          : 'border-[var(--line)]',
        $attrs.class,
      ]"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @blur="$emit('blur', $event)"
      v-bind="{ ...$attrs, class: undefined }"
    />
    <p v-if="error" class="text-xs text-[var(--danger)]">{{ error }}</p>
    <p v-else-if="hint" class="text-xs text-[var(--subtle)]">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  modelValue?: string
  type?: string
  label?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  error?: string
  hint?: string
}

withDefaults(defineProps<Props>(), {
  modelValue: '',
  type: 'text',
  label: '',
  placeholder: '',
  disabled: false,
  readonly: false,
  required: false,
  error: '',
  hint: '',
})

defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
}>()

const inputRef = ref<HTMLInputElement>()
const inputId = ref(`input-${Math.random().toString(36).slice(2, 8)}`)

defineExpose({ focus: () => inputRef.value?.focus() })
</script>
