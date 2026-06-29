<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    minAge?: number;
    maxAge?: number;
    lowerLimit?: number;
    upperLimit?: number;
  }>(),
  {
    minAge: undefined,
    maxAge: undefined,
    lowerLimit: 0,
    upperLimit: 120,
  }
);

const emit = defineEmits<{
  "update:minAge": [value: number | undefined];
  "update:maxAge": [value: number | undefined];
}>();

const triggerRef = ref<HTMLButtonElement | null>(null);
const popoverRef = ref<HTMLDivElement | null>(null);
const open = ref(false);
const draftMin = ref<number | undefined>();
const draftMax = ref<number | undefined>();
const popoverStyle = ref<Record<string, string>>({});

const rangeMin = computed(() => draftMin.value ?? props.lowerLimit);
const rangeMax = computed(() => draftMax.value ?? props.upperLimit);
const rangeSpan = computed(() => Math.max(1, props.upperLimit - props.lowerLimit));
const trackStyle = computed(() => ({
  left: `${((rangeMin.value - props.lowerLimit) / rangeSpan.value) * 100}%`,
  right: `${100 - ((rangeMax.value - props.lowerLimit) / rangeSpan.value) * 100}%`,
}));

const displayValue = computed(() => {
  if (props.minAge !== undefined && props.maxAge !== undefined) return `${props.minAge}–${props.maxAge} 岁`;
  if (props.minAge !== undefined) return `${props.minAge} 岁及以上`;
  if (props.maxAge !== undefined) return `${props.maxAge} 岁及以下`;
  return "全部年龄";
});

const hasValue = computed(() => props.minAge !== undefined || props.maxAge !== undefined);

function clamp(value: number) {
  return Math.min(props.upperLimit, Math.max(props.lowerLimit, Math.round(value)));
}

function positionPopover() {
  const trigger = triggerRef.value;
  if (!trigger) return;
  const rect = trigger.getBoundingClientRect();
  const width = Math.min(320, window.innerWidth - 24);
  const left = Math.min(Math.max(12, rect.left), Math.max(12, window.innerWidth - width - 12));
  const estimatedHeight = 255;
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
  open.value = next;
  if (!next) return;
  draftMin.value = props.minAge;
  draftMax.value = props.maxAge;
  await nextTick();
  positionPopover();
}

function updateMin(value: number | undefined) {
  if (value === undefined) {
    draftMin.value = undefined;
    return;
  }
  draftMin.value = Math.min(clamp(value), rangeMax.value);
}

function updateMax(value: number | undefined) {
  if (value === undefined) {
    draftMax.value = undefined;
    return;
  }
  draftMax.value = Math.max(clamp(value), rangeMin.value);
}

function numberFromInput(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  return value === "" ? undefined : Number(value);
}

function clear() {
  draftMin.value = undefined;
  draftMax.value = undefined;
  emit("update:minAge", undefined);
  emit("update:maxAge", undefined);
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function apply() {
  emit("update:minAge", draftMin.value);
  emit("update:maxAge", draftMax.value);
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

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== "Escape" || !open.value) return;
  open.value = false;
  triggerRef.value?.focus();
}

watch(() => [props.minAge, props.maxAge], () => {
  if (open.value) {
    draftMin.value = props.minAge;
    draftMax.value = props.maxAge;
  }
});

onMounted(() => {
  document.addEventListener("mousedown", handleDocumentPointer);
  document.addEventListener("keydown", handleKeydown);
  window.addEventListener("resize", handleWindowChange);
  window.addEventListener("scroll", handleWindowChange, true);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleDocumentPointer);
  document.removeEventListener("keydown", handleKeydown);
  window.removeEventListener("resize", handleWindowChange);
  window.removeEventListener("scroll", handleWindowChange, true);
});
</script>

<template>
  <div class="age-range-filter">
    <button
      ref="triggerRef"
      type="button"
      class="age-range-trigger"
      :class="{ open, active: hasValue }"
      :aria-expanded="open"
      aria-haspopup="dialog"
      aria-label="年龄范围"
      @click="setOpen(!open)"
      @keydown.down.prevent="setOpen(true)"
    >
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M4 7h16M7 4v6M17 4v6M6 13h12M8 17h8" />
      </svg>
      <span>{{ displayValue }}</span>
      <svg class="chevron" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>

    <Teleport to="body">
      <Transition name="age-range-popover">
        <div
          v-if="open"
          ref="popoverRef"
          class="age-range-popover"
          :style="popoverStyle"
          role="dialog"
          aria-label="设置年龄范围"
        >
          <header>
            <div>
              <strong>年龄范围</strong>
              <span>{{ rangeMin }}–{{ rangeMax }} 岁</span>
            </div>
            <button v-if="hasValue" type="button" @click="clear">清除</button>
          </header>

          <div class="age-range-slider">
            <div class="age-range-track"><span :style="trackStyle" /></div>
            <input
              :value="rangeMin"
              type="range"
              :min="lowerLimit"
              :max="upperLimit"
              aria-label="最小年龄滑杆"
              @input="updateMin(Number(($event.target as HTMLInputElement).value))"
            />
            <input
              :value="rangeMax"
              type="range"
              :min="lowerLimit"
              :max="upperLimit"
              aria-label="最大年龄滑杆"
              @input="updateMax(Number(($event.target as HTMLInputElement).value))"
            />
          </div>

          <div class="age-range-fields">
            <label>
              <span>最小年龄</span>
              <div><input :value="draftMin ?? ''" type="number" :min="lowerLimit" :max="upperLimit" placeholder="不限" @input="updateMin(numberFromInput($event))" /><em>岁</em></div>
            </label>
            <span class="separator">—</span>
            <label>
              <span>最大年龄</span>
              <div><input :value="draftMax ?? ''" type="number" :min="lowerLimit" :max="upperLimit" placeholder="不限" @input="updateMax(numberFromInput($event))" /><em>岁</em></div>
            </label>
          </div>

          <footer>
            <span>可筛选 {{ lowerLimit }}–{{ upperLimit }} 岁</span>
            <button type="button" @click="apply">应用</button>
          </footer>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.age-range-filter {
  width: 240px;
  min-width: 210px;
}

.age-range-trigger {
  width: 100%;
  height: 38px;
  display: grid;
  grid-template-columns: 17px minmax(0, 1fr) 15px;
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

.age-range-trigger:hover,
.age-range-trigger.open,
.age-range-trigger:focus-visible {
  border-color: var(--primary);
  box-shadow: var(--focus);
  outline: none;
}

.age-range-trigger.active {
  background: color-mix(in srgb, var(--primary-soft) 42%, var(--surface));
}

.age-range-trigger svg {
  width: 17px;
  height: 17px;
  color: var(--muted);
}

.age-range-trigger span {
  overflow: hidden;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.age-range-trigger .chevron {
  width: 15px;
  height: 15px;
  transition: transform 150ms ease;
}

.age-range-trigger.open .chevron {
  transform: rotate(180deg);
}

.age-range-popover {
  z-index: 10020;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 18px 48px rgba(15, 35, 33, 0.18);
}

.age-range-popover header,
.age-range-popover footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.age-range-popover header > div {
  display: grid;
  gap: 2px;
}

.age-range-popover header strong {
  color: var(--ink);
  font-size: 13px;
}

.age-range-popover header span,
.age-range-popover footer > span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 650;
}

.age-range-popover header button {
  padding: 4px;
  border: 0;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
}

.age-range-popover header button:hover,
.age-range-popover header button:focus-visible {
  color: var(--danger);
  outline: none;
}

.age-range-slider {
  position: relative;
  height: 36px;
  margin: 14px 4px 5px;
}

.age-range-track {
  position: absolute;
  top: 16px;
  right: 0;
  left: 0;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--line);
}

.age-range-track span {
  position: absolute;
  top: 0;
  bottom: 0;
  border-radius: inherit;
  background: var(--primary);
}

.age-range-slider input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 36px;
  margin: 0;
  padding: 0;
  appearance: none;
  background: transparent;
  pointer-events: none;
}

.age-range-slider input::-webkit-slider-thumb {
  width: 18px;
  height: 18px;
  appearance: none;
  border: 3px solid var(--surface);
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 0 1px var(--primary), 0 2px 7px rgba(0, 80, 80, 0.22);
  cursor: grab;
  pointer-events: auto;
}

.age-range-slider input::-moz-range-thumb {
  width: 13px;
  height: 13px;
  border: 3px solid var(--surface);
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 0 1px var(--primary), 0 2px 7px rgba(0, 80, 80, 0.22);
  cursor: grab;
  pointer-events: auto;
}

.age-range-slider input:focus-visible {
  outline: none;
}

.age-range-slider input:focus-visible::-webkit-slider-thumb {
  box-shadow: var(--focus), 0 0 0 1px var(--primary);
}

.age-range-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
  align-items: end;
  gap: 7px;
}

.age-range-fields label > span {
  display: block;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 750;
}

.age-range-fields label > div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  height: 36px;
  padding-right: 9px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--surface);
}

.age-range-fields label > div:focus-within {
  border-color: var(--primary);
  box-shadow: var(--focus);
}

.age-range-fields input {
  min-width: 0;
  height: 34px;
  padding: 0 4px 0 9px;
  border: 0;
  background: transparent;
  color: var(--ink);
  font: inherit;
  font-size: 12px;
  font-weight: 750;
  outline: none;
}

.age-range-fields input::-webkit-inner-spin-button {
  appearance: none;
}

.age-range-fields em {
  color: var(--subtle);
  font-size: 10px;
  font-style: normal;
}

.age-range-fields .separator {
  align-self: center;
  margin-top: 16px;
  color: var(--subtle);
  text-align: center;
}

.age-range-popover footer {
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.age-range-popover footer button {
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

.age-range-popover footer button:hover,
.age-range-popover footer button:focus-visible {
  background: var(--primary-strong);
  outline: none;
}

.age-range-popover-enter-active,
.age-range-popover-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.age-range-popover-enter-from,
.age-range-popover-leave-to {
  opacity: 0;
  scale: 0.97;
}

@media (max-width: 980px) {
  .age-range-filter {
    width: 100%;
    max-width: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .age-range-trigger,
  .age-range-trigger .chevron,
  .age-range-popover-enter-active,
  .age-range-popover-leave-active {
    transition: none;
  }
}
</style>
