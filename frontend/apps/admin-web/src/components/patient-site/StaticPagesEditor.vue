<script setup lang="ts">
import type { StaticPageConfig } from "@smart-cloud-brain/shared-api";
import type { EditingTarget } from "../../composables/usePatientSiteConfigEditor";

defineProps<{
  search: string;
  disabledOnly: boolean;
  pages: { page: StaticPageConfig; index: number }[];
  toggleEnabled: (item: { enabled?: boolean }) => void;
  openEditor: (target: EditingTarget) => void;
  addStaticPage: () => void;
  removeStaticPage: (index: number) => void;
}>();

defineEmits<{
  "update:search": [value: string];
  "update:disabledOnly": [value: boolean];
}>();
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>静态页面</h3>
        <p>按患者端页面入口匹配的内容页。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addStaticPage">新增页面</button>
    </div>
    <div class="static-page-tools">
      <label>
        <span>搜索页面</span>
        <input :value="search" type="search" placeholder="搜索页面入口、页面名称或标题" @input="$emit('update:search', ($event.target as HTMLInputElement).value)">
      </label>
      <label class="check-field compact">
        <input :checked="disabledOnly" type="checkbox" @change="$emit('update:disabledOnly', ($event.target as HTMLInputElement).checked)">
        <span>仅看停用项</span>
      </label>
    </div>
    <div class="config-card-grid">
      <article v-for="{ page, index } in pages" :key="`${page.routeName}-${index}`" class="config-card config-summary-card">
        <div>
          <strong>{{ page.title || "未命名页面" }}</strong>
          <p>页面入口：{{ page.routeName }} · 显示名称：{{ page.label || "无分组" }} · {{ page.points?.length || 0 }} 个要点</p>
          <small>{{ page.intro || "未填写说明" }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="page.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(page)">{{ page.enabled === false ? "禁用" : "启用" }}</button>
          <button type="button" class="topbar-refresh" @click="openEditor({ type: 'static-page', index })">编辑</button>
          <button type="button" class="danger-link" @click="removeStaticPage(index)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>
