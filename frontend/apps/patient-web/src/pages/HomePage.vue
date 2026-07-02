<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  api,
  type Department,
  type Doctor,
  type PatientNotice,
  type PatientRecommendation,
} from "@smart-cloud-brain/shared-api";
import PatientSiteSectionRenderer from "../components/cms/PatientSiteSectionRenderer.vue";
import { actionValue, contentArray, contentOf, homeModuleToSection, isRouteAction, isSectionHomeModule, numberValue, onImageError, recommendationDescription, recommendationKey, record, textValue } from "../site-config/homePageHelpers";
import { toPatientRoute, withPatientPreview } from "../site-config/routeTarget";
import { usePatientSiteConfig } from "../site-config/usePatientSiteConfig";
import type { PatientHomeModule } from "../site-config/types";

const router = useRouter();
const route = useRoute();
const departments = ref<Department[]>([]);
const doctors = ref<Doctor[]>([]);
const portalNotices = ref<PatientNotice[]>([]);
const hotDepartments = ref<PatientRecommendation[]>([]);
const recommendedDoctors = ref<PatientRecommendation[]>([]);
const conditionQuery = ref("");
const { config, loadHome, loadPreview } = usePatientSiteConfig();

const hero = computed(() => config.value.home.hero);
const heroStyle = computed(() => {
  const imageUrl = hero.value.backgroundImageUrl?.trim();
  return imageUrl ? { "--hero-bg": `url("${imageUrl.replace(/"/g, '\\"')}")` } : undefined;
});
const homeModules = computed(() => config.value.home.modules);
const previewToken = computed(() => {
  const value = route.query.previewToken;
  return typeof value === "string" ? value : "";
});
const notice = computed(() => moduleByType("notice"));
const noticeText = computed(() => {
  if (!notice.value) return "";
  const firstNotice = portalNotices.value[0];
  if (firstNotice) return `${firstNotice.title}：${firstNotice.content}`;
  return String(notice.value?.content?.text || "");
});
const introModule = computed(() => moduleByType("intro"));
const locationsModule = computed(() => moduleByType("locations"));
const featuredDepartmentsModule = computed(() => moduleByType("featured_departments"));
const doctorListModule = computed(() => moduleByType("doctor_list"));
const staticContentModule = computed(() => moduleByType("static_content"));

const quickActions = computed(() => {
  const module = moduleByType("quick_actions");
  const items = contentArray(module, "items");
  return items.filter(isRouteAction);
});

const introContent = computed(() => contentOf(introModule.value));
const locationsContent = computed(() => contentOf(locationsModule.value));
const featuredDepartmentsContent = computed(() => contentOf(featuredDepartmentsModule.value));
const doctorListContent = computed(() => contentOf(doctorListModule.value));
const staticContent = computed(() => contentOf(staticContentModule.value));
const sectionHomeModules = computed(() =>
  homeModules.value
    .filter((module) => module.enabled !== false && isSectionHomeModule(module) && module.type !== "doctor_list")
    .map((module) => homeModuleToSection(module)),
);

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
  return items.length ? items : [];
});

const featuredDepartmentLimit = computed(() => numberValue(featuredDepartmentsContent.value.limit, 12));
const recommendedDoctorLimit = computed(() => numberValue(doctorListContent.value.limit, 6));
const featuredDepartments = computed(() => {
  if (hotDepartments.value.length) return hotDepartments.value.slice(0, featuredDepartmentLimit.value);
  return departments.value.slice(0, featuredDepartmentLimit.value);
});
const displayedRecommendedDoctors = computed(() => recommendedDoctors.value.slice(0, recommendedDoctorLimit.value));
const configuredDepartmentLinks = computed(() => contentArray(featuredDepartmentsModule.value, "items").filter(isRouteAction));
const fallbackDepartmentNames = computed(() => {
  const names = contentArray(featuredDepartmentsModule.value, "fallbackNames")
    .filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    .map((item) => item.trim());
  return names.length ? names : [];
});
const fallbackDoctorNames = computed(() =>
  contentArray(doctorListModule.value, "fallbackNames")
    .filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    .map((item) => item.trim()),
);

function moduleByType(type: string): PatientHomeModule | undefined {
  return homeModules.value.find((module) => module.type === type && module.enabled !== false);
}

function goSearch(q = "") {
  const queryText = q.trim();
  router.push(withPatientPreview({ name: "public-search", query: queryText ? { q: queryText } : {} }, previewToken.value));
}

function recommendationTitle(item: PatientRecommendation | Department | Doctor) {
  return String(("title" in item && item.title) || ("targetName" in item && item.targetName) || ("name" in item && item.name) || "科室");
}

onMounted(async () => {
  if (previewToken.value) {
    await loadPreview(previewToken.value);
    portalNotices.value = [];
    hotDepartments.value = [];
    recommendedDoctors.value = [];
  } else {
    const homeConfig = await loadHome();
    const homeRow = record(homeConfig);
    portalNotices.value = Array.isArray(homeRow.notices) ? homeRow.notices as PatientNotice[] : [];
    hotDepartments.value = Array.isArray(homeRow.hotDepartments) ? homeRow.hotDepartments as PatientRecommendation[] : [];
    recommendedDoctors.value = Array.isArray(homeRow.recommendedDoctors) ? homeRow.recommendedDoctors as PatientRecommendation[] : [];
  }
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
    <section v-if="hero.enabled !== false" class="home-hero" :style="heroStyle">
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

    <div v-if="noticeText" class="home-alert">
      <strong>!</strong>
      <span>{{ noticeText }}</span>
    </div>

    <section v-if="quickActions.length" class="home-section home-care-grid">
      <RouterLink v-for="action in quickActions" :key="action.label" :to="toPatientRoute(action)">
        {{ action.label }} <span>→</span>
      </RouterLink>
    </section>

    <section v-if="sectionHomeModules.length" class="home-section home-cms-sections">
      <PatientSiteSectionRenderer
        v-for="section in sectionHomeModules"
        :key="section.id || `${section.type}-${section.sort || 0}`"
        :section="section"
      />
    </section>

    <section class="home-section home-conditions">
      <div>
        <p class="home-eyebrow">按首字母查找疾病百科</p>
        <div class="home-letters" aria-label="疾病索引">
          <RouterLink
            v-for="letter in ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','#']"
            :key="letter"
            :to="withPatientPreview({ name: 'public-conditions', query: { letter } }, previewToken)"
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
          @error="onImageError"
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
          <img :src="item.imageUrl" :alt="item.alt" @error="onImageError">
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
          :key="recommendationKey(dept)"
          :to="withPatientPreview({ name: 'public-search', query: { q: recommendationTitle(dept) } }, previewToken)"
        >
          {{ recommendationTitle(dept) }} <span>→</span>
        </RouterLink>
        <template v-if="!featuredDepartments.length && configuredDepartmentLinks.length">
          <RouterLink v-for="item in configuredDepartmentLinks" :key="item.label" :to="toPatientRoute(item)">
            {{ item.label }} <span>→</span>
          </RouterLink>
        </template>
        <template v-else-if="!featuredDepartments.length">
          <RouterLink v-for="name in fallbackDepartmentNames" :key="name" :to="withPatientPreview({ name: 'public-search', query: { q: name } }, previewToken)">
            {{ name }} <span>→</span>
          </RouterLink>
        </template>
      </div>
    </section>

    <section v-if="doctorListModule && (displayedRecommendedDoctors.length || fallbackDoctorNames.length)" class="home-section">
      <div class="home-section-head">
        <h2>{{ textValue(doctorListContent.title, "推荐医生") }}</h2>
        <p>根据管理端配置优先展示推荐专家，患者可继续通过搜索查看完整医生团队。</p>
      </div>
      <div class="home-doctor-grid">
        <RouterLink
          v-for="doctor in displayedRecommendedDoctors"
          :key="recommendationKey(doctor)"
          :to="withPatientPreview({ name: 'public-search', query: { q: recommendationTitle(doctor) } }, previewToken)"
        >
          <img v-if="doctor.imageUrl" :src="doctor.imageUrl" :alt="recommendationTitle(doctor)" @error="onImageError">
          <span>
            <strong>{{ recommendationTitle(doctor) }}</strong>
            <small>{{ recommendationDescription(doctor) }}</small>
          </span>
        </RouterLink>
        <RouterLink
          v-for="name in fallbackDoctorNames"
          v-show="!displayedRecommendedDoctors.length"
          :key="name"
          :to="withPatientPreview({ name: 'public-search', query: { q: name } }, previewToken)"
        >
          <span>
            <strong>{{ name }}</strong>
            <small>管理端配置的推荐医生</small>
          </span>
        </RouterLink>
      </div>
    </section>

    <section v-if="staticContentModule" class="home-donate">
      <div>
        <img
          :src="textValue(staticContent.imageUrl, 'https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&w=1200&q=80')"
          :alt="textValue(staticContent.imageAlt, '实验室医学研究人员')"
          @error="onImageError"
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
