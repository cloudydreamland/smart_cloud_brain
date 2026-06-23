<script setup lang="ts">
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

type RouteTarget = string | { name: string; query?: Record<string, string> };

type NavLink = {
  label: string;
  to: RouteTarget;
  description?: string;
};

type NavMenu = {
  key: string;
  label: string;
  lead: string;
  description: string;
  links: NavLink[];
  feature: NavLink;
};

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const { session } = storeToRefs(auth);
const activeMenu = ref<string | null>(null);
const mobileOpen = ref(false);
const userOpen = ref(false);

auth.load("patient-session", "PATIENT");

const isSignedIn = computed(() => Boolean(session.value && !auth.permissionError));
const userName = computed(() => session.value?.name || "患者");

const homeMenu: NavMenu = {
  key: "home",
  label: "首页",
  lead: "智慧云脑医院首页",
  description: "从官方医院入口开始，快速进入预约、医生、科室、健康资料和患者服务。",
  links: [
    { label: "医院首页", to: { name: "patient-home" }, description: "返回智慧云脑首页" },
    { label: "在线挂号", to: { name: "patient-doctors" }, description: "选择医生和号源" },
    { label: "查找医生", to: { name: "public-search", query: { q: "医生" } }, description: "按姓名、科室和专长检索" },
    { label: "健康资料库", to: { name: "public-conditions" }, description: "疾病、症状和检查资料" },
  ],
  feature: { label: "开始预约就诊", to: { name: "patient-doctors" } },
};

const navMenus: NavMenu[] = [
  homeMenu,
  {
    key: "care",
    label: "智慧云脑医疗服务",
    lead: "从需要帮助到获得照护",
    description: "把分诊、挂号、医生科室、检查预约、住院和急诊指引放在同一医院官网体系中。",
    links: [
      { label: "在线挂号", to: { name: "patient-doctors" }, description: "查看可预约号源" },
      { label: "AI智能分诊", to: { name: "patient-triage" }, description: "描述症状并获得科室建议" },
      { label: "查找医生", to: { name: "public-search", query: { q: "医生" } }, description: "检索医生和专长" },
      { label: "查找科室", to: { name: "public-departments" }, description: "浏览科室导航" },
      { label: "互联网门诊", to: { name: "service-internet-clinic" }, description: "了解线上复诊与咨询" },
      { label: "检查检验预约", to: { name: "service-exam-booking" }, description: "检查前准备与预约说明" },
      { label: "住院服务", to: { name: "service-inpatient" }, description: "入院前后流程" },
      { label: "急诊指南", to: { name: "service-emergency" }, description: "急症识别与到院指引" },
      { label: "国际医疗", to: { name: "service-international" }, description: "多语种与跨境患者服务" },
    ],
    feature: { label: "查看医疗服务", to: { name: "service-internet-clinic" } },
  },
  {
    key: "patient",
    label: "患者服务",
    lead: "登录后仍在官网内完成服务",
    description: "个人预约、病历、处方、报告和账户资料保留医院门户质感，与官网体验保持一致。",
    links: [
      { label: "我的预约", to: { name: "patient-appointments" }, description: "查看和取消挂号" },
      { label: "我的病历", to: { name: "patient-records" }, description: "查看医生保存的病历" },
      { label: "我的处方", to: { name: "patient-prescriptions" }, description: "查看处方和用药风险" },
      { label: "检查报告", to: { name: "patient-reports" }, description: "查看检查检验结果" },
      { label: "电子发票", to: { name: "patient-invoices" }, description: "费用与票据说明" },
      { label: "消息中心", to: { name: "patient-messages" }, description: "就诊提醒与系统通知" },
      { label: "家庭成员管理", to: { name: "patient-visitors" }, description: "维护就诊人资料" },
      { label: "账户设置", to: { name: "patient-profile" }, description: "维护基本资料" },
    ],
    feature: { label: "进入患者服务", to: { name: "patient-dashboard" } },
  },
  {
    key: "doctors",
    label: "医生与科室",
    lead: "选择适合的专家和专科",
    description: "按医生、科室、专病中心和出诊时间组织信息，降低患者选择成本。",
    links: [
      { label: "查找医生", to: { name: "public-search", query: { q: "医生" } }, description: "按姓名或专业方向检索" },
      { label: "专家团队", to: { name: "doctor-experts" }, description: "多学科专家介绍" },
      { label: "科室导航", to: { name: "public-departments" }, description: "按科室了解诊疗范围" },
      { label: "专病中心", to: { name: "doctor-centers" }, description: "复杂疾病协作门诊" },
      { label: "医生出诊时间", to: { name: "doctor-schedules" }, description: "查看门诊排班说明" },
    ],
    feature: { label: "查找医生", to: { name: "public-search", query: { q: "医生" } } },
  },
  {
    key: "library",
    label: "健康资料库",
    lead: "可靠、克制、面向患者的医学资料",
    description: "把疾病、症状、药品、检查和康复资料做成就医前后的参考。",
    links: [
      { label: "疾病百科", to: { name: "public-conditions" }, description: "按疾病准备就诊资料" },
      { label: "症状百科", to: { name: "library-symptoms" }, description: "按症状了解风险信号" },
      { label: "药品百科", to: { name: "library-drugs" }, description: "用药注意事项" },
      { label: "检查项目", to: { name: "library-tests" }, description: "检查前准备和流程" },
      { label: "康复指导", to: { name: "library-rehab" }, description: "诊后恢复建议" },
      { label: "健康文章", to: { name: "library-articles" }, description: "患者教育内容" },
    ],
    feature: { label: "搜索健康资料", to: { name: "public-search" } },
  },
  {
    key: "ai",
    label: "智慧云脑AI",
    lead: "AI 是患者服务的一部分",
    description: "以辅助分诊、症状咨询、病历摘要、用药助手和健康评估服务患者，不替代医生判断。",
    links: [
      { label: "AI智能分诊", to: { name: "patient-triage" }, description: "获得推荐科室" },
      { label: "AI症状咨询", to: { name: "ai-symptom" }, description: "整理症状和风险提示" },
      { label: "AI病历摘要", to: { name: "ai-record-summary" }, description: "复诊前回顾关键资料" },
      { label: "AI用药助手", to: { name: "ai-medication" }, description: "理解处方和用药提醒" },
      { label: "AI健康评估", to: { name: "ai-assessment" }, description: "形成健康风险概览" },
    ],
    feature: { label: "开始AI分诊", to: { name: "patient-triage" } },
  },
  {
    key: "about",
    label: "关于智慧云脑",
    lead: "医院、科研、教育与联系信息",
    description: "保持医院官网的信息完整性，让患者理解机构能力、学术背景和服务边界。",
    links: [
      { label: "医院介绍", to: { name: "about-hospital" }, description: "机构定位和服务理念" },
      { label: "新闻动态", to: { name: "about-news" }, description: "医院公告与资讯" },
      { label: "科研与教育", to: { name: "public-research" }, description: "科研方向和教学资源" },
      { label: "招聘信息", to: { name: "about-careers" }, description: "人才与岗位信息" },
      { label: "联系我们", to: { name: "about-contact" }, description: "电话、地址和咨询入口" },
    ],
    feature: { label: "了解智慧云脑", to: { name: "about-hospital" } },
  },
];

const userLinks: NavLink[] = [
  { label: "患者服务", to: { name: "patient-dashboard" } },
  { label: "我的预约", to: { name: "patient-appointments" } },
  { label: "我的病历", to: { name: "patient-records" } },
  { label: "我的处方", to: { name: "patient-prescriptions" } },
  { label: "消息中心", to: { name: "patient-messages" } },
  { label: "账户设置", to: { name: "patient-profile" } },
];

function showMenu(key: string) {
  activeMenu.value = key;
}

function toggleMenu(key: string) {
  activeMenu.value = activeMenu.value === key ? null : key;
}

function closeMenus() {
  activeMenu.value = null;
  mobileOpen.value = false;
  userOpen.value = false;
}

function isMenuActive(menu: NavMenu) {
  return menu.links.some((link) => {
    const to = typeof link.to === "string" ? link.to : router.resolve(link.to).path;
    return route.path === to || (to !== "/" && route.path.startsWith(to));
  });
}

function openSearch() {
  closeMenus();
  router.push({ name: "public-search" });
}

async function logout() {
  auth.logout();
  closeMenus();
  await router.push({ name: "patient-home" });
}
</script>

<template>
  <header class="site-header" @mouseleave="activeMenu = null">
    <div class="site-header-inner">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页" @click="closeMenus">
        <span>智慧云脑</span>
        <i></i><i></i>
      </RouterLink>

      <nav class="site-nav" :class="{ open: mobileOpen }" aria-label="主导航">
        <div
          v-for="menu in navMenus"
          :key="menu.key"
          class="site-nav-item"
          :class="{ 'is-open': activeMenu === menu.key, active: isMenuActive(menu) }"
          @mouseenter="showMenu(menu.key)"
        >
          <button type="button" :aria-expanded="activeMenu === menu.key" @click="toggleMenu(menu.key)">
            {{ menu.label }}
            <span aria-hidden="true"></span>
          </button>
          <div v-show="activeMenu === menu.key" class="site-mega">
            <section class="site-mega-lead">
              <p>{{ menu.lead }}</p>
              <h2>{{ menu.label }}</h2>
              <span>{{ menu.description }}</span>
            </section>
            <section class="site-mega-links">
              <RouterLink v-for="link in menu.links" :key="link.label" :to="link.to" @click="closeMenus">
                <strong>{{ link.label }}</strong>
                <span v-if="link.description">{{ link.description }}</span>
              </RouterLink>
            </section>
            <aside class="site-mega-feature">
              <p>患者导向服务</p>
              <strong>官网内完成查找、预约、资料查看和复诊准备。</strong>
              <RouterLink :to="menu.feature.to" @click="closeMenus">{{ menu.feature.label }}</RouterLink>
            </aside>
          </div>
        </div>
      </nav>

      <div class="site-actions">
        <RouterLink class="site-appointment-link" :to="{ name: 'patient-doctors' }" @click="closeMenus">预约就诊</RouterLink>
        <template v-if="!isSignedIn">
          <RouterLink class="site-login-link" :to="{ name: 'patient-login' }" @click="closeMenus">登录/注册</RouterLink>
        </template>
        <div v-else class="site-user-menu">
          <button type="button" :aria-expanded="userOpen" @click="userOpen = !userOpen">
            {{ userName }}
            <span aria-hidden="true"></span>
          </button>
          <div v-show="userOpen" class="site-user-dropdown">
            <RouterLink v-for="link in userLinks" :key="link.label" :to="link.to" @click="closeMenus">
              {{ link.label }}
            </RouterLink>
            <button type="button" @click="logout">退出登录</button>
          </div>
        </div>
        <button class="site-search-button" type="button" aria-label="搜索" @click="openSearch">
          <span class="search-symbol" aria-hidden="true"></span>
        </button>
        <button class="site-menu-button" type="button" aria-label="打开菜单" @click="mobileOpen = !mobileOpen">
          <span></span>
        </button>
      </div>
    </div>
  </header>
</template>
