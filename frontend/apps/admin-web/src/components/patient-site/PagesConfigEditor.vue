<script setup lang="ts">
import { computed, ref } from "vue";
import type { PatientSitePagesConfig, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import { ScbSelect } from "@smart-cloud-brain/shared-ui";
import { patientSiteFieldLabel } from "../../patientSitePresentation";
import PageSectionFieldsEditor from "./PageSectionFieldsEditor.vue";

const props = defineProps<{
  pagesDraft: PatientSitePagesConfig;
  patientRouteOptions: readonly { name: string; label: string }[];
  sectionTypeOptions: { type: PatientSiteSectionType; label: string }[];
  toggleEnabled: (item: { enabled?: boolean }) => void;
  addCmsPage: () => void;
  removeCmsPage: (index: number) => void;
  previewCmsPage: (page: PatientSitePagesConfig["pages"][number]) => void;
  addPageSection: (pageIndex: number, type: PatientSiteSectionType) => void;
  removePageSection: (pageIndex: number, sectionIndex: number) => void;
  reorderCmsPage: (fromIndex: number, toIndex: number) => void;
  reorderPageSection: (pageIndex: number, fromIndex: number, toIndex: number) => void;
}>();

const routeSelectOptions = computed(() =>
  props.patientRouteOptions.map((r) => ({ value: r.name, label: r.label }))
);

const draggedPageIndex = ref<number | null>(null);
const draggedSection = ref<{ pageIndex: number; sectionIndex: number } | null>(null);

function startPageDrag(pageIndex: number) {
  draggedPageIndex.value = pageIndex;
}

function dropPage(toIndex: number) {
  if (draggedPageIndex.value === null) return;
  props.reorderCmsPage(draggedPageIndex.value, toIndex);
  draggedPageIndex.value = null;
}

function startSectionDrag(pageIndex: number, sectionIndex: number) {
  draggedSection.value = { pageIndex, sectionIndex };
}

function dropSection(pageIndex: number, toIndex: number) {
  if (!draggedSection.value || draggedSection.value.pageIndex !== pageIndex) return;
  props.reorderPageSection(pageIndex, draggedSection.value.sectionIndex, toIndex);
  draggedSection.value = null;
}

function sectionTypeLabel(type: PatientSiteSectionType) {
  return props.sectionTypeOptions.find((option) => option.type === type)?.label || type;
}
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>CMS 动态页</h3>
        <p>配置患者端动态内容页，页面地址会生成在 /pages/ 下。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addCmsPage">新增页面</button>
    </div>

    <div class="config-card-grid cms-page-editor-grid">
      <article
        v-for="(page, pageIndex) in pagesDraft.pages"
        :key="`${page.slug || page.routeName}-${pageIndex}`"
        class="config-card page-editor-card"
        :class="{ dragging: draggedPageIndex === pageIndex }"
        draggable="true"
        @dragstart="startPageDrag(pageIndex)"
        @dragover.prevent
        @drop="dropPage(pageIndex)"
        @dragend="draggedPageIndex = null"
      >
        <div class="page-editor-head">
          <div>
            <strong>{{ page.title || "未命名 CMS 动态页" }}</strong>
            <p>{{ page.slug ? `页面地址：/pages/${page.slug}` : "未填写页面地址" }} · {{ page.sections.length }} 个区块</p>
          </div>
          <div class="config-card-actions">
            <button type="button" class="status-pill" :class="page.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(page)">
              {{ page.enabled === false ? "禁用" : "启用" }}
            </button>
            <button type="button" class="topbar-refresh" @click="previewCmsPage(page)">预览</button>
            <button type="button" class="danger-link" @click="removeCmsPage(pageIndex)">删除</button>
          </div>
        </div>

        <div class="config-grid four">
          <label>
            <span>{{ patientSiteFieldLabel("routeName") }}</span>
            <ScbSelect v-model="page.routeName" :options="routeSelectOptions" />
          </label>
          <label><span>{{ patientSiteFieldLabel("slug") }}</span><input v-model.trim="page.slug" type="text" placeholder="例如：hospital-guide"></label>
          <label><span>{{ patientSiteFieldLabel("sort") }}</span><input v-model.number="page.sort" type="number"></label>
          <label class="check-field"><input v-model="page.enabled" type="checkbox"><span>启用</span></label>
        </div>

        <div class="config-grid two">
          <label><span>{{ patientSiteFieldLabel("label") }}</span><input v-model.trim="page.label" type="text"></label>
          <label><span>{{ patientSiteFieldLabel("title") }}</span><input v-model.trim="page.title" type="text"></label>
          <label><span>页面简介</span><textarea v-model.trim="page.intro" rows="3"></textarea></label>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>SEO</strong>
              <button v-if="!page.seo" type="button" class="topbar-refresh" @click="page.seo = {}">添加 SEO</button>
              <button v-else type="button" class="danger-link" @click="page.seo = undefined">移除 SEO</button>
            </div>
            <div v-if="page.seo" class="config-grid two">
              <label><span>SEO 标题</span><input v-model.trim="page.seo.title" type="text"></label>
              <label><span>SEO 描述</span><input v-model.trim="page.seo.description" type="text"></label>
            </div>
          </div>
        </div>

        <div class="nested-list">
          <div class="nested-list-head">
            <strong>页面区块</strong>
            <div class="section-add-buttons">
              <button v-for="option in sectionTypeOptions" :key="option.type" type="button" class="topbar-refresh" @click="addPageSection(pageIndex, option.type)">
                {{ option.label }}
              </button>
            </div>
          </div>

          <div
            v-for="(section, sectionIndex) in page.sections"
            :key="section.id"
            class="config-row-card page-section-card"
            :class="{ dragging: draggedSection?.pageIndex === pageIndex && draggedSection.sectionIndex === sectionIndex }"
            draggable="true"
            @dragstart="startSectionDrag(pageIndex, sectionIndex)"
            @dragover.prevent
            @drop="dropSection(pageIndex, sectionIndex)"
            @dragend="draggedSection = null"
          >
            <div class="page-section-head">
              <strong>{{ sectionTypeLabel(section.type) }}</strong>
              <button type="button" class="danger-link" @click="removePageSection(pageIndex, sectionIndex)">删除区块</button>
            </div>
            <PageSectionFieldsEditor :section="section" :patient-route-options="patientRouteOptions" />
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
