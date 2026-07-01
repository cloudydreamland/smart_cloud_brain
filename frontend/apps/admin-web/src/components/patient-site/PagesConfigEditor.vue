<script setup lang="ts">
import { computed, ref } from "vue";
import type { PatientSitePagesConfig, PatientSiteSection, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
import { ScbSelect } from "@smart-cloud-brain/shared-ui";
import { patientSiteFieldLabel } from "../../patientSitePresentation";
import PageSectionFieldsEditor from "./PageSectionFieldsEditor.vue";

type CmsPage = PatientSitePagesConfig["pages"][number];
type EditingPageState = { index: number; draft: CmsPage; isNew: boolean };
type EditingSectionState = { pageIndex: number; sectionIndex: number; draft: PatientSiteSection; isNew: boolean };

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
const editingPageState = ref<EditingPageState | null>(null);
const editingSectionState = ref<EditingSectionState | null>(null);

const editingPage = computed(() => editingPageState.value?.draft || null);
const editingSection = computed(() => editingSectionState.value?.draft || null);

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function startPageDrag(pageIndex: number) {
  draggedPageIndex.value = pageIndex;
}

function dropPage(toIndex: number) {
  if (draggedPageIndex.value === null) return;
  const fromIndex = draggedPageIndex.value;
  props.reorderCmsPage(draggedPageIndex.value, toIndex);
  if (editingPageState.value?.index === fromIndex) editingPageState.value.index = toIndex;
  if (editingSectionState.value?.pageIndex === fromIndex) {
    editingSectionState.value.pageIndex = toIndex;
  }
  draggedPageIndex.value = null;
}

function startSectionDrag(pageIndex: number, sectionIndex: number) {
  draggedSection.value = { pageIndex, sectionIndex };
}

function dropSection(pageIndex: number, toIndex: number) {
  if (!draggedSection.value || draggedSection.value.pageIndex !== pageIndex) return;
  props.reorderPageSection(pageIndex, draggedSection.value.sectionIndex, toIndex);
  if (
    editingSectionState.value?.pageIndex === pageIndex &&
    editingSectionState.value.sectionIndex === draggedSection.value.sectionIndex
  ) {
    editingSectionState.value.sectionIndex = toIndex;
  }
  draggedSection.value = null;
}

function sectionTypeLabel(type: PatientSiteSectionType) {
  return props.sectionTypeOptions.find((option) => option.type === type)?.label || type;
}

function routeLabel(routeName?: string) {
  return props.patientRouteOptions.find((option) => option.name === routeName)?.label || routeName || "-";
}

function pageMeta(page: CmsPage) {
  return [
    `页面标识：${page.slug || "未填写"}`,
    `页面入口：${routeLabel(page.routeName)}`,
    `${page.sections.length} 个区块`,
  ].join(" · ");
}

function sectionSummary(section: PatientSiteSection) {
  const sectionSummaryMap: Record<PatientSiteSectionType, () => string> = {
    notice: () => section.type === "notice" ? section.text || "未填写通知内容" : "",
    rich_text: () => section.type === "rich_text" ? `${section.body?.length || 0} 字正文` : "",
    card_grid: () => section.type === "card_grid" ? `${section.cards.length} 张卡片` : "",
    faq: () => section.type === "faq" ? `${section.items.length} 个问答` : "",
    timeline: () => section.type === "timeline" ? `${section.items.length} 条时间线` : "",
    cta: () => section.type === "cta" ? section.text || "未填写转化文案" : "",
    link_grid: () => section.type === "link_grid" ? `${section.links.length} 个链接` : "",
    department_links: () => section.type === "department_links" ? `${section.links.length} 个科室入口` : "",
    image_text: () => section.type === "image_text" ? section.text || "未填写图文正文" : "",
    hero: () => section.type === "hero" ? section.text || "未填写首屏说明" : "",
    gallery: () => section.type === "gallery" ? `${section.images.length} 张图片` : "",
    contact_panel: () => section.type === "contact_panel" ? section.text || "未填写联系说明" : "",
    stats: () => section.type === "stats" ? `${section.items.length} 个指标` : "",
    doctor_list: () => section.type === "doctor_list" ? `${section.links.length} 个医生入口` : "",
    department_list: () => section.type === "department_list" ? `${section.links.length} 个科室入口` : "",
  };
  return sectionSummaryMap[section.type]();
}

function addCmsPageAndOpen() {
  const nextIndex = props.pagesDraft.pages.length;
  props.addCmsPage();
  if (props.pagesDraft.pages.length > nextIndex) openPageEditor(nextIndex, true);
}

function removeCmsPageAndClose(pageIndex: number) {
  props.removeCmsPage(pageIndex);
  if (editingPageState.value?.index === pageIndex) editingPageState.value = null;
  else if (editingPageState.value && editingPageState.value.index > pageIndex) editingPageState.value.index -= 1;
  if (editingSectionState.value?.pageIndex === pageIndex) editingSectionState.value = null;
  else if (editingSectionState.value && editingSectionState.value.pageIndex > pageIndex) {
    editingSectionState.value.pageIndex -= 1;
  }
}

function addPageSectionAndOpen(pageIndex: number, type: PatientSiteSectionType) {
  const page = props.pagesDraft.pages[pageIndex];
  if (!page) return;
  const nextIndex = page.sections.length;
  props.addPageSection(pageIndex, type);
  if (page.sections.length > nextIndex) openSectionEditor(pageIndex, nextIndex, true);
}

function removePageSectionAndClose(pageIndex: number, sectionIndex: number) {
  props.removePageSection(pageIndex, sectionIndex);
  if (
    editingSectionState.value?.pageIndex === pageIndex &&
    editingSectionState.value.sectionIndex === sectionIndex
  ) {
    editingSectionState.value = null;
  } else if (
    editingSectionState.value?.pageIndex === pageIndex &&
    editingSectionState.value.sectionIndex > sectionIndex
  ) {
    editingSectionState.value.sectionIndex -= 1;
  }
}

function openPageEditor(pageIndex: number, isNew = false) {
  const page = props.pagesDraft.pages[pageIndex];
  if (!page) return;
  editingPageState.value = { index: pageIndex, draft: clone(page), isNew };
}

function cancelPageEditor() {
  const state = editingPageState.value;
  if (state?.isNew) props.pagesDraft.pages.splice(state.index, 1);
  editingPageState.value = null;
}

function savePageEditor() {
  const state = editingPageState.value;
  if (!state || !props.pagesDraft.pages[state.index]) return;
  props.pagesDraft.pages[state.index] = clone(state.draft);
  editingPageState.value = null;
}

function openSectionEditor(pageIndex: number, sectionIndex: number, isNew = false) {
  const section = props.pagesDraft.pages[pageIndex]?.sections[sectionIndex];
  if (!section) return;
  editingSectionState.value = { pageIndex, sectionIndex, draft: clone(section), isNew };
}

function cancelSectionEditor() {
  const state = editingSectionState.value;
  if (state?.isNew) {
    const sections = props.pagesDraft.pages[state.pageIndex]?.sections;
    sections?.splice(state.sectionIndex, 1);
  }
  editingSectionState.value = null;
}

function saveSectionEditor() {
  const state = editingSectionState.value;
  if (!state || !props.pagesDraft.pages[state.pageIndex]?.sections[state.sectionIndex]) return;
  props.pagesDraft.pages[state.pageIndex].sections[state.sectionIndex] = clone(state.draft);
  editingSectionState.value = null;
}

function removeSeo() {
  if (!editingPage.value?.seo || !window.confirm("确认移除 SEO 信息？移除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。")) return;
  editingPage.value.seo = undefined;
}
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>CMS 动态页</h3>
        <p>配置患者端动态内容页，用于导航入口、专题介绍和运营内容承载。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addCmsPageAndOpen">新增页面</button>
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
            <p>{{ pageMeta(page) }}</p>
            <small v-if="page.intro">{{ page.intro }}</small>
          </div>
          <div class="config-card-actions">
            <button type="button" class="status-pill" :class="page.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(page)">
              {{ page.enabled === false ? "禁用" : "启用" }}
            </button>
            <button type="button" class="topbar-refresh" @click="openPageEditor(pageIndex)">编辑</button>
            <button type="button" class="topbar-refresh" @click="previewCmsPage(page)">预览</button>
            <button type="button" class="danger-link" @click="removeCmsPageAndClose(pageIndex)">删除</button>
          </div>
        </div>

        <div class="cms-section-board">
          <div class="nested-list-head">
            <strong>页面区块</strong>
            <div class="section-add-buttons">
              <button v-for="option in sectionTypeOptions" :key="option.type" type="button" class="topbar-refresh" @click="addPageSectionAndOpen(pageIndex, option.type)">
                {{ option.label }}
              </button>
            </div>
          </div>

          <div class="cms-section-card-grid">
            <div
              v-for="(section, sectionIndex) in page.sections"
              :key="section.id"
              class="config-row-card page-section-card cms-section-summary-card"
              :class="{ dragging: draggedSection?.pageIndex === pageIndex && draggedSection.sectionIndex === sectionIndex }"
              draggable="true"
              @dragstart="startSectionDrag(pageIndex, sectionIndex)"
              @dragover.prevent
              @drop="dropSection(pageIndex, sectionIndex)"
              @dragend="draggedSection = null"
            >
              <div class="page-section-head">
                <div>
                  <strong>{{ section.title || sectionTypeLabel(section.type) }}</strong>
                  <p>{{ sectionTypeLabel(section.type) }} · {{ section.id || "未填写区块 ID" }} · 排序 {{ section.sort ?? "-" }}</p>
                </div>
                <span class="status-pill" :class="section.enabled === false ? 'disabled' : 'enabled'">
                  {{ section.enabled === false ? "禁用" : "启用" }}
                </span>
              </div>
              <small>{{ sectionSummary(section) }}</small>
              <div class="config-card-actions">
                <button type="button" class="topbar-refresh" @click="openSectionEditor(pageIndex, sectionIndex)">编辑</button>
                <button type="button" class="danger-link" @click="removePageSectionAndClose(pageIndex, sectionIndex)">删除区块</button>
              </div>
            </div>
            <div v-if="!page.sections.length" class="cms-empty-card">暂无区块，选择上方类型新增。</div>
          </div>
        </div>
      </article>
    </div>

    <div v-if="editingPage" class="patient-config-modal-backdrop" @click.self="cancelPageEditor">
      <section class="patient-config-modal-card cms-editor-modal-card" role="dialog" aria-modal="true" :aria-label="`编辑 ${editingPage.title || 'CMS 动态页'}`">
        <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="cancelPageEditor">×</button>
        <header class="patient-config-modal-head">
          <h2>编辑 CMS 动态页</h2>
          <p>维护页面入口、页面标识、标题、简介和 SEO 信息。区块内容在区块卡片中单独编辑。</p>
        </header>

        <div class="patient-config-modal">
          <div class="config-grid four">
            <label>
              <span>{{ patientSiteFieldLabel("routeName") }}</span>
              <ScbSelect v-model="editingPage.routeName" :options="routeSelectOptions" close-on-scroll />
            </label>
            <label><span>页面标识</span><input v-model.trim="editingPage.slug" type="text" placeholder="例如：hospital-guide"></label>
            <label><span>{{ patientSiteFieldLabel("sort") }}</span><input v-model.number="editingPage.sort" type="number"></label>
            <label class="check-field"><input v-model="editingPage.enabled" type="checkbox"><span>启用</span></label>
          </div>

          <div class="config-grid two">
            <label><span>{{ patientSiteFieldLabel("label") }}</span><input v-model.trim="editingPage.label" type="text"></label>
            <label><span>{{ patientSiteFieldLabel("title") }}</span><input v-model.trim="editingPage.title" type="text"></label>
            <label class="span-two"><span>页面简介</span><textarea v-model.trim="editingPage.intro" rows="4"></textarea></label>
          </div>

          <div class="nested-list">
            <div class="nested-list-head">
              <strong>SEO</strong>
              <button v-if="!editingPage.seo" type="button" class="topbar-refresh" @click="editingPage.seo = {}">添加 SEO</button>
              <button v-else type="button" class="danger-link" @click="removeSeo">移除 SEO</button>
            </div>
            <div v-if="editingPage.seo" class="config-grid two">
              <label><span>SEO 标题</span><input v-model.trim="editingPage.seo.title" type="text"></label>
              <label><span>SEO 描述</span><input v-model.trim="editingPage.seo.description" type="text"></label>
            </div>
          </div>
        </div>

        <div class="patient-config-modal-footer">
          <button type="button" class="topbar-refresh" @click="cancelPageEditor">取消</button>
          <button type="button" class="quick-btn" @click="savePageEditor">保存</button>
        </div>
      </section>
    </div>

    <div v-if="editingSection && editingSectionState" class="patient-config-modal-backdrop" @click.self="cancelSectionEditor">
      <section class="patient-config-modal-card cms-editor-modal-card wide" role="dialog" aria-modal="true" :aria-label="`编辑 ${sectionTypeLabel(editingSection.type)} 区块`">
        <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="cancelSectionEditor">×</button>
        <header class="patient-config-modal-head">
          <h2>编辑页面区块</h2>
          <p>{{ sectionTypeLabel(editingSection.type) }} · {{ editingSection.id || "未填写区块 ID" }}</p>
        </header>

        <div class="patient-config-modal">
          <PageSectionFieldsEditor :section="editingSection" :patient-route-options="patientRouteOptions" />
        </div>

        <div class="patient-config-modal-footer">
          <button
            type="button"
            class="danger-link"
            @click="removePageSectionAndClose(editingSectionState.pageIndex, editingSectionState.sectionIndex)"
          >
            删除区块
          </button>
          <button type="button" class="topbar-refresh" @click="cancelSectionEditor">取消</button>
          <button type="button" class="quick-btn" @click="saveSectionEditor">保存</button>
        </div>
      </section>
    </div>
  </section>
</template>
