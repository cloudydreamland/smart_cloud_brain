import type {
  PatientFooterConfig,
  PatientHomeConfig,
  PatientHospitalInfoConfig,
  PatientNavConfig,
  PatientSiteConfigKey,
  PatientSitePagesConfig,
  PatientStaticPagesConfig,
  RouteTargetConfig,
} from "@smart-cloud-brain/shared-api";
import { patientSiteSectionRegistry, patientSiteSectionTypes } from "@smart-cloud-brain/shared-api";
import { homeModuleTypeLabel } from "../patientSitePresentation";

export type ConfigKey = PatientSiteConfigKey;
export type ConfigTab = { key: ConfigKey; label: string; description: string };
export type EditingTarget =
  | { type: "brand" }
  | { type: "nav-menu"; index: number }
  | { type: "user-link"; index: number }
  | { type: "home-hero" }
  | { type: "home-module"; index: number }
  | { type: "static-page"; index: number };

export type ConfigDrafts = {
  patient_nav: PatientNavConfig;
  patient_home: PatientHomeConfig;
  patient_static_pages: PatientStaticPagesConfig;
  patient_pages: PatientSitePagesConfig;
  patient_hospital_info: PatientHospitalInfoConfig;
  patient_footer: PatientFooterConfig;
};

export const configTabs: ConfigTab[] = [
  { key: "patient_nav", label: "导航配置", description: "患者端顶部导航、下拉入口和登录后用户菜单。" },
  { key: "patient_home", label: "首页配置", description: "首页横幅、通知公告和快捷入口模块。" },
  { key: "patient_hospital_info", label: "医院信息", description: "患者端展示的医院名称、简介、联系方式和院区信息。" },
  { key: "patient_static_pages", label: "静态页配置", description: "按页面入口匹配的内容页标题、说明、要点和主按钮。" },
  { key: "patient_pages", label: "CMS 动态页", description: "配置患者端动态页面及页面区块内容。" },
  { key: "patient_footer", label: "页脚配置", description: "页脚品牌、联系方式、常用入口和法律链接。" },
];

const legacyHomeModuleTypeOptions = [
  { value: "notice", label: homeModuleTypeLabel("notice") },
  { value: "quick_actions", label: homeModuleTypeLabel("quick_actions") },
  { value: "intro", label: homeModuleTypeLabel("intro") },
  { value: "locations", label: homeModuleTypeLabel("locations") },
  { value: "featured_departments", label: homeModuleTypeLabel("featured_departments") },
  { value: "static_content", label: homeModuleTypeLabel("static_content") },
];

export const homeModuleTypeOptions = [
  ...legacyHomeModuleTypeOptions,
  ...patientSiteSectionTypes
    .filter((type) => !legacyHomeModuleTypeOptions.some((option) => option.value === type))
    .map((type) => ({ value: type, label: patientSiteSectionRegistry[type].label })),
];

const emptyAction = (): RouteTargetConfig => ({ label: "", routeName: "" });

export const emptyDrafts: ConfigDrafts = {
  patient_nav: { brand: { name: "", homeRoute: "" }, menus: [], userLinks: [] },
  patient_home: { hero: { enabled: false, eyebrow: "", title: "", primaryAction: emptyAction(), secondaryAction: emptyAction() }, modules: [] },
  patient_static_pages: { pages: [] },
  patient_pages: { pages: [] },
  patient_hospital_info: { name: "", intro: "", address: "", phone: "", workHours: "", website: "", locations: [] },
  patient_footer: { brandName: "", description: "", copyright: "", contactPhone: "", contactAddress: "", links: [], legalLinks: [] },
};
