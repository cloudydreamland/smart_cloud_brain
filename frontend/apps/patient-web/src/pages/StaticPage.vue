<script setup lang="ts">
import { computed, onMounted } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { defaultStaticPages } from "../site-config/defaultConfig";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";
import type { RouteTargetConfig, StaticPageConfig } from "../site-config/types";

const route = useRoute();
const { config, load } = usePatientSiteConfig();

onMounted(load);

const fallback: StaticPageConfig = {
  routeName: "patient-home",
  label: "智慧云脑",
  title: "智慧云脑医疗服务",
  intro: "该栏目正在按医院官网的信息架构整理，患者可继续使用预约、分诊、医生科室和诊后资料服务。",
  points: [
    { title: "专业信息", text: "内容围绕患者就医前、就医中和就医后的真实需求组织。" },
    { title: "统一入口", text: "相关服务保留在同一医院官网视觉体系中，与公共医疗信息自然衔接。" },
    { title: "持续完善", text: "后续可接入真实业务数据和医院公告内容。" },
  ],
  primary: { label: "返回首页", routeName: "patient-home" },
};

const page = computed(() => {
  const routeName = String(route.name || "");
  return config.value.staticPages.pages.find((item) => item.routeName === routeName)
    || defaultStaticPages.find((item) => item.routeName === routeName)
    || fallback;
});

function toRoute(link: RouteTargetConfig) {
  return link.query ? { name: link.routeName, query: link.query } : { name: link.routeName };
}
</script>

<template>
  <main class="static-info-page">
    <header class="resource-header">
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>{{ page.title }}</span>
      </nav>
      <p class="resource-label">{{ page.label }}</p>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
      <RouterLink v-if="page.primary" class="patient-primary static-primary" :to="toRoute(page.primary)">
        {{ page.primary.label }}
      </RouterLink>
    </header>

    <section class="static-info-grid">
      <article v-for="point in page.points" :key="point.title">
        <h2>{{ point.title }}</h2>
        <p>{{ point.text }}</p>
      </article>
    </section>
  </main>
</template>
