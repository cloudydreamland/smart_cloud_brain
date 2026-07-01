import { isAllowedPatientRoute } from "./patientSiteRoutes";
import type {
  CardGridSection,
  ContactPanelSection,
  CtaSection,
  DepartmentLinksSection,
  DepartmentListSection,
  DoctorListSection,
  FaqSection,
  GallerySection,
  HeroSection,
  ImageTextSection,
  LinkGridSection,
  NoticeSection,
  PatientSiteCard,
  PatientSiteFaqItem,
  PatientSitePageConfig,
  PatientSitePagesConfig,
  PatientSiteSection,
  PatientSiteSectionType,
  PatientSiteStatItem,
  PatientSiteTimelineItem,
  RichTextSection,
  SectionRegistryItem,
  StatsSection,
  TimelineSection,
} from "./patientSitePageTypes";
import type { RouteTargetConfig } from "./patientSiteTypes";
import type { DataRow } from "./types";

export const patientSiteSectionRegistry: {
  [Type in PatientSiteSectionType]: SectionRegistryItem<Extract<PatientSiteSection, { type: Type }>>;
} = {
  notice: {
    type: "notice",
    label: "通知提示",
    description: "用于展示服务提醒、急诊边界或重要公告。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "level", label: "提示级别", kind: "select", options: ["info", "warning", "success"], required: true },
      { key: "text", label: "正文", kind: "textarea", required: true },
    ],
    createDefault: () => ({ id: sectionId("notice"), type: "notice", enabled: true, sort: 10, level: "info", text: "请填写提示内容。" }),
  },
  rich_text: {
    type: "rich_text",
    label: "富文本正文",
    description: "用于正文说明、患者教育和服务介绍。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "body", label: "正文", kind: "textarea", required: true },
    ],
    createDefault: () => ({ id: sectionId("rich_text"), type: "rich_text", enabled: true, sort: 20, title: "正文标题", body: "请填写正文内容。" }),
  },
  card_grid: {
    type: "card_grid",
    label: "卡片网格",
    description: "用于展示服务、资源或重点模块列表。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "cards", label: "卡片", kind: "card_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("card_grid"), type: "card_grid", enabled: true, sort: 30, title: "重点内容", cards: [] }),
  },
  faq: {
    type: "faq",
    label: "常见问题",
    description: "用于问答式解释流程、边界和注意事项。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "items", label: "问答", kind: "faq_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("faq"), type: "faq", enabled: true, sort: 40, title: "常见问题", items: [] }),
  },
  timeline: {
    type: "timeline",
    label: "时间线",
    description: "用于展示就诊流程、服务步骤或项目进展。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "items", label: "步骤", kind: "timeline_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("timeline"), type: "timeline", enabled: true, sort: 50, title: "服务流程", items: [] }),
  },
  cta: {
    type: "cta",
    label: "行动入口",
    description: "用于页面主按钮、次按钮和转化入口。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "text", label: "说明", kind: "textarea", required: true },
      { key: "primary", label: "主按钮", kind: "route_target" },
      { key: "secondary", label: "次按钮", kind: "route_target" },
    ],
    createDefault: () => ({ id: sectionId("cta"), type: "cta", enabled: true, sort: 60, title: "需要帮助？", text: "请选择下一步服务。" }),
  },
  link_grid: {
    type: "link_grid",
    label: "链接网格",
    description: "用于展示站内相关页面入口。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "links", label: "链接", kind: "route_target_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("link_grid"), type: "link_grid", enabled: true, sort: 70, title: "相关入口", links: [] }),
  },
  department_links: {
    type: "department_links",
    label: "科室入口",
    description: "用于展示科室链接和默认科室名。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "limit", label: "数量上限", kind: "number" },
      { key: "fallbackNames", label: "默认科室名", kind: "string_list" },
      { key: "links", label: "固定链接", kind: "route_target_list" },
    ],
    createDefault: () => ({ id: sectionId("department_links"), type: "department_links", enabled: true, sort: 80, title: "科室导航", limit: 12, fallbackNames: [], links: [] }),
  },
  image_text: {
    type: "image_text",
    label: "图文混排",
    description: "用于展示图片、说明文字和一个行动入口。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "text", label: "正文", kind: "textarea", required: true },
      { key: "image", label: "图片", kind: "image" },
      { key: "imagePosition", label: "图片位置", kind: "select", options: ["left", "right"], required: true },
      { key: "primary", label: "行动入口", kind: "route_target" },
    ],
    createDefault: () => ({ id: sectionId("image_text"), type: "image_text", enabled: true, sort: 90, title: "图文内容", text: "请填写图文说明。", imagePosition: "left" }),
  },
  hero: {
    type: "hero",
    label: "页面首屏",
    description: "用于 CMS 页面顶部的主标题、导语、图片和主按钮。",
    fields: [
      { key: "eyebrow", label: "眉题", kind: "text" },
      { key: "title", label: "标题", kind: "text" },
      { key: "text", label: "说明", kind: "textarea", required: true },
      { key: "image", label: "图片", kind: "image" },
      { key: "primary", label: "主按钮", kind: "route_target" },
      { key: "secondary", label: "次按钮", kind: "route_target" },
    ],
    createDefault: () => ({ id: sectionId("hero"), type: "hero", enabled: true, sort: 100, title: "页面标题", text: "请填写页面首屏说明。" }),
  },
  gallery: {
    type: "gallery",
    label: "图片组",
    description: "用于展示院区、环境、活动或服务图片。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "images", label: "图片", kind: "image_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("gallery"), type: "gallery", enabled: true, sort: 110, title: "图片展示", images: [] }),
  },
  contact_panel: {
    type: "contact_panel",
    label: "联系面板",
    description: "用于展示电话、地址、服务时间和联系入口。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "text", label: "说明", kind: "textarea", required: true },
      { key: "phone", label: "电话", kind: "text" },
      { key: "address", label: "地址", kind: "text" },
      { key: "workHours", label: "服务时间", kind: "text" },
      { key: "image", label: "图片", kind: "image" },
      { key: "primary", label: "行动入口", kind: "route_target" },
    ],
    createDefault: () => ({ id: sectionId("contact_panel"), type: "contact_panel", enabled: true, sort: 120, title: "联系方式", text: "请填写联系说明。" }),
  },
  stats: {
    type: "stats",
    label: "指标数字",
    description: "用于展示服务能力、运营数据或学科指标。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "items", label: "指标", kind: "stat_list", required: true },
    ],
    createDefault: () => ({ id: sectionId("stats"), type: "stats", enabled: true, sort: 130, title: "关键指标", items: [] }),
  },
  doctor_list: {
    type: "doctor_list",
    label: "医生列表",
    description: "用于配置推荐医生入口或医生姓名兜底展示。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "limit", label: "数量上限", kind: "number" },
      { key: "fallbackNames", label: "医生姓名", kind: "string_list" },
      { key: "links", label: "固定入口", kind: "route_target_list" },
    ],
    createDefault: () => ({ id: sectionId("doctor_list"), type: "doctor_list", enabled: true, sort: 140, title: "推荐医生", limit: 6, fallbackNames: [], links: [] }),
  },
  department_list: {
    type: "department_list",
    label: "科室列表",
    description: "用于配置推荐科室入口或科室名称兜底展示。",
    fields: [
      { key: "title", label: "标题", kind: "text" },
      { key: "limit", label: "数量上限", kind: "number" },
      { key: "fallbackNames", label: "科室名称", kind: "string_list" },
      { key: "links", label: "固定入口", kind: "route_target_list" },
    ],
    createDefault: () => ({ id: sectionId("department_list"), type: "department_list", enabled: true, sort: 150, title: "推荐科室", limit: 8, fallbackNames: [], links: [] }),
  },
};

export const patientSiteSectionTypes = Object.keys(patientSiteSectionRegistry) as PatientSiteSectionType[];

export function createDefaultPatientSiteSection(type: PatientSiteSectionType): PatientSiteSection {
  return patientSiteSectionRegistry[type].createDefault();
}

export function normalizePatientSitePagesConfig(source: unknown): PatientSitePagesConfig {
  const row = isRecord(source) ? source : {};
  return {
    pages: normalizeArray(row.pages, normalizePatientSitePage),
  };
}

export function normalizePatientSitePage(source: unknown, index = 0): PatientSitePageConfig | undefined {
  const row = isRecord(source) ? source : {};
  const routeName = text(row.routeName, "");
  const slug = slugValue(row.slug);
  const title = text(row.title, "");
  if (!routeName || !title) return undefined;
  return {
    routeName,
    slug,
    label: text(row.label, ""),
    title,
    intro: text(row.intro, ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
    seo: normalizeSeo(row.seo),
    sections: normalizeArray(row.sections, normalizePatientSiteSection),
  };
}

export function normalizePatientSiteSection(source: unknown, index = 0): PatientSiteSection | undefined {
  const row = isRecord(source) ? source : {};
  const type = text(row.type, "") as PatientSiteSectionType;
  if (!isPatientSiteSectionType(type)) return undefined;
  if (type === "notice") return normalizeNoticeSection(row, index);
  if (type === "rich_text") return normalizeRichTextSection(row, index);
  if (type === "card_grid") return normalizeCardGridSection(row, index);
  if (type === "faq") return normalizeFaqSection(row, index);
  if (type === "timeline") return normalizeTimelineSection(row, index);
  if (type === "cta") return normalizeCtaSection(row, index);
  if (type === "link_grid") return normalizeLinkGridSection(row, index);
  if (type === "department_links") return normalizeDepartmentLinksSection(row, index);
  if (type === "image_text") return normalizeImageTextSection(row, index);
  if (type === "hero") return normalizeHeroSection(row, index);
  if (type === "gallery") return normalizeGallerySection(row, index);
  if (type === "contact_panel") return normalizeContactPanelSection(row, index);
  if (type === "stats") return normalizeStatsSection(row, index);
  if (type === "doctor_list") return normalizeDoctorListSection(row, index);
  return normalizeDepartmentListSection(row, index);
}

export function validatePatientSitePagesConfig(config: PatientSitePagesConfig): string[] {
  return config.pages.flatMap((page, pageIndex) => validatePage(page, `pages[${pageIndex}]`));
}

export function isPatientSiteSectionType(type: string): type is PatientSiteSectionType {
  return Object.prototype.hasOwnProperty.call(patientSiteSectionRegistry, type);
}

function normalizeNoticeSection(row: DataRow, index: number): NoticeSection {
  const level = text(row.level, "info");
  return {
    ...baseSection(row, "notice", index),
    level: level === "warning" || level === "success" ? level : "info",
    text: text(row.text, ""),
  };
}

function normalizeRichTextSection(row: DataRow, index: number): RichTextSection {
  return { ...baseSection(row, "rich_text", index), body: text(row.body, "") };
}

function normalizeCardGridSection(row: DataRow, index: number): CardGridSection {
  return { ...baseSection(row, "card_grid", index), cards: normalizeArray(row.cards, normalizeCard) };
}

function normalizeFaqSection(row: DataRow, index: number): FaqSection {
  return { ...baseSection(row, "faq", index), items: normalizeArray(row.items, normalizeFaqItem) };
}

function normalizeTimelineSection(row: DataRow, index: number): TimelineSection {
  return { ...baseSection(row, "timeline", index), items: normalizeArray(row.items, normalizeTimelineItem) };
}

function normalizeCtaSection(row: DataRow, index: number): CtaSection {
  return {
    ...baseSection(row, "cta", index),
    text: text(row.text, ""),
    primary: normalizeRouteTarget(row.primary),
    secondary: normalizeRouteTarget(row.secondary),
  };
}

function normalizeLinkGridSection(row: DataRow, index: number): LinkGridSection {
  return { ...baseSection(row, "link_grid", index), links: normalizeArray(row.links, normalizeRouteTarget) };
}

function normalizeDepartmentLinksSection(row: DataRow, index: number): DepartmentLinksSection {
  return {
    ...baseSection(row, "department_links", index),
    limit: numberValue(row.limit, 12),
    fallbackNames: normalizeStringList(row.fallbackNames),
    links: normalizeArray(row.links, normalizeRouteTarget),
  };
}

function normalizeImageTextSection(row: DataRow, index: number): ImageTextSection {
  const imagePosition = text(row.imagePosition, "left");
  return {
    ...baseSection(row, "image_text", index),
    text: text(row.text, ""),
    image: normalizeImage(row.image),
    imagePosition: imagePosition === "right" ? "right" : "left",
    primary: normalizeRouteTarget(row.primary),
  };
}

function normalizeHeroSection(row: DataRow, index: number): HeroSection {
  return {
    ...baseSection(row, "hero", index),
    eyebrow: text(row.eyebrow, ""),
    text: text(row.text, ""),
    image: normalizeImage(row.image),
    primary: normalizeRouteTarget(row.primary),
    secondary: normalizeRouteTarget(row.secondary),
  };
}

function normalizeGallerySection(row: DataRow, index: number): GallerySection {
  return { ...baseSection(row, "gallery", index), images: normalizeArray(row.images, normalizeImage) };
}

function normalizeContactPanelSection(row: DataRow, index: number): ContactPanelSection {
  return {
    ...baseSection(row, "contact_panel", index),
    text: text(row.text, ""),
    phone: text(row.phone, ""),
    address: text(row.address, ""),
    workHours: text(row.workHours, ""),
    image: normalizeImage(row.image),
    primary: normalizeRouteTarget(row.primary),
  };
}

function normalizeStatsSection(row: DataRow, index: number): StatsSection {
  return { ...baseSection(row, "stats", index), items: normalizeArray(row.items, normalizeStatItem) };
}

function normalizeDoctorListSection(row: DataRow, index: number): DoctorListSection {
  return {
    ...baseSection(row, "doctor_list", index),
    limit: numberValue(row.limit, 6),
    fallbackNames: normalizeStringList(row.fallbackNames),
    links: normalizeArray(row.links, normalizeRouteTarget),
  };
}

function normalizeDepartmentListSection(row: DataRow, index: number): DepartmentListSection {
  return {
    ...baseSection(row, "department_list", index),
    limit: numberValue(row.limit, 8),
    fallbackNames: normalizeStringList(row.fallbackNames),
    links: normalizeArray(row.links, normalizeRouteTarget),
  };
}

function baseSection<Type extends PatientSiteSectionType>(row: DataRow, type: Type, index: number) {
  return {
    id: text(row.id, sectionId(type)),
    type,
    title: text(row.title, ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, index * 10),
  };
}

function normalizeCard(source: unknown): PatientSiteCard | undefined {
  const row = isRecord(source) ? source : {};
  const title = text(row.title, "");
  const cardText = text(row.text, "");
  if (!title || !cardText) return undefined;
  return {
    title,
    text: cardText,
    meta: text(row.meta, ""),
    image: normalizeImage(row.image),
    target: normalizeRouteTarget(row.target),
  };
}

function normalizeFaqItem(source: unknown): PatientSiteFaqItem | undefined {
  const row = isRecord(source) ? source : {};
  const question = text(row.question, "");
  const answer = text(row.answer, "");
  return question && answer ? { question, answer } : undefined;
}

function normalizeTimelineItem(source: unknown): PatientSiteTimelineItem | undefined {
  const row = isRecord(source) ? source : {};
  const title = text(row.title, "");
  const itemText = text(row.text, "");
  return title && itemText ? { title, text: itemText, time: text(row.time, "") } : undefined;
}

function normalizeStatItem(source: unknown): PatientSiteStatItem | undefined {
  const row = isRecord(source) ? source : {};
  const label = text(row.label, "");
  const value = text(row.value, "");
  return label && value ? { label, value, caption: text(row.caption, "") } : undefined;
}

function normalizeImage(source: unknown) {
  const row = isRecord(source) ? source : {};
  const url = text(row.url, "");
  const alt = text(row.alt, "");
  const objectKey = text(row.objectKey, "");
  return url || alt || objectKey ? { url, alt, objectKey: objectKey || undefined } : undefined;
}

function normalizeRouteTarget(source: unknown): RouteTargetConfig | undefined {
  const row = isRecord(source) ? source : {};
  const label = text(row.label, "");
  const routeName = text(row.routeName, "");
  if (!label || !routeName || !isAllowedPatientRoute(routeName)) return undefined;
  return {
    label,
    routeName,
    slug: slugValue(row.slug),
    query: isStringRecord(row.query) ? row.query : undefined,
    description: text(row.description, ""),
    enabled: row.enabled === false ? false : true,
    sort: numberValue(row.sort, 0),
  };
}

function normalizeSeo(source: unknown) {
  const row = isRecord(source) ? source : {};
  const title = text(row.title, "");
  const description = text(row.description, "");
  return title || description ? { title, description } : undefined;
}

function validatePage(page: PatientSitePageConfig, path: string) {
  const errors: string[] = [];
  requireText(page.routeName, `${path}.routeName`, errors);
  if (page.routeName && !isAllowedPatientRoute(page.routeName)) errors.push(`${path}.routeName 不在患者端路由白名单内：${page.routeName}`);
  if (page.slug && !/^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$/.test(page.slug)) errors.push(`${path}.slug 格式不合法：${page.slug}`);
  requireText(page.label, `${path}.label`, errors);
  requireText(page.title, `${path}.title`, errors);
  page.sections.forEach((section, index) => errors.push(...validateSection(section, `${path}.sections[${index}]`)));
  return errors;
}

function validateSection(section: PatientSiteSection, path: string) {
  const errors: string[] = [];
  if (!isPatientSiteSectionType(section.type)) errors.push(`${path}.type 不支持：${section.type}`);
  if (section.type === "notice") requireText(section.text, `${path}.text`, errors);
  if (section.type === "rich_text") requireText(section.body, `${path}.body`, errors);
  if (section.type === "card_grid" && !section.cards.length) errors.push(`${path}.cards 至少需要 1 项`);
  if (section.type === "faq" && !section.items.length) errors.push(`${path}.items 至少需要 1 项`);
  if (section.type === "timeline" && !section.items.length) errors.push(`${path}.items 至少需要 1 项`);
  if (section.type === "cta") requireText(section.text, `${path}.text`, errors);
  if (section.type === "cta" && section.primary) validateRouteTarget(section.primary, `${path}.primary`, errors);
  if (section.type === "cta" && section.secondary) validateRouteTarget(section.secondary, `${path}.secondary`, errors);
  if (section.type === "link_grid" && !section.links.length) errors.push(`${path}.links 至少需要 1 项`);
  if (section.type === "link_grid") section.links.forEach((link, index) => validateRouteTarget(link, `${path}.links[${index}]`, errors));
  if (section.type === "department_links") section.links.forEach((link, index) => validateRouteTarget(link, `${path}.links[${index}]`, errors));
  if (section.type === "image_text") requireText(section.text, `${path}.text`, errors);
  if (section.type === "image_text" && section.primary) validateRouteTarget(section.primary, `${path}.primary`, errors);
  if (section.type === "hero") requireText(section.text, `${path}.text`, errors);
  if (section.type === "hero" && section.primary) validateRouteTarget(section.primary, `${path}.primary`, errors);
  if (section.type === "hero" && section.secondary) validateRouteTarget(section.secondary, `${path}.secondary`, errors);
  if (section.type === "gallery" && !section.images.length) errors.push(`${path}.images 至少需要 1 项`);
  if (section.type === "contact_panel") requireText(section.text, `${path}.text`, errors);
  if (section.type === "contact_panel" && section.primary) validateRouteTarget(section.primary, `${path}.primary`, errors);
  if (section.type === "stats" && !section.items.length) errors.push(`${path}.items 至少需要 1 项`);
  if (section.type === "doctor_list") section.links.forEach((link, index) => validateRouteTarget(link, `${path}.links[${index}]`, errors));
  if (section.type === "department_list") section.links.forEach((link, index) => validateRouteTarget(link, `${path}.links[${index}]`, errors));
  return errors;
}

function validateRouteTarget(link: RouteTargetConfig, path: string, errors: string[]) {
  requireText(link.label, `${path}.label`, errors);
  requireText(link.routeName, `${path}.routeName`, errors);
  if (link.routeName && !isAllowedPatientRoute(link.routeName)) errors.push(`${path}.routeName 不在患者端路由白名单内：${link.routeName}`);
  if (link.routeName === "cms-page" && !link.slug?.trim()) errors.push(`${path}.slug 在 routeName 为 cms-page 时必填`);
}

function requireText(value: unknown, path: string, errors: string[]) {
  if (typeof value !== "string" || !value.trim()) errors.push(`${path} 不能为空`);
}

function sectionId(type: PatientSiteSectionType) {
  return `${type}-${Date.now()}`;
}

function normalizeArray<T>(source: unknown, normalize: (item: unknown, index: number) => T | undefined): T[] {
  if (!Array.isArray(source)) return [];
  return source
    .map((item, index) => normalize(item, index))
    .filter((item): item is T => Boolean(item))
    .sort((left, right) => numberValue(isRecord(left) ? left.sort : undefined, 0) - numberValue(isRecord(right) ? right.sort : undefined, 0));
}

function normalizeStringList(source: unknown) {
  return Array.isArray(source) ? source.map((item) => String(item || "").trim()).filter(Boolean) : [];
}

function slugValue(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim().toLowerCase() : undefined;
}

function text(value: unknown, fallback: string) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function isRecord(value: unknown): value is DataRow {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function isStringRecord(value: unknown): value is Record<string, string> {
  return isRecord(value) && Object.values(value).every((item) => typeof item === "string");
}
