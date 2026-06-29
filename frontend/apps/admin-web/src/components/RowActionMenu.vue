<script setup lang="ts">
import { IconDots } from "@tabler/icons-vue";
import { nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from "vue";

export interface RowActionMenuItem {
  key: string;
  label: string;
  danger?: boolean;
  separatorBefore?: boolean;
  disabled?: boolean;
}

const props = withDefaults(defineProps<{
  items: RowActionMenuItem[];
  ariaLabel: string;
  disabled?: boolean;
}>(), {
  disabled: false,
});

const emit = defineEmits<{
  select: [key: string];
}>();

const menuId = `row-action-menu-${useId()}`;
const open = ref(false);
const triggerRef = ref<HTMLButtonElement | null>(null);
const menuRef = ref<HTMLDivElement | null>(null);
const itemRefs = ref<HTMLButtonElement[]>([]);
const menuStyle = ref<Record<string, string>>({});
const menuWidth = 156;

function setItemRef(element: unknown, index: number) {
  if (element instanceof HTMLButtonElement) itemRefs.value[index] = element;
}

function positionMenu() {
  const trigger = triggerRef.value;
  if (!trigger) return;

  const viewportGap = 8;
  const safeMenuWidth = Math.min(menuWidth, window.innerWidth - viewportGap * 2);
  const menuHeight = menuRef.value?.offsetHeight ?? props.items.length * 36 + 12;
  const rect = trigger.getBoundingClientRect();
  const left = Math.min(
    Math.max(viewportGap, rect.right - safeMenuWidth),
    window.innerWidth - safeMenuWidth - viewportGap,
  );
  const hasRoomBelow = window.innerHeight - rect.bottom >= menuHeight + viewportGap;
  const top = hasRoomBelow
    ? rect.bottom + 6
    : Math.max(viewportGap, rect.top - menuHeight - 6);

  menuStyle.value = {
    position: "fixed",
    left: `${left}px`,
    top: `${top}px`,
    width: `${safeMenuWidth}px`,
  };
}

async function showMenu(focus: "none" | "first" | "last" = "none") {
  if (props.disabled || !props.items.length) return;
  open.value = true;
  itemRefs.value = [];
  await nextTick();
  positionMenu();
  await nextTick();

  if (focus === "first") itemRefs.value.find((item) => !item.disabled)?.focus();
  if (focus === "last") [...itemRefs.value].reverse().find((item) => !item.disabled)?.focus();
}

function closeMenu(restoreFocus = false) {
  if (!open.value) return;
  open.value = false;
  if (restoreFocus) nextTick(() => triggerRef.value?.focus());
}

function toggleMenu() {
  if (open.value) closeMenu();
  else void showMenu();
}

function selectItem(item: RowActionMenuItem) {
  if (props.disabled || item.disabled) return;
  closeMenu();
  emit("select", item.key);
}

function focusRelative(currentIndex: number, direction: 1 | -1) {
  if (!itemRefs.value.length) return;
  let nextIndex = currentIndex;

  for (let attempts = 0; attempts < itemRefs.value.length; attempts += 1) {
    nextIndex = (nextIndex + direction + itemRefs.value.length) % itemRefs.value.length;
    if (!itemRefs.value[nextIndex]?.disabled) {
      itemRefs.value[nextIndex]?.focus();
      return;
    }
  }
}

function onTriggerKeydown(event: KeyboardEvent) {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    void showMenu("first");
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    void showMenu("last");
  }
}

function onItemKeydown(event: KeyboardEvent, index: number) {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    focusRelative(index, 1);
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    focusRelative(index, -1);
  } else if (event.key === "Home") {
    event.preventDefault();
    itemRefs.value.find((item) => !item.disabled)?.focus();
  } else if (event.key === "End") {
    event.preventDefault();
    [...itemRefs.value].reverse().find((item) => !item.disabled)?.focus();
  } else if (event.key === "Escape") {
    event.preventDefault();
    closeMenu(true);
  }
}

function onDocumentPointerDown(event: MouseEvent) {
  if (!open.value) return;
  const target = event.target as Node;
  if (triggerRef.value?.contains(target) || menuRef.value?.contains(target)) return;
  closeMenu();
}

function onDocumentFocusIn(event: FocusEvent) {
  if (!open.value) return;
  const target = event.target as Node;
  if (triggerRef.value?.contains(target) || menuRef.value?.contains(target)) return;
  closeMenu();
}

function onDocumentKeydown(event: KeyboardEvent) {
  if (event.key === "Escape" && open.value) {
    event.preventDefault();
    closeMenu(true);
  }
}

function closeWithoutFocus() {
  closeMenu();
}

watch(() => props.disabled, (disabled) => {
  if (disabled) closeMenu();
});

onMounted(() => {
  document.addEventListener("mousedown", onDocumentPointerDown);
  document.addEventListener("focusin", onDocumentFocusIn);
  document.addEventListener("keydown", onDocumentKeydown);
  window.addEventListener("scroll", closeWithoutFocus, true);
  window.addEventListener("resize", closeWithoutFocus);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", onDocumentPointerDown);
  document.removeEventListener("focusin", onDocumentFocusIn);
  document.removeEventListener("keydown", onDocumentKeydown);
  window.removeEventListener("scroll", closeWithoutFocus, true);
  window.removeEventListener("resize", closeWithoutFocus);
});
</script>

<template>
  <button
    ref="triggerRef"
    class="row-action-menu-trigger"
    type="button"
    title="更多操作"
    aria-haspopup="menu"
    :aria-controls="menuId"
    :aria-expanded="open"
    :aria-label="ariaLabel"
    :disabled="disabled"
    @click="toggleMenu"
    @keydown="onTriggerKeydown"
  >
    <IconDots aria-hidden="true" :size="18" :stroke-width="2" />
  </button>

  <Teleport to="body">
    <Transition name="row-action-menu">
      <div
        v-if="open"
        :id="menuId"
        ref="menuRef"
        class="row-action-menu"
        role="menu"
        :aria-label="ariaLabel"
        :style="menuStyle"
      >
        <template v-for="(item, index) in items" :key="item.key">
          <div v-if="item.separatorBefore" class="row-action-menu-separator" role="separator" />
          <button
            :ref="(element) => setItemRef(element, index)"
            type="button"
            role="menuitem"
            :class="{ danger: item.danger }"
            :disabled="disabled || item.disabled"
            @click="selectItem(item)"
            @keydown="onItemKeydown($event, index)"
          >
            {{ item.label }}
          </button>
        </template>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.row-action-menu-trigger {
  width: 32px;
  min-width: 32px;
  height: 32px;
  min-height: 32px;
  padding: 0;
  border-color: transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
}

.row-action-menu-trigger:hover,
.row-action-menu-trigger[aria-expanded="true"] {
  border-color: var(--line);
  background: var(--surface-alt);
  color: var(--ink);
}

.row-action-menu {
  z-index: 90;
  display: grid;
  box-sizing: border-box;
  width: 156px;
  max-width: calc(100vw - 16px);
  overflow: hidden;
  padding: 5px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
}

.row-action-menu button {
  display: flex;
  box-sizing: border-box;
  align-items: center;
  justify-content: flex-start;
  width: 100%;
  min-height: 34px;
  height: 34px;
  padding: 0 10px;
  border-color: transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  text-align: left;
  white-space: nowrap;
}

.row-action-menu button:hover,
.row-action-menu button:focus-visible {
  border-color: transparent;
  background: var(--surface-alt);
}

.row-action-menu button.danger {
  border-color: transparent;
  background: transparent;
  color: var(--danger);
  box-shadow: none;
}

.row-action-menu button.danger:hover,
.row-action-menu button.danger:focus-visible {
  background: var(--danger-soft);
  filter: none;
}

.row-action-menu-separator {
  height: 1px;
  margin: 4px 5px;
  background: var(--line);
}

.row-action-menu-enter-active,
.row-action-menu-leave-active {
  transition: opacity 120ms ease, transform 120ms ease;
  transform-origin: top right;
}

.row-action-menu-enter-from,
.row-action-menu-leave-to {
  opacity: 0;
  transform: translateY(-3px) scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .row-action-menu-enter-active,
  .row-action-menu-leave-active {
    transition: none;
  }
}
</style>
