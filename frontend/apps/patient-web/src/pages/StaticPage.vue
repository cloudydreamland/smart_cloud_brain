<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";

type LinkTarget = { name: string; query?: Record<string, string> };

type StaticPageContent = {
  label: string;
  title: string;
  intro: string;
  points: { title: string; text: string }[];
  primary?: { label: string; to: LinkTarget };
};

const route = useRoute();

const fallback: StaticPageContent = {
  label: "智慧云脑",
  title: "智慧云脑医疗服务",
  intro: "该栏目正在按医院官网的信息架构整理，患者可继续使用预约、分诊、医生科室和诊后资料服务。",
  points: [
    { title: "专业信息", text: "内容围绕患者就医前、就医中和就医后的真实需求组织。" },
    { title: "统一入口", text: "相关服务保留在同一医院官网视觉体系中，与公共医疗信息自然衔接。" },
    { title: "持续完善", text: "后续可接入真实业务数据和医院公告内容。" },
  ],
  primary: { label: "返回首页", to: { name: "patient-home" } },
};

const pages: Record<string, StaticPageContent> = {
  "service-internet-clinic": {
    label: "医疗服务",
    title: "互联网门诊",
    intro: "面向复诊、病情咨询和诊后随访场景，帮助患者在到院前后获得连续、克制、可追踪的医疗服务。",
    points: [
      { title: "适用场景", text: "复诊咨询、检查结果解读、慢病随访和用药问题整理。" },
      { title: "服务边界", text: "急症、首次明确诊断和需要体格检查的情况，应优先线下就诊。" },
      { title: "就诊准备", text: "准备近期病历、处方、检查报告和症状变化记录，有助于医生判断。" },
    ],
    primary: { label: "先做AI智能分诊", to: { name: "patient-triage" } },
  },
  "service-exam-booking": {
    label: "医疗服务",
    title: "检查检验预约",
    intro: "围绕检查前准备、预约确认、到院流程和结果查看，帮助患者减少重复等待。",
    points: [
      { title: "预约前", text: "确认医生开具的检查项目、是否空腹、是否需要停药或携带既往资料。" },
      { title: "到院时", text: "按预约时间完成签到和身份核验，特殊检查以现场医嘱为准。" },
      { title: "检查后", text: "报告生成后可在患者服务中查看，复诊时建议一并携带病历和处方。" },
    ],
    primary: { label: "查看检查报告", to: { name: "patient-reports" } },
  },
  "service-inpatient": {
    label: "医疗服务",
    title: "住院服务",
    intro: "说明住院前资料准备、入院办理、住院期间沟通和出院后复诊资料管理。",
    points: [
      { title: "入院前", text: "准备身份证件、医保信息、既往病历、检查影像和长期用药清单。" },
      { title: "住院中", text: "治疗方案、检查安排和出院计划以主管医生团队沟通为准。" },
      { title: "出院后", text: "出院小结、复诊建议、处方和康复指导应统一归档，便于长期管理。" },
    ],
    primary: { label: "查看我的病历", to: { name: "patient-records" } },
  },
  "service-emergency": {
    label: "医疗服务",
    title: "急诊指南",
    intro: "当出现红旗症状时，不应等待线上分诊或预约，应优先线下急诊或拨打急救电话。",
    points: [
      { title: "立即急诊", text: "胸痛、呼吸困难、意识改变、单侧肢体无力、大量出血、严重过敏等情况需立即处理。" },
      { title: "携带资料", text: "如条件允许，携带身份证件、用药清单、过敏史和近期检查资料。" },
      { title: "线上服务边界", text: "AI 分诊仅用于非紧急情况下的科室建议，不替代急诊判断。" },
    ],
    primary: { label: "了解就诊准备", to: { name: "public-guide" } },
  },
  "service-international": {
    label: "医疗服务",
    title: "国际医疗",
    intro: "为跨境患者、外籍患者和多语种就诊场景提供预约、资料准备和连续照护说明。",
    points: [
      { title: "资料准备", text: "建议提前整理英文或中文病历摘要、检查影像、用药清单和既往治疗记录。" },
      { title: "预约协同", text: "根据专科方向、就诊时间和资料完整度安排合适的专家资源。" },
      { title: "连续照护", text: "诊后资料、复诊建议和用药信息应形成可追踪记录，便于跨地区随访。" },
    ],
    primary: { label: "联系我们", to: { name: "about-contact" } },
  },
  "doctor-experts": {
    label: "医生与科室",
    title: "专家团队",
    intro: "以多学科协作和复杂疾病诊疗为核心，帮助患者理解专家团队的服务方向。",
    points: [
      { title: "多学科协作", text: "针对复杂疾病，医生团队可围绕诊断、治疗、康复和随访形成连续方案。" },
      { title: "专长检索", text: "患者可通过医生姓名、科室、疾病方向和关键词查找合适专家。" },
      { title: "出诊信息", text: "具体号源和出诊时间以在线挂号页面显示为准。" },
    ],
    primary: { label: "查找医生", to: { name: "public-search", query: { q: "医生" } } },
  },
  "doctor-centers": {
    label: "医生与科室",
    title: "专病中心",
    intro: "围绕肿瘤、心脑血管、慢病、康复和儿童健康等方向组织跨科室服务。",
    points: [
      { title: "专病导向", text: "按疾病和治疗阶段组织科室资源，减少患者在科室之间反复选择。" },
      { title: "连续资料", text: "分诊、挂号、病历和处方资料可服务于复诊和长期随访。" },
      { title: "专业判断", text: "最终诊疗方案由医生结合检查和面诊情况确认。" },
    ],
    primary: { label: "浏览科室导航", to: { name: "public-departments" } },
  },
  "doctor-schedules": {
    label: "医生与科室",
    title: "医生出诊时间",
    intro: "展示医生排班、可预约号源和门诊时间说明，具体余号以在线挂号页面为准。",
    points: [
      { title: "按科室筛选", text: "先选择科室或专病方向，再查看对应医生号源。" },
      { title: "按时间安排", text: "结合患者时间、检查准备和复诊周期选择合适门诊。" },
      { title: "预约确认", text: "完成预约后，请在我的预约中查看状态和就诊时间。" },
    ],
    primary: { label: "在线挂号", to: { name: "patient-doctors" } },
  },
  "library-symptoms": {
    label: "健康资料库",
    title: "症状百科",
    intro: "按症状整理就医前应记录的信息、可能涉及科室和需要尽快就医的风险信号。",
    points: [
      { title: "记录方式", text: "记录开始时间、持续时间、诱因、缓解因素和伴随症状。" },
      { title: "风险信号", text: "症状突然加重或伴随意识、呼吸、胸痛、神经功能异常，应优先急诊。" },
      { title: "分诊辅助", text: "不确定科室时，可先使用 AI 智能分诊整理症状。" },
    ],
    primary: { label: "AI智能分诊", to: { name: "patient-triage" } },
  },
  "library-drugs": {
    label: "健康资料库",
    title: "药品百科",
    intro: "帮助患者理解处方药品、用药注意事项、相互作用和复诊时需要反馈的信息。",
    points: [
      { title: "按医嘱用药", text: "剂量、频次和疗程以医生处方为准，不自行停药或加量。" },
      { title: "关注风险", text: "出现过敏、明显不适或疑似不良反应时，应及时联系医生或就医。" },
      { title: "复诊准备", text: "复诊时带上当前用药清单、漏服情况和症状变化记录。" },
    ],
    primary: { label: "查看我的处方", to: { name: "patient-prescriptions" } },
  },
  "library-tests": {
    label: "健康资料库",
    title: "检查项目",
    intro: "按检查类型说明准备事项、流程、报告查看和复诊沟通重点。",
    points: [
      { title: "检查前", text: "确认是否空腹、是否停药、是否需要携带既往影像或检验结果。" },
      { title: "检查中", text: "按现场医护要求完成身份核验和检查准备。" },
      { title: "检查后", text: "报告结果应结合医生诊断解读，不单独作为自我诊断依据。" },
    ],
    primary: { label: "检查检验预约", to: { name: "service-exam-booking" } },
  },
  "library-rehab": {
    label: "健康资料库",
    title: "康复指导",
    intro: "为术后、慢病、神经康复、骨科康复和长期随访患者提供诊后执行建议。",
    points: [
      { title: "循序渐进", text: "康复训练应遵循医生或康复治疗师建议，避免过度训练。" },
      { title: "记录变化", text: "记录疼痛、活动能力、睡眠、饮食和生活质量变化。" },
      { title: "及时复诊", text: "出现症状加重、功能明显下降或异常反应时，应及时复诊。" },
    ],
    primary: { label: "查看我的病历", to: { name: "patient-records" } },
  },
  "library-articles": {
    label: "健康资料库",
    title: "健康文章",
    intro: "提供面向患者的疾病认知、就医准备、复诊管理和健康生活方式内容。",
    points: [
      { title: "患者可读", text: "文章强调清晰、克制和可执行，不制造焦虑。" },
      { title: "医学边界", text: "内容用于健康教育，不替代医生诊断和治疗。" },
      { title: "连续服务", text: "文章与分诊、预约、病历和处方服务形成同一资料体系。" },
    ],
    primary: { label: "搜索资料", to: { name: "public-search" } },
  },
  "ai-symptom": {
    label: "智慧云脑AI",
    title: "AI症状咨询",
    intro: "用结构化问题帮助患者整理症状、风险信号和就医准备，不直接给出诊断结论。",
    points: [
      { title: "症状整理", text: "帮助患者补充时间、部位、程度、诱因和伴随表现。" },
      { title: "风险提示", text: "识别需要线下急诊或尽快就医的情况。" },
      { title: "下一步", text: "可衔接 AI 智能分诊和在线挂号。" },
    ],
    primary: { label: "进入AI智能分诊", to: { name: "patient-triage" } },
  },
  "ai-record-summary": {
    label: "智慧云脑AI",
    title: "AI病历摘要",
    intro: "面向复诊和长期管理，帮助患者从既往病历、处方和检查资料中整理关键信息。",
    points: [
      { title: "复诊准备", text: "摘要可帮助患者回顾诊断、医嘱、检查和用药变化。" },
      { title: "资料来源", text: "以医生保存的病历和处方记录为基础，避免脱离原始资料。" },
      { title: "医生确认", text: "摘要用于沟通辅助，正式医疗判断以医生确认为准。" },
    ],
    primary: { label: "查看我的病历", to: { name: "patient-records" } },
  },
  "ai-medication": {
    label: "智慧云脑AI",
    title: "AI用药助手",
    intro: "帮助患者理解处方用法、风险提醒和复诊时应反馈的问题。",
    points: [
      { title: "用药理解", text: "把处方中的药品、剂量、频次和注意事项转化为更清晰的说明。" },
      { title: "安全提醒", text: "关注过敏史、特殊人群、相互作用和不良反应线索。" },
      { title: "处方边界", text: "任何用药调整均需医生确认。" },
    ],
    primary: { label: "查看我的处方", to: { name: "patient-prescriptions" } },
  },
  "ai-assessment": {
    label: "智慧云脑AI",
    title: "AI健康评估",
    intro: "围绕症状、慢病指标、生活方式和复诊资料形成健康风险概览。",
    points: [
      { title: "指标趋势", text: "长期血压、血糖、体重、睡眠和运动记录比单次数据更有参考价值。" },
      { title: "风险沟通", text: "评估结果用于帮助患者和医生沟通，不作为诊断。" },
      { title: "行动建议", text: "结合医生建议形成复诊、检查和生活方式管理计划。" },
    ],
    primary: { label: "查看患者服务", to: { name: "patient-dashboard" } },
  },
  "about-hospital": {
    label: "关于智慧云脑",
    title: "医院介绍",
    intro: "智慧云脑以专业医院官网为主入口，将患者服务、医生协作和 AI 辅助能力整合为连续照护体验。",
    points: [
      { title: "患者中心", text: "信息架构围绕找医生、找科室、预约、就诊和诊后资料展开。" },
      { title: "专业克制", text: "界面强调权威、清晰和留白，保持国际化医院官网体验。" },
      { title: "智能辅助", text: "AI 用于改善分诊、资料整理和用药理解，不替代医生判断。" },
    ],
    primary: { label: "查看医疗服务", to: { name: "service-internet-clinic" } },
  },
  "about-news": {
    label: "关于智慧云脑",
    title: "新闻动态",
    intro: "发布医院公告、服务更新、科研进展和患者教育资讯。",
    points: [
      { title: "服务公告", text: "门诊安排、系统维护和患者服务变更可在此集中发布。" },
      { title: "科研进展", text: "展示临床应用研究、教育项目和跨学科协作成果。" },
      { title: "患者教育", text: "通过健康文章和专题内容支持患者做出清晰决策。" },
    ],
    primary: { label: "健康文章", to: { name: "library-articles" } },
  },
  "about-careers": {
    label: "关于智慧云脑",
    title: "招聘信息",
    intro: "面向临床、护理、医学信息化、AI 应用和患者服务岗位建设专业团队。",
    points: [
      { title: "临床协作", text: "欢迎具备多学科协作经验的医护和运营团队加入。" },
      { title: "数字医疗", text: "关注真实医疗流程、数据安全、患者体验和 AI 边界。" },
      { title: "专业成长", text: "通过科研、教育和服务改进项目持续提升能力。" },
    ],
    primary: { label: "联系我们", to: { name: "about-contact" } },
  },
  "about-contact": {
    label: "关于智慧云脑",
    title: "联系我们",
    intro: "患者可通过在线服务、电话咨询和到院指引获得帮助；紧急情况请优先线下急诊。",
    points: [
      { title: "患者服务", text: "预约、病历、处方、报告和账户问题可先查看患者服务入口。" },
      { title: "到院咨询", text: "具体院区地址、交通和窗口安排以医院现场公告为准。" },
      { title: "紧急情况", text: "急症不要等待线上回复，请立即前往急诊或拨打急救电话。" },
    ],
    primary: { label: "患者服务", to: { name: "patient-dashboard" } },
  },
  "patient-reports": {
    label: "患者服务",
    title: "检查报告",
    intro: "检查检验结果生成后在患者服务中查看，报告内容应结合医生诊断解读。",
    points: [
      { title: "报告查看", text: "按检查项目、检查时间和报告状态整理，便于复诊前回看。" },
      { title: "复诊沟通", text: "如结果异常或与症状不一致，应结合病历、处方和医生建议判断。" },
      { title: "资料归档", text: "重要报告建议与病历、处方和出院小结一并保存。" },
    ],
    primary: { label: "查看我的病历", to: { name: "patient-records" } },
  },
  "patient-invoices": {
    label: "患者服务",
    title: "电子发票",
    intro: "围绕门诊、检查和处方相关费用提供票据查看入口，正式票据以医院财务系统为准。",
    points: [
      { title: "费用范围", text: "可按就诊记录、检查项目和处方记录回看相关费用说明。" },
      { title: "票据状态", text: "开票、补打和抬头修改等流程以医院财务规则为准。" },
      { title: "隐私保护", text: "票据与就诊资料均属于敏感信息，登录后仅展示本人或授权就诊人资料。" },
    ],
    primary: { label: "查看我的预约", to: { name: "patient-appointments" } },
  },
  "patient-messages": {
    label: "患者服务",
    title: "消息中心",
    intro: "集中查看预约提醒、就诊状态、处方审核、报告生成和系统通知。",
    points: [
      { title: "就诊提醒", text: "预约确认、就诊时间变化和取消提醒会优先展示。" },
      { title: "诊后通知", text: "病历、处方、检查报告等资料生成后可通过消息提示查看。" },
      { title: "重要信息", text: "涉及急症或病情变化时，请不要等待线上消息，优先线下就医。" },
    ],
    primary: { label: "进入患者服务", to: { name: "patient-dashboard" } },
  },
};

const page = computed(() => pages[String(route.name)] ?? fallback);
</script>

<template>
  <main class="static-info-page">
    <header class="resource-header">
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>{{ page.title }}</span>
      </nav>
      <p class="resource-label">{{ page.label }}</p>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
      <RouterLink v-if="page.primary" class="patient-primary static-primary" :to="page.primary.to">
        {{ page.primary.label }}
      </RouterLink>
    </header>

    <section class="static-info-grid">
      <article v-for="point in page.points" :key="point.title">
        <h2>{{ point.title }}</h2>
        <p>{{ point.text }}</p>
      </article>
    </section>
  </main>
</template>
