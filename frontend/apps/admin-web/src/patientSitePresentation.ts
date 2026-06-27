export type PatientHomeModuleType =
  | "notice"
  | "quick_actions"
  | "intro"
  | "locations"
  | "featured_departments"
  | "static_content";

const homeModuleTypeLabels: Record<PatientHomeModuleType, string> = {
  notice: "通知公告",
  quick_actions: "快捷入口",
  intro: "服务介绍",
  locations: "院区位置",
  featured_departments: "重点科室",
  static_content: "图文内容",
};

const homeModuleKeyLabels: Record<string, string> = {
  emergency_notice: "紧急就医提示",
  quick_actions: "快捷入口",
  patient_service_intro: "患者服务介绍",
  hospital_locations: "院区位置",
  featured_departments: "重点科室",
  research_education: "科研教学介绍",
};

const configStatusLabels: Record<string, string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  ARCHIVED: "已归档",
};

const fieldLabels: Record<string, string> = {
  routeName: "页面入口",
  label: "显示名称",
  title: "页面标题",
  slug: "页面地址",
  sort: "排序值",
  key: "内部标识",
  type: "模块类型",
  enabled: "启用状态",
};

export function homeModuleTypeLabel(type?: string) {
  return (type && homeModuleTypeLabels[type as PatientHomeModuleType]) || (type ? "自定义模块" : "未设置模块");
}

export function homeModuleBusinessTitle(type?: string, key?: string) {
  const typeLabel = homeModuleTypeLabel(type);
  const keyLabel = key ? homeModuleKeyLabels[key] : "";
  return keyLabel && keyLabel !== typeLabel ? `${typeLabel} / ${keyLabel}` : typeLabel;
}

export function homeModuleInternalIdentifier(type?: string, key?: string) {
  return [type, key].filter(Boolean).join(" / ") || "-";
}

export function patientSiteConfigStatusText(status?: string) {
  return (status && configStatusLabels[status]) || status || "-";
}

export function patientSiteFieldLabel(field: string) {
  return fieldLabels[field] || field;
}
