<script setup lang="ts">
import { onMounted } from "vue";
import { formatDateTime } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfigEditor } from "../../composables/usePatientSiteConfigEditor";
import { patientSiteConfigStatusText } from "../../patientSitePresentation";
import NavConfigEditor from "./NavConfigEditor.vue";
import HomeConfigEditor from "./HomeConfigEditor.vue";
import PagesConfigEditor from "./PagesConfigEditor.vue";
import StaticPagesEditor from "./StaticPagesEditor.vue";
import PatientSiteEditorModal from "./PatientSiteEditorModal.vue";
import PatientSiteHistoryPanel from "./PatientSiteHistoryPanel.vue";

const editor = usePatientSiteConfigEditor();
const {
  tabs,
  activeKey,
  activeTab,
  activeRecord,
  activeErrors,
  histories,
  loading,
  saving,
  status,
  error,
  remarks,
  historyPages,
  historyLoading,
  navDraft,
  homeDraft,
  pagesDraft,
  staticSearch,
  staticDisabledOnly,
  filteredStaticPages,
  sectionTypeOptions,
  editorOpen,
  editingTarget,
  editingDraft,
  editorTitle,
  editorDescription,
  patientRouteOptions,
  homeModuleTypeOptions,
} = editor;

onMounted(editor.loadAll);
</script>

<template>
  <section class="patient-site-page">
    <div class="panel">
      <div class="panel-header patient-site-header">
        <div>
          <strong>患者端站点配置</strong>
        </div>
      </div>

      <div class="patient-site-tabs" role="tablist" aria-label="配置类型">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ active: activeKey === tab.key }" @click="editor.switchTab(tab.key)">
          {{ tab.label }}
        </button>
      </div>

      <div class="patient-site-editor">
        <aside>
          <h2>{{ activeTab.label }}</h2>
          <dl>
            <div>
              <dt>状态</dt>
              <dd>{{ activeRecord?.status ? patientSiteConfigStatusText(activeRecord.status) : "未保存" }}</dd>
            </div>
            <div>
              <dt>版本</dt>
              <dd>{{ activeRecord?.version || "-" }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatDateTime(activeRecord?.updatedAt) || "-" }}</dd>
            </div>
          </dl>
          <label>
            <span>本次备注</span>
            <input v-model.trim="remarks[activeKey]" type="text" placeholder="本次调整说明">
          </label>
          <div class="patient-site-side-actions">
            <button type="button" class="topbar-refresh" :disabled="loading" @click="editor.loadConfig()">重新加载</button>
            <button type="button" class="topbar-refresh" @click="editor.useTemplate">使用模板</button>
            <button type="button" class="quick-btn publish" :disabled="saving" @click="editor.saveAndApply">保存并生效</button>
          </div>
          <PatientSiteHistoryPanel
            :records="histories[activeKey]"
            :page-info="historyPages[activeKey]"
            :history-loading="historyLoading"
            :saving="saving"
            :load-history-page="editor.loadHistoryPage"
            :save-draft="editor.saveDraft"
            :publish-draft="editor.publishDraft"
            :rollback-to="editor.rollbackTo"
          />
          <div v-if="status" class="notice success">{{ status }}</div>
          <div v-if="error" class="notice error">{{ error }}</div>
          <div v-if="activeErrors.length" class="notice error">
            <strong>请先修正以下问题</strong>
            <ul>
              <li v-for="item in activeErrors" :key="item">{{ item }}</li>
            </ul>
          </div>
        </aside>

        <div class="patient-site-main">
          <NavConfigEditor
            v-if="activeKey === 'patient_nav'"
            :nav-draft="navDraft"
            :route-label="editor.routeLabel"
            :toggle-enabled="editor.toggleEnabled"
            :open-editor="editor.openEditor"
            :add-menu="editor.addMenu"
            :remove-menu="editor.removeMenu"
            :add-user-link="editor.addUserLink"
            :remove-user-link="editor.removeUserLink"
          />
          <HomeConfigEditor
            v-else-if="activeKey === 'patient_home'"
            :home-draft="homeDraft"
            :module-summary="editor.moduleSummary"
            :toggle-enabled="editor.toggleEnabled"
            :open-editor="editor.openEditor"
            :add-notice-module="editor.addNoticeModule"
            :add-quick-actions-module="editor.addQuickActionsModule"
            :add-intro-module="editor.addIntroModule"
            :add-locations-module="editor.addLocationsModule"
            :add-featured-departments-module="editor.addFeaturedDepartmentsModule"
            :add-static-content-module="editor.addStaticContentModule"
            :remove-home-module="editor.removeHomeModule"
          />
          <StaticPagesEditor
            v-else-if="activeKey === 'patient_static_pages'"
            v-model:search="staticSearch"
            v-model:disabled-only="staticDisabledOnly"
            :pages="filteredStaticPages"
            :toggle-enabled="editor.toggleEnabled"
            :open-editor="editor.openEditor"
            :add-static-page="editor.addStaticPage"
            :remove-static-page="editor.removeStaticPage"
          />
          <PagesConfigEditor
            v-else
            :pages-draft="pagesDraft"
            :patient-route-options="patientRouteOptions"
            :section-type-options="sectionTypeOptions"
            :toggle-enabled="editor.toggleEnabled"
            :add-cms-page="editor.addCmsPage"
            :remove-cms-page="editor.removeCmsPage"
            :preview-cms-page="editor.previewCmsPage"
            :add-page-section="editor.addPageSection"
            :remove-page-section="editor.removePageSection"
            :reorder-cms-page="editor.reorderCmsPage"
            :reorder-page-section="editor.reorderPageSection"
          />
        </div>
      </div>
    </div>

    <PatientSiteEditorModal
      v-if="editorOpen && editingTarget && editingDraft"
      :title="editorTitle"
      :description="editorDescription"
      :editing-target="editingTarget"
      :editing-draft="editingDraft"
      :patient-route-options="patientRouteOptions"
      :home-module-type-options="homeModuleTypeOptions"
      :close-editor="editor.closeEditor"
      :apply-editor="editor.applyEditor"
      :hydrate-editing-home-module-content="editor.hydrateEditingHomeModuleContent"
      :editing-content-items="editor.editingContentItems"
      :add-editing-link="editor.addEditingLink"
      :add-editing-quick-action="editor.addEditingQuickAction"
      :add-editing-point="editor.addEditingPoint"
      :add-editing-location-item="editor.addEditingLocationItem"
      :add-editing-department-link="editor.addEditingDepartmentLink"
      :add-editing-fallback-name="editor.addEditingFallbackName"
    />
  </section>
</template>
