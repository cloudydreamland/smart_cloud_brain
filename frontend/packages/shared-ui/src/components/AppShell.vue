<script setup lang="ts">
import SideNav from "./SideNav.vue";

defineProps<{
  mark: string;
  title: string;
  subtitle: string;
  userName?: string;
  userMeta?: string;
  navGroups: Array<{ label: string; items: Array<{ label: string; to: string; badge?: string | number }> }>;
}>();

defineEmits<{ logout: [] }>();
</script>

<template>
  <main class="workspace-shell">
    <aside class="workspace-sidebar">
      <div class="workspace-sidebar-inner">
        <div class="brand-mark">
          <div class="brand-icon">{{ mark }}</div>
          <div>
            <h1>{{ title }}</h1>
            <span>{{ subtitle }}</span>
          </div>
        </div>

        <div v-if="userName" class="user-box">
          <strong>{{ userName }}</strong>
          <span>{{ userMeta }}</span>
          <slot name="user" />
        </div>

        <SideNav :groups="navGroups" />

        <div class="sidebar-footer">
          <button type="button" class="ghost" @click="$emit('logout')">退出登录</button>
        </div>
      </div>
    </aside>
    <section class="workspace">
      <slot />
    </section>
  </main>
</template>
