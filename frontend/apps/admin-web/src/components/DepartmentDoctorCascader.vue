<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import type { Department, Doctor } from "@smart-cloud-brain/shared-api";

const props = defineProps<{
  departmentId: number;
  doctorId: number;
  departments: Department[];
  doctors: Doctor[];
}>();

const emit = defineEmits<{
  "update:departmentId": [value: number];
  "update:doctorId": [value: number];
}>();

const triggerRef = ref<HTMLButtonElement | null>(null);
const menuRef = ref<HTMLDivElement | null>(null);
const open = ref(false);
const menuStyle = ref<Record<string, string>>({});
const expandedDepartments = ref<Set<number>>(new Set());

const selectedLabel = computed(() => {
  const doctor = props.doctors.find((item) =>
    Number(item.id) === Number(props.doctorId)
    && (!props.departmentId || Number(item.departmentId) === Number(props.departmentId))
  );
  if (doctor) {
    const department = props.departments.find((item) => Number(item.id) === Number(doctor.departmentId));
    return `${doctor.name} / ${department?.name || doctor.departmentName || "未分配科室"}`;
  }
  const department = props.departments.find((item) => Number(item.id) === Number(props.departmentId));
  return department?.name || "全部科室 / 全部医生";
});

function doctorsInDepartment(departmentId: number) {
  return props.doctors.filter((doctor) => Number(doctor.departmentId) === Number(departmentId));
}

function positionMenu() {
  const trigger = triggerRef.value;
  if (!trigger) return;
  const rect = trigger.getBoundingClientRect();
  const width = Math.min(Math.max(rect.width, 300), window.innerWidth - 24);
  const left = Math.min(Math.max(12, rect.left), Math.max(12, window.innerWidth - width - 12));
  const maxHeight = Math.min(420, window.innerHeight - 24);
  const openAbove = window.innerHeight - rect.bottom < Math.min(360, maxHeight) && rect.top > window.innerHeight - rect.bottom;
  menuStyle.value = {
    position: "fixed",
    width: `${width}px`,
    maxHeight: `${maxHeight}px`,
    left: `${left}px`,
    top: openAbove ? `${Math.max(12, rect.top - 8)}px` : `${Math.min(window.innerHeight - 12, rect.bottom + 8)}px`,
    transform: openAbove ? "translateY(-100%)" : "none",
    transformOrigin: openAbove ? "bottom left" : "top left",
  };
}

async function toggle() {
  open.value = !open.value;
  if (open.value) {
    const selectedDoctor = props.doctors.find((item) =>
      Number(item.id) === Number(props.doctorId)
      && (!props.departmentId || Number(item.departmentId) === Number(props.departmentId))
    );
    const selectedDepartmentId = selectedDoctor?.departmentId || props.departmentId;
    if (selectedDepartmentId) expandedDepartments.value.add(Number(selectedDepartmentId));
    await nextTick();
    positionMenu();
  }
}

function toggleDepartment(departmentId: number) {
  const next = new Set(expandedDepartments.value);
  if (next.has(departmentId)) next.delete(departmentId);
  else next.add(departmentId);
  expandedDepartments.value = next;
}

function selectAll() {
  emit("update:departmentId", 0);
  emit("update:doctorId", 0);
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function selectDepartment(departmentId: number) {
  emit("update:departmentId", departmentId);
  emit("update:doctorId", 0);
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function selectDoctor(doctor: Doctor) {
  emit("update:departmentId", Number(doctor.departmentId));
  emit("update:doctorId", Number(doctor.id));
  open.value = false;
  nextTick(() => triggerRef.value?.focus());
}

function handleDocumentPointer(event: MouseEvent) {
  const target = event.target as Node;
  if (triggerRef.value?.contains(target) || menuRef.value?.contains(target)) return;
  open.value = false;
}

function handleWindowChange() {
  if (open.value) positionMenu();
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== "Escape" || !open.value) return;
  open.value = false;
  triggerRef.value?.focus();
}

watch(() => props.doctorId, (doctorId) => {
  const doctor = props.doctors.find((item) => Number(item.id) === Number(doctorId));
  if (doctor) expandedDepartments.value.add(Number(doctor.departmentId));
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
  <div class="schedule-cascader">
    <button
      ref="triggerRef"
      type="button"
      class="schedule-cascader-trigger"
      :class="{ open }"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="toggle"
      @keydown.down.prevent="open || toggle()"
    >
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M4 5h6v5H4zM14 14h6v5h-6zM10 7.5h4a2 2 0 0 1 2 2V14M7 10v7a2 2 0 0 0 2 2h5" />
      </svg>
      <span>{{ selectedLabel }}</span>
      <svg class="chevron" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6" /></svg>
    </button>

    <Teleport to="body">
      <Transition name="schedule-cascader-popover">
        <div
          v-if="open"
          ref="menuRef"
          class="schedule-cascader-menu"
          :style="menuStyle"
          role="listbox"
          aria-label="按科室或医生筛选"
        >
          <button
            type="button"
            class="schedule-cascader-all"
            :class="{ selected: !departmentId && !doctorId }"
            role="option"
            :aria-selected="!departmentId && !doctorId"
            @click="selectAll"
          >
            <span class="schedule-cascader-icon">
              <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z" /></svg>
            </span>
            <span><strong>全部科室</strong><small>显示全部医生的排班</small></span>
            <svg v-if="!departmentId && !doctorId" class="check" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m5 12 4 4L19 6" /></svg>
          </button>

          <div class="schedule-cascader-divider">按科室查看</div>
          <section v-for="department in departments" :key="department.id" class="schedule-cascader-group">
            <div class="schedule-cascader-department">
              <button
                type="button"
                class="schedule-cascader-department-select"
                :class="{ selected: Number(departmentId) === Number(department.id) && !doctorId }"
                role="option"
                :aria-selected="Number(departmentId) === Number(department.id) && !doctorId"
                @click="selectDepartment(Number(department.id))"
              >
                <span class="schedule-cascader-icon">{{ department.name.slice(0, 1) }}</span>
                <span><strong>{{ department.name }}</strong><small>{{ doctorsInDepartment(Number(department.id)).length }} 位医生</small></span>
                <svg v-if="Number(departmentId) === Number(department.id) && !doctorId" class="check" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m5 12 4 4L19 6" /></svg>
              </button>
              <button
                type="button"
                class="schedule-cascader-expand"
                :aria-label="`${expandedDepartments.has(Number(department.id)) ? '收起' : '展开'}${department.name}医生`"
                :aria-expanded="expandedDepartments.has(Number(department.id))"
                @click="toggleDepartment(Number(department.id))"
              >
                <svg :class="{ expanded: expandedDepartments.has(Number(department.id)) }" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6" /></svg>
              </button>
            </div>
            <div v-if="expandedDepartments.has(Number(department.id))" class="schedule-cascader-doctors">
              <button
                v-for="doctor in doctorsInDepartment(Number(department.id))"
                :key="doctor.id"
                type="button"
                :class="{ selected: Number(doctorId) === Number(doctor.id) }"
                role="option"
                :aria-selected="Number(doctorId) === Number(doctor.id)"
                @click="selectDoctor(doctor)"
              >
                <span class="doctor-dot" />
                <span><strong>{{ doctor.name }}</strong><small>{{ doctor.title || "医生" }}</small></span>
                <svg v-if="Number(doctorId) === Number(doctor.id)" class="check" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m5 12 4 4L19 6" /></svg>
              </button>
              <p v-if="!doctorsInDepartment(Number(department.id)).length">该科室暂无医生</p>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.schedule-cascader {
  width: 240px;
  min-width: 210px;
}

.schedule-cascader-trigger {
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
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.schedule-cascader-trigger:hover,
.schedule-cascader-trigger.open,
.schedule-cascader-trigger:focus-visible {
  border-color: var(--primary);
  box-shadow: var(--focus);
  outline: none;
}

.schedule-cascader-trigger svg {
  width: 17px;
  height: 17px;
  color: var(--muted);
}

.schedule-cascader-trigger span {
  overflow: hidden;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-cascader-trigger .chevron {
  width: 15px;
  height: 15px;
  transition: transform 150ms ease;
}

.schedule-cascader-trigger.open .chevron {
  transform: rotate(180deg);
}

.schedule-cascader-menu {
  z-index: 10020;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 18px 48px rgba(15, 35, 33, 0.18);
}

.schedule-cascader-all,
.schedule-cascader-department-select,
.schedule-cascader-doctors button {
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 9px;
  padding: 8px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
}

.schedule-cascader-all:hover,
.schedule-cascader-all:focus-visible,
.schedule-cascader-all.selected,
.schedule-cascader-department-select:hover,
.schedule-cascader-department-select:focus-visible,
.schedule-cascader-department-select.selected,
.schedule-cascader-doctors button:hover,
.schedule-cascader-doctors button:focus-visible,
.schedule-cascader-doctors button.selected {
  background: var(--primary-soft);
  color: var(--primary);
  outline: none;
}

.schedule-cascader-all > span:nth-child(2),
.schedule-cascader-department-select > span:nth-child(2),
.schedule-cascader-doctors button > span:nth-child(2) {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.schedule-cascader-menu strong {
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-cascader-menu small {
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-cascader-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: var(--surface-alt);
  color: var(--primary);
  font-size: 12px;
  font-weight: 850;
}

.schedule-cascader-icon svg {
  width: 15px;
  height: 15px;
}

.schedule-cascader-menu .check {
  width: 15px;
  height: 15px;
  color: var(--primary);
}

.schedule-cascader-divider {
  margin: 5px 8px 4px;
  padding-top: 7px;
  border-top: 1px solid var(--line);
  color: var(--subtle);
  font-size: 10px;
  font-weight: 800;
}

.schedule-cascader-department {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  align-items: center;
}

.schedule-cascader-expand {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.schedule-cascader-expand:hover,
.schedule-cascader-expand:focus-visible {
  background: var(--surface-alt);
  color: var(--primary);
  outline: none;
}

.schedule-cascader-expand svg {
  width: 14px;
  height: 14px;
  transition: transform 150ms ease;
}

.schedule-cascader-expand svg.expanded {
  transform: rotate(90deg);
}

.schedule-cascader-doctors {
  margin: 1px 0 4px 23px;
  padding-left: 12px;
  border-left: 1px solid var(--line);
}

.schedule-cascader-doctors button {
  grid-template-columns: 12px minmax(0, 1fr) 18px;
  padding: 7px 8px;
}

.doctor-dot {
  width: 6px;
  height: 6px;
  justify-self: center;
  border-radius: 50%;
  background: var(--line-strong);
}

.schedule-cascader-doctors button.selected .doctor-dot {
  background: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.schedule-cascader-doctors p {
  margin: 5px 8px 8px;
  color: var(--subtle);
  font-size: 11px;
}

.schedule-cascader-popover-enter-active,
.schedule-cascader-popover-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}

.schedule-cascader-popover-enter-from,
.schedule-cascader-popover-leave-to {
  opacity: 0;
  scale: 0.97;
}

@media (max-width: 980px) {
  .schedule-cascader {
    width: 100%;
    max-width: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .schedule-cascader-trigger,
  .schedule-cascader-expand svg,
  .schedule-cascader-popover-enter-active,
  .schedule-cascader-popover-leave-active {
    transition: none;
  }
}
</style>
