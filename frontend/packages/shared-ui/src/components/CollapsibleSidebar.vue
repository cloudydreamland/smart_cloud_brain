<script setup lang="ts">
import { ref } from "vue";

export interface NavItem {
  label: string;
  to: string;
  badge?: string | number;
  icon?: string;
}

export interface NavGroup {
  label?: string;
  items: NavItem[];
}

const props = withDefaults(defineProps<{
  mark: string;
  title: string;
  groups: NavGroup[];
  userName?: string;
  userMeta?: string;
  currentPath?: string;
  icons?: Record<string, string>;
}>(), {
  currentPath: "/",
  icons: () => ({}),
});

defineEmits<{ logout: []; navigate: [path: string] }>();

const open = ref(false);

/* 默认图标 map（仅在 props.icons 未覆盖时使用） */
const defaultIcons: Record<string, string> = {
  首页: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 13h7V4H4v9Zm9 7h7V4h-7v16ZM4 20h7v-5H4v5Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
  队列: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 6h16M4 12h10M4 18h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  病历: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><rect x="5" y="2" width="14" height="20" rx="2" stroke="currentColor" stroke-width="2"/><path d="M9 7h6M9 11h6M9 15h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  处方: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><rect x="9" y="3" width="6" height="4" rx="1" stroke="currentColor" stroke-width="2"/><path d="M9 14h6M9 18h4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  通知: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M13.73 21a2 2 0 0 1-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  设置: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z" stroke="currentColor" stroke-width="1.8"/></svg>',
  工作台: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 13h7V4H4v9Zm9 7h7V4h-7v16ZM4 20h7v-5H4v5Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
  科室: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><polyline points="9,22 9,12 15,12 15,22" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
  医生: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  药品: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Z" stroke="currentColor" stroke-width="2"/><path d="M9 12h6M12 9v6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  排班: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/><path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  分诊台: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" stroke="currentColor" stroke-width="2"/><rect x="8" y="2" width="8" height="4" rx="1" stroke="currentColor" stroke-width="2"/><path d="M9 14l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  知识库: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 7 12 3l8 4-8 4-8-4Zm3 4v5c0 2 2.2 4 5 4s5-2 5-4v-5" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
  提示词: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  字典: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" stroke="currentColor" stroke-width="2"/></svg>',
  搜索: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/><path d="m21 21-4.35-4.35" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
  账户权限: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
};

function getIcon(name: string): string {
  const merged = { ...defaultIcons, ...props.icons };
  return merged[name] || merged["首页"];
}

function isActive(to: string) {
  const p = props.currentPath;
  if (to === "/") return p === "/";
  return p === to || p.startsWith(to + "/");
}
</script>

<template>
  <aside class="c-sidebar" :class="{ open }" @mouseenter="open = true" @mouseleave="open = false">
    <div class="c-sidebar-inner">
      <!-- Brand -->
      <div class="c-org-bar">
        <div class="c-ghost-btn">
          <span class="c-mark">{{ mark }}</span>
          <span class="c-label">{{ title }}</span>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="c-nav-wrap">
        <template v-for="(group, gi) in groups" :key="group.label ?? gi">
          <div v-if="group.label" class="c-group-label">{{ group.label }}</div>
          <div class="c-nav-group">
            <a @click.prevent="$emit('navigate', item.to)"
              v-for="item in group.items"
              :key="item.to"
              :href="item.to"
              class="c-nav-item"
              :class="{ active: isActive(item.to) }"
            >
              <span class="c-nav-icon" v-html="getIcon(item.icon || item.label)" />
              <span class="c-label">{{ item.label }}</span>
              <span v-if="item.badge !== undefined" class="c-badge">{{ item.badge }}</span>
            </a>
          </div>
          <div v-if="gi < groups.length - 1" class="c-divider" />
        </template>
      </nav>

      <!-- Footer: User -->
      <div class="c-sidebar-footer">
        <div class="c-ghost-btn">
          <span class="c-avatar">{{ userName?.charAt(0) || '?' }}</span>
          <span class="c-user-copy">
            <span class="c-label">{{ userName }}</span>
            <span class="c-user-meta">{{ userMeta }}</span>
          </span>
          <span class="c-chevron">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="m7 9 5-5 5 5M7 15l5 5 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </span>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.c-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 100;
  width: 56px;
  background: rgba(255, 255, 255, 0.88);
  border-right: 1px solid color-mix(in srgb, var(--line, #e5e7eb) 86%, transparent);
  box-shadow: 1px 0 16px rgba(15, 35, 42, 0.05);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
  transition: width 220ms cubic-bezier(.2, .8, .2, 1);
  overflow: hidden;
}

.c-sidebar.open {
  width: 236px;
}

.c-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  color: var(--muted, #71717a);
  padding: 10px 8px;
}

/* ── Org bar ── */
.c-org-bar {
  min-height: 40px;
  display: flex;
  align-items: center;
  padding: 0;
  margin-bottom: 12px;
  flex-shrink: 0;
}

/* ── Ghost button (reusable) ── */
.c-ghost-btn {
  position: relative;
  width: 100%;
  min-height: 40px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 9px;
  cursor: pointer;
  text-align: left;
  transition: background 160ms ease, color 160ms ease, transform 160ms ease;
}

.c-ghost-btn:hover {
  background: var(--surface-2, #f3f5f8);
  color: var(--ink, #171717);
  transform: translateX(1px);
}

/* ── Mark (icon circle) ── */
.c-mark {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex: 0 0 24px;
  font-size: 12px;
  font-weight: 900;
  background: linear-gradient(145deg, var(--primary, #007a7a), color-mix(in srgb, var(--primary, #007a7a) 78%, white));
  color: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25), 0 8px 18px color-mix(in srgb, var(--primary, #007a7a) 22%, transparent);
}

/* ── Label (hidden when collapsed) ── */
.c-label {
  opacity: 0;
  transform: translateX(-8px);
  white-space: nowrap;
  font-size: 13px;
  transition: opacity 160ms ease, transform 160ms ease;
}

.c-sidebar.open .c-label {
  opacity: 1;
  transform: translateX(0);
}

/* ── Chevron ── */
.c-chevron {
  margin-left: auto;
  color: var(--weak, #a1a1aa);
  opacity: 0;
  transition: opacity 160ms ease;
  flex-shrink: 0;
}

.c-sidebar.open .c-chevron {
  opacity: 1;
}

/* ── Nav wrap ── */
.c-nav-wrap {
  flex: 1;
  overflow: auto;
  padding: 0;
}

.c-nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 6px 0;
}

.c-group-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--weak, #a1a1aa);
  padding: 8px 8px 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0;
  transition: opacity 160ms ease;
}

.c-sidebar.open .c-group-label {
  opacity: 1;
}

.c-divider {
  height: 1px;
  background: color-mix(in srgb, var(--line, #e5e7eb) 75%, transparent);
  margin: 6px 0;
}

/* ── Nav item ── */
.c-nav-item {
  width: 100%;
  height: 38px;
  border-radius: 12px;
  padding: 0 9px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted, #71717a);
  cursor: pointer;
  font-size: 13px;
  text-decoration: none;
  transition: background 160ms ease, color 160ms ease, transform 160ms ease;
}

.c-nav-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.c-nav-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.c-nav-item:hover {
  background: var(--surface-2, #f3f5f8);
  color: var(--ink, #171717);
  transform: translateX(1px);
}

.c-nav-item.active {
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary, #2563eb) 12%, transparent), color-mix(in srgb, var(--primary, #2563eb) 6%, transparent));
  color: var(--primary, #2563eb);
  font-weight: 800;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary, #2563eb) 12%, transparent);
}

/* ── Badge ── */
.c-badge {
  margin-left: auto;
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--primary-soft, #dbeafe);
  color: var(--primary, #2563eb);
  font-size: 10px;
  font-weight: 900;
  opacity: 0;
  transition: opacity 160ms ease;
}

.c-sidebar.open .c-badge {
  opacity: 1;
}

/* ── Footer ── */
.c-sidebar-footer {
  flex-shrink: 0;
  padding-top: 10px;
  border-top: 1px solid color-mix(in srgb, var(--line, #e5e7eb) 75%, transparent);
  display: grid;
  gap: 2px;
  background: transparent;
}

.c-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex: 0 0 24px;
  font-size: 12px;
  font-weight: 900;
  background: var(--surface-2, #f3f5f8);
  color: var(--primary, #334155);
}

.c-user-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.c-user-meta {
  opacity: 0;
  transform: translateX(-8px);
  color: var(--muted, #71717a);
  font-size: 11px;
  line-height: 1.1;
  white-space: nowrap;
  transition: opacity 160ms ease, transform 160ms ease;
}

.c-sidebar.open .c-user-meta {
  opacity: 1;
  transform: translateX(0);
}
</style>
