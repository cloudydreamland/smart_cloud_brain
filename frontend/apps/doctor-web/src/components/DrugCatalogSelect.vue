<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import type { DrugCatalogOption } from "../types/drugCatalog";

const props = withDefaults(defineProps<{
  modelValue: string;
  drugs: DrugCatalogOption[];
  disabled?: boolean;
  placeholder?: string;
  ariaLabel?: string;
  emptyMessage?: string;
  emptyActionLabel?: string;
  placement?: "auto" | "top" | "bottom";
}>(), {
  placeholder: "搜索药品",
  ariaLabel: "选择药品",
  emptyMessage: "未找到匹配药品。",
  emptyActionLabel: "",
  placement: "auto",
});

const emit = defineEmits<{
  "update:modelValue": [value: string];
  select: [drug: DrugCatalogOption];
  retry: [];
}>();

const controlEl = ref<HTMLDivElement | null>(null);
const inputEl = ref<HTMLInputElement | null>(null);
const popoverEl = ref<HTMLDivElement | null>(null);
const inputValue = ref(props.modelValue);
const searchTerm = ref("");
const searching = ref(false);
const open = ref(false);
const activeIndex = ref(0);
const popoverPlacement = ref<"top" | "bottom">("bottom");
const popoverStyle = ref<Record<string, string>>({});

const filteredDrugs = computed(() => {
  const keyword = searchTerm.value.trim().toLowerCase();
  if (!keyword) return props.drugs;
  return props.drugs.filter((drug) =>
    [drug.name, drug.specification, drug.contraindication, drug.interactionRule]
      .some((value) => String(value ?? "").toLowerCase().includes(keyword)),
  );
});

const activeDrug = computed(() => filteredDrugs.value[activeIndex.value]);

watch(() => props.modelValue, (value) => {
  if (!searching.value) inputValue.value = value;
});

watch(() => props.disabled, (disabled) => {
  if (disabled) closeList();
});

watch(filteredDrugs, (drugs) => {
  if (activeIndex.value >= drugs.length) activeIndex.value = Math.max(0, drugs.length - 1);
  if (open.value) nextTick(positionPopover);
});

function positionPopover() {
  const input = inputEl.value;
  if (!input || !open.value) return;

  const rect = input.getBoundingClientRect();
  const viewportPadding = 12;
  const viewportWidth = window.innerWidth;
  const width = Math.min(
    340,
    Math.max(300, rect.width),
    Math.max(0, viewportWidth - viewportPadding * 2),
  );
  const left = Math.min(
    Math.max(viewportPadding, rect.left),
    Math.max(viewportPadding, viewportWidth - width - viewportPadding),
  );

  const footer = input.closest(".panel")?.querySelector<HTMLElement>(".footer-actions");
  const footerTop = footer?.getBoundingClientRect().top;
  const bottomBoundary = Math.min(
    window.innerHeight - viewportPadding,
    footerTop == null ? window.innerHeight - viewportPadding : footerTop - 8,
  );
  const spaceBelow = Math.max(0, bottomBoundary - rect.bottom - 6);
  const spaceAbove = Math.max(0, rect.top - viewportPadding - 6);
  const openAbove = props.placement === "top"
    || (props.placement === "auto" && spaceBelow < 220 && spaceAbove > spaceBelow);
  const availableHeight = openAbove ? spaceAbove : spaceBelow;
  const maxHeight = Math.max(120, Math.min(320, availableHeight || 320));

  popoverPlacement.value = openAbove ? "top" : "bottom";
  popoverStyle.value = openAbove
    ? {
        position: "fixed",
        left: `${left}px`,
        top: `${rect.top - 6}px`,
        width: `${width}px`,
        maxHeight: `${maxHeight}px`,
        transform: "translateY(-100%)",
        transformOrigin: "bottom left",
      }
    : {
        position: "fixed",
        left: `${left}px`,
        top: `${rect.bottom + 6}px`,
        width: `${width}px`,
        maxHeight: `${maxHeight}px`,
        transformOrigin: "top left",
      };
}

function scrollActiveIntoView() {
  nextTick(() => {
    const option = popoverEl.value
      ?.querySelector<HTMLElement>(`[data-option-index="${activeIndex.value}"]`);
    if (typeof option?.scrollIntoView === "function") {
      option.scrollIntoView({ block: "nearest" });
    }
  });
}

function firstSelectableIndex() {
  const index = filteredDrugs.value.findIndex((drug) => !drug.disabled);
  return index < 0 ? 0 : index;
}

async function openList() {
  if (props.disabled) return;
  inputValue.value = props.modelValue;
  searchTerm.value = "";
  searching.value = false;
  const selectedIndex = filteredDrugs.value.findIndex((drug) => drug.name === props.modelValue && !drug.disabled);
  activeIndex.value = selectedIndex >= 0 ? selectedIndex : firstSelectableIndex();
  open.value = true;
  await nextTick();
  positionPopover();
  if (props.modelValue) inputEl.value?.select();
  scrollActiveIntoView();
}

function focusInput() {
  if (props.disabled) return;
  if (document.activeElement === inputEl.value) {
    void openList();
    return;
  }
  inputEl.value?.focus();
}

function handleInput() {
  searching.value = true;
  searchTerm.value = inputValue.value;
  activeIndex.value = 0;
  if (props.modelValue) emit("update:modelValue", "");
  open.value = true;
  nextTick(() => {
    positionPopover();
    scrollActiveIntoView();
  });
}

function selectDrug(drug: DrugCatalogOption) {
  if (drug.disabled || props.disabled) return;
  inputValue.value = drug.name;
  searchTerm.value = "";
  searching.value = false;
  emit("update:modelValue", drug.name);
  emit("select", drug);
  open.value = false;
}

function move(delta: number) {
  if (props.disabled) return;
  if (!open.value) {
    void openList();
    return;
  }

  const count = filteredDrugs.value.length;
  if (!count) return;
  let next = activeIndex.value;
  for (let index = 0; index < count; index += 1) {
    next = (next + delta + count) % count;
    if (!filteredDrugs.value[next]?.disabled) {
      activeIndex.value = next;
      scrollActiveIntoView();
      return;
    }
  }
}

function chooseActive() {
  if (activeDrug.value && !activeDrug.value.disabled) selectDrug(activeDrug.value);
}

function closeList(restoreValue = true) {
  open.value = false;
  searchTerm.value = "";
  searching.value = false;
  if (restoreValue) inputValue.value = props.modelValue;
}

function handleBlur() {
  window.setTimeout(() => {
    const active = document.activeElement;
    if (active && popoverEl.value?.contains(active)) return;
    closeList();
  }, 0);
}

function clearSelection() {
  inputValue.value = "";
  searchTerm.value = "";
  searching.value = false;
  emit("update:modelValue", "");
  nextTick(() => {
    inputEl.value?.focus();
    void openList();
  });
}

function retry() {
  emit("retry");
  closeList(false);
}

function handleDocumentPointer(event: MouseEvent) {
  if (!open.value) return;
  const target = event.target as Node;
  if (controlEl.value?.contains(target) || popoverEl.value?.contains(target)) return;
  closeList();
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key !== "Escape" || !open.value) return;
  closeList();
  inputEl.value?.focus();
}

function handleWindowChange() {
  if (open.value) positionPopover();
}

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
  <div class="drug-catalog-select" :class="{ open, disabled }">
    <div ref="controlEl" class="drug-select-control" @click="focusInput">
      <input
        ref="inputEl"
        v-model="inputValue"
        type="text"
        :disabled="disabled"
        :placeholder="placeholder"
        :aria-label="ariaLabel"
        role="combobox"
        aria-autocomplete="list"
        :aria-expanded="open"
        autocomplete="off"
        @input="handleInput"
        @focus="openList"
        @blur="handleBlur"
        @keydown.down.prevent="move(1)"
        @keydown.up.prevent="move(-1)"
        @keydown.enter.prevent="chooseActive"
        @keydown.esc.prevent="closeList()"
      />
      <button
        v-if="modelValue"
        type="button"
        class="drug-clear-btn"
        :disabled="disabled"
        aria-label="清空已选药品"
        @mousedown.prevent
        @click.stop="clearSelection"
      >
        ×
      </button>
    </div>

    <Teleport to="body">
      <Transition name="drug-options">
        <div
          v-if="open"
          ref="popoverEl"
          class="drug-options-popover"
          :class="`placement-${popoverPlacement}`"
          :style="popoverStyle"
          role="listbox"
        >
          <button
            v-for="(drug, index) in filteredDrugs"
            :key="drug.id ?? drug.name"
            type="button"
            class="drug-option"
            :class="{ active: index === activeIndex, selected: drug.name === modelValue, disabled: drug.disabled }"
            :disabled="drug.disabled"
            :aria-selected="drug.name === modelValue"
            :data-option-index="index"
            role="option"
            @mousedown.prevent
            @click="selectDrug(drug)"
          >
            <span class="drug-option-main">
              <strong>{{ drug.name }}</strong>
              <small>{{ drug.specification || "未填写规格" }}</small>
            </span>
            <span v-if="drug.contraindication || drug.interactionRule" class="drug-option-warning">
              {{ drug.contraindication || drug.interactionRule }}
            </span>
          </button>
          <div v-if="!filteredDrugs.length" class="drug-option-empty">
            <span>{{ emptyMessage }}</span>
            <button
              v-if="emptyActionLabel"
              type="button"
              class="drug-option-retry"
              @mousedown.prevent
              @click="retry"
            >
              {{ emptyActionLabel }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.drug-catalog-select {
  position: relative;
  min-width: 0;
}

.drug-select-control {
  position: relative;
}

.drug-select-control input {
  width: 100%;
  height: 36px;
  padding: 0 30px 0 10px;
  border: 1px solid var(--line, #dfe7ea);
  border-radius: 10px;
  background: var(--color-white, #fff);
  color: var(--ink, #132225);
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.drug-select-control input:focus {
  border-color: var(--primary, #0b5f78);
  outline: none;
  box-shadow: 0 0 0 3px rgba(11, 95, 120, 0.12);
}

.drug-select-control input:disabled {
  cursor: not-allowed;
  background: rgba(248, 250, 251, 0.84);
  color: var(--muted, #667681);
}

.drug-clear-btn {
  position: absolute;
  top: 50%;
  right: 6px;
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  transform: translateY(-50%);
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--muted, #667681);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.drug-clear-btn:hover {
  background: rgba(11, 95, 120, 0.08);
  color: var(--primary, #0b5f78);
}

.drug-options-popover {
  z-index: 1200;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid rgba(203, 217, 222, 0.96);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 18px 42px rgba(15, 35, 42, 0.18);
  overscroll-behavior: contain;
}

.drug-options-popover.placement-top {
  box-shadow: 0 -14px 34px rgba(15, 35, 42, 0.14);
}

.drug-option {
  display: grid;
  width: 100%;
  gap: 3px;
  padding: 7px 9px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ink, #132225);
  text-align: left;
  cursor: pointer;
}

.drug-option:hover,
.drug-option.active {
  background: rgba(224, 242, 241, 0.72);
}

.drug-option.selected {
  box-shadow: inset 2px 0 0 var(--primary, #0b5f78);
}

.drug-option:disabled,
.drug-option.disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.drug-option-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.drug-option-main strong,
.drug-option-main small {
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drug-option-main strong {
  font-size: 12px;
}

.drug-option-main small,
.drug-option-warning,
.drug-option-empty {
  color: var(--muted, #667681);
  font-size: 11px;
  line-height: 1.45;
}

.drug-option-warning {
  display: block;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drug-option-empty {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  background: rgba(248, 250, 251, 0.9);
}

.drug-option-retry {
  justify-self: start;
  min-height: 26px;
  padding: 0 9px;
  border: 1px solid rgba(11, 95, 120, 0.24);
  border-radius: 8px;
  background: rgba(224, 242, 241, 0.72);
  color: var(--primary, #0b5f78);
  font-size: 11px;
  font-weight: 900;
  cursor: pointer;
}

.drug-option-retry:hover {
  border-color: var(--primary, #0b5f78);
  background: rgba(224, 242, 241, 0.96);
}

.drug-options-enter-active,
.drug-options-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.drug-options-enter-from,
.drug-options-leave-to {
  opacity: 0;
}
</style>
