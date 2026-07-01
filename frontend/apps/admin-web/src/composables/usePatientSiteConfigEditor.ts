import { computed, reactive, ref } from "vue";
import {
  adminApi,
  api,
  patientSiteConfigTemplates,
  patientSiteSectionRegistry,
  patientSiteSectionTypes,
  resolvePatientSiteConfigSection,
  type PatientHomeModule,
  type PatientSiteAdminConfigMap,
  type PatientSiteConfigHistoryPage,
  type PatientSiteConfigKey,
  type PatientSiteConfigRecord,
  type PatientSiteSectionType,
  useAuthStore,
} from "@smart-cloud-brain/shared-api";
import { patientRouteOptions } from "../patientSiteRoutes";
import {
  clone,
  editorText,
  emptyHistoryPage as createEmptyHistoryPage,
  filterStaticPages,
  loadHistory,
  loadMessageFrom,
  messageFrom,
  moveItem,
  moduleSummary,
  nextSort,
  resequenceSort,
  sanitizeBeforeSubmit,
  routeLabel,
  validateConfig,
} from "./patientSiteConfigEditorUtils";
import { createPatientSiteDraftActions } from "./patientSiteConfigDraftActions";
import {
  configTabs,
  emptyDrafts,
  homeModuleTypeOptions,
  type ConfigDrafts,
  type ConfigKey,
  type ConfigTab,
  type EditingTarget,
} from "./patientSiteConfigEditorState";
import type { PatientSiteEditorDraft } from "./patientSiteEditorDraftTypes";

type EditorDraft = PatientSiteEditorDraft | null;
export { configTabs, homeModuleTypeOptions };
export type { ConfigTab, EditingTarget };

const configKeys = new Set<ConfigKey>(configTabs.map((tab) => tab.key));
const historyPageSize = 10;
const emptyHistoryPage = () => createEmptyHistoryPage(historyPageSize);
const allowedHomeModules = new Set(homeModuleTypeOptions.map((item) => item.value));
const templates = clone(patientSiteConfigTemplates) as ConfigDrafts;

export function usePatientSiteConfigEditor() {
  const auth = useAuthStore();
  const activeKey = ref<ConfigKey>("patient_nav");
  const loading = ref(false);
  const saving = ref(false);
  const status = ref("");
  const error = ref("");
  const editorOpen = ref(false);
  const editingTarget = ref<EditingTarget | null>(null);
  const editingDraft = ref<EditorDraft>(null);
  const staticSearch = ref("");
  const staticDisabledOnly = ref(false);
  const drafts = reactive<ConfigDrafts>(clone(emptyDrafts));
  const latest = reactive<Record<ConfigKey, PatientSiteConfigRecord | null>>({ patient_nav: null, patient_home: null, patient_static_pages: null, patient_pages: null, patient_hospital_info: null, patient_footer: null });
  const histories = reactive<Record<ConfigKey, PatientSiteConfigRecord[]>>({ patient_nav: [], patient_home: [], patient_static_pages: [], patient_pages: [], patient_hospital_info: [], patient_footer: [] });
  const historyLoaded = reactive<Record<ConfigKey, boolean>>({ patient_nav: false, patient_home: false, patient_static_pages: false, patient_pages: false, patient_hospital_info: false, patient_footer: false });
  const editingSources = reactive<Record<ConfigKey, string>>({ patient_nav: "", patient_home: "", patient_static_pages: "", patient_pages: "", patient_hospital_info: "", patient_footer: "" });
  const historyPages = reactive<Record<ConfigKey, PatientSiteConfigHistoryPage>>({
    patient_nav: emptyHistoryPage(),
    patient_home: emptyHistoryPage(),
    patient_static_pages: emptyHistoryPage(),
    patient_pages: emptyHistoryPage(),
    patient_hospital_info: emptyHistoryPage(),
    patient_footer: emptyHistoryPage(),
  });
  const historyLoading = ref(false);
  const remarks = reactive<Record<ConfigKey, string>>({ patient_nav: "", patient_home: "", patient_static_pages: "", patient_pages: "", patient_hospital_info: "", patient_footer: "" });
  const validationErrors = reactive<Record<ConfigKey, string[]>>({ patient_nav: [], patient_home: [], patient_static_pages: [], patient_pages: [], patient_hospital_info: [], patient_footer: [] });

  const activeTab = computed(() => configTabs.find((tab) => tab.key === activeKey.value) || configTabs[0]);
  const activeRecord = computed(() => latest[activeKey.value]);
  const activeEditingSource = computed(() => editingSources[activeKey.value]);
  const activeErrors = computed(() => validationErrors[activeKey.value]);
  const activeHistories = computed(() => histories[activeKey.value] || []);
  const activeHistoryPage = computed(() => historyPages[activeKey.value]);
  const navDraft = computed(() => drafts.patient_nav);
  const homeDraft = computed(() => drafts.patient_home);
  const staticDraft = computed(() => drafts.patient_static_pages);
  const pagesDraft = computed(() => drafts.patient_pages);
  const hospitalDraft = computed(() => drafts.patient_hospital_info);
  const footerDraft = computed(() => drafts.patient_footer);
  const filteredStaticPages = computed(() => filterStaticPages(staticDraft.value.pages, staticSearch.value, staticDisabledOnly.value));
  const sectionTypeOptions = computed(() => patientSiteSectionTypes.map((type) => ({ type, label: patientSiteSectionRegistry[type].label })));
  const editorTitle = computed(() => editorText(editingTarget.value, "title"));
  const editorDescription = computed(() => editorText(editingTarget.value, "description"));
  const draftActions = createPatientSiteDraftActions({
    auth,
    drafts,
    remarks,
    saving,
    status,
    error,
    editingDraft,
    navDraft,
    homeDraft,
    staticDraft,
    pagesDraft,
    validationErrors,
    openEditor,
    refreshHistory: (key) => refreshHistory(key),
  });

  function setDraft(key: ConfigKey, value: unknown) {
    drafts[key] = resolvePatientSiteConfigSection(key, value, { preserveDisabled: true }) as never;
  }

  function configJsonFrom(record: PatientSiteConfigRecord | null) {
    if (!record?.configJson) return {};
    try {
      return JSON.parse(record.configJson) as unknown;
    } catch {
      return {};
    }
  }

  function isConfigRecord(value: unknown): value is PatientSiteConfigRecord {
    return Boolean(value && typeof value === "object" && !Array.isArray(value) && "configJson" in value);
  }

  function applyRecord(key: ConfigKey, record: PatientSiteConfigRecord | null) {
    latest[key] = record;
    setDraft(key, configJsonFrom(record));
    remarks[key] = typeof record?.remark === "string" ? record.remark : "";
    editingSources[key] = record?.status || "";
  }

  async function loadConfig(key = activeKey.value) {
    if (!auth.session) return;
    loading.value = true;
    error.value = "";
    status.value = "";
    validationErrors[key] = [];
    try {
      const record = await adminApi.patientSiteConfig(key);
      applyRecord(key, isConfigRecord(record) ? record : null);
      await refreshHistory(key, 1);
      status.value = record?.status === "DRAFT" ? "已加载草稿内容" : "已加载当前发布版本";
    } catch (err) {
      latest[key] = null;
      histories[key] = [];
      historyPages[key] = emptyHistoryPage();
      historyLoaded[key] = false;
      editingSources[key] = "";
      setDraft(key, {});
      remarks[key] = "";
      error.value = loadMessageFrom(err);
    } finally {
      loading.value = false;
    }
  }

  async function loadAll() {
    if (!auth.session) return;
    loading.value = true;
    error.value = "";
    status.value = "";
    try {
      const overview = await adminApi.patientSiteConfig() as PatientSiteAdminConfigMap;
      configTabs.forEach((tab) => {
        validationErrors[tab.key] = [];
        const record = overview[tab.key];
        applyRecord(tab.key, isConfigRecord(record) ? record : null);
      });
      activeKey.value = configTabs[0].key;
      await ensureHistoryLoaded(activeKey.value);
      status.value = "已加载管理端配置总览，草稿优先用于编辑";
    } catch (err) {
      error.value = loadMessageFrom(err);
    } finally {
      loading.value = false;
    }
  }

  function switchTab(key: ConfigKey) {
    activeKey.value = key;
    error.value = "";
    status.value = "";
    if (!latest[key]) {
      void loadConfig(key);
      return;
    }
    void ensureHistoryLoaded(key);
  }

  async function loadPublishedConfig() {
    if (!auth.session) return;
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      await refreshHistory(activeKey.value, 1);
      const published = histories[activeKey.value].find((row) => row.status === "PUBLISHED") || null;
      if (!published) {
        error.value = "未找到当前发布版本，无法重新载入。";
        return;
      }
      setDraft(activeKey.value, configJsonFrom(published));
      remarks[activeKey.value] = typeof published.remark === "string" ? published.remark : "";
      editingSources[activeKey.value] = "PUBLISHED";
      status.value = "已重新载入当前发布版本，保存草稿或发布前不会影响患者端";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  function useTemplate() {
    if (!window.confirm("这会将当前编辑内容改为完整默认模板；保存并生效后会写入数据库成为当前真实配置。是否继续？")) return;
    setDraft(activeKey.value, templates[activeKey.value]);
    status.value = "已填入默认模板；保存草稿不会影响患者端，保存并生效或发布后才会更新正式页面";
    error.value = "";
    validationErrors[activeKey.value] = [];
  }

  function openEditor(target: EditingTarget) {
    editingTarget.value = target;
    editingDraft.value = clone(draftActions.readEditingTarget(target) || {}) as PatientSiteEditorDraft;
    draftActions.hydrateEditingDraft(target);
    editorOpen.value = true;
  }

  function closeEditor() {
    editorOpen.value = false;
    editingTarget.value = null;
    editingDraft.value = null;
  }

  function applyEditor() {
    if (!editingTarget.value) return;
    draftActions.writeEditingTarget(editingTarget.value, editingDraft.value);
    status.value = "已更新当前编辑内容，保存后会直接生效";
    error.value = "";
    closeEditor();
  }

  function reorderHomeModule(fromIndex: number, toIndex: number) {
    moveItem(drafts.patient_home.modules, fromIndex, toIndex);
    resequenceSort(drafts.patient_home.modules);
    status.value = "已调整首页模块顺序，保存草稿或发布后生效";
    error.value = "";
  }

  function confirmRemove(message: string) {
    return window.confirm(`${message}。删除后会从当前编辑稿中禁用该项，保存草稿不会影响患者端，发布或保存并生效后才会更新正式页面。是否继续？`);
  }

  function removeMenu(index: number) {
    const menu = navDraft.value.menus[index];
    if (!menu || !confirmRemove(`确认删除导航菜单「${menu.label || "未命名菜单"}」`)) return;
    menu.enabled = false;
    status.value = "已删除导航菜单，保存草稿或发布后生效";
    error.value = "";
  }

  function removeUserLink(index: number) {
    const link = navDraft.value.userLinks[index];
    if (!link || !confirmRemove(`确认删除用户入口「${link.label || "未命名入口"}」`)) return;
    link.enabled = false;
    status.value = "已删除用户入口，保存草稿或发布后生效";
    error.value = "";
  }

  function removeHomeModule(module: PatientHomeModule) {
    if (!confirmRemove("确认删除首页模块")) return;
    module.enabled = false;
    status.value = "已删除首页模块，保存草稿或发布后生效";
    error.value = "";
  }

  function removeStaticPage(index: number) {
    const page = staticDraft.value.pages[index];
    if (!page || !confirmRemove(`确认删除静态页「${page.title || page.label || page.routeName}」`)) return;
    page.enabled = false;
    status.value = "已删除静态页，保存草稿或发布后生效";
    error.value = "";
  }

  async function saveAndApply() {
    if (!auth.session) return;
    const payload = preparePayload(activeKey.value);
    if (!payload) return;
    const remark = publishRemark();
    if (!remark) return;
    if (!confirmPublish("保存并生效", remark, payload.configJson)) return;
    saving.value = true;
    status.value = "";
    try {
      const row = await api.savePublishedPatientSiteConfig({
        configKey: activeKey.value,
        configJson: payload.configJson,
        remark,
      });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      editingSources[activeKey.value] = row.status || "PUBLISHED";
      if (typeof row.configJson === "string") setDraft(activeKey.value, JSON.parse(row.configJson));
      status.value = "配置已保存并生效；患者端刷新后会读取最新内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function saveDraft() {
    if (!auth.session) return;
    const payload = preparePayload(activeKey.value);
    if (!payload) return;
    saving.value = true;
    status.value = "";
    try {
      const row = await api.savePatientSiteConfig({
        configKey: activeKey.value,
        configJson: payload.configJson,
        remark: remarks[activeKey.value],
      });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      editingSources[activeKey.value] = row.status || "DRAFT";
      status.value = "草稿已保存，患者端不会读取；发布后才会生效";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function publishDraft() {
    if (!auth.session) return;
    const remark = publishRemark();
    if (!remark) return;
    if (!confirmPublish("发布最新草稿", remark, latest[activeKey.value]?.configJson || "")) return;
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      const row = await api.publishPatientSiteConfig({ configKey: activeKey.value, remark });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      editingSources[activeKey.value] = row.status || "PUBLISHED";
      if (typeof row.configJson === "string") setDraft(activeKey.value, JSON.parse(row.configJson));
      status.value = "最新草稿已发布，患者端刷新后会读取最新内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function rollbackTo(record: PatientSiteConfigRecord) {
    if (!auth.session || typeof record.configJson !== "string") return;
    if (!window.confirm(`确认回滚到版本 ${record.version || "-"}？这会生成一个新的已发布版本。`)) return;
    saving.value = true;
    status.value = "";
    error.value = "";
    try {
      const row = await api.savePublishedPatientSiteConfig({
        configKey: activeKey.value,
        configJson: record.configJson,
        remark: `回滚到版本 ${record.version || "-"}`,
      });
      await refreshHistory(activeKey.value);
      latest[activeKey.value] = row;
      editingSources[activeKey.value] = row.status || "PUBLISHED";
      setDraft(activeKey.value, JSON.parse(record.configJson));
      status.value = "已回滚并发布为新版本；患者端刷新后会读取回滚内容";
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  function preparePayload(key: ConfigKey) {
    error.value = "";
    validationErrors[key] = [];
    if (!configKeys.has(key)) {
      validationErrors[key] = [`configKey 不合法：${key}`];
      return null;
    }
    const normalized = resolvePatientSiteConfigSection(key, clone(drafts[key]), { preserveDisabled: true });
    sanitizeBeforeSubmit(key, normalized);
    const errors = validateConfig(key, normalized, allowedHomeModules);
    if (errors.length) {
      validationErrors[key] = errors;
      return null;
    }
    drafts[key] = normalized as never;
    return { configJson: JSON.stringify(normalized), data: normalized };
  }

  function publishRemark() {
    const remark = remarks[activeKey.value].trim();
    if (!remark) {
      validationErrors[activeKey.value] = ["发布前必须填写本次备注"];
      error.value = "发布前必须填写本次备注";
      return "";
    }
    return remark;
  }

  function confirmPublish(action: string, remark: string, nextConfigJson: string) {
    const label = activeTab.value?.label || activeKey.value;
    return window.confirm(`${action}「${label}」配置？这会更新患者端正式公开页面，影响模块：${label}。本次备注：${remark}\n\n变更摘要：${publishChangeSummary(nextConfigJson)}`);
  }

  function publishChangeSummary(nextConfigJson: string) {
    const published = histories[activeKey.value].find((row) => row.status === "PUBLISHED");
    const beforeJson = published?.configJson || activeRecord.value?.configJson || "";
    const before = flattenForSummary(parseJsonForSummary(beforeJson));
    const after = flattenForSummary(parseJsonForSummary(nextConfigJson));
    const paths = Array.from(new Set([...Object.keys(before), ...Object.keys(after)]))
      .sort()
      .filter((path) => before[path] !== after[path]);
    if (!paths.length) return "未检测到字段差异。";
    const preview = paths.slice(0, 6).join("、");
    return `${paths.length} 处字段变化：${preview}${paths.length > 6 ? " 等" : ""}`;
  }

  function parseJsonForSummary(json: string) {
    if (!json) return null;
    try {
      return JSON.parse(json) as unknown;
    } catch {
      return null;
    }
  }

  function flattenForSummary(value: unknown, path = "$", output: Record<string, string> = {}) {
    if (Array.isArray(value)) {
      if (!value.length) output[path] = "[]";
      value.forEach((item, index) => flattenForSummary(item, `${path}[${index}]`, output));
      return output;
    }
    if (value && typeof value === "object") {
      const entries = Object.entries(value);
      if (!entries.length) output[path] = "{}";
      entries.forEach(([key, child]) => flattenForSummary(child, `${path}.${key}`, output));
      return output;
    }
    output[path] = value === undefined ? "" : JSON.stringify(value);
    return output;
  }

  async function refreshHistory(key: ConfigKey, page = historyPages[key].page, syncLatest = false) {
    historyLoading.value = true;
    try {
      const result = await loadHistory(key, page, historyPageSize);
      histories[key] = result.items ?? [];
      historyPages[key] = result;
      historyLoaded[key] = true;
      if (syncLatest) {
        latest[key] = (result.items ?? []).find((row) => row.status === "PUBLISHED") || latest[key] || null;
      }
    } finally {
      historyLoading.value = false;
    }
  }

  async function loadHistoryPage(page: number) {
    if (!auth.session) return;
    await refreshHistory(activeKey.value, page);
  }

  async function ensureHistoryLoaded(key: PatientSiteConfigKey) {
    if (historyLoaded[key]) return;
    await refreshHistory(key, 1);
  }

  return {
    tabs: configTabs,
    activeKey,
    activeTab,
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
    staticDraft,
    pagesDraft,
    hospitalDraft,
    footerDraft,
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
    loadAll,
    loadConfig,
    loadPublishedConfig,
    loadHistoryPage,
    switchTab,
    useTemplate,
    openEditor,
    closeEditor,
    applyEditor,
    saveAndApply,
    saveDraft,
    publishDraft,
    rollbackTo,
    routeLabel,
    moduleSummary,
    hydrateEditingHomeModuleContent: draftActions.hydrateEditingHomeModuleContent,
    editingContentItems: draftActions.editingContentItems,
    addEditingLink: () => draftActions.editingArray("links").push({ label: "新链接", routeName: "patient-home", description: "", enabled: true, sort: draftActions.editingArray("links").length * 10 + 10 }),
    addEditingQuickAction: () => draftActions.editingContentItems().push({ label: "新快捷入口", routeName: "patient-home", enabled: true, sort: nextSort(draftActions.editingContentItems()) }),
    addEditingPoint: () => draftActions.editingArray("points").push({ title: "新要点", text: "" }),
    addEditingLocationItem: () => draftActions.ensureObjectArray("items").push({ title: "新院区", meta: "", imageUrl: "", alt: "", imageObjectKey: "" }),
    addEditingDepartmentLink: () => draftActions.ensureObjectArray("items").push({ label: "新诊疗领域", routeName: "public-search", enabled: true, sort: nextSort(draftActions.ensureObjectArray("items")) }),
    addEditingFallbackName: () => draftActions.ensureStringArray("fallbackNames").push("新科室"),
    addMenu: draftActions.addMenu,
    removeMenu,
    addUserLink: draftActions.addUserLink,
    removeUserLink,
    addNoticeModule: () => draftActions.addHomeModule("notice", "notice", { level: "info", text: "" }),
    addQuickActionsModule: () => draftActions.addHomeModule("quick_actions", "quick-actions", { items: [] }),
    addIntroModule: () => draftActions.addHomeModule("intro", "intro"),
    addLocationsModule: () => draftActions.addHomeModule("locations", "locations"),
    addFeaturedDepartmentsModule: () => draftActions.addHomeModule("featured_departments", "featured-departments", { limit: 12 }),
    addStaticContentModule: () => draftActions.addHomeModule("static_content", "static-content"),
    addHomeSectionModule: (type: PatientSiteSectionType) => draftActions.addHomeModule(type, type.replace(/_/g, "-")),
    reorderHomeModule,
    removeHomeModule,
    addStaticPage: draftActions.addStaticPage,
    removeStaticPage,
    addCmsPage: draftActions.addCmsPage,
    removeCmsPage: draftActions.removeCmsPage,
    addPageSection: draftActions.addPageSection,
    removePageSection: draftActions.removePageSection,
    reorderCmsPage: draftActions.reorderCmsPage,
    reorderPageSection: draftActions.reorderPageSection,
    previewSite: draftActions.previewSite,
    previewCmsPage: draftActions.previewCmsPage,
    toggleEnabled: (item: { enabled?: boolean }) => {
      item.enabled = item.enabled === false;
      status.value = item.enabled === false ? "已切换为禁用，保存后会直接生效" : "已切换为启用，保存后会直接生效";
      error.value = "";
    },
  };
}
