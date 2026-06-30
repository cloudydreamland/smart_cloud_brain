<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import type { DrugCatalogOption } from "../types/drugCatalog";

const props = withDefaults(defineProps<{
  modelValue: string;
  drugs: DrugCatalogOption[];
  disabled?: boolean;
  placeholder?: string;
  ariaLabel?: string;
}>(), {
  placeholder: "搜索并选择目录药品",
  ariaLabel: "选择药品",
});

const emit = defineEmits<{
  "update:modelValue": [value: string];
  select: [drug: DrugCatalogOption];
}>();

const query = ref(props.modelValue);
const open = ref(false);
const activeIndex = ref(0);
const inputEl = ref<HTMLInputElement | null>(null);

watch(() => props.modelValue, (value) => {
  query.value = value;
});

const selectedDrug = computed(() =>
  props.drugs.find((drug) => drug.name === props.modelValue),
);

const filteredDrugs = computed(() => {
  const keyword = query.value.trim().toLowerCase();
  const source = props.drugs;
  if (!keyword) return source.slice(0, 8);
  return source
    .filter((drug) =>
      [drug.name, drug.specification, drug.contraindication, drug.interactionRule]
        .some((value) => String(value ?? "").toLowerCase().includes(keyword)),
    )
    .slice(0, 8);
});

const activeDrug = computed(() => filteredDrugs.value[activeIndex.value]);

function focusInput() {
  inputEl.value?.focus();
  if (!props.disabled) open.value = true;
}

function handleInput() {
  emit("update:modelValue", "");
  activeIndex.value = 0;
  if (!props.disabled) open.value = true;
}

function selectDrug(drug: DrugCatalogOption) {
  if (drug.disabled || props.disabled) return;
  query.value = drug.name;
  emit("update:modelValue", drug.name);
  emit("select", drug);
  open.value = false;
}

function move(delta: number) {
  if (!open.value) {
    open.value = true;
    return;
  }
  const count = filteredDrugs.value.length;
  if (!count) return;
  activeIndex.value = (activeIndex.value + delta + count) % count;
}

function chooseActive() {
  if (activeDrug.value) selectDrug(activeDrug.value);
}

function closeSoon() {
  window.setTimeout(() => {
    open.value = false;
    query.value = props.modelValue;
  }, 120);
}

function clearSelection() {
  query.value = "";
  emit("update:modelValue", "");
  nextTick(() => focusInput());
}
</script>

<template>
  <div class="drug-catalog-select" :class="{ open, disabled }">
    <div class="drug-select-control" @click="focusInput">
      <input
        ref="inputEl"
        v-model="query"
        type="text"
        :disabled="disabled"
        :placeholder="placeholder"
        :aria-label="ariaLabel"
        role="combobox"
        :aria-expanded="open"
        autocomplete="off"
        @input="handleInput"
        @focus="open = !disabled"
        @blur="closeSoon"
        @keydown.down.prevent="move(1)"
        @keydown.up.prevent="move(-1)"
        @keydown.enter.prevent="chooseActive"
        @keydown.esc.prevent="open = false"
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

    <div v-if="selectedDrug" class="drug-selected-meta">
      <span v-if="selectedDrug.specification">{{ selectedDrug.specification }}</span>
      <span v-if="selectedDrug.status" class="drug-status">{{ selectedDrug.status }}</span>
    </div>

    <div v-if="open" class="drug-options-popover" role="listbox">
      <button
        v-for="(drug, index) in filteredDrugs"
        :key="drug.id ?? drug.name"
        type="button"
        class="drug-option"
        :class="{ active: index === activeIndex, disabled: drug.disabled }"
        :disabled="drug.disabled"
        role="option"
        @mousedown.prevent
        @click="selectDrug(drug)"
      >
        <span class="drug-option-main">
          <strong>{{ drug.name }}</strong>
          <small>{{ drug.specification || "未填写规格" }}</small>
        </span>
        <span v-if="drug.status" class="drug-option-status">{{ drug.status }}</span>
        <span v-if="drug.contraindication || drug.interactionRule" class="drug-option-warning">
          {{ drug.contraindication || drug.interactionRule }}
        </span>
      </button>
      <div v-if="!filteredDrugs.length" class="drug-option-empty">
        未找到匹配药品，请从管理端药品目录维护后再选择。
      </div>
    </div>
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
  height: 34px;
  padding: 0 28px 0 10px;
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

.drug-selected-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
  color: var(--muted, #667681);
  font-size: 10px;
  font-weight: 800;
}

.drug-selected-meta span {
  overflow: hidden;
  max-width: 100%;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drug-status {
  color: var(--success, #15803d);
}

.drug-options-popover {
  position: absolute;
  z-index: 50;
  top: calc(100% + 6px);
  left: 0;
  width: min(360px, 78vw);
  max-height: 270px;
  overflow: auto;
  padding: 6px;
  border: 1px solid rgba(203, 217, 222, 0.96);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 42px rgba(15, 35, 42, 0.16);
}

.drug-option {
  display: grid;
  width: 100%;
  gap: 5px;
  padding: 9px 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--ink, #132225);
  text-align: left;
  cursor: pointer;
}

.drug-option:hover,
.drug-option.active {
  background: rgba(224, 242, 241, 0.74);
}

.drug-option:disabled,
.drug-option.disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.drug-option-main {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.drug-option-main strong,
.drug-option-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drug-option-main strong {
  font-size: 12px;
}

.drug-option-main small,
.drug-option-status,
.drug-option-warning,
.drug-option-empty {
  color: var(--muted, #667681);
  font-size: 11px;
  line-height: 1.45;
}

.drug-option-status {
  color: var(--primary, #0b5f78);
  font-weight: 800;
}

.drug-option-warning {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drug-option-empty {
  padding: 12px;
  border-radius: 10px;
  background: rgba(248, 250, 251, 0.9);
}
</style>
