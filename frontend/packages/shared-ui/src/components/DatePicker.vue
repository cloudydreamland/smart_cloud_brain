<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    label?: string;
    placeholder?: string;
    disabled?: boolean;
    min?: string;
    max?: string;
    clearable?: boolean;
    ariaLabel?: string;
  }>(),
  {
    modelValue: "",
    label: "",
    placeholder: "请选择日期",
    disabled: false,
    min: "",
    max: "",
    clearable: true,
    ariaLabel: "",
  }
);

const emit = defineEmits<{ "update:modelValue": [value: string] }>();

const triggerRef = ref<HTMLButtonElement | null>(null);
const popoverRef = ref<HTMLDivElement | null>(null);
const open = ref(false);
const popoverStyle = ref<Record<string, string>>({});
const today = new Date();
const viewYear = ref(today.getFullYear());
const viewMonth = ref(today.getMonth() + 1);
const weekDays = ["日", "一", "二", "三", "四", "五", "六"];

function isoDate(year: number, month: number, day: number) {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function parseIsoDate(value?: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "");
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null;
  return { year, month, day };
}

const displayValue = computed(() => {
  const parsed = parseIsoDate(props.modelValue);
  return parsed ? isoDate(parsed.year, parsed.month, parsed.day) : "";
});

function isDisabledDate(value: string) {
  return Boolean((props.min && value < props.min) || (props.max && value > props.max));
}

const calendarCells = computed(() => {
  const firstDay = new Date(viewYear.value, viewMonth.value - 1, 1).getDay();
  const daysInMonth = new Date(viewYear.value, viewMonth.value, 0).getDate();
  const todayValue = isoDate(today.getFullYear(), today.getMonth() + 1, today.getDate());
  const cells: Array<{ day: number; value: string; disabled: boolean; selected: boolean; isToday: boolean }> = [];

  for (let index = 0; index < firstDay; index += 1) {
    cells.push({ day: 0, value: "", disabled: true, selected: false, isToday: false });
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const value = isoDate(viewYear.value, viewMonth.value, day);
    cells.push({
      day,
      value,
      disabled: isDisabledDate(value),
      selected: value === props.modelValue,
      isToday: value === todayValue,
    });
  }
  return cells;
});

const todayValue = computed(() => isoDate(today.getFullYear(), today.getMonth() + 1, today.getDate()));
const todayDisabled = computed(() => isDisabledDate(todayValue.value));

function syncView() {
  const parsed = parseIsoDate(props.modelValue);
  const target = parsed || { year: today.getFullYear(), month: today.getMonth() + 1 };
  viewYear.value = target.year;
  viewMonth.value = target.month;
}

function positionPopover() {
  const trigger = triggerRef.value;
  if (!trigger) return;
  const rect = trigger.getBoundingClientRect();
  const width = Math.min(292, window.innerWidth - 24);
  const left = Math.min(Math.max(12, rect.left), Math.max(12, window.innerWidth - width - 12));
  const estimatedHeight = 350;
  const openAbove = window.innerHeight - rect.bottom < estimatedHeight && rect.top > estimatedHeight;
  popoverStyle.value = {
    position: "fixed",
    width: `${width}px`,
    left: `${left}px`,
    top: openAbove ? `${Math.max(12, rect.top - 8)}px` : `${Math.min(window.innerHeight - 12, rect.bottom + 8)}px`,
    transform: openAbove ? "translateY(-100%)" : "none",
    transformOrigin: openAbove ? "bottom left" : "top left",
  };
}

function focusInitialDay() {
  const target = popoverRef.value?.querySelector<HTMLButtonElement>("[data-selected='true']:not(:disabled)")
    || popoverRef.value?.querySelector<HTMLButtonElement>("[data-today='true']:not(:disabled)")
    || popoverRef.value?.querySelector<HTMLButtonElement>(".scb-date-picker-day:not(:disabled)");
  target?.focus();
}

async function setOpen(next: boolean) {
  if (props.disabled) return;
  open.value = next;
  if (next) {
    syncView();
    await nextTick();
    positionPopover();
    focusInitialDay();
  }
}

function toggle() {
  void setOpen(!open.value);
}

function changeMonth(delta: number) {
  const next = new Date(viewYear.value, viewMonth.value - 1 + delta, 1);
  viewYear.value = next.getFullYear();
  viewMonth.value = next.getMonth() + 1;
}

function selectDate(value: string) {
  if (!value || isDisabledDate(value)) return;
  emit("update:modelValue", value);
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function selectToday() {
  if (!todayDisabled.value) selectDate(todayValue.value);
}

function clear() {
  emit("update:modelValue", "");
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function handleDocumentPointer(event: MouseEvent) {
  const target = event.target as Node;
  if (triggerRef.value?.contains(target) || popoverRef.value?.contains(target)) return;
  open.value = false;
}

function handleWindowChange() {
  if (open.value) positionPopover();
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key !== "Escape" || !open.value) return;
  open.value = false;
  triggerRef.value?.focus();
}

watch(() => props.modelValue, () => {
  if (open.value) syncView();
});

watch(() => props.disabled, (disabled) => {
  if (disabled) open.value = false;
});

onMounted(() => {
  document.addEventListener("mousedown", handleDocumentPointer);
  document.addEventListener("keydown", handleGlobalKeydown);
  window.addEventListener("resize", handleWindowChange);
  window.addEventListener("scroll", handleWindowChange, true);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleDocumentPointer);
  document.removeEventListener("keydown", handleGlobalKeydown);
  window.removeEventListener("resize", handleWindowChange);
  window.removeEventListener("scroll", handleWindowChange, true);
});
</script>

<template>
  <div class="scb-date-picker">
    <label v-if="label" class="scb-date-picker-label">{{ label }}</label>
    <button
      ref="triggerRef"
      type="button"
      class="scb-date-picker-trigger"
      :class="{ open }"
      :disabled="disabled"
      :aria-expanded="open"
      :aria-label="ariaLabel || label || placeholder"
      aria-haspopup="dialog"
      @click="toggle"
      @keydown.down.prevent="setOpen(true)"
    >
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect width="18" height="18" x="3" y="4" rx="2" />
        <path d="M16 2v4M8 2v4M3 10h18" />
      </svg>
      <span :class="{ placeholder: !displayValue }">{{ displayValue || placeholder }}</span>
      <svg class="chevron" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>

    <Teleport to="body">
      <Transition name="scb-date-popover">
        <div
          v-if="open"
          ref="popoverRef"
          class="scb-date-picker-popover"
          :style="popoverStyle"
          role="dialog"
          :aria-label="`${viewYear}年${viewMonth}月日期选择`"
        >
          <header class="scb-date-picker-header">
            <button type="button" aria-label="上个月" @click="changeMonth(-1)">
              <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6" /></svg>
            </button>
            <strong aria-live="polite">{{ viewYear }}年{{ viewMonth }}月</strong>
            <button type="button" aria-label="下个月" @click="changeMonth(1)">
              <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </header>
          <div class="scb-date-picker-weekdays" aria-hidden="true">
            <span v-for="day in weekDays" :key="day">{{ day }}</span>
          </div>
          <div class="scb-date-picker-grid" role="grid">
            <template v-for="(cell, index) in calendarCells" :key="`${cell.value}-${index}`">
              <span v-if="!cell.day" class="scb-date-picker-day empty" />
              <button
                v-else
                type="button"
                class="scb-date-picker-day"
                :disabled="cell.disabled"
                :data-selected="cell.selected || undefined"
                :data-today="cell.isToday || undefined"
                :aria-label="cell.value"
                :aria-selected="cell.selected"
                @click="selectDate(cell.value)"
              >
                {{ cell.day }}
              </button>
            </template>
          </div>
          <footer class="scb-date-picker-footer">
            <button type="button" :disabled="todayDisabled" @click="selectToday">今天</button>
            <button v-if="clearable && modelValue" type="button" class="clear" @click="clear">清除</button>
          </footer>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.scb-date-picker {
  position: relative;
  width: 100%;
  min-width: 0;
}

.scb-date-picker-label {
  display: block;
  margin-bottom: 6px;
  color: var(--ink);
  font-size: 13px;
  font-weight: 650;
}

.scb-date-picker-trigger {
  width: 100%;
  height: 38px;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) 15px;
  align-items: center;
  gap: 9px;
  padding: 0 11px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  color: var(--ink);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 150ms ease, box-shadow 150ms ease, background 150ms ease;
}

.scb-date-picker-trigger:hover:not(:disabled),
.scb-date-picker-trigger.open,
.scb-date-picker-trigger:focus-visible {
  border-color: var(--primary);
  box-shadow: var(--focus);
  outline: none;
}

.scb-date-picker-trigger:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.scb-date-picker-trigger svg {
  width: 17px;
  height: 17px;
  color: var(--muted);
}

.scb-date-picker-trigger span {
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scb-date-picker-trigger span.placeholder {
  color: var(--subtle);
  font-weight: 600;
}

.scb-date-picker-trigger .chevron {
  width: 15px;
  height: 15px;
  transition: transform 150ms ease;
}

.scb-date-picker-trigger.open .chevron {
  transform: rotate(180deg);
}

.scb-date-picker-popover {
  z-index: 10020;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 18px 48px rgba(15, 35, 33, 0.18);
}

.scb-date-picker-header {
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  margin-bottom: 10px;
}

.scb-date-picker-header strong {
  color: var(--ink);
  font-size: 14px;
  text-align: center;
}

.scb-date-picker-header button,
.scb-date-picker-footer button {
  border: 0;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.scb-date-picker-header button {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: 9px;
}

.scb-date-picker-header button:hover,
.scb-date-picker-header button:focus-visible {
  background: var(--surface-alt);
  color: var(--primary);
  outline: none;
}

.scb-date-picker-header svg {
  width: 16px;
  height: 16px;
}

.scb-date-picker-weekdays,
.scb-date-picker-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 2px;
}

.scb-date-picker-weekdays span {
  padding: 4px 0 6px;
  color: var(--subtle);
  font-size: 11px;
  font-weight: 700;
  text-align: center;
}

.scb-date-picker-day {
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--ink);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: background 120ms ease, color 120ms ease, transform 120ms ease;
}

.scb-date-picker-day:hover:not(:disabled),
.scb-date-picker-day:focus-visible {
  background: var(--primary-soft);
  color: var(--primary);
  outline: none;
}

.scb-date-picker-day:active:not(:disabled) {
  transform: scale(0.92);
}

.scb-date-picker-day[data-today="true"] {
  box-shadow: inset 0 0 0 1px var(--primary);
  color: var(--primary);
  font-weight: 800;
}

.scb-date-picker-day[data-selected="true"] {
  background: var(--primary);
  box-shadow: none;
  color: var(--color-white, #fff);
  font-weight: 800;
}

.scb-date-picker-day:disabled {
  color: color-mix(in srgb, var(--subtle) 42%, transparent);
  cursor: not-allowed;
}

.scb-date-picker-footer {
  min-height: 34px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 9px;
  padding-top: 9px;
  border-top: 1px solid var(--line);
}

.scb-date-picker-footer button {
  padding: 4px 2px;
  color: var(--primary);
  font-size: 12px;
  font-weight: 750;
}

.scb-date-picker-footer button.clear {
  margin-left: auto;
  color: var(--muted);
}

.scb-date-picker-footer button:hover:not(:disabled),
.scb-date-picker-footer button:focus-visible {
  text-decoration: underline;
  outline: none;
}

.scb-date-picker-footer button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.scb-date-popover-enter-active,
.scb-date-popover-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.scb-date-popover-enter-from,
.scb-date-popover-leave-to {
  opacity: 0;
  scale: 0.97;
}

@media (prefers-reduced-motion: reduce) {
  .scb-date-picker-trigger,
  .scb-date-picker-day,
  .scb-date-popover-enter-active,
  .scb-date-popover-leave-active {
    transition: none;
  }
}
</style>
