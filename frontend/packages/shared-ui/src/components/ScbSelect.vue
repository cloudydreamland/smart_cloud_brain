<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

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
    /** 滚动时关闭下拉面板，适用于 Modal 内避免定位漂移 */
    closeOnScroll?: boolean;
  }>(),
  { label: "", placeholder: "请选择", disabled: false, closeOnScroll: true }
);

const emit = defineEmits<{ "update:modelValue": [value: string | number | undefined] }>();

const isOpen = ref(false);
const triggerRef = ref<HTMLButtonElement | null>(null);
const menuRef = ref<HTMLDivElement | null>(null);
const menuStyle = ref<Record<string, string>>({});

/* ---- 祖先滚动监听（修复 Modal 内 overflow:auto 导致下拉面板定位漂移） ---- */
let scrollAncestor: HTMLElement | null = null;

function findScrollAncestor(el: HTMLElement): HTMLElement | null {
  let parent = el.parentElement;
  while (parent && parent !== document.body) {
    const s = getComputedStyle(parent);
    if ((s.overflowY === "auto" || s.overflowY === "scroll") && parent.scrollHeight > parent.clientHeight) {
      return parent;
    }
    parent = parent.parentElement;
  }
  return null;
}

function addAncestorScrollListener() {
  removeAncestorScrollListener();
  if (!triggerRef.value) return;
  scrollAncestor = findScrollAncestor(triggerRef.value);
  if (scrollAncestor) {
    scrollAncestor.addEventListener("scroll", onReposition, { passive: true });
  }
}

function removeAncestorScrollListener() {
  if (scrollAncestor) {
    scrollAncestor.removeEventListener("scroll", onReposition);
    scrollAncestor = null;
  }
}

const selectedLabel = computed(() => {
  const found = props.options.find((o) => o.value === props.modelValue);
  return found?.label ?? props.placeholder;
});

const hasLabel = computed(() => Boolean(props.label));

function positionMenu() {
  const el = triggerRef.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const MENU_MAX_H = 260;
  const spaceBelow = window.innerHeight - rect.bottom;
  const openAbove = spaceBelow < MENU_MAX_H + 8 && rect.top > spaceBelow;
  menuStyle.value = openAbove
    ? { position: "fixed", left: `${rect.left}px`, top: `${rect.top - 6}px`, transform: "translateY(-100%)", width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px` }
    : { position: "fixed", left: `${rect.left}px`, top: `${rect.bottom + 6}px`, width: `${rect.width}px`, maxHeight: `${MENU_MAX_H}px` };
}

function toggle() {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    nextTick(() => { positionMenu(); addAncestorScrollListener(); });
  } else {
    removeAncestorScrollListener();
  }
}

function select(value: string | number | undefined) {
  emit("update:modelValue", value);
  isOpen.value = false;
  removeAncestorScrollListener();
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape" && isOpen.value) {
    isOpen.value = false;
    removeAncestorScrollListener();
    triggerRef.value?.focus();
  }
}

function onClickOutside(e: MouseEvent) {
  if (!isOpen.value) return;
  const target = e.target as Node;
  if (triggerRef.value?.contains(target) || menuRef.value?.contains(target)) return;
  isOpen.value = false;
  removeAncestorScrollListener();
}

function onReposition(e: Event) {
  if (!isOpen.value) return;
  if (props.closeOnScroll) {
    // 面板内滚动不关闭（用户在选选项），只关闭 Modal/页面滚动
    const target = e.target as Node;
    if (menuRef.value?.contains(target)) return;
    isOpen.value = false;
    removeAncestorScrollListener();
  } else {
    positionMenu();
  }
}

onMounted(() => {
  document.addEventListener("mousedown", onClickOutside);
  document.addEventListener("keydown", onKeydown);
  window.addEventListener("scroll", onReposition, true);
  window.addEventListener("resize", onReposition);
});

onBeforeUnmount(() => {
  removeAncestorScrollListener();
  document.removeEventListener("mousedown", onClickOutside);
  document.removeEventListener("keydown", onKeydown);
  window.removeEventListener("scroll", onReposition, true);
  window.removeEventListener("resize", onReposition);
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
    <Teleport to="body">
      <div
        v-if="isOpen"
        ref="menuRef"
        class="scb-select-menu"
        role="listbox"
        :style="menuStyle"
      >
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
    </Teleport>
  </div>
</template>

<style>
/* ===== ScbSelect — 自定义下拉框，Teleport 到 body 避免 overflow/z-index 问题 ===== */
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
  z-index: 9999;
  overflow-y: auto;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
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
