<script setup lang="ts">
import { computed, ref } from "vue";
import { IconChevronDown, IconChevronUp, IconGripVertical, IconPencil, IconPlus, IconTrash } from "@tabler/icons-vue";
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
const baseModuleActions = computed(() => [
  { type: "notice", label: homeModuleTypeLabel("notice"), action: props.addNoticeModule },
  { type: "quick_actions", label: homeModuleTypeLabel("quick_actions"), action: props.addQuickActionsModule },
  { type: "intro", label: homeModuleTypeLabel("intro"), action: props.addIntroModule },
  { type: "locations", label: homeModuleTypeLabel("locations"), action: props.addLocationsModule },
  { type: "featured_departments", label: homeModuleTypeLabel("featured_departments"), action: props.addFeaturedDepartmentsModule },
  { type: "static_content", label: homeModuleTypeLabel("static_content"), action: props.addStaticContentModule },
]);
const homeSectionTypeOptions = computed(() =>
  props.sectionTypeOptions.filter((option) => !legacyHomeModuleTypes.has(option.type)),
);
const orderedModules = computed(() =>
  props.homeDraft.modules.map((module, index) => ({ module, index })),
);
const enabledModuleCount = computed(() => props.homeDraft.modules.filter((module) => module.enabled !== false).length);
const heroActionSummary = computed(() => {
  const actions = [props.homeDraft.hero.primaryAction?.label, props.homeDraft.hero.secondaryAction?.label].filter(Boolean);
  return actions.length ? actions.join(" / ") : "未配置按钮";
});

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
  <section class="home-config-hero-grid" aria-label="首页配置概览">
    <article class="config-card home-hero-summary-card">
      <div class="home-card-head">
        <div>
          <span class="home-card-kicker">首屏横幅</span>
          <h3>{{ homeDraft.hero.title || "未填写横幅标题" }}</h3>
          <p>{{ homeDraft.hero.eyebrow || "未填写眉题" }}</p>
        </div>
        <button type="button" class="status-pill" :class="homeDraft.hero.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(homeDraft.hero)">
          {{ homeDraft.hero.enabled === false ? "禁用" : "启用" }}
        </button>
      </div>
      <div class="home-hero-meta">
        <span>{{ heroActionSummary }}</span>
        <span>{{ homeDraft.hero.backgroundImageUrl ? "已配置背景图" : "未配置背景图" }}</span>
      </div>
      <div class="config-card-actions">
        <button type="button" class="topbar-refresh icon-action" @click="openEditor({ type: 'home-hero' })">
          <IconPencil :size="15" />
          <span>编辑横幅</span>
        </button>
      </div>
    </article>

    <article class="config-card home-module-stat-card">
      <div>
        <span class="home-card-kicker">模块状态</span>
        <h3>{{ enabledModuleCount }} / {{ homeDraft.modules.length }}</h3>
        <p>启用模块 / 全部模块。模块详情集中在编辑弹窗中维护。</p>
      </div>
    </article>
  </section>

  <section class="config-section home-module-toolbox">
    <div class="config-section-head">
      <div>
        <h3>新增首页模块</h3>
        <p>按模块用途分组，避免所有操作挤在标题栏。</p>
      </div>
    </div>
    <div class="home-add-groups">
      <div class="home-add-group">
        <strong>基础模块</strong>
        <div class="home-add-actions">
          <button v-for="item in baseModuleActions" :key="item.type" type="button" class="topbar-refresh icon-action" @click="item.action()">
            <IconPlus :size="15" />
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
      <div class="home-add-group">
        <strong>内容区块</strong>
        <div class="home-add-actions">
          <button v-for="option in homeSectionTypeOptions" :key="option.type" type="button" class="topbar-refresh icon-action" @click="addHomeSectionModule(option.type)">
            <IconPlus :size="15" />
            <span>{{ option.label }}</span>
          </button>
          <span v-if="!homeSectionTypeOptions.length" class="home-empty-note">暂无可新增区块</span>
        </div>
      </div>
    </div>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>首页模块编排</h3>
        <p>拖动卡片或使用上移、下移调整顺序；卡片只展示摘要，详细内容点击编辑。</p>
      </div>
    </div>
    <div class="home-module-board">
      <article
        v-for="{ module, index: moduleIndex } in orderedModules"
        :key="module.key || moduleIndex"
        class="config-card home-module-row"
        :class="{ dragging: draggedModuleIndex === moduleIndex, disabled: module.enabled === false }"
        draggable="true"
        @dragstart="startModuleDrag(moduleIndex)"
        @dragover.prevent
        @drop="dropModule(moduleIndex)"
        @dragend="draggedModuleIndex = null"
      >
        <div class="home-module-drag" aria-hidden="true">
          <IconGripVertical :size="18" />
        </div>
        <div class="home-module-order">{{ moduleIndex + 1 }}</div>
        <div class="home-module-content">
          <div class="home-module-title">
            <strong>{{ homeModuleBusinessTitle(module.type, module.key) }}</strong>
            <span>{{ homeModuleTypeLabel(module.type) }}</span>
          </div>
          <p>{{ moduleSummary(module) }}</p>
          <small>显示顺序：{{ module.sort ?? "-" }} · 内部标识：{{ homeModuleInternalIdentifier(module.type, module.key) }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="module.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(module)">
            {{ module.enabled === false ? "禁用" : "启用" }}
          </button>
          <button type="button" class="topbar-refresh icon-only" aria-label="上移模块" :disabled="moduleIndex === 0" @click="reorderHomeModule(moduleIndex, moduleIndex - 1)">
            <IconChevronUp :size="16" />
          </button>
          <button type="button" class="topbar-refresh icon-only" aria-label="下移模块" :disabled="moduleIndex === homeDraft.modules.length - 1" @click="reorderHomeModule(moduleIndex, moduleIndex + 1)">
            <IconChevronDown :size="16" />
          </button>
          <button type="button" class="topbar-refresh icon-action" @click="openEditor({ type: 'home-module', index: moduleIndex })">
            <IconPencil :size="15" />
            <span>编辑</span>
          </button>
          <button type="button" class="danger-link icon-action" @click="removeHomeModule(module)">
            <IconTrash :size="15" />
            <span>删除</span>
          </button>
        </div>
      </article>
      <div v-if="!homeDraft.modules.length" class="home-empty-module">
        暂无首页模块，先从上方选择基础模块或内容区块。
      </div>
    </div>
  </section>
</template>
