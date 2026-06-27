<script setup lang="ts">
import type { PatientHomeConfig, PatientHomeModule } from "@smart-cloud-brain/shared-api";
import type { EditingTarget } from "../../composables/usePatientSiteConfigEditor";

defineProps<{
  homeDraft: PatientHomeConfig;
  moduleSummary: (module: PatientHomeModule) => string;
  toggleEnabled: (item: { enabled?: boolean }) => void;
  openEditor: (target: EditingTarget) => void;
  addNoticeModule: () => void;
  addQuickActionsModule: () => void;
  addIntroModule: () => void;
  addLocationsModule: () => void;
  addFeaturedDepartmentsModule: () => void;
  addStaticContentModule: () => void;
  removeHomeModule: (module: PatientHomeModule) => void;
}>();
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
        <p>{{ homeDraft.hero.eyebrow || "无 eyebrow" }}</p>
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
        <button type="button" class="topbar-refresh" @click="addNoticeModule">新增 notice</button>
        <button type="button" class="topbar-refresh" @click="addQuickActionsModule">新增 quick_actions</button>
        <button type="button" class="topbar-refresh" @click="addIntroModule">新增 intro</button>
        <button type="button" class="topbar-refresh" @click="addLocationsModule">新增 locations</button>
        <button type="button" class="topbar-refresh" @click="addFeaturedDepartmentsModule">新增 featured_departments</button>
        <button type="button" class="topbar-refresh" @click="addStaticContentModule">新增 static_content</button>
      </div>
    </div>
    <div class="config-card-grid">
      <article v-for="(module, moduleIndex) in homeDraft.modules" :key="module.key || moduleIndex" class="config-card config-summary-card">
        <div>
          <strong>{{ module.type }} / {{ module.key }}</strong>
          <p>{{ moduleSummary(module) }}</p>
          <small>sort {{ module.sort ?? "-" }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="module.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(module)">{{ module.enabled === false ? "禁用" : "启用" }}</button>
          <button type="button" class="topbar-refresh" @click="openEditor({ type: 'home-module', index: moduleIndex })">编辑</button>
          <button type="button" class="danger-link" @click="removeHomeModule(module)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>
