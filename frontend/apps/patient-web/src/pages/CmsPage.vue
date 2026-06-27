<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { RouterLink, useRoute } from "vue-router";
import type { PatientSiteSection } from "@smart-cloud-brain/shared-api";
import PatientSiteSectionRenderer from "../components/cms/PatientSiteSectionRenderer.vue";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";

const route = useRoute();
const { config, loading, load, loadPreview, clearPreview } = usePatientSiteConfig();

onMounted(() => {
  if (previewToken.value) void loadPreview(previewToken.value);
  else void load();
});

onUnmounted(() => {
  if (previewToken.value) clearPreview();
});

const slug = computed(() => {
  const value = route.params.slug;
  return (Array.isArray(value) ? value[0] : value || "").toLowerCase();
});

const page = computed(() =>
  config.value.pages.pages.find((item) => item.slug === slug.value && (isPreview.value || item.enabled !== false)),
);
const previewToken = computed(() => String(route.query.previewToken || ""));
const isPreview = computed(() => Boolean(previewToken.value));

const visibleSections = computed(() => page.value?.sections.filter((section) => isPreview.value || section.enabled !== false) || []);
const tocItems = computed(() =>
  visibleSections.value
    .map((section, index) => ({ id: sectionAnchor(section, index), title: section.title || "" }))
    .filter((item) => item.title),
);

function sectionAnchor(section: PatientSiteSection, index: number) {
  return section.id || `${section.type}-${index + 1}`;
}
</script>

<template>
  <main class="public-info-page cms-page">
    <header class="resource-header">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>{{ page?.label || "专题页" }}</span>
      </nav>
      <template v-if="page">
        <p class="resource-label">{{ isPreview ? "PREVIEW" : page.label }}</p>
        <h1>{{ page.title }}</h1>
        <p>{{ page.intro }}</p>
      </template>
      <template v-else>
        <p class="resource-label">CMS PAGE</p>
        <h1>{{ loading ? "页面加载中" : "页面未发布" }}</h1>
        <p>{{ loading ? "正在读取患者端站点配置。" : "该专题页不存在、未启用或尚未发布。" }}</p>
      </template>
    </header>

    <div v-if="page" class="resource-layout cms-page-layout">
      <aside class="resource-toc" aria-label="页面目录">
        <strong>页面目录</strong>
        <a v-for="item in tocItems" :key="item.id" :href="`#${item.id}`">{{ item.title }}</a>
        <span v-if="!tocItems.length">暂无目录</span>
      </aside>

      <section class="resource-content cms-page-content">
        <div
          v-for="(section, index) in visibleSections"
          :id="sectionAnchor(section, index)"
          :key="sectionAnchor(section, index)"
          class="cms-section-anchor"
        >
          <PatientSiteSectionRenderer :section="section" />
        </div>
        <p v-if="!visibleSections.length" class="public-empty">该页面暂未配置正文内容。</p>
      </section>
    </div>

    <section v-else class="resource-search-page cms-page-empty">
      <RouterLink :to="{ name: 'patient-home' }">返回首页</RouterLink>
      <RouterLink :to="{ name: 'public-search' }">搜索患者资料</RouterLink>
    </section>
  </main>
</template>
