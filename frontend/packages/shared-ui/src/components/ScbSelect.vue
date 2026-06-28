<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

export interface ScbSelectOption {
  value: string | number;
  label: string;
}

const props = withDefaults(
  defineProps<{
    modelValue: string | number | undefined;
    options: ScbSelectOption[];
    label?: string;
    placeholder?: string;
    disabled?: boolean;
  }>(),
  { label: "", placeholder: "请选择", disabled: false }
);

const emit = defineEmits<{ "update:modelValue": [value: string | number | undefined] }>();

const isOpen = ref(false);
const triggerRef = ref<HTMLButtonElement | null>(null);
const menuRef = ref<HTMLDivElement | null>(null);

const selectedLabel = computed(() => {
  const found = props.options.find((o) => o.value === props.modelValue);
  return found?.label ?? props.placeholder;
});

const hasLabel = computed(() => Boolean(props.label));

function toggle() {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;
}

function select(value: string | number | undefined) {
  emit("update:modelValue", value);
  isOpen.value = false;
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape" && isOpen.value) {
    isOpen.value = false;
    triggerRef.value?.focus();
  }
}

function onClickOutside(e: MouseEvent) {
  if (
    !triggerRef.value?.contains(e.target as Node) &&
    !menuRef.value?.contains(e.target as Node)
  ) {
    isOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener("mousedown", onClickOutside);
  document.addEventListener("keydown", onKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", onClickOutside);
  document.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <div class="scb-select" :class="{ open: isOpen, disabled }">
    <button
      ref="triggerRef"
      class="scb-select-trigger"
      type="button"
      :aria-expanded="isOpen"
      :aria-disabled="disabled"
      @click="toggle"
    >
      <span v-if="hasLabel" class="scb-select-label">{{ label }}</span>
      <strong class="scb-select-value">{{ selectedLabel }}</strong>
      <svg
        aria-hidden="true"
        width="15"
        height="15"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>
    <div v-if="isOpen" ref="menuRef" class="scb-select-menu" role="listbox">
      <button
        v-for="option in options"
        :key="String(option.value)"
        type="button"
        role="option"
        :aria-selected="modelValue === option.value"
        :class="{ selected: modelValue === option.value }"
        @click="select(option.value)"
      >
        <span>{{ option.label }}</span>
        <svg
          v-if="modelValue === option.value"
          aria-hidden="true"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m20 6-11 11-5-5" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style>
/* ===== ScbSelect — 自定义下拉框，复用通知中心风格 ===== */
.scb-select {
  position: relative;
  min-width: 160px;
}

.scb-select-trigger {
  width: 100%;
  height: 38px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--color-white, #fff);
  color: var(--ink);
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.scb-select-trigger:hover,
.scb-select.open .scb-select-trigger {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(11, 95, 120, 0.1);
}

.scb-select-label {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.scb-select-value {
  min-width: 0;
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scb-select-trigger svg {
  color: var(--muted);
  flex-shrink: 0;
}

.scb-select-menu {
  position: absolute;
  z-index: 30;
  top: calc(100% + 6px);
  right: 0;
  left: 0;
  min-width: 100%;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-lg, var(--shadow));
  padding: 5px;
}

.scb-select-menu button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 32px;
  padding: 7px 9px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ink);
  font-size: 13px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.scb-select-menu button:hover,
.scb-select-menu button.selected {
  background: var(--primary-soft);
  color: var(--primary);
}

.scb-select.disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
