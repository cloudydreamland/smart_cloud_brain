<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";
import type { PatientNavMenu, RouteTargetConfig } from "../site-config/types";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const { session } = storeToRefs(auth);
const activeMenu = ref<string | null>(null);
const mobileOpen = ref(false);
const userOpen = ref(false);
const { config, load } = usePatientSiteConfig();

auth.load("patient-session", "PATIENT");
onMounted(load);

const isSignedIn = computed(() => Boolean(session.value && !auth.permissionError));
const userName = computed(() => session.value?.name || "患者");
const nav = computed(() => config.value.nav);
const navMenus = computed(() => nav.value.menus);
const userLinks = computed(() => nav.value.userLinks);
const brandRoute = computed(() => ({ name: nav.value.brand.homeRoute || "patient-home" }));

function toRoute(link: RouteTargetConfig) {
  return link.query ? { name: link.routeName, query: link.query } : { name: link.routeName };
}

function showMenu(key: string) {
  activeMenu.value = key;
}

function toggleMenu(key: string) {
  activeMenu.value = activeMenu.value === key ? null : key;
}

function closeMenus() {
  activeMenu.value = null;
  mobileOpen.value = false;
  userOpen.value = false;
}

function isMenuActive(menu: PatientNavMenu) {
  return (menu.links || []).some((link) => {
    const to = router.resolve(toRoute(link)).path;
    return route.path === to || (to !== "/" && route.path.startsWith(to));
  });
}

function openSearch() {
  closeMenus();
  router.push({ name: "public-search" });
}

async function logout() {
  auth.logout();
  closeMenus();
  await router.push({ name: "patient-home" });
}
</script>

<template>
  <header class="site-header" @mouseleave="activeMenu = null">
    <div class="site-header-inner">
      <RouterLink class="scb-brand" :to="brandRoute" :aria-label="`${nav.brand.name}首页`" @click="closeMenus">
        <span>{{ nav.brand.name }}</span>
        <i></i><i></i>
      </RouterLink>

      <nav class="site-nav" :class="{ open: mobileOpen }" aria-label="主导航">
        <div
          v-for="menu in navMenus"
          :key="menu.key"
          class="site-nav-item"
          :class="{ 'is-open': activeMenu === menu.key, active: isMenuActive(menu) }"
          @mouseenter="showMenu(menu.key)"
        >
          <button type="button" :aria-expanded="activeMenu === menu.key" @click="toggleMenu(menu.key)">
            {{ menu.label }}
            <span aria-hidden="true"></span>
          </button>
          <div v-show="activeMenu === menu.key" class="site-mega">
            <section class="site-mega-lead">
              <p>{{ menu.lead }}</p>
              <h2>{{ menu.label }}</h2>
              <span>{{ menu.description }}</span>
            </section>
            <section class="site-mega-links">
              <RouterLink v-for="link in menu.links" :key="`${menu.key}-${link.label}`" :to="toRoute(link)" @click="closeMenus">
                <strong>{{ link.label }}</strong>
                <span v-if="link.description">{{ link.description }}</span>
              </RouterLink>
            </section>
            <aside v-if="menu.feature" class="site-mega-feature">
              <p>患者导向服务</p>
              <strong>官网内完成查找、预约、资料查看和复诊准备。</strong>
              <RouterLink :to="toRoute(menu.feature)" @click="closeMenus">{{ menu.feature.label }}</RouterLink>
            </aside>
          </div>
        </div>
      </nav>

      <div class="site-actions">
        <RouterLink class="site-appointment-link" :to="{ name: 'patient-doctors' }" @click="closeMenus">预约就诊</RouterLink>
        <template v-if="!isSignedIn">
          <RouterLink class="site-login-link" :to="{ name: 'patient-login' }" @click="closeMenus">登录/注册</RouterLink>
        </template>
        <div v-else class="site-user-menu">
          <button type="button" :aria-expanded="userOpen" @click="userOpen = !userOpen">
            {{ userName }}
            <span aria-hidden="true"></span>
          </button>
          <div v-show="userOpen" class="site-user-dropdown">
            <RouterLink v-for="link in userLinks" :key="link.label" :to="toRoute(link)" @click="closeMenus">
              {{ link.label }}
            </RouterLink>
            <button type="button" @click="logout">退出登录</button>
          </div>
        </div>
        <button class="site-search-button" type="button" aria-label="搜索" @click="openSearch">
          <span class="search-symbol" aria-hidden="true"></span>
        </button>
        <button class="site-menu-button" type="button" aria-label="打开菜单" @click="mobileOpen = !mobileOpen">
          <span></span>
        </button>
      </div>
    </div>
  </header>
</template>
