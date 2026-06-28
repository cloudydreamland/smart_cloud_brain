<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../site-config/routeTarget";
import { startPatientSiteConfigAutoRefresh, usePatientSiteConfig } from "../site-config/usePatientSiteConfig";
import type { PatientNavMenu, RouteTargetConfig } from "../site-config/types";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const { session } = storeToRefs(auth);
const activeMenu = ref<string | null>(null);
const mobileOpen = ref(false);
const userOpen = ref(false);
const { config, disabledStaticPageRouteNames, load } = usePatientSiteConfig();

auth.load("patient-session", "PATIENT");
onMounted(() => {
  void load();
  startPatientSiteConfigAutoRefresh();
});

const isSignedIn = computed(() => Boolean(session.value && !auth.permissionError));
const userName = computed(() => session.value?.name || "患者");
const nav = computed(() => config.value.nav);
const navMenus = computed<PatientNavMenu[]>(() =>
  nav.value.menus
    .filter((menu) => menu.enabled !== false)
    .map((menu) => {
      const links = visibleLinks(menu.links);
      const feature = isVisibleLink(menu.feature) ? menu.feature : undefined;
      return { ...menu, links, feature };
    })
    .filter((menu) => Boolean(menu.label && ((menu.links || []).length || menu.feature))),
);
const userLinks = computed(() => visibleLinks(nav.value.userLinks));
const brandRoute = computed(() => ({ name: nav.value.brand.homeRoute || "patient-home" }));
const brandLogoAlt = computed(() => nav.value.brand.logoAlt || nav.value.brand.name);

function isVisibleLink(link: RouteTargetConfig | undefined): link is RouteTargetConfig {
  return Boolean(
    link &&
      link.enabled !== false &&
      link.label &&
      link.routeName &&
      (link.routeName !== "cms-page" || link.slug) &&
      !disabledStaticPageRouteNames.value.has(link.routeName),
  );
}

function visibleLinks(links: RouteTargetConfig[] | undefined) {
  return (links || []).filter(isVisibleLink);
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
    const to = router.resolve(toPatientRoute(link)).path;
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
        <img v-if="nav.brand.logoUrl" class="site-brand-logo" :src="nav.brand.logoUrl" :alt="brandLogoAlt">
        <template v-else>
          <i></i><i></i>
        </template>
        <span>{{ nav.brand.name }}</span>
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
              <RouterLink v-for="link in menu.links" :key="`${menu.key}-${link.label}`" :to="toPatientRoute(link)" @click="closeMenus">
                <strong>{{ link.label }}</strong>
                <span v-if="link.description">{{ link.description }}</span>
              </RouterLink>
            </section>
            <aside v-if="menu.feature" class="site-mega-feature">
              <p>患者导向服务</p>
              <strong>官网内完成查找、预约、资料查看和复诊准备。</strong>
              <RouterLink :to="toPatientRoute(menu.feature)" @click="closeMenus">{{ menu.feature.label }}</RouterLink>
            </aside>
          </div>
        </div>
      </nav>

      <div class="site-actions">
        <template v-if="!isSignedIn">
          <RouterLink class="site-login-link" :to="{ name: 'patient-login' }" @click="closeMenus">登录/注册</RouterLink>
        </template>
        <div v-else class="site-user-menu">
          <button type="button" :aria-expanded="userOpen" @click="userOpen = !userOpen">
            {{ userName }}
            <span aria-hidden="true"></span>
          </button>
          <div v-show="userOpen" class="site-user-dropdown">
            <RouterLink v-for="link in userLinks" :key="link.label" :to="toPatientRoute(link)" @click="closeMenus">
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
