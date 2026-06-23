<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";

type PublicPage = {
  eyebrow: string;
  title: string;
  intro: string;
  actions: { label: string; to: { name: string; query?: Record<string, string> } }[];
  sections: { title: string; body: string; items: string[] }[];
};

const route = useRoute();

const pages: Record<string, PublicPage> = {
  "public-guide": {
    eyebrow: "Patient guide",
    title: "患者服务指南",
    intro: "把就诊前准备、在线分诊、预约挂号、诊后资料查看放在同一条清晰流程里，减少患者在不同系统间来回确认。",
    actions: [
      { label: "开始智能分诊", to: { name: "patient-triage" } },
      { label: "查找科室", to: { name: "public-departments" } },
    ],
    sections: [
      {
        title: "就诊前",
        body: "首次使用建议先完善本人资料和常用就诊人，再通过 AI 分诊描述主要症状、持续时间和伴随表现。",
        items: ["准备身份证件和既往检查结果", "记录过敏史、既往病史和正在使用的药物", "紧急症状请优先线下急诊或拨打急救电话"],
      },
      {
        title: "预约与到院",
        body: "分诊结果会推荐适合的科室。登录后可查看可预约号源，选择医生、日期与时段并确认挂号。",
        items: ["按推荐科室筛选号源", "确认就诊人和联系电话", "到院后按院区指引完成签到和候诊"],
      },
      {
        title: "诊后管理",
        body: "医生保存病历和处方后，患者端会同步展示，便于复诊时回看诊断、医嘱和用药风险提示。",
        items: ["查看病历记录", "核对处方与风险审核结果", "需要复诊时带上近期记录和检查报告"],
      },
    ],
  },
  "public-conditions": {
    eyebrow: "Conditions",
    title: "疾病与病症索引",
    intro: "按常见症状和疾病方向组织信息，帮助患者理解可能涉及的科室、观察重点和何时需要尽快就医。",
    actions: [
      { label: "搜索疾病或症状", to: { name: "public-search", query: { q: "头痛" } } },
      { label: "使用 AI 分诊", to: { name: "patient-triage" } },
    ],
    sections: [
      {
        title: "常见症状",
        body: "症状信息用于就诊准备，不替代医生诊断。若出现胸痛、意识障碍、呼吸困难、单侧肢体无力等情况，应立即急诊处理。",
        items: ["发热、咳嗽、咽痛", "头痛、眩晕、肢体麻木", "腹痛、腹泻、恶心呕吐", "胸闷、心悸、气短"],
      },
      {
        title: "慢病管理",
        body: "慢性病需要持续记录指标和用药反应，复诊时提供趋势比单次读数更有价值。",
        items: ["高血压与血脂异常", "糖尿病与体重管理", "慢性呼吸系统疾病", "骨关节和康复问题"],
      },
      {
        title: "就诊建议",
        body: "无法判断科室时，先通过 AI 分诊整理症状，系统会给出推荐科室和说明，再进入预约流程。",
        items: ["描述主要症状和持续时间", "补充诱因、缓解因素和伴随症状", "上传或携带既往检查结果"],
      },
    ],
  },
  "public-locations": {
    eyebrow: "Locations",
    title: "院区位置",
    intro: "展示智慧云脑线下协作院区和到院服务信息，方便患者在预约前确认交通、服务范围和现场流程。",
    actions: [
      { label: "查找科室", to: { name: "public-departments" } },
      { label: "预约就诊", to: { name: "patient-login" } },
    ],
    sections: [
      {
        title: "北京中心",
        body: "面向综合门诊、神经内科、心血管内科和康复医学等方向，适合需要多学科协作评估的患者。",
        items: ["海淀区临床协作中心", "工作日门诊与周末部分专科门诊", "支持检查结果线上回看"],
      },
      {
        title: "上海中心",
        body: "聚焦消化、呼吸、妇科和儿科等常见专科服务，衔接线上分诊和现场就诊流程。",
        items: ["浦东院区", "地铁与停车路线清晰", "支持复诊患者资料同步"],
      },
      {
        title: "深圳中心",
        body: "提供肿瘤随访、骨科康复和慢病管理协作服务，便于长期患者持续追踪。",
        items: ["南山院区", "康复评估与随访", "专科护士协同健康教育"],
      },
    ],
  },
  "public-professionals": {
    eyebrow: "For professionals",
    title: "医疗专业人士",
    intro: "为医生、护理团队和合作机构提供转诊、资料协同、临床路径和继续教育入口。",
    actions: [
      { label: "科研与教育", to: { name: "public-research" } },
      { label: "查找重点专科", to: { name: "public-departments" } },
    ],
    sections: [
      {
        title: "转诊协作",
        body: "面向需要进一步专科评估的患者，协作医生可以围绕症状、既往检查和治疗反应快速形成转诊摘要。",
        items: ["推荐科室与医生方向", "结构化病史和检查摘要", "诊后结果回传与复诊建议"],
      },
      {
        title: "临床资料协同",
        body: "系统围绕分诊、挂号、病历和处方审核串联数据，帮助专业团队减少重复录入。",
        items: ["病历生成辅助", "处方风险审核", "诊疗过程留痕"],
      },
      {
        title: "专业教育",
        body: "围绕常见病、急诊分级、用药风险和病例复盘构建继续教育材料。",
        items: ["病例讨论", "临床路径学习", "AI 辅助诊疗应用培训"],
      },
    ],
  },
  "public-research": {
    eyebrow: "Research and education",
    title: "科研与教育",
    intro: "将临床问题、数据治理和医学教育连接起来，支持更可靠的智能诊疗辅助能力。",
    actions: [
      { label: "医疗专业人士", to: { name: "public-professionals" } },
      { label: "支持项目", to: { name: "public-giving" } },
    ],
    sections: [
      {
        title: "临床研究方向",
        body: "围绕分诊准确性、慢病随访、处方风险和诊后依从性开展应用研究。",
        items: ["智能分诊效果评估", "号源匹配与患者体验", "处方审核与用药安全"],
      },
      {
        title: "医学教育",
        body: "把真实业务流程拆解成可学习、可复盘的训练材料，帮助医护团队理解 AI 工具的适用边界。",
        items: ["病例结构化训练", "急诊分级标准学习", "医患沟通与病历书写"],
      },
      {
        title: "数据治理",
        body: "研究和教学场景坚持最小必要使用、权限控制和脱敏处理，保护患者隐私。",
        items: ["数据脱敏", "访问审计", "研究伦理和授权管理"],
      },
    ],
  },
  "public-giving": {
    eyebrow: "Giving",
    title: "支持智慧云脑",
    intro: "支持患者照护、医学研究和人才培养，让更多患者获得清晰、连续、可追踪的医疗服务。",
    actions: [
      { label: "了解科研项目", to: { name: "public-research" } },
      { label: "患者服务指南", to: { name: "public-guide" } },
    ],
    sections: [
      {
        title: "患者照护支持",
        body: "面向长期治疗、异地复诊和复杂病情患者，改善从分诊到诊后的服务连续性。",
        items: ["弱势患者就医协助", "慢病随访教育", "复诊资料整理"],
      },
      {
        title: "医学研究支持",
        body: "资助围绕诊疗质量、用药安全和患者体验的应用研究。",
        items: ["智能分诊评估", "临床路径优化", "患者安全研究"],
      },
      {
        title: "人才培养支持",
        body: "支持年轻医生和护理团队接受数字医疗、AI 辅助诊疗和数据治理培训。",
        items: ["继续教育课程", "病例复盘工作坊", "跨学科协作训练"],
      },
    ],
  },
};

const page = computed(() => pages[String(route.name)] ?? pages["public-guide"]);
</script>

<template>
  <main class="public-info-page">
    <section class="public-hero">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <p class="home-eyebrow">{{ page.eyebrow }}</p>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
      <div class="public-actions">
        <RouterLink v-for="action in page.actions" :key="action.label" class="home-pill outline-blue" :to="action.to">
          {{ action.label }}
        </RouterLink>
      </div>
    </section>

    <section class="public-section-grid">
      <article v-for="section in page.sections" :key="section.title">
        <h2>{{ section.title }}</h2>
        <p>{{ section.body }}</p>
        <ul>
          <li v-for="item in section.items" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>
  </main>
</template>
