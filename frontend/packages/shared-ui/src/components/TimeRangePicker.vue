<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

export interface TimeRangePreset {
  label: string;
  value: string;
}

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    stepMinutes?: number;
    presets?: TimeRangePreset[];
    disabled?: boolean;
    placeholder?: string;
  }>(),
  {
    modelValue: "",
    stepMinutes: 30,
    presets: () => [
      { label: "上午门诊", value: "09:00-12:00" },
      { label: "下午门诊", value: "14:00-17:00" },
    ],
    disabled: false,
    placeholder: "请选择时段",
  }
);

const emit = defineEmits<{ "update:modelValue": [value: string] }>();

const triggerRef = ref<HTMLButtonElement | null>(null);
const popoverRef = ref<HTMLDivElement | null>(null);
const open = ref(false);
const popoverStyle = ref<Record<string, string>>({});
const startTime = ref("09:00");
const endTime = ref("12:00");

function toMinutes(value: string) {
  const match = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(value);
  return match ? Number(match[1]) * 60 + Number(match[2]) : -1;
}

function fromMinutes(minutes: number) {
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}

function parseRange(value?: string) {
  const match = /^([0-2]\d:[0-5]\d)-([0-2]\d:[0-5]\d)$/.exec(value || "");
  if (!match || toMinutes(match[1]) < 0 || toMinutes(match[2]) <= toMinutes(match[1])) return null;
  return { start: match[1], end: match[2] };
}

const normalizedStep = computed(() => {
  const step = Math.floor(Number(props.stepMinutes));
  return step > 0 && step < 1440 ? step : 30;
});

const baseTimes = computed(() => {
  const values: string[] = [];
  for (let minutes = 0; minutes < 1440; minutes += normalizedStep.value) values.push(fromMinutes(minutes));
  return values;
});

function withCurrent(values: string[], current: string) {
  return Array.from(new Set([...values, current]))
    .filter((value) => toMinutes(value) >= 0)
    .sort((left, right) => toMinutes(left) - toMinutes(right));
}

const startOptions = computed(() => withCurrent(baseTimes.value.filter((value) => toMinutes(value) < 1439), startTime.value));
const endOptions = computed(() => withCurrent(baseTimes.value, endTime.value).filter((value) => toMinutes(value) > toMinutes(startTime.value)));
const displayValue = computed(() => props.modelValue ? props.modelValue.replace("-", "–") : "");
const selectedPreset = computed(() => props.presets.find((preset) => preset.value === props.modelValue)?.value || "");

function syncFromModel() {
  const parsed = parseRange(props.modelValue);
  startTime.value = parsed?.start || "09:00";
  endTime.value = parsed?.end || "12:00";
}

function positionPopover() {
  const trigger = triggerRef.value;
  if (!trigger) return;
  const rect = trigger.getBoundingClientRect();
  const width = Math.min(320, window.innerWidth - 24);
  const left = Math.min(Math.max(12, rect.left), Math.max(12, window.innerWidth - width - 12));
  const estimatedHeight = 285;
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

async function setOpen(next: boolean) {
  if (props.disabled) return;
  open.value = next;
  if (next) {
    syncFromModel();
    await nextTick();
    positionPopover();
  }
}

function choosePreset(value: string) {
  const parsed = parseRange(value);
  if (!parsed) return;
  emit("update:modelValue", value);
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function emitCustom() {
  if (toMinutes(endTime.value) <= toMinutes(startTime.value)) return;
  emit("update:modelValue", `${startTime.value}-${endTime.value}`);
}

function changeStart() {
  if (toMinutes(endTime.value) <= toMinutes(startTime.value)) {
    const next = Math.min(1439, toMinutes(startTime.value) + normalizedStep.value);
    endTime.value = fromMinutes(next);
  }
  emitCustom();
}

function changeEnd() {
  emitCustom();
}

function done() {
  emitCustom();
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

watch(() => props.modelValue, syncFromModel, { immediate: true });
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
  <div class="scb-time-range-picker">
    <button
      ref="triggerRef"
      type="button"
      class="scb-time-range-trigger"
      :class="{ open }"
      :disabled="disabled"
      :aria-expanded="open"
      aria-haspopup="dialog"
      @click="setOpen(!open)"
      @keydown.down.prevent="setOpen(true)"
    >
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </svg>
      <span :class="{ placeholder: !displayValue }">{{ displayValue || placeholder }}</span>
      <svg class="chevron" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6" /></svg>
    </button>

    <Teleport to="body">
      <Transition name="scb-time-popover">
        <div
          v-if="open"
          ref="popoverRef"
          class="scb-time-range-popover"
          :style="popoverStyle"
          role="dialog"
          aria-label="选择排班时段"
        >
          <div class="scb-time-range-section">
            <span class="scb-time-range-caption">常用时段</span>
            <div class="scb-time-range-presets">
              <button
                v-for="preset in presets"
                :key="preset.value"
                type="button"
                :class="{ selected: selectedPreset === preset.value }"
                @click="choosePreset(preset.value)"
              >
                <span>{{ preset.label }}</span>
                <strong>{{ preset.value.replace("-", "–") }}</strong>
              </button>
            </div>
          </div>
          <div class="scb-time-range-divider"><span>自定义</span></div>
          <div class="scb-time-range-custom">
            <label>
              <span>开始时间</span>
              <select v-model="startTime" aria-label="开始时间" @change="changeStart">
                <option v-for="time in startOptions" :key="time" :value="time">{{ time }}</option>
              </select>
            </label>
            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-5-5 5 5-5 5" /></svg>
            <label>
              <span>结束时间</span>
              <select v-model="endTime" aria-label="结束时间" @change="changeEnd">
                <option v-for="time in endOptions" :key="time" :value="time">{{ time }}</option>
              </select>
            </label>
          </div>
          <div class="scb-time-range-foot">
            <span>按 {{ normalizedStep }} 分钟调整</span>
            <button type="button" @click="done">完成</button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.scb-time-range-picker {
  width: 100%;
  min-width: 0;
}

.scb-time-range-trigger {
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
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.scb-time-range-trigger:hover:not(:disabled),
.scb-time-range-trigger.open,
.scb-time-range-trigger:focus-visible {
  border-color: var(--primary);
  box-shadow: var(--focus);
  outline: none;
}

.scb-time-range-trigger:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.scb-time-range-trigger svg {
  width: 17px;
  height: 17px;
  color: var(--muted);
}

.scb-time-range-trigger span {
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scb-time-range-trigger span.placeholder {
  color: var(--subtle);
  font-weight: 600;
}

.scb-time-range-trigger .chevron {
  width: 15px;
  height: 15px;
  transition: transform 150ms ease;
}

.scb-time-range-trigger.open .chevron {
  transform: rotate(180deg);
}

.scb-time-range-popover {
  z-index: 10020;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 18px 48px rgba(15, 35, 33, 0.18);
}

.scb-time-range-caption,
.scb-time-range-custom label > span {
  display: block;
  margin-bottom: 7px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.scb-time-range-presets {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.scb-time-range-presets button {
  min-width: 0;
  display: grid;
  gap: 3px;
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--surface);
  color: var(--ink);
  text-align: left;
  cursor: pointer;
}

.scb-time-range-presets button:hover,
.scb-time-range-presets button:focus-visible,
.scb-time-range-presets button.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
  outline: none;
}

.scb-time-range-presets span {
  font-size: 11px;
  font-weight: 700;
}

.scb-time-range-presets strong {
  font-size: 12px;
}

.scb-time-range-divider {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 13px 0 10px;
  color: var(--subtle);
  font-size: 10px;
  font-weight: 800;
}

.scb-time-range-divider::before,
.scb-time-range-divider::after {
  height: 1px;
  flex: 1;
  background: var(--line);
  content: "";
}

.scb-time-range-custom {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
  align-items: end;
  gap: 8px;
}

.scb-time-range-custom > svg {
  width: 16px;
  height: 16px;
  margin-bottom: 10px;
  color: var(--subtle);
}

.scb-time-range-custom select {
  width: 100%;
  height: 36px;
  padding: 0 9px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--surface);
  color: var(--ink);
  font: inherit;
  font-size: 12px;
  font-weight: 750;
}

.scb-time-range-custom select:focus {
  border-color: var(--primary);
  box-shadow: var(--focus);
  outline: none;
}

.scb-time-range-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.scb-time-range-foot span {
  color: var(--subtle);
  font-size: 11px;
}

.scb-time-range-foot button {
  min-height: 30px;
  padding: 0 13px;
  border: 0;
  border-radius: 8px;
  background: var(--primary);
  color: var(--color-white, #fff);
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.scb-time-range-foot button:hover,
.scb-time-range-foot button:focus-visible {
  background: var(--primary-strong);
  outline: none;
}

.scb-time-popover-enter-active,
.scb-time-popover-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.scb-time-popover-enter-from,
.scb-time-popover-leave-to {
  opacity: 0;
  scale: 0.97;
}

@media (max-width: 420px) {
  .scb-time-range-presets {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scb-time-range-trigger,
  .scb-time-popover-enter-active,
  .scb-time-popover-leave-active {
    transition: none;
  }
}
</style>
