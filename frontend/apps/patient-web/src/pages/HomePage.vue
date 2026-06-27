<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";
import { toPatientRoute } from "../site-config/routeTarget";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";
import type { PatientHomeModule, RouteTargetConfig } from "../site-config/types";

type HomeLocation = {
  title: string;
  meta: string;
  imageUrl: string;
  alt: string;
};

const router = useRouter();
const departments = ref<DataRow[]>([]);
const doctors = ref<DataRow[]>([]);
const conditionQuery = ref("");
const { config, load: loadSiteConfig } = usePatientSiteConfig();

const hero = computed(() => config.value.home.hero);
const homeModules = computed(() => config.value.home.modules);
const notice = computed(() => homeModules.value.find((module) => module.type === "notice"));
const introModule = computed(() => moduleByType("intro"));
const locationsModule = computed(() => moduleByType("locations"));
const featuredDepartmentsModule = computed(() => moduleByType("featured_departments"));
const staticContentModule = computed(() => moduleByType("static_content"));

const quickActions = computed(() => {
  const module = moduleByType("quick_actions");
  const items = contentArray(module, "items");
  return items.filter(isRouteAction);
});

const introContent = computed(() => contentOf(introModule.value));
const locationsContent = computed(() => contentOf(locationsModule.value));
const featuredDepartmentsContent = computed(() => contentOf(featuredDepartmentsModule.value));
const staticContent = computed(() => contentOf(staticContentModule.value));

const introAction = computed(() =>
  actionValue(introContent.value.action, { label: "进入患者服务", routeName: "patient-dashboard" }),
);

const staticContentAction = computed(() =>
  actionValue(staticContent.value.action, { label: "了解科研与教育", routeName: "public-research" }),
);

const locationItems = computed(() => {
  const items = contentArray(locationsModule.value, "items")
    .map((item) => {
      const row = record(item);
      return {
        title: textValue(row.title, ""),
        meta: textValue(row.meta, ""),
        imageUrl: textValue(row.imageUrl, ""),
        alt: textValue(row.alt, textValue(row.title, "院区图片")),
      };
    })
    .filter((item) => item.title && item.imageUrl);
  return items.length ? items : defaultLocations;
});

const featuredDepartmentLimit = computed(() => numberValue(featuredDepartmentsContent.value.limit, 12));
const featuredDepartments = computed(() => departments.value.slice(0, featuredDepartmentLimit.value));
const configuredDepartmentLinks = computed(() => contentArray(featuredDepartmentsModule.value, "items").filter(isRouteAction));
const fallbackDepartmentNames = computed(() => {
  const names = contentArray(featuredDepartmentsModule.value, "fallbackNames")
    .filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    .map((item) => item.trim());
  return names.length ? names : defaultDepartmentNames;
});

const defaultLocations: HomeLocation[] = [
  {
    imageUrl: "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=900&q=80",
    alt: "现代化医院建筑",
    title: "智慧云脑北京中心",
    meta: "北京 · 海淀",
  },
  {
    imageUrl: "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=900&q=80",
    alt: "医院走廊",
    title: "智慧云脑上海中心",
    meta: "上海 · 浦东",
  },
  {
    imageUrl: "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=900&q=80",
    alt: "临床团队工作场景",
    title: "智慧云脑深圳中心",
    meta: "深圳 · 南山",
  },
];

const defaultDepartmentNames = ["神经内科", "心血管内科", "呼吸内科", "消化内科", "骨科", "妇科", "儿科", "肿瘤科", "康复医学科"];

function moduleByType(type: string): PatientHomeModule | undefined {
  return homeModules.value.find((module) => module.type === type);
}

function contentOf(module: PatientHomeModule | undefined): Record<string, unknown> {
  return record(module?.content);
}

function contentArray(module: PatientHomeModule | undefined, key: string): unknown[] {
  const value = contentOf(module)[key];
  return Array.isArray(value) ? value : [];
}

function record(value: unknown): Record<string, unknown> {
  return Boolean(value && typeof value === "object" && !Array.isArray(value)) ? value as Record<string, unknown> : {};
}

function textValue(value: unknown, fallback: string) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) && value > 0 ? value : fallback;
}

function actionValue(value: unknown, fallback: RouteTargetConfig): RouteTargetConfig {
  return isRouteAction(value) ? value : fallback;
}

function isRouteAction(value: unknown): value is RouteTargetConfig {
  if (!value || typeof value !== "object" || !("label" in value) || !("routeName" in value)) return false;
  const link = value as RouteTargetConfig;
  return link.routeName !== "cms-page" || Boolean(link.slug);
}

function goSearch(q = "") {
  const queryText = q.trim();
  router.push({ name: "public-search", query: queryText ? { q: queryText } : {} });
}

onMounted(async () => {
  await loadSiteConfig();
  try {
    const [departmentRows, doctorRows] = await Promise.all([api.departments(), api.doctors()]);
    departments.value = departmentRows;
    doctors.value = doctorRows;
  } catch {
    departments.value = [];
    doctors.value = [];
  }
});
</script>

<template>
  <main class="home-page">
    <section v-if="hero.enabled !== false" class="home-hero">
      <div class="home-hero-content">
        <p class="home-eyebrow">{{ hero.eyebrow }}</p>
        <h1>{{ hero.title }}</h1>
        <div class="home-hero-actions">
          <RouterLink v-if="hero.primaryAction" :to="toPatientRoute(hero.primaryAction)">{{ hero.primaryAction.label }}</RouterLink>
          <RouterLink v-if="hero.secondaryAction" class="home-pill" :to="toPatientRoute(hero.secondaryAction)">
            {{ hero.secondaryAction.label }}
          </RouterLink>
        </div>
      </div>
    </section>

    <div v-if="notice" class="home-alert">
      <strong>!</strong>
      <span>{{ String(notice.content?.text || "") }}</span>
    </div>

    <section v-if="quickActions.length" class="home-section home-care-grid">
      <RouterLink v-for="action in quickActions" :key="action.label" :to="toPatientRoute(action)">
        {{ action.label }} <span>→</span>
      </RouterLink>
    </section>

    <section class="home-section home-conditions">
      <div>
        <p class="home-eyebrow">按首字母查找疾病百科</p>
        <div class="home-letters" aria-label="疾病索引">
          <RouterLink
            v-for="letter in ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','R','S','T','U','V','W','X','Y','Z','#']"
            :key="letter"
            :to="{ name: 'public-conditions', query: { letter } }"
          >
            {{ letter }}
          </RouterLink>
        </div>
      </div>
      <div>
        <label class="condition-label" for="condition-search">搜索疾病、症状、医生或科室</label>
        <form class="home-searchbox" @submit.prevent="goSearch(conditionQuery)">
          <span class="search-symbol" aria-hidden="true"></span>
          <input id="condition-search" v-model="conditionQuery" type="search" placeholder="搜索">
        </form>
      </div>
    </section>

    <section v-if="introModule" class="home-section home-split">
      <div>
        <h2>{{ textValue(introContent.title, "患者服务是医院官网的一部分") }}</h2>
        <h3>{{ textValue(introContent.firstTitle, "从分诊到复诊，保持同一条就医线索") }}</h3>
        <p>{{ textValue(introContent.firstText, "智慧云脑把 AI 分诊、医生号源、预约记录、病历和处方连接起来，让患者在同一医院门户内完成关键服务。") }}</p>
        <h3>{{ textValue(introContent.secondTitle, "真实数据驱动服务入口") }}</h3>
        <p>
          {{ textValue(introContent.secondTextPrefix, "当前已接入") }}
          {{ departments.length }}
          {{ textValue(introContent.departmentUnit, "个科室、") }}{{ doctors.length }}
          {{ textValue(introContent.doctorUnit, "位医生。登录后可查看个人预约、诊后资料和家庭成员信息。") }}
        </p>
        <RouterLink class="home-pill outline-blue" :to="toPatientRoute(introAction)">{{ introAction.label }}</RouterLink>
      </div>
      <div class="home-image-panel">
        <img
          :src="textValue(introContent.imageUrl, 'https://images.unsplash.com/photo-1582750433449-648ed127bb54?auto=format&fit=crop&w=1200&q=80')"
          :alt="textValue(introContent.imageAlt, '医生与患者沟通')"
        >
      </div>
    </section>

    <section v-if="locationsModule" class="home-section">
      <div class="home-section-head">
        <h2>{{ textValue(locationsContent.title, "院区与到院服务") }}</h2>
        <p>{{ textValue(locationsContent.description, "了解智慧云脑各院区服务方向，结合预约时间、科室位置和检查安排做好到院准备。") }}</p>
      </div>
      <div class="home-locations">
        <article v-for="item in locationItems" :key="item.title">
          <img :src="item.imageUrl" :alt="item.alt">
          <h3>{{ item.title }}</h3>
          <p>{{ item.meta }}</p>
        </article>
      </div>
    </section>

    <section v-if="featuredDepartmentsModule" class="home-section">
      <div class="home-section-head">
        <h2>{{ textValue(featuredDepartmentsContent.title, "重点诊疗领域") }}</h2>
        <p>{{ textValue(featuredDepartmentsContent.description, "优先展示后端真实科室数据；暂无数据时展示常见诊疗方向。") }}</p>
      </div>
      <div class="home-care-grid">
        <RouterLink
          v-for="dept in featuredDepartments"
          :key="String(dept.departmentId || dept.id || dept.name)"
          :to="{ name: 'public-search', query: { q: String(dept.name || dept.departmentName || '科室') } }"
        >
          {{ dept.name || dept.departmentName || "科室" }} <span>→</span>
        </RouterLink>
        <template v-if="!featuredDepartments.length && configuredDepartmentLinks.length">
          <RouterLink v-for="item in configuredDepartmentLinks" :key="item.label" :to="toPatientRoute(item)">
            {{ item.label }} <span>→</span>
          </RouterLink>
        </template>
        <template v-else-if="!featuredDepartments.length">
          <RouterLink v-for="name in fallbackDepartmentNames" :key="name" :to="{ name: 'public-search', query: { q: name } }">
            {{ name }} <span>→</span>
          </RouterLink>
        </template>
      </div>
    </section>

    <section v-if="staticContentModule" class="home-donate">
      <div>
        <img
          :src="textValue(staticContent.imageUrl, 'https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&w=1200&q=80')"
          :alt="textValue(staticContent.imageAlt, '实验室医学研究人员')"
        >
      </div>
      <div>
        <h2>{{ textValue(staticContent.title, "科研与教育推动更好的患者照护") }}</h2>
        <p>{{ textValue(staticContent.text, "智慧云脑关注分诊质量、诊疗效率、用药安全和患者可理解性，把临床问题转化为可持续改进的服务能力。") }}</p>
        <RouterLink class="home-pill" :to="toPatientRoute(staticContentAction)">{{ staticContentAction.label }}</RouterLink>
      </div>
    </section>
  </main>
</template>
