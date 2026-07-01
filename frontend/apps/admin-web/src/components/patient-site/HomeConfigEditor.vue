<script setup lang="ts">
import { computed, ref } from "vue";
import type { PatientHomeConfig, PatientHomeModule, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import type { EditingTarget } from "../../composables/usePatientSiteConfigEditor";
import { homeModuleBusinessTitle, homeModuleInternalIdentifier, homeModuleTypeLabel } from "../../patientSitePresentation";

const props = defineProps<{
  homeDraft: PatientHomeConfig;
  sectionTypeOptions: { type: PatientSiteSectionType; label: string }[];
  moduleSummary: (module: PatientHomeModule) => string;
  toggleEnabled: (item: { enabled?: boolean }) => void;
  openEditor: (target: EditingTarget) => void;
  addNoticeModule: () => void;
  addQuickActionsModule: () => void;
  addIntroModule: () => void;
  addLocationsModule: () => void;
  addFeaturedDepartmentsModule: () => void;
  addStaticContentModule: () => void;
  addHomeSectionModule: (type: PatientSiteSectionType) => void;
  reorderHomeModule: (fromIndex: number, toIndex: number) => void;
  removeHomeModule: (module: PatientHomeModule) => void;
}>();

const legacyHomeModuleTypes = new Set(["notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"]);
const draggedModuleIndex = ref<number | null>(null);
const homeSectionTypeOptions = computed(() =>
  props.sectionTypeOptions.filter((option) => !legacyHomeModuleTypes.has(option.type)),
);

function startModuleDrag(moduleIndex: number) {
  draggedModuleIndex.value = moduleIndex;
}

function dropModule(toIndex: number) {
  if (draggedModuleIndex.value === null) return;
  props.reorderHomeModule(draggedModuleIndex.value, toIndex);
  draggedModuleIndex.value = null;
}
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>首屏横幅</h3>
        <p>首页首屏标题、按钮和启用状态。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'home-hero' })">编辑</button>
    </div>
    <article class="config-card config-summary-card">
      <div>
        <strong>{{ homeDraft.hero.title }}</strong>
        <p>{{ homeDraft.hero.eyebrow || "未填写眉题" }}</p>
        <small>{{ homeDraft.hero.primaryAction?.label }} / {{ homeDraft.hero.secondaryAction?.label }}</small>
      </div>
      <button type="button" class="status-pill" :class="homeDraft.hero.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(homeDraft.hero)">
        {{ homeDraft.hero.enabled === false ? "禁用" : "启用" }}
      </button>
    </article>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>首页模块</h3>
        <p>模块详情在弹窗中维护。</p>
      </div>
      <div class="inline-actions">
        <button type="button" class="topbar-refresh" @click="addNoticeModule">新增{{ homeModuleTypeLabel("notice") }}</button>
        <button type="button" class="topbar-refresh" @click="addQuickActionsModule">新增{{ homeModuleTypeLabel("quick_actions") }}</button>
        <button type="button" class="topbar-refresh" @click="addIntroModule">新增{{ homeModuleTypeLabel("intro") }}</button>
        <button type="button" class="topbar-refresh" @click="addLocationsModule">新增{{ homeModuleTypeLabel("locations") }}</button>
        <button type="button" class="topbar-refresh" @click="addFeaturedDepartmentsModule">新增{{ homeModuleTypeLabel("featured_departments") }}</button>
        <button type="button" class="topbar-refresh" @click="addStaticContentModule">新增{{ homeModuleTypeLabel("static_content") }}</button>
        <button v-for="option in homeSectionTypeOptions" :key="option.type" type="button" class="topbar-refresh" @click="addHomeSectionModule(option.type)">
          新增{{ option.label }}
        </button>
      </div>
    </div>
    <div class="config-card-grid">
      <article
        v-for="(module, moduleIndex) in homeDraft.modules"
        :key="module.key || moduleIndex"
        class="config-card config-summary-card home-module-card"
        :class="{ dragging: draggedModuleIndex === moduleIndex }"
        draggable="true"
        @dragstart="startModuleDrag(moduleIndex)"
        @dragover.prevent
        @drop="dropModule(moduleIndex)"
        @dragend="draggedModuleIndex = null"
      >
        <div>
          <strong>{{ homeModuleBusinessTitle(module.type, module.key) }}</strong>
          <p>{{ moduleSummary(module) }}</p>
          <small>显示顺序：{{ module.sort ?? "-" }} · 内部标识：{{ homeModuleInternalIdentifier(module.type, module.key) }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="module.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(module)">{{ module.enabled === false ? "禁用" : "启用" }}</button>
          <button type="button" class="topbar-refresh" :disabled="moduleIndex === 0" @click="reorderHomeModule(moduleIndex, moduleIndex - 1)">上移</button>
          <button type="button" class="topbar-refresh" :disabled="moduleIndex === homeDraft.modules.length - 1" @click="reorderHomeModule(moduleIndex, moduleIndex + 1)">下移</button>
          <button type="button" class="topbar-refresh" @click="openEditor({ type: 'home-module', index: moduleIndex })">编辑</button>
          <button type="button" class="danger-link" @click="removeHomeModule(module)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>
