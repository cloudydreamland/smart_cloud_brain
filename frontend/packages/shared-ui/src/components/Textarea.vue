<template>
  <div class="flex flex-col gap-1.5">
    <label
      v-if="label"
      :for="textareaId"
      class="text-sm font-medium text-[var(--ink)]"
    >
      {{ label }}
      <span v-if="required" class="text-[var(--danger)] ml-0.5">*</span>
    </label>
    <textarea
      :id="textareaId"
      ref="textareaRef"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :rows="rows"
      :class="[
        'flex w-full rounded-md border bg-white px-3 py-2 text-sm text-[var(--ink)]',
        'placeholder:text-[var(--subtle)]',
        'min-h-[80px] resize-y leading-1.5',
        'transition-colors',
        'focus-visible:outline-none focus-visible:border-[var(--primary)] focus-visible:ring-2 focus-visible:ring-[var(--focus)]',
        'disabled:cursor-not-allowed disabled:opacity-50',
        error
          ? 'border-[var(--danger)] focus-visible:ring-[var(--danger)]'
          : 'border-[var(--line)]',
        $attrs.class,
      ]"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      @blur="$emit('blur', $event)"
      v-bind="{ ...$attrs, class: undefined }"
    />
    <div class="flex items-center justify-between">
      <p v-if="error" class="text-xs text-[var(--danger)]">{{ error }}</p>
      <p v-else-if="hint" class="text-xs text-[var(--subtle)]">{{ hint }}</p>
      <p v-if="maxlength" class="text-xs text-[var(--subtle)] ml-auto">
        {{ (modelValue ?? '').length }}/{{ maxlength }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  modelValue?: string
  label?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  rows?: number
  maxlength?: number
  error?: string
  hint?: string
}

withDefaults(defineProps<Props>(), {
  modelValue: '',
  label: '',
  placeholder: '',
  disabled: false,
  readonly: false,
  required: false,
  rows: 4,
  maxlength: 0,
  error: '',
  hint: '',
})

defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
}>()

const textareaRef = ref<HTMLTextAreaElement>()
const textareaId = ref(`textarea-${Math.random().toString(36).slice(2, 8)}`)

defineExpose({ focus: () => textareaRef.value?.focus() })
</script>
