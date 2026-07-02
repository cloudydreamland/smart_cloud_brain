<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import type { PatientSiteSection } from "@smart-cloud-brain/shared-api";
import PatientSiteSectionRenderer from "../components/cms/PatientSiteSectionRenderer.vue";
import { getFallbackPublicPage, makeCmsPublicPage, type PublicPage, type RouteTarget } from "../site-content/publicContent";
import { withPatientPreview } from "../site-config/routeTarget";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";

const route = useRoute();
const { config, activePreviewToken } = usePatientSiteConfig();
const routeName = computed(() => String(route.name || ""));
const isPreview = computed(() => Boolean(activePreviewToken.value));
const cmsStaticPage = computed(() =>
  config.value.staticPages.pages.find((item) => item.routeName === routeName.value),
);
const boundCmsPage = computed(() => {
  const staticPage = cmsStaticPage.value;
  if (staticPage?.contentSource !== "cms-page" || !staticPage.slug || (!isPreview.value && staticPage.enabled === false)) return null;
  return config.value.pages.pages.find((item) => item.slug === staticPage.slug && (isPreview.value || item.enabled !== false)) || null;
});
const page = computed<PublicPage>(() => {
  if (boundCmsPage.value) {
    return {
      ...getFallbackPublicPage(routeName.value),
      label: boundCmsPage.value.label,
      title: boundCmsPage.value.title,
      intro: boundCmsPage.value.intro,
      metrics: [],
      narrative: [],
      features: [],
      workflow: [],
      faq: [],
      blocks: [],
      related: [],
    };
  }
  const cmsPage = cmsStaticPage.value;
  if (cmsPage && cmsPage.enabled !== false) return makeCmsPublicPage(cmsPage);
  return getFallbackPublicPage(routeName.value);
});
const visibleSections = computed(() => boundCmsPage.value?.sections.filter((section) => isPreview.value || section.enabled !== false) || []);
const tocItems = computed(() =>
  visibleSections.value
    .map((section, index) => ({ id: sectionAnchor(section, index), title: section.title || "" }))
    .filter((item) => item.title),
);

function sectionAnchor(section: PatientSiteSection, index: number) {
  return section.id || `${section.type}-${index + 1}`;
}

function previewRoute(to: RouteTarget) {
  return withPatientPreview(to, activePreviewToken.value);
}
</script>

<template>
  <main class="public-info-page">
    <header class="resource-header">
      <RouterLink class="scb-brand" :to="previewRoute({ name: 'patient-home' })" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav aria-label="当前位置">
        <RouterLink :to="previewRoute({ name: 'patient-home' })">首页</RouterLink>
        <span>/</span>
        <span>{{ page.title }}</span>
      </nav>
      <p class="resource-label">{{ page.label }}</p>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
      <div v-if="page.metrics?.length" class="resource-metrics">
        <article v-for="item in page.metrics" :key="`${item.value}-${item.label}`">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
          <p>{{ item.caption }}</p>
        </article>
      </div>
    </header>

    <div class="resource-layout">
      <aside class="resource-toc" aria-label="资料目录">
        <strong>资料目录</strong>
        <template v-if="boundCmsPage">
          <a v-for="item in tocItems" :key="item.id" :href="`#${item.id}`">{{ item.title }}</a>
          <span v-if="!tocItems.length">暂无目录</span>
        </template>
        <template v-else>
          <a v-if="page.features?.length" href="#重点模块">重点模块</a>
          <a v-for="block in page.blocks" :key="block.title" :href="`#${block.title}`">{{ block.title }}</a>
          <a v-if="page.workflow?.length" href="#服务流程">服务流程</a>
          <a v-if="page.faq?.length" href="#常见问题">常见问题</a>
        </template>
      </aside>

      <section class="resource-content">
        <template v-if="boundCmsPage">
          <div
            v-for="(section, index) in visibleSections"
            :id="sectionAnchor(section, index)"
            :key="sectionAnchor(section, index)"
            class="cms-section-anchor"
          >
            <PatientSiteSectionRenderer :section="section" />
          </div>
          <p v-if="!visibleSections.length" class="public-empty">该页面暂未配置正文内容。</p>
        </template>
        <template v-else>
          <div v-if="page.emergency" class="resource-alert">
            <strong>就医提醒</strong>
            <p>{{ page.emergency }}</p>
          </div>

          <section v-if="page.narrative?.length" class="resource-article-section">
            <article v-for="section in page.narrative" :key="section.title" class="resource-article">
              <h2>{{ section.title }}</h2>
              <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
            </article>
          </section>

          <section v-if="page.features?.length" id="重点模块" class="resource-feature-section">
            <div class="resource-section-head">
              <h2>重点模块</h2>
              <p>围绕患者真实使用场景，整理可直接进入的服务、资料和业务线索。</p>
            </div>
            <div class="resource-feature-grid">
              <article v-for="feature in page.features" :key="feature.title" class="resource-feature-card">
                <span>{{ feature.meta }}</span>
                <h3>{{ feature.title }}</h3>
                <p>{{ feature.text }}</p>
                <div>
                  <em v-for="tag in feature.tags" :key="tag">{{ tag }}</em>
                </div>
                <RouterLink v-if="feature.to" :to="previewRoute(feature.to)">进入</RouterLink>
              </article>
            </div>
          </section>

          <article v-for="block in page.blocks" :id="block.title" :key="block.title" class="resource-block">
            <div>
              <h2>{{ block.title }}</h2>
              <p>{{ block.summary }}</p>
            </div>
            <ul>
              <li v-for="detail in block.details" :key="detail">{{ detail }}</li>
            </ul>
            <p v-if="block.note" class="resource-note">{{ block.note }}</p>
            <div v-if="block.links?.length" class="resource-links">
              <RouterLink v-for="link in block.links" :key="link.label" :to="previewRoute(link.to)">{{ link.label }}</RouterLink>
            </div>
          </article>

          <section v-if="page.workflow?.length" id="服务流程" class="resource-workflow">
            <div class="resource-section-head">
              <h2>服务流程</h2>
              <p>把页面信息转化为可执行步骤，帮助患者从准备资料到完成服务。</p>
            </div>
            <ol>
              <li v-for="(step, index) in page.workflow" :key="step.title">
                <span>{{ index + 1 }}</span>
                <div>
                  <strong>{{ step.title }}</strong>
                  <p>{{ step.text }}</p>
                </div>
              </li>
            </ol>
          </section>

          <section v-if="page.faq?.length" id="常见问题" class="resource-faq">
            <div class="resource-section-head">
              <h2>常见问题</h2>
              <p>补充患者在预约、资料准备和诊后执行中最容易卡住的问题。</p>
            </div>
            <details v-for="item in page.faq" :key="item.question">
              <summary>{{ item.question }}</summary>
              <p>{{ item.answer }}</p>
            </details>
          </section>

          <section class="resource-related" aria-label="相关阅读">
            <h2>相关阅读</h2>
            <div>
              <RouterLink v-for="item in page.related" :key="item.label" :to="previewRoute(item.to)">
                <strong>{{ item.label }}</strong>
                <span>{{ item.description }}</span>
              </RouterLink>
            </div>
          </section>
        </template>
      </section>
    </div>
  </main>
</template>
