<script setup lang="ts">
import { ref } from "vue";
import type { PatientSitePagesConfig, PatientSiteSectionType } from "@smart-cloud-brain/shared-api";
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
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>CMS pages</h3>
        <p>Build dynamic patient pages rendered at /pages/:slug.</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addCmsPage">Add page</button>
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
            <strong>{{ page.title || "Untitled CMS page" }}</strong>
            <p>{{ page.slug ? `/pages/${page.slug}` : "slug missing" }} · {{ page.sections.length }} sections</p>
          </div>
          <div class="config-card-actions">
            <button type="button" class="status-pill" :class="page.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(page)">
              {{ page.enabled === false ? "disabled" : "enabled" }}
            </button>
            <button type="button" class="topbar-refresh" @click="previewCmsPage(page)">Preview</button>
            <button type="button" class="danger-link" @click="removeCmsPage(pageIndex)">Delete</button>
          </div>
        </div>

        <div class="config-grid four">
          <label>
            <span>routeName</span>
            <select v-model="page.routeName">
              <option v-for="route in patientRouteOptions" :key="route.name" :value="route.name">{{ route.label }} / {{ route.name }}</option>
            </select>
          </label>
          <label><span>slug</span><input v-model.trim="page.slug" type="text" placeholder="hospital-guide"></label>
          <label><span>sort</span><input v-model.number="page.sort" type="number"></label>
          <label class="check-field"><input v-model="page.enabled" type="checkbox"><span>enabled</span></label>
        </div>

        <div class="config-grid two">
          <label><span>label</span><input v-model.trim="page.label" type="text"></label>
          <label><span>title</span><input v-model.trim="page.title" type="text"></label>
          <label><span>intro</span><textarea v-model.trim="page.intro" rows="3"></textarea></label>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>seo</strong>
              <button v-if="!page.seo" type="button" class="topbar-refresh" @click="page.seo = {}">Add SEO</button>
              <button v-else type="button" class="danger-link" @click="page.seo = undefined">Remove SEO</button>
            </div>
            <div v-if="page.seo" class="config-grid two">
              <label><span>seo.title</span><input v-model.trim="page.seo.title" type="text"></label>
              <label><span>seo.description</span><input v-model.trim="page.seo.description" type="text"></label>
            </div>
          </div>
        </div>

        <div class="nested-list">
          <div class="nested-list-head">
            <strong>sections</strong>
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
              <strong>{{ section.type }}</strong>
              <button type="button" class="danger-link" @click="removePageSection(pageIndex, sectionIndex)">Delete section</button>
            </div>
            <PageSectionFieldsEditor :section="section" :patient-route-options="patientRouteOptions" />
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
