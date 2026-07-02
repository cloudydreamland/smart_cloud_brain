<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ConfirmDialog } from "@smart-cloud-brain/shared-ui";
import { formatDateTime } from "@smart-cloud-brain/shared-api";
import type { PatientSiteConfigKey } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfigEditor } from "../../composables/usePatientSiteConfigEditor";
import { usePatientSiteConfirmHost } from "../../composables/patientSiteConfirm";
import { patientSiteConfigStatusText } from "../../patientSitePresentation";
import FooterConfigEditor from "./FooterConfigEditor.vue";
import NavConfigEditor from "./NavConfigEditor.vue";
import HomeConfigEditor from "./HomeConfigEditor.vue";
import HospitalInfoConfigEditor from "./HospitalInfoConfigEditor.vue";
import NoticeConfigEditor from "./NoticeConfigEditor.vue";
import PagesConfigEditor from "./PagesConfigEditor.vue";
import RecommendationConfigEditor from "./RecommendationConfigEditor.vue";
import StaticPagesEditor from "./StaticPagesEditor.vue";
import PatientSiteEditorModal from "./PatientSiteEditorModal.vue";
import PatientSiteHistoryPanel from "./PatientSiteHistoryPanel.vue";

const patientSiteConfirm = usePatientSiteConfirmHost();
const editor = usePatientSiteConfigEditor({ confirm: patientSiteConfirm.confirm });
const {
  tabs,
  activeKey,
  activeRecord,
  activeEditingSource,
  activeErrors,
  activeHistories,
  loading,
  saving,
  status,
  error,
  remarks,
  activeHistoryPage,
  historyLoading,
  navDraft,
  homeDraft,
  hospitalDraft,
  footerDraft,
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

type PanelKey = PatientSiteConfigKey | "patient_notices" | "patient_recommendations";

const activePanel = ref<PanelKey>("patient_nav");
const structuredTabs = [
  { key: "patient_notices" as const, label: "公告通知", description: "维护患者端公开公告、置顶、生效时间和启停状态。" },
  { key: "patient_recommendations" as const, label: "推荐内容", description: "维护热门科室和推荐医生。" },
];
const allTabs = computed(() => [...tabs, ...structuredTabs]);
const activePanelMeta = computed(() => allTabs.value.find((tab) => tab.key === activePanel.value) || allTabs.value[0]);
const isConfigPanel = computed(() => tabs.some((tab) => tab.key === activePanel.value));

function switchPanel(key: PanelKey) {
  activePanel.value = key;
  if (tabs.some((tab) => tab.key === key)) editor.switchTab(key as PatientSiteConfigKey);
}

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
        <button v-for="tab in allTabs" :key="tab.key" type="button" :class="{ active: activePanel === tab.key }" @click="switchPanel(tab.key)">
          {{ tab.label }}
        </button>
      </div>

      <div class="patient-site-editor">
        <aside>
          <h2>{{ activePanelMeta.label }}</h2>
          <p class="muted-hint">{{ activePanelMeta.description }}</p>
          <dl v-if="isConfigPanel">
            <div>
              <dt>状态</dt>
              <dd>{{ activeRecord?.status ? patientSiteConfigStatusText(activeRecord.status) : "未保存" }}</dd>
            </div>
            <div>
              <dt>编辑来源</dt>
              <dd>{{ activeEditingSource ? patientSiteConfigStatusText(activeEditingSource) : "未保存" }}</dd>
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
          <label v-if="isConfigPanel">
            <span>本次备注</span>
            <input v-model.trim="remarks[activeKey]" type="text" placeholder="本次调整说明">
          </label>
          <div v-if="isConfigPanel" class="patient-site-side-actions">
            <button type="button" class="topbar-refresh" :disabled="loading" @click="editor.loadConfig()">重新加载</button>
            <button type="button" class="topbar-refresh" :disabled="saving" @click="editor.loadPublishedConfig">载入发布版</button>
            <button type="button" class="topbar-refresh" @click="editor.useTemplate">使用模板</button>
            <button type="button" class="topbar-refresh" :disabled="saving" @click="editor.previewSite">预览整站</button>
            <button type="button" class="quick-btn publish" :disabled="saving" @click="editor.saveAndApply">保存并生效</button>
          </div>
          <PatientSiteHistoryPanel
            v-if="isConfigPanel"
            :records="activeHistories"
            :page-info="activeHistoryPage"
            :history-loading="historyLoading"
            :saving="saving"
            :load-history-page="editor.loadHistoryPage"
            :save-draft="editor.saveDraft"
            :publish-draft="editor.publishDraft"
            :rollback-to="editor.rollbackTo"
          />
          <div v-if="status" class="notice success">{{ status }}</div>
          <div v-if="error" class="notice error">{{ error }}</div>
          <div v-if="isConfigPanel && activeErrors.length" class="notice error">
            <strong>请先修正以下问题</strong>
            <ul>
              <li v-for="item in activeErrors" :key="item">{{ item }}</li>
            </ul>
          </div>
        </aside>

        <div class="patient-site-main">
          <NavConfigEditor
            v-if="activePanel === 'patient_nav'"
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
            v-else-if="activePanel === 'patient_home'"
            :home-draft="homeDraft"
            :section-type-options="sectionTypeOptions"
            :module-summary="editor.moduleSummary"
            :toggle-enabled="editor.toggleEnabled"
            :open-editor="editor.openEditor"
            :add-notice-module="editor.addNoticeModule"
            :add-quick-actions-module="editor.addQuickActionsModule"
            :add-intro-module="editor.addIntroModule"
            :add-locations-module="editor.addLocationsModule"
            :add-featured-departments-module="editor.addFeaturedDepartmentsModule"
            :add-static-content-module="editor.addStaticContentModule"
            :add-home-section-module="editor.addHomeSectionModule"
            :reorder-home-module="editor.reorderHomeModule"
            :remove-home-module="editor.removeHomeModule"
          />
          <HospitalInfoConfigEditor
            v-else-if="activePanel === 'patient_hospital_info'"
            :hospital-draft="hospitalDraft"
            :toggle-enabled="editor.toggleEnabled"
          />
          <StaticPagesEditor
            v-else-if="activePanel === 'patient_static_pages'"
            v-model:search="staticSearch"
            v-model:disabled-only="staticDisabledOnly"
            :pages="filteredStaticPages"
            :toggle-enabled="editor.toggleEnabled"
            :open-editor="editor.openEditor"
            :add-static-page="editor.addStaticPage"
            :remove-static-page="editor.removeStaticPage"
          />
          <PagesConfigEditor
            v-else-if="activePanel === 'patient_pages'"
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
          <FooterConfigEditor
            v-else-if="activePanel === 'patient_footer'"
            :footer-draft="footerDraft"
            :patient-route-options="patientRouteOptions"
            :toggle-enabled="editor.toggleEnabled"
          />
          <NoticeConfigEditor v-else-if="activePanel === 'patient_notices'" />
          <RecommendationConfigEditor v-else />
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
    <ConfirmDialog
      :open="patientSiteConfirm.dialog.open"
      :title="patientSiteConfirm.dialog.title"
      :message="patientSiteConfirm.dialog.message"
      :confirm-text="patientSiteConfirm.dialog.confirmText"
      :tone="patientSiteConfirm.dialog.tone"
      @close="patientSiteConfirm.closeDialog"
      @confirm="patientSiteConfirm.confirmDialog"
    />
  </section>
</template>
