<script setup lang="ts">
import type { PatientNavConfig } from "@smart-cloud-brain/shared-api";
import type { EditingTarget } from "../../composables/usePatientSiteConfigEditor";
import { patientSiteFieldLabel } from "../../patientSitePresentation";

defineProps<{
  navDraft: PatientNavConfig;
  routeLabel: (routeName?: string) => string;
  toggleEnabled: (item: { enabled?: boolean }) => void;
  openEditor: (target: EditingTarget) => void;
  addMenu: () => void;
  removeMenu: (index: number) => void;
  addUserLink: () => void;
  removeUserLink: (index: number) => void;
}>();
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>品牌信息</h3>
      </div>
      <button type="button" class="topbar-refresh" @click="openEditor({ type: 'brand' })">编辑</button>
    </div>
    <article class="config-card config-summary-card">
      <div>
        <strong>{{ navDraft.brand.name }}</strong>
        <p>首页入口：{{ routeLabel(navDraft.brand.homeRoute) }} · 内部标识：{{ navDraft.brand.homeRoute }}</p>
      </div>
      <span class="status-pill enabled">生效</span>
    </article>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>顶部菜单</h3>
        <p>每个菜单点击后在弹窗中维护链接和 feature 入口。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addMenu">新增菜单</button>
    </div>
    <div class="config-card-grid">
      <article v-for="(menu, menuIndex) in navDraft.menus" :key="menu.key || menuIndex" class="config-card config-summary-card">
        <div>
          <strong>{{ menu.label || "未命名菜单" }}</strong>
          <p>内部标识：{{ menu.key }} · {{ menu.links?.length || 0 }} 个链接 · {{ patientSiteFieldLabel("sort") }}：{{ menu.sort ?? "-" }}</p>
          <small>{{ menu.lead || menu.description || "未填写导语" }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="menu.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(menu)">{{ menu.enabled === false ? "禁用" : "启用" }}</button>
          <button type="button" class="topbar-refresh" @click="openEditor({ type: 'nav-menu', index: menuIndex })">编辑</button>
          <button type="button" class="danger-link" @click="removeMenu(menuIndex)">删除</button>
        </div>
      </article>
    </div>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>用户菜单入口</h3>
        <p>登录后用户下拉菜单中的入口。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addUserLink">新增入口</button>
    </div>
    <div class="config-list">
      <article v-for="(link, linkIndex) in navDraft.userLinks" :key="`user-${linkIndex}`" class="config-row-card config-summary-row">
        <div>
          <strong>{{ link.label || "未命名入口" }}</strong>
          <span>页面入口：{{ routeLabel(link.routeName) }} · 内部标识：{{ link.routeName }} · {{ patientSiteFieldLabel("sort") }}：{{ link.sort ?? "-" }}</span>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="link.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(link)">{{ link.enabled === false ? "禁用" : "启用" }}</button>
          <button type="button" class="topbar-refresh" @click="openEditor({ type: 'user-link', index: linkIndex })">编辑</button>
          <button type="button" class="danger-link" @click="removeUserLink(linkIndex)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>
